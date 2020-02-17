package com.tud.alexw.visualplacerecognition.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.TextView;

import com.tud.alexw.visualplacerecognition.Config;
import com.tud.alexw.visualplacerecognition.Utils;
import com.tud.alexw.visualplacerecognition.VLADPQFramework;
import com.tud.alexw.visualplacerecognition.result.Annotation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Tester {

    private String TAG = "Tester";
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

    private TextView mTextView;
    private VLADPQFramework mVLADPQFramework;
    private Context mContext;
    private Config mConfig;
    private StringBuilder stringBuilderResultCSV;
    private StringBuilder stringBuilderQueryCSV;

    public Tester(Context context,VLADPQFramework vladpqFramework, TextView textView){
        mTextView = textView;
        mVLADPQFramework = vladpqFramework;
        mContext = context;
        mConfig = mVLADPQFramework.mConfig;
        stringBuilderResultCSV = new StringBuilder();
        stringBuilderQueryCSV = new StringBuilder();
    }

    public void test() throws Exception {
        // dump config
        String configDumpeFilename = mConfig.getBaseFilename() + ".config";
        saveAsFile(configDumpeFilename, mConfig.toString());

        Bitmap bitmap;
        Annotation annotation;
        File[] files = mConfig.getTestDatasetDir().listFiles();
        for (int i = 0; i < files.length; ++i) {
            File file = files[i];
            if (!file.isDirectory() && file.getAbsolutePath().endsWith(".jpg")) {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                mVLADPQFramework.inferenceAndNNS(bitmap);
                annotation = Annotation.fromFilename(file.getName());
                if(annotation != null){
                    // queryNumb, trueLabel, trueX, trueY, trueYaw, truePitch
                    stringBuilderQueryCSV
                            .append(i).append(",")
                            .append(annotation.x).append(",")
                            .append(annotation.y).append(",")
                            .append(annotation.yaw).append(",")
                            .append(annotation.pitch).append("\n");

                }
                else{
                    Log.e(TAG, "Couldn't decode query filename!");
                    return;
                }

                // resultCount, resultLabel, confidence, meanX, meanY, meanYaw, meanPitch,
                if(mVLADPQFramework.getResultCounter() % mConfig.getnQueriesForResult() == 0){

                    int[] meanResultPose = mVLADPQFramework.getMeanPose();
                    stringBuilderResultCSV
                            .append(mVLADPQFramework.getResultCounter()).append(",")
                            .append(mVLADPQFramework.getResultLabel()).append(",")
                            .append(mVLADPQFramework.getConfidence()).append(",")
                            .append(meanResultPose[0]).append(",")
                            .append(meanResultPose[1]).append(",")
                            .append(meanResultPose[2]).append(",")
                            .append(meanResultPose[3]).append("\n");

                }

            }
        }
        saveAsFile(mConfig.getBaseFilename() + "_result_annotations.txt", mVLADPQFramework.getAnnotationsCSVContent());
        saveAsFile(mConfig.getBaseFilename() + "_query_annotations.txt", stringBuilderQueryCSV.toString());
        saveAsFile(mConfig.getBaseFilename() + "_results.txt", stringBuilderResultCSV.toString());
    }

    private boolean saveAsFile(String filename, String content){
        File file = new File(mContext.getExternalFilesDir(null), filename);
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(content.getBytes());
            return true;
        }
        catch (IOException e){
            Log.e(TAG, Log.getStackTraceString(e));
            Utils.addTextRed(mTextView, Log.getStackTraceString(e));
            return false;
        }
    }


}
