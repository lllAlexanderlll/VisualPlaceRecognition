package com.tud.alexw.visualplacerecognition;

import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Annotations{
    private static String TAG = "Annotations";

    private int sumX = 0;
    private int sumY = 0;
    private int sumPitch = 0;
    private int sumYaw = 0;
    private List<Annotation> annotationList;


    Annotations(){
        annotationList = new LinkedList<>();
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
            return Integer.compare(a.count, b.count);
        }
    }

    public String getLabelCount(){
        final String[] labels = new String[annotationList.size()];
        int i=0;
        for(Annotation annotation : annotationList){
            labels[i] = annotation.label;
            i++;
        }
        List labelsList = Arrays.asList(labels);
        Set<String> labelsSet = new HashSet<String>(labelsList);

        List<MajorityCount> majorityCounts = new LinkedList<>();
        for(String label : labelsSet){
            majorityCounts.add(new MajorityCount(Collections.frequency(labelsList, label), label));
        }

        StringBuilder sb = new StringBuilder();
        Collections.sort(majorityCounts, new MajorityCountComparator());
        int sum = 0;
        for(MajorityCount majorityCount : majorityCounts){
            sum += majorityCount.count;
            sb.append(majorityCount.label).append(": ")
                .append(Collections.frequency(labelsList, majorityCount.label)).append("\n");
        }
        sb.append(majorityCounts.get(0).label).append(": ").append(Utils.blue(Integer.toString(majorityCounts.get(0).count)));
        return sb.toString();
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

}
