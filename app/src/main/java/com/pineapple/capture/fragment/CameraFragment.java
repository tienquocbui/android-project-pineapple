package com.pineapple.capture.fragment;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
//import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.pineapple.capture.R;
//import com.pineapple.capture.feed.FeedItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class CameraFragment extends Fragment {

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private boolean flashEnabled = false;
    private ImageButton toggleFlashButton;
    private boolean isUsingFrontCamera = false;
    private File capturedImageFile;
    private TextInputEditText captionInput;
    private Button postButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        previewView = view.findViewById(R.id.previewView);
        captionInput = view.findViewById(R.id.caption_input);
        postButton = view.findViewById(R.id.post_button);
        postButton.setVisibility(View.GONE);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }

        ImageButton switchCameraButton = view.findViewById(R.id.switch_camera_button);
        switchCameraButton.setOnClickListener(v -> {
            isUsingFrontCamera = !isUsingFrontCamera;
            startCamera();
        });

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            if (capturedImageFile != null) {
                capturedImageFile = null;
                postButton.setVisibility(View.GONE);
                captionInput.setVisibility(View.GONE);
                startCamera();
            } else {
                requireActivity().onBackPressed();
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

//        postButton.setOnClickListener(v -> postToFeed());

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

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
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
                        postButton.setVisibility(View.VISIBLE);
                        captionInput.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Failed to capture image", exception);
                        Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

//    private void postToFeed() {
//        if (capturedImageFile == null) return;
//
//        String caption = captionInput.getText() != null ? captionInput.getText().toString() : "";
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//        // Upload image to Firebase Storage
//        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
//        String imageName = "posts/" + userId + "/" + capturedImageFile.getName();
//        StorageReference imageRef = storageRef.child(imageName);
//
//        imageRef.putFile(android.net.Uri.fromFile(capturedImageFile))
//                .addOnSuccessListener(taskSnapshot -> {
//                    // Get the download URL
//                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
//                        // Create FeedItem and save to Firestore
//                        FeedItem feedItem = new FeedItem(userId, caption, uri.toString());
//                        FirebaseFirestore.getInstance().collection("posts")
//                                .add(feedItem)
//                                .addOnSuccessListener(documentReference -> {
//                                    Toast.makeText(requireContext(), "Posted successfully!", Toast.LENGTH_SHORT).show();
//                                    NavHostFragment.findNavController(this).navigate(R.id.action_cameraFragment_to_homeFragment);
//                                })
//                                .addOnFailureListener(e -> {
//                                    Toast.makeText(requireContext(), "Failed to post", Toast.LENGTH_SHORT).show();
//                                    Log.e("CameraFragment", "Error posting to feed", e);
//                                });
//                    });
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
//                    Log.e("CameraFragment", "Error uploading image", e);
//                });
    }