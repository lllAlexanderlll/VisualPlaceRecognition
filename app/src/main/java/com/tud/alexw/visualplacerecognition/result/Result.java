package com.tud.alexw.visualplacerecognition.result;

import android.util.Log;

import java.util.Arrays;

import gr.iti.mklab.visual.utilities.Answer;

public class Result {

    private static String TAG = "Result";
    private Answer[] answers;
    private int counter;


    public Result(int nMaxAnswers){
        if(nMaxAnswers <= 0){
            throw new IllegalArgumentException("nMaxAnswers must be greater than zero!");
        }
        answers = new Answer[nMaxAnswers];
        counter = 0;
    }

    public Annotations addAnswerOrGetAnnotations(Answer answer){
        answers[counter] = answer;
        counter++;
        if (counter >= answers.length){
            Annotations annotations = getAnnotations();
            counter = 0;
            Arrays.fill(answers, null);
            return annotations;
        }
        return null;
    }

    public Annotations getAnnotations(){
        Annotations annotations = new Annotations();
        for(Answer answer : answers){
            for(String filename : answer.getIds()){
                annotations.addAnotation(decodeFilename(filename));
            }
        }
        return annotations;
    }


    private Annotation decodeFilename(String filename){
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
