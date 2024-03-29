package com.senior_project.group_1.mobilesr.img_processing;

import android.graphics.Bitmap;
import android.util.Log;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.activities.PreprocessAndEnhanceActivity;
import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;
import com.senior_project.group_1.mobilesr.networking.ClientSocketBinary;

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

            String ip = ApplicationConstants.SERVER_IP;
            int port = ApplicationConstants.SERVER_PORT;
            Log.i("RemoteImageProcessingTask.doInBackground",
                    String.format("Sending request to  -  %s:%d", ip, port));

            for (int imgIndex = 0, len = bitmapInfos.size(); imgIndex < len; imgIndex++) {

                // server.py is pretty basic, so recreate a socket each time!
                ClientSocketBinary conn = new ClientSocketBinary(InetAddress.getByName(ip), port);

                if (bitmapInfos.get(imgIndex).isProcessed()) // cached case
                    continue;

                // try to load the bitmap first
                Bitmap bitmap = bitmapInfos.get(imgIndex).getBitmap();
                if (bitmap != null) {
                    Log.i("RemoteImageProcessingTask", "Sent Bitmap is being called !");
                    conn.sendBitmap(bitmap);
                    Log.i("RemoteImageProcessingTask", "Done Sent !");

                    if (isCancelled())
                        break;

                    bitmapInfos.get(imgIndex).setBitmap( conn.getBitmap() );
                }

                publishProgress(100 * (imgIndex + 1));

                conn.endConnection();
            }
        }
        catch ( Exception ex )
        {
            ex.printStackTrace();
            Log.e("RemoteImageProcessingTask", "Connection Error :(");
        }
        return bitmapInfos;
    }
}
