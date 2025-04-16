package com.pineapple.capture.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
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
import java.util.List;
import java.util.Locale;

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
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::loadFeedPosts);
        
        // Set colors for the refresh animation
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_blue,
            R.color.accent,
            R.color.primary_text
        );

        // Load feed posts
        loadFeedPosts();

        return root;
    }

    private void loadFeedPosts() {
        swipeRefreshLayout.setRefreshing(true);
        
        FirebaseFirestore.getInstance()
                .collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    swipeRefreshLayout.setRefreshing(false);
                    
                    if (error != null) {
                        Toast.makeText(requireContext(), "Error loading posts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    feedItems.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            FeedItem post = doc.toObject(FeedItem.class);
                            post.setId(doc.getId());
                            feedItems.add(post);
                        }
                        
                        if (feedItems.isEmpty()) {
                            // Show empty state view
                            binding.emptyStateLayout.setVisibility(View.VISIBLE);
                            feedRecyclerView.setVisibility(View.GONE);
                        } else {
                            // Show the posts
                            binding.emptyStateLayout.setVisibility(View.GONE);
                            feedRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                    feedAdapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
        private final List<FeedItem> feedItems;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());

        public FeedAdapter(List<FeedItem> feedItems) {
            this.feedItems = feedItems;
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
                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions = requestOptions.transforms(new CenterCrop(), new RoundedCorners(16));
                    
                    Glide.with(itemView.getContext())
                            .load(post.getImageUrl())
                            .apply(requestOptions)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .into(postImage);
                } else {
                    postImage.setVisibility(View.GONE);
                }
            }
        }
    }
}
