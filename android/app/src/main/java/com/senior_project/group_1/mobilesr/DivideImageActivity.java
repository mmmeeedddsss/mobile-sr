package com.senior_project.group_1.mobilesr;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

public class DivideImageActivity extends Activity {
    private Button mergeButton;
    public static Bitmap bitmap;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.divide_image_activity);
        mergeButton = findViewById(R.id.buttonMergeImage);
        mergeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bitmap = PreprocessAndEnhanceActivity.reconstructImage(PreprocessAndEnhanceActivity.bitmap, 100, 90, 20, 30);
                Intent mergeImageIntent = new Intent(DivideImageActivity.this, MergeImageActivity.class);
                startActivity(mergeImageIntent);
            }
        });


        //Getting the grid view and setting an adapter to it
        GridView grid = findViewById(R.id.divided_image_grid_view);
        grid.setAdapter(new ImageAdapter(this, PreprocessAndEnhanceActivity.chunkImages));
        grid.setNumColumns(PreprocessAndEnhanceActivity.columns);
    }
}
