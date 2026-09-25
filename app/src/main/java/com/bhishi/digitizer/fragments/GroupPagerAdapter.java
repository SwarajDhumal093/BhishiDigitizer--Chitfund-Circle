package com.bhishi.digitizer.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class GroupPagerAdapter extends FragmentStateAdapter {

    private final String groupId;
    private final double monthlyAmount;
    private final String mode;
    private final boolean isAdmin;

    public GroupPagerAdapter(@NonNull FragmentActivity activity, String groupId, double monthlyAmount, String mode, boolean isAdmin) {
        super(activity);
        this.groupId = groupId;
        this.monthlyAmount = monthlyAmount;
        this.mode = mode;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return ContributionTrackerFragment.newInstance(groupId, monthlyAmount, isAdmin);
            case 1:
                return DrawAuctionFragment.newInstance(groupId, monthlyAmount, mode, isAdmin);
            case 2:
                return MembersFragment.newInstance(groupId);
            case 3:
            default:
                return HistoryFragment.newInstance(groupId);
        }
    }

    @Override
    public int getItemCount() { return 4; }
}
