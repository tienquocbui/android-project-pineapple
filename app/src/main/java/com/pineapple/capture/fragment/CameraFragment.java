package com.pineapple.capture.fragment;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.Manifest;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pineapple.capture.MainActivity;
import com.pineapple.capture.R;
import com.pineapple.capture.feed.FeedItem;
import com.pineapple.capture.utils.CloudinaryManager;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.callback.ErrorInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.Map;

public class CameraFragment extends Fragment {

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private boolean flashEnabled = false;
    private ImageButton toggleFlashButton;
    private boolean isUsingFrontCamera = false;
    private File capturedImageFile;
    private ImageView capturedImageView;

    private TextInputEditText captionInput;
    private Button postButton;
    private Button cancelButton;
    private View buttonContainer;
    private com.google.android.material.textfield.TextInputLayout captionLayout;

    private static final int MAX_WORD_COUNT = 50;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        previewView = view.findViewById(R.id.previewView);
        captionInput = view.findViewById(R.id.caption_input);
        postButton = view.findViewById(R.id.post_button);
        capturedImageView = view.findViewById(R.id.captured_image_view);
        buttonContainer = view.findViewById(R.id.button_container);
        cancelButton = view.findViewById(R.id.cancel_button);
        captionLayout = view.findViewById(R.id.caption_layout);

