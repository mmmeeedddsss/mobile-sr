package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.img_processing.UserSelectedBitmapInfo;

import java.util.ArrayList;

public class SingleImageEnhanceActivity extends PreprocessAndEnhanceActivity {

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // disable process all & prev & next
        processAllButton.setEnabled(false);
    }

    public void endImageProcessing(ArrayList<UserSelectedBitmapInfo> outputBitmapUris) {
        // an assertion to check for the returned length
        if (BuildConfig.DEBUG && outputBitmapUris.size() != 1)
            throw new AssertionError();
        // load the image and attach it to the current view
        imageView.attachProcessedBitmap(outputBitmapUris.get(0).getBitmap());
        cleanUpTask();
    }

}
