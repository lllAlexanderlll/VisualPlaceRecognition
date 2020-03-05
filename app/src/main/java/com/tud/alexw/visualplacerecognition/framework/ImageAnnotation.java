package com.tud.alexw.visualplacerecognition.framework;

import android.util.Log;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import androidx.annotation.NonNull;

public class ImageAnnotation {
    public int x, y, yaw, pitch;
    public String label;
    private static String TAG = "Annotation";
    private long timeTaken;

    public ImageAnnotation(int x, int y, int yaw, int pitch, String label) {
        this.x = x;
        this.y = y;
        this.yaw = yaw;
        this.pitch = pitch;
        this.label = label;
        this.timeTaken = -1;
    }

    public static ImageAnnotation decodeFilename(String filename){
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
//            Log.i(TAG, label);
            int x = Integer.parseInt(annotations[annotations.length - 4]);
            int y = Integer.parseInt(annotations[annotations.length - 3]);
            int yaw = Integer.parseInt(annotations[annotations.length - 2]);
            int pitch = Integer.parseInt(annotations[annotations.length - 1]);
            return new ImageAnnotation(x, y, yaw, pitch, label);
    }

    private void transformToGlobalCoordinates(){
        // convert local pitch and yaw to global measurements

        if(pitch > 90){
            if(pitch == 174){
                pitch = 0;
            }
            else{
                pitch -= 90;
            }
            yaw += 180;
        }
        yaw %= 360;
    }

    public String encodeFilename(){
        transformToGlobalCoordinates();
        String timeFormatted = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Timestamp(timeTaken));
        if(label == null){
            label = "none";
        }
        return String.format("%s_%s_%d_%d_%d_%d.jpg", timeFormatted, label, x, y, yaw, pitch);

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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getYaw() {
        return yaw;
    }

    public int getPitch() {
        return pitch;
    }

    public String getLabel() {
        return label;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(long timeTaken) {
        this.timeTaken = timeTaken;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setYaw(int yaw) {
        this.yaw = yaw;
    }

    public void setPitch(int pitch) {
        this.pitch = pitch;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}

