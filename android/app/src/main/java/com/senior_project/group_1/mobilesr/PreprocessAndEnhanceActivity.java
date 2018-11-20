package com.senior_project.group_1.mobilesr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class PreprocessAndEnhanceActivity extends AppCompatActivity {

    private Bitmap bitmap;
    private ZoomableImageView imageView;
    private Button rotateButton;
    private Button processButton;
    private Button splitButton;
    private BitmapProcessor bitmapProcessor;
    public static  ArrayList<Bitmap> chunkImages; // TODO: Only for debugging purpose (DELETE later.)
    public static Bitmap[] bitmapArray; // TODO: This will not be the public static in the next stages
    public static int columns;
    private Uri mImageUri;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        bitmapProcessor = new TFLiteBilinearInterpolator(this);
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

        splitButton = findViewById(R.id.split_image_button);
        splitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap[] bitmapArray = divideImage(bitmap, 100 , 90, 20, 30);
                Intent splitImageIntent = new Intent(PreprocessAndEnhanceActivity.this, MergedImageActivity.class);
                startActivity(splitImageIntent);
            }
        });

        // Set content of Zoomable image view
        Intent intent = getIntent();
        mImageUri = intent.getParcelableExtra("imageUri");
        setImage();
    }

    public void processImage() {
        if(bitmap != null) {
            long startTime = System.nanoTime();
            Log.i("processImage", String.format("Bitmap size: %d %d", bitmap.getHeight(), bitmap.getWidth()));
            bitmap = bitmapProcessor.processBitmap(bitmap);
            long estimatedTime = System.nanoTime() - startTime;
            Toast.makeText(this,"Elapsed Time in ms: "+estimatedTime/1000000, Toast.LENGTH_LONG ).show();
            imageView.setImageBitmap(bitmap);
        }
    }

    protected void setImage() {
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageUri);
            imageView.setImageBitmap(bitmap);
        }
        catch (Exception ex) {
            Log.e("PreprocessAndEnhanceActivity.onActivityResult", "Error while loading image bitmap from URI", ex);
        }
    }


    /**
     * This method does the necessary checks for the image to be divided.
     * Just for now, this method gives a RuntimeException for the edge cases.
     * @param scaledBitmap : Bitmap to be divided
     * @param chunkHeight : Divided chunk height value
     * @param chunkWidth : Divided chunk width value
     * @param overlapX : Overlap for the X-axis
     * @param overlapY : Overlap for the Y-axis
     * @throws RuntimeException
     */
    private void imageCheck(final Bitmap scaledBitmap, int chunkHeight, int chunkWidth, int overlapX, int overlapY) throws RuntimeException {

        // Check that overlap values are not bigger than chunk height and width
        if(overlapX > chunkWidth || overlapY > chunkHeight)
            throw new RuntimeException();

        // Check that image width and height is divisible by the chunk values and overlap
        if(scaledBitmap.getHeight() % (chunkHeight - overlapY) != 0 || scaledBitmap.getWidth() % (chunkWidth - overlapX) != 0) {
            Log.i("ImageCheck(): ", String.format("Bitmap size: %d %d", scaledBitmap.getHeight(), scaledBitmap.getWidth()));
            throw new RuntimeException();

        }
        bitmapArray = divideImage(scaledBitmap, chunkHeight, chunkWidth, overlapX, overlapY);

    }

    private Bitmap[] divideImage(final Bitmap scaledBitmap, int chunkHeight, int chunkWidth, int overlapX, int overlapY) {
        /*This method takes a bitmap, and arbitrary width and height values and an overlap value,
         * and divides the image into grids of given height and width values, with a given overlap between chunks*/
        chunkImages = new ArrayList<>();

        columns = scaledBitmap.getWidth() / (chunkWidth-overlapX) + 1;

        //coordinateX and coordinateY are the pixel positions of the image chunks
        for(int coordinateY=0; coordinateY<scaledBitmap.getHeight(); coordinateY += chunkHeight - overlapY){

            for(int coordinateX=0; coordinateX<scaledBitmap.getWidth(); coordinateX += chunkWidth - overlapX){
                // The rest of the bitmap's width and height is lower than the chunkwidth and chunkheight
                if(scaledBitmap.getWidth()-coordinateX < chunkWidth && scaledBitmap.getHeight()-coordinateY < chunkHeight)
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, scaledBitmap.getWidth()-coordinateX, scaledBitmap.getHeight()-coordinateY));

                // The rest of the bitmap's width is lower than the chunkwidth
                else if(scaledBitmap.getWidth()-coordinateX < chunkWidth)
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, scaledBitmap.getWidth()-coordinateX, chunkHeight));

                // The rest of the bitmap's height is lower than the chunkheight
                else if(scaledBitmap.getHeight()-coordinateY < chunkHeight)
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, chunkWidth, scaledBitmap.getHeight()-coordinateY));

                else
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, chunkWidth, chunkHeight));
            }
        }

        Bitmap[] bitmapArray = new Bitmap[chunkImages.size()];
        chunkImages.toArray(bitmapArray);
        return bitmapArray;
    }

    /*TODO: This method is not completed yet*/
    public void reassembleImage(final Bitmap scaledBitmap,int chunkHeight, int chunkWidth) {
        int rows = scaledBitmap.getHeight() / chunkHeight;
        int cols = scaledBitmap.getWidth() / chunkWidth;

        Bitmap bitmap = Bitmap.createBitmap(chunkWidth * cols, chunkHeight * rows,  Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);

        int chunkNumbers = 0;
        for(int i=0; i<rows; i++) {
            for(int j=0; j<cols; j++) {
                canvas.drawBitmap(chunkImages.get(chunkNumbers), chunkWidth * j, chunkHeight * i, null);
                chunkNumbers++;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bitmapProcessor.close();
    }


}
