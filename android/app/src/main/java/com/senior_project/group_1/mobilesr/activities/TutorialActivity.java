package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.app.Activity;
import android.widget.Button;
import android.widget.ImageView;


import com.senior_project.group_1.mobilesr.R;

public class TutorialActivity extends AppCompatActivity {
    ImageView iv;
    Button nextBtn;
    Button skipBtn;
    Button prevBtn;
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
        nextBtn = findViewById(R.id.next_btn);
        skipBtn = findViewById(R.id.skip_btn);
        prevBtn = findViewById(R.id.prev_btn);
        nextBtn.setOnClickListener(v -> nextImage());
        skipBtn.setOnClickListener(v -> skipTutorial());
        prevBtn.setOnClickListener(v -> prevImage());
        prevBtn.setEnabled(false);

    }

    private void nextImage() {
        if (current >= 2 )
            finish();
        else
            iv.setImageResource(imgs[++current]);
        prevBtn.setEnabled(true);
    }

    private void prevImage() {
        iv.setImageResource(imgs[--current]);
        if (current == 0)
            prevBtn.setEnabled(false);
    }

    private void skipTutorial() {
        finish();
    }
}
