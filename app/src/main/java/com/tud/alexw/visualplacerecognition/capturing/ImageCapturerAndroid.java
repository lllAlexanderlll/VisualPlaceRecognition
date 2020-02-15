package com.tud.alexw.visualplacerecognition.capturing;

import android.app.Activity;
import android.graphics.Bitmap;

import java.io.IOException;

public class ImageCapturerAndroid extends ImageCapturer{

    private static final String TAG = "ImageCapturerAndroid";

    private Activity mActivity;

    public ImageCapturerAndroid(Activity activity){
        mActivity = activity;
    }



    @Override
    public Bitmap captureImage() throws IOException {
        return null;
    }
}
