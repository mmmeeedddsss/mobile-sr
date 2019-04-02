package com.senior_project.group_1.mobilesr.img_processing;

import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.activities.PreprocessAndEnhanceActivity;

public class ImageProcessingDialog extends Dialog {

    private PreprocessAndEnhanceActivity creator;
    private Button cancel;
    private ProgressBar pbar;
    private TextView textView;

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
        textView = findViewById(R.id.editTextImageProcessing);
        cancel.setOnClickListener(view -> creator.cancelImageProcessing());
    }

    // callback hell continues, called by the ImageProcessingTask
    public void updateProgressBar(String text, int i) {
        textView.setText(text);
        pbar.setProgress(i);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i("Dialog.onTouchEvent", ""+event.getAction());

        if ( event.getAction () == MotionEvent.ACTION_DOWN ) {
            Rect r = new Rect( 0, 0, 0, 0 );
            this.getWindow ().getDecorView ().getHitRect ( r );
            boolean intersects = r.contains ( (int) event.getX(), (int) event.getY() );
            if ( !intersects ) {
                this.creator.cancelImageProcessing();
                this.dismiss();
                return true;
            }
        }
        // let the system handle the event
        return super.onTouchEvent( event );
    }
}
