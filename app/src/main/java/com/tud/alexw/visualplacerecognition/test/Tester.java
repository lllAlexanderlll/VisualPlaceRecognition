package com.tud.alexw.visualplacerecognition.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;

import com.tud.alexw.visualplacerecognition.R;
import com.tud.alexw.visualplacerecognition.VLADPQFramework;

public class Tester {

    // test:
    // with 'k = index size' for small indexes
    // with 'k = '
    // one test for scalability with artificial images (rotated [1,2,3,4,5]*(+-1) --> x10 #images)

    // dump config + use its hash as shared id for files
    // resultCount, resultLabel, confidence, trueLabel, trueX, trueY, trueYaw, truePitch
    // resultCount, rank, inferenceTime, searchTime, label, x, y, yaw, pitch

    // TODO: calc with Python:
    // need to know:
    // precision: OutOfRetrievedSet(TP/(TP + FP))
    // for recall: FN (misses) --> number of place representations in dataset is needed in python script
    // precision, recall, recall@k(-NN), precision@k(-NN)
    // averageRecall, averagePrecision, averageRecall@k(-NN), averagePrecision@k(-NN)
    // Single System wide number: mAP

    TextView mTextView;
    VLADPQFramework mVLADPQFramework;
    Context mContext;

    public Tester(Context context,VLADPQFramework vladpqFramework, TextView textView){
        mTextView = textView;
        mVLADPQFramework = vladpqFramework;
        mContext = context;
    }

    public void test() {
        // create testID
        // dump config


        //for each image in test folder
        //  vladpqFramework.inferenceAndSearch(image)


        //  vladpqFramework.toCSV()
        //write to text file
    }




}
