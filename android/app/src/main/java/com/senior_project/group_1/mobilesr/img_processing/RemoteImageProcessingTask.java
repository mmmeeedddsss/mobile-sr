package com.senior_project.group_1.mobilesr.img_processing;

import android.graphics.Bitmap;
import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.activities.PreprocessAndEnhanceActivity;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
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
        for(int imgIndex = 0, len = bitmapInfos.size(); imgIndex < len; imgIndex++) {

            if( bitmapInfos.get(imgIndex).isProcessed() )
                continue;

            // try to load the bitmap first
            Bitmap bitmap = bitmapInfos.get(imgIndex).getBitmap();
            if(bitmap != null) {

                // TODO create single or multiple serialized bitmaps for sending over tcp to server

                // AsyncTask things
                publishProgress(0);
                if (isCancelled())
                    break;

                // reconstruct the image and save it
                Bitmap result = null; // TODO fill
                bitmapInfos.get(imgIndex).setBitmap(result);
            }
        }
        return bitmapInfos;
    }
}
