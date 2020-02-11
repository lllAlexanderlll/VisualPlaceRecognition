package com.tud.alexw.visualplacerecognition;

import android.graphics.Bitmap;

public abstract class ImageCapturer {

    protected Bitmap mBitmap;

    public abstract Bitmap captureImage();

    public final boolean gotBitmap(){
        return mBitmap != null;
    }

}
