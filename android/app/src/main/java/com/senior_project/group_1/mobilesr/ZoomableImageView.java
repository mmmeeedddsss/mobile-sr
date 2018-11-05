package com.senior_project.group_1.mobilesr;

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
    Bitmap bm; // original picture bitmap
    float start_x,start_y; // first finger, first touch
    float pinched_x, pinched_y; // second finger, first touch
    float center_of_zoom_x, center_of_zoom_y; // center of zooming, in source rectange of drawing
    int viewHeight, viewWidth; // original width and height of view in screen, in pixels
    int current_pointer_count; // number of touching fingers
    float start_distance, current_distance; // distances between the two fingers
    float zoom_factor; // a number that represents the amount of zoom that user wanted
    Rect src_rect; // a subset of pixel coordinates that will be cropped from original bitmap,
    // then, will be scaled to fit in the dest_rect( which is not globally defined )
    // for ideal operation, src_rect's edge ratio should be same with the dest_rect

    final float zoom_constant = 0.08F; // Constant numbers to adjust the sensitivity of user gestures
    final float movement_constant = 4F;

    // Constructors
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

    // Overrided to get the original bitmap
    @Override
    public void setImageBitmap(Bitmap bm) {
        // these two state variables must be reset in each new image
        zoom_factor = 1;
        current_pointer_count = 0;

        this.bm = bm;

        // do not neet to call super.setImageBitmap
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = (e.getAction() & MotionEvent.ACTION_MASK);
        switch (action){
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN: // this is the case where a new finger is introduced
                current_pointer_count++;
                if (current_pointer_count == 1) { // if new finger is the first one
                    float x = e.getX(0);
                    float y = e.getY(0);
                    start_x = x;
                    start_y = y; // refer to comments on top, saving first finger's x and y
                    Log.d("TAG", String.format("1 finger started X = %.5f, Y = %.5f", x, y));
                }
                else if (current_pointer_count == 2) {
                    // Starting distance between fingers
                    float x = e.getX(1);
                    float y = e.getY(1);
                    pinched_x = x;
                    pinched_y = y; // refer to comments on top, saving second finger's x and y
                    // Calculation of center of the zoom in src
                    center_of_zoom_x = (start_x + pinched_x)/2.0F;
                    center_of_zoom_y = (start_y + pinched_y)/2.0F;
                    // Below, im converting the coordinates of the screen to the current subset of
                    // Original image bitmap, to find the actual location of the zoom center on
                    // original bitmap
                    center_of_zoom_x = center_of_zoom_x/viewWidth*getWidth(src_rect)+src_rect.left;
                    center_of_zoom_y = center_of_zoom_y/viewHeight*getHeight(src_rect)+src_rect.top;
                    // Current distance between fingers, note that change on this distance means
                    // User is trying to zoom in our out
                    start_distance = getDistance(start_x,start_y,pinched_x,pinched_y);
                    Log.d("TAG", String.format("2 finger started X = %.5f, Y = %.5f", x, y));
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: // User removed a finger from screen
                current_pointer_count--;
                return true;
            case MotionEvent.ACTION_MOVE:
                if( current_pointer_count == 1 ) { // If there is a one finger on screen, user is moving the image
                    if( e.getHistorySize() > 0 ) {
                        // Those are the previous x and y
                        float px = e.getHistoricalX(0, e.getHistorySize() - 1);
                        float py = e.getHistoricalY(0, e.getHistorySize() - 1);
                        // and current x,y
                        float x = e.getX(0);
                        float y = e.getY(0);

                        // Matematically, user is changing the center of the zoom by this opeartion
                        center_of_zoom_x -= (x - px)*movement_constant;
                        center_of_zoom_y -= (y - py)*movement_constant;

                        // TODO correctly implement
                        // This is temprorraly code to prevent user to change center of zoom locations
                        // to outside of the image
                        if( center_of_zoom_x < 3 ) // such a magic number
                            center_of_zoom_x = 3;
                        if( center_of_zoom_x > bm.getWidth() - 4 ) // such another magic number
                            center_of_zoom_x = bm.getWidth() - 4;
                        if( center_of_zoom_y < 5 ) // wow another magic number
                            center_of_zoom_y = 5;
                        if( center_of_zoom_y > bm.getHeight() - 6 ) // omg so much magic
                            center_of_zoom_y = bm.getHeight() - 6;

                        invalidate(); // this triggers draw method of view
                        return true;
                    }
                }
                else if( current_pointer_count == 2 ) { // This is called in zoom-in or zoom-out oprations
                    float x = e.getX(1);
                    float y = e.getY(1);
                    // TODO find a better calculation algorithm
                    current_distance = getDistance(start_x, start_y, x, y);
                    // Differantially:
                    // If distance has increased, increase zoom_factor
                    if( current_distance > start_distance )
                        zoom_factor += zoom_constant;
                    else if(current_distance < start_distance)// If distance has decrased, increase zoom_factor
                        zoom_factor -= zoom_constant;
                    if(zoom_factor < 1) // Dont allow to zoom out
                        zoom_factor = 1;
                    start_distance = current_distance;
                    invalidate();// this triggers draw method of view
                    return true;
                }
        }
        return super.onTouchEvent(e);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas); // TODO check if required to call super
        Paint p = new Paint(); // TODO dont init every time
        p.setARGB(0,255,255,255);
        if( bm != null ) {
            canvas.drawRect( 0,0, viewWidth, viewHeight, p); // clear the canvas
            src_rect = generateSourceRectange( bm.getWidth(), bm.getHeight() ); // calculate src and dsc rects
            Rect dest_rect = generateDestinationRectange( src_rect );
            canvas.drawBitmap(bm, src_rect, dest_rect, null); // draw image
            /*
            Here, we have actually 4 rectangles,
            bm : original image, can be thought as a rectangle
            View: original View size, can be thought as a rectangle
            src: Sub-rectangle of bm rectange
            dst: Sub-rectangle of view rectange
            In each draw method, I'm calculating the src rectange, considering the zoom_factor and
            center of zoom coordinates, than find the maximum sized dst rect that having the same w/h
            ratio with the src rectange. Note that src rectangle may not have same w/h ratio with bm,
            as dst may not have the same ratio with View ratio.
             */
        }
    }

    private Rect generateSourceRectange( int src_width, int src_height){
        float original_ratio = src_width/src_height;

        //TODO there are cases where I might not user bm w/h ratio, add this
        // Calculating the src rect
        Rect src_rect = new Rect((int)(center_of_zoom_x-bm.getWidth()/zoom_factor/2),
                (int)(center_of_zoom_y-bm.getHeight()/zoom_factor/2),
                (int)(center_of_zoom_x+bm.getWidth()/zoom_factor/2),
                (int)(center_of_zoom_y+bm.getHeight()/zoom_factor/2));
        return normalize( src_rect );
    }

    private Rect generateDestinationRectange(Rect src_rect){
        Rect dest_rect = new Rect(0,0, viewWidth, viewHeight);
        int src_width = src_rect.right - src_rect.left;
        int src_height = src_rect.bottom - src_rect.top;
        float original_ratio = src_width/src_height; // w/h ratio of src

        if( abs(original_ratio - viewWidth/viewHeight) < 0.01F ) // if scaling is okay, return
            return dest_rect;

        // if I need to add padding to the dest rect, add
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

    // Basic distance method
    private float getDistance(float x1, float y1, float x2, float y2){
        return (float) (sqrt( pow(x1 - x2, 2) + pow(y1 - y2, 2)));
    }

    // Overwritten method called when view size is changed
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        viewHeight= MeasureSpec.getSize(heightMeasureSpec);
        viewWidth= MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    // Basic helpers, might be migrated
    protected int getWidth( Rect r )
    {
        return r.right - r.left;
    }

    protected int getHeight( Rect r )
    {
        return r.bottom - r.top;
    }

    // TODO think about naming
    // A method that crops the src rect to be sure that it is a sub-rectangle of bm
    private Rect normalize(Rect src_rect) {
        src_rect.left = src_rect.left<0?0:src_rect.left;
        src_rect.top = src_rect.top<0?0:src_rect.top;
        src_rect.right = src_rect.right>bm.getWidth()? bm.getWidth(): src_rect.right;
        src_rect.bottom = src_rect.bottom>bm.getHeight()?bm.getHeight():src_rect.bottom;
        return src_rect;
    }

    // Method for rotating the selected image
    public void rotate() {
        Matrix matrix = new Matrix();
        matrix.postRotate(90); // Creating a rotation matrix

        // TODO possibly a memory leak, check if it is possible to do this in O(1) extra memory
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm, bm.getWidth(), bm.getHeight(),true);
        bm = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        scaledBitmap.recycle();
        invalidate(); // trigger drawing
    }
}
