package com.senior_project.group_1.mobilesr;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import java.util.ArrayList;

public class ImageProcessingHelper {

    public static ArrayList<Bitmap> chunkImages; // TODO: Only for debugging purpose (DELETE later.)
    public static int columns;
    public static int rows;


    public static Bitmap reconstructImage(){
        return reconstructImage(
                ApplicationConstants.IMAGE_CHUNK_SIZE_Y,
                ApplicationConstants.IMAGE_CHUNK_SIZE_X,
                ApplicationConstants.IMAGE_OVERLAP_X,
                ApplicationConstants.IMAGE_OVERLAP_Y);
    }


    public static void divideImage( final Bitmap scaledBitmap ) {
        divideImage(
                scaledBitmap,
                ApplicationConstants.IMAGE_CHUNK_SIZE_Y,
                ApplicationConstants.IMAGE_CHUNK_SIZE_X,
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

    public static ArrayList<Bitmap> prepareChunks(int chunkHeight, int chunkWidth, int overlapX, int overlapY) {
        ArrayList<Bitmap> result = new ArrayList<>();
        for(int i=0; i<chunkImages.size(); i++) {
            result.add(Bitmap.createBitmap(chunkImages.get(i), overlapX/ApplicationConstants.MODEL_ZOOM_FACTOR, overlapY/ApplicationConstants.MODEL_ZOOM_FACTOR, chunkWidth-overlapX, chunkHeight-overlapY));
        }
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
    public static Bitmap reconstructImage(int chunkHeight, int chunkWidth, int overlapX, int overlapY) {
        int originalHeight = (chunkHeight-overlapY)*rows;
        int originalWidth = (chunkWidth-overlapX)*columns;
        Bitmap bitmap = Bitmap.createBitmap(
                originalHeight*ApplicationConstants.MODEL_ZOOM_FACTOR,
                originalWidth*ApplicationConstants.MODEL_ZOOM_FACTOR,
                Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap); // this constructor causes canvas operations to write on provided bitmap

        ArrayList<Bitmap> result = prepareChunks(chunkHeight, chunkWidth,overlapX, overlapY); // Prepare the chunks to reassemble the image.

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
        Log.i("ImageCheck(): ", String.format("Bitmap size: %d %d", bitmap.getHeight(), bitmap.getWidth()));
        return bitmap;
    }

    public static void processImages(Activity requestingActivity) {
        BitmapProcessor bitmapProcessor;
        bitmapProcessor = new TFLiteBilinearInterpolator(requestingActivity);
        for( int i=0; i<chunkImages.size(); i++ ){
            Log.i("Divided Image Sizes","Image sizes for image "+i+"  "+chunkImages.get(i).getWidth()+"x"+chunkImages.get(i).getHeight());
            chunkImages.set(i, bitmapProcessor.processBitmap(chunkImages.get(i)));
            // TODO parallelism can be used in here
        }
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
    public static void divideImage(final Bitmap scaledBitmap, int chunkHeight, int chunkWidth, int overlapX, int overlapY) throws RuntimeException{

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
            for (int coordinateY = 0; coordinateY < scaledBitmap.getHeight()-chunkHeight; coordinateY += chunkHeight - overlapY) {

                for (int coordinateX = 0; coordinateX < scaledBitmap.getWidth()-chunkWidth; coordinateX += chunkWidth - overlapX) {
                    chunkImages.add(Bitmap.createBitmap(scaledBitmap, coordinateX, coordinateY, chunkWidth, chunkHeight));
                    rows++;

                }
                columns++;
            }
            rows=rows/columns;

            Log.i("DivideImage","Image is divided to  "+rows+"x"+columns);
        }

    }

}
