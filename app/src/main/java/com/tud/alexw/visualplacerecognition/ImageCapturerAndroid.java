package com.tud.alexw.visualplacerecognition;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import static android.app.Activity.RESULT_OK;

public class ImageCapturerAndroid extends ImageCapturer{

    private static final String TAG = "ImageCapturerAndroid";

    private Activity mActivity;

    ImageCapturerAndroid(Activity activity){
        mActivity = activity;
    }

    final int REQUEST_IMAGE_CAPTURE = 1;
    public Bitmap captureImage(){
        mBitmap = null;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            mActivity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
        return mBitmap;
    }

    public void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            mBitmap = (Bitmap) extras.get("data");
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ImageView) mActivity.findViewById(R.id.imageView)).setImageBitmap(mBitmap);
                }
            });
        }
    }

}
