package com.pineapple.capture.activities;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pineapple.capture.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private TextView selectedCountText;
    private Button saveButton;
    private List<String> selectedInterests = new ArrayList<>();
    private final int MAX_SELECTIONS = 6;

    // Interest categories and their options
    private final Map<String, List<String>> interestCategories = new HashMap<String, List<String>>() {{
        put("Creativity", Arrays.asList("Art", "Dancing", "Make-up", "Video", "Cosplay", "Design", "Photography", "Crafts", "Fashion", "Singing"));
        put("Sports", Arrays.asList("Badminton", "Bouldering", "Crew", "Baseball", "Bowling", "Cricket", "Basketball", "Boxing", "Cycling"));
        put("Pets", Arrays.asList("Amphibians", "Cats", "Horses", "Arthropods", "Dogs", "Rabbits", "Birds", "Fish", "Reptiles", "Turtles"));
        // Add more categories as needed
    }};
    
    // Map to store interest colors
    private Map<String, Integer> interestColors = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Initialize views
        selectedCountText = findViewById(R.id.interests_count);
        saveButton = findViewById(R.id.save_button);
        ImageButton backButton = findViewById(R.id.back_button);
        
        // Initialize interest colors for all possible interests
        for (List<String> interestList : interestCategories.values()) {
            for (String interest : interestList) {
                interestColors.put(interest, getInterestCategoryColor(interest));
            }
        }
        
        // Setup UI components
        setupInterestCategories();
        updateSelectedCount();
        
        // Set up click listeners
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveInterests());
        
        // Add help button functionality
        findViewById(R.id.help_button).setOnClickListener(v -> {
            showHelpDialog();
        });
        
        // Load existing interests if any
        loadExistingInterests();
    }
    
    private void setupInterestCategories() {
        LinearLayout interestsContainer = findViewById(R.id.interests_container);
        
        // For each category, create a section title and chip group
        for (Map.Entry<String, List<String>> category : interestCategories.entrySet()) {
            // Add category title
            TextView categoryTitle = new TextView(this);
            categoryTitle.setText(category.getKey());
            categoryTitle.setTextSize(24);
            categoryTitle.setTextColor(getResources().getColor(R.color.white));
            categoryTitle.setPadding(0, 40, 0, 20);
            interestsContainer.addView(categoryTitle);
            
            // Add chips for this category
            ChipGroup chipGroup = new ChipGroup(this);
            chipGroup.setChipSpacingHorizontal(16);
            chipGroup.setChipSpacingVertical(16);
            
            for (String interest : category.getValue()) {
                Chip chip = new Chip(this);
                chip.setText(interest);
                chip.setCheckable(true);
                chip.setClickable(true);
                
                // Set up chip styling
                chip.setChipBackgroundColorResource(R.color.background_dark);
                chip.setTextColor(getResources().getColor(R.color.white));
                chip.setChipStrokeWidth(1);
                chip.setChipStrokeColorResource(R.color.white);
                
                // Set icon based on interest
                setChipIcon(chip, interest);
                
                // Use stored color for this interest
                chip.setTag(interestColors.get(interest));
                
                // Set chip click listener
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (selectedInterests.size() >= MAX_SELECTIONS) {
                            chip.setChecked(false);
                            Toast.makeText(InterestsActivity.this, 
                                "You can select up to " + MAX_SELECTIONS + " interests", 
                                Toast.LENGTH_SHORT).show();
                        } else {
                            selectedInterests.add(interest);
                            chip.setChipBackgroundColorResource(interestColors.get(interest));
                            chip.setTextColor(getResources().getColor(R.color.black));
                            chip.setElevation(2f);
                            chip.setChipCornerRadius(16f);
                        }
                    } else {
                        selectedInterests.remove(interest);
                        chip.setChipBackgroundColorResource(R.color.background_dark);
                        chip.setTextColor(getResources().getColor(R.color.white));
                        chip.setElevation(0f);
                        chip.setChipCornerRadius(8f);
                    }
                    updateSelectedCount();
                });
                
                // Set long click listener to unselect interests
                chip.setOnLongClickListener(v -> {
                    if (selectedInterests.contains(interest)) {
                        // Add a visual effect for feedback
                        Animation shake = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
                        shake.setDuration(300);
                        chip.startAnimation(shake);
                        
                        // Wait for animation to complete then unselect
                        new Handler().postDelayed(() -> {
                            chip.setChecked(false);
                            Toast.makeText(InterestsActivity.this, 
                                "'" + interest + "' removed", Toast.LENGTH_SHORT).show();
                        }, 300);
                        
                        return true;
                    }
                    return false;
                });
                
                chipGroup.addView(chip);
            }
            
            interestsContainer.addView(chipGroup);
        }
        
        // After setupInterestCategories method completes, show a tooltip explaining how to unselect
        Toast.makeText(this, "Tip: Long-press a selected interest to remove it", Toast.LENGTH_LONG).show();
    }
    
    private void setChipIcon(Chip chip, String interest) {
        // Set appropriate icon for each interest
        // This would ideally use a mapping or switch statement to set the correct drawable
        // For simplicity, using placeholder approach
        int iconResId = getInterestIconResource(interest);
        if (iconResId != 0) {
            chip.setChipIconResource(iconResId);
            chip.setChipIconVisible(true);
        }
    }
    
    private int getInterestIconResource(String interest) {
        // Map common interest names to drawable resources
        String lowerInterest = interest.toLowerCase();
        
        // Specific interest icons
        if (lowerInterest.equals("art")) return R.drawable.ic_art;
        if (lowerInterest.equals("dancing")) return R.drawable.ic_dancing;
        if (lowerInterest.equals("photography")) return R.drawable.ic_photography;
        if (lowerInterest.equals("singing")) return R.drawable.ic_music;
        
        // Category-based icons for other interests
        if (Arrays.asList("make-up", "design", "crafts", "fashion", "video", "cosplay").contains(lowerInterest)) {
            return R.drawable.ic_creativity;
        }
        
        if (Arrays.asList("badminton", "bouldering", "crew", "baseball", "bowling", "cricket", "basketball", "boxing", "cycling").contains(lowerInterest)) {
            return R.drawable.ic_sports;
        }
        
        if (Arrays.asList("amphibians", "cats", "horses", "arthropods", "dogs", "rabbits", "birds", "fish", "reptiles", "turtles").contains(lowerInterest)) {
            return R.drawable.ic_pets;
        }
        
        return R.drawable.ic_favorite; // Default icon
    }
    
    private void updateSelectedCount() {
        selectedCountText.setText("Interests (" + selectedInterests.size() + "/" + MAX_SELECTIONS + ")");
        saveButton.setEnabled(!selectedInterests.isEmpty());
    }
    
    private void loadExistingInterests() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("interests")) {
                        List<String> userInterests = (List<String>) documentSnapshot.get("interests");
                        if (userInterests != null) {
                            selectedInterests = new ArrayList<>(userInterests);
                            updateChipSelections();
                            updateSelectedCount();
                        }
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to load interests: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show());
        }
    }
    
    private void updateChipSelections() {
        // Find all chip groups and update chip states based on loaded interests
        for (String category : interestCategories.keySet()) {
            ChipGroup chipGroup = findChipGroupForCategory(category);
            if (chipGroup != null) {
                for (int i = 0; i < chipGroup.getChildCount(); i++) {
                    View child = chipGroup.getChildAt(i);
                    if (child instanceof Chip) {
                        Chip chip = (Chip) child;
                        String interestName = chip.getText().toString();
                        boolean isSelected = selectedInterests.contains(interestName);
                        chip.setChecked(isSelected);
                        
                        // Update background color based on selection state
                        if (isSelected) {
                            chip.setChipBackgroundColorResource(interestColors.get(interestName));
                            chip.setTextColor(getResources().getColor(R.color.black));
                            chip.setElevation(2f);
                            chip.setChipCornerRadius(16f);
                        } else {
                            chip.setChipBackgroundColorResource(R.color.background_dark);
                            chip.setTextColor(getResources().getColor(R.color.white));
                            chip.setElevation(0f);
                            chip.setChipCornerRadius(8f);
                        }
                    }
                }
            }
        }
    }
    
    private ChipGroup findChipGroupForCategory(String category) {
        LinearLayout container = findViewById(R.id.interests_container);
        boolean foundCategory = false;
        
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            
            if (child instanceof TextView && ((TextView) child).getText().equals(category)) {
                foundCategory = true;
            } else if (foundCategory && child instanceof ChipGroup) {
                return (ChipGroup) child;
            }
        }
        
        return null;
    }
    
    private void saveInterests() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                .update("interests", selectedInterests)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(InterestsActivity.this, 
                        "Interests saved successfully", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Set result so profile fragment knows to refresh
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(InterestsActivity.this, 
                        "Failed to save interests: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Assigns consistent colors to interests based on their category
     */
    private int getInterestCategoryColor(String interest) {
        String lowerInterest = interest.toLowerCase();
        
        // Creativity category (orange/yellow)
        if (Arrays.asList("art", "design", "photography", "crafts", "fashion", "singing", "dancing", "video", "cosplay", "make-up").contains(lowerInterest)) {
            if (lowerInterest.equals("art")) return R.color.pastel_orange;
            if (lowerInterest.equals("dancing")) return R.color.pastel_yellow;
            if (lowerInterest.equals("photography")) return R.color.pastel_orange;
            if (lowerInterest.equals("singing")) return R.color.pastel_yellow;
            return R.color.pastel_orange;
        }
        
        // Sports category (blue/green)
        if (Arrays.asList("badminton", "bouldering", "crew", "baseball", "bowling", "cricket", "basketball", "boxing", "cycling").contains(lowerInterest)) {
            if (lowerInterest.equals("basketball")) return R.color.pastel_blue;
            if (lowerInterest.equals("cycling")) return R.color.pastel_green;
            if (lowerInterest.equals("baseball")) return R.color.pastel_blue;
            return R.color.pastel_green;
        }
        
        // Pets category (purple/pink)
        if (Arrays.asList("amphibians", "cats", "horses", "arthropods", "dogs", "rabbits", "birds", "fish", "reptiles", "turtles").contains(lowerInterest)) {
            if (lowerInterest.equals("cats")) return R.color.pastel_purple;
            if (lowerInterest.equals("dogs")) return R.color.pastel_pink;
            if (lowerInterest.equals("birds")) return R.color.pastel_purple;
            return R.color.pastel_pink;
        }
        
        // Default colors based on first letter for any other interest
        char firstChar = lowerInterest.charAt(0);
        switch (firstChar % 8) {
            case 0: return R.color.pastel_blue;
            case 1: return R.color.pastel_green;
            case 2: return R.color.pastel_purple;
            case 3: return R.color.pastel_pink;
            case 4: return R.color.pastel_orange;
            case 5: return R.color.pastel_yellow;
            case 6: return R.color.pastel_teal;
            case 7: return R.color.pastel_cyan;
            default: return R.color.pastel_blue;
        }
    }

    // Add a method to show a help dialog explaining features
    private void showHelpDialog() {
        new AlertDialog.Builder(this)
            .setTitle("How to Use")
            .setMessage("• Tap an interest to select it\n• Long-press a selected interest to remove it\n• You can select up to " + MAX_SELECTIONS + " interests\n• Selected interests will appear on your profile")
            .setPositiveButton("Got it", null)
            .show();
    }
} 