package com.tud.alexw.visualplacerecognition;

import android.content.Context;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import com.segway.robot.sdk.locomotion.head.Head;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Utility class with static methods for conversion, text colouring, and memory status logging
 */
public class Utils {

    public static String TAG = "Utils";

    /**
     * Log maximum, available and allocated heap memory in MB
     */
    public static void logMemory(){
        final Runtime runtime = Runtime.getRuntime();
        final long used_heap_memory_mb = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        final long max_heap_memory_mb = runtime.maxMemory() / 1048576L;
        final long available_heap_memory_mb = max_heap_memory_mb - used_heap_memory_mb;
        Log.i(TAG, String.format("%d MB of %d MB of heap memory allocated. %d MB available.", used_heap_memory_mb, max_heap_memory_mb, available_heap_memory_mb));
    }

    /**
     * Checks if storage structure i.e. folders are created. If not creates it. Throws IO exception, if folder couldn't be created
     * @param context Application context required for storage location
     * @return if storage structure is created after calling this method
     * @throws IOException
     */
    public static boolean isStorageStructureCreated(Context context) throws IOException {
        // Get the directory for the app's private pictures directory.
        File file = context.getExternalFilesDir(null);
        if(!file.exists()){
            if (!file.mkdirs()) {
                Log.e(TAG, "Directory not created");
                throw new IOException(TAG + "Couldn't create folder! ");
            }
            else{
                Log.i(TAG ,"Directory created: " + file.getAbsolutePath());
                return false;
            }
        }
        else{
            Log.i(TAG ,"Exists already!: " + file.getAbsolutePath());
            return true;
        }
    }

    /**
     * Convert a linked list to an array
     * @param linkedList the linked list
     * @param <T> type of the elemts within the linked list
     * @return
     */
    public static <T> Object[] linkedListToArray(LinkedList<T> linkedList){
        Object[] array = linkedList.toArray();
        return array;
    }

    /**
     * Appends an HTML text in a new line to given textview
     * @param textView the text view to write to
     * @param msg the message (can be HTML)
     */
    public static void addText(TextView textView, String msg){
        msg = msg.replace("\n", "<br/>");
        textView.append(Html.fromHtml("<br/>" + msg));
    }

    /**
     * Appends an HTML text in a new line to given textview in red
     * @param textView the text view to write to
     * @param msg the message (can be HTML)
     */
    public static void addTextRed(TextView textView, String msg){
        msg = msg.replace("\n", "<br/>");
        textView.append(Html.fromHtml( "<br/>" + red(msg)));
    }

    /**
     * Appends an HTML text in a new line to given textview; all numbers within message are displayed blue
     * @param textView the text view to write to
     * @param msg the message (can be HTML)
     */
    public static void addTextNumbersBlue(TextView textView, String msg){
        msg = msg.replace("\n", "<br/>");
        msg = msg.replaceAll("(\\d)", blue("$1"));
        textView.append(Html.fromHtml("<br/>" + msg));
    }

    /**
     * Add HTML font colour blue to message
     * @param msg message
     * @return coloured HTML message
     */
    public static String blue(String msg){
        return "<font color=#42baff>" + msg +"</font>";
    }

    /**
     * Add HTML font colour red to message
     * @param msg message
     * @return coloured HTML message
     */
    public static String red(String msg){
        return "<font color=#ff0000>" + msg +"</font>";
    }

    /**
     * Transforms degrees to radian
     * @param degree degree value
     * @return corresponding radian value
     */
    public static float degreeToRad(int degree){
        return (float) (degree * Math.PI/180);
    }

    /**
     * Transforms radian to degree
     * @param rad radian value
     * @return corresponding degree value
     */
    public static int radToDegree(float rad){
        return (int) (rad* 180/Math.PI);
    }

    /**
     * Compares two degrees in a soft way (5° deviation allowed). Soft comparison, since robot head measurements "wiggle" a little. High value of 5° set for fast head movement.
     * @param deg1 degree value to compare
     * @param deg2 degree value to compare
     * @return whether the two values are close
     */
    public static boolean isClose(int deg1, int deg2){
        boolean result = Math.abs(deg1 - deg2) < 5;
        if(!result){
            Log.v(TAG, String.format("%d° != %d°", deg1, deg2));
        }
        return result;
    }
    
}
