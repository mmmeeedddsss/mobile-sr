package com.senior_project.group_1.mobilesr.activities;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.views.BitmapHelpers;

import java.util.ArrayList;

public class MultipleImageEnhanceActivity extends PreprocessAndEnhanceActivity {

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // disable process
        processButton.setEnabled(false);
    }

    public void endImageProcessing(ArrayList<Uri> outputBitmapUris) {
        // an assertion to check for the returned length
        if (BuildConfig.DEBUG && outputBitmapUris.size() == 1)
            throw new AssertionError();
        // load the images and set them as current view
        // replace new URIs where required
        for(int i = 0, len = outputBitmapUris.size(); i < len; ++i) {
            int j = (imgIndex + i) % numImages;
            imageUris.set(j, outputBitmapUris.get(i));
        }
        // set the current bitmap as view
        refreshImage();
        // clean up
        cleanUpTask();
    }

}
