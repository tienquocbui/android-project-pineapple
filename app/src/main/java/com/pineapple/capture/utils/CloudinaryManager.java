package com.pineapple.capture.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {

    private static boolean isInitialized = false;

    public static void init(Context context) {
        if (!isInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "den0bpixy");
            config.put("api_key", "955574316579734");
            config.put("api_secret", "WfyFf4y1Wsnxzuf6pRC0KKwxNBI");
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
