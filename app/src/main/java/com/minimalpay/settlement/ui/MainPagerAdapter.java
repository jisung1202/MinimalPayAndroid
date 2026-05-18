package com.minimalpay.settlement.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case 0 -> new GroupFragment();
            case 1 -> new ExpenseFragment();
            case 2 -> new ReportFragment();
            case 3 -> new TransferFragment();
            default -> new GroupFragment();
        };
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
