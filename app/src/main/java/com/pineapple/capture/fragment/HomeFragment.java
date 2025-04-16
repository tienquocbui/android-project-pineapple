package com.pineapple.capture.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.pineapple.capture.R;
import com.pineapple.capture.databinding.FragmentHomeBinding;
import com.pineapple.capture.feed.FeedItem;
import com.pineapple.capture.profile.ProfileViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private ProfileViewModel profileViewModel;
    private RecyclerView feedRecyclerView;
    private FeedAdapter feedAdapter;
    private List<FeedItem> feedItems;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyStateLayout;
    private FirebaseFirestore db;
    private Button createTestPostButton;
    private Button clearAllPostsButton;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize views
        feedRecyclerView = root.findViewById(R.id.feed_recycler_view);
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_layout);
        emptyStateLayout = root.findViewById(R.id.empty_state_layout);
        createTestPostButton = root.findViewById(R.id.create_test_post_button);
        clearAllPostsButton = root.findViewById(R.id.clear_all_posts_button);

        // Initialize data
        feedItems = new ArrayList<>();
        
        // Setup RecyclerView
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        feedAdapter = new FeedAdapter(feedItems);
        feedAdapter.setHasStableIds(true); // Improve RecyclerView performance
        feedRecyclerView.setAdapter(feedAdapter);
        
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        
        // Setup test post button
        if (createTestPostButton != null) {
            createTestPostButton.setOnClickListener(v -> createTestPost());
        }
        
        // Setup clear all posts button
        if (clearAllPostsButton != null) {
            clearAllPostsButton.setOnClickListener(v -> deleteAllPosts());
        }
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadFeedPosts);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_blue);
        
        // Load initial data
        loadFeedPosts();
        
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload posts when fragment becomes visible
        Log.d("HomeFragment", "onResume - reloading posts");
        loadFeedPosts();
    }

    public void loadFeedPosts() {
        if (getActivity() == null) return;
        
        Log.d("HomeFragment", "Loading feed posts...");
        swipeRefreshLayout.setRefreshing(true);


        // First, log the number of posts in the collection for diagnostic purposes
        db.collection("posts").get()
            .addOnSuccessListener(snapshot -> {
                int count = snapshot.size();
                Log.d("HomeFragment", "Found " + count + " posts in collection");
                
                if (count > 0) {
                    // Log each post for debugging
                    for (QueryDocumentSnapshot document : snapshot) {
                        Log.d("HomeFragment", "Post: id=" + document.getId() + 
                              ", content=" + document.getString("content") + 
                              ", imageUrl=" + document.getString("imageUrl") + 
                              ", username=" + document.getString("username"));
                    }
                }
            })
            .addOnFailureListener(e -> Log.e("HomeFragment", "Error checking post count", e));
        
        // Fetch posts sorted by timestamp
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d("HomeFragment", "Query returned " + queryDocumentSnapshots.size() + " documents");
                
                feedItems.clear();
                Log.d("HomeFragment", "Cleared previous feedItems. Size now: " + feedItems.size());
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        // Log raw document data
                        Log.d("HomeFragment", "Processing document: " + document.getId());
                        Log.d("HomeFragment", "Document data: " + document.getData());
                        
                        FeedItem item = document.toObject(FeedItem.class);
                        
                        // Check for null values
                        if (item == null) {
                            Log.e("HomeFragment", "Failed to convert document to FeedItem: " + document.getId());
                            continue;
                        }
                        
                        // Make sure item has an ID set
                        item.setId(document.getId());
                        
                        // Debug log all properties
                        Log.d("HomeFragment", "FeedItem: id=" + item.getId() + 
                              ", content=" + item.getContent() + 
                              ", imageUrl=" + item.getImageUrl() + 
                              ", username=" + item.getUsername() +
                              ", timestamp=" + (item.getTimestamp() != null ? item.getTimestamp().toDate() : "null"));
                        
                        // Add valid posts only
                        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                            feedItems.add(item);
                            Log.d("HomeFragment", "Added post to feedItems. New size: " + feedItems.size());
                        } else {
                            Log.w("HomeFragment", "Skipping post with empty image URL: " + item.getId());
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error processing document: " + document.getId(), e);
                    }
                }
                
                Log.d("HomeFragment", "Processed " + feedItems.size() + " posts");
                Log.d("HomeFragment", "Showing " + feedItems.size() + " posts");
                
                // Update UI
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d("HomeFragment", "Notifying adapter of data changes. Items count: " + feedItems.size());
                        feedAdapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                        updateEmptyState();
                        
                        // Check empty state
                        boolean isEmpty = feedItems == null || feedItems.isEmpty();
                        Log.d("HomeFragment", "Feed is empty: " + isEmpty);
                        Log.d("HomeFragment", "EmptyStateLayout visibility: " + 
                              (emptyStateLayout.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                        Log.d("HomeFragment", "RecyclerView visibility: " + 
                              (feedRecyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                    });
                }
            })
            .addOnFailureListener(e -> {
                String errorMsg = e.getMessage();
                Log.e("HomeFragment", "Error loading posts: " + errorMsg, e);
                
                // Check for network connectivity issues
                if (errorMsg != null && errorMsg.contains("UNAVAILABLE")) {
                    Log.w("HomeFragment", "Network appears to be unavailable, retrying in 3 seconds");
                    
                    // Retry after a delay
                    new Handler(Looper.getMainLooper()).postDelayed(
                        this::loadFeedPosts, 
                        3000
                    );
                } else {
                    // For other errors, just stop the refresh indicator
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(), "Error loading posts: " + errorMsg, Toast.LENGTH_SHORT).show();
                            updateEmptyState();
                        });
                    }
                }
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Test method to create a post directly
    private void createTestPost() {
        if (!isAdded()) return;
        
        Toast.makeText(requireContext(), "Creating test post...", Toast.LENGTH_SHORT).show();
        
        // Get current user
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Create test post data
        Map<String, Object> postData = new HashMap<>();
        postData.put("userId", userId);
        postData.put("content", "Test post created at " + new Date().toString());
        postData.put("imageUrl", "https://picsum.photos/600/400");  // Random image from Lorem Picsum
        postData.put("timestamp", Timestamp.now());
        postData.put("likes", 0);
        postData.put("profilePictureUrl", "");
        postData.put("username", "TestUser");
        
        // Save to Firestore
        db.collection("posts")
            .add(postData)
            .addOnSuccessListener(documentReference -> {
                String postId = documentReference.getId();
                Toast.makeText(requireContext(), "Test post created with ID: " + postId, Toast.LENGTH_LONG).show();
                
                // Verify post creation
                checkPostsData(postId);
                
                // Reload posts
                loadFeedPosts();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Error creating test post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("HomeFragment", "Error creating test post", e);
            });
    }

    // Debug method to check Firestore data
    private void checkPostsData(String specificPostId) {
        Log.d("HomeFragment", "DEBUGGING: Checking posts collection directly");
        
        // Fetch all posts
        db.collection("posts")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d("HomeFragment", "DEBUG: Total posts in Firestore: " + querySnapshot.size());
                
                // Look for the specific post
                boolean foundSpecificPost = false;
                
                for (DocumentSnapshot doc : querySnapshot) {
                    Map<String, Object> data = doc.getData();
                    
                    Log.d("HomeFragment", "DEBUG: Post " + doc.getId() + " data: " + data);
                    
                    // Check if this is the specific post we're looking for
                    if (doc.getId().equals(specificPostId)) {
                        foundSpecificPost = true;
                        Log.d("HomeFragment", "DEBUG: Found the specific post: " + specificPostId);
                        Log.d("HomeFragment", "DEBUG: Post data: " + data);
                        
                        // Verify all required fields
                        String imageUrl = (String) data.get("imageUrl");
                        String content = (String) data.get("content");
                        String username = (String) data.get("username");
                        Timestamp timestamp = (Timestamp) data.get("timestamp");
                        
                        Log.d("HomeFragment", "DEBUG: imageUrl = " + imageUrl);
                        Log.d("HomeFragment", "DEBUG: content = " + content);
                        Log.d("HomeFragment", "DEBUG: username = " + username);
                        Log.d("HomeFragment", "DEBUG: timestamp = " + (timestamp != null ? timestamp.toDate() : "null"));
                        
                        // Check if image URL is valid
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Log.d("HomeFragment", "DEBUG: Image URL is valid");
                        } else {
                            Log.e("HomeFragment", "DEBUG: Image URL is invalid: " + imageUrl);
                        }
                    }
                }
                
                if (!foundSpecificPost) {
                    Log.e("HomeFragment", "DEBUG: Couldn't find the specific post: " + specificPostId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("HomeFragment", "DEBUG: Error fetching posts", e);
            });
    }

    // Add new method to delete all posts
    private void deleteAllPosts() {
        swipeRefreshLayout.setRefreshing(true);
        
        // Show confirmation dialog
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete All Posts")
            .setMessage("Are you sure you want to delete all posts? This action cannot be undone.")
            .setPositiveButton("Delete All", (dialog, which) -> {
                Log.d("HomeFragment", "Deleting all posts...");
                
                db.collection("posts")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        // Create a batch to perform all deletions in one transaction
                        WriteBatch batch = db.batch();
                        final int[] count = {0}; // Use array to make it effectively final
                        
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            batch.delete(document.getReference());
                            count[0]++;
                        }
                        
                        if (count[0] > 0) {
                            // Execute the batch
                            batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("HomeFragment", "Successfully deleted " + count[0] + " posts");
                                    Toast.makeText(requireContext(), count[0] + " posts deleted", Toast.LENGTH_SHORT).show();
                                    
                                    // Clear local list and update UI
                                    feedItems.clear();
                                    feedAdapter.notifyDataSetChanged();
                                    updateEmptyState();
                                    swipeRefreshLayout.setRefreshing(false);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("HomeFragment", "Error deleting posts", e);
                                    Toast.makeText(requireContext(), "Error deleting posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    swipeRefreshLayout.setRefreshing(false);
                                });
                        } else {
                            Log.d("HomeFragment", "No posts to delete");
                            Toast.makeText(requireContext(), "No posts to delete", Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("HomeFragment", "Error getting posts to delete", e);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    });
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                swipeRefreshLayout.setRefreshing(false);
            })
            .show();
    }

    private static class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
        private final List<FeedItem> feedItems;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());

        public FeedAdapter(List<FeedItem> feedItems) {
            this.feedItems = feedItems;
            setHasStableIds(true); // Ensure stable IDs for better RecyclerView performance
        }
        
        @Override
        public long getItemId(int position) {
            // Use the post ID as the stable ID by hashing it
            FeedItem item = feedItems.get(position);
            return item.getId() != null ? item.getId().hashCode() : position;
        }

        @NonNull
        @Override
        public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_feed_post, parent, false);
            return new FeedViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
            FeedItem post = feedItems.get(position);
            holder.bind(post, dateFormat);
        }

        @Override
        public int getItemCount() {
            return feedItems.size();
        }

        static class FeedViewHolder extends RecyclerView.ViewHolder {
            private final ImageView profileImage;
            private final TextView usernameText;
            private final TextView captionText;
            private final ImageView postImage;
            private final TextView timestampText;
            private final TextView likesText;
            private final ImageButton likeButton, commentButton, shareButton;


            public FeedViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profile_image);
                usernameText = itemView.findViewById(R.id.username_text);
                captionText = itemView.findViewById(R.id.caption_text);
                postImage = itemView.findViewById(R.id.post_image);
                timestampText = itemView.findViewById(R.id.timestamp_text);
                likesText = itemView.findViewById(R.id.likes_text);
                commentButton = itemView.findViewById(R.id.comment_button);
                shareButton = itemView.findViewById(R.id.share_button);
                likeButton = itemView.findViewById(R.id.like_button);
            }

            public void bind(FeedItem post, SimpleDateFormat dateFormat) {
                // Set username
                usernameText.setText(post.getUsername() != null ? post.getUsername() : "Anonymous");
                
                // Set caption
                if (post.getContent() != null && !post.getContent().isEmpty()) {
                    captionText.setVisibility(View.VISIBLE);
                    captionText.setText(post.getContent());
                } else {
                    captionText.setVisibility(View.GONE);
                }
                
                // Set timestamp
                if (post.getTimestamp() != null) {
                    Timestamp timestamp = post.getTimestamp();
                    Date date = timestamp.toDate();
                    timestampText.setText(dateFormat.format(date));
                } else {
                    timestampText.setText("Just now");
                }
                
                // Set likes count
                likesText.setText(String.format(Locale.getDefault(), "%d likes", post.getLikes()));

                // Load profile image
                if (post.getProfilePictureUrl() != null && !post.getProfilePictureUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(post.getProfilePictureUrl())
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(profileImage);
                } else {
                    // Set default profile image
                    Glide.with(itemView.getContext())
                            .load(R.drawable.ic_person)
                            .circleCrop()
                            .into(profileImage);
                }

                // Load post image with rounded corners
                if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                    try {
                        String imageUrl = post.getImageUrl().trim();
                        Log.d("FeedViewHolder", "Loading post image: " + imageUrl + " for post: " + post.getId());
                        
                        // Ensure URL starts with http or https
                        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                            imageUrl = "https://" + imageUrl;
                            Log.d("FeedViewHolder", "Fixed image URL to: " + imageUrl);
                        }
                        
                        // Create a final copy of the URL for use in the listener
                        final String finalImageUrl = imageUrl;
                        
                        // Make sure postImage is visible
                        postImage.setVisibility(View.VISIBLE);
                        
                        // Set a colored background while loading
                        postImage.setBackgroundColor(itemView.getResources().getColor(R.color.system_gray));
                        
                        // Use more reliable RequestOptions
                        RequestOptions requestOptions = new RequestOptions()
                                .transforms(new CenterCrop(), new RoundedCorners(16))
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.error_image);
                                
                        // Using more reliable image loading approach
                        Glide.with(itemView.getContext())
                                .load(finalImageUrl)
                                .apply(requestOptions)
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                                                              Target<Drawable> target, 
                                                              boolean isFirstResource) {
                                        Log.e("FeedViewHolder", "Failed to load image: " + finalImageUrl + 
                                              ", error: " + (e != null ? e.getMessage() : "unknown"));
                                        
                                        // Try one more time with http if https failed
                                        if (finalImageUrl.startsWith("https://")) {
                                            String httpUrl = "http://" + finalImageUrl.substring(8);
                                            Log.d("FeedViewHolder", "Retrying with HTTP URL: " + httpUrl);
                                            
                                            Glide.with(itemView.getContext())
                                                .load(httpUrl)
                                                .apply(requestOptions)
                                                .into(postImage);
                                        }
                                        
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, 
                                                                  Object model, 
                                                                  Target<Drawable> target, 
                                                                  DataSource dataSource, 
                                                                  boolean isFirstResource) {
                                        Log.d("FeedViewHolder", "Successfully loaded image: " + finalImageUrl);
                                        // Clear the background when image loads
                                        postImage.setBackgroundColor(0);
                                        return false; // let Glide handle displaying the resource
                                    }
                                })
                                .into(postImage);
                    } catch (Exception e) {
                        Log.e("FeedViewHolder", "Error loading image: " + e.getMessage(), e);
                        postImage.setVisibility(View.VISIBLE);
                        Glide.with(itemView.getContext())
                                .load(R.drawable.error_image)
                                .into(postImage);
                    }
                } else {
                    Log.w("FeedViewHolder", "No image URL for post: " + post.getId());
                    postImage.setVisibility(View.GONE);
                }
            }
        }
    }

    private void updateEmptyState() {
        if (feedItems == null || feedItems.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            feedRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            feedRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
