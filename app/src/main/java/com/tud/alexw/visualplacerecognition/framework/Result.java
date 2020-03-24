package com.tud.alexw.visualplacerecognition.framework;

import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import gr.iti.mklab.visual.utilities.Answer;

/**
 * Contains all kNN responses (answers), keeps track of query number, for each query image of the same nQueryResult session a majority count object, the actual result label according to majority counts and its confidence (# most popular label / # all labels)
 */
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


    /**
     * Constructor for a result object containing maxAnswers answers
     * @param nMaxAnswers maximum number of answers for the results i.e. number of query images required for one result
     */
    public Result(int nMaxAnswers){
        if(nMaxAnswers <= 0){
            throw new IllegalArgumentException("nQueriesForResult must be greater than zero!");
        }
        answers = new Answer[nMaxAnswers];
        majorityCounts = new LinkedList<>();
        queryCounter = 0;
    }


    /**
     * Add an NNS answer to the result object
     * @param answer
     */
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

    /**
     * Invoked after all anwsers are added. Calculates result label and confidence by generating a majority count object for each label within answers and compares them accoridng to label occurrence count.
     */
    public void majorityCount(){

        int resultLength = answers.length * answers[0].getImageAnnotations().length;

        // generate a set of labels
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

        // generate a majority count object for each label in the label set
        majorityCounts.clear();
        for(String label : labelsSet){
            majorityCounts.add(new MajorityCount(Collections.frequency(labelsList, label), label));
        }
        Collections.sort(majorityCounts, new MajorityCountComparator());

        // compare the majority count objects and get set predicted label and its confidence
        int sum = 0;
        for(MajorityCount majorityCount : majorityCounts){
            sum += majorityCount.count;
            stringBuilder.append(majorityCount.label).append(": ").append(Collections.frequency(labelsList, majorityCount.label)).append("\n");
        }

        resultLabel = majorityCounts.get(0).label;
        confidence = ((float)majorityCounts.get(0).count / sum);
    }

    /**
     * Calculate the mean pose of all added answers
     * @return
     */
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

