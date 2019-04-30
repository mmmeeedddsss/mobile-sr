package com.senior_project.group_1.mobilesr.activities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;
import com.senior_project.group_1.mobilesr.img_processing.BitmapHelpers;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 111;
    static final int REQUEST_IMAGE_SELECT = 112;

    Button pickPhotoButton, takePhotoButton, settingsButton, settingsButton2, tutorialButton;
    Button tcpTestButton;
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

        // Settings button activitys
        settingsButton = (Button) findViewById(R.id.buttonSetttings);

        // Settings button clicks
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        // Settings button activitys
        settingsButton2 = (Button) findViewById(R.id.buttonSetttings2);

        // Settings button clicks
        settingsButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity2.class);
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
            File root = android.os.Environment.getExternalStorageDirectory();
            File file = new File(root.getAbsolutePath(), "sr_model_configurations.xml");
            if( file.exists() )
                file.delete(); // TODO remove on production
            if (!file.exists())
            {
                InputStream inConfig = getAssets().open(ApplicationConstants.CONFIGURATION_FILE_NAME);
                OutputStream outConfig = new FileOutputStream(file);
                byte[] buf = new byte[1024];
                int read;
                while ((read = inConfig.read(buf)) != -1)
                    outConfig.write(buf, 0, read);
                outConfig.flush();
                outConfig.close();
                inConfig.close();
            }
            SRModelConfigurationManager
                    .initilizeConfigurations( new FileInputStream(file));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
        gallery_intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
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
                        "com.senior_project.group_1.mobilesr",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                mImageUri = photoURI;
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("onActivityResult","SRModelConfigurationManager.getCurrentConfiguration().isRemote() = "+SRModelConfigurationManager.getCurrentConfiguration().isRemote());
        ClipData imageClipData = null;
        if( resultCode == RESULT_OK ) {
            Intent pickPhotoIntent = null;
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Below, we are adding the captured image to gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(mImageUri);
                this.sendBroadcast(mediaScanIntent);
                // create a clipdata from the URI
                ClipDescription description = new ClipDescription(null, new String[] {"text/uri-list"});
                ClipData.Item item = new ClipData.Item(mImageUri);
                imageClipData = new ClipData(description, item);
                // create a single image intent
                if(SRModelConfigurationManager.getCurrentConfiguration().isRemote()) {
                    pickPhotoIntent = new Intent(MainActivity.this,
                            RemoteImageEnhanceActivity.class);
                }
                else {
                    pickPhotoIntent = new Intent(MainActivity.this,
                            SingleImageEnhanceActivity.class);
                }
            }
            else if (requestCode == REQUEST_IMAGE_SELECT) {
                // start the proper activity depending on the number of selected photos
                imageClipData = data.getClipData();
                Log.i("Clipdata info:", imageClipData.getDescription().getLabel() + "---" + imageClipData.getDescription().getMimeType(0));
                if(SRModelConfigurationManager.getCurrentConfiguration().isRemote()) {
                    pickPhotoIntent = new Intent(MainActivity.this,
                            RemoteImageEnhanceActivity.class);
                }
                else {
                    if (imageClipData.getItemCount() == 1) { // one photo was picked
                        pickPhotoIntent = new Intent(MainActivity.this,
                                SingleImageEnhanceActivity.class);
                    } else { // more than one photo was picked
                        pickPhotoIntent = new Intent(MainActivity.this,
                                MultipleImageEnhanceActivity.class);
                    }
                }
            }
            pickPhotoIntent.putExtra("imageClipData", imageClipData);
            pickPhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickPhotoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity", "ON DESTROY CALLED, clearing temp folder");
        BitmapHelpers.clearTempFolder();
        BitmapHelpers.moderateCacheSize();
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
