package com.senior_project.group_1.mobilesr;

public final class ApplicationConstants {
    public static final int BATCH_SIZE = 1; // the number of chunks to be processed by the interpreter at a time
    public static final int IMAGE_OVERLAP_X = 16; // %10 of width & height might be ideal
    public static final int IMAGE_OVERLAP_Y = 16;
    public static final int IMAGE_CHUNK_SIZE_X = 64;
    public static final int IMAGE_CHUNK_SIZE_Y = 64;
    public static final int MODEL_ZOOM_FACTOR = 2;
}
