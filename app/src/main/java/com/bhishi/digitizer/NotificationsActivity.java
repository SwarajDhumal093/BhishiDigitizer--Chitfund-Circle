package com.bhishi.digitizer;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.adapters.NotificationAdapter;
import com.bhishi.digitizer.models.NotificationItem;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationsActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvNotifications);
        View empty = findViewById(R.id.emptyNotifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        NotificationAdapter adapter = new NotificationAdapter();
        rv.setAdapter(adapter);

        String myUid = new PrefsManager(this).getUid();
        FirebasePaths.notifications(myUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NotificationItem> items = new ArrayList<>();
                for (DataSnapshot n : snapshot.getChildren()) {
                    NotificationItem item = n.getValue(NotificationItem.class);
                    if (item != null) items.add(item);
                }
                Collections.sort(items, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                adapter.setItems(items);
                boolean isEmpty = items.isEmpty();
                empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                for (DataSnapshot n : snapshot.getChildren()) {
                    if (!Boolean.TRUE.equals(n.child("seen").getValue(Boolean.class))) {
                        n.getRef().child("seen").setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                empty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            }
        });
    }
}
