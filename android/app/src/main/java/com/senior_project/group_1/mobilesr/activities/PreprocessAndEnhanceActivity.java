package com.senior_project.group_1.mobilesr.activities;

import android.app.PendingIntent;
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

    public static Bitmap bitmap;
    private ZoomableImageView imageView;
    private Button rotateButton;
    private Button processButton;
    private Button splitButton;
    private BitmapProcessor bitmapProcessor;
    private Uri mImageUri;
    private ImageProcessingDialog dialog;
    private ImageProcessingTask imageProcessingTask;

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

        // Set content of Zoomable image view
        Intent intent = getIntent();
        mImageUri = intent.getParcelableExtra("imageUri");
        setImage();
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
        sendNotification();
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

    // send a notification, for use when processing is complete
    private void sendNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, ApplicationConstants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Superresolution Completed")
                .setContentText("The images you have assigned for superresolution have been processed.")
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(492, mBuilder.build());

    }
}
