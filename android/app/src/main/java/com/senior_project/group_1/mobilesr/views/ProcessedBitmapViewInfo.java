package com.senior_project.group_1.mobilesr.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

public class ProcessedBitmapViewInfo {
    Bitmap processedBitmap; // processed picture bitmap
    Rect dest_rect;
    double creationZoomFactor;

    public ProcessedBitmapViewInfo(Bitmap processedBitmap, Rect dest_rect, double creationZoomFactor) {
        this.processedBitmap = processedBitmap;
        this.dest_rect = new Rect(dest_rect);
        this.creationZoomFactor = creationZoomFactor;
    }

    public void drawOn(Canvas canvas, double currentZoomFactor) {
        canvas.drawBitmap( processedBitmap, BitmapHelpers.getBitmapRect(processedBitmap), dest_rect, null );
    }

    public void translate(double translation_x, double translation_y) {
        Log.i("ProcessedBitmapViewInfo", String.format("x : %f, y: %f", translation_x, translation_y));
        dest_rect.top += (int)translation_y*2;
        dest_rect.bottom += (int)translation_y*2;
        dest_rect.right += (int)translation_x*2;
        dest_rect.left += (int)translation_x*2;
    }
}
