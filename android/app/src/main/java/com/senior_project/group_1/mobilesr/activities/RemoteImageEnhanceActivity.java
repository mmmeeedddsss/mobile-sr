package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;
import android.util.Log;

import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;
import com.senior_project.group_1.mobilesr.img_processing.BitmapHelpers;
import com.senior_project.group_1.mobilesr.img_processing.ImageProcessingDialog;
import com.senior_project.group_1.mobilesr.img_processing.LocalImageProcessingTask;
import com.senior_project.group_1.mobilesr.img_processing.RemoteImageProcessingTask;
import com.senior_project.group_1.mobilesr.img_processing.UserSelectedBitmapInfo;
import java.util.ArrayList;

public class RemoteImageEnhanceActivity extends PreprocessAndEnhanceActivity {

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        // Overriding processAll Button, currently( also probably in future too )
        // Remote processing only a
        SRModelConfiguration modelConfiguration = SRModelConfigurationManager.getCurrentConfiguration();
        imageProcessingTask = new RemoteImageProcessingTask(this, dialog, modelConfiguration);
        processButton.setOnClickListener(v -> {
            processAllImages();
            rotateButton.setEnabled(false);
        });
    }

    @Override
    protected void setupAsyncTask() {
        dialog = new ImageProcessingDialog(this);
        SRModelConfiguration modelConfiguration = SRModelConfigurationManager.getCurrentConfiguration();
        imageProcessingTask = new RemoteImageProcessingTask(this, dialog, modelConfiguration);
    }

    @Override
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

    @Override
    protected void processImages( ArrayList<UserSelectedBitmapInfo> bmInfos ) {
        try {
            notProcessedYet = false;
            toggleFabs();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        dialog = new ImageProcessingDialog(this);
        dialog.show();

        imageProcessingTask.execute(bmInfos);
        Log.i("RemoteImageEnhanceActivity", "Called processImage of Remote Processing Activity");
    }
}
