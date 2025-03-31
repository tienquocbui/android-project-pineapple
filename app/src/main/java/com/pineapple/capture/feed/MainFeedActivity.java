package com.pineapple.capture.feed;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.pineapple.capture.R;

public class MainFeedActivity extends AppCompatActivity {
    private MainFeedViewModel viewModel;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_feed);

        viewModel = new ViewModelProvider(this).get(MainFeedViewModel.class);
        
        recyclerView = findViewById(R.id.feed_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Observe feed updates
        viewModel.getFeedItems().observe(this, feedItems -> {
            // Update RecyclerView adapter with new items
        });
    }
} 