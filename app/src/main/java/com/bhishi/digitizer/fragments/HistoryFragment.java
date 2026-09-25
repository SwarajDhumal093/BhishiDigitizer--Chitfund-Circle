package com.bhishi.digitizer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.adapters.PassbookAdapter;
import com.bhishi.digitizer.models.Contribution;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String ARG_GROUP_ID = "group_id";
    private String groupId;
    private View emptyState;
    private RecyclerView rv;

    public static HistoryFragment newInstance(String groupId) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        if (getArguments() != null) groupId = getArguments().getString(ARG_GROUP_ID);

        rv = view.findViewById(R.id.rvHistory);
        emptyState = view.findViewById(R.id.emptyHistory);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        PassbookAdapter adapter = new PassbookAdapter();
        rv.setAdapter(adapter);

        loadHistory(adapter);
        return view;
    }

    private void loadHistory(PassbookAdapter adapter) {
        String myUid = new PrefsManager(requireContext()).getUid();

        FirebasePaths.contributions(groupId, "").getParent().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot allMonthsSnapshot) {
                DataSnapshot groupContributions = allMonthsSnapshot.child(groupId);
                List<Contribution> rows = new ArrayList<>();

                for (DataSnapshot monthSnap : groupContributions.getChildren()) {
                    String monthKey = monthSnap.getKey();
                    Contribution c = monthSnap.child(myUid).getValue(Contribution.class);
                    if (c != null) {
                        c.memberName = monthKey; // repurposed to show the month label in the row
                        rows.add(c);
                    }
                }
                adapter.setRows(rows);
                boolean empty = rows.isEmpty();
                rv.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (emptyState != null) emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                rv.setVisibility(View.GONE);
                if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            }
        });
    }
}
