package com.senior_project.group_1.mobilesr.img_processing;

import android.graphics.Bitmap;
import android.util.Log;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.activities.PreprocessAndEnhanceActivity;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.networking.ClientSocketBinary;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

public class RemoteImageProcessingTask extends ImageProcessingTask {


    public RemoteImageProcessingTask(PreprocessAndEnhanceActivity requestingActivity, ImageProcessingDialog dialog, SRModelConfiguration modelConfiguration) {
        super(requestingActivity, dialog, modelConfiguration);
    }

    @Override
    protected ArrayList<UserSelectedBitmapInfo> doInBackground(ArrayList<UserSelectedBitmapInfo>... bitmapUrisArr) {
        // apparently this replaces assert in Android
        if(BuildConfig.DEBUG && bitmapUrisArr.length != 1)
            throw new AssertionError();
        ArrayList<UserSelectedBitmapInfo> bitmapInfos = bitmapUrisArr[0];
        numImages = bitmapInfos.size();

        try {
            ClientSocketBinary conn = new ClientSocketBinary(InetAddress.getByName("192.168.1.26"), 61275);
            for (int imgIndex = 0, len = bitmapInfos.size(); imgIndex < len; imgIndex++) {

                if (bitmapInfos.get(imgIndex).isProcessed()) // cached case TODO correct
                    continue;

                // try to load the bitmap first
                Bitmap bitmap = bitmapInfos.get(imgIndex).getBitmap();
                if (bitmap != null) {

                    Log.i("RemoteImageProcessingTask", "Sent Bitmap is being called !");

                    conn.sendBitmap(bitmap);

                    Log.i("RemoteImageProcessingTask", "Done Sent !");


                    // AsyncTask things
                    //publishProgress(0);
                    if (isCancelled())
                        break;

                    bitmapInfos.get(imgIndex).setBitmap( conn.getBitmap() );
                }
            }
            conn.endConnection();
        }
        catch ( Exception ex )
        {
            ex.printStackTrace();
            Log.e("RemoteImageProcessingTask", "Connection Error :(");
        }
        return bitmapInfos;
    }
}
