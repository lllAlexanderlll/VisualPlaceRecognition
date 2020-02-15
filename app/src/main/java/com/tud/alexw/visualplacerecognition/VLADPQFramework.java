package com.tud.alexw.visualplacerecognition;

import android.graphics.Bitmap;
import android.util.Log;

import com.tud.alexw.visualplacerecognition.result.Annotations;
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
    long cacheSize_mb = 10;
    long inferenceTime = 0;
    private Result mResult;
    private Annotations mAnnotations;
    private StringBuilder mResultStringBuilder;
    private StringBuilder mStatusStringBuilder;

    // resultCount, resultLabel, confidence, trueLabel, trueX, trueY, trueYaw, truePitch
    // resultCount, rank, inferenceTime, searchTime, label, x, y, yaw, pitch
    private StringBuilder mStringBuilderCSV;
    private int resultCounter = 0;

    VLADPQFramework(Config config) {
        mIsSetup = false;
        mConfig = config;
        mResult = new Result(mConfig.nMaxAnswers);
        mResultStringBuilder = new StringBuilder();
        mStatusStringBuilder = new StringBuilder();
        mStringBuilderCSV = new StringBuilder();
    }

    public void setup() throws Exception {
        surfExtractor = new SURFExtractor();
        codebooks = AbstractFeatureAggregator.readQuantizers(mConfig.codebookFiles, mConfig.codebookSizes, AbstractFeatureExtractor.SURFLength);
        vladAggregator = new VladAggregatorMultipleVocabularies(codebooks);

        int initialLength = mConfig.codebookSizes.size() * mConfig.codebookSizes.get(0) * AbstractFeatureExtractor.SURFLength;
        if (mConfig.doPCA && mConfig.projectionLength < initialLength) {
            pca = new PCA(mConfig.projectionLength, 1, initialLength, mConfig.doWhitening);
            pca.loadPCAFromFile(mConfig.pcaFile);
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

        index = new PQ(mConfig.vectorLength, mConfig.maxIndexSize, mConfig.readOnly, mConfig.pqIndexDir, mConfig.numSubVectors, mConfig.numProductCentroids, PQ.TransformationType.None, true, 0, true, cacheSize_mb);

        Log.i(TAG, "Loading the PQ from file:");
        ((PQ) index).loadProductQuantizer(mConfig.pqCodebookFile);
        Log.i(TAG, ((PQ) index).toString());
    }

    public void loadLinearIndex() throws Exception {
        checkSetup();
        index = new Linear(mConfig.vectorLength, mConfig.maxIndexSize, mConfig.readOnly, mConfig.linearIndexDir, true, true, 0, cacheSize_mb);
        Log.i(TAG, "Linear index loaded");
    }

    private double[] inference(Bitmap bitmap) throws Exception {
        long start = System.currentTimeMillis();
        checkSetup();

        double[][] features = surfExtractor.extractFeaturesInternal(bitmap, mConfig.width, mConfig.height);
        mStatusStringBuilder.append("Inference:").append(String.format("(%dx%d)->(%dx%d) %d features", bitmap.getWidth(), bitmap.getHeight(), mConfig.width, mConfig.height, features.length)).append("\n");
        if (mConfig.projectionLength > vladAggregator.getVectorLength() || mConfig.projectionLength <= 0) {
            throw new Exception("Target vector length should be between 1 and " + vladAggregator.getVectorLength());
        }
        double[] vladVector = vladAggregator.aggregate(features);

        if (mConfig.doPCA && vladVector.length > mConfig.projectionLength) {
            // PCA projection
            int vladLength = vladVector.length;
            vladVector = pca.sampleToEigenSpace(vladVector);
            Log.i(TAG, "PCA performed.");

            mStatusStringBuilder.append("PCA");
            if(mConfig.doWhitening){
                mStatusStringBuilder.append("w");
            }
            mStatusStringBuilder.append(vladLength).append("to").append(vladVector.length).append("\n");
        } else {
            Log.i(TAG, "No PCA projection needed!");
        }

        inferenceTime = System.currentTimeMillis() - start;
        mStatusStringBuilder.append("Inference time:").append(inferenceTime).append("\n");
        return vladVector;
    }

    private Answer search(double[] vladVector) throws Exception {
        checkSetup();
        Answer answer = index.computeNearestNeighbors(mConfig.nNearestNeighbors, vladVector);
        mStatusStringBuilder.append(answer.toString());
        return answer;
    }

    public void inferenceAndNNS(Bitmap bitmap) throws Exception{
        double[] vector = inference(bitmap);
        Answer answer = search(vector);
        mAnnotations = mResult.addAnswerOrGetAnnotations(answer);
        if(mAnnotations != null){
            resultCounter++;
            mResultStringBuilder.append("Result ").append(resultCounter).append("\n")
                .append(mAnnotations.toString()).append("\n");
        }
    }

    public long getInferenceTime() {
        return inferenceTime;
    }

    public String popResultString(){
        String temp = mResultStringBuilder.toString();
        mResultStringBuilder.setLength(0);
        return temp;
    }

    public String popStatusString(){
        String temp = mStatusStringBuilder.toString();
        mStatusStringBuilder.setLength(0);
        return temp;
    }
}
