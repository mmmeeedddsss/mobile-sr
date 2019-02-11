package com.senior_project.group_1.mobilesr.activities;

import android.app.Activity;
import android.os.Bundle;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.views.ZoomableImageView;


public class MergeImageActivity extends Activity {
    public ZoomableImageView imageView;
    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.merge_image_activity);
        imageView = findViewById(R.id.merged_image_view);
        imageView.setImageBitmap(DivideImageActivity.bitmap);
    }
}
