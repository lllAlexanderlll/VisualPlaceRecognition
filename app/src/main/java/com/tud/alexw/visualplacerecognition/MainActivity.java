package com.tud.alexw.visualplacerecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import com.segway.robot.sdk.vision.Vision;
import com.tud.alexw.visualplacerecognition.capturing.ImageCapturer;
import com.tud.alexw.visualplacerecognition.capturing.ImageCapturerAndroid;
import com.tud.alexw.visualplacerecognition.capturing.ImageCapturerLoomo;
import com.tud.alexw.visualplacerecognition.evaluation.Tester;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "MainActivity";

    private Vision mVision;
    private Head mHead;
    VLADPQFramework mVLADPQFramework;

    public boolean mRunningOnLoomo = true;
    private ImageCapturer mImageCapturer;
    private Bitmap mBitmap;

    private Config mConfig;

    private TextView mStatusTextView;
    private TextView mResultTextView;
    private ImageView mImageView;
    private Button mCaptureButton;
    private TextView mTestTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTestTextView = (TextView) findViewById(R.id.test);
        mCaptureButton = (Button) findViewById(R.id.capture);
        mStatusTextView = (TextView) findViewById(R.id.status);
        mResultTextView = (TextView) findViewById(R.id.result);
        mImageView = (ImageView) findViewById(R.id.imageView);


        // get Vision SDK instance
        mVision = Vision.getInstance();
        mHead = Head.getInstance();

        // Setup loomo
        mRunningOnLoomo &= mVision.bindService(this, mBindStateListenerVision);
        mRunningOnLoomo &= mHead.bindService(getApplicationContext(), mServiceBindListenerHead);

        try{
            mConfig = Config.getConfigLoomo(getApplicationContext());
//            if(mRunningOnLoomo){
//                mConfig = Config.getConfigLoomo(getApplicationContext());
//            }
//            else{
//                mConfig = Config.getConfigAndroid(getApplicationContext());
//            }

            Log.i(TAG, mConfig.toString());
        }
        catch (Exception e) {
            Utils.addTextRed(mStatusTextView, "Setup error!");
            String msg = Log.getStackTraceString(e);
            Utils.addTextRed(mStatusTextView, e.getMessage());
            Log.e(TAG, e.getMessage() + "\n" + msg);
            return;
        }




        if (mRunningOnLoomo) {
            mImageCapturer = new ImageCapturerLoomo(mVision);
            findViewById(R.id.loomoFlag).setVisibility(View.VISIBLE);
        } else {
            mImageCapturer = new ImageCapturerAndroid(this, mImageView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ((ImageCapturerAndroid)mImageCapturer).cameraPermission();
            }
            mVision = null;
            mHead = null;
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


        mStatusTextView.setMovementMethod(new ScrollingMovementMethod());
        mResultTextView.setMovementMethod(new ScrollingMovementMethod());
        mCaptureButton.setEnabled(false);
        Utils.addText(mStatusTextView, "Loading index...");

        new AsyncFrameworkSetup(mVLADPQFramework, mStatusTextView, mConfig.isDoRunTests() ? null : mCaptureButton, mConfig.isDoRunTests(), getApplicationContext()).execute();

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
                if (mRunningOnLoomo) {
                    mHead.setWorldPitch(Utils.degreeToRad(45));
                    Utils.moveHead(mHead, 0, 0);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int[] yawMoves   = new int[]{90, 0, 0, -90};
                            int[] pitchMoves = new int[]{0, 0, 0, 0}; //It's a lie! --> Camera in use is not influenced by pitch
                            mConfig.setnQueriesForResult(yawMoves.length); // capturing convinience! --> not part of tests therefore hash code generation not a problem
                            int pitch_deg;
                            int yaw_deg;
                            if(mConfig.getnQueriesForResult() != yawMoves.length || yawMoves.length != pitchMoves.length){
                                Log.e(TAG, "Check 'mConfig.nQueriesForResult != yawMoves.length || yawMoves.length != pitchMoves.length' failed");
                                return;
                            }

                            for (int i = 0; i < mConfig.getnQueriesForResult(); i++) {

                                yaw_deg = yawMoves[i];
                                pitch_deg = pitchMoves[i];
                                Log.i(TAG, String.format("Image %d %d %d",i, yaw_deg, pitch_deg));
                                Utils.moveHead(mHead, yaw_deg, pitch_deg);
                                try {
                                    mBitmap = mImageCapturer.getImage();
                                } catch (Exception e) {
                                    Log.e("Capture:", Log.getStackTraceString(e));
                                }

                                //update UI and start inferenceAndNNS
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mStatusTextView.setText("");
                                        if (mBitmap == null) {
                                            Utils.addTextRed(mStatusTextView, "Capture: Bitmap is null!");
                                        } else {
                                            mImageView.setImageBitmap(mBitmap);
                                            try{
                                                mVLADPQFramework.inferenceAndNNS(mBitmap);
                                            }
                                            catch (Exception e){
                                                String msg = Log.getStackTraceString(e);
                                                Utils.addTextRed(mStatusTextView, e.getMessage());
                                                Log.e(TAG, e.getMessage() + "\n" + msg);
                                            }
                                            Utils.addTextNumbersBlue(mStatusTextView, mVLADPQFramework.popStatusString());
                                            Utils.addTextNumbersBlue(mResultTextView, mVLADPQFramework.popResultString());
                                        }
                                    }
                                });
                            }
                            mHead.resetOrientation();
                        }
                    }).start();
                } else {
                   try{
                       ((ImageCapturerAndroid)mImageCapturer).dispatchTakePictureIntent();
                   }
                   catch (Exception e){
                       Log.e("Capture:", Log.getStackTraceString(e));
                   }
                }

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
//        Toast.makeText(getApplicationContext(), "onStart() called!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRunningOnLoomo) {
            ((ImageCapturerLoomo) mImageCapturer).stopFrameListening();
            mVision.unbindService();
            mHead.unbindService();
        }
        super.onStop();
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
        if (mRunningOnLoomo) {
            if (!mHead.isBind()) {
                mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
            }
            mVision.bindService(this, mBindStateListenerVision);
        }
//        Toast.makeText(getApplicationContext(), "onResume() called!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (mRunningOnLoomo) {
            mVision.unbindService();
            mHead.unbindService();
        }
        super.onDestroy();
    }

    //force landscape mode
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation/keyboard change
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRunningOnLoomo) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            if (requestCode == ((ImageCapturerAndroid)mImageCapturer).REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                ((ImageCapturerAndroid)mImageCapturer).setPic();
                try{
                    mVLADPQFramework.inferenceAndNNS(mImageCapturer.getImage());
                }
                catch (Exception e){
                    String msg = Log.getStackTraceString(e);
                    Utils.addTextRed(mStatusTextView, e.getMessage());
                    Log.e(TAG, e.getMessage() + "\n" + msg);
                }
                Utils.addTextNumbersBlue(mStatusTextView, mVLADPQFramework.popStatusString());
                Utils.addTextNumbersBlue(mResultTextView, mVLADPQFramework.popResultString());
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ((ImageCapturerAndroid)mImageCapturer).onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    ServiceBinder.BindStateListener mBindStateListenerVision = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "Vision onBind() called");
            if (mRunningOnLoomo) {
                ((ImageCapturerLoomo) mImageCapturer).startFrameListening();
            }
            Button button = (Button) findViewById(R.id.capture);
            while (!mImageCapturer.gotBitmap()) {
                Log.v(TAG, String.format("Waiting for image capture."));
            }
            button.setEnabled(true);
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Vision onUnbind() called with: reason = [" + reason + "]");
        }
    };

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
