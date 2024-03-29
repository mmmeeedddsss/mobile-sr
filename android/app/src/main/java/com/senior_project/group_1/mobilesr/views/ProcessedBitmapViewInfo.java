package com.senior_project.group_1.mobilesr.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.SwitchPreference;
import android.support.v7.preference.PreferenceManager;

import com.senior_project.group_1.mobilesr.img_processing.BitmapHelpers;

public class ProcessedBitmapViewInfo {
    Bitmap processedBitmap; // processed picture bitmap
    private RectF dest_rect;
    final Rect creation_dest_rect;
    Rect src_rect;
    double creationZoomFactor;
    Paint paint;
    int rescalingFactor;

    final PointF creationMiddlePoint;

    Context context;

    public ProcessedBitmapViewInfo(Context context, Bitmap processedBitmap, PointF offset, double creationZoomFactor, Rect current_dest_rect, int rescalingFactor) {
        this.processedBitmap = processedBitmap;
        // Below, 2 is such a magic number xd
        this.dest_rect = BitmapHelpers.scale(new RectF(current_dest_rect), 2.0/rescalingFactor, false);
        this.src_rect = BitmapHelpers.getBitmapRect(processedBitmap);
        this.creationZoomFactor = creationZoomFactor;
        paint = new Paint();
        paint.setColor(Color.rgb(255, 0, 0));
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);

        this.creationMiddlePoint = new PointF(offset.x, offset.y);
        this.creation_dest_rect = new Rect(current_dest_rect);

        this.rescalingFactor = rescalingFactor;

        this.context = context;
        ;
    }

    public void renderOn(Canvas canvas, double currentZoomFactor) {
        //Log.i("ProcessedBitmapViewInfo", String.format("Current Zoom=%f, initial=%f, corresponding_center=%f,%f",currentZoomFactor, creationZoomFactor, corresponding_center.x,corresponding_center.y));
        canvas.drawBitmap(processedBitmap, calculateSrcRect(currentZoomFactor), calculateDstRect(currentZoomFactor), null);
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("draw_superresolution_border", true))
            canvas.drawRect(calculateDstRect(currentZoomFactor), paint);
    }

    public void overrideOn(Canvas canvas, PointF offset)
    {
        RectF saving_dest_rect = new RectF(
                offset.x - ((float) processedBitmap.getWidth()) / rescalingFactor,
                offset.y - ((float) processedBitmap.getHeight()) / rescalingFactor,
                offset.x + ((float) processedBitmap.getWidth()) / rescalingFactor,
                offset.y + ((float) processedBitmap.getHeight()) / rescalingFactor
        );
        canvas.drawBitmap(processedBitmap, BitmapHelpers.getBitmapRect(processedBitmap), saving_dest_rect, null);
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("draw_superresolution_border", true))
            canvas.drawRect(saving_dest_rect, paint);
    }

    private Rect calculateSrcRect(double currentZoomFactor) {
        return src_rect;
    }

    private RectF calculateDstRect(double currentZoomFactor) {
        return BitmapHelpers.scale(dest_rect, currentZoomFactor/creationZoomFactor, false);
    }

    public void setOffset(PointF currentOffset) {
        dest_rect.left = creation_dest_rect.left + currentOffset.x;
        dest_rect.right = creation_dest_rect.right + currentOffset.x;
        dest_rect.top = creation_dest_rect.top + currentOffset.y;
        dest_rect.bottom = creation_dest_rect.bottom + currentOffset.y;
    }
}
