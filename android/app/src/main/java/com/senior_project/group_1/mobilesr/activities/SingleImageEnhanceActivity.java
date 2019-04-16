package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;
import android.util.Log;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;
import com.senior_project.group_1.mobilesr.img_processing.LocalImageProcessingTask;
import com.senior_project.group_1.mobilesr.img_processing.UserSelectedBitmapInfo;

import java.util.ArrayList;

public class SingleImageEnhanceActivity extends PreprocessAndEnhanceActivity {

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // disable process all & prev & next
        processAllButton.setEnabled(false);
        SRModelConfiguration modelConfiguration = SRModelConfigurationManager.getCurrentConfiguration();
        imageProcessingTask = new LocalImageProcessingTask(this, dialog, modelConfiguration);
    }

    public void endImageProcessing(ArrayList<UserSelectedBitmapInfo> outputBitmapUris) {
        // an assertion to check for the returned length
        if (BuildConfig.DEBUG && outputBitmapUris.size() != 1)
            throw new AssertionError();
        // load the image and attach it to the current view

        Log.i("SingleImageEnhanceActivity", "endImageProcessing called");

        imageView.attachProcessedBitmap(outputBitmapUris.get(0).getBitmap());
        cleanUpTask();
    }

}
