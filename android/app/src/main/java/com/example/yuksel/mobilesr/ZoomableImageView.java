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
    float center_of_zoom_x, center_of_zoom_y;
    int viewHeight, viewWidth;
    int current_pointer_count;
    float start_distance, current_distance;
    float zoom_factor;

    final float zoom_constant = 1;

    public ZoomableImageView(Context context) {
        super(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() { // Called after init of View
        super.onFinishInflate();

        zoom_factor = 1;
        current_pointer_count = 0;
    }

    @Override
    public void setImageBitmap(Bitmap bm) { // Overrided and get bitmap for custom drawing
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
                    Log.d("TAG", String.format("1 finger started X = %.5f, Y = %.5f", x, y));
                }
                if (current_pointer_count == 2) {
                    // Starting distance between fingers
                    pinched_x = x;
                    pinched_y = y;
                    center_of_zoom_x = (start_x + pinched_x)/2;
                    center_of_zoom_y = (start_y + pinched_y)/2;
                    start_distance = getDistance(start_x,start_y,pinched_x,pinched_y);
                    Log.d("TAG", String.format("2 finger started X = %.5f, Y = %.5f", x, y));
                }
                break;
            case MotionEvent.ACTION_MOVE:
                current_distance = getDistance(start_x, start_y, x, y);
                zoom_factor = (current_distance - start_distance)*zoom_constant;
                invalidate();
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

            Rect src_rect = generateSourceRectange( bm.getWidth(), bm.getHeight() );
            Rect dest_rect = generateDestinationRectange( bm.getWidth(), bm.getHeight(), src_rect );
            canvas.drawBitmap(bm, src_rect, dest_rect, null);
        }
    }

    private Rect generateSourceRectange( int src_width, int src_height){
        float original_ratio = src_width/src_height;

        Rect src_rect = new Rect(0, 0, (int)(src_width/zoom_factor), (int)(src_height/zoom_factor));
        // TODO calculate center for current src rect, then calculate
        return src_rect;
    }

    private Rect generateDestinationRectange( int src_width, int src_height, Rect src_rect){
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

    private float getDistance(float x1, float y1, float x2, float y2){
        return (float) (sqrt( pow(x1 - x2, 2) + pow(y1 - y2, 2)));
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        viewHeight= MeasureSpec.getSize(heightMeasureSpec);
        viewWidth= MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
