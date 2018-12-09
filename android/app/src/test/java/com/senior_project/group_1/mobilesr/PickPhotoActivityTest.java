package com.senior_project.group_1.mobilesr;

import android.content.Intent;
import android.widget.Button;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import static org.robolectric.Shadows.shadowOf;
import static org.junit.Assert.*;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class PickPhotoActivityTest {
    private MainActivity activity;
    private ShadowActivity shadowActivity;
    private Button pickPhotoButton;

    @Before
    public void setup() {
        activity = Robolectric.setupActivity(MainActivity.class);
        shadowActivity = shadowOf(activity);
        pickPhotoButton =  activity.findViewById(R.id.buttonPickPhoto);
    }

    @Test
    public void clickingPickPhoto_shouldOpenGallery() {
        pickPhotoButton.performClick();
        Intent actualIntent = shadowActivity.peekNextStartedActivityForResult().intent;
        Intent expectedIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }
}
