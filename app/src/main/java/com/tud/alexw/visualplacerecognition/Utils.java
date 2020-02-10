package com.tud.alexw.visualplacerecognition;

import android.content.Context;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class Utils {

    public static String TAG = "Utils";
//    public static File getOrCreateFolder(String folder) throws IOException {
//        String path = Environment.getExternalStoragePublicDirectory(null)
//                + File.separator
//                + folder
//                + File.separator;
//
//        File directory = new File(path);
//        if (!directory.exists()) {
//            if (!directory.mkdirs()) {
//                Log.d(TAG, "failed to create directory");
//                throw new IOException("Couldn't create folder, despite it is not existing! getOrCreateFolder");
//            }
//            else{
//                Log.i(TAG ,"Directory created: " + directory.getAbsolutePath());
//            }
//        }
//        else{
//            Log.i(TAG ,"Directory exists: " + directory.getAbsolutePath());
//        }
//        return directory;
//    }

    public static File checkStorage(Context context) throws IOException {
        // Get the directory for the app's private pictures directory.
        File file = context.getExternalFilesDir(null);
        if(!file.exists()){
            if (!file.mkdirs()) {
                Log.e(TAG, "Directory not created");
                throw new IOException(TAG + "Couldn't create folder! ");
            }
            else{
                Log.i(TAG ,"Directory created: " + file.getAbsolutePath());
                return file;
            }
        }
        else{
            Log.i(TAG ,"Exists already!: " + file.getAbsolutePath());
            return file;
        }
    }

    public static void addStatus(TextView textView, String msg){
        textView.append(Html.fromHtml("<br/>" + msg));
    }

    public static void addStatusRed(TextView textView, String msg){
        textView.append(Html.fromHtml( "<br/>" + red(msg)));
    }

    public static String blue(String msg){
        return "<font color=#42baff>" + msg +"</font>";
    }

    public static String red(String msg){
        return "<font color=#ff0000>" + msg +"</font>";
    }

    public static void addStatusBlue(TextView textView, String msg){
        textView.append(Html.fromHtml("<br/>" + blue(msg)));
    }

    public static float degreeToRad(int degree){
        return (float) (degree * Math.PI/180);
    }

    public static int radToDegree(float rad){
        return (int) (rad* 180/Math.PI);
    }

    public static boolean isClose(int deg1, int deg2){
        boolean result = Math.abs(deg1 - deg2) < 5;
        if(!result){
            Log.v(TAG, String.format("%d° != %d°", deg1, deg2));
        }
        return result;
    }
}
