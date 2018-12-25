package com.senior_project.group_1.mobilesr;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class ImageProcessingTask extends AsyncTask<Bitmap, Integer, Bitmap> {

    private ArrayList<Bitmap> chunkImages; // TODO: Only for debugging purpose (DELETE later.)
    private int columns;
    private int rows;
    private PreprocessAndEnhanceActivity requestingActivity;
    private ImageProcessingDialog dialog;
    private SRModelConfiguration modelConfiguration;
    private long startTime;

    public ImageProcessingTask(PreprocessAndEnhanceActivity requestingActivity, ImageProcessingDialog dialog, SRModelConfiguration modelConfiguration) {
        super();
        this.requestingActivity = requestingActivity;
        this.dialog = dialog;
        this.modelConfiguration = modelConfiguration;
    }

    protected void onPreExecute() {
        startTime = System.nanoTime();
    }

    protected Bitmap doInBackground(Bitmap... bitmapObjs) {
        // apparently this replaces assert in Android
        if(BuildConfig.DEBUG && bitmapObjs.length != 1)
            throw new AssertionError();
        Bitmap bitmap = bitmapObjs[0];
        divideImage(bitmap);
        // processImages copy & paste
        int batchSize = ApplicationConstants.BATCH_SIZE;
        Bitmap[] bitmaps = new Bitmap [batchSize]; // buffer to hold input bitmaps
        BitmapProcessor bitmapProcessor = new TFLiteSuperResolver(requestingActivity, batchSize, modelConfiguration);
        int i = 0, nchunks = chunkImages.size();
        while(i < nchunks) {
            // load bitmaps into the array
            int j = 0;
            while(j < batchSize && i < nchunks) {
                Bitmap chunk = chunkImages.get(i++);
                // Log.i("Divided Image Sizes","Image sizes for image "+ (i++) +"  "+chunk.getWidth()+"x"+chunk.getHeight());
                bitmaps[j++] = chunk;
            }
            // process the bitmaps
            Bitmap[] outputBitmaps = bitmapProcessor.processBitmaps(bitmaps);
            // unload the bitmaps back into the list
            int k = i;
            while(j > 0)
                chunkImages.set(--k, outputBitmaps[--j]);
            // AsyncTask things
            publishProgress((int) ((i / (float) nchunks) * 100));
            if(isCancelled())
                break;
        }
        bitmapProcessor.close();
        return reconstructImage();
    }

    protected void onProgressUpdate(Integer... progress) {
        dialog.updateProgressBar(progress[0]);
    }

    protected void onPostExecute(Bitmap result) {
        long estimatedTime = System.nanoTime() - startTime;
        Toast.makeText(requestingActivity, "Elapsed time: " + estimatedTime / 1000000 + " ms", Toast.LENGTH_LONG).show();
        requestingActivity.endImageProcessing(result);
    }

    public Bitmap reconstructImage(){
        return reconstructImage(
                modelConfiguration.getInputImageHeight(),
                modelConfiguration.getInputImageWidth(),
                ApplicationConstants.IMAGE_OVERLAP_X,
                ApplicationConstants.IMAGE_OVERLAP_Y);
    }

    public void divideImage( final Bitmap scaledBitmap ) {
        divideImage(
                scaledBitmap,
                modelConfiguration.getInputImageHeight(),
                modelConfiguration.getInputImageWidth(),
                ApplicationConstants.IMAGE_OVERLAP_X,
                ApplicationConstants.IMAGE_OVERLAP_Y);
    }

    /**
     * This method arranges divided chunks so that the chunks are ready to reconstruction of the image
     * Creates a new bitmap array and cuts every image chunk so that the overlapped sections are gone.
     * Then returns the new bitmap array to be reconstructed.
     * @param overlapX
     * @param overlapY
     * @return ArrayList<Bitmap>
     */
    public ArrayList<Bitmap> arrangeChunks(int overlapX, int overlapY){
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
        return result;
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
        int originalHeight = (chunkHeight-overlapY*2)*rows;
        int originalWidth = (chunkWidth-overlapX*2)*columns;
        Bitmap bitmap = Bitmap.createBitmap(
                originalWidth * modelConfiguration.getRescalingFactor(),
                originalHeight * modelConfiguration.getRescalingFactor(),
                Bitmap.Config.ARGB_4444);
        Log.i("ReconstructImage", String.format("New bm is created : %dx%d",bitmap.getWidth(), bitmap.getHeight()));
        Canvas canvas = new Canvas(bitmap); // this constructor causes canvas operations to write on provided bitmap

        ArrayList<Bitmap> result = prepareChunks(chunkHeight, chunkWidth,overlapX, overlapY); // Prepare the chunks to reassemble the image.


        int newChunkWidth = result.get(0).getWidth();
        int newChunkHeight = result.get(0).getHeight();
        for (int r = 0 ; r<rows; r++) // y
            for (int c =0 ; c<columns ; c++) // x
                canvas.drawBitmap(result.get(c+r*columns), c*newChunkWidth, r*newChunkHeight, null);

        Log.i("ImageCheck(): ", String.format("Bitmap size: %d %d", bitmap.getWidth(), bitmap.getHeight()));
        return bitmap;
    }

    public void processImages(Activity requestingActivity, SRModelConfiguration model_configuration) {
        int batchSize = ApplicationConstants.BATCH_SIZE;
        Bitmap[] bitmaps = new Bitmap [batchSize]; // buffer to hold input bitmaps
        BitmapProcessor bitmapProcessor = new TFLiteSuperResolver(requestingActivity, batchSize, model_configuration);
        int i = 0, nchunks = chunkImages.size();
        while(i < nchunks) {
            // load bitmaps into the array
            int j = 0;
            while(j < batchSize && i < nchunks) {
                Bitmap chunk = chunkImages.get(i++);
                // Log.i("Divided Image Sizes","Image sizes for image "+ (i++) +"  "+chunk.getWidth()+"x"+chunk.getHeight());
                bitmaps[j++] = chunk;
            }
            // process the bitmaps
            Bitmap[] outputBitmaps = bitmapProcessor.processBitmaps(bitmaps);
            // unload the bitmaps back into the list
            int k = i;
            while(j > 0)
                chunkImages.set(--k, outputBitmaps[--j]);
        }
        bitmapProcessor.close();
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

            Log.i("DivideImage","Image is divided to  "+rows+"x"+columns);
        }

    }

}
