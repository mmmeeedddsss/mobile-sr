package com.example.yuksel.mobilesr;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class PickPhotoActivity extends Activity {
    private static final int PICK_IMAGE = 100;

    private Bitmap bitmap;
    private ImageView imageView;
    private Button pickButton;
    private Button rotateButton;
    private Button processButton;
    private BitmapProcessor bitmapProcessor;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        bitmapProcessor = new TFLiteBilinearInterpolator(this);
        //Get the view From pick_photo_activity
        setContentView(R.layout.pick_photo_activity);

        imageView = findViewById(R.id.pick_photo_image_view);

        pickButton = findViewById(R.id.pick_photo_button);
        pickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });

        rotateButton = findViewById(R.id.rotate_image_button);
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setRotation( (imageView.getRotation() + 90)%360 );
            }
        });

        processButton = findViewById(R.id.process_image_button);
        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processImage();
            }
        });
    }

    public void pickImage() {
        Intent gallery_intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery_intent, PICK_IMAGE);
    }

    public void processImage() {
        if(bitmap != null) {
            long startTime = System.currentTimeMillis();
            bitmap = bitmapProcessor.processBitmap(bitmap);
            long estimatedTime = System.currentTimeMillis() - startTime;
            Toast.makeText(this,"Elapsed Time: "+estimatedTime, Toast.LENGTH_LONG ).show();
            imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            }
            catch (Exception ex) {
                Log.e("PickPhotoActivity.onActivityResult", "Error while loading image bitmap from URI", ex);
            }
            imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bitmapProcessor.close();
    }
}
