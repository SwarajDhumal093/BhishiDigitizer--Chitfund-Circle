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
import com.bhishi.digitizer.adapters.MemberAdapter;
import com.bhishi.digitizer.models.GroupMember;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MembersFragment extends Fragment {

    private static final String ARG_GROUP_ID = "group_id";
    private String groupId;
    private MemberAdapter adapter;

    public static MembersFragment newInstance(String groupId) {
        MembersFragment fragment = new MembersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_members, container, false);

        if (getArguments() != null) groupId = getArguments().getString(ARG_GROUP_ID);

        RecyclerView rv = view.findViewById(R.id.rvMembers);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MemberAdapter();
        rv.setAdapter(adapter);

        loadMembers();
        return view;
    }

    private void loadMembers() {
        FirebasePaths.groupMembers(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<GroupMember> members = new ArrayList<>();
                for (DataSnapshot m : snapshot.getChildren()) {
                    GroupMember member = m.getValue(GroupMember.class);
                    if (member != null) members.add(member);
                }
                adapter.setMembers(members);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
            }
        });
    }
}
