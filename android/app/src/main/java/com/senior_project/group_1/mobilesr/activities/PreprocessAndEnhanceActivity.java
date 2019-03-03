package com.senior_project.group_1.mobilesr.activities;

import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import com.senior_project.group_1.mobilesr.views.BitmapHelpers;
import com.senior_project.group_1.mobilesr.views.ZoomableImageView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PreprocessAndEnhanceActivity extends AppCompatActivity {

    private Bitmap bitmap;
    private boolean isPaused;
    private ZoomableImageView imageView;
    private Button rotateButton, processButton, processAllButton,
                   nextButton, prevButton, toggleButton,
                   saveButton, shareButton;
    private Uri mImageUri;
    private ImageProcessingDialog dialog;
    private ImageProcessingTask imageProcessingTask;
    private ArrayList<Uri> imageUris; // TODO: wrap uri + processing state in a class?
    private ArrayList<Boolean> isProcessed;
    private int numImages;
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
        rotateButton.setEnabled(true);
        rotateButton.setOnClickListener(v -> imageView.rotate());

        processButton = findViewById(R.id.process_image_button);
        processButton.setOnClickListener(v -> {
            processImage();
            rotateButton.setEnabled(false);
        });

        processAllButton = findViewById(R.id.process_all_button);
        processAllButton.setOnClickListener(v -> processAllImages());

        nextButton = findViewById(R.id.next_image_button);
        nextButton.setOnClickListener(v -> nextImage());

        prevButton = findViewById(R.id.prev_image_button);
        prevButton.setOnClickListener(v -> prevImage());

        toggleButton = findViewById(R.id.toggle_sr_button);
        toggleButton.setOnClickListener(v -> imageView.toggleSrDrawal());

        shareButton = findViewById(R.id.share_image_button);
        shareButton.setOnClickListener(v -> {
            Uri savedImageUri = BitmapHelpers.saveImageExternal(imageView.getFullBitmap(), getApplicationContext());
            startActivity(Intent.createChooser(BitmapHelpers.createShareIntentByUri(savedImageUri),"Share Image"));
        });

        saveButton = findViewById(R.id.save_image_button);
        saveButton.setOnClickListener(v -> BitmapHelpers.saveImageExternal(imageView.getFullBitmap(), getApplicationContext()));

        Intent intent = getIntent();
        // fill image URIs
        ClipData imageClipData = intent.getParcelableExtra("imageClipData");
        imageUris = new ArrayList<>();
        isProcessed = new ArrayList<>();
        for(int i = 0, len = imageClipData.getItemCount(); i < len; ++i) {
            imageUris.add(imageClipData.getItemAt(i).getUri());
            isProcessed.add(false);
        }
        numImages = imageUris.size();

        // Set content of Zoomable image view
        refreshImage();
    }

    private void processImages(ArrayList<Uri> inputUris) {
        SRModelConfiguration modelConfiguration = SRModelConfigurationManager.getCurrentConfiguration();
        dialog = new ImageProcessingDialog(this);
        dialog.show();
        imageProcessingTask = new ImageProcessingTask(this, dialog, modelConfiguration);
        imageProcessingTask.execute(inputUris);
    }

    // TODO: reuse arraylists?
    public void processImage() {
        // add only the current image's URI
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(imageUris.get(imgIndex));
        processImages(uris);
    }

    private void processAllImages() {
        // add all URIs with the current view order
        ArrayList<Uri> uris = new ArrayList<>();
        int i = imgIndex;
        do {
            uris.add(imageUris.get(i));
            i = (i + 1) % numImages;
        } while(i != imgIndex);
        processImages(uris);
    }

    private void nextImage() {
        if(imgIndex < numImages - 1) {
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

    // TODO: handle the difference between setBitmap & attach
    // called by imageprocessingdialog
    public void cancelImageProcessing() {
        if(BuildConfig.DEBUG && imageProcessingTask == null)
            throw new AssertionError();
        imageProcessingTask.cancel(true);
        dialog.dismiss();
        dialog = null;
    }

    // called by imageprocessingtask
    public void endImageProcessing(ArrayList<Uri> outputBitmapUris) {
        // insert new URIs where required
        for(int i = 0, len = outputBitmapUris.size(); i < len; ++i) {
            int j = (imgIndex + i) % numImages;
            imageUris.set(j, outputBitmapUris.get(i));
            isProcessed.set(i, true);
        }
        // clean up and refresh
        imageProcessingTask = null;
        dialog.dismiss();
        dialog = null;
        refreshImage();
    }

    private void refreshImage() {
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                    imageUris.get(imgIndex));
            if(isProcessed.get(imgIndex))
                imageView.attachProcessedBitmap(bitmap);
            else
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
