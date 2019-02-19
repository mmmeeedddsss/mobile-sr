package com.senior_project.group_1.mobilesr.activities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 111;
    static final int REQUEST_IMAGE_SELECT = 112;

    Button pickPhotoButton, takePhotoButton, settingsButton, tutorialButton;
    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create the notification channel for the application
        createNotificationChannel();

        // Pick photo button activity
        pickPhotoButton = (Button) findViewById(R.id.buttonPickPhoto);

        // Capture Pick Photo clicks
        pickPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImageFromGallery();
            }
        });

        // Take photo button activity
        takePhotoButton = (Button) findViewById(R.id.buttonTakePhoto);

        // Take Photo clicks hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
            }
        });

        // Settings button activity
        settingsButton = (Button) findViewById(R.id.buttonSetttings);

        // Settings button clicks
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        // Tutorial button activity
        tutorialButton = (Button) findViewById(R.id.buttonTutorial);

        // Tutorial button clicks
        tutorialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent tutorialIntent = new Intent(MainActivity.this, TutorialActivity.class);
                startActivity(tutorialIntent);
            }
        });

        try { // Load configuration xml
            SRModelConfigurationManager
                    .initilizeConfigurations( getAssets().open(ApplicationConstants.CONFIGURATION_FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                ApplicationConstants.EXTERNAL_WRITE_PERMISSION_ID);
    }

    public void pickImageFromGallery() {
        Intent gallery_intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery_intent, REQUEST_IMAGE_SELECT);
    }

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null; // Create the File where the photo should be written
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this.getApplicationContext(),
                        "Error occured while creating an image file", Toast.LENGTH_LONG).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.senior_project.group_1.mobilesr.activities.MainActivity",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                mImageUri = photoURI;
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( resultCode == RESULT_OK ) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Below, we are adding the captured image to gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(mImageUri);
                this.sendBroadcast(mediaScanIntent);
            }
            else if (requestCode == REQUEST_IMAGE_SELECT) {
                mImageUri = data.getData();
            }
            // Call opration activity
            Intent pickPhotoIntent = new Intent(MainActivity.this, PreprocessAndEnhanceActivity.class);
            pickPhotoIntent.putExtra("imageUri", mImageUri); // uri implements Parsable
            startActivity(pickPhotoIntent);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,".jpg", storageDir );

        return image;
    }

    // have to create a notification channel in Android 8.0+
    // copied verbatim from https://developer.android.com/training/notify-user/channels
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(ApplicationConstants.NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
