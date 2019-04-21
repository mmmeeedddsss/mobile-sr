package com.senior_project.group_1.mobilesr.img_processing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.activities.PreprocessAndEnhanceActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class LocalImageProcessingTask extends ImageProcessingTask {

    private ArrayList<Bitmap> chunkImages; // TODO: Only for debugging purpose (DELETE later.)
    private int columns;
    private int rows;

    public LocalImageProcessingTask(PreprocessAndEnhanceActivity requestingActivity, ImageProcessingDialog dialog, SRModelConfiguration modelConfiguration) {
        super(requestingActivity,dialog, modelConfiguration);
    }

    protected ArrayList<UserSelectedBitmapInfo> doInBackground(
            ArrayList<UserSelectedBitmapInfo>... bitmapUrisArr) {
        // apparently this replaces assert in Android
        if(BuildConfig.DEBUG && bitmapUrisArr.length != 1)
            throw new AssertionError();
        // generate and store result
        int batchSize = modelConfiguration.getNumParallelBatch();
        BitmapProcessor bitmapProcessor = new TFLiteSuperResolver(requestingActivity,
                batchSize, modelConfiguration);
        ArrayList<UserSelectedBitmapInfo> bitmapInfos = bitmapUrisArr[0];
        numImages = bitmapInfos.size();
        for(int imgIndex = 0, len = bitmapInfos.size(); imgIndex < len; imgIndex++) {

            if( bitmapInfos.get(imgIndex).isProcessed() ) // cached case
                continue;

            // try to load the bitmap first
            Bitmap bitmap = bitmapInfos.get(imgIndex).getBitmap();
            if(bitmap != null) {
                // the bitmap was loaded successfully
                Log.i("doInBackgrouund", "Image is send for division");
                divideImage(bitmap);
                Log.i("doInBackgrouund", "Division is done");
                // processImages copy & paste
                Bitmap[] bitmaps = new Bitmap[batchSize]; // buffer to hold input bitmaps
                int i = 0, nchunks = chunkImages.size();
                while (i < nchunks) {
                    // load bitmaps into the array
                    int j = 0;
                    while (j < batchSize && i < nchunks) {
                        Bitmap chunk = chunkImages.get(i++);
                        // Log.i("Divided Image Sizes","Image sizes for image "+ (i++) +"  "+chunk.getWidth()+"x"+chunk.getHeight());
                        bitmaps[j++] = chunk;
                    }
                    // process the bitmaps
                    Log.i("doInBackgrouund", "Process is called!");
                    System.gc();
                    Bitmap[] outputBitmaps = bitmapProcessor.processBitmaps(bitmaps);
                    Log.i("doInBackgrouund", "Process is end!");
                    // unload the bitmaps back into the list
                    int k = i;
                    while (j > 0)
                        chunkImages.set(--k, outputBitmaps[--j]);
                    // AsyncTask things
                    publishProgress(100 * imgIndex + (int) ((i / (float) nchunks) * 100));
                    if (isCancelled())
                        break;
                }
                // reconstruct the image and save it
                Bitmap result = reconstructImage();
                bitmapInfos.get(imgIndex).setBitmap(result);
                //Uri resultUri = BitmapHelpers.saveImageToTemp(bitmapInfos.get(imgIndex).getBitmap(), requestingActivity);
                //bitmapInfos.get(imgIndex).setProcessedUri(resultUri);
                // TODO: think about how gallery saving could still be useful
                /*
                String path = MediaStore.Images.Media.insertImage(
                        requestingActivity.getContentResolver(), result, "", "");
                Uri resultUri = Uri.parse(path);
                */
            }
        }
        bitmapProcessor.close();
        return bitmapInfos;
    }

    public Bitmap reconstructImage(){
        return reconstructImage(
                modelConfiguration.getInputImageHeight(),
                modelConfiguration.getInputImageWidth(),
                ApplicationConstants.IMAGE_OVERLAP_X,
                ApplicationConstants.IMAGE_OVERLAP_Y);
    }

    private void divideImage( final Bitmap scaledBitmap ) {
        divideImage(
                scaledBitmap,
                modelConfiguration.getInputImageHeight(),
                modelConfiguration.getInputImageWidth(),
                ApplicationConstants.IMAGE_OVERLAP_X,
                ApplicationConstants.IMAGE_OVERLAP_Y);
    }

    /**
     * This method takes the divided Images, from the model, model will increase the size of the image,
     * with respect to model_zoom_factor defined in the ApplicationConstants.java, then this method cuts
     * the middle of the chunks and returns and array list of that chunks.
     * @param chunkHeight
     * @param chunkWidth
     * @param overlapX
     * @param overlapY
     * @return Bitmap ArrayList
     */

    public ArrayList<Bitmap> prepareChunks(int chunkHeight, int chunkWidth, int overlapX, int overlapY) {
        ArrayList<Bitmap> result = new ArrayList<>();
        int modelZoomFactor = modelConfiguration.getRescalingFactor();
        for(int i=0; i<chunkImages.size(); i++) {
            try {
                result.add(Bitmap.createBitmap(chunkImages.get(i),
                        overlapX * modelZoomFactor,
                        overlapY * modelZoomFactor,
                        (chunkWidth - overlapX * 2) * modelZoomFactor,
                        (chunkHeight - overlapY * 2) * modelZoomFactor));
            }
            catch(NullPointerException e) {
                Log.e("prepareChunks", "image: " + i);
            }
        }
        Log.i("PrepareChunks", String.format("Reconstruction Chunk sizes %dx%d",result.get(0).getWidth(), result.get(0).getHeight()));
        return result;
    }

    /**
     * This method reconstructs the divided image, first prepares the image by calling the prepareChunks() method.
     * @param chunkHeight
     * @param chunkWidth
     * @param overlapX
     * @param overlapY
     * @return bitmap.
     */
    public Bitmap reconstructImage(int chunkHeight, int chunkWidth, int overlapX, int overlapY) {
        ArrayList<Bitmap> result = prepareChunks(chunkHeight, chunkWidth,overlapX, overlapY); // Prepare the chunks to reassemble the image.
        int newChunkWidth = result.get(0).getWidth();
        int newChunkHeight = result.get(0).getHeight();

        int outputHeight = newChunkHeight*rows;
        int outputWidth = newChunkWidth*columns;
        Bitmap bitmap = Bitmap.createBitmap(
                outputWidth,
                outputHeight,
                Bitmap.Config.ARGB_4444);
        Log.i("ReconstructImage", String.format("%d chunks with size %dx%d is created",chunkImages.size(), chunkImages.get(0).getWidth(), chunkImages.get(0).getHeight()));
        Canvas canvas = new Canvas(bitmap); // this constructor causes canvas operations to write on provided bitmap

        Log.i("ReconstructImage ", String.format("Chunks : %d", rows*columns));
        for (int r = 0 ; r<rows; r++) // y
            for (int c =0 ; c<columns ; c++) // x
                canvas.drawBitmap(result.get(c+r*columns), c*newChunkWidth, r*newChunkHeight, null);

        for (int r = 0 ; r<rows; r++) // y
            for (int c =0 ; c<columns ; c++) // x
                result.get(c+r*columns).recycle();

        Log.i("ImageCheck(): ", String.format("Bitmap size: %d %d", bitmap.getWidth(), bitmap.getHeight()));
        return bitmap;
    }

    /**
     * This method divides the image into chunks with some overlap in both X and Y axis.
     * Method creates a bitmap array, each bitmap represents a chunk in the whole picture.
     * Method does not divide the image if the width and height values of the image is not dividable
     * by overlap and chunk width and chunk height
     * @param scaledBitmap
     * @param chunkHeight
     * @param chunkWidth
     * @param overlapX
     * @param overlapY
     * @return
     */
    public void divideImage(final Bitmap scaledBitmap, int chunkHeight, int chunkWidth, int overlapX, int overlapY) throws RuntimeException{

        // Check that overlap values are not bigger than chunk height and width
        if (overlapX > chunkWidth || overlapY > chunkHeight)
            throw new RuntimeException();

        // Check that image width and height is divisible by the chunk values and overlap
        if ((scaledBitmap.getHeight()-overlapY*2) % (chunkHeight - overlapY*2) != 0 || (scaledBitmap.getWidth()-overlapX*2) % (chunkWidth - overlapX*2) != 0) {
            Log.i("ImageCheck(): ", String.format("Bitmap size: %d %d", scaledBitmap.getHeight(), scaledBitmap.getWidth()));
            throw new RuntimeException();

        }
        else {
            chunkImages = new ArrayList<>();

            columns =0;
            rows = 0;
            //coordinateX and coordinateY are the pixel positions of the image chunks
            for (int coordinateY = 0; coordinateY <= scaledBitmap.getHeight()-chunkHeight; coordinateY += chunkHeight - overlapY*2) {

                for (int coordinateX = 0; coordinateX <= scaledBitmap.getWidth()-chunkWidth; coordinateX += chunkWidth - overlapX*2) {
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, chunkWidth, chunkHeight));
                    columns++;
                }
                rows++;
            }
            columns=columns/rows;

            Log.i("DivideImage","Image is divided to  "+columns+"x"+rows);
        }

    }

}
