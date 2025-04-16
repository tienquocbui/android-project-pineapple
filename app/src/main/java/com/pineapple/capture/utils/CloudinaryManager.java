package com.pineapple.capture.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.pineapple.capture.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {

    private static boolean isInitialized = false;

    public static void init(Context context) {
        String cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME;
        String apiKey = BuildConfig.CLOUDINARY_API_KEY;
        String apiSecret = BuildConfig.CLOUDINARY_API_SECRET;


        if (!isInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", cloudName);
            config.put("api_key", apiKey);
            config.put("api_secret", apiSecret);
            MediaManager.init(context, config);
            isInitialized = true;
        }
    }

    public static void uploadImage(Uri fileUri, UploadCallback callback) {
        MediaManager.get().upload(fileUri)
                .option("upload_preset", "unsigned_feed_uploads")
                .callback(callback)
                .dispatch();
    }

}
