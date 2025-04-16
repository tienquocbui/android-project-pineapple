package com.pineapple.capture.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize ProfileViewModel
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Initialize feed items list and adapter
        feedItems = new ArrayList<>();
        feedAdapter = new FeedAdapter(feedItems);
        
        // Set up RecyclerView
        feedRecyclerView = root.findViewById(R.id.feed_recycler_view);
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        feedRecyclerView.setAdapter(feedAdapter);
        
        // Log initialization
        Log.d("HomeFragment", "HomeFragment created and RecyclerView initialized");
        
        // Add test button for creating a test post
        Button testButton = new Button(requireContext());
        testButton.setText("Create Test Post");
        testButton.setOnClickListener(v -> createTestPost());
        ((ViewGroup)root).addView(testButton);
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_layout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> loadFeedPosts());
            
            // Set colors for the refresh animation
            swipeRefreshLayout.setColorSchemeResources(
                R.color.primary_blue,
                R.color.accent
            );
        }

        // Load feed posts
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
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        
        Log.d("HomeFragment", "Loading feed posts...");
        
        // Check if we're attached to a context
        if (!isAdded()) {
            Log.e("HomeFragment", "Fragment not attached to context, can't load posts");
            return;
        }
        
        // Clear the current items
        feedItems.clear();
        feedAdapter.notifyDataSetChanged();
        
        // First, log the number of posts in the collection for diagnostic purposes
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts").get()
            .addOnSuccessListener(snapshot -> {
                Log.d("HomeFragment", "DIAGNOSTIC: Total posts in collection: " + snapshot.size());
                
                // List all post IDs and content for debugging
                for (QueryDocumentSnapshot doc : snapshot) {
                    Log.d("HomeFragment", "DIAGNOSTIC: Post ID: " + doc.getId() + 
                          ", Content: " + doc.getString("content") + 
                          ", ImageURL: " + doc.getString("imageUrl") +
                          ", Username: " + doc.getString("username"));
                }
                
                // If we have no posts, or there's an issue with the posts, show a toast
                if (snapshot.isEmpty()) {
                    Toast.makeText(requireContext(), "No posts found. Please create a post.", Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("HomeFragment", "DIAGNOSTIC: Error checking posts collection: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Error loading posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
                
                // Handle the error and retry in case of network issues
                if (e.getMessage() != null && e.getMessage().contains("UNAVAILABLE")) {
                    Log.d("HomeFragment", "Network appears to be unavailable, will retry");
                    // Retry after a delay
                    new android.os.Handler().postDelayed(this::loadFeedPosts, 3000);
                }
            });
        
        // Now proceed with normal loading
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()  // Using get() instead of addSnapshotListener for a one-time load
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                
                Log.d("HomeFragment", "Firestore query successful, got " + queryDocumentSnapshots.size() + " documents");
                
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d("HomeFragment", "No posts found in Firestore");
                    if (binding != null) {
                        binding.emptyStateLayout.setVisibility(View.VISIBLE);
                        feedRecyclerView.setVisibility(View.GONE);
                    }
                    return;
                }
                
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    try {
                        // Log the raw data for debugging
                        Log.d("HomeFragment", "Document ID: " + doc.getId());
                        Log.d("HomeFragment", "Document data: " + doc.getData());
                        
                        // Extract fields manually to ensure all data is captured
                        String id = doc.getId();
                        String userId = doc.getString("userId");
                        String content = doc.getString("content");
                        String imageUrl = doc.getString("imageUrl");
                        String profilePictureUrl = doc.getString("profilePictureUrl");
                        String username = doc.getString("username");
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        
                        // Skip posts with missing image URLs
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            Log.w("HomeFragment", "Skipping post with empty imageUrl: " + id);
                            continue;
                        }
                        
                        // Log individual fields for verification
                        Log.d("HomeFragment", "Parsed fields: userId=" + userId + ", imageUrl=" + imageUrl + 
                              ", username=" + username + ", content=" + content);
                        
                        // Create a new FeedItem
                        FeedItem post = new FeedItem();
                        post.setId(id);
                        post.setUserId(userId);
                        post.setContent(content);
                        post.setImageUrl(imageUrl);
                        post.setProfilePictureUrl(profilePictureUrl);
                        post.setUsername(username);
                        
                        // Set timestamp or use current time as fallback
                        if (timestamp != null) {
                            post.setTimestamp(timestamp);
                        } else {
                            Log.w("HomeFragment", "Timestamp is null for post " + id);
                            post.setTimestamp(Timestamp.now());
                        }
                        
                        // Get and set likes (default to 0 if not present)
                        Long likesLong = doc.getLong("likes");
                        int likes = (likesLong != null) ? likesLong.intValue() : 0;
                        post.setLikes(likes);
                        
                        // Add the post to our list
                        feedItems.add(post);
                        
                        // Force notify after each addition for immediate display
                        feedAdapter.notifyItemInserted(feedItems.size() - 1);
                        
                        Log.d("HomeFragment", "Added post to feed: " + post);
                        
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error parsing post: " + e.getMessage(), e);
                    }
                }
                
                Log.d("HomeFragment", "Processed " + feedItems.size() + " posts");
                
                if (feedItems.isEmpty() && binding != null) {
                    // Show empty state view
                    Log.d("HomeFragment", "No valid posts found, showing empty state");
                    binding.emptyStateLayout.setVisibility(View.VISIBLE);
                    feedRecyclerView.setVisibility(View.GONE);
                } else if (binding != null) {
                    // Show the posts
                    Log.d("HomeFragment", "Showing " + feedItems.size() + " posts");
                    binding.emptyStateLayout.setVisibility(View.GONE);
                    feedRecyclerView.setVisibility(View.VISIBLE);
                    
                    // Notify adapter of changes
                    feedAdapter.notifyDataSetChanged();
                }
            })
            .addOnFailureListener(e -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                
                Log.e("HomeFragment", "Error loading posts: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Error loading posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                
                if (binding != null) {
                    binding.emptyStateLayout.setVisibility(View.VISIBLE);
                    feedRecyclerView.setVisibility(View.GONE);
                }
                
                // Handle the error and retry in case of network issues
                if (e.getMessage() != null && e.getMessage().contains("UNAVAILABLE")) {
                    Log.d("HomeFragment", "Network appears to be unavailable, will retry");
                    // Retry after a delay
                    new android.os.Handler().postDelayed(this::loadFeedPosts, 3000);
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
        FirebaseFirestore.getInstance().collection("posts")
            .add(postData)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(requireContext(), "Test post created with ID: " + documentReference.getId(), Toast.LENGTH_LONG).show();
                loadFeedPosts();  // Reload posts
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Error creating test post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("HomeFragment", "Error creating test post", e);
            });
    }

    private static class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
        private final List<FeedItem> feedItems;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());

        public FeedAdapter(List<FeedItem> feedItems) {
            this.feedItems = feedItems;
            setHasStableIds(true); // Improve recycler view performance
        }
        
        @Override
        public long getItemId(int position) {
            // Use the post ID as a long hash to ensure consistent item binding
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

            public FeedViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profile_image);
                usernameText = itemView.findViewById(R.id.username_text);
                captionText = itemView.findViewById(R.id.caption_text);
                postImage = itemView.findViewById(R.id.post_image);
                timestampText = itemView.findViewById(R.id.timestamp_text);
                likesText = itemView.findViewById(R.id.likes_text);
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
                        RequestOptions requestOptions = new RequestOptions()
                                .transforms(new CenterCrop(), new RoundedCorners(16))
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.error_image);
                        
                        String imageUrl = post.getImageUrl().trim();
                        Log.d("HomeFragment", "Loading image: " + imageUrl);
                        
                        // Ensure URL starts with http or https
                        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                            imageUrl = "https://" + imageUrl;
                        }
                        
                        // Create a final copy of the URL for use in the listener
                        final String finalImageUrl = imageUrl;
                        
                        postImage.setVisibility(View.VISIBLE);
                        
                        // Using more reliable image loading approach
                        Glide.with(itemView.getContext())
                                .load(finalImageUrl)
                                .apply(requestOptions)
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                                                              Target<Drawable> target, 
                                                              boolean isFirstResource) {
                                        Log.e("HomeFragment", "Failed to load image: " + finalImageUrl + 
                                              ", error: " + (e != null ? e.getMessage() : "unknown"));
                                        return false; // let Glide handle the error case
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, 
                                                                  Object model, 
                                                                  Target<Drawable> target, 
                                                                  DataSource dataSource, 
                                                                  boolean isFirstResource) {
                                        Log.d("HomeFragment", "Successfully loaded image: " + finalImageUrl);
                                        return false; // let Glide handle displaying the resource
                                    }
                                })
                                .into(postImage);
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error loading image: " + e.getMessage(), e);
                        postImage.setVisibility(View.VISIBLE);
                        Glide.with(itemView.getContext())
                                .load(R.drawable.error_image)
                                .into(postImage);
                    }
                } else {
                    Log.w("HomeFragment", "No image URL for post: " + post.getId());
                    postImage.setVisibility(View.GONE);
                }
            }
        }
    }
}
