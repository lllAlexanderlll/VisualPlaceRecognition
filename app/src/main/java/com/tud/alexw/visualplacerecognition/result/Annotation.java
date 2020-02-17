package com.tud.alexw.visualplacerecognition.result;

import android.util.Log;

public class Annotation{
    public int x, y, yaw, pitch;
    public String label;
    private static String TAG = "Annotation";

    public Annotation(int x, int y, int yaw, int pitch, String label) {
        this.x = x;
        this.y = y;
        this.yaw = yaw;
        this.pitch = pitch;
        this.label = label;
    }

    public static Annotation fromFilename(String filename){
            String[] split = filename.split("\\.");
            if(split.length != 2){
                Log.e(TAG, "Couldn't decode file. Invalid filename (exactly one dot allowed): " + filename);
                return null;
            }
            String[] annotations = split[0].split("_");
            if(annotations.length != 7){
                Log.e(TAG, "Couldn't decode file. Invalid filename (seven '_'-separated annotations required: IMG_date_time_label_x_y_yaw_pitch): " + filename);
                return null;
            }
//        String date = annotations[0];
//        String time = annotations[1];
            String label = annotations[2];
            int x = Integer.parseInt(annotations[3]);
            int y = Integer.parseInt(annotations[4]);
            int yaw = Integer.parseInt(annotations[5]);
            int pitch = Integer.parseInt(annotations[6]);
            return new Annotation(x, y, yaw, pitch, label);
    }
}

