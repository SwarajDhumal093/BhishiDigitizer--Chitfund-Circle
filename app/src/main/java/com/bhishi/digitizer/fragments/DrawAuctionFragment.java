package com.bhishi.digitizer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.models.AuctionBid;
import com.bhishi.digitizer.models.Contribution;
import com.bhishi.digitizer.models.GroupMember;
import com.bhishi.digitizer.models.Payout;
import com.bhishi.digitizer.models.RoundConfig;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.bhishi.digitizer.utils.PayoutCertificatePdfGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Payout UI for lucky draw and lowest-bid auction.
 *
 * Members can submit sealed bids directly under Firebase security rules, but winner selection is
 * intentionally NOT performed on the Android client. The authenticated backend re-reads the group,
 * verified contributions, prior winners and bids, generates a cryptographic audit nonce, selects the
 * result and locks it with a server-side transaction. This prevents a modified admin app from simply
 * writing a preferred winner.
 */
public class DrawAuctionFragment extends Fragment {

    private static final String ARG_GROUP_ID = "group_id";
    private static final String ARG_MONTHLY_AMOUNT = "monthly_amount";
    private static final String ARG_MODE = "mode";
    private static final String ARG_IS_ADMIN = "is_admin";

    private String groupId;
    private String mode = "draw";
    private double monthlyAmount;
    private boolean isAdmin;
    private String myUid;
    private String myName;

    private LinearLayout modePanel, auctionSessionPanel, auctionBidPanel, adminActionPanel, resultPanel;
    private ImageView ivModeIcon;
    private TextView tvModeTitle, tvModeDescription, tvRoundStatus, tvEligibility, tvPool;
    private TextView btnSubmitBid, tvMyBidStatus, btnRunDraw, tvAdminHint;
    private TextView tvResultTitle, tvResultSubtitle, tvAuditCode;
    private TextView tvAuctionSessionStatus, tvAuctionTimer, btnOpenAuction, btnAuditDetails, btnShareResult, btnWinnerCertificate;
    private EditText etBidAmount;

    private boolean allContributionsLocked;
    private boolean canCurrentUserBid;
    private boolean auctionOpen;
    private long auctionCloseAt;
    private Payout lastPayout;
    private Runnable auctionTicker;
    private int memberCount;
    private double poolAmount;
    private final List<GroupMember> eligibleMembers = new ArrayList<>();
    private final Map<String, GroupMember> memberByUid = new HashMap<>();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public static DrawAuctionFragment newInstance(String groupId, double monthlyAmount, String mode, boolean isAdmin) {
        DrawAuctionFragment fragment = new DrawAuctionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        args.putDouble(ARG_MONTHLY_AMOUNT, monthlyAmount);
        args.putString(ARG_MODE, mode);
        args.putBoolean(ARG_IS_ADMIN, isAdmin);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_draw_auction, container, false);
        if (getArguments() != null) {
            groupId = getArguments().getString(ARG_GROUP_ID);
            monthlyAmount = getArguments().getDouble(ARG_MONTHLY_AMOUNT);
            String argMode = getArguments().getString(ARG_MODE);
            if (argMode != null) mode = argMode;
            isAdmin = getArguments().getBoolean(ARG_IS_ADMIN, false);
        }

        PrefsManager prefs = new PrefsManager(requireContext());
        myUid = prefs.getUid();
        myName = prefs.getName();
        currency.setMaximumFractionDigits(0);

