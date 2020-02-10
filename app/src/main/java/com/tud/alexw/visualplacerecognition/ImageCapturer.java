package com.tud.alexw.visualplacerecognition;

import android.graphics.Bitmap;
import android.util.Log;

import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

public class ImageCapturer {

    private static final String TAG = "ImageCapturer";
    private Vision mVision;
    private StreamInfo mColorInfo;
    private Bitmap mBitmap;

    public ImageCapturer(Vision mVision) {
        this.mVision = mVision;
        this.mBitmap = null;
    }

    public synchronized Bitmap captureImage() {
//        Log.d(TAG, "captureImage() called");

        //start image stream listener
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            if (info.getStreamType() == StreamType.COLOR) {
                mColorInfo = info;
                return mBitmap;
            }
        }
        Log.wtf(TAG, "No camera active!"); // What a terrible failure
        return null;
    }

    public synchronized void start() {
        Log.d(TAG, "start() called");
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    mColorInfo = info;
                    mVision.startListenFrame(StreamType.COLOR, mFrameListener);
                    Log.d(TAG, "mVision.startListenFrame(StreamType.COLOR, mFrameListener) called");
                    break;
                case StreamType.DEPTH:
                    mVision.startListenFrame(StreamType.DEPTH, mFrameListener);
                    break;
            }
        }
    }
    public synchronized void stop() {
        Log.d(TAG, "stop() called");
        mVision.stopListenFrame(StreamType.COLOR);
        mVision.stopListenFrame(StreamType.DEPTH);
    }

    public boolean gotBitmap(){
        return mBitmap != null;
    }

    /**
     * FrameListener instance for get raw image data form vision service
     */
    Vision.FrameListener mFrameListener = new Vision.FrameListener() {

        @Override
        public void onNewFrame(int streamType, Frame frame) {
            Bitmap mColorBitmap = Bitmap.createBitmap(mColorInfo.getWidth(), mColorInfo.getHeight(), Bitmap.Config.ARGB_8888);
            if (streamType == StreamType.COLOR) {
                // draw color image to bitmap and display
                mColorBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
                mBitmap = mColorBitmap;
                Log.v(TAG, "Got frame as bitmap");
            }
        }
    };
}
