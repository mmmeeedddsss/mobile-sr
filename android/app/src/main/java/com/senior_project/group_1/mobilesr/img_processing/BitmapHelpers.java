package com.senior_project.group_1.mobilesr.img_processing;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.senior_project.group_1.mobilesr.BuildConfig;
import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;
import com.senior_project.group_1.mobilesr.views.GenericFileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

import static java.lang.Math.max;

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

    public static RectF scale(RectF r, double factor, boolean use_top_left ){
        if( use_top_left )
        {
            return new RectF(r.left,r.top,
                    (float)(r.left + r.width() * factor), (float)(r.top + r.height() * factor));
        }
        else {
            double center_x = r.width() / 2 + r.left;
            double center_y = r.height() / 2 + r.top;
            factor /= 2;
            return new RectF((float) (center_x - r.width() * factor), (float) (center_y - r.height() * factor),
                    (float) (center_x + r.width() * factor), (float) (center_y + r.height() * factor));
        }
    }

    public static Intent createShareIntentByUri(Uri uri){
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/png");
        return intent;
    }


    /**
     * Whenever you want to use this method from outside of the ImageView, pass getBitmapRect(bm)
     * as its second argument
     *
     * Update by Deniz: added an overload for the single argument case, thanks for the comment
     *
     * CALL RECYCLE AFTER YOUR JOB IS DONE WITH THE RETURNED BITMAP
     */
    public static Bitmap cropBitmapUsingSubselection(Bitmap bm, Rect src_rect) {
        if( bm != null ) {
            // Main challenge here is selecting a sub-bitmap that fits the model constraints

            // -> Integer division causes cropping, maybe increasing those divisions
            // with proper handling might be work( also handle setImage, crop the reconstructed one )
            // Overlap*(n+1)+(chunk_size-2*overlap)*n = size_y
            // overlap + (chunk_size-overlap)*n = size_y
            // (size_y-overlap)/(chunk_size-overlap) = n
            // + 1 is selecting a bigger area

            if( SRModelConfigurationManager.getCurrentConfiguration().isRemote() )
                return bm;

            int modelInputSizeX = SRModelConfigurationManager.getCurrentConfiguration().getInputImageWidth();
            int modelInputSizeY = SRModelConfigurationManager.getCurrentConfiguration().getInputImageWidth();

            int chunkCountForX = max((BitmapHelpers.getWidth(src_rect))
                    / (modelInputSizeX-ApplicationConstants.IMAGE_OVERLAP_X*2) + 1, 1);
            int chunkCountForY = max((BitmapHelpers.getHeight(src_rect))
                    / (modelInputSizeY-ApplicationConstants.IMAGE_OVERLAP_Y*2) + 1, 1);

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
                //Log.i("ZoomableImageView","Selected a subrectangle with sizes: "+ BitmapHelpers.getWidth(subselectionRect)+"x"+ BitmapHelpers.getHeight(subselectionRect) );
                return createSubBitmapWithPadding( bm, subselectionRect );
            }
        }
        return null;
    }

    /**
     * Overload for the case when we want to crop/pad a full bitmap without ZoomableImageView
     */
    public static Bitmap cropBitmapUsingSubselection(Bitmap bm) {
        if( SRModelConfigurationManager.getCurrentConfiguration().isRemote() )
            return bm;
        return cropBitmapUsingSubselection(bm, getBitmapRect(bm));
    }

    private static Bitmap createSubBitmapWithPadding(Bitmap bm, Rect subselectionRect) {
        Rect originalBmRect = BitmapHelpers.getBitmapRect(bm);
        if( originalBmRect.contains(subselectionRect) ) {
            Log.i("ZoomableImageView","Submitting a bitmap with sizes: "+ BitmapHelpers.getWidth(subselectionRect)+"x"+ BitmapHelpers.getHeight(subselectionRect));
            return Bitmap.createBitmap(bm, subselectionRect.left, subselectionRect.top,
                    BitmapHelpers.getWidth(subselectionRect), BitmapHelpers.getHeight(subselectionRect));
        }
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
            Log.i("ZoomableImageView","Submitting a bitmap with sizes: "+ new_bm.getWidth()+"x"+ new_bm.getHeight());
            long estimatedTime = System.nanoTime() - startTime;
            //Toast.makeText(getContext(), "Elapsed Time in ms for reflection: " + estimatedTime / 1000000, Toast.LENGTH_LONG).show();
            return new_bm;
        }
    }

    /**
     * A method to provide quick loading of bitmaps from a given URI
     * @param uri Uri of the bitmap that should be loaded into memory
     * @param contentResolver the content resolver used while loading, usually gotten with
     *                        context.getContentResolver()
     * @return null if an IOException occurs, otherwise the requested Bitmap
     */
    public static Bitmap loadBitmapFromURI(Uri uri, ContentResolver contentResolver) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri);
        } catch (IOException ex) {
            Log.e("BitmapHelpers::loadBitmapFromURI", "IOError while loading URI: " + uri.toString(), ex);
        }
        return bitmap;
    }

    public static boolean isBitmapCached(UserSelectedBitmapInfo bmInfo, Context context) {
        String md5Digest = getMD5DigestOfFile(bmInfo.getNonProcessedUri());
        if( md5Digest == null )
            return false;
        File file = new File(getCacheFolder(), md5Digest + ".png");
        if( file.exists() ) {
            bmInfo.setProcessed(true);
            bmInfo.setProcessedUri(Uri.parse(file.toURI().toString()));
            bmInfo.setBitmap( loadBitmapFromURI( bmInfo.getProcessedUri(),
                    context.getContentResolver() ) );
            return true;
        }
        return false;
    }

    private static void createNomediaFileOnDir( File dir )
    {
        try {
            File nomedia = new File( dir, ".nomedia" );
            nomedia.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getCacheFolder() {
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.i("BitmapHelpers.getCacheFolder", "Folder : "+root+ "/.MOBILE_SR_CACHE");
        File cacheDir = new File(root + "/.MOBILE_SR_CACHE");
        cacheDir.mkdirs();
        createNomediaFileOnDir(cacheDir);
        return cacheDir;
    }

    public static File getTempFolder() {
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.i("BitmapHelpers.getCacheFolder", "Folder : "+root+ "/.MOBILE_SR_TEMP");
        File cacheDir = new File(root + "/.MOBILE_SR_TEMP");
        cacheDir.mkdirs();
        createNomediaFileOnDir(cacheDir);
        return cacheDir;
    }

    public static File getExternalSavingFolder(){
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        File externalSaveFolder = new File(root + "/MOBILE_SR_IMAGES");
        externalSaveFolder.mkdirs();
        return externalSaveFolder;
    }

    private static Uri saveImage(Bitmap bm,  Context context,
                                 File savingFolder, String filename) {
        //TODO - Should be processed in another thread
        Uri uri = null;
        try {
            Log.i("BitmapHelpers", "Saving image on "+savingFolder+" / "+filename);

            File file = new File(savingFolder, filename + ".png");
            if( file.exists() )
                return GenericFileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + "", file);

            file.createNewFile();

            FileOutputStream stream = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.PNG, 99, stream);
            stream.close();
            Log.i("BitmapHelpers","Auth : "+context.getApplicationContext().getPackageName());
            uri = GenericFileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + "", file);
        } catch (IOException e) {
            Log.d("BitmapHelpers", "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }

    public static void moderateCacheSize(){
        final long _128mbInBytes = 128 * 1024 * 1024;
        File cacheDir = getCacheFolder();
        File[] files = cacheDir.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                long k = o1.lastModified() - o2.lastModified();
                if( k > 0 ) return 1;
                if( k < 0 ) return -1;
                return 0;
            }
        });

        int deletionIndex = 0;
        while( getFolderSize( cacheDir ) > _128mbInBytes )
        {
            if( files[deletionIndex].isFile() )
                files[deletionIndex].delete();
            deletionIndex++;
        }
    }

    // in bytes
    public static long getFolderSize(File directory) {
        long folderSize = 0;
        try {
            for (File file : directory.listFiles()) {
                if (file.isFile())
                    folderSize += file.length();
            }
        } catch (Exception ex){}

        return folderSize;
    }

    public static void saveImageToCache(UserSelectedBitmapInfo bmInfo, Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri processedUri = saveImage(bmInfo.getBitmap(), context, getCacheFolder(),
                        getMD5DigestOfFile(bmInfo.getNonProcessedUri()));
                bmInfo.setProcessedUri(processedUri);
            }
        }).start();
    }

    public static Uri saveImageToTemp(Bitmap bm, Context context) {
        return saveImage(bm, context, getTempFolder(), ""+Calendar.getInstance().getTimeInMillis());
    }

    public static Uri saveImageExternal( Bitmap bm, Context context ) {
        String fname = "SR_IMAGE_" + Calendar.getInstance().getTimeInMillis();
        return saveImage(bm, context, getExternalSavingFolder(), fname);
    }

    public static void saveImageInternal(Bitmap bm, Uri originalPath, Context context){
        try{
            String path = originalPath.getPath().substring( originalPath.getPath().indexOf('/', 1)+1 );
            Log.i("saveImageInternal", "Image path : " + path);
            FileOutputStream stream =  new FileOutputStream( new File( path ),false );
            if( path.endsWith("jpg") || path.endsWith("jpeg") )
                bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            else
                bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();
        }catch (Exception e){
            e.printStackTrace();

        }
    }

    /*
        Code modified from an answer in :
        https://stackoverflow.com/questions/13152736/how-to-generate-an-md5-checksum-for-a-file-in-android
     */
    public static String getMD5DigestOfFile(Uri uri) {
        Log.i("BitmapHelpers.getMd5Digest", "Requested file : "+ uri.getPath());
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(uri.getPath().getBytes());
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            Log.i("BitmapHelpers.getMd5Digest", "Got error");
            e.printStackTrace();
            return null;
        } finally {
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            returnVal += Integer.toString(( md5Bytes[i] & 0xff ) + 0x100, 16).substring(1);
        }
        return returnVal.toUpperCase();
    }

    public static void clearTempFolder() {
        try {
            File tempdir = getTempFolder();
            File[] children = tempdir.listFiles();
            for (int i = 0; i < children.length; i++)
                children[i].delete();
        } catch (Exception ex){}
    }
}

