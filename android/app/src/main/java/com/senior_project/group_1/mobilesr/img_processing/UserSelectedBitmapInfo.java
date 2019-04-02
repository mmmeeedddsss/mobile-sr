package com.senior_project.group_1.mobilesr.img_processing;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;

public class UserSelectedBitmapInfo {
    private Uri nonProcessedUri, processedUri;
    private Bitmap bitmap;
    private Integer previewIndex;

    private boolean processed;

    public UserSelectedBitmapInfo(Uri nonProcessedUri, Integer previewIndex, ContentResolver cr) {
        this.nonProcessedUri = nonProcessedUri;
        this.bitmap = BitmapHelpers.loadBitmapFromURI(nonProcessedUri, cr);
        this.previewIndex = previewIndex;
        this.processedUri = null;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap.recycle();
        this.bitmap = bitmap;
    }

    public Uri getNonProcessedUri() {
        return nonProcessedUri;
    }

    public void setNonProcessedUri(Uri nonProcessedUri) {
        this.nonProcessedUri = nonProcessedUri;
    }

    public Uri getProcessedUri() {
        return processedUri;
    }

    public void setProcessedUri(Uri processedUri) {
        this.processedUri = processedUri;
    }

    public Integer getPreviewIndex() {
        return previewIndex;
    }

    public void setPreviewIndex(Integer previewIndex) {
        this.previewIndex = previewIndex;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
}
