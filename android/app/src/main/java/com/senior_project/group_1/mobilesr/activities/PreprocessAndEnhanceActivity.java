package com.senior_project.group_1.mobilesr.activities;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.img_processing.BitmapHelpers;
import com.senior_project.group_1.mobilesr.img_processing.GallerySavingUtil;
import com.senior_project.group_1.mobilesr.img_processing.ImageProcessingDialog;
import com.senior_project.group_1.mobilesr.img_processing.ImageProcessingTask;
import com.senior_project.group_1.mobilesr.img_processing.UserSelectedBitmapInfo;
import com.senior_project.group_1.mobilesr.views.ZoomableImageView;

import java.io.File;
import java.util.ArrayList;

import ru.dimorinny.floatingtextbutton.FloatingTextButton;

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
    protected FloatingTextButton processButton, processAllButton;
    protected ImageProcessingDialog dialog;
    protected ImageProcessingTask imageProcessingTask;
    protected  ArrayList<UserSelectedBitmapInfo> bitmapInfos;
    protected int numImages;
    protected int imgIndex;

    Menu settingsMenu;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        isPaused = false;
        imgIndex = 0;

        //Get the view From pick_photo_activity
        setContentView(R.layout.pick_photo_activity);

        imageView = findViewById(R.id.pick_photo_image_view);

        processButton = findViewById(R.id.process_image_button);
        processButton.setOnClickListener(v -> {
            processImage();
        });

        processAllButton = findViewById(R.id.process_all_button);
        processAllButton.setOnClickListener(v -> processAllImages());


        imageView.setSwipeListener(new ZoomableImageView.SwipeEventListener() {
            @Override
            public void swipeLeft() {
                nextImage();
                updateTitle();
            }

            @Override
            public void swipeRight() {
                prevImage();
                updateTitle();
            }
        });

        Intent intent = getIntent();
        // fill image URIs
        ClipData imageClipData = intent.getParcelableExtra("imageClipData");
        bitmapInfos = new ArrayList<>();
        for(int i = 0, len = imageClipData.getItemCount(); i < len; ++i)
            bitmapInfos.add( new UserSelectedBitmapInfo(imageClipData.getItemAt(i).getUri(), i, this.getContentResolver()));
        numImages = bitmapInfos.size();

        // Set content of Zoomable image view
        refreshImage();

        placeProcessingButtons();

        updateTitle();
    }

    abstract protected void setupAsyncTask();

    protected void processImages( ArrayList<UserSelectedBitmapInfo> bmInfos ) {
        settingsMenu.findItem(R.id.rotate).setEnabled(false);
        setupAsyncTask();
        dialog.show();
        imageProcessingTask.execute(bmInfos);
    }

    private void processImage() {
        // create a new URI for the requested part of the image
        Bitmap bm = imageView.getCurrentBitmap();
        Uri partialUri = BitmapHelpers.saveImageToTemp(bm, this);
        ArrayList<UserSelectedBitmapInfo> bmInfos = new ArrayList<>();
        bmInfos.add(new UserSelectedBitmapInfo(partialUri, 0, this.getContentResolver()));
        processImages(bmInfos);
    }

    // This method preprocessed the images to add padding, skips if image is cached
    public void processAllImages() {
        int i = imgIndex;
        do {
            // isCached call fills the content if processed bitmap is cached
            if( !BitmapHelpers.isBitmapCached(bitmapInfos.get(i), this) )
            {
                Bitmap origBitmap = bitmapInfos.get(i).getBitmap();
                Bitmap paddedBitmap = BitmapHelpers.cropBitmapUsingSubselection(origBitmap);
                bitmapInfos.get(i).setBitmap(paddedBitmap);
            }
            i = (i + 1) % numImages;
        } while(i != imgIndex);
        processImages(bitmapInfos);
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
        Log.i("PreprocessAndEnhanceAct", "Cancel task is called !");
        if(BuildConfig.DEBUG && imageProcessingTask == null)
            throw new AssertionError();
        imageProcessingTask.cancel(true);
        cleanUpTask();
    }

    // called by imageprocessingtask, should be properly overriden by extending classes
    public abstract void endImageProcessing(ArrayList<UserSelectedBitmapInfo> outputBitmapUris);

    // call after cancellation/end
    protected void cleanUpTask() {
        try {
            dialog.dismiss();
        } catch (Exception ex)
        {
            Log.i("PreprocessAndEnhanceAct.cleanup_task", ex.getMessage());
        }
    }

    protected void refreshImage() {
        Bitmap bitmap = bitmapInfos.get(imgIndex).getBitmap();
        if(bitmap != null)
            imageView.setImageBitmap(bitmap, bitmapInfos.get(imgIndex).getNonProcessedBmSize());
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

    protected void placeProcessingButtons(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        int offsetFromBottom = -130;
        int offsetFromRight = -width+480;

        if( this instanceof SingleImageEnhanceActivity ){ // Indicator of such a nice design that we have
            processButton.setVisibility(View.VISIBLE);
            processAllButton.setVisibility(View.INVISIBLE);

            processButton.animate().translationY(-getResources().getDimension(R.dimen.padding)-offsetFromBottom);
        }
        else{
            processButton.setVisibility(View.VISIBLE);
            processAllButton.setVisibility(View.VISIBLE);

            processButton.animate().translationY(-getResources().getDimension(R.dimen.padding)-offsetFromBottom);
            processAllButton.animate().translationY(-getResources().getDimension(R.dimen.padding)-offsetFromBottom);
            processAllButton.animate().translationX( offsetFromRight );
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        settingsMenu = menu;
        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save) {
            CharSequence cs[] = {"Override", "Save as new", "Save only processed parts"};
            new AlertDialog.Builder(PreprocessAndEnhanceActivity.this)
                    .setTitle("Select Saving Preferance")
                    .setItems( cs, (dialog, which) -> {
                        if (which == 0){
                            BitmapHelpers.saveImageInternal(imageView.getFullBitmap(),
                                    bitmapInfos.get(imgIndex).getNonProcessedUri(), getApplicationContext());
                        }
                        else if (which == 1){
                            //BitmapHelpers.saveImageExternal(imageView.getFullBitmap(), getApplicationContext());
                            String url = GallerySavingUtil.insertImage(getContentResolver(),
                                    imageView.getFullBitmap(), "MobileSR", "MobileSR");
                            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            File f = new File(url);
                            Uri contentUri = Uri.fromFile(f);
                            mediaScanIntent.setData(contentUri);
                            this.sendBroadcast(mediaScanIntent);
                        }
                        else if (which == 2){
                            ArrayList<Bitmap> processedBitmaps = imageView.getProcessedBitmaps();
                            for ( Bitmap bm : processedBitmaps ) {
                                //BitmapHelpers.saveImageExternal(bm, getApplicationContext());
                                String url = GallerySavingUtil.insertImage(getContentResolver(),
                                        bm, "MobileSR", "MobileSR");
                                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                File f = new File(url);
                                Uri contentUri = Uri.fromFile(f);
                                mediaScanIntent.setData(contentUri);
                                this.sendBroadcast(mediaScanIntent);
                            }
                        }
                    }).show();
        }
        if (item.getItemId() == R.id.share) {
            Uri savedImageUri = BitmapHelpers.saveImageExternal(imageView.getFullBitmap(), getApplicationContext());
            startActivity(Intent.createChooser(BitmapHelpers.createShareIntentByUri(savedImageUri), "Share Image"));
        }
        if (item.getItemId() == R.id.rotate){
            imageView.rotate();
        }
        if (item.getItemId() == R.id.toggle){
            imageView.toggleSrDrawal();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateTitle()
    {
        if( numImages > 1 )
            setTitle(String.format("MobileSR (%d/%d)", imgIndex+1, numImages));
        else
            setTitle("MobileSR");
    }
}




