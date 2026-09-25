package com.bhishi.digitizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bhishi.digitizer.models.Dispute;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;

public class RaiseDisputeActivity extends BaseActivity {

    public static final String EXTRA_GROUP_ID = "extra_group_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raise_dispute);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        EditText etMonth = findViewById(R.id.etMonth);
        EditText etIssue = findViewById(R.id.etIssue);

        findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            String month = etMonth.getText().toString().trim();
            String issue = etIssue.getText().toString().trim();

            if (TextUtils.isEmpty(month) || TextUtils.isEmpty(issue)) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            PrefsManager prefs = new PrefsManager(this);
            Dispute dispute = new Dispute(prefs.getUid(), prefs.getName(), month, issue);

            FirebasePaths.disputes(groupId).push().setValue(dispute)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Dispute submitted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Could not submit: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}
