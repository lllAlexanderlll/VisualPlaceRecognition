package com.tud.alexw.visualplacerecognition.result;

import android.util.Log;

import java.util.Arrays;

import androidx.annotation.NonNull;

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
            if(annotations.length < 7){
                Log.e(TAG, "Couldn't decode file. Invalid filename (seven or more '_'-separated annotations required: date_time_label_x_y_yaw_pitch, label can be split in several '_' separated labels): " + filename + ": " + Arrays.toString(annotations));
                return null;
            }
//        String date = annotations[0];
//        String time = annotations[1];
            StringBuilder labelBuilder = new StringBuilder();
            for(int i = 2; i < annotations.length - 4; i++){
                labelBuilder.append(annotations[i]).append("_");
            }
            String label = labelBuilder.toString();
            label = label.substring(0, label.length() - 1);
            Log.i(TAG, label);
            int x = Integer.parseInt(annotations[annotations.length - 4]);
            int y = Integer.parseInt(annotations[annotations.length - 3]);
            int yaw = Integer.parseInt(annotations[annotations.length - 2]);
            int pitch = Integer.parseInt(annotations[annotations.length - 1]);
            return new Annotation(x, y, yaw, pitch, label);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder
                .append(label).append("\n")
                .append(x).append("\n")
                .append(y).append("\n")
                .append(yaw).append("\n")
                .append(pitch).append("\n")
                .toString();
    }
}

