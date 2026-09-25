package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.adapters.DisputeAdapter;
import com.bhishi.digitizer.models.Dispute;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DisputeLogActivity extends BaseActivity {

    private DisputeAdapter adapter;
    private String firstGroupId; // used as the target group when raising a new dispute

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispute_log);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvDisputes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DisputeAdapter(this::showDisputeDetail);
        rv.setAdapter(adapter);

        findViewById(R.id.btnRaiseDispute).setOnClickListener(v -> {
            if (firstGroupId == null) {
                android.widget.Toast.makeText(this, "Join a group first to raise a dispute", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, RaiseDisputeActivity.class);
            intent.putExtra(RaiseDisputeActivity.EXTRA_GROUP_ID, firstGroupId);
            startActivity(intent);
        });

        loadDisputes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDisputes();
    }

    private void loadDisputes() {
        String myUid = new PrefsManager(this).getUid();

        FirebasePaths.user(myUid).child("groupsJoined").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot joinedSnapshot) {
                List<Dispute> myDisputes = new ArrayList<>();
                for (DataSnapshot joined : joinedSnapshot.getChildren()) {
                    String groupId = joined.getKey();
                    if (firstGroupId == null) firstGroupId = groupId;

                    FirebasePaths.disputes(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot disputesSnapshot) {
                            for (DataSnapshot d : disputesSnapshot.getChildren()) {
                                Dispute dispute = d.getValue(Dispute.class);
                                if (dispute != null && myUid.equals(dispute.raisedByUid)) {
                                    dispute.disputeId = d.getKey();
                                    myDisputes.add(dispute);
                                }
                            }
                            adapter.setDisputes(myDisputes);
                        }

                        @Override
                        public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
            }
        });
    }

    private void showDisputeDetail(Dispute dispute) {
        String message = "Status: " + dispute.status
                + (dispute.resolutionNote != null ? "\n\nResolution: " + dispute.resolutionNote : "\n\nAwaiting admin review.");
        new AlertDialog.Builder(this)
                .setTitle(dispute.issue)
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }
}
