package com.senior_project.group_1.mobilesr;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public class PreprocessAndEnhanceActivity extends AppCompatActivity {

    public static Bitmap bitmap;
    private ZoomableImageView imageView;
    private Button rotateButton;
    private Button processButton;
    private Button splitButton;
    private BitmapProcessor bitmapProcessor;
    private Uri mImageUri;
    private ProgressBar mProgressbar;
    private Activity self;

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
                processImage();
            }
        });

        self = this;

        mProgressbar = findViewById(R.id.pbarImageProcessingProgress);
        mProgressbar.setMin(0);
        mProgressbar.setProgress(0);
        mProgressbar.setProgressTintList(ColorStateList.valueOf(Color.rgb(65, 242, 71)));

        // Set content of Zoomable image view
        Intent intent = getIntent();
        mImageUri = intent.getParcelableExtra("imageUri");
        setImage();
    }

    public void processImage() {
        if (bitmap != null) {
            Toast.makeText(getApplicationContext(), "Process Started", Toast.LENGTH_LONG).show();
            new Thread(new Runnable() {
                @Override
                public void run() {

                    long startTime = System.nanoTime();
                    Log.i("processImage", String.format("Bitmap size: %d %d", bitmap.getWidth(), bitmap.getHeight()));

                    SRModelConfiguration modelConfiguration = SRModelConfigurationManager.getCurrentConfiguration();

                    ImageProcessingHelper.divideImage(imageView.getCurrentBitmap());
                    ImageProcessingHelper.processImages(self, modelConfiguration, mProgressbar);
                    Bitmap processed_bitmap = ImageProcessingHelper.reconstructImage();

                    long estimatedTime = System.nanoTime() - startTime;
                    ImageProcessingHelper.writeToSDFile(
                            String.format(Locale.ENGLISH, "Time: %s | Conf: %s | dT: %d | I: %dx%d",
                                    Calendar.getInstance().getTime(),
                                    SRModelConfigurationManager.getCurrentConfiguration().toString(),
                                    estimatedTime,
                                    imageView.getCurrentBitmap().getWidth(),
                                    imageView.getCurrentBitmap().getHeight()));

                    //Toast.makeText(getApplicationContext(), "Elapsed Time in ms: " + estimatedTime / 1000000, Toast.LENGTH_LONG).show();
                    imageView.setImageBitmap(processed_bitmap);
                }
            }).start();
        }
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
