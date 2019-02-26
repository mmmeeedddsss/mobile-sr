package com.senior_project.group_1.mobilesr.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

public class BitmapHelpers {

    public static Rect getBitmapRect(Bitmap bm) {
        return new Rect(0,0,bm.getWidth(), bm.getHeight());
    }

    public static int getWidth( Rect r )
    {
        return r.right - r.left;
    }

    public static int getHeight(Rect r )
    {
        return r.bottom - r.top;
    }

    public static Rect scale( Rect r, double factor ){
        return scale( r, factor, false );
    }

    public static Rect scale( Rect r, double factor, boolean use_top_left ){
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

    public static Uri saveImageExternal(Bitmap image, Context context) {
        //TODO - Should be processed in another thread
        Uri uri = null;
        try {
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File myDir = new File(root + "/MOBILE_SR_IMAGES");
            Log.i("BitmapHelpers", "Saving image on "+myDir);
            myDir.mkdirs();

            String fname = "SR_IMAGE_" + Calendar.getInstance().getTimeInMillis() + ".png";
            File file = new File(myDir, fname);

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 99, stream);
            stream.close();
            Log.i("BitmapHelpers","Auth : "+context.getApplicationContext().getPackageName());
            uri = GenericFileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + "", file);
        } catch (IOException e) {
            Log.d("BitmapHelpers", "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }

    public static Intent createShareIntentByUri(Uri uri){
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/png");
        return intent;
    }
}

