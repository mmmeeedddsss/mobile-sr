package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.app.Activity;
import android.widget.Button;
import android.widget.ImageView;


import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.views.OnSwipeTouchListener;

public class TutorialActivity extends AppCompatActivity {
    ImageView iv;
    Button skipBtn;
    int current;
    int[] imgs;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        //Get the view From pick_photo_activity
        setContentView(R.layout.tutorial_activity);
        iv = findViewById(R.id.imageView);
        current = 0;
        imgs = new int[]{R.drawable.first, R.drawable.second, R.drawable.third};
        skipBtn = findViewById(R.id.skip_btn);
        skipBtn.setOnClickListener(v -> skipTutorial());
        iv.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                nextImage();
            }
            @Override
            public void onSwipeRight() {
                prevImage();
            }
        });

    }

    private void nextImage() {
        if (current >= 2 )
            finish();
        else
            iv.setImageResource(imgs[++current]);
    }

    private void prevImage() {
        if (current == 0)
            finish();
        else
            iv.setImageResource(imgs[--current]);
    }

    private void skipTutorial() {
        finish();
    }
}
