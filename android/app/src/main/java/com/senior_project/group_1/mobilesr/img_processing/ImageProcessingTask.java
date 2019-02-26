package com.senior_project.group_1.mobilesr.img_processing;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
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

public class ImageProcessingTask extends AsyncTask<ArrayList<Uri>, Integer, ArrayList<Uri>> {

    private ArrayList<Bitmap> chunkImages; // TODO: Only for debugging purpose (DELETE later.)
    private int columns;
    private int rows;
    private PreprocessAndEnhanceActivity requestingActivity;
    private ImageProcessingDialog dialog;
    private SRModelConfiguration modelConfiguration;
    private long startTime;
    private boolean notifActive = false;
    private NotificationCompat.Builder notifBuilder;
    private NotificationManagerCompat notifManager;
    private int numImages;

    private static final int NOTIF_ID = 0;

    public ImageProcessingTask(PreprocessAndEnhanceActivity requestingActivity, ImageProcessingDialog dialog, SRModelConfiguration modelConfiguration) {
        super();
        this.requestingActivity = requestingActivity;
        this.dialog = dialog;
        this.modelConfiguration = modelConfiguration;
        this.notifManager = NotificationManagerCompat.from(requestingActivity);
    }

    public void writeToSDFile(String data){

        // Find the root of the external storage.
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

        File root = android.os.Environment.getExternalStorageDirectory();
        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        File file = new File(root.getAbsolutePath(), "statistics.txt");

        try {
            file.createNewFile();
            FileOutputStream f = new FileOutputStream(file,true);
            PrintWriter pw = new PrintWriter(f);
            pw.println(data);
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onPreExecute() {
        // log the starting time
        startTime = System.nanoTime();
    }

    protected ArrayList<Uri> doInBackground(ArrayList<Uri>... bitmapUrisArr) {
        // apparently this replaces assert in Android
        if(BuildConfig.DEBUG && bitmapUrisArr.length != 1)
            throw new AssertionError();
        // generate and store result
        int batchSize = modelConfiguration.getNumParallelBatch();
        BitmapProcessor bitmapProcessor = new TFLiteSuperResolver(requestingActivity,
                batchSize, modelConfiguration);
        ArrayList<Uri> resultUris = new ArrayList<>();
        ArrayList<Uri> bitmapUris = bitmapUrisArr[0];
        numImages = bitmapUris.size();
        for(int imgIndex = 0, len = bitmapUris.size(); imgIndex < len; imgIndex++) {
            // try to load the bitmap first
            Bitmap bitmap = loadBitmap(bitmapUris.get(imgIndex));
            if(bitmap != null) {
                // the bitmap was loaded successfully
                divideImage(reflectpadBitmap(bitmap));
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
                    Bitmap[] outputBitmaps = bitmapProcessor.processBitmaps(bitmaps);
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
                String path = MediaStore.Images.Media.insertImage(
                        requestingActivity.getContentResolver(), result, "", "");
                Uri resultUri = Uri.parse(path);
                resultUris.add(resultUri);
            }
        }
        bitmapProcessor.close();
        return resultUris;
    }

    protected void onProgressUpdate(Integer... progress) {
        int imgsDone = progress[0] / 100, imgProgress = progress[0] % 100;

        // set a proper title string for the dialog/notification
        // cannot 'cache' the string, since I do not want to call
        // format('superres in progress', imgsDone, numImages)
        String titleString = numImages > 0 ?
                "Superresolution in progress..." :
                String.format("Superresolving image %d/%d", imgsDone, numImages);

        if(requestingActivity.inBackground()) {
            // activate the notification bar
            notifActive = true;
            notifBuilder = new NotificationCompat.Builder(requestingActivity, ApplicationConstants.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon);
        }
        else {
            // update the visible progress bar
            dialog.updateProgressBar(titleString, imgProgress);
        }

        // update the notification progress bar if the app was put in the background at least once
        if(notifActive) {
            notifBuilder.setContentTitle(titleString)
                    .setProgress(100, imgProgress, false);
            notifManager.notify(NOTIF_ID, notifBuilder.build());
        }
    }

    protected void onPostExecute(ArrayList<Uri> result) {
        // log results
        long estimatedTime = System.nanoTime() - startTime;
        Toast.makeText(requestingActivity, "Elapsed time: " + estimatedTime / 1000000 + " ms", Toast.LENGTH_LONG).show();
        requestingActivity.endImageProcessing(result);
        for(Uri uri : result) {
            Bitmap bitmap = loadBitmap(uri);
            if(bitmap != null) {
                writeToSDFile(
                        String.format(Locale.ENGLISH, "Time: %s | Conf: %s | dT: %d | I: %dx%d",
                                Calendar.getInstance().getTime(),
                                modelConfiguration.toString(),
                                estimatedTime,
                                bitmap.getWidth() / modelConfiguration.getRescalingFactor(),
                                bitmap.getHeight() / modelConfiguration.getRescalingFactor()));
            }
        }
        // complete the notification if it was active
        if(notifActive) {
            notifBuilder.setProgress(0, 0, false)
                    .setContentTitle("Superresolution Complete")
                    .setContentText("The superresolution of your images was completed in the background.");
            notifManager.notify(NOTIF_ID, notifBuilder.build());
        }
    }

    private Bitmap loadBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(
                    requestingActivity.getContentResolver(), uri);
        }
        catch(Exception ex) {
            Log.e("ImageProcessingTask",
                    "Error while loading image bitmap from URI, skipped image", ex);
        }
        return bitmap;
    }

    private Bitmap reflectpadBitmap(Bitmap bitmap) {
        int h = bitmap.getHeight(), w = bitmap.getWidth();
        int ih = modelConfiguration.getInputImageHeight(),
            iw = modelConfiguration.getInputImageWidth();
        int ox = ApplicationConstants.IMAGE_OVERLAP_X,
            oy = ApplicationConstants.IMAGE_OVERLAP_Y;

        int up = 0, bp = 0;
        int hrem = h % (ih - oy);
        if(hrem != 0) {
            up = hrem / 2;
            bp = hrem - up;
        }

        int lp = 0, rp = 0;
        int wrem = w % (iw - ox);
        if(wrem != 0) {
            lp = wrem / 2;
            rp = wrem - lp;
        }

        int nh = up + h + bp, nw = lp + w + rp;
        int nsize = nh * nw;
        Log.i("ImageProcessingTask",
              String.format("Calculated padded size as: %dX%d", nw, nh));

        return bitmap;
    }

    private Bitmap reconstructImage(){
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
