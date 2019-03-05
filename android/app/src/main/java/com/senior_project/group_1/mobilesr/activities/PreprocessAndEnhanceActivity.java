package com.senior_project.group_1.mobilesr.activities;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.img_processing.ImageProcessingDialog;
import com.senior_project.group_1.mobilesr.img_processing.ImageProcessingTask;
import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;
import com.senior_project.group_1.mobilesr.views.BitmapHelpers;
import com.senior_project.group_1.mobilesr.views.ZoomableImageView;

import java.util.ArrayList;

/* I changed PreprocessAndEnhanceActivity into an abstract base class since we want
 * different actions for the single image/multi image case. This class implements all
 * the required common functionality. Two concrete classes extend it, one for the single
 * image and the other for the multiple image case. They have to provide their own constructors
 * to disable/enable necessary buttons and their own version of endImageProcessing to decide
 * on what to do when image processing ends.
 */

public abstract class PreprocessAndEnhanceActivity extends AppCompatActivity {

    private boolean isPaused;
    protected ZoomableImageView imageView;
    protected Button rotateButton, processButton, processAllButton,
                     nextButton, prevButton, toggleButton,
                     saveButton, shareButton;
    private ImageProcessingDialog dialog;
    private ImageProcessingTask imageProcessingTask;
    protected ArrayList<Uri> imageUris; // TODO: wrap uri + processing state in a class?
    protected int numImages;
    protected int imgIndex;

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
        for(int i = 0, len = imageClipData.getItemCount(); i < len; ++i)
            imageUris.add(imageClipData.getItemAt(i).getUri());
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

    private void processImage() {
        // create a new URI for the requested part of the image
        Bitmap partialImg = imageView.getCurrentBitmap();
        Uri partialUri = BitmapHelpers.saveImageExternal(partialImg, this);
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(partialUri);
        processImages(uris);
    }

    private void processAllImages() {
        // add all URIs with the current view order
        // have to create new URIs for properly reflect-padded versions though
        ArrayList<Uri> uris = new ArrayList<>();
        int i = imgIndex;
        do {
            Bitmap origBitmap = BitmapHelpers.loadBitmapFromURI(
                    imageUris.get(i), this.getContentResolver()
            );
            Bitmap paddedBitmap = BitmapHelpers.cropBitmapUsingSubselection(origBitmap);
            Uri paddedUri = BitmapHelpers.saveImageExternal(paddedBitmap, this);
            uris.add(paddedUri);
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

    // called by imageprocessingdialog
    public void cancelImageProcessing() {
        if(BuildConfig.DEBUG && imageProcessingTask == null)
            throw new AssertionError();
        imageProcessingTask.cancel(true);
        cleanUpTask();
    }

    // called by imageprocessingtask, should be properly overriden by extending classes
    public abstract void endImageProcessing(ArrayList<Uri> outputBitmapUris);

    // call after cancellation/end
    protected void cleanUpTask() {
        imageProcessingTask = null;
        dialog.dismiss();
        dialog = null;
    }

    protected void refreshImage() {
        Bitmap bitmap = BitmapHelpers.loadBitmapFromURI(imageUris.get(imgIndex), this.getContentResolver());
        if(bitmap != null)
            imageView.setImageBitmap(bitmap);
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
