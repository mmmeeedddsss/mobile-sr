package com.senior_project.group_1.mobilesr;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;


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
