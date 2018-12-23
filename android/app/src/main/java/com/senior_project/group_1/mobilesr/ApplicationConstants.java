package com.senior_project.group_1.mobilesr;

public final class ApplicationConstants {
    public static final int BATCH_SIZE = 16; // the number of chunks to be processed by the interpreter at a time
    public static final int IMAGE_OVERLAP_X = 16; // %10 of width & height might be ideal
    public static final int IMAGE_OVERLAP_Y = 16;

    public static final double DeltaForPinchZoomScaling = 4;

    public static final String CONFIGURATION_FILE_NAME = "sr_model_configurations.xml";
}
