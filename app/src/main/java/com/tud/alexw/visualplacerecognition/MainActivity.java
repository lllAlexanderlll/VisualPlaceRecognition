package com.tud.alexw.visualplacerecognition;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import gr.iti.mklab.visual.utilities.Answer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.vision.Vision;


public class MainActivity extends AppCompatActivity {

    public static String TAG = "MainActivity";

    private Vision mVision;
    private Head mHead;
    VLADPQFramework mVLADPQFramework;

    public boolean mRunningOnLoomo = true;
    private ImageCapturer mImageCapturer;
    private Bitmap mBitmap;

    private TextView mTextView;
    private ImageView mImageView;
    private Button mCaptureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCaptureButton = (Button) findViewById(R.id.capture);
        mTextView = (TextView) findViewById(R.id.status);
        mImageView = (ImageView) findViewById(R.id.imageView);

        // get Vision SDK instance
        mVision = Vision.getInstance();
        mHead = Head.getInstance();

        mRunningOnLoomo &= mVision.bindService(this, mBindStateListenerVision);
        mRunningOnLoomo &= mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        if (mRunningOnLoomo) {
            mImageCapturer = new ImageCapturerLoomo(mVision);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraPermission();
            }
            mImageCapturer = new ImageCapturerAndroid(this);
            mVision = null;
            mHead = null;
            findViewById(R.id.loomoFlag).setVisibility(View.GONE);
        }


        try {
            if(Utils.isStorageStructureCreated(getApplicationContext())){
                mVLADPQFramework = new VLADPQFramework(
                        new Config(
                                getApplicationContext(),
                                new String[]{"codebook/features.csv_codebook-64A-64C-100I-1S_power+l2.csv"},
                                new Integer[]{64},
                                "pca/linearIndexBDB_307200_surf_4096pca_245_128_10453ms.txt",
                                4096,
                                true,
                                "linearIndex/BDB_307200_surf_4096/",
                                "pqIndex/",
                                "pqCodebook/pq_4096_8x3_244.csv"
                        )
                );
            }
            else{
                Utils.addStatus(mTextView, "Stoarge structure is created now. Populate with codebook, index and pca files!");
                return;
            }

        } catch (Exception e) {
            Utils.addStatusRed(mTextView, "Couldn't find or create private storage!");
            String msg = Log.getStackTraceString(e);
            Utils.addStatusRed(mTextView, e.getMessage());
            Log.e(TAG, e.getMessage() + "\n" + msg);
            return;
        }


        mTextView.setMovementMethod(new ScrollingMovementMethod());
        mCaptureButton.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Loading index...", Toast.LENGTH_LONG).show();
        new AsyncSetup(mVLADPQFramework, mTextView, mCaptureButton, getApplicationContext()).execute();

        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRunningOnLoomo) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            mHead.resetOrientation();
                            mBitmap = mImageCapturer.captureImage();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextView.setText("");
                                    if (mBitmap == null) {
                                        Utils.addStatusRed(mTextView, "Capture: Bitmap is null!");
                                    } else {
                                        mImageView.setImageBitmap(mBitmap);
                                        inference();
                                    }
                                }
                            });


                            mHead.resetOrientation();
                            mHead.setWorldPitch(Utils.degreeToRad(45));
                        }
                    }).start();
                } else {
                    mBitmap = mImageCapturer.captureImage();
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

            ((ImageCapturerAndroid) mImageCapturer).handleOnActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();

            } else {

                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();

            }

        }
    }

    private void inference() {
        try {
            long start = System.currentTimeMillis();
            double[] vladVector = mVLADPQFramework.inference(mBitmap, 960, 540, 4096);
            if (mVLADPQFramework.ismDoPCA()) {
                Utils.addStatus(mTextView, "PCA: " + Utils.blue("true"));
                Utils.addStatus(mTextView, String.format("Whitening: %s", Utils.blue(Boolean.toString(mVLADPQFramework.ismDoWhitening()))));
            }
            Utils.addStatus(mTextView, String.format("Vectorized image to VLAD vector! (Length: %d) %s", vladVector.length, Utils.blue((System.currentTimeMillis() - start) + " ms")));

            start = System.currentTimeMillis();
            Answer answer = mVLADPQFramework.search(5, vladVector);
            Utils.addStatusBlue(mTextView, "Search result:");
            Utils.addStatus(mTextView, answer.toString());
            Utils.addStatus(mTextView, String.format("Search took %s", Utils.blue((System.currentTimeMillis() - start) + " ms")));
        } catch (Exception e) {
            String msg = Log.getStackTraceString(e);
            Utils.addStatusRed(mTextView, msg);
            Log.e(TAG, e.getMessage() + "\n" + msg);
        }

    }

    private static final int MY_CAMERA_REQUEST_CODE = 100;

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void cameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_REQUEST_CODE);
        }
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
                Log.v(TAG, String.format("Waiting for image capture. Image captured: %b", mImageCapturer.gotBitmap()));
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
            mHead.setWorldPitch(Utils.degreeToRad(45));
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Head onUnbind() called with: reason = [" + reason + "]");
        }
    };
}
