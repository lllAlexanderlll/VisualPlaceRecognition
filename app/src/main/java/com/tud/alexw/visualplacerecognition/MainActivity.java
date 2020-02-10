package com.tud.alexw.visualplacerecognition;

import androidx.appcompat.app.AppCompatActivity;
import gr.iti.mklab.visual.datastructures.PQ;
import gr.iti.mklab.visual.utilities.Answer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
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
    VLADPQFramework vladpqFramework;
    private Vision mVision;
    private Head mHead;
    private ImageCapturer mImageCapturer;
    private boolean isVisionBound = false;
    private Bitmap bitmap;
    private TextView textView;
    ImageView imageView;
    private Button captureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        captureButton = (Button) findViewById(R.id.capture);
        textView = (TextView) findViewById(R.id.status);
        imageView = (ImageView) findViewById(R.id.imageView);

        // get Vision SDK instance
        mVision = Vision.getInstance();
        mHead = Head.getInstance();

        mVision.bindService(this, mBindStateListenerVision);
        mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        mImageCapturer =  new ImageCapturer(mVision);

        try {
            Utils.checkStorage(getApplicationContext());
        }
        catch (Exception e){
            Utils.addStatusRed(textView, "CouldN#t find or create private storage!");
            String msg = Log.getStackTraceString(e);
            Utils.addStatusRed(textView, msg);
            Log.e(TAG, e.getMessage() + "\n" + msg);
            return;
        }

        vladpqFramework = new VLADPQFramework();

        textView.setMovementMethod(new ScrollingMovementMethod());
        captureButton.setEnabled(false);
        Toast.makeText(getApplicationContext(), "Loading index...", Toast.LENGTH_LONG).show();
        new AsyncSetup(vladpqFramework, textView, captureButton, getApplicationContext()).execute();

        captureButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        mHead.resetOrientation();
                        bitmap = mImageCapturer.captureImage();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView.setText("");
                                    if (bitmap == null) {
                                        Utils.addStatusRed(textView, "Capture: Bitmap is null!");
                                    }
                                    else{
                                        imageView.setImageBitmap(bitmap);
                                        inference();
                                    }
                                }
                            });


                        mHead.resetOrientation();
                        mHead.setWorldPitch(Utils.degreeToRad(45));
                    }
                }).start();
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
        mImageCapturer.stop();
        mVision.unbindService();
        mHead.unbindService();
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
        if(!mHead.isBind()){
            mHead.bindService(getApplicationContext(), mServiceBindListenerHead);
        }
        mVision.bindService(this, mBindStateListenerVision);
//        Toast.makeText(getApplicationContext(), "onResume() called!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        mVision.unbindService();
        mHead.unbindService();
        super.onDestroy();
    }

    private void inference(){
        try {
            long start = System.currentTimeMillis();
            double[] vladVector = vladpqFramework.inference(bitmap, 960, 540, 4096);
            if (vladpqFramework.isDoPCA()) {
                Utils.addStatus(textView,"PCA: " + Utils.blue("true"));
                Utils.addStatus(textView, String.format("Whitening: %s", Utils.blue(Boolean.toString(vladpqFramework.isDoWhitening()))));
            }
            Utils.addStatus(textView, String.format("Vectorized image to VLAD vector! (Length: %d) %s", vladVector.length, Utils.blue((System.currentTimeMillis() - start) + " ms")));

            start = System.currentTimeMillis();
            Answer answer = vladpqFramework.search(5, vladVector);
            Utils.addStatusBlue(textView, "Search result:");
            Utils.addStatus(textView, answer.toString());
            Utils.addStatus(textView, String.format("Search took %s", Utils.blue((System.currentTimeMillis() - start) + " ms")));
        } catch (Exception e) {
            String msg = Log.getStackTraceString(e);
            Utils.addStatusRed(textView, msg);
            Log.e(TAG, e.getMessage() + "\n" + msg);
        }

    }

    ServiceBinder.BindStateListener mBindStateListenerVision = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "Vision onBind() called");
            mImageCapturer.start();
            isVisionBound = true;
            Button button = (Button) findViewById(R.id.capture);
            while(!(isVisionBound && mImageCapturer.gotBitmap())){
                Log.v(TAG, String.format("Waiting for bitmap: %b isVisionBound: %b", isVisionBound, mImageCapturer.gotBitmap()));
            }
            button.setEnabled(isVisionBound);
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
