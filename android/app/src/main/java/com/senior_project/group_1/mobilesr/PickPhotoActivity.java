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

import java.util.ArrayList;
import java.util.Collections;

public class PickPhotoActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 100;

    private ZoomableImageView imageView;
    private Button pickButton;
    private Button rotateButton;
    private Button processButton;
    private Button splitButton;
    private BitmapProcessor bitmapProcessor;
    public static  ArrayList<Bitmap> chunkImages; // TODO: Only for debugging purpose (DELETE later.)

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

        splitButton = findViewById(R.id.split_image_button);
        splitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                splitImageHandler();
            }
        });

    }

    private void pickImage() {
        Intent gallery_intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery_intent, PICK_IMAGE);
    }

    private void processImage() {
        if(imageView.getCurrentBitmap() != null) {
            long startTime = System.nanoTime();
            Log.i("processImage", String.format("Bitmap size: %d %d",
                    imageView.getCurrentBitmap().getHeight(),
                    imageView.getCurrentBitmap().getWidth()));

            Bitmap bitmap = bitmapProcessor.processBitmap(imageView.getCurrentBitmap());
            long estimatedTime = System.nanoTime() - startTime;
            Toast.makeText(this,"Elapsed Time in ms: "+estimatedTime/1000000, Toast.LENGTH_LONG ).show();
            imageView.setImageBitmap(bitmap);
        }
    }

    private void splitImageHandler(){
        Bitmap[] bitmapArray = splitImage(imageView.getCurrentBitmap(), 25);
        Intent splitImageIntent = new Intent(PickPhotoActivity.this, MergedImageActivity.class);
        startActivity(splitImageIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();
            try {
                imageView.setImageBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri));
            }
            catch (Exception ex) {
                Log.e("PickPhotoActivity.onActivityResult", "Error while loading image bitmap from URI", ex);
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bitmapProcessor.close();
    }

    /**
     * Splits the source image and show them all into a grid in a new activity
     *  @param image The source image to split
     * @param chunkNumbers The target number of small image chunks to be formed from the   source image
     */
    // TODO: Parameters will change : width, height will be added.
    private Bitmap[] splitImage(final Bitmap scaledBitmap, int chunkNumbers) {
        // Number of rows and columns of the grid to be displayed.
        int rows, cols;

        // Height and width of small image chunks
        int chunkHeight, chunkWidth;

        // To store all the chunks in a list as bitmap format
        chunkImages = new ArrayList<>(chunkNumbers);

        // Get the scaled bitmap of the image
        /*
        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        */

        rows = cols = (int) Math.sqrt(chunkNumbers);
        chunkHeight = imageView.getCurrentBitmap().getHeight() / rows;
        chunkWidth = imageView.getCurrentBitmap().getWidth() / cols;

        //coordinateX and coordinateY are the pixel positions of the image chunks
        int coordinateY = 0;
        for(int x=0; x<rows; x++){
            int coordinateX = 0;
            for(int y=0; y<cols; y++){
                chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, chunkWidth, chunkHeight));
                coordinateX += chunkWidth;
            }
            coordinateY += chunkHeight;
        }
        Bitmap[] bitmapArray = new Bitmap[chunkImages.size()];
        chunkImages.toArray(bitmapArray);
        Collections.shuffle(chunkImages);
        return bitmapArray;
    }


}
