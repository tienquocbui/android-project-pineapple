package com.pineapple.capture.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pineapple.capture.R;
import com.pineapple.capture.databinding.FragmentHomeBinding;
import com.pineapple.capture.viewmodel.ProfileViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private ProfileViewModel profileViewModel;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize ProfileViewModel
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Observe user data
        profileViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.usernameText.setText(user.getUsername());
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
