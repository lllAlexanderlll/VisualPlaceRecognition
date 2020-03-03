package com.tud.alexw.visualplacerecognition.framework;

import java.util.Comparator;

public class MajorityCount {
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