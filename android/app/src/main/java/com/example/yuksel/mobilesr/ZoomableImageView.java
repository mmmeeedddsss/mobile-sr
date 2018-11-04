package com.example.yuksel.mobilesr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import static java.lang.Math.abs;
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
    Rect src_rect;

    final float zoom_constant = 0.08F;
    final float movement_constant = 4F;

    public ZoomableImageView(Context context) {
        super(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() { // Called after init of View
        super.onFinishInflate();
    }

    @Override
    public void setImageBitmap(Bitmap bm) { // Overrided and get bitmap for custom drawing
        zoom_factor = 1;
        current_pointer_count = 0;
        this.bm = bm;
        //super.setImageBitmap(bm);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = (e.getAction() & MotionEvent.ACTION_MASK);
        switch (action){
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                current_pointer_count++;
                if (current_pointer_count == 1) {
                    float x = e.getX(0);
                    float y = e.getY(0);
                    start_x = x;
                    start_y = y;
                    Log.d("TAG", String.format("1 finger started X = %.5f, Y = %.5f", x, y));
                }
                else if (current_pointer_count == 2) {
                    // Starting distance between fingers
                    float x = e.getX(1);
                    float y = e.getY(1);
                    pinched_x = x;
                    pinched_y = y;
                    center_of_zoom_x = (start_x + pinched_x)/2.0F;
                    center_of_zoom_y = (start_y + pinched_y)/2.0F;
                    center_of_zoom_x = center_of_zoom_x/viewWidth*getWidth(src_rect)+src_rect.left;
                    center_of_zoom_y = center_of_zoom_y/viewHeight*getHeight(src_rect)+src_rect.top;
                    start_distance = getDistance(start_x,start_y,pinched_x,pinched_y);
                    Log.d("TAG", String.format("2 finger started X = %.5f, Y = %.5f", x, y));
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                current_pointer_count--;
                return true;
            case MotionEvent.ACTION_MOVE:
                if( current_pointer_count == 1 ) {
                    if( e.getHistorySize() > 0 ) {
                        float px = e.getHistoricalX(0, e.getHistorySize() - 1);
                        float py = e.getHistoricalY(0, e.getHistorySize() - 1);
                        float x = e.getX(0);
                        float y = e.getY(0);

                        center_of_zoom_x -= (x - px)*movement_constant;
                        center_of_zoom_y -= (y - py)*movement_constant;

                        if( center_of_zoom_x < 3 ) // such a magic number
                            center_of_zoom_x = 3;
                        if( center_of_zoom_x > bm.getWidth() - 4 ) // such another magic number
                            center_of_zoom_x = bm.getWidth() - 4;
                        if( center_of_zoom_y < 5 ) // wow another magic number
                            center_of_zoom_y = 5;
                        if( center_of_zoom_y > bm.getHeight() - 6 ) // omg so much magic
                            center_of_zoom_y = bm.getHeight() - 6;



                        invalidate();
                        return true;
                    }
                }
                else if( current_pointer_count == 2 ) {
                    float x = e.getX(1);
                    float y = e.getY(1);
                    current_distance = getDistance(start_x, start_y, x, y);
                    if( current_distance > start_distance )
                        zoom_factor += zoom_constant;
                    else
                        zoom_factor -= zoom_constant;
                    if(zoom_factor < 1)
                        zoom_factor = 1;
                    start_distance = current_distance;
                    invalidate();
                    return true;
                }
        }
        return super.onTouchEvent(e);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        Paint p = new Paint();
        p.setARGB(0,255,255,255);
        if( bm != null ) {
            canvas.drawRect( 0,0, viewWidth, viewHeight, p);
            src_rect = generateSourceRectange( bm.getWidth(), bm.getHeight() );
            Rect dest_rect = generateDestinationRectange( src_rect );
            canvas.drawBitmap(bm, src_rect, dest_rect, null);
        }
    }

    private Rect generateSourceRectange( int src_width, int src_height){
        float original_ratio = src_width/src_height;

        Rect src_rect = new Rect((int)(center_of_zoom_x-bm.getWidth()/zoom_factor/2),
                (int)(center_of_zoom_y-bm.getHeight()/zoom_factor/2),
                (int)(center_of_zoom_x+bm.getWidth()/zoom_factor/2),
                (int)(center_of_zoom_y+bm.getHeight()/zoom_factor/2));
        // TODO calculate center for current src rect, then calculate
        return normalize( src_rect );
    }

    private Rect generateDestinationRectange(Rect src_rect){
        Rect dest_rect = new Rect(0,0, viewWidth, viewHeight);
        int src_width = src_rect.right - src_rect.left;
        int src_height = src_rect.bottom - src_rect.top;
        float original_ratio = src_width/src_height;

        if( abs(original_ratio - viewWidth/viewHeight) < 0.01F )
            return dest_rect;

        if( viewWidth/viewHeight > original_ratio ) {
            dest_rect.left += original_ratio*viewHeight/2;
            dest_rect.right -= original_ratio*viewHeight/2;
        }
        else{
            dest_rect.top += viewWidth/original_ratio/2;
            dest_rect.bottom -= viewWidth/original_ratio/2;
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

    protected int getWidth( Rect r )
    {
        return r.right - r.left;
    }

    protected int getHeight( Rect r )
    {
        return r.bottom - r.top;
    }

    private Rect normalize(Rect src_rect) {
        src_rect.left = src_rect.left<0?0:src_rect.left;
        src_rect.top = src_rect.top<0?0:src_rect.top;
        src_rect.right = src_rect.right>bm.getWidth()? bm.getWidth(): src_rect.right;
        src_rect.bottom = src_rect.bottom>bm.getHeight()?bm.getHeight():src_rect.bottom;
        return src_rect;
    }

    public void rotate() {
        Matrix matrix = new Matrix();

        matrix.postRotate(90);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm, bm.getWidth(), bm.getHeight(),true);
        bm = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        scaledBitmap.recycle();
        invalidate();
    }
}
