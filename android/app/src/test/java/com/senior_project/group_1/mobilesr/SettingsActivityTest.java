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
public class SettingsActivityTest {
    private MainActivity activity;
    private ShadowActivity shadowActivity;
    private Button settingsButton;

    @Before
    public void setup() {
        activity = Robolectric.setupActivity(MainActivity.class);
        shadowActivity = shadowOf(activity);
        settingsButton =  activity.findViewById(R.id.buttonSetttings);
    }


    @Test
    public void clickingSettings_shouldStartSettingsActivity() {
        settingsButton.performClick();
        Intent actualIntent = shadowActivity.peekNextStartedActivityForResult().intent;
        Intent expectedIntent =  new Intent(activity, SettingsActivity.class);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }
}
