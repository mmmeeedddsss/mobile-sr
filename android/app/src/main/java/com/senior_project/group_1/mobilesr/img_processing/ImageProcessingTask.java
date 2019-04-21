package com.senior_project.group_1.mobilesr.img_processing;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;
import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.activities.PreprocessAndEnhanceActivity;
import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public abstract class ImageProcessingTask extends AsyncTask<ArrayList<UserSelectedBitmapInfo>, Integer, ArrayList<UserSelectedBitmapInfo>> {

    @SuppressLint("StaticFieldLeak")
    PreprocessAndEnhanceActivity requestingActivity;
    private ImageProcessingDialog dialog;
    SRModelConfiguration modelConfiguration;
    private long startTime;
    private boolean notifActive = false;
    private NotificationCompat.Builder notifBuilder;
    private NotificationManagerCompat notifManager;
    int numImages;
    private static final int NOTIF_ID = 0;

    ImageProcessingTask(PreprocessAndEnhanceActivity requestingActivity, ImageProcessingDialog dialog, SRModelConfiguration modelConfiguration) {
        super();
        this.requestingActivity = requestingActivity;
        this.dialog = dialog;
        this.modelConfiguration = modelConfiguration;
        this.notifManager = NotificationManagerCompat.from(requestingActivity);
    }


    private void writeToSDFile(String data){

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

    protected abstract ArrayList<UserSelectedBitmapInfo> doInBackground(
            ArrayList<UserSelectedBitmapInfo>... bitmapUrisArr);

    protected void onProgressUpdate(Integer... progress) {
        int imgsDone = progress[0] / 100, imgProgress = progress[0] % 100;

        // set a proper title string for the dialog/notification
        // cannot 'cache' the string, since I do not want to call
        // format('superres in progress', imgsDone, numImages)
        Log.i("ImageProcessingTask.onProgressPudate", String.format("Progress : %d", imgProgress));
        String titleString = numImages == 0 ?
                "Superresolution in progress..." :
                String.format("Superresolving image %d/%d", imgsDone + 1, numImages);

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

    protected void onPostExecute(ArrayList<UserSelectedBitmapInfo> results) {
        // log results
        long estimatedTime = System.nanoTime() - startTime;
        Toast.makeText(requestingActivity, "Elapsed time: " + estimatedTime / 1000000 + " ms", Toast.LENGTH_LONG).show();
        // complete the notification if it was active
        if(notifActive) {
            notifBuilder.setProgress(0, 0, false)
                    .setContentTitle("Superresolution Complete")
                    .setContentText("The superresolution of your images was completed in the background.");
            notifManager.notify(NOTIF_ID, notifBuilder.build());
        }
        requestingActivity.endImageProcessing(results);
        for(UserSelectedBitmapInfo bmInfo : results) {
            Bitmap bitmap = bmInfo.getBitmap();
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
    }

}
