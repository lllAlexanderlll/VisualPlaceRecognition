package com.tud.alexw.visualplacerecognition;

import android.graphics.Bitmap;
import android.util.Log;

import com.tud.alexw.visualplacerecognition.result.ImageAnnotation;
import com.tud.alexw.visualplacerecognition.result.MajorityCount;
import com.tud.alexw.visualplacerecognition.result.Result;

import gr.iti.mklab.visual.aggregation.AbstractFeatureAggregator;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.datastructures.AbstractSearchStructure;
import gr.iti.mklab.visual.datastructures.Linear;
import gr.iti.mklab.visual.datastructures.PQ;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.utilities.Answer;

public class VLADPQFramework {

    public static String TAG = "VLADPQFramework";
    double[][][] codebooks;
    VladAggregatorMultipleVocabularies vladAggregator;
    SURFExtractor surfExtractor;
    PCA pca;
    AbstractSearchStructure index;
    boolean mIsSetup;
    public Config mConfig;
    long cacheSize_mb = 20;
    long mInferenceTime = 0;
    long mSearchTime = 0;
    int mInferenceCounter = 0;
    private Result mResult;
    private StringBuilder mStringBuilderResult;
    private StringBuilder mStringBuilderStatus;

    private StringBuilder mStringBuilderAnnotationCSV;
    private int mResultCounter = 0;

    VLADPQFramework(Config config) {
        mIsSetup = false;
        mConfig = config;
        mResult = new Result(mConfig.getnQueriesForResult());
        mStringBuilderResult = new StringBuilder();
        mStringBuilderStatus = new StringBuilder();
        mStringBuilderAnnotationCSV = new StringBuilder("queryNumb,resultCount,rank,label,x,y,yaw,pitch,distance\n");
    }

    public void setup() throws Exception {
        surfExtractor = new SURFExtractor();
        codebooks = AbstractFeatureAggregator.readQuantizers(mConfig.getCodebookFiles(), mConfig.getCodebookSizes(), AbstractFeatureExtractor.SURFLength);
        vladAggregator = new VladAggregatorMultipleVocabularies(codebooks);

        int initialLength = mConfig.getCodebookSizes().length * mConfig.getCodebookSizes()[0] * AbstractFeatureExtractor.SURFLength;
        if (mConfig.isDoPCA() && mConfig.getProjectionLength()< initialLength) {
            pca = new PCA(mConfig.getProjectionLength(), 1, initialLength, mConfig.isDoWhitening());
            pca.loadPCAFromFile(mConfig.getPcaFile());
        }
        mIsSetup = true;
    }

    private void checkSetup(){
        if(!mIsSetup){
            throw new IllegalStateException("VLADPQFramework must be setup first!");
        }
    }

    public void loadPQIndex() throws Exception {
        checkSetup();

        index = new PQ(mConfig.getVectorLength(), mConfig.getIndexSize(), mConfig.isReadOnly(), mConfig.getPqIndexDir(), mConfig.getnSubVectors(), mConfig.getnProductCentroids(), PQ.TransformationType.None, true, 0, true, cacheSize_mb);

        Log.i(TAG, "Loading the PQ from file:");
        ((PQ) index).loadProductQuantizer(mConfig.getPqCodebookFile());
        Log.i(TAG, ((PQ) index).toString());
    }

    public void loadLinearIndex() throws Exception {
        checkSetup();
        index = new Linear(mConfig.getVectorLength(), mConfig.getIndexSize(), mConfig.isReadOnly(), mConfig.getLinearIndexDir(), true, true, 0, cacheSize_mb);
        Log.i(TAG, "Linear index loaded");
    }

