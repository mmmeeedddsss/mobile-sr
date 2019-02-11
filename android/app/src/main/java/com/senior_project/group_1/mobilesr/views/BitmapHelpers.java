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
}
