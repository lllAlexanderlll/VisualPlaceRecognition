package com.tud.alexw.visualplacerecognition;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import gr.iti.mklab.visual.utilities.Answer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.tud.alexw.visualplacerecognition.capturing.ImageCapturer;
import com.tud.alexw.visualplacerecognition.capturing.ImageCapturerAndroid;
import com.tud.alexw.visualplacerecognition.capturing.ImageCapturerLoomo;
import com.tud.alexw.visualplacerecognition.result.Annotations;
import com.tud.alexw.visualplacerecognition.result.Result;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    public static String TAG = "MainActivity";

    private Vision mVision;
    private Head mHead;
    VLADPQFramework mVLADPQFramework;

    public boolean mRunningOnLoomo = false;
    private ImageCapturer mImageCapturer;
    private Bitmap mBitmap;

    private Result mResult;
    private int inferenceCounter = 0;
    private Annotations mAnnotations;
    private Config mConfig;

    private TextView mStatusTextView;
    private TextView mResultTextView;
    private ImageView mImageView;
    private Button mCaptureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCaptureButton = (Button) findViewById(R.id.capture);
        mStatusTextView = (TextView) findViewById(R.id.status);
        mResultTextView = (TextView) findViewById(R.id.result);
        mImageView = (ImageView) findViewById(R.id.imageView);

        try{
            mConfig = new Config(
                    getApplicationContext(),
                    false,
                    960, //960x540 or 640x480
                    540,
                    new String[]{
                        "codebooks/codebook_split_0.csv",
                        "codebooks/codebook_split_1.csv",
                        "codebooks/codebook_split_2.csv",
                        "codebooks/codebook_split_3.csv",
                    },
                    new Integer[]{128,128,128,128},
                    true,
                    "pca96/pca_32768_to_96.txt",
                    96,
                    true,
                    "linearIndex4Codebooks128WithPCAw96/BDB_518400_surf_32768to96w/", //"linearIndex4Codebooks128WithPCA96/BDB_518400_surf_32768to96/", //linearIndex4Codebooks128WithPCAw96/BDB_518400_surf_32768to96w/
                    "pqIndex4Codebooks128WithPCAw96/", //"pqIndex4Codebooks128WithPCA96/", //pqIndex4Codebooks128WithPCAw96/
                    "pqIndex4Codebooks128WithPCAw96/pq_96_8x3_1244.csv", //"pqIndex4Codebooks128WithPCA96/pq_96_8x3_1244.csv", //pqIndex4Codebooks128WithPCAw96/pq_96_8x3_1244.csv
                    8,
                    10,
                    96,
                    1244,
                    true,
                    10,
                    2
            );
            Log.i(TAG, mConfig.toString());
        }
        catch (Exception e) {
            Utils.addTextRed(mStatusTextView, "Setup error!");
            String msg = Log.getStackTraceString(e);
            Utils.addTextRed(mStatusTextView, e.getMessage());
            Log.e(TAG, e.getMessage() + "\n" + msg);
            return;
        }

        mResult = new Result(mConfig.nMaxAnswers);

        // get Vision SDK instance
        mVision = Vision.getInstance();
        mHead = Head.getInstance();

        // Setup loomo
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
        Toast.makeText(getApplicationContext(), "Loading index...", Toast.LENGTH_SHORT).show();
        new AsyncSetup(mVLADPQFramework, mStatusTextView, mCaptureButton, getApplicationContext()).execute();

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
                            int[] pitchMoves = new int[]{0, 0, 0, 0}; //It's a lie!
                            mConfig.nMaxAnswers = yawMoves.length; //TODO: remove?
                            int pitch_deg;
                            int yaw_deg;
                            if(mConfig.nMaxAnswers != yawMoves.length || yawMoves.length != pitchMoves.length){
                                Log.e(TAG, "Check 'mConfig.nMaxAnswers != yawMoves.length || yawMoves.length != pitchMoves.length' failed");
                                return;
                            }

                            for (int i = 0; i < mConfig.nMaxAnswers; i++) {

                                yaw_deg = yawMoves[i];
                                pitch_deg = pitchMoves[i];
                                Log.i(TAG, String.format("Image %d %d %d",i, yaw_deg, pitch_deg));
                                Utils.moveHead(mHead, yaw_deg, pitch_deg);
                                try {
                                    mBitmap = mImageCapturer.captureImage();
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
                                            inferenceAndNNS();
                                        }
                                    }
                                });
                            }
                            mHead.resetOrientation();
                        }
                    }).start();
                } else {
                   try{
                       dispatchTakePictureIntent();
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
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                setPic();
                inferenceAndNNS();
            }
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

    public void inferenceAndNNS() {
        try {
            long start = System.currentTimeMillis();
            double[] vladVector = mVLADPQFramework.inference(mBitmap);

            Utils.addText(mStatusTextView, String.format("Vectorized image to VLAD vector! (Length: %d) %s", vladVector.length, Utils.blue((System.currentTimeMillis() - start) + " ms")));
            start = System.currentTimeMillis();
            Answer answer = mVLADPQFramework.search(vladVector);

            Utils.addText(mStatusTextView, String.format("Search took %s", Utils.blue((System.currentTimeMillis() - start) + " ms")));
            Utils.addText(mStatusTextView, answer.toString());
            if(inferenceCounter % mConfig.nMaxAnswers == 0){
                Utils.addText(mStatusTextView, "");
            }
            mAnnotations = mResult.addAnswerOrGetAnnotations(answer);
            if(mAnnotations != null){
                //Start majority count
                if(inferenceCounter == 0){
                    Utils.addTextRed(mResultTextView, "Results");
                }
                Utils.addTextRed(mResultTextView, String.format("#%d", inferenceCounter));
                Utils.addText(mResultTextView, mAnnotations.getLabelCount());
                Utils.addText(mResultTextView, "Mean pose: " + Arrays.toString(mAnnotations.getMeanPose()));
                Utils.addTextRed(mStatusTextView, String.format("#%d end.", inferenceCounter));
            }
            inferenceCounter++;
        } catch (Exception e) {
            String msg = Log.getStackTraceString(e);
            Utils.addTextRed(mStatusTextView, msg);
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

    final int REQUEST_IMAGE_CAPTURE = 1;
    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "temp_" + timeStamp + ".jpg";


        File imagePath = new File(getFilesDir(), "images");
        File image= new File(imagePath, imageFileName);
        try{
            /* Making sure the Pictures directory exist.*/
            if(!imagePath.mkdirs()){
                Log.e("createImageFile", "couldn't create image file");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }


        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    static final int REQUEST_TAKE_PHOTO = 1;
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Couldn't create photo file");
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }



    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getMaxWidth();
        int targetH = mImageView.getMaxHeight();

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

        mBitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        mImageView.setImageBitmap(mBitmap);
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
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Head onUnbind() called with: reason = [" + reason + "]");
        }
    };
}
