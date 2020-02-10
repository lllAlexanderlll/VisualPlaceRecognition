package com.tud.alexw.visualplacerecognition;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

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

    public static File getInternalStorageDir(Context context, String albumName) throws IOException {
        // Get the directory for the app's private pictures directory.
        File file = new File(context.getExternalFilesDir(null), albumName);
        if(!file.exists()){
            if (!file.mkdirs()) {
                Log.e(TAG, "Directory not created");
                throw new IOException("Couldn't create folder! getInternalStorageDir");
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

}
