package com.example.yuksel.mobilesr;

import android.app.Activity;
import android.os.Bundle;

public class TakePhotoActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        //Get the view From pick_photo_activity
        setContentView(R.layout.take_photo_activity);
    }

}
