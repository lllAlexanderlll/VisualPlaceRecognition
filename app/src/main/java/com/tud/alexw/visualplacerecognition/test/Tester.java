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
    // precision, recall, recall@k(-NN), precision@k(-NN)
    // averageRecall, averagePrecision, averageRecall@k(-NN), averagePrecision@k(-NN)
    // Single System wide number: mAP

    TextView mTextView;
    VLADPQFramework mVLADPQFramework;
    Context mContext;
    String mConfigName;
    SharedPreferences mSharedPref;

    public Tester(String configName, Context context,VLADPQFramework vladpqFramework, TextView textView){
        mTextView = textView;
        mVLADPQFramework = vladpqFramework;
        mConfigName = configName;
        mContext = context;
        createConfig(configName);
    }


    private  void createConfig(String name){
        mSharedPref = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);

        String key = "dataSetPath";
        if(!mSharedPref.contains(key)){
            mSharedPref.edit().putString(key,"captureHomeTest").apply();
        }

        key = "kNN";
        if(!mSharedPref.contains(key)) {
            mSharedPref.edit().putInt(key, mVLADPQFramework.mConfig.maxIndexSize).apply();
        }

    }

    public void test() {
       mTextView.append("kNN:" + mSharedPref.getInt("kNN", 0));
       mTextView.append("datasetPath:" + mSharedPref.getInt("dataSetPath", 0));
        // create testID
        // dump config


        //for each image in test folder
        //  vladpqFramework.inferenceAndSearch(image)


        //  vladpqFramework.toCSV()
        //write to text file
    }




}
