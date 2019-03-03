package com.senior_project.group_1.mobilesr.configurations;

public final class ApplicationConstants {
    public static final int IMAGE_OVERLAP_X = 16; // %10 of width & height might be ideal
    public static final int IMAGE_OVERLAP_Y = 16;

    public static final double DeltaForPinchZoomScaling = 4;

    public static final String CONFIGURATION_FILE_NAME = "sr_model_configurations.xml";
    public static final int EXTERNAL_WRITE_PERMISSION_ID = 123;
    
    public static final String NOTIFICATION_CHANNEL_ID = "notif-channel";

    public static final float ZOOM_CONSTANT = 0.035F; // Constant numbers to adjust the sensitivity of user gestures
    public static final float MOVEMENT_CONSTANT = 5.5F;
}
