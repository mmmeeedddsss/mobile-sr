package com.senior_project.group_1.mobilesr.views;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class BitmapHelpers {

    static Rect getBitmapRect(Bitmap bm)
    {
        return new Rect(0,0,bm.getWidth(), bm.getHeight());
    }

    static int getWidth( Rect r )
    {
        return r.right - r.left;
    }

    static int getHeight(Rect r )
    {
        return r.bottom - r.top;
    }

    static Rect scale( Rect r, double factor ){
        return scale( r, factor, false );
    }

    static Rect scale( Rect r, double factor, boolean use_top_left ){
        if( use_top_left )
        {
            return new Rect(r.left,r.top,
                    (int) (r.left + getWidth(r) * factor), (int) (r.top + getHeight(r) * factor));
        }
        else {
            double center_x = getWidth(r) / 2 + r.left;
            double center_y = getHeight(r) / 2 + r.top;
            factor /= 2;
            return new Rect((int) (center_x - getWidth(r) * factor), (int) (center_y - getHeight(r) * factor),
                    (int) (center_x + getWidth(r) * factor), (int) (center_y + getHeight(r) * factor));
        }
    }
}

