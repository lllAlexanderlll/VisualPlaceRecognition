package com.tud.alexw.visualplacerecognition.capturing;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import com.tud.alexw.visualplacerecognition.framework.ImageAnnotation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;


/**
 * The aim of this service is to secretly take pictures (without preview or opening device's camera app)
 * from all available cameras using Android Camera 2 API
 *
 * @author hzitoun (zitoun.hamed@gmail.com)
 *
 * The android-camera2-secret-picture-taker is covered by the MIT License.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Hamed ZITOUN and contributors to the android-camera2-secret-picture-taker project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP) //NOTE: camera 2 api was added in API level 21
public class CapturingService extends AbstractCapturingService {

    private static final String TAG = CapturingService.class.getSimpleName();

    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    /***
     * camera ids queue.
     */
    private Queue<String> cameraIds;

    private String currentCameraId;
    private boolean cameraClosed;

    private ImageAnnotation imageAnnotation;
    /**
     * stores a sorted map of (pictureUrlOnDisk, PictureData).
     */
    private byte[] image;
    private CapturingListener capturingListener;

//    private Handler mBackgroundHandler;
//    private HandlerThread mBackgroundThread;

    /***
     * private constructor, meant to force the use of {@link #getInstance}  method
     */
    private CapturingService(final Activity activity) {
        super(activity);
    }

    /**
     * @param activity the activity used to get the app's context and the display manager
     * @return a new instance
     */
    public static AbstractCapturingService getInstance(final Activity activity) {
        return new CapturingService(activity);
    }

    /**
     * Starts pictures capturing treatment.
     *
     * @param listener picture capturing listener
     */
    @Override
    public void startCapturing(final CapturingListener listener, ImageAnnotation imageAnnotation) {
        this.imageAnnotation = imageAnnotation;
        this.image = null;
        this.capturingListener = listener;
        this.cameraIds = new LinkedList<>();
        try {
            final String[] cameraIds = manager.getCameraIdList();
            if (cameraIds.length > 0) {
//                this.cameraIds.addAll(Arrays.asList(cameraIds));
                this.cameraIds.add(cameraIds[0]);
                this.currentCameraId = this.cameraIds.poll();
                openCamera();
            } else {
                Log.e(TAG, "No cameras detected!");
                endCapturing();
            }
        } catch (final CameraAccessException e) {
            Log.e(TAG, "Exception occurred while accessing the list of cameras", e);
        }
    }

    private void openCamera() {
        Log.d(TAG, "opening camera " + currentCameraId);
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(currentCameraId, stateCallback, null);
            }
        } catch (final CameraAccessException e) {
            Log.e(TAG, " exception occurred while opening camera " + currentCameraId, e);
        }
    }

    private final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "Capture failed!");
        }

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            Log.i(TAG, "Capture started!");
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.i(TAG, "Capture processed!");
        }
    };

    private final ImageReader.OnImageAvailableListener onImageAvailableListener = (ImageReader imReader) -> {
        final Image image = imReader.acquireLatestImage();
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        final byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        registerImage(bytes);
        image.close();
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraClosed = false;
            Log.d(TAG, "camera " + camera.getId() + " opened");
            cameraDevice = camera;
            Log.i(TAG, "Taking picture from camera " + camera.getId());
            //Take the picture after some delay. It may resolve getting a black dark photos.
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, " camera " + camera.getId() + " disconnected");
            if (cameraDevice != null && !cameraClosed) {
                cameraClosed = true;
                cameraDevice.close();
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            cameraClosed = true;
            Log.d(TAG, "camera " + camera.getId() + " closed");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "camera in error, int code " + error);
            if (cameraDevice != null && !cameraClosed) {
                cameraDevice.close();
            }
            capturingListener.onCapturingFailed();
        }
    };


    private Range<Integer> getRange() {
        try {
            CameraCharacteristics chars = manager.getCameraCharacteristics(cameraDevice.getId());
            Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Range<Integer> result = null;
            for (Range<Integer> range : ranges) {
                int upper = range.getUpper();
                // 10 - min range upper for my needs
                if (upper >= 10) {
                    if (result == null || upper < result.getUpper().intValue()) {
                        result = range;
                    }
                }
            }
            if (result == null) {
                result = ranges[0];
            }
            return result;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void capture(){
        if(!cameraClosed) {
            Log.i(TAG, "Taking new image");
            new Handler().postDelayed(this::takePicture, 500);
        }
        else{
            capturingListener.onCapturingFailed();
        }
    }

    private void takePicture() {
        try {
            if (null == cameraDevice) {
                Log.e(TAG, "cameraDevice is null");
                return;
            }
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (streamConfigurationMap != null) {
                jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            }
            final boolean jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
            int width = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 640;
            int height = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 480;
            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            final List<Surface> outputSurfaces = new ArrayList<>();
            outputSurfaces.add(reader.getSurface());
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
    //        captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 1200);
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 100000000L);

            if(imageAnnotation.getPitch() <= 90){
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
            }
            else{
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 180);
            }
            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,getRange());
            reader.setOnImageAvailableListener(onImageAvailableListener, null);
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, null);
                    } catch (final CameraAccessException e) {
                        Log.e(TAG, " exception occurred while accessing " + currentCameraId, e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }
            , null);
        } catch (final CameraAccessException e) {
            Log.e(TAG, " exception occurred while taking picture from " + currentCameraId, e);
            capturingListener.onCapturingFailed();
        }
    }


    private void registerImage(final byte[] bytes) {
        imageAnnotation.setTimeTaken(System.currentTimeMillis());


        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator
                + "testset"
                + File.separator
                + imageAnnotation.getLabel()
                + File.separator;

        File directory = new File(path);
        if (!directory.exists() || !doSaveImage) {
            directory.mkdirs();
        }
        File file = new File(directory, imageAnnotation.encodeFilename());

        if(doSaveImage) {
            try (final OutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(bytes);
                outputStream.flush();
                ((FileOutputStream) outputStream).getFD().sync();
                outputStream.close();

                MediaScannerConnection.scanFile(context,
                        new String[]{file.toString()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);
                            }
                        });
                Log.i(TAG, "Saved: " + file.getAbsolutePath());
            } catch (final IOException e) {
                Log.e(TAG, "Exception occurred while saving picture to external storage ", e);
            }
        }
        image = bytes;
        capturingListener.onCaptureDone(image);
    }

    @Override
    public void endCapturing(){
        if(!cameraClosed){
            closeCamera();
        }
    }


    private void closeCamera() {
        Log.d(TAG, "closing camera " + cameraDevice.getId());
        if (null != cameraDevice && !cameraClosed) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

//    @Override
//    public void startBackgroundThread() {
//        mBackgroundThread = new HandlerThread("Camera Background");
//        mBackgroundThread.start();
//        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
//    }
//
//    @Override
//    public void stopBackgroundThread() {
//        mBackgroundThread.quitSafely();
//        try {
//            mBackgroundThread.join();
//            mBackgroundThread = null;
//            mBackgroundHandler = null;
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

}
