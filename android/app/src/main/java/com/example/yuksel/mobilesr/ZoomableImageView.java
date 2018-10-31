package com.example.yuksel.mobilesr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class ZoomableImageView extends AppCompatImageView {
    Bitmap bm;
    float start_x,start_y;
    float pinched_x, pinched_y;
    int viewHeight, viewWidth;
    int current_pointer_count;

    public ZoomableImageView(Context context) {
        super(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        this.bm = bm;
        super.setImageBitmap(bm);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = (e.getAction() & MotionEvent.ACTION_MASK);
        float x = e.getX();
        float y = e.getY();
        switch (action){
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                current_pointer_count++;
                if (current_pointer_count == 1) {
                    start_x = x;
                    start_y = y;
                    Log.d("TAG", String.format("POINTER ONE X = %.5f, Y = %.5f", x, y));
                }
                if (current_pointer_count == 2) {
                    // Starting distance between fingers
                    pinched_x = x;
                    pinched_y = y;
                    Log.d("TAG", String.format("POINTER TWO X = %.5f, Y = %.5f", x, y));
                }

                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                current_pointer_count--;
                break;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if( bm != null ) {
            float center_x, center_y;
            center_x = ( start_x + pinched_x )/2;
            center_y = ( start_y + pinched_y )/2;

            float scale_factor = (float) (sqrt( pow(start_x - pinched_x, 2) + pow(start_y - pinched_y, 2))/100);

            Rect src_rect = generateSourceRectange( bm.getWidth(), bm.getHeight(),
                    center_x, center_y, scale_factor );
            Rect dest_rect = generateDestinationRectange( bm.getWidth(), bm.getHeight(),
                    center_x, center_y, scale_factor, src_rect );
            canvas.drawBitmap(bm, src_rect, dest_rect, null);
        }
    }

    private Rect generateSourceRectange( int src_width, int src_height, float center_of_zoom_x,
                                              float center_of_zoom_y, float scale_factor){
        Rect src_rect = new Rect(0, 0, src_width, src_height);
        float original_ratio = src_width/src_height;

        return src_rect;
    }

    private Rect generateDestinationRectange( int src_width, int src_height, float center_of_zoom_x,
                                          float center_of_zoom_y, float scale_factor, Rect src_rect){
        Rect dest_rect = new Rect(0,0, viewWidth, viewHeight);
        if( src_width < viewWidth ) {
            dest_rect.left += (viewWidth-src_width)/2;
            dest_rect.right -= (viewWidth-src_width)/2;
        }
        if( src_height < viewHeight ) {
            dest_rect.top += (viewHeight-src_height)/2;
            dest_rect.bottom -= (viewHeight-src_height)/2;
        }
        return dest_rect;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        current_pointer_count = 0;
        viewHeight= MeasureSpec.getSize(heightMeasureSpec);
        viewWidth= MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
