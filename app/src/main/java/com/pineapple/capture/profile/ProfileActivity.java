package com.pineapple.capture.profile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.pineapple.capture.R;
import com.pineapple.capture.models.User;

public class ProfileActivity extends AppCompatActivity {
    private ProfileViewModel viewModel;
    private ImageView profileImage;
    private TextView userName;
    private TextView userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        profileImage = findViewById(R.id.profile_image);
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);

        // Observe user data changes
        viewModel.getUserData().observe(this, user -> {
            if (user != null) {
                userName.setText(user.getUsername());
                userEmail.setText(user.getEmail());
                
                // Load profile image using Glide
                if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                    Glide.with(this)
                            .load(user.getProfilePictureUrl())
                            .circleCrop()
                            .into(profileImage);
                }
            }
        });
    }
} 