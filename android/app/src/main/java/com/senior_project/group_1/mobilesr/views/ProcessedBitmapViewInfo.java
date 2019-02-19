package com.senior_project.group_1.mobilesr.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;

public class ProcessedBitmapViewInfo {
    Bitmap processedBitmap; // processed picture bitmap
    Rect dest_rect;
    Rect src_rect;
    double creationZoomFactor;
    PointF corresponding_center;
    Paint paint;

    float zoom_constant = ApplicationConstants.ZOOM_CONSTANT;
    float movement_constant = ApplicationConstants.MOVEMENT_CONSTANT;

    public ProcessedBitmapViewInfo(Bitmap processedBitmap, PointF corresponding_center, double creationZoomFactor) {
        this.processedBitmap = processedBitmap;
        this.dest_rect = new Rect(dest_rect);
        this.src_rect = BitmapHelpers.getBitmapRect(processedBitmap);
        this.creationZoomFactor = creationZoomFactor;
        this.corresponding_center = new PointF(corresponding_center.x, corresponding_center.y);
        paint = new Paint();
        paint.setColor(Color.rgb(255, 0, 0));
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void drawOn(Canvas canvas, double currentZoomFactor) {
        Log.i("ProcessedBitmapViewInfo", String.format("Current Zoom=%f, initial=%f, corresponding_center=%f,%f",currentZoomFactor, creationZoomFactor, corresponding_center.x,corresponding_center.y));
        canvas.drawBitmap( processedBitmap, calculateSrcRect(currentZoomFactor), calculateDstRect(currentZoomFactor), null );
        canvas.drawRect(calculateDstRect(currentZoomFactor), paint);
    }

    private Rect calculateSrcRect(double currentZoomFactor) {
        if( creationZoomFactor < currentZoomFactor )
            return BitmapHelpers.scale(src_rect, creationZoomFactor/currentZoomFactor);
        return src_rect;
    }

    private Rect calculateDstRect(double currentZoomFactor) {
        /*if( creationZoomFactor > currentZoomFactor )
            return BitmapHelpers.scale(dest_rect, currentZoomFactor/creationZoomFactor);
        return dest_rect;*/
        return new Rect(
                (int)corresponding_center.x - processedBitmap.getWidth()/2,
                (int)corresponding_center.y - processedBitmap.getHeight()/2,
                (int)corresponding_center.x + processedBitmap.getWidth()/2,
                (int)corresponding_center.y + processedBitmap.getHeight()/2);
    }

    public void translate(double translation_x, double translation_y) {
        corresponding_center.x -= translation_x;
        corresponding_center.y -= translation_y;
    }
}
