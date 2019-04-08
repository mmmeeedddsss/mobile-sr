package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.img_processing.UserSelectedBitmapInfo;
import com.senior_project.group_1.mobilesr.img_processing.BitmapHelpers;

import java.util.ArrayList;

public class MultipleImageEnhanceActivity extends PreprocessAndEnhanceActivity {

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // disable process
        processButton.setEnabled(false);
    }

    public void endImageProcessing(ArrayList<UserSelectedBitmapInfo> outputBmInfos) {
        // load the images and set them as current view
        // replace new URIs where required
        for(int i = 0, len = outputBmInfos.size(); i < len; ++i) {
            int j = (imgIndex + i) % numImages;
            bitmapInfos.set(j, outputBmInfos.get(i));
            BitmapHelpers.saveImageToCache(outputBmInfos.get(i),this);
        }
        // set the current bitmap as view
        refreshImage();
        // clean up
        cleanUpTask();
    }

}
