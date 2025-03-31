package com.pineapple.capture.profile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.pineapple.capture.R;

public class ProfileActivity extends AppCompatActivity {
    private ProfileViewModel viewModel;
    private ImageView profileImage;
    private TextView userName;
    private TextView userBio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        profileImage = findViewById(R.id.profile_image);
        userName = findViewById(R.id.user_name);
        userBio = findViewById(R.id.user_bio);

        // Observe profile data changes
        viewModel.getUserProfile().observe(this, userProfile -> {
            if (userProfile != null) {
                userName.setText(userProfile.getName());
                userBio.setText(userProfile.getBio());
                // Load profile image using a library like Glide
            }
        });
    }
} 