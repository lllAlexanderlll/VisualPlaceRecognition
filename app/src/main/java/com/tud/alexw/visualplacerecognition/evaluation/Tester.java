package com.tud.alexw.visualplacerecognition.evaluation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.tud.alexw.visualplacerecognition.framework.Config;
import com.tud.alexw.visualplacerecognition.Utils;
import com.tud.alexw.visualplacerecognition.framework.VLADPQFramework;
import com.tud.alexw.visualplacerecognition.framework.ImageAnnotation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Tester extends AsyncTask<Void, Void, String>{

    private String TAG = "Tester";
    // test:
    // with 'k = 100' (kNN) --> search for good k with calculation of P@k
    // several tests for nQueriesForResult [1, 2, 3, 4, 5, 6, 7, 8]

    //at the end
    // one test for scalability with artificial images (rotated [1,2,3,4,5]*(+-1) --> x10 #images)

    // dump config + use its hash as shared id for files
    // resultCount, resultLabel, confidence, meanX, meanY, meanYaw, meanPitch
    // queryNumb, trueLabel, trueX, trueY, trueYaw, truePitch
    // queryNumb, resultCount, rank, inferenceTime, searchTime, label, x, y, yaw, pitch


    // TODO: calc with Python:
    // need to know:
    // precision: OutOfRetrievedSet(TP/(TP + FP))
    // for recall: FN (misses) --> number of place representations in dataset is needed in python script
    // precision, recall, recall@k(-NN), precision@k(-NN)
    // averageRecall, averagePrecision, averageRecall@k(-NN), averagePrecision@k(-NN) --> to search for appropriate k
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
        stringBuilderResultCSV = new StringBuilder("resultCount,resultLabel,confidence,meanX,meanY,meanYaw,meanPitch\n");
        stringBuilderQueryCSV = new StringBuilder("queryNumb,inferenceTime,searchTime,trueLabel,trueX,trueY,trueYaw,truePitch,path\n");
    }

    private boolean test() throws Exception {
        Bitmap bitmap;
        ImageAnnotation imageAnnotation;
        int lastResultCounter =  0;
        List<File> files;
        int count = 0;
        for (File directory : mConfig.getTestDatasetDir().listFiles(File::isDirectory)) {
            files = Arrays.asList(directory.listFiles());

            int nFilesWithoutResult = files.size() % mConfig.getnQueriesForResult();
            for (int i = 0; i < files.size() - nFilesWithoutResult; ++i) {
                File file = files.get(i);

                if (!file.isDirectory() && file.getAbsolutePath().endsWith(".jpg")) {
                    Log.i(TAG, String.format("Test image %d of %d %s (%d total)", i, files.size() - nFilesWithoutResult, directory.getName(), count));
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    mVLADPQFramework.inferenceAndNNS(bitmap);
                    Log.i(TAG, "Decoding query file:");
                    imageAnnotation = ImageAnnotation.decodeFilename(file.getName());
                    if (imageAnnotation != null) {
                        stringBuilderQueryCSV
                                .append(count).append(",")
                                .append(mVLADPQFramework.getInferenceTime()).append(",")
                                .append(mVLADPQFramework.getmSearchTime()).append(",")
                                .append(imageAnnotation.label).append(",")
                                .append(imageAnnotation.x).append(",")
                                .append(imageAnnotation.y).append(",")
                                .append(imageAnnotation.yaw).append(",")
                                .append(imageAnnotation.pitch).append(",")
                                .append(file.getParent() + "/" + file.getName()).append("\n");
                        count++;
                    } else {
                        Log.e(TAG, "Couldn't decode query filename!");
                        return false;
                    }
                    if (mVLADPQFramework.getResultCounter() > 0 && mVLADPQFramework.getResultCounter() > lastResultCounter) {

                        int[] meanResultPose = mVLADPQFramework.getMeanPose();
                        stringBuilderResultCSV
                                .append(mVLADPQFramework.getResultCounter()-1).append(",")
                                .append(mVLADPQFramework.getResultLabel()).append(",")
                                .append(mVLADPQFramework.getConfidence()).append(",")
                                .append(meanResultPose[0]).append(",")
                                .append(meanResultPose[1]).append(",")
                                .append(meanResultPose[2]).append(",")
                                .append(meanResultPose[3]).append("\n");
                    }
                    lastResultCounter = mVLADPQFramework.getResultCounter();
                }
            }
        }
        return true;
    }

    private synchronized boolean saveAsFile(String filename, String content){
        File file = new File(mContext.getExternalFilesDir(null), filename);
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(content.getBytes());
            Utils.addText(mTextView, "Written to " + file.getAbsolutePath());
            return true;
        }
        catch (IOException e){
            Log.e(TAG, Log.getStackTraceString(e));
            Utils.addTextRed(mTextView, Log.getStackTraceString(e));
            return false;
        }
    }


    @Override
    protected String doInBackground(Void... voids) {
        try {
            if(!test()){
                return "Error! File decoding/saving failed!";
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return "Error! " + Log.getStackTraceString(e);
        }
        return "Ok";
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Utils.addText(mTextView, "Performing tests...");


        File dir = new File(mContext.getExternalFilesDir(null), mConfig.getBaseDirName());
        if(!dir.exists()){
            if(!dir.mkdir());
            {
                Log.e(TAG, "Couldn't create test base dir");
            }
        }
        // dump config
        if(!saveAsFile(mConfig.getBaseFilename() + ".config", mConfig.toString())){
            this.cancel(true);
        }
    }

    @Override
    protected void onPostExecute(String string) {
        super.onPostExecute(string);
        if(string.startsWith("Error")){
            Utils.addTextRed(mTextView, string);
        }
        else{
            boolean ok = true;
            ok &= saveAsFile(mConfig.getBaseFilename() + "_result_annotations.csv", mVLADPQFramework.getAnnotationsCSVContent());
            ok &= saveAsFile(mConfig.getBaseFilename() + "_query_annotations.csv", stringBuilderQueryCSV.toString());
            ok &= saveAsFile(mConfig.getBaseFilename() + "_results.csv", stringBuilderResultCSV.toString());
            if(ok){
                Utils.addText(mTextView, "Tests successfully done");
            }
        }
        Utils.addText(mTextView, "[ " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()) + " ]");

    }
}
