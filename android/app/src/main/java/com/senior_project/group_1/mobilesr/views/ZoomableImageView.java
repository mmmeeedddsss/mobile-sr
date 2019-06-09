package com.senior_project.group_1.mobilesr.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;
import com.senior_project.group_1.mobilesr.img_processing.BitmapHelpers;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class ZoomableImageView extends AppCompatImageView {
    Bitmap bm; // original picture bitmap
    List<ProcessedBitmapViewInfo> processedBitmaps;
    float start_x,start_y; // first finger, first touch
    float pinched_x, pinched_y; // second finger, first touch
    float center_of_zoom_x, center_of_zoom_y; // center of zooming, in source rectange of drawing
    float center_of_zoom_x_tba, center_of_zoom_y_tba; // center of zooming, to be achieved for smooth transformations
    float previous_center_x, previous_center_y;
    int viewHeight, viewWidth; // original width and height of view in screen, in pixels
    int current_pointer_count; // number of touching fingers
    float start_distance, current_distance; // distances between the two fingers
    float zoom_factor; // a number that represents the amount of zoom that user wanted
    boolean sr_draw_flag = true;
    Rect src_rect; // a subset of pixel coordinates that will be cropped from original bitmap,
    // then, will be scaled to fit in the dest_rect
    // for ideal operation, src_rect's edge ratio should be same with the dest_rect
    Rect dest_rect;
    Paint clearing_paint; // Paint for clearing canvas on draw method

    float zoom_constant = ApplicationConstants.ZOOM_CONSTANT;
    float movement_constant = ApplicationConstants.MOVEMENT_CONSTANT;
    long lastTapTime = 0;

    // double tap constants
    //
    // tap delay between two consecutive tap events
    int tap_delay = ApplicationConstants.DOUBLE_TAP_DELAY;
    // distance between fingers to avoid taking pinch zoom as double tap mistakenly
    int tap_distance = ApplicationConstants.DOUBLE_TAP_FINGER_DISTANCE;

    // Listener object
    SwipeEventListener swipeListener;

    // translation constant
    float swipe_constant = ApplicationConstants.SWIPE_CONSTANT;

    // Constructors
    public ZoomableImageView(Context context) {
        super(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        clearing_paint = new Paint();
        clearing_paint.setARGB(0,255,255,255);
        processedBitmaps = new ArrayList<>();
    }

    public void setImageBitmap(Bitmap bm, Rect originalImageSize) {
        if( bm.getWidth() > originalImageSize.width() ) { // Means that image is at least started processing
            int rescalingFactor = SRModelConfigurationManager.getCurrentConfiguration().getRescalingFactor();
            Bitmap croppedBitmap = null;
            if( bm.getWidth() - originalImageSize.width()*rescalingFactor >= 0 ) // Means that processing is done
            {
                int paddingX = (bm.getWidth() - originalImageSize.width() * rescalingFactor) / 2;
                int paddingY = (bm.getHeight() - originalImageSize.height() * rescalingFactor) / 2;
                 croppedBitmap = Bitmap.createBitmap(bm, paddingX, paddingY,
                        originalImageSize.width() * rescalingFactor,
                        originalImageSize.height() * rescalingFactor);
                setImageBitmap(croppedBitmap);
            } else {
                int paddingX = (bm.getWidth() - originalImageSize.width()) / 2;
                int paddingY = (bm.getHeight() - originalImageSize.height()) / 2;
                croppedBitmap = Bitmap.createBitmap(bm, paddingX, paddingY,
                        originalImageSize.width(),
                        originalImageSize.height());
                setImageBitmap(croppedBitmap);
            }
            // TODO: this thing causes errors when retrieving this bitmap
            // maybe we should recycle the original bitmap and not the cropped one?
            // croppedBitmap.recycle();
        } else {
            setImageBitmap(bm);
        }
    }

    // Overrided to get the original bitmap
    @Override
    public void setImageBitmap(Bitmap bm) {
        // these two state variables must be reset in each new image
        zoom_factor = 1;
        current_pointer_count = 0;

        this.bm = bm.copy(Bitmap.Config.ARGB_8888, true);

        center_of_zoom_x = bm.getWidth()/2;
        center_of_zoom_y = bm.getHeight()/2;
        previous_center_x = center_of_zoom_x;
        previous_center_y = center_of_zoom_y;

        generateSourceRectangle(bm.getWidth(), bm.getHeight());

        invalidate();
    }

    public void setSwipeListener(SwipeEventListener listener) {
      this.swipeListener = listener;
    }

    /*
        Call this one with the resultant processed image
     */
    public void attachProcessedBitmap(Bitmap processedBitmap){
        int rescalingFactor = SRModelConfigurationManager.getCurrentConfiguration().getRescalingFactor();
        int addedPaddingX = (processedBitmap.getWidth() - BitmapHelpers.getWidth(src_rect)*rescalingFactor)/2;
        int addedPaddingY = (processedBitmap.getHeight() - BitmapHelpers.getHeight(src_rect)*rescalingFactor)/2;
        Log.i("ZoomableImageView", String.format("Received a bitmap with sizes are w:%d h:%d ", processedBitmap.getWidth(), processedBitmap.getHeight()));
        ProcessedBitmapViewInfo bmInfo = new ProcessedBitmapViewInfo(this.getContext(),
                Bitmap.createBitmap(processedBitmap, addedPaddingX, addedPaddingY,BitmapHelpers.getWidth(src_rect)*rescalingFactor, BitmapHelpers.getHeight(src_rect)*rescalingFactor),
                new PointF(src_rect.centerX(), src_rect.centerY()),
                zoom_factor, dest_rect, rescalingFactor);
        processedBitmaps.add( bmInfo );
        invalidate();
        // TODO delete processedBitmap?
    }

    public void toggleSrDrawal()
    {
        sr_draw_flag = !sr_draw_flag;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = (e.getAction() & MotionEvent.ACTION_MASK);
        switch (action){
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN: // this is the case where a new finger is introduced
                current_pointer_count++;
                if (System.currentTimeMillis() - lastTapTime <= tap_delay
                    && Math.abs(start_x-e.getX(0)) < tap_distance
                    && Math.abs(start_y-e.getY(0)) < tap_distance
                    && current_pointer_count == 1) {
                    // double tap occurred
                    Log.d("MobileSR", "double tap");
                    center_of_zoom_x_tba = start_x / viewWidth * BitmapHelpers.getWidth(src_rect) + src_rect.left;
                    center_of_zoom_y_tba = start_y / viewHeight * BitmapHelpers.getHeight(src_rect) + src_rect.top;
                    double_tap_action(center_of_zoom_x_tba, center_of_zoom_y_tba);
                } else {
                    lastTapTime = System.currentTimeMillis();
                    if (current_pointer_count == 1) { // if new finger is the first one
                        float x = e.getX(0);
                        float y = e.getY(0);
                        start_x = x;
                        start_y = y; // refer to comments on top, saving first finger's x and y
                        Log.d("TAG", String.format("1 finger started X = %.5f, Y = %.5f", x, y));
                    } else if (current_pointer_count == 2) {
                        // Starting distance between fingers
                        float x = e.getX(1);
                        float y = e.getY(1);
                        pinched_x = x;
                        pinched_y = y; // refer to comments on top, saving second finger's x and y
                        // ----------------
                        // Calculation of center of the zoom in src
                        center_of_zoom_x_tba = (start_x + pinched_x) / 2.0F;
                        center_of_zoom_y_tba = (start_y + pinched_y) / 2.0F;
                        // Below, im converting the coordinates of the screen to the current subset of
                        // Original image bitmap, to find the actual location of the zoom center on
                        // original bitmap
                        center_of_zoom_x_tba = center_of_zoom_x_tba / viewWidth * BitmapHelpers.getWidth(src_rect) + src_rect.left;
                        center_of_zoom_y_tba = center_of_zoom_y_tba / viewHeight * BitmapHelpers.getHeight(src_rect) + src_rect.top;
                        // Current distance between fingers, note that change on this distance means
                        // User is trying to zoom in our out
                        // -----------------
                        start_distance = getDistance(start_x, start_y, pinched_x, pinched_y);
                        Log.d("TAG", String.format("2 finger started X = %.5f, Y = %.5f", x, y));
                    }
                }
                return true;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: // User removed a finger from screen
                current_pointer_count--;
                // potentially trigger swipe event
                if (zoom_factor == 1.0) {
                    float last_x = e.getX(0);
                    float x_translation = last_x-start_x;
                    Log.d("MobileSR", "swipe with translation: "+x_translation);
                    // a swipe will happen
                    if (Math.abs(x_translation) >= swipe_constant) {
                        if (x_translation > 0) {
                            swipeListener.swipeRight();
                            return true;
                        }
                        else {
                            swipeListener.swipeLeft();
                            return true;
                        }
                    }
                    return true;

                }
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


                        double translation_x = (x - px)*(BitmapHelpers.getWidth(src_rect)/(float) BitmapHelpers.getWidth(dest_rect))*movement_constant;
                        double translation_y = (y - py)*(BitmapHelpers.getWidth(src_rect)/(float) BitmapHelpers.getWidth(dest_rect))*movement_constant;

                        // Matematically, user is changing the center of the zoom by this opeartion
                        center_of_zoom_x -= translation_x;
                        center_of_zoom_y -= translation_y;

                        align_center_of_zoom(); // prevent if center of zoom is outside of the bm,

                        center_of_zoom_x_tba = center_of_zoom_x;
                        center_of_zoom_y_tba = center_of_zoom_y;

                        invalidate(); // this triggers draw method of view
                        return true;
                    }
                }
                else if( current_pointer_count == 2 ) { // This is called in zoom-in or zoom-out oprations
                    float x = e.getX(1);
                    float y = e.getY(1);
                    current_distance = getDistance(start_x, start_y, x, y);
                    //Log.i("Distances on screen", String.format("%f , %f",current_distance, start_distance));

                    // Differantially:
                    // If distance has increased, increase zoom_factor
                    if( current_distance > start_distance + ApplicationConstants.DeltaForPinchZoomScaling )
                    {
                        zoom_factor += zoom_constant*zoom_factor;
                        start_distance = current_distance;
                        invalidate();// this triggers draw method of view
                    }
                    else if(current_distance < start_distance - ApplicationConstants.DeltaForPinchZoomScaling )// If distance has decrased, increase zoom_factor
                    {
                        zoom_factor -= zoom_constant * zoom_factor;
                        start_distance = current_distance;
                        invalidate();// this triggers draw method of view
                    }
                    if(zoom_factor < 1) // Dont allow to zoom out
                        zoom_factor = 1;
                    return true;
                }
        }
        return super.onTouchEvent(e);
    }

    private void align_center_of_zoom() {
        // Code to prevent user to change center of zoom locations
        // to outside of the image
        if( center_of_zoom_x < BitmapHelpers.getWidth(src_rect)/2 )
            center_of_zoom_x = BitmapHelpers.getWidth(src_rect)/2;
        if( center_of_zoom_x > bm.getWidth() - BitmapHelpers.getWidth(src_rect)/2 )
            center_of_zoom_x = bm.getWidth() - BitmapHelpers.getWidth(src_rect)/2;
        if( center_of_zoom_y < BitmapHelpers.getHeight(src_rect)/2 )
            center_of_zoom_y = BitmapHelpers.getHeight(src_rect)/2;
        if( center_of_zoom_y > bm.getHeight() - BitmapHelpers.getHeight(src_rect)/2 )
            center_of_zoom_y = bm.getHeight() - BitmapHelpers.getHeight(src_rect)/2;
    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void draw(Canvas canvas) {
        if( bm != null ) {
            canvas.drawRect( 0,0, viewWidth, viewHeight, clearing_paint); // clear the canvas
            iterateCenterOfZoom();
            src_rect = generateSourceRectangle( bm.getWidth(), bm.getHeight() ); // calculate src and dsc rects
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

            // After drawing the original one, draw the processed bm to top of that if present

            for( ProcessedBitmapViewInfo bmInfo : processedBitmaps )
                bmInfo.setOffset( getDestCoordinatesOfPixelwrpCenter(bmInfo.creationMiddlePoint) );

            previous_center_x = center_of_zoom_x;
            previous_center_y = center_of_zoom_y;

            if( sr_draw_flag ) {
                for (ProcessedBitmapViewInfo bmInfo : processedBitmaps) {
                    bmInfo.renderOn(canvas, zoom_factor);
                }
            }
        }
    }

    public PointF getDestCoordinatesOfPixelwrpCenter(PointF pixel){
        return getDestCoordinatesOfPixelwrpCenter(pixel,src_rect, dest_rect);
    }

    public PointF getDestCoordinatesOfPixelwrpCenter(PointF pixel, Rect src_rect, Rect dest_rect){
        PointF coords = new PointF();
        coords.x = ( pixel.x - ( ((float)src_rect.right + (float)src_rect.left)/2  ) )/BitmapHelpers.getWidth(src_rect)*BitmapHelpers.getWidth(dest_rect);
        coords.y = ( pixel.y - ( ((float)src_rect.bottom + (float)src_rect.top)/2 ) )/BitmapHelpers.getHeight(src_rect)*BitmapHelpers.getHeight(dest_rect);
        return coords;
    }

    private void iterateCenterOfZoom()
    {
        center_of_zoom_x += (center_of_zoom_x_tba - center_of_zoom_x)/10.0;
        center_of_zoom_y += (center_of_zoom_y_tba - center_of_zoom_y)/10.0;
        center_of_zoom_x_tba -= (center_of_zoom_x_tba - center_of_zoom_x)/10.0;
        center_of_zoom_y_tba -= (center_of_zoom_y_tba - center_of_zoom_y)/10.0;
    }

    private Rect generateSourceRectangle(int src_width, int src_height){
        Rect src_rect = new Rect((int)((int)center_of_zoom_x-bm.getWidth()/zoom_factor/2),
                (int)((int)center_of_zoom_y-bm.getHeight()/zoom_factor/2),
                (int)((int)center_of_zoom_x+bm.getWidth()/zoom_factor/2),
                (int)((int)center_of_zoom_y+bm.getHeight()/zoom_factor/2));
        return fit_in_original_bm( src_rect );
    }

    private Rect generateDestinationRectange(Rect src_rect){
        Rect dest_rect = new Rect(0,0, viewWidth, viewHeight);

        float original_ratio = BitmapHelpers.getWidth(src_rect)/(float) BitmapHelpers.getHeight(src_rect); // w/h ratio of src

        if( abs(original_ratio - viewWidth/(float)viewHeight) < 0.00001F ) // if scaling is okay, return
            return dest_rect;

        // if I need to add padding to the dest rect, add
        if( viewWidth/viewHeight > original_ratio ) {
            if( viewWidth/(float)viewHeight > bm.getWidth()/(float) BitmapHelpers.getHeight(src_rect) ) {// what we can get at most still not enough
                src_rect.left = 0;
                src_rect.right = bm.getWidth(); // do that best scenario
                original_ratio = BitmapHelpers.getWidth(src_rect)/(float) BitmapHelpers.getHeight(src_rect);
                dest_rect.left += original_ratio*viewHeight/2; // then add padding to the screen
                dest_rect.right -= original_ratio*viewHeight/2;

            }
            else{ // We can adjust the src as we want
                int desired_width = (int)((viewWidth/(float)viewHeight)* BitmapHelpers.getHeight(src_rect));
                int previous_width = BitmapHelpers.getWidth(src_rect);
                src_rect.top -= (desired_width - previous_width)/2; // then adjust it
                src_rect.bottom += (desired_width - previous_width)/2;
            }
        }
        else{ // viewWidth/viewHeight < original_ratio -- increasing src height or decerasing dest height
            if( viewWidth/(float)viewHeight < BitmapHelpers.getWidth(src_rect)/(float)bm.getHeight() ) {// what we can get at most
                src_rect.top = 0;
                src_rect.bottom = bm.getHeight();
                original_ratio = BitmapHelpers.getWidth(src_rect)/(float) BitmapHelpers.getHeight(src_rect);
                dest_rect.top = (int)(viewHeight/2 - viewWidth/original_ratio/2);
                dest_rect.bottom = (int)(viewHeight/2 + viewWidth/original_ratio/2);

            }
            else{ // In this case, desired src H is
                int desired_height = (int)(BitmapHelpers.getWidth(src_rect)/(viewWidth/(float)viewHeight));
                int previous_height = BitmapHelpers.getHeight(src_rect);
                src_rect.top -= (desired_height - previous_height)/2;
                src_rect.bottom += (desired_height - previous_height)/2;
            }
        }
        fit_in_original_bm(src_rect);
        this.dest_rect = dest_rect;
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

    // A method that shifts the src rect to be sure that it is a sub-rectangle of bm
    private Rect fit_in_original_bm(Rect src_rect) {

        if( src_rect.left < 0 ){
            src_rect.right += -src_rect.left;
            src_rect.left = 0;
        }
        if( src_rect.right >= bm.getWidth() ){
            src_rect.left -= src_rect.right - bm.getWidth() + 1;
            src_rect.right = bm.getWidth() - 1;
        }
        if( src_rect.top < 0 ){
            src_rect.bottom += -src_rect.top;
            src_rect.top = 0;
        }
        if( src_rect.bottom >= bm.getHeight() ){
            src_rect.top -= src_rect.bottom - bm.getHeight() + 1;
            src_rect.bottom = bm.getHeight() - 1;
        }

        // second pass

        if( src_rect.left < 0 || src_rect.right >= bm.getWidth() ){
            src_rect.left = 0;
            src_rect.right = bm.getWidth() - 1;
        }
        if( src_rect.top < 0 || src_rect.bottom >= bm.getHeight()  ){
            src_rect.top = 0;
            src_rect.bottom = bm.getHeight() - 1;
        }

        return src_rect;
    }

    // puts default values to pinch zoom variables and then redraws
    private void double_tap_action( float new_center_of_zoom_x, float new_center_of_zoom_y ) {
        if( zoom_factor > 1.2 ) {
            zoom_factor = 1;
            previous_center_x = center_of_zoom_x;
            previous_center_y = center_of_zoom_y;
            center_of_zoom_x = bm.getWidth() / 2;
            center_of_zoom_y = bm.getHeight() / 2;
        } else {
            zoom_factor *= 3;
            previous_center_x = center_of_zoom_x;
            previous_center_y = center_of_zoom_y;
            center_of_zoom_x = new_center_of_zoom_x;
            center_of_zoom_y = new_center_of_zoom_y;
        }
        invalidate();
    }

    // Method for rotating the selected image
    public void rotate() {
        Matrix matrix = new Matrix();
        matrix.postRotate(-90); // Creating a rotation matrix
        bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        invalidate(); // trigger drawing
    }

    /**
     * CALL RECYCLE AFTER YOUR JOB IS DONE WITH THE RETURNED BITMAP
     */
    public Bitmap getFullBitmap(){
        Log.i("ZoomableImageView", String.format("Bm size: %dx%d",bm.getWidth(), bm.getHeight()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm,bm.getWidth()*2, bm.getHeight()*2,false);
        Log.i("ZoomableImageView", String.format("ResizedBm size: %dx%d",resizedBitmap.getWidth(), resizedBitmap.getHeight()));
        Canvas fullCanvas = new Canvas(resizedBitmap);

        for (ProcessedBitmapViewInfo bmInfo : processedBitmaps) {
            Log.i("ZoomableImageView", String.format("Creation Offset: %f,%f", bmInfo.creationMiddlePoint.x, bmInfo.creationMiddlePoint.y));
            bmInfo.overrideOn(fullCanvas, new PointF(bmInfo.creationMiddlePoint.x*2, bmInfo.creationMiddlePoint.y*2));
        }
        return resizedBitmap;
    }

    public ArrayList<Bitmap> getProcessedBitmaps(){
        ArrayList<Bitmap> bms = new ArrayList<>();
        for (ProcessedBitmapViewInfo bmInfo : processedBitmaps) {
            bms.add(bmInfo.processedBitmap);
        }
        return bms;
    }


    public Bitmap getCurrentBitmap() {
        return BitmapHelpers.cropBitmapUsingSubselection(this.bm, src_rect);
    }

    // Listener for swipe events
    // used for next and prev images
    public interface SwipeEventListener {

      // swipe left method
      // for next image
      public void swipeLeft();
      // swipe right method
      // for prev image
      public void swipeRight();
    }
}