        // Initially hide the post controls
        buttonContainer.setVisibility(View.GONE);
        captionLayout.setVisibility(View.GONE);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }

        ImageButton switchCameraButton = view.findViewById(R.id.switch_camera_button);
        switchCameraButton.setOnClickListener(v -> {
            isUsingFrontCamera = !isUsingFrontCamera;
            startCamera();
        });

        cancelButton.setOnClickListener(v -> {
            if (capturedImageFile != null) {
                resetCameraState();
            }
        });

        ImageButton captureButton = view.findViewById(R.id.capture_button);
        captureButton.setOnClickListener(v -> captureImage());

        toggleFlashButton = view.findViewById(R.id.toggle_flash);
        toggleFlashButton.setOnClickListener(v -> {
            flashEnabled = !flashEnabled;
            updateFlashIconColor();
            startCamera();
        });

        postButton.setOnClickListener(v -> postToFeed());

        // Add text watcher to limit caption to 50 words
        captionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Count words
                String input = s.toString().trim();
                if (input.isEmpty()) return;
                
                String[] words = input.split("\\s+");
                if (words.length > MAX_WORD_COUNT) {
                    // Truncate to max word count
                    StringBuilder truncated = new StringBuilder();
                    for (int i = 0; i < MAX_WORD_COUNT; i++) {
                        truncated.append(words[i]).append(" ");
                    }
                    captionInput.setText(truncated.toString().trim());
                    captionInput.setSelection(truncated.toString().trim().length());
                    
                    Toast.makeText(requireContext(), "Caption limited to 50 words", Toast.LENGTH_SHORT).show();
                }
                
                // Update caption counter if needed
                captionLayout.setHelperText(words.length + "/" + MAX_WORD_COUNT + " words");
            }
        });

        startCamera();

        return view;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(isUsingFrontCamera
                                ? CameraSelector.LENS_FACING_FRONT
                                : CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Configure capture with square aspect ratio
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetAspectRatio(1) // Set a 1:1 aspect ratio for square photos
                        .setFlashMode(flashEnabled ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(), cameraSelector, preview, imageCapture
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Failed to start camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void updateFlashIconColor() {
        int color = flashEnabled
                ? ContextCompat.getColor(requireContext(), android.R.color.white)
                : ContextCompat.getColor(requireContext(), R.color.primary_blue);

        toggleFlashButton.setColorFilter(color);
    }

    private void resetCameraState() {
        capturedImageFile = null;

        // Hide post controls
        buttonContainer.setVisibility(View.GONE);
        captionLayout.setVisibility(View.GONE);

        // Show camera controls
        capturedImageView.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        findViewById(R.id.capture_button).setVisibility(View.VISIBLE);
        findViewById(R.id.switch_camera_button).setVisibility(View.VISIBLE);
        findViewById(R.id.toggle_flash).setVisibility(View.VISIBLE);

        startCamera();
    }

    private void captureImage() {
        if (imageCapture == null) return;

        capturedImageFile = new File(requireContext().getExternalFilesDir(null),
                new SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                        .format(new Date()) + ".jpg");

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(capturedImageFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Log.d("CameraX", "Image saved: " + capturedImageFile.getAbsolutePath());

                        previewView.setVisibility(View.GONE);
                        capturedImageView.setVisibility(View.VISIBLE);
                        
                        // Hide camera controls when showing captured image
                        findViewById(R.id.capture_button).setVisibility(View.GONE);
                        findViewById(R.id.switch_camera_button).setVisibility(View.GONE);
                        findViewById(R.id.toggle_flash).setVisibility(View.GONE);

                        // Try to keep the true color of the picture
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                        // Handle rotating picture
                        Bitmap bitmap = BitmapFactory.decodeFile(capturedImageFile.getAbsolutePath());
                        try {
                            ExifInterface exif = new ExifInterface(capturedImageFile.getAbsolutePath());
                            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            int rotationInDegrees = 0;
                            switch (orientation) {
                                case ExifInterface.ORIENTATION_ROTATE_90:
                                    rotationInDegrees = 90;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_180:
                                    rotationInDegrees = 180;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_270:
                                    rotationInDegrees = 270;
                                    break;
                            }
                            if (rotationInDegrees != 0) {
                                Matrix matrix = new Matrix();
                                matrix.postRotate(rotationInDegrees);
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        capturedImageView.setImageBitmap(bitmap);

                        // Show post controls
                        buttonContainer.setVisibility(View.VISIBLE);
                        captionLayout.setVisibility(View.VISIBLE);
                        
                        // Clear any previous caption
                        captionInput.setText("");
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Failed to capture image", exception);
                        Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void postToFeed() {
        if (capturedImageFile == null) {
            Toast.makeText(requireContext(), "Please capture an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        String caption = captionInput.getText().toString().trim();

        // Update UI to show loading state
        postButton.setEnabled(false);
        cancelButton.setEnabled(false);
        postButton.setText("Posting...");

        CloudinaryManager.init(requireContext());

        CloudinaryManager.uploadImage(Uri.fromFile(capturedImageFile), new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                // Show loading indicator or message
                Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                // Update progress if needed
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                // Use the new helper method to get a clean image URL
                String imageUrl = CloudinaryManager.getImageUrl(resultData);
                Log.d("CameraFragment", "Cloudinary upload successful, image URL: " + imageUrl);
                savePostToFirestore(imageUrl, caption);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                postButton.setEnabled(true);
                postButton.setText("Post");
                Toast.makeText(requireContext(), "Error uploading image: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                Toast.makeText(requireContext(), "Upload rescheduled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePostToFirestore(String imageUrl, String content) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.e("CameraFragment", "Cannot save post with empty imageUrl");
            Toast.makeText(requireContext(), "Error: No image URL received", Toast.LENGTH_SHORT).show();
            postButton.setEnabled(true);
            postButton.setText("Post");
            return;
        }
        
        // Get current user
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("CameraFragment", "Creating post for user: " + userId);
        
        // Get user profile data
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                    String username = documentSnapshot.getString("username");
                    
                    if (profilePictureUrl == null) {
                        profilePictureUrl = ""; // Default empty string if no profile picture
                    }
                    
                    if (username == null || username.isEmpty()) {
                        username = "Anonymous"; // Default username if not set
                    }
                    
                    // Log data for debugging
                    Log.d("CameraFragment", "Saving post with data:");
                    Log.d("CameraFragment", "  userId: " + userId);
                    Log.d("CameraFragment", "  content: " + content);
                    Log.d("CameraFragment", "  imageUrl: " + imageUrl);
                    Log.d("CameraFragment", "  username: " + username);
                    Log.d("CameraFragment", "  profilePictureUrl: " + profilePictureUrl);
                    
                    // Create a manual map of post data
                    Map<String, Object> postData = new HashMap<>();
                    postData.put("userId", userId);
                    postData.put("content", content);
                    postData.put("imageUrl", imageUrl);
                    postData.put("timestamp", com.google.firebase.Timestamp.now());
                    postData.put("likes", 0);
                    postData.put("profilePictureUrl", profilePictureUrl);
                    postData.put("username", username);
                    
                    // Save directly with map to ensure all fields are included
                    FirebaseFirestore.getInstance().collection("posts")
                        .add(postData)
                        .addOnSuccessListener(documentReference -> {
                            // Success - post created and image uploaded
                            String postId = documentReference.getId();
                            Log.d("CameraFragment", "Post saved with ID: " + postId);
                            Toast.makeText(requireContext(), "Post uploaded successfully!", Toast.LENGTH_SHORT).show();
                            
                            // For testing: verify the post was saved by reading it back
                            FirebaseFirestore.getInstance().collection("posts").document(postId)
                                .get()
                                .addOnSuccessListener(postSnapshot -> {
                                    if (postSnapshot.exists()) {
                                        Log.d("CameraFragment", "Verification - Post was saved successfully: " + postSnapshot.getData());
                                    } else {
                                        Log.w("CameraFragment", "Verification - Post was not found after saving");
                                    }
                                    
                                    // Reset UI regardless of verification result
                                    resetCameraAfterPost();
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("CameraFragment", "Verification - Failed to verify post save: " + e.getMessage());
                                    // Still reset UI as the post was saved
                                    resetCameraAfterPost();
                                });
                        })
                        .addOnFailureListener(e -> {
                            // Error saving post
                            Log.e("CameraFragment", "Error saving post: " + e.getMessage(), e);
                            postButton.setEnabled(true);
                            cancelButton.setEnabled(true);
                            postButton.setText("Post");
                            Toast.makeText(requireContext(), "Error saving post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                } else {
                    // User document doesn't exist
                    Log.e("CameraFragment", "User profile not found");
                    Toast.makeText(requireContext(), "Error: User profile not found", Toast.LENGTH_SHORT).show();
                    postButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    postButton.setText("Post");
                }
            })
            .addOnFailureListener(e -> {
                // Error fetching user profile
                Log.e("CameraFragment", "Error fetching user profile: " + e.getMessage());
                postButton.setEnabled(true);
                cancelButton.setEnabled(true);
                postButton.setText("Post");
                Toast.makeText(requireContext(), "Error fetching user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private View findViewById(int id) {
        return requireView().findViewById(id);
    }
    
    // Reset entire camera state after a successful post
    private void resetCameraAfterPost() {
        capturedImageFile = null;
        
        // Hide post controls
        buttonContainer.setVisibility(View.GONE);
        captionLayout.setVisibility(View.GONE);
        
        // Show camera preview and controls
        capturedImageView.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        findViewById(R.id.capture_button).setVisibility(View.VISIBLE);
        findViewById(R.id.switch_camera_button).setVisibility(View.VISIBLE);
        findViewById(R.id.toggle_flash).setVisibility(View.VISIBLE);
        
        // Reset button states
        postButton.setEnabled(true);
        cancelButton.setEnabled(true);
        postButton.setText("Post");
        
        // Return to the home tab with the new post
        Log.d("CameraFragment", "Post successful, returning to home tab");
        if (getActivity() != null) {
            try {
                // 1. Load the HomeFragment
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
                    
                // 2. Select the home tab in the bottom navigation
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.navigation_home);
                }
            } catch (Exception e) {
                Log.e("CameraFragment", "Error navigating back to home tab: " + e.getMessage(), e);
                // Just finish as fallback
                getActivity().finish();
            }
        }
    }
}