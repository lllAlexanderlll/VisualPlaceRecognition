package com.tud.alexw.visualplacerecognition.framework;

import java.util.Comparator;

/**
 *
 */
public class MajorityCount {
    public int count;
    public String label;

    public MajorityCount(int count, String label) {
        this.count = count;
        this.label = label;
    }
}

/**
 * Comparator to compare two majority count data objects by their count values
 */
class MajorityCountComparator implements Comparator<MajorityCount> {

    /**
     * Compares two majority count data objects
     * @param a majority count data objects
     * @param b majority count data objects
     * @return 0 if x is equals to y
     * greater than 0 if x is less than y (for ordering)
     * less than 0 if x is greater than y (for ordering)
     */
    @Override
    public int compare(MajorityCount a, MajorityCount b) {
        return -1*Integer.compare(a.count, b.count);
    }
}