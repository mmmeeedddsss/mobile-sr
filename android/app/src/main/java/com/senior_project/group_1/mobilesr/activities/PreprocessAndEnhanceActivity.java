package com.senior_project.group_1.mobilesr.activities;

import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
import com.senior_project.group_1.mobilesr.img_processing.BitmapProcessor;
import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.img_processing.ImageProcessingDialog;
import com.senior_project.group_1.mobilesr.img_processing.ImageProcessingTask;
import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;
import com.senior_project.group_1.mobilesr.views.ZoomableImageView;

public class PreprocessAndEnhanceActivity extends AppCompatActivity {

    private Bitmap bitmap;
    private boolean isPaused;
    private ZoomableImageView imageView;
    private Button rotateButton, processButton, processAllButton,
                   nextButton, prevButton;
    private Uri mImageUri;
    private ImageProcessingDialog dialog;
    private ImageProcessingTask imageProcessingTask;
    private ClipData imageClipData;
    private int imgIndex;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        isPaused = false;
        imgIndex = 0;

        //Get the view From pick_photo_activity
        setContentView(R.layout.pick_photo_activity);

        imageView = findViewById(R.id.pick_photo_image_view);

        rotateButton = findViewById(R.id.rotate_image_button);
        rotateButton.setOnClickListener(v -> imageView.rotate());

        processButton = findViewById(R.id.process_image_button);
        processButton.setOnClickListener(v -> processImage());

        nextButton = findViewById(R.id.next_image_button);
        nextButton.setOnClickListener(v -> nextImage());

        prevButton = findViewById(R.id.prev_image_button);
        prevButton.setOnClickListener(v -> prevImage());

        // Set content of Zoomable image view
        Intent intent = getIntent();
        imageClipData = intent.getParcelableExtra("imageClipData");

        refreshImage();
    }

    public void processImage() {
        if (bitmap != null) {
            Log.i("processImage", String.format("Bitmap size: %d %d", bitmap.getWidth(), bitmap.getHeight()));

            SRModelConfiguration modelConfiguration = SRModelConfigurationManager.getConfiguration("SRCNN_NR_128");

            Bitmap inputBitmap = imageView.getCurrentBitmap();
            dialog = new ImageProcessingDialog(this);
            dialog.show();
            imageProcessingTask = new ImageProcessingTask(this, dialog, modelConfiguration);
            imageProcessingTask.execute(inputBitmap);
        }
    }

    private void nextImage() {
        if(imgIndex < imageClipData.getItemCount() - 1) {
            imgIndex++;
            refreshImage();
        }
    }

    private void prevImage() {
        if(imgIndex > 0) {
            imgIndex--;
            refreshImage();
        }
    }

    // too many callbacks?!

    // called by imageprocessingdialog
    public void cancelImageProcessing() {
        if(BuildConfig.DEBUG && imageProcessingTask == null)
            throw new AssertionError();
        imageProcessingTask.cancel(true);
        dialog.dismiss();
        dialog = null;
    }

    // called by imageprocessingtask
    public void endImageProcessing(Bitmap outputBitmap) {
        imageView.setImageBitmap(outputBitmap);
        imageProcessingTask = null;
        dialog.dismiss();
        dialog = null;
    }

    private void refreshImage() {
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                    imageClipData.getItemAt(imgIndex).getUri());
            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {
            Log.e("PreprocessAndEnhanceActivity.onActivityResult", "Error while loading image bitmap from URI", ex);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MobileSR", "Preprocess & enhance activity paused");
        synchronized(this) {
            isPaused = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MobileSR", "Preprocess & enhance activity resumed");
        synchronized(this) {
            isPaused = false;
        }
    }

    // used by the image-processing asynchronous task to determine
    // whether to send a notification or not
    public boolean inBackground() {
        // TODO; access to isPaused COULD be concurrent in an extreme case,
        // but I am not sure if locking is really necessary or not
        synchronized(this) {
            return isPaused;
        }
    }
}