        bindViews(view);
        configureModeUi();
        btnSubmitBid.setOnClickListener(v -> submitBid());
        btnRunDraw.setOnClickListener(v -> startFinalizationSequence());
        btnOpenAuction.setOnClickListener(v -> openAuctionWindow());
        btnAuditDetails.setOnClickListener(v -> showAuditDetails());
        btnShareResult.setOnClickListener(v -> shareResult());
        btnWinnerCertificate.setOnClickListener(v -> shareWinnerCertificate());
        refreshRound();
        return view;
    }

    private void bindViews(View view) {
        modePanel = view.findViewById(R.id.modePanel);
        auctionSessionPanel = view.findViewById(R.id.auctionSessionPanel);
        auctionBidPanel = view.findViewById(R.id.auctionBidPanel);
        adminActionPanel = view.findViewById(R.id.adminActionPanel);
        resultPanel = view.findViewById(R.id.resultPanel);
        ivModeIcon = view.findViewById(R.id.ivModeIcon);
        tvModeTitle = view.findViewById(R.id.tvModeTitle);
        tvModeDescription = view.findViewById(R.id.tvModeDescription);
        tvRoundStatus = view.findViewById(R.id.tvRoundStatus);
        tvEligibility = view.findViewById(R.id.tvEligibility);
        tvPool = view.findViewById(R.id.tvPool);
        etBidAmount = view.findViewById(R.id.etBidAmount);
        btnSubmitBid = view.findViewById(R.id.btnSubmitBid);
        tvMyBidStatus = view.findViewById(R.id.tvMyBidStatus);
        btnRunDraw = view.findViewById(R.id.btnRunDraw);
        tvAdminHint = view.findViewById(R.id.tvAdminHint);
        tvResultTitle = view.findViewById(R.id.tvResultTitle);
        tvResultSubtitle = view.findViewById(R.id.tvResultSubtitle);
        tvAuditCode = view.findViewById(R.id.tvAuditCode);
        tvAuctionSessionStatus = view.findViewById(R.id.tvAuctionSessionStatus);
        tvAuctionTimer = view.findViewById(R.id.tvAuctionTimer);
        btnOpenAuction = view.findViewById(R.id.btnOpenAuction);
        btnAuditDetails = view.findViewById(R.id.btnAuditDetails);
        btnShareResult = view.findViewById(R.id.btnShareResult);
        btnWinnerCertificate = view.findViewById(R.id.btnWinnerCertificate);
    }

    private void configureModeUi() {
        boolean auction = "auction".equalsIgnoreCase(mode);
        if (auction) {
            modePanel.setBackgroundResource(R.drawable.bg_auction_panel);
            ivModeIcon.setImageResource(R.drawable.ic_gavel);
            tvModeTitle.setText("Private sealed-bid auction");
            tvModeDescription.setText("Eligible members submit private bids. The server chooses the lowest valid bid; any exact tie is resolved with the locked audit nonce.");
            auctionSessionPanel.setVisibility(View.VISIBLE);
            auctionBidPanel.setVisibility(View.VISIBLE);
            btnOpenAuction.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            btnRunDraw.setText("Close auction securely");
            tvAdminHint.setText("The backend re-checks every contribution and bid before it locks the result. The admin cannot choose the winner from this screen.");
        } else {
            modePanel.setBackgroundResource(R.drawable.bg_draw_panel);
            ivModeIcon.setImageResource(R.drawable.ic_trophy);
            tvModeTitle.setText("Server-verified lucky draw");
            tvModeDescription.setText("The backend builds the eligible list, generates a cryptographic nonce and deterministically selects the winner. A public audit code is stored with the result.");
            auctionSessionPanel.setVisibility(View.GONE);
            auctionBidPanel.setVisibility(View.GONE);
            btnRunDraw.setText("Run verified lucky draw");
            tvAdminHint.setText("The draw unlocks only after every current contribution is confirmed by both the member and admin.");
        }
        adminActionPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
    }

    private void refreshRound() {
        String monthKey = FirebasePaths.currentMonthKey();
        FirebasePaths.payout(groupId, monthKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Payout payout = snapshot.getValue(Payout.class);
                    if (payout != null) showLockedResult(payout);
                } else {
                    resultPanel.setVisibility(View.GONE);
                    loadEligibility();
                    if ("auction".equalsIgnoreCase(mode)) loadAuctionWindow();
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                tvRoundStatus.setText("Could not load payout state");
            }
        });
    }

    private void loadEligibility() {
        String monthKey = FirebasePaths.currentMonthKey();
        FirebasePaths.groupMembers(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot membersSnapshot) {
                memberByUid.clear();
                for (DataSnapshot child : membersSnapshot.getChildren()) {
                    GroupMember member = child.getValue(GroupMember.class);
                    if (member != null && member.uid != null) memberByUid.put(member.uid, member);
                }
                memberCount = memberByUid.size();
                poolAmount = monthlyAmount * memberCount;
                tvPool.setText(currency.format(poolAmount));

                FirebasePaths.contributions(groupId, monthKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot contributionsSnapshot) {
                        eligibleMembers.clear();
                        allContributionsLocked = memberCount > 0;
                        canCurrentUserBid = false;
                        int verifiedCount = 0;

                        for (GroupMember member : memberByUid.values()) {
                            Contribution c = contributionsSnapshot.child(member.uid).getValue(Contribution.class);
                            boolean locked = c != null && c.isLocked();
                            if (locked) verifiedCount++; else allContributionsLocked = false;
                            if (locked && !member.hasReceivedPayout) {
                                eligibleMembers.add(member);
                                if (member.uid.equals(myUid)) canCurrentUserBid = true;
                            }
                        }

                        tvEligibility.setText(String.valueOf(eligibleMembers.size()));
                        if (memberCount == 0) {
                            tvRoundStatus.setText("No members in this group yet");
                        } else if (allContributionsLocked) {
                            tvRoundStatus.setText("All " + memberCount + " contributions are dual-verified");
                        } else {
                            tvRoundStatus.setText(verifiedCount + "/" + memberCount + " contributions fully verified");
                        }
                        updateActionAvailability();
                        if ("auction".equalsIgnoreCase(mode)) loadOwnBid();
                    }

                    @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
                });
            }

            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
        });
    }

    private void updateActionAvailability() {
        boolean ready = allContributionsLocked && !eligibleMembers.isEmpty();
        boolean auction = "auction".equalsIgnoreCase(mode);
        boolean canFinalize = ready && (!auction || auctionCloseAt > 0);
        btnRunDraw.setEnabled(isAdmin && canFinalize);
        btnRunDraw.setAlpha(isAdmin && canFinalize ? 1f : 0.55f);
        if (auction) {
            boolean bidEnabled = canCurrentUserBid && auctionOpen && System.currentTimeMillis() < auctionCloseAt;
            btnSubmitBid.setEnabled(bidEnabled);
            btnSubmitBid.setAlpha(bidEnabled ? 1f : 0.55f);
            etBidAmount.setEnabled(bidEnabled);
            btnOpenAuction.setEnabled(isAdmin && ready && !auctionOpen);
            btnOpenAuction.setAlpha(isAdmin && ready && !auctionOpen ? 1f : 0.55f);
            if (!allContributionsLocked) {
                tvMyBidStatus.setText("Bidding opens after every current contribution is dual-verified.");
            } else if (!auctionOpen) {
                tvMyBidStatus.setText("The sealed auction is not open. Your bid cannot be changed outside the auction window.");
            } else if (!canCurrentUserBid) {
                tvMyBidStatus.setText("You are not eligible for this payout round.");
            }
        }
    }

    private void loadOwnBid() {
        if (myUid == null) return;
        FirebasePaths.auctionBids(groupId, FirebasePaths.currentMonthKey()).child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        AuctionBid bid = snapshot.getValue(AuctionBid.class);
                        if (bid != null && bid.amount > 0) {
                            etBidAmount.setText(formatPlainAmount(bid.amount));
                            tvMyBidStatus.setText("Sealed bid saved: " + currency.format(bid.amount) + ". You may update it until the round closes.");
                        } else if (canCurrentUserBid) {
                            tvMyBidStatus.setText("No bid submitted yet. Other members cannot read your amount.");
                        }
                    }

                    @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
                });
    }

    private void submitBid() {
        if (!canCurrentUserBid || myUid == null) {
            Toast.makeText(requireContext(), "You are not eligible to bid in this round", Toast.LENGTH_LONG).show();
            return;
        }
        if (!auctionOpen || System.currentTimeMillis() >= auctionCloseAt) {
            Toast.makeText(requireContext(), "The auction window is closed", Toast.LENGTH_LONG).show();
            loadAuctionWindow();
            return;
        }
        String raw = etBidAmount.getText().toString().trim().replace(",", "");
        if (TextUtils.isEmpty(raw)) {
            etBidAmount.setError("Enter a bid amount");
            return;
        }

        final double bid;
        try {
            bid = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            etBidAmount.setError("Enter a valid amount");
            return;
        }
        if (bid <= 0 || bid > poolAmount) {
            etBidAmount.setError("Bid must be above ₹0 and not exceed " + currency.format(poolAmount));
            return;
        }

        GroupMember member = memberByUid.get(myUid);
        AuctionBid auctionBid = new AuctionBid(myUid, member != null ? member.name : myName, bid);
        btnSubmitBid.setEnabled(false);
        FirebasePaths.auctionBids(groupId, FirebasePaths.currentMonthKey()).child(myUid).setValue(auctionBid)
                .addOnSuccessListener(unused -> {
                    btnSubmitBid.setEnabled(true);
                    tvMyBidStatus.setText("Sealed bid saved: " + currency.format(bid) + ". You may update it until the round closes.");
                    Toast.makeText(requireContext(), "Sealed bid saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSubmitBid.setEnabled(true);
                    Toast.makeText(requireContext(), "Could not save bid: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadAuctionWindow() {
        FirebasePaths.roundConfig(groupId, FirebasePaths.currentMonthKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        RoundConfig config = snapshot.getValue(RoundConfig.class);
                        long now = System.currentTimeMillis();
                        auctionOpen = config != null && config.isOpen(now);
                        auctionCloseAt = config == null ? 0 : config.closeAt;
                        if (auctionOpen) {
                            tvAuctionSessionStatus.setText(getString(R.string.auction_open));
                            btnOpenAuction.setText("Auction open");
                            startAuctionTicker();
                        } else if (config != null && config.closeAt > 0) {
                            tvAuctionSessionStatus.setText(getString(R.string.auction_closed));
                            tvAuctionTimer.setText("00:00");
                            btnOpenAuction.setText("Reopen sealed auction · 30 min");
                        } else {
                            tvAuctionSessionStatus.setText(getString(R.string.auction_not_open));
                            tvAuctionTimer.setText("30:00");
                            btnOpenAuction.setText(getString(R.string.open_auction_30));
                        }
                        updateActionAvailability();
                    }
                    @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        tvAuctionSessionStatus.setText("Could not load auction window");
                    }
                });
    }

    private void openAuctionWindow() {
        if (!isAdmin) return;
        if (!allContributionsLocked || eligibleMembers.isEmpty()) {
            Toast.makeText(requireContext(), "Verify every current contribution before opening the auction", Toast.LENGTH_LONG).show();
            return;
        }
        long now = System.currentTimeMillis();
        RoundConfig config = new RoundConfig("open", now, now + 30L * 60L * 1000L, 30);
        btnOpenAuction.setEnabled(false);
        FirebasePaths.roundConfig(groupId, FirebasePaths.currentMonthKey()).setValue(config)
                .addOnSuccessListener(unused -> {
                    auctionOpen = true;
                    auctionCloseAt = config.closeAt;
                    tvAuctionSessionStatus.setText(getString(R.string.auction_open));
                    btnOpenAuction.setText("Auction open");
                    startAuctionTicker();
                    updateActionAvailability();
                    notifyRoundEventAsync("auction_started");
                    Toast.makeText(requireContext(), "30-minute sealed auction opened", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnOpenAuction.setEnabled(true);
                    Toast.makeText(requireContext(), "Could not open auction: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void startAuctionTicker() {
        if (auctionTicker != null) main.removeCallbacks(auctionTicker);
        auctionTicker = new Runnable() {
            @Override public void run() {
                if (!isAdded()) return;
                long remaining = auctionCloseAt - System.currentTimeMillis();
                if (remaining <= 0) {
                    auctionOpen = false;
                    tvAuctionTimer.setText("00:00");
                    tvAuctionSessionStatus.setText(getString(R.string.auction_closed));
                    btnOpenAuction.setText("Reopen sealed auction · 30 min");
                    updateActionAvailability();
                    return;
                }
                long totalSeconds = remaining / 1000L;
                long minutes = totalSeconds / 60L;
                long seconds = totalSeconds % 60L;
                tvAuctionTimer.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
                main.postDelayed(this, 1000L);
            }
        };
        main.post(auctionTicker);
    }

    private void startFinalizationSequence() {
        if (!isAdmin || !allContributionsLocked || eligibleMembers.isEmpty()) return;
        if ("auction".equalsIgnoreCase(mode)) {
            finalizeRoundOnServer();
            return;
        }
        btnRunDraw.setEnabled(false);
        notifyRoundEventAsync("draw_started");
        final int[] frame = {0};
        Runnable shufflePreview = new Runnable() {
            @Override public void run() {
                if (!isAdded()) return;
                if (frame[0] < 18) {
                    GroupMember preview = eligibleMembers.get(frame[0] % eligibleMembers.size());
                    tvRoundStatus.setText("Verifying pool • " + (preview.name == null ? "Member" : preview.name));
                    frame[0]++;
                    main.postDelayed(this, 115L + (frame[0] * 7L));
                } else {
                    tvRoundStatus.setText("Display shuffle complete • requesting secure server result…");
                    finalizeRoundOnServer();
                }
            }
        };
        main.post(shufflePreview);
    }

    private void showAuditDetails() {
        if (lastPayout == null) return;
        String message = "Mode: " + ("auction".equalsIgnoreCase(lastPayout.mode) ? "Sealed auction" : "Lucky draw") +
                "\nEligible participants: " + lastPayout.eligibleCount +
                "\nPool: " + currency.format(lastPayout.poolAmount) +
                "\nAudit code: " + (TextUtils.isEmpty(lastPayout.auditHash) ? "Legacy result" : lastPayout.auditHash) +
                "\nResult state: permanently locked";
        new AlertDialog.Builder(requireContext())
                .setTitle("Payout audit details")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void shareResult() {
        if (lastPayout == null) return;
        String text = "Bhishi Digitizer payout result\n" +
                "Winner: " + lastPayout.winnerName + "\n" +
                "Cycle: " + lastPayout.monthKey + "\n" +
                "Mode: " + lastPayout.mode + "\n" +
                "Pool: " + currency.format(lastPayout.poolAmount) + "\n" +
                ("auction".equalsIgnoreCase(lastPayout.mode) ? "Winning bid: " + currency.format(lastPayout.bidAmount) + "\n" : "") +
                "Audit code: " + (TextUtils.isEmpty(lastPayout.auditHash) ? "legacy" : lastPayout.auditHash);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Share payout result"));
    }

    private void shareWinnerCertificate() {
        if (lastPayout == null) return;
        try {
            File file = PayoutCertificatePdfGenerator.generate(requireContext(), lastPayout);
            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/pdf");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            send.putExtra(Intent.EXTRA_TEXT, "Bhishi Digitizer payout certificate • " + lastPayout.monthKey);
            startActivity(Intent.createChooser(send, getString(R.string.winner_certificate)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not create certificate: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void finalizeRoundOnServer() {
        if (!isAdmin || !allContributionsLocked || eligibleMembers.isEmpty()) return;
        if ("auction".equalsIgnoreCase(mode) && auctionCloseAt <= 0) {
            Toast.makeText(requireContext(), "Open the sealed auction before finalising it", Toast.LENGTH_LONG).show();
            return;
        }
        String baseUrl = getString(R.string.payment_backend_base_url).trim();
        if (!baseUrl.startsWith("https://") || baseUrl.contains("YOUR_BACKEND_DOMAIN")) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Secure backend not configured")
                    .setMessage("Set payment_backend_base_url in strings.xml to the HTTPS backend included with this project. Draw and auction finalisation are intentionally server-authoritative so a modified admin app cannot write its own winner.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        btnRunDraw.setEnabled(false);
        btnRunDraw.setAlpha(0.6f);
        tvRoundStatus.setText("Finalising securely on the server…");
        withFirebaseToken(token -> executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("groupId", groupId);
                JSONObject response = postJson(baseUrl + "/payouts/finalize", body, token);
                JSONObject p = response.getJSONObject("payout");
                Payout payout = parsePayout(p);
                main.post(() -> {
                    if (!isAdded()) return;
                    showLockedResult(payout);
                    Toast.makeText(requireContext(), "Payout result locked", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (!isAdded()) return;
                    btnRunDraw.setEnabled(true);
                    btnRunDraw.setAlpha(1f);
                    tvRoundStatus.setText("Round is ready");
                    Toast.makeText(requireContext(), safeMessage(e), Toast.LENGTH_LONG).show();
                    refreshRound();
                });
            }
        }));
    }

    private void notifyRoundEventAsync(String eventType) {
        String baseUrl = getString(R.string.payment_backend_base_url).trim();
        if (!baseUrl.startsWith("https://") || baseUrl.contains("YOUR_BACKEND_DOMAIN")) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        user.getIdToken(false).addOnSuccessListener(result -> executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("groupId", groupId);
                body.put("eventType", eventType);
                postJson(baseUrl + "/rounds/event", body, result.getToken());
            } catch (Exception ignored) {
                // Notification delivery must never block or fail the financial round itself.
            }
        }));
    }

    private Payout parsePayout(JSONObject json) {
        Payout payout = new Payout();
        payout.monthKey = json.optString("monthKey", FirebasePaths.currentMonthKey());
        payout.winnerUid = json.optString("winnerUid", "");
        payout.winnerName = json.optString("winnerName", "Member");
        payout.mode = json.optString("mode", mode);
        payout.bidAmount = json.optDouble("bidAmount", 0);
        payout.dividendPerMember = json.optDouble("dividendPerMember", 0);
        payout.poolAmount = json.optDouble("poolAmount", poolAmount);
        payout.eligibleCount = json.optInt("eligibleCount", eligibleMembers.size());
        payout.auditNonce = json.optString("auditNonce", null);
        payout.auditHash = json.optString("auditHash", null);
        payout.lockedResult = json.optBoolean("lockedResult", true);
        payout.timestamp = json.optLong("timestamp", System.currentTimeMillis());
        return payout;
    }

    private interface TokenCallback { void onToken(String token); }

    private void withFirebaseToken(TokenCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show();
            btnRunDraw.setEnabled(true);
            return;
        }
        user.getIdToken(false)
                .addOnSuccessListener(result -> callback.onToken(result.getToken()))
                .addOnFailureListener(e -> {
                    btnRunDraw.setEnabled(true);
                    Toast.makeText(requireContext(), "Could not authorize payout request", Toast.LENGTH_LONG).show();
                });
    }

    private JSONObject postJson(String endpoint, JSONObject body, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder text = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) text.append(line);
            }
        }
        connection.disconnect();
        if (status < 200 || status >= 300) {
            String message = "Server could not finalise this round";
            try {
                JSONObject error = new JSONObject(text.toString());
                message = error.optString("error", message);
            } catch (Exception ignored) { }
            throw new IllegalStateException(message);
        }
        return new JSONObject(text.toString());
    }

    private void showLockedResult(Payout payout) {
        lastPayout = payout;
        if (auctionTicker != null) main.removeCallbacks(auctionTicker);
        auctionOpen = false;
        resultPanel.setVisibility(View.VISIBLE);
        adminActionPanel.setVisibility(View.GONE);
        auctionBidPanel.setVisibility(View.GONE);
        auctionSessionPanel.setVisibility(View.GONE);
        tvRoundStatus.setText("Result locked for " + payout.monthKey);
        tvEligibility.setText(payout.eligibleCount > 0 ? String.valueOf(payout.eligibleCount) : "—");
        tvPool.setText(currency.format(payout.poolAmount));
        tvResultTitle.setText(payout.winnerName + " receives this month's payout");
        if ("auction".equalsIgnoreCase(payout.mode)) {
            tvResultSubtitle.setText("Winning bid " + currency.format(payout.bidAmount) + " • dividend " + currency.format(payout.dividendPerMember) + " to each other member");
        } else {
            tvResultSubtitle.setText(currency.format(payout.poolAmount) + " payout • selected from verified eligible members");
        }
        tvAuditCode.setText("Audit code  " + (TextUtils.isEmpty(payout.auditHash) ? "legacy result" : payout.auditHash));
        resultPanel.setAlpha(0f);
        resultPanel.setScaleX(0.97f);
        resultPanel.setScaleY(0.97f);
        resultPanel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(320)
                .setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    private String formatPlainAmount(double value) {
        return Math.rint(value) == value ? String.valueOf((long) value) : String.valueOf(value);
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? "Could not finalise payout round" : e.getMessage();
    }

    @Override
    public void onDestroy() {
        if (auctionTicker != null) main.removeCallbacks(auctionTicker);
        super.onDestroy();
        executor.shutdownNow();
    }
}
