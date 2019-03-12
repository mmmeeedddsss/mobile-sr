package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.UserSelectedBitmapInfo;
import com.senior_project.group_1.mobilesr.views.BitmapHelpers;

import java.util.ArrayList;

public class MultipleImageEnhanceActivity extends PreprocessAndEnhanceActivity {

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // disable process
        processButton.setEnabled(false);
    }

    public void endImageProcessing(ArrayList<UserSelectedBitmapInfo> outputBmInfos) {
        // an assertion to check for the returned length
        if (BuildConfig.DEBUG && outputBmInfos.size() == 1)
            throw new AssertionError();
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
