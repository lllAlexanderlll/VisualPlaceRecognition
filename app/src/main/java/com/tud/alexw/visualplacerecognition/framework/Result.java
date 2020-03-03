package com.tud.alexw.visualplacerecognition.framework;

import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import gr.iti.mklab.visual.utilities.Answer;

public class Result {

    private static String TAG = "Result";
    private Answer[] answers;
    private int queryCounter;
    private List<MajorityCount> majorityCounts;
    private String resultLabel;
    private float confidence;

    private int sumX = 0;
    private int sumY = 0;
    private int sumPitch = 0;
    private int sumYaw = 0;

    private StringBuilder stringBuilder = new StringBuilder();


    public Result(int nMaxAnswers){
        if(nMaxAnswers <= 0){
            throw new IllegalArgumentException("nQueriesForResult must be greater than zero!");
        }
        answers = new Answer[nMaxAnswers];
        majorityCounts = new LinkedList<>();
        queryCounter = 0;
    }



    public void addAnswer(Answer answer){
        if (queryCounter >= answers.length){
            queryCounter = 0;
            Arrays.fill(answers, null);
        }
        answer.calculateAnnotations();
        for(ImageAnnotation imageAnnotation :answer.getImageAnnotations()){
            sumX += imageAnnotation.x;
            sumY += imageAnnotation.y;
            sumPitch += imageAnnotation.pitch;
            sumYaw += imageAnnotation.yaw;
        }
        answers[queryCounter] = answer;
        queryCounter++;
    }

    public Answer[] getAnswers() {
        return answers;
    }

    public int getQueryCounter() {
        return queryCounter;
    }

    public List<MajorityCount> getMajorityCounts() {
        return majorityCounts;
    }

    public void majorityCount(){
        int resultLength = answers.length * answers[0].getImageAnnotations().length;
        final String[] labels = new String[resultLength];
        int count = 0;
        for(int i=0; i < answers.length; i++){
            for(int k = 0; k < answers[i].getImageAnnotations().length; k++) {
                labels[count] = answers[i].getImageAnnotations()[k].label;
                count++;
            }
        }
        List labelsList = Arrays.asList(labels);
        Set<String> labelsSet = new HashSet<String>(labelsList);

        majorityCounts.clear();
        for(String label : labelsSet){
            majorityCounts.add(new MajorityCount(Collections.frequency(labelsList, label), label));
        }
        Collections.sort(majorityCounts, new MajorityCountComparator());

        int sum = 0;
        for(MajorityCount majorityCount : majorityCounts){
            sum += majorityCount.count;
            stringBuilder.append(majorityCount.label).append(": ").append(Collections.frequency(labelsList, majorityCount.label)).append("\n");
        }

        resultLabel = majorityCounts.get(0).label;
        confidence = ((float)majorityCounts.get(0).count / sum);
    }

    public int[] getMeanPose(){
        if(answers.length > 0){
            int nRetrieved = (answers.length * answers[0].getImageAnnotations().length);
            return new int[]{
                    sumX / nRetrieved ,
                    sumY / nRetrieved ,
                    sumYaw / nRetrieved ,
                    sumPitch / nRetrieved
            };
        }
        Log.e(TAG, "Couldn't calculate mean!");
        return null;
    }

    public String getResultLabel() {
        return resultLabel;
    }

    public float getConfidence() {
        return confidence;
    }

}

