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
import com.tud.alexw.visualplacerecognition.capturing.CapturingService;
import com.tud.alexw.visualplacerecognition.evaluation.Tester;
import com.tud.alexw.visualplacerecognition.framework.AsyncFrameworkSetup;
import com.tud.alexw.visualplacerecognition.framework.Config;
import com.tud.alexw.visualplacerecognition.framework.VLADPQFramework;
import com.tud.alexw.visualplacerecognition.head.MoveHead;
import com.tud.alexw.visualplacerecognition.head.MoveHeadListener;
import com.tud.alexw.visualplacerecognition.framework.ImageAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity. Setups the GUI, head movements and triggers VLADPQFramework setup. User interacts by clicking a button to trigger place recognition.
 * Head movement and capturing alternates by waiting for each others callbacks
 */
public class MainActivity extends AppCompatActivity implements CapturingListener, MoveHeadListener, ActivityCompat.OnRequestPermissionsResultCallback {

    public static String TAG = "MainActivity";

    private Head mHead;
    VLADPQFramework mVLADPQFramework;

    private Config mConfig;
    //The capture service
    private AbstractCapturingService capturingService;
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


    /**
     * Setup GUI, Head object of robot vision SDK, CapturingService, VLADPQFramework and button to allow user command to conduct place recognition now
     * @param savedInstanceState saved app instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // show memory status before index loaded
        Utils.logMemory();

        mTestTextView = (TextView) findViewById(R.id.test);
        mCaptureButton = (Button) findViewById(R.id.capture);
        mStatusTextView = (TextView) findViewById(R.id.status);
        mResultTextView = (TextView) findViewById(R.id.result);
        mImageView = (ImageView) findViewById(R.id.imageView);

        mStatusTextView.setMovementMethod(new ScrollingMovementMethod());
        mResultTextView.setMovementMethod(new ScrollingMovementMethod());
        mCaptureButton.setEnabled(false);

        mImageAnnotation = new ImageAnnotation(-1,-1,-1,-1,null);

        //Setup loomo
        mHead = Head.getInstance();
        mHead.bindService(getApplicationContext(), mServiceBindListenerHead);

        // setup camera for capturing and saving
        capturingService = CapturingService.getInstance(this);
        capturingService.setDoSaveImage(false);
        capturingService.startCapturing(this, mImageAnnotation);

        //setup robot head for movement. nQueriesForResult set by pitchValues and yawValues length (must be same length)
        // it DOES NOT change nQueriesForResult in config, but array lengths must match it! Config controls when a place belief is issued and not arrays of movement poses!

        // 24 capturing poses
        // int[] pitchValues = {   0,   0,   0,   0,  0,  0,  0, 35,  35,  35,  35, 35, 35, 35, 145, 145, 145, 145, 145, 174, 174, 174, 174, 174};
        // int[] yawValues = {     0, -30, -60, -90, 90, 60, 30,  0, -30, -60, -90, 90, 60, 30,   0, -30, -60,  60,  30,   0, -30, -60,  60,  30};

        // front and back capturing poses
//        int[] pitchValues = {   0, 35, 145, 174};
//        int[] yawValues = {     0,  0,   0,   0};

        // front capturing poses
        // int[] pitchValues = {   0, 45};
        // int[] yawValues = {     0, 0};

        // front and left capturing poses
        int[] pitchValues = {   0, 35, 0, 35};
        int[] yawValues = {     0,  0,   90,   90};

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


        // Load index and asynchronously setup VLADPQFramework
        Utils.addText(mStatusTextView, "Loading index...");
        new AsyncFrameworkSetup(mVLADPQFramework, mStatusTextView, mConfig.isDoRunTests() ? null : mCaptureButton, getApplicationContext()).execute();

        // Check if evaluation is wanted
        if(mConfig.isDoRunTests()) {
            mTestTextView.setVisibility(View.VISIBLE);
            new Tester(getApplicationContext(), mVLADPQFramework, mStatusTextView).execute();
        }
        else{
            mCaptureButton.setVisibility(View.VISIBLE);
        }

        // start place recognition is button is clicked by user (only possible if VLADPQFramework setup was successful)
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHead.resetOrientation();
                moveHead.next();
            }
        });

    }

    /**
     * annotate image according to head position and take an image. Triggers to capture an image
     * @param yaw yaw value in degree
     * @param pitch pitch value in degree
     */
    @Override
    public void onHeadMovementDone(int yaw, int pitch) {
        Log.i(TAG, String.format("Head movement (%d, %d) done" , yaw, pitch));
        mImageAnnotation.setYaw(yaw);
        mImageAnnotation.setPitch(pitch);
        captureTime_ms = System.currentTimeMillis();
        capturingService.capture();
//        final Handler handler = new Handler();
//        handler.postDelayed(() -> {
//            if(System.currentTimeMillis() - captureTime_ms > 2000){
//                Log.e(TAG, "Needed to restart capturing after time limit reached!");
//                capturingService.capture();
//            }
//        }, 2000);
    }

    /**
     * Displays the picture taken, triggers next head movement and start image vectorisation and Nearest Neighbour Search
     */
    @Override
    public void onCaptureDone(byte[] pictureData) {
        if (pictureData != null) {
            runOnUiThread(() -> showImage(pictureData));
            Log.i(TAG, String.format("Taking a photo took %d ms", System.currentTimeMillis() - captureTime_ms));
            captureTime_ms = System.currentTimeMillis();
            moveHead.next();
            try{
                mVLADPQFramework.inferenceAndNNS(BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length));
                Utils.addTextNumbersBlue(mStatusTextView, mVLADPQFramework.popStatusString());
                Utils.addTextNumbersBlue(mResultTextView, mVLADPQFramework.popResultString());
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    /**
     * Log all head movements done. Head movement counter was reset by MoveHead automatically.
     */
    @Override
    public void onAllHeadMovementsDone(){
        Log.i(TAG, "No movements left! Capturing finished!");
//        Log.e(TAG, "Start capturing all over again: Infinity loop!");
//        pictureService.capture();
    }


    /**
     * Log capturing failed and retries capturing
     */
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

    /**
     * Stops capturing and robot SDK head service and activity itself
     */
    @Override
    protected void onStop() {
        mHead.unbindService();
        capturingService.endCapturing();
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
        capturingService.endCapturing();
        super.onDestroy();
    }

    /**
     * force landscape mode
     */

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation/keyboard change
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Show image in a memory friendly way
     * @param pictureData image data
     */
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

    /**
     * Triggered by android to report users decision for giving camera permission or not
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
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

    /**
     * Listens to robot SDK head service. Reports its movement mode and binding status
     */
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
