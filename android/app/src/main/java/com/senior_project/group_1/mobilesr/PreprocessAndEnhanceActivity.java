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

    public static Bitmap bitmap;
    private ZoomableImageView imageView;
    private Button rotateButton;
    private Button processButton;
    private Button splitButton;
    private BitmapProcessor bitmapProcessor;
    public static ArrayList<Bitmap> chunkImages; // TODO: Only for debugging purpose (DELETE later.)
    public static int columns;
    public static int rows;
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
                divideImage(bitmap, 100, 90, 20, 30);
                Intent splitImageIntent = new Intent(PreprocessAndEnhanceActivity.this, DivideImageActivity.class);
                startActivity(splitImageIntent);
            }
        });

        // Set content of Zoomable image view
        Intent intent = getIntent();
        mImageUri = intent.getParcelableExtra("imageUri");
        setImage();
    }

    public void processImage() {
        if (bitmap != null) {
            long startTime = System.nanoTime();
            Log.i("processImage", String.format("Bitmap size: %d %d", bitmap.getHeight(), bitmap.getWidth()));
            bitmap = bitmapProcessor.processBitmap(bitmap);
            long estimatedTime = System.nanoTime() - startTime;
            Toast.makeText(this, "Elapsed Time in ms: " + estimatedTime / 1000000, Toast.LENGTH_LONG).show();
            imageView.setImageBitmap(bitmap);
        }
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
        bitmapProcessor.close();
    }

    /**
     * This method arranges divided chunks so that the chunks are ready to reconstruction of the image
     * Creates a new bitmap array and cuts every image chunk so that the overlapped sections are gone.
     * Then returns the new bitmap array to be reconstructed.
     * @param overlapX
     * @param overlapY
     * @return ArrayList<Bitmap>
     */
    public static ArrayList<Bitmap> arrangeChunks(int overlapX, int overlapY){
        int startX,startY,cutX,cutY;
        ArrayList<Bitmap> result = new ArrayList<>();
        int  index = 0;
        for (int r =0; r<rows;r++){
            for (int c =0 ; c<columns ; c++){

                if(c == 0){
                    startX = 0; startY=0;
                    cutX = chunkImages.get(index).getWidth();
                    if(r == rows-1){
                        cutY = chunkImages.get(index).getHeight();
                    }else {
                        cutY = chunkImages.get(index).getHeight() - overlapY;
                    }
                }else {
                    startX = overlapX;
                    startY  = 0;
                    cutX=chunkImages.get(index).getWidth() - overlapX;
                    if(r == rows-1){
                        cutY = chunkImages.get(index).getHeight();
                    }else {
                        cutY = chunkImages.get(index).getHeight() - overlapY;
                    }
                }

                result.add(Bitmap.createBitmap(chunkImages.get(index),startX,startY,cutX,cutY));

                index++;
            }
        }
        return  result;
    }

    /**
     * This method reconstructs the divided image, first prepares the image by calling the arrangeChunks() method.
     * This method has few bugs if you run the app, you will see it :-))
     * TODO: Try to clear the bugs in the method.
     * @param scaledBitmap
     * @param chunkHeight
     * @param chunkWidth
     * @param overlapX
     * @param overlapY
     * @return bitmap.
     */
    public static Bitmap reconstructImage(final Bitmap scaledBitmap, int chunkHeight, int chunkWidth, int overlapX, int overlapY) {
        Bitmap bitmap = Bitmap.createBitmap(scaledBitmap.getWidth(), scaledBitmap.getHeight(),  Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        // Prepare the chunks to reassemble the image.
        ArrayList<Bitmap> result = arrangeChunks(overlapX, overlapY);


        int index = 0;
        int top = 0 , left =0;
        for (int c = 0 ; c<columns; c++){
            for (int r =0 ; r<rows ; r++){
                canvas.drawBitmap(result.get(index), left, top, null);
                left+=result.get(index).getWidth();
                index++;
            }
            left=0;
            top += result.get(index-1).getHeight();
        }
        return bitmap;
    }



    /**
     * This method does the necessary checks for the image to be divided.
     * Just for now, this method gives a RuntimeException for the edge cases.
     *
     * @param scaledBitmap : Bitmap to be divided
     * @param chunkHeight  : Divided chunk height value
     * @param chunkWidth   : Divided chunk width value
     * @param overlapX     : Overlap for the X-axis
     * @param overlapY     : Overlap for the Y-axis
     * @throws RuntimeException
     */
    private void imageCheck(final Bitmap scaledBitmap, int chunkHeight, int chunkWidth, int overlapX, int overlapY) throws RuntimeException {

        // Check that overlap values are not bigger than chunk height and width
        if (overlapX > chunkWidth || overlapY > chunkHeight)
            throw new RuntimeException();

        // Check that image width and height is divisible by the chunk values and overlap
        if (scaledBitmap.getHeight() % (chunkHeight - overlapY) != 0 || scaledBitmap.getWidth() % (chunkWidth - overlapX) != 0) {
            Log.i("ImageCheck(): ", String.format("Bitmap size: %d %d", scaledBitmap.getHeight(), scaledBitmap.getWidth()));
            throw new RuntimeException();

        }
        divideImage(scaledBitmap, chunkHeight, chunkWidth, overlapX, overlapY);

    }

    /**
     * This method divides the image into chunks with some overlap in both X and Y axis.
     * Method creates a bitmap array, each bitmap represents a chunk in the whole picture.
     * @param scaledBitmap
     * @param chunkHeight
     * @param chunkWidth
     * @param overlapX
     * @param overlapY
     * @return
     */
    private void divideImage(final Bitmap scaledBitmap, int chunkHeight, int chunkWidth, int overlapX, int overlapY) {
        chunkImages = new ArrayList<>();

        columns =0;
        rows = 0;
        //coordinateX and coordinateY are the pixel positions of the image chunks
        for (int coordinateY = 0; coordinateY < scaledBitmap.getHeight(); coordinateY += chunkHeight - overlapY) {

            for (int coordinateX = 0; coordinateX < scaledBitmap.getWidth(); coordinateX += chunkWidth - overlapX) {
                // The rest of the bitmap's width and height is lower than the chunkwidth and chunkheight
                if (scaledBitmap.getWidth() - coordinateX < chunkWidth && scaledBitmap.getHeight() - coordinateY < chunkHeight)
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, scaledBitmap.getWidth() - coordinateX, scaledBitmap.getHeight() - coordinateY));

                    // The rest of the bitmap's width is lower than the chunkwidth
                else if (scaledBitmap.getWidth() - coordinateX < chunkWidth)
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, scaledBitmap.getWidth() - coordinateX, chunkHeight));

                    // The rest of the bitmap's height is lower than the chunkheight
                else if (scaledBitmap.getHeight() - coordinateY < chunkHeight)
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, chunkWidth, scaledBitmap.getHeight() - coordinateY));

                else
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, chunkWidth, chunkHeight));

                rows++;
            }
            columns++;
        }
        rows=rows/columns;


    }
}
