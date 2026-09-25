package com.bhishi.digitizer;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FinancialCalendarActivity extends BaseActivity {

    private LinearLayout upcomingContainer;
    private View emptyState;
    private final List<CalendarEvent> events = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financial_calendar);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        upcomingContainer = findViewById(R.id.upcomingContainer);
        emptyState = findViewById(R.id.emptyCalendar);
        loadEvents();
    }

    private void loadEvents() {
        String uid = new PrefsManager(this).getUid();
        if (uid == null) return;
        events.clear();
        FirebasePaths.user(uid).child("groupsJoined").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    renderEvents();
                    return;
                }
                final int total = (int) snapshot.getChildrenCount();
                final int[] done = {0};
                for (DataSnapshot joined : snapshot.getChildren()) {
                    String groupId = joined.getKey();
                    FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot groupSnap) {
                            Group g = groupSnap.getValue(Group.class);
                            if (g != null) addGroupEvents(g);
                            done[0]++;
                            if (done[0] >= total) renderEvents();
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            done[0]++;
                            if (done[0] >= total) renderEvents();
                        }
                    });
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                renderEvents();
            }
        });
    }

    private void addGroupEvents(Group group) {
        Calendar cal = Calendar.getInstance();
        int dueDay = group.dueDay <= 0 ? 5 : group.dueDay;
        cal.set(Calendar.DAY_OF_MONTH, Math.min(dueDay, cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
        events.add(new CalendarEvent(cal.getTime(), group.groupName, getString(R.string.monthly_contribution_due)));

        Calendar reminder = (Calendar) cal.clone();
        reminder.add(Calendar.DATE, group.graceDays <= 0 ? 3 : group.graceDays);
        events.add(new CalendarEvent(reminder.getTime(), group.groupName, getString(R.string.grace_period_ends)));

        Calendar payout = (Calendar) cal.clone();
        payout.add(Calendar.DATE, 6);
        events.add(new CalendarEvent(payout.getTime(), group.groupName,
                "auction".equalsIgnoreCase(group.mode) ? getString(R.string.sealed_auction_window) : getString(R.string.lucky_draw_round)));
    }

    private void renderEvents() {
        upcomingContainer.removeAllViews();
        Collections.sort(events, Comparator.comparing(e -> e.date));
        boolean empty = events.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        upcomingContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
        for (CalendarEvent event : events) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            card.setLayoutParams(lp);
            card.setBackgroundResource(R.drawable.bg_card);

            TextView title = new TextView(this);
            title.setText(event.groupName);
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(15f);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            card.addView(title);

            TextView subtitle = new TextView(this);
            subtitle.setText(event.label + " · " + dateFormat.format(event.date));
            subtitle.setTextColor(getColor(R.color.text_secondary));
            subtitle.setTextSize(12f);
            subtitle.setPadding(0, dp(6), 0, 0);
            card.addView(subtitle);

            TextView badge = new TextView(this);
            badge.setText(event.label.toUpperCase(Locale.US));
            badge.setTextColor(getColor(R.color.gold_dark));
            badge.setBackgroundResource(R.drawable.bg_chip_neutral);
            badge.setPadding(dp(10), dp(6), dp(10), dp(6));
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeLp.topMargin = dp(10);
            badge.setLayoutParams(badgeLp);
            badge.setGravity(Gravity.CENTER);
            card.addView(badge);

            upcomingContainer.addView(card);
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private static class CalendarEvent {
        final Date date;
        final String groupName;
        final String label;

        CalendarEvent(Date date, String groupName, String label) {
            this.date = date;
            this.groupName = groupName == null ? "Bhishi group" : groupName;
            this.label = label;
        }
    }
}
