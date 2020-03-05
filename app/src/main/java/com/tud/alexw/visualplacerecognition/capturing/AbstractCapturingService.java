package com.tud.alexw.visualplacerecognition.capturing;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.util.SparseIntArray;
import android.view.Surface;

import com.tud.alexw.visualplacerecognition.framework.ImageAnnotation;

import java.lang.annotation.Annotation;

/**
 * Abstract Picture Taking Service.
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
 *
 */
public abstract class AbstractCapturingService {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    protected final Activity activity;
    final Context context;
    final CameraManager manager;
    boolean doSaveImage;
    ImageAnnotation imageAnnotation;

    /***
     * constructor.
     *
     * @param activity the activity used to get display manager and the application context
     */
    AbstractCapturingService(final Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.doSaveImage = true;
    }

    /***
     * @return  orientation
     */
    int getOrientation() {
        final int rotation = this.activity.getWindowManager().getDefaultDisplay().getRotation();
        return ORIENTATIONS.get(rotation);
    }


    /**
     * starts pictures capturing process.
     *
     * @param listener picture capturing listener
     */
    public abstract void startCapturing(final CapturingListener listener, ImageAnnotation imageAnnotation);

    public abstract void endCapturing();
    public abstract void capture();

    public void setDoSaveImage(boolean doSaveImage) {
        this.doSaveImage = doSaveImage;
    }

//    public abstract void startBackgroundThread();
//    public abstract void stopBackgroundThread();
}
