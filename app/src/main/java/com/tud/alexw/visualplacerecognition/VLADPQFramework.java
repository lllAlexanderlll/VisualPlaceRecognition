package com.tud.alexw.visualplacerecognition;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;

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
    Config mConfig;
    long cacheSize_mb = 10;

    VLADPQFramework(Config config) {
        mIsSetup = false;
        mConfig = config;
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

    public double[] inference(Bitmap bitmap) throws Exception {
        checkSetup();
        double[][] features = surfExtractor.extractFeaturesInternal(bitmap, mConfig.width, mConfig.height);
        if (mConfig.projectionLength > vladAggregator.getVectorLength() || mConfig.projectionLength <= 0) {
            throw new Exception("Target vector length should be between 1 and " + vladAggregator.getVectorLength());
        }
        double[] vladVector = vladAggregator.aggregate(features);


        if (mConfig.doPCA && vladVector.length != mConfig.projectionLength) {
            // PCA projection
            double[] projected = pca.sampleToEigenSpace(vladVector);
            return projected;
        } else {
            Log.i(TAG, "No PCA projection needed!");
            return vladVector;
        }
    }

    public Answer search(double[] vladVector) throws Exception {
        checkSetup();
        return index.computeNearestNeighbors(mConfig.nNearestNeighbors, vladVector);
    }
}
