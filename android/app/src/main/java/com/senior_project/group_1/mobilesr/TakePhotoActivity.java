package com.senior_project.group_1.mobilesr;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class TakePhotoActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        //Get the view From pick_photo_activity
        setContentView(R.layout.take_photo_activity);
    }

}
