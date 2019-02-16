package com.senior_project.group_1.mobilesr.views;

import android.annotation.SuppressLint;
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

import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class ZoomableImageView extends AppCompatImageView {
    Bitmap bm; // original picture bitmap
    List<ProcessedBitmapViewInfo> processedBitmaps;
    float start_x,start_y; // first finger, first touch
    float pinched_x, pinched_y; // second finger, first touch
    float center_of_zoom_x, center_of_zoom_y; // center of zooming, in source rectange of drawing
    float center_of_zoom_x_tba, center_of_zoom_y_tba; // center of zooming, to be achieved for smooth transformations
    int viewHeight, viewWidth; // original width and height of view in screen, in pixels
    int current_pointer_count; // number of touching fingers
    float start_distance, current_distance; // distances between the two fingers
    float zoom_factor; // a number that represents the amount of zoom that user wanted
    Rect src_rect; // a subset of pixel coordinates that will be cropped from original bitmap,
    // then, will be scaled to fit in the dest_rect
    // for ideal operation, src_rect's edge ratio should be same with the dest_rect
    Rect dest_rect;
    Paint clearing_paint; // Paint for clearing canvas on draw method

    float zoom_constant = 0.035F; // Constant numbers to adjust the sensitivity of user gestures
    float movement_constant = 5.5F;

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

    // Overrided to get the original bitmap
    @Override
    public void setImageBitmap(Bitmap bm) {
        // these two state variables must be reset in each new image
        zoom_factor = 1;
        current_pointer_count = 0;

        this.bm = bm.copy(Bitmap.Config.ARGB_8888, true);;

        center_of_zoom_x = bm.getWidth()/2;
        center_of_zoom_y = bm.getHeight()/2;

        generateSourceRectangle(bm.getWidth(), bm.getHeight());

        invalidate();
    }

    /*
        Call this one with the resultant processed image
     */
    public void attachProcessedBitmap(Bitmap processedBitmap){
        int addedPaddingX = processedBitmap.getWidth()/2 - BitmapHelpers.getWidth(src_rect);
        int addedPaddingY = processedBitmap.getHeight()/2 - BitmapHelpers.getHeight(src_rect);
        Log.i("ZoomableImageView", String.format("processedBitmap sizes are w:%d h:%d ", processedBitmap.getWidth(), processedBitmap.getHeight()));
        ProcessedBitmapViewInfo bmInfo = new ProcessedBitmapViewInfo(
                Bitmap.createBitmap(processedBitmap, addedPaddingX/2,addedPaddingY/2,BitmapHelpers.getWidth(src_rect)*2, BitmapHelpers.getHeight(src_rect)*2),
                dest_rect, zoom_factor);
        processedBitmaps.add( bmInfo );
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
                    // ----------------
                    // Calculation of center of the zoom in src
                    center_of_zoom_x_tba = (start_x + pinched_x)/2.0F;
                    center_of_zoom_y_tba = (start_y + pinched_y)/2.0F;
                    // Below, im converting the coordinates of the screen to the current subset of
                    // Original image bitmap, to find the actual location of the zoom center on
                    // original bitmap
                    center_of_zoom_x_tba = center_of_zoom_x_tba/viewWidth* BitmapHelpers.getWidth(src_rect)+src_rect.left;
                    center_of_zoom_y_tba = center_of_zoom_y_tba/viewHeight* BitmapHelpers.getHeight(src_rect)+src_rect.top;
                    // Current distance between fingers, note that change on this distance means
                    // User is trying to zoom in our out
                    // -----------------
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

                        double translation_x = (x - px)*(BitmapHelpers.getWidth(src_rect)/(float) BitmapHelpers.getWidth(dest_rect))*movement_constant;
                        double translation_y = (y - py)*(BitmapHelpers.getWidth(src_rect)/(float) BitmapHelpers.getWidth(dest_rect))*movement_constant;

                        // Matematically, user is changing the center of the zoom by this opeartion
                        center_of_zoom_x -= translation_x;
                        center_of_zoom_y -= translation_y;

                        for( ProcessedBitmapViewInfo bmInfo : processedBitmaps )
                            bmInfo.translate(translation_x, translation_y);

                        //center_of_zoom_x_tba = center_of_zoom_x;
                        //center_of_zoom_y_tba = center_of_zoom_y;

                        align_center_of_zoom(); // prevent if center of zoom is outside of the bm,

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

            for( ProcessedBitmapViewInfo bmInfo : processedBitmaps ) {
                bmInfo.drawOn(canvas, zoom_factor);
            }
        }
    }

    private void iterateCenterOfZoom()
    {
        //center_of_zoom_x += (center_of_zoom_x_tba - center_of_zoom_x);
        //center_of_zoom_y += (center_of_zoom_y_tba - center_of_zoom_y);
        //center_of_zoom_x_tba -= (center_of_zoom_x_tba - center_of_zoom_x)/10;
        //center_of_zoom_y_tba -= (center_of_zoom_y_tba - center_of_zoom_y)/10;
    }

    private Rect generateSourceRectangle(int src_width, int src_height){
        Rect src_rect = new Rect((int)(center_of_zoom_x-bm.getWidth()/zoom_factor/2),
                (int)(center_of_zoom_y-bm.getHeight()/zoom_factor/2),
                (int)(center_of_zoom_x+bm.getWidth()/zoom_factor/2),
                (int)(center_of_zoom_y+bm.getHeight()/zoom_factor/2));
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
    public Bitmap getCurrentBitmap() {
        if( bm != null ) {
            // Main challenge here is selecting a sub-bitmap that fits the model constraints

            // -> Integer division causes cropping, maybe increasing those divisions
            // with proper handling might be work( also handle setImage, crop the reconstructed one )
            // Overlap*(n+1)+(chunk_size-2*overlap)*n = size_y
            // overlap + (chunk_size-overlap)*n = size_y
            // (size_y-overlap)/(chunk_size-overlap) = n
            // + 1 is selecting a bigger area

            int modelInputSizeX = SRModelConfigurationManager.getCurrentConfiguration().getInputImageWidth();
            int modelInputSizeY = SRModelConfigurationManager.getCurrentConfiguration().getInputImageWidth();

            int chunkCountForX = (BitmapHelpers.getWidth(src_rect)-ApplicationConstants.IMAGE_OVERLAP_X*2)
                    / (modelInputSizeX-ApplicationConstants.IMAGE_OVERLAP_X*2) + 1;
            int chunkCountForY = (BitmapHelpers.getHeight(src_rect)-ApplicationConstants.IMAGE_OVERLAP_Y*2)
                    / (modelInputSizeY-ApplicationConstants.IMAGE_OVERLAP_Y*2) + 1;

            Log.i("ZoomableImageView", chunkCountForX + " - " + chunkCountForY);

            if( chunkCountForX > 0 && chunkCountForY > 0 ){
                int subselectionMiddleX = BitmapHelpers.getWidth(src_rect)/2 + src_rect.left;
                int subselectionMiddleY = BitmapHelpers.getHeight(src_rect)/2 + src_rect.top;
                int subselectionWidth = chunkCountForX*(modelInputSizeX-ApplicationConstants.IMAGE_OVERLAP_X*2)
                        + ApplicationConstants.IMAGE_OVERLAP_X*2;
                int subselectionHeight = chunkCountForY*(modelInputSizeY-ApplicationConstants.IMAGE_OVERLAP_Y*2)
                        + ApplicationConstants.IMAGE_OVERLAP_Y*2;
                Log.i("ZoomableImageView", String.format(" subselection w:%d h:%d ", subselectionWidth, subselectionHeight));
                Log.i("ZoomableImageView", String.format(" while src_size is w:%d h:%d ", BitmapHelpers.getWidth(src_rect), BitmapHelpers.getHeight(src_rect)));
                Rect subselectionRect = new Rect(
                        (subselectionMiddleX-subselectionWidth/2),
                        (subselectionMiddleY-subselectionHeight/2),
                        (subselectionMiddleX+( subselectionWidth%2 == 0 ? subselectionWidth/2 : subselectionWidth/2 + 1)),
                        (subselectionMiddleY+( subselectionHeight%2 == 0 ? subselectionHeight/2 : subselectionHeight/2 + 1)));
                Log.i("ZoomableImageView","Selected a subrectangle with sizes: "+ BitmapHelpers.getWidth(subselectionRect)+"x"+ BitmapHelpers.getHeight(subselectionRect) );
                return createSubBitmapWithPadding( subselectionRect );
            }
        }
        return null;
    }

    private Bitmap createSubBitmapWithPadding(Rect subselectionRect) {

        Rect originalBmRect = BitmapHelpers.getBitmapRect(bm);
        if( originalBmRect.contains(subselectionRect) )
            return Bitmap.createBitmap(bm, subselectionRect.left, subselectionRect.top,
                    BitmapHelpers.getWidth(subselectionRect), BitmapHelpers.getHeight(subselectionRect));
        else {

            long startTime = System.nanoTime();

            int originalWidth = bm.getWidth();
            int originalHeight = bm.getHeight();
            int[] pixelArray = new int[ originalHeight*originalWidth ];
            bm.getPixels(pixelArray, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight());
            int[] newPixelArray = new int[ BitmapHelpers.getWidth(subselectionRect)* BitmapHelpers.getHeight(subselectionRect) ];

            for(int y = 0; y< BitmapHelpers.getHeight(subselectionRect); y++ )
            {
                for(int x = 0; x< BitmapHelpers.getWidth(subselectionRect); x++ )
                {
                    int px = x + subselectionRect.left;
                    int py = y + subselectionRect.top;
                    if( originalBmRect.contains( px, py) )
                        newPixelArray[ x + y* BitmapHelpers.getWidth(subselectionRect) ] = pixelArray[ px + py*originalWidth ];
                    else
                    {
                        // Reflecting
                        if( px < 0 ) px = -px;
                        else if( px >= originalWidth ) px = px - 2*( px - originalWidth ) - 1; // minus 1 for bounds
                        if( py < 0 ) py = -py;
                        else if( py >= originalBmRect.bottom  ) py = py - 2*( py - originalHeight ) - 1; // minus 1 for bounds
                        //Log.i("ZoomableImageView", "x,y -> "+x+":"+y+"  px,py -> "+px+","+py);
                        newPixelArray[ x + y* BitmapHelpers.getWidth(subselectionRect) ] = pixelArray[ px + py*originalWidth ];
                    }
                }
            }
            Bitmap new_bm = Bitmap.createBitmap(newPixelArray, BitmapHelpers.getWidth(subselectionRect), BitmapHelpers.getHeight(subselectionRect), Bitmap.Config.ARGB_8888);

            long estimatedTime = System.nanoTime() - startTime;
            //Toast.makeText(getContext(), "Elapsed Time in ms for reflection: " + estimatedTime / 1000000, Toast.LENGTH_LONG).show();
            return new_bm;
        }
    }
}
