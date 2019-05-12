package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;
import com.senior_project.group_1.mobilesr.img_processing.ImageProcessingDialog;
import com.senior_project.group_1.mobilesr.img_processing.LocalImageProcessingTask;
import com.senior_project.group_1.mobilesr.img_processing.RemoteImageProcessingTask;
import com.senior_project.group_1.mobilesr.img_processing.UserSelectedBitmapInfo;
import com.senior_project.group_1.mobilesr.img_processing.BitmapHelpers;

import java.util.ArrayList;

public class MultipleImageEnhanceActivity extends PreprocessAndEnhanceActivity {

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // disable process
        processButton.setEnabled(false);
        SRModelConfiguration modelConfiguration = SRModelConfigurationManager.getCurrentConfiguration();
        imageProcessingTask = new LocalImageProcessingTask(this, dialog, modelConfiguration);
    }

    @Override
    protected void setupAsyncTask() {
        dialog = new ImageProcessingDialog(this);
        SRModelConfiguration modelConfiguration = SRModelConfigurationManager.getCurrentConfiguration();
        imageProcessingTask = new LocalImageProcessingTask(this, dialog, modelConfiguration);
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
