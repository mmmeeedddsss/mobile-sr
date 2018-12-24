package com.senior_project.group_1.mobilesr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class PreprocessAndEnhanceActivity extends AppCompatActivity {

    public static Bitmap bitmap;
    private ZoomableImageView imageView;
    private Button rotateButton;
    private Button processButton;
    private Button splitButton;
    private BitmapProcessor bitmapProcessor;
    private Uri mImageUri;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        // TODO: fix the helper to use this instance of BitmapProcessor
        bitmapProcessor = null; // new TFLiteSuperResolver(this, ApplicationConstants.BATCH_SIZE);
        //Get the view From pick_photo_activity
        setContentView(R.layout.pick_photo_activity);

        imageView = findViewById(R.id.pick_photo_image_view);

        rotateButton = findViewById(R.id.rotate_image_button);
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.rotate();
            }
        });

        processButton = findViewById(R.id.process_image_button);
        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImageProcessing();
            }
        });

        splitButton = findViewById(R.id.split_image_button);
        splitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                ImageProcessingTask.divideImage(bitmap);
                Intent splitImageIntent = new Intent(PreprocessAndEnhanceActivity.this, DivideImageActivity.class);
                startActivity(splitImageIntent);
                */
            }
        });

        // Set content of Zoomable image view
        Intent intent = getIntent();
        mImageUri = intent.getParcelableExtra("imageUri");
        setImage();
    }

    public void startImageProcessing() {
        if (bitmap != null) {
            Log.i("processImage", String.format("Bitmap size: %d %d", bitmap.getWidth(), bitmap.getHeight()));

            SRModelConfiguration modelConfiguration = SRModelConfigurationManager.getConfiguration("SRCNN_NR_128");

            Bitmap inputBitmap = imageView.getCurrentBitmap();
            new ImageProcessingTask(this, modelConfiguration).execute(inputBitmap);
        }
    }

    // called by asynctask
    public void endImageProcessing(Bitmap outputBitmap) {
        imageView.setImageBitmap(outputBitmap);
    }

    protected void setImage() {
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageUri);
            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {
            Log.e("PreprocessAndEnhanceActivity.onActivityResult", "Error while loading image bitmap from URI", ex);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bitmapProcessor != null)
            bitmapProcessor.close();
    }
}
