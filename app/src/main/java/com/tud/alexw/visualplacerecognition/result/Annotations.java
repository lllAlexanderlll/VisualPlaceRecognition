package com.tud.alexw.visualplacerecognition.result;

import android.util.Log;

import com.tud.alexw.visualplacerecognition.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;

public class Annotations{
    private static String TAG = "Annotations";

    private int sumX = 0;
    private int sumY = 0;
    private int sumPitch = 0;
    private int sumYaw = 0;
    private List<Annotation> annotationList;
    List<MajorityCount> majorityCounts;
    String resultLabel;
    float confidence;



    Annotations(){
        annotationList = new LinkedList<>();
        majorityCounts = new LinkedList<>();
    }

    public void addAnotation(Annotation annotation){
        annotationList.add(annotation);
        sumX += annotation.x;
        sumY += annotation.y;
        sumPitch += annotation.pitch;
        sumYaw += annotation.yaw;
    }

    private class MajorityCount {
        public int count;
        public String label;

        public MajorityCount(int count, String label) {
            this.count = count;
            this.label = label;
        }
    }

    class MajorityCountComparator implements Comparator<MajorityCount> {
        @Override
        public int compare(MajorityCount a, MajorityCount b) {
            return -1*Integer.compare(a.count, b.count);
        }
    }

    public String getLabelCount(){
        final String[] labels = new String[annotationList.size()];
        for(int i=0; i < annotationList.size(); i++){
            labels[i] = annotationList.get(i).label;
        }
        List labelsList = Arrays.asList(labels);
        Set<String> labelsSet = new HashSet<String>(labelsList);

        for(String label : labelsSet){
            majorityCounts.add(new MajorityCount(Collections.frequency(labelsList, label), label));
        }

        StringBuilder stringBuilder = new StringBuilder();
        Collections.sort(majorityCounts, new MajorityCountComparator());
        int sum = 0;
        for(MajorityCount majorityCount : majorityCounts){
            sum += majorityCount.count;
            stringBuilder.append(majorityCount.label).append(": ")
                .append(Collections.frequency(labelsList, majorityCount.label)).append("\n");
        }
        resultLabel = majorityCounts.get(0).label;
        confidence = ((float)majorityCounts.get(0).count / sum);
        return stringBuilder.toString();
    }

    public int[] getMeanPose(){
        if(annotationList.size() > 0){
            return new int[]{
                    sumX / annotationList.size(),
                    sumY / annotationList.size(),
                    sumYaw / annotationList.size(),
                    sumPitch / annotationList.size()
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

    @NonNull
    @Override
    public String toString() {
        return getLabelCount() + Arrays.toString(getMeanPose());
    }


}
