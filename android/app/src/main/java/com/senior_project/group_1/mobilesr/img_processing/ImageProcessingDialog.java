package com.senior_project.group_1.mobilesr.img_processing;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.activities.PreprocessAndEnhanceActivity;

public class ImageProcessingDialog extends Dialog {

    private PreprocessAndEnhanceActivity creator;
    private Button cancel;
    private ProgressBar pbar;

    public ImageProcessingDialog(PreprocessAndEnhanceActivity creator) {
        super(creator);
        this.creator = creator;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.image_processing_dialog);
        cancel = findViewById(R.id.buttonImageProcessingCancel);
        pbar = findViewById(R.id.progressBarImageProcessing);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                creator.cancelImageProcessing();
            }
        });
    }

    // callback hell continues, called by the ImageProcessingTask
    public void updateProgressBar(int i) {
        pbar.setProgress(i);
    }
}
