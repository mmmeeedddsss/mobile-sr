package com.example.yuksel.mobilesr;

import android.graphics.Bitmap;

// An interface generalizing classes which will be able to process bitmaps. Currently
// created to allow for easy switching between TFLite and ONNX models. The interface
// consists of a single simple function which takes an input bitmap and returns the
// processed bitmap. Also a possibly empty function for doing cleanup.
public interface BitmapProcessor {
    Bitmap processBitmap(final Bitmap inputBitmap);
    void close();
}
