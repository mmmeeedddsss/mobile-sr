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
    final Rect creation_dest_rect;
    Rect src_rect;
    double creationZoomFactor;
    PointF corresponding_center;
    Paint paint;

    final PointF creationMiddlePoint;

    float zoom_constant = ApplicationConstants.ZOOM_CONSTANT;
    float movement_constant = ApplicationConstants.MOVEMENT_CONSTANT;

    public ProcessedBitmapViewInfo(Bitmap processedBitmap, PointF offset, double creationZoomFactor, Rect current_dest_rect) {
        this.processedBitmap = processedBitmap;
        this.dest_rect = new Rect(current_dest_rect);
        this.src_rect = BitmapHelpers.getBitmapRect(processedBitmap);
        this.creationZoomFactor = creationZoomFactor;
        paint = new Paint();
        paint.setColor(Color.rgb(255, 0, 0));
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        this.creationMiddlePoint = new PointF(offset.x, offset.y);
        this.creation_dest_rect = new Rect(dest_rect);
    }

    public void drawOn(Canvas canvas, double currentZoomFactor) {
        //Log.i("ProcessedBitmapViewInfo", String.format("Current Zoom=%f, initial=%f, corresponding_center=%f,%f",currentZoomFactor, creationZoomFactor, corresponding_center.x,corresponding_center.y));
        canvas.drawBitmap( processedBitmap, calculateSrcRect(currentZoomFactor), calculateDstRect(currentZoomFactor), null );
        canvas.drawRect(calculateDstRect(currentZoomFactor), paint);
    }

    private Rect calculateSrcRect(double currentZoomFactor) {
        //if( creationZoomFactor < currentZoomFactor )
        //    return BitmapHelpers.scale(src_rect, creationZoomFactor/currentZoomFactor);
        return src_rect;
    }

    private Rect calculateDstRect(double currentZoomFactor) {
        //if( creationZoomFactor > currentZoomFactor )
            return BitmapHelpers.scale(dest_rect, currentZoomFactor/creationZoomFactor, false);
        //return dest_rect;
    }

    public void setOffset(PointF currentOffset) {
        dest_rect.left = creation_dest_rect.left + (int) currentOffset.x;
        dest_rect.right = creation_dest_rect.right + (int) currentOffset.x;
        dest_rect.top = creation_dest_rect.top + (int) currentOffset.y;
        dest_rect.bottom = creation_dest_rect.bottom + (int) currentOffset.y;
    }

    public double getCreationZoomFactor() {
        return creationZoomFactor;
    }
}
