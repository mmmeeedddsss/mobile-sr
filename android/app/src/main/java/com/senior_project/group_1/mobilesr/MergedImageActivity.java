package com.senior_project.group_1.mobilesr;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

public class MergedImageActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.merge_image_activity);

        //Getting the grid view and setting an adapter to it
        GridView grid = (GridView) findViewById(R.id.merge_image_view);
        grid.setAdapter(new ImageAdapter(this, PickPhotoActivity.chunkImages));
        grid.setNumColumns((int) Math.sqrt(PickPhotoActivity.chunkImages.size()));
    }
}
