package com.example.bat_mon.FrontEnd.Helpers;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.bat_mon.FrontEnd.Balancing_Fragment;
import com.example.bat_mon.FrontEnd.CID_Fragment;

public class VPAdapter extends FragmentStateAdapter {

    public VPAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        Bundle args = new Bundle();
        args.putInt(CID_Fragment.CID_NUMBER, position + 1);

        if (position == 0 || position == 1) {
            fragment = new CID_Fragment();
        } else {
            fragment = new Balancing_Fragment();
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
