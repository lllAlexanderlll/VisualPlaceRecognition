package com.tud.alexw.visualplacerecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.tud.alexw.visualplacerecognition.capturing.AbstractCapturingService;
import com.tud.alexw.visualplacerecognition.capturing.CapturingListener;
import com.tud.alexw.visualplacerecognition.evaluation.Tester;
import com.tud.alexw.visualplacerecognition.framework.AsyncFrameworkSetup;
import com.tud.alexw.visualplacerecognition.framework.Config;
import com.tud.alexw.visualplacerecognition.framework.VLADPQFramework;
import com.tud.alexw.visualplacerecognition.head.MoveHead;
import com.tud.alexw.visualplacerecognition.head.MoveHeadListener;
import com.tud.alexw.visualplacerecognition.result.ImageAnnotation;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CapturingListener, MoveHeadListener, ActivityCompat.OnRequestPermissionsResultCallback {

    public static String TAG = "MainActivity";

    private Head mHead;
    VLADPQFramework mVLADPQFramework;

    private Bitmap mBitmap;

    private Config mConfig;
    //The capture service
    private AbstractCapturingService pictureService;
    private ImageAnnotation mImageAnnotation;
    private MoveHead moveHead;
    private long captureTime_ms = 0;

    private TextView mStatusTextView;
    private TextView mResultTextView;
    private ImageView mImageView;
    private Button mCaptureButton;
    private TextView mTestTextView;

    private static final String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTestTextView = (TextView) findViewById(R.id.test);
        mCaptureButton = (Button) findViewById(R.id.capture);
        mStatusTextView = (TextView) findViewById(R.id.status);
        mResultTextView = (TextView) findViewById(R.id.result);
        mImageView = (ImageView) findViewById(R.id.imageView);

        mStatusTextView.setMovementMethod(new ScrollingMovementMethod());
        mResultTextView.setMovementMethod(new ScrollingMovementMethod());
        mCaptureButton.setEnabled(false);

        //Setup loomo
        mHead = Head.getInstance();
        mHead.bindService(getApplicationContext(), mServiceBindListenerHead);

        pictureService.setDoSaveImage(false);
        pictureService.startCapturing(this, mImageAnnotation);
        int[] pitchValues = {   0,   0,   0,   0,  0,  0,  0, 35,  35,  35,  35, 35, 35, 35, 145, 145, 145, 145, 145, 174, 174, 174, 174, 174};
        int[] yawValues = {     0, -30, -60, -90, 90, 60, 30,  0, -30, -60, -90, 90, 60, 30,   0, -30, -60,  60,  30,   0, -30, -60,  60,  30};
//        int[] pitchValues = {   0, 35, 145, 174};
//        int[] yawValues = {     0,  0,   0,   0};
//            int[] pitchValues = {   0, 45};
//            int[] yawValues = {     0, 0};
        moveHead = new MoveHead(mHead, this, yawValues, pitchValues);

        try{
            mConfig = Config.getConfigLoomo(getApplicationContext());
            Log.i(TAG, mConfig.toString());
        }
        catch (Exception e) {
            Utils.addTextRed(mStatusTextView, "Setup error!");
            String msg = Log.getStackTraceString(e);
            Utils.addTextRed(mStatusTextView, e.getMessage());
            Log.e(TAG, e.getMessage() + "\n" + msg);
            return;
        }

        //setup and check storage and files
        try {
            if(Utils.isStorageStructureCreated(getApplicationContext())){
                mVLADPQFramework = new VLADPQFramework(mConfig);
            }
            else{
                Utils.addText(mStatusTextView, "Stoarge structure is created now. Populate with codebook, index and pca files!");
                return;
            }
        } catch (Exception e) {
            Utils.addTextRed(mStatusTextView, "Couldn't find or create private storage!");
            String msg = Log.getStackTraceString(e);
            Utils.addTextRed(mStatusTextView, e.getMessage());
            Log.e(TAG, e.getMessage() + "\n" + msg);
            return;
        }


        // Load index
        Utils.addText(mStatusTextView, "Loading index...");
        new AsyncFrameworkSetup(mVLADPQFramework, mStatusTextView, mConfig.isDoRunTests() ? null : mCaptureButton, mConfig.isDoRunTests(), getApplicationContext()).execute();

        // Check if evaluation is wanted
        if(mConfig.isDoRunTests()) {
            mTestTextView.setVisibility(View.VISIBLE);
            new Tester(getApplicationContext(), mVLADPQFramework, mStatusTextView).execute();
        }
        else{
            mCaptureButton.setVisibility(View.VISIBLE);
        }

        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHead.resetOrientation();
                moveHead.next();
            }
        });

    }


    @Override
    public void onHeadMovementDone(int yaw, int pitch) {
        Log.i(TAG, String.format("Head movement (%d, %d) done" , yaw, pitch));
        captureTime_ms = System.currentTimeMillis();
        pictureService.capture();
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            if(System.currentTimeMillis() - captureTime_ms > 2000){
                Log.e(TAG, "Needed to restart capturing after time limit reached!");
                pictureService.capture();
            }
        }, 2000);
    }

    @Override
    public void onCaptureDone(byte[] pictureData) {
        if (pictureData != null) {
            runOnUiThread(() -> showImage(pictureData));
            Log.i(TAG, String.format("Taking a photo took %d ms", System.currentTimeMillis() - captureTime_ms));
            captureTime_ms = System.currentTimeMillis();
            moveHead.next();
        }
    }


    @Override
    public void onAllHeadMovementsDone(){
        Log.i(TAG, "No movements left! Capturing finished!");
//        Log.e(TAG, "Start capturing all over again: Infinity loop!");
//        pictureService.capture();
    }


    @Override
    public void onCapturingFailed(){
        Log.e(TAG, "Capturing failed!");
        moveHead.retry();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        Toast.makeText(getApplicationContext(), "onStart() called!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        mHead.unbindService();
        pictureService.endCapturing();
        super.onStop();
        finish();

//        Toast.makeText(getApplicationContext(), "onStop() called!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        Toast.makeText(getApplicationContext(), "onPause() called!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mHead.isBind()) {
            mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        }
//        Toast.makeText(getApplicationContext(), "onResume() called!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        mHead.unbindService();
        pictureService.endCapturing();
        super.onDestroy();
    }

    //force landscape mode
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation/keyboard change
        super.onConfigurationChanged(newConfig);
    }

//                Utils.addTextNumbersBlue(mStatusTextView, mVLADPQFramework.popStatusString());
//                Utils.addTextNumbersBlue(mResultTextView, mVLADPQFramework.popResultString());


    private void showImage(byte[] pictureData) {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }

    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final List<String> neededPermissions = new ArrayList<>();
        for (final String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }

    private ServiceBinder.BindStateListener mServiceBindListenerHead = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            int mode = Head.MODE_SMOOTH_TACKING;
            mHead.setMode(mode);
            Log.d(TAG, "Head onBind() called. " + (mode > Head.MODE_SMOOTH_TACKING ? "lock orientation" : "smooth tracking") + " mode");
            mHead.resetOrientation();
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Head onUnbind() called with: reason = [" + reason + "]");
        }
    };
}
