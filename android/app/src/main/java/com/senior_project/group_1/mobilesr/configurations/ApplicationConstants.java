package com.senior_project.group_1.mobilesr.configurations;

public final class ApplicationConstants {
    // TODO: use build related variable instead of application constant?
    // to be useed for debugging. If set, persistent things like model configurations
    // and preferences are deleted when starting the app.
    public static final boolean RESET_PERSISTENT = true;

    public static final int IMAGE_OVERLAP_X = 16; // %10 of width & height might be ideal
    public static final int IMAGE_OVERLAP_Y = 16;

    public static final double DeltaForPinchZoomScaling = 4;

    public static final String CONFIGURATION_FILE_NAME = "sr_model_configurations.xml";
    public static final int EXTERNAL_WRITE_PERMISSION_ID = 123;
    
    public static final String NOTIFICATION_CHANNEL_ID = "notif-channel";

    public static final float ZOOM_CONSTANT = 0.035F; // Constant numbers to adjust the sensitivity of user gestures
    public static final float MOVEMENT_CONSTANT = 5.5F;

    // distance between fingers to avoid taking pinch zoom as double tap mistakenly
    public static final int DOUBLE_TAP_FINGER_DISTANCE = 100; 
    // time delay between two consecutive tap events
    public static final int DOUBLE_TAP_DELAY = 300;

    // Threshold distance for swipe event for x-axis
    public static final float SWIPE_CONSTANT = 90.0f;

    // The default model to be selected when first opening the app
    public static final String DEFAULT_MODEL = "SRCNN_NR_256";

    // hardcoded server paremeters
    public static final String SERVER_IP = "10.0.2.2"; // change this IP
    public static final int SERVER_PORT = 61275;
}
