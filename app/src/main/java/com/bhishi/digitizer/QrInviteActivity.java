package com.bhishi.digitizer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.QrUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.Locale;

public class QrInviteActivity extends BaseActivity {
    public static final String EXTRA_GROUP_ID = "extra_group_id";
    private String groupId;
    private Group group;
    private ImageView ivQr;
    private TextView tvGroupName, tvGroupMeta, tvGroupCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_invite);
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        if (groupId == null) { finish(); return; }
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ivQr = findViewById(R.id.ivQr);
        tvGroupName = findViewById(R.id.tvGroupName);
        tvGroupMeta = findViewById(R.id.tvGroupMeta);
        tvGroupCode = findViewById(R.id.tvGroupCode);
        findViewById(R.id.btnShare).setOnClickListener(v -> shareInvite());
        loadGroup();
    }

    private void loadGroup() {
        FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                group = snapshot.getValue(Group.class);
                if (group == null) { finish(); return; }
                group.groupId = groupId;
                group.memberCount = (int) snapshot.child("members").getChildrenCount();
                NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                currency.setMaximumFractionDigits(0);
                tvGroupName.setText(group.groupName);
                tvGroupMeta.setText(currency.format(group.monthlyAmount) + " / month  •  " + group.memberCount + " members");
                tvGroupCode.setText(group.groupCode);
                renderQr();
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(QrInviteActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderQr() {
        if (group == null) return;
        try {
            Bitmap bitmap = QrUtils.generate(QrUtils.invitePayload(groupId, group.groupCode), 720);
            ivQr.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Toast.makeText(this, "Could not generate QR", Toast.LENGTH_LONG).show();
        }
    }

    private void shareInvite() {
        if (group == null) return;
        String message = "Join my Bhishi Digitizer group \"" + group.groupName + "\".\n" +
                "Group code: " + group.groupCode + "\n" +
                "Open Bhishi Digitizer → Scan QR or enter the code.";
        try {
            Bitmap bitmap = QrUtils.generate(QrUtils.invitePayload(groupId, group.groupCode), 1000);
            File directory = new File(getExternalFilesDir(null), "invites");
            if (!directory.exists()) directory.mkdirs();
            File image = new File(directory, "Bhishi_Invite_" + group.groupCode + ".png");
            try (FileOutputStream out = new FileOutputStream(image)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", image);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, message);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.share_invite)));
        } catch (Exception e) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(share, getString(R.string.share_invite)));
        }
    }
}
