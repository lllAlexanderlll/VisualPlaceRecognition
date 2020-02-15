package com.tud.alexw.visualplacerecognition.capturing;

import android.graphics.Bitmap;

import java.io.IOException;

public abstract class ImageCapturer {

    protected Bitmap mBitmap;

    public abstract Bitmap captureImage() throws IOException;

    public final boolean gotBitmap(){
        return mBitmap != null;
    }

}
