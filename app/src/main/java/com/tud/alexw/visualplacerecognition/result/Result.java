package com.tud.alexw.visualplacerecognition.result;

import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
        for(Annotation annotation :answer.getAnnotations()){

            sumX += annotation.x;
            sumY += annotation.y;
            sumPitch += annotation.pitch;
            sumYaw += annotation.yaw;
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
        final String[] labels = new String[answers.length];
        for(int i=0; i < answers.length; i++){
            for(Annotation annotation : answers[i].getAnnotations()) {
                labels[i] = annotation.label;
            }
        }
        List labelsList = Arrays.asList(labels);
        Set<String> labelsSet = new HashSet<String>(labelsList);

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
            int nRetrieved = (answers.length * answers[0].getAnnotations().length);
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