    private double[] inference(Bitmap bitmap) throws Exception {
        long start = System.currentTimeMillis();
        checkSetup();


        double[][] features = surfExtractor.extractFeaturesInternal(bitmap, mConfig.getWidth(), mConfig.getHeight());
        mStringBuilderStatus.append("Inference: ").append(String.format("(%dx%d)->(%dx%d) %d features", bitmap.getWidth(), bitmap.getHeight(), mConfig.getWidth(), mConfig.getHeight(), features.length)).append("\n");
        if (mConfig.getProjectionLength()> vladAggregator.getVectorLength() || mConfig.getProjectionLength() <= 0) {
            throw new Exception("Target vector length should be between 1 and " + vladAggregator.getVectorLength());
        }
        double[] vladVector = vladAggregator.aggregate(features);

        if (mConfig.isDoPCA() && vladVector.length > mConfig.getProjectionLength()) {
            // PCA projection
            int vladLength = vladVector.length;
            vladVector = pca.sampleToEigenSpace(vladVector);
            Log.i(TAG, "PCA performed.");

            mStringBuilderStatus.append("PCA");
            if(mConfig.isDoWhitening()){
                mStringBuilderStatus.append("w");
            }
            mStringBuilderStatus.append(vladLength).append("to").append(vladVector.length).append("\n");
        } else {
            Log.i(TAG, "No PCA projection needed!");
        }

        mInferenceTime = System.currentTimeMillis() - start;
        mStringBuilderStatus.append("Inference time: ").append(mInferenceTime).append("ms \n");
        mInferenceCounter++;
        return vladVector;
    }

    private Answer search(double[] vladVector) throws Exception {
        checkSetup();
        Answer answer = index.computeNearestNeighbors(mConfig.getnNearestNeighbors(), vladVector);
        mStringBuilderStatus.append(answer.toString());
        return answer;
    }

    public void inferenceAndNNS(Bitmap bitmap) throws Exception{
        double[] vector = inference(bitmap);
        Answer answer = search(vector);
        mResult.addAnswer(answer);
        addToAnnotationsCSV(answer);
        mSearchTime = answer.getIndexSearchTime() + answer.getNameLookupTime();
        if(mConfig.getnQueriesForResult() == mResult.getQueryCounter()) {
            mResult.majorityCount();
            for (MajorityCount majorityCount : mResult.getMajorityCounts()) {
                mStringBuilderResult.append(majorityCount.label).append(": ").append(majorityCount.count).append("\n");
            }
            mStringBuilderResult.append("Result: " + mResult.getResultLabel()).append(" ").append(mResult.getConfidence()).append("\n");
            mResultCounter++;
        }
    }

    public long getInferenceTime() {
        return mInferenceTime;
    }

    public long getmSearchTime() {
        return mSearchTime;
    }

    public int getmInferenceCounter() {
        return mInferenceCounter;
    }

    public int getResultCounter() {
        return mResultCounter;
    }

    public String popResultString(){
        String temp = mStringBuilderResult.toString();
        mStringBuilderResult.setLength(0);
        return temp;
    }

    public String popStatusString(){
        String temp = mStringBuilderStatus.toString();
        mStringBuilderStatus.setLength(0);
        return temp;
    }

    private void addToAnnotationsCSV(Answer answer){
        int rank = 0;
        for(ImageAnnotation imageAnnotation : answer.getImageAnnotations()){
            mStringBuilderAnnotationCSV
                    .append(mInferenceCounter-1).append(",")
                    .append(mResultCounter).append(",")
                    .append(rank).append(",")
                    .append(imageAnnotation.label).append(",")
                    .append(imageAnnotation.x).append(",")
                    .append(imageAnnotation.y).append(",")
                    .append(imageAnnotation.yaw).append(",")
                    .append(imageAnnotation.pitch).append(",")
                    .append(answer.getDistances()[rank]).append("\n");
            rank++;
        }
    }

    public String getAnnotationsCSVContent(){
        return mStringBuilderAnnotationCSV.toString();
    }

    public String getResultLabel(){
        return mResult.getResultLabel();
    }

    public float getConfidence(){
        return mResult.getConfidence();
    }

    public int[] getMeanPose(){
        return mResult.getMeanPose();
    }
}
