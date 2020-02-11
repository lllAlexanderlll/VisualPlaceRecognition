package com.tud.alexw.visualplacerecognition;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.core.content.FileProvider;

import static android.app.Activity.RESULT_OK;

public class ImageCapturerAndroid extends ImageCapturer{

    private static final String TAG = "ImageCapturerAndroid";

    private Activity mActivity;

    ImageCapturerAndroid(Activity activity){
        mActivity = activity;
    }



    @Override
    public Bitmap captureImage() throws IOException {
        return null;
    }
}
