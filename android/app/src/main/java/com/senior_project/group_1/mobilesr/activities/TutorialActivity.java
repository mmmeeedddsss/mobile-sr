package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.views.DoubleClickListener;
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
        imgs = new int[]{
                R.drawable.img1,
                R.drawable.img2,
                R.drawable.img3,
                R.drawable.img4,
                R.drawable.img5,
                R.drawable.img6,
                R.drawable.img7,
        };
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
        iv.setClickable(true);
        iv.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Log.d("MobileSR", "single tap");
            }

            @Override
            public void onDoubleClick(View v) {
                Log.d("MobileSR", "double tap");

            }
        });
    }

    private void nextImage() {
        if (current >= imgs.length -1 )
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
