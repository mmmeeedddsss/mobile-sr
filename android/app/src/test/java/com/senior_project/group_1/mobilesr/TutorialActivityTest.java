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
public class TutorialActivityTest {
    private MainActivity activity;
    private ShadowActivity shadowActivity;
    private Button tutorialButton;

    @Before
    public void setup() {
        activity = Robolectric.setupActivity(MainActivity.class);
        shadowActivity = shadowOf(activity);
        tutorialButton =  activity.findViewById(R.id.buttonTutorial);
    }

    @Test
    public void clickingTutorial_shouldStartTutorialActivity() {
        tutorialButton.performClick();
        Intent actualIntent = shadowActivity.peekNextStartedActivityForResult().intent;
        Intent expectedIntent =  new Intent(activity, TutorialActivity.class);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }
}
