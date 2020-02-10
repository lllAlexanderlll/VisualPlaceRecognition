package com.tud.alexw.visualplacerecognition;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import gr.iti.mklab.visual.aggregation.AbstractFeatureAggregator;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
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
    PQ pq;
    boolean doPCA = false;
    boolean doWhitening = true;

    VLADPQFramework( File[] codebookFiles, int[] numCentroids, File pcaFile, int projectionLength) throws Exception {
//        String[] codebookFiles = { "C:/codebook1.csv", "C:/codebook2.csv", "C:/codebook3.csv", "C:/codebook4.csv" };
//        int[] numCentroids = { 64, 64, 64, 64 };
//        String pcaFilename = "C:/pca.txt";
//        int projectionLength = 128;



        if( !(codebookFiles.length == numCentroids.length && numCentroids.length > 0)){
            throw new IllegalArgumentException("Number of codebooks and number of their sizes must be greater zero and not differ!");
        }

        surfExtractor = new SURFExtractor();
        codebooks = AbstractFeatureAggregator.readQuantizers(codebookFiles, numCentroids, AbstractFeatureExtractor.SURFLength);
        vladAggregator = new VladAggregatorMultipleVocabularies(codebooks);

        int initialLength = numCentroids.length * numCentroids[0] * AbstractFeatureExtractor.SURFLength;
        if (projectionLength < initialLength) {
            pca = new PCA(projectionLength, 1, initialLength, doWhitening);
            pca.loadPCAFromFile(pcaFile);
        }




    }

    public void loadPQIndex(File pqIndexDir, File pqCodebookFile) throws Exception {
        long cacheSize_mb = 10;
        pq = new PQ(4096, 245, false, pqIndexDir, 8, 10, PQ.TransformationType.None, true, 0, true, cacheSize_mb);

        Log.i(TAG, "Loading the PQ from file:");
        pq.loadProductQuantizer(pqCodebookFile);
        Log.i(TAG, pq.toString());

    }

    public double[] inference(Bitmap bitmap, int width, int height, int targetVectorLength) throws Exception {
        double[][] features = surfExtractor.extractFeaturesInternal(bitmap, width, height);
        if (targetVectorLength > vladAggregator.getVectorLength() || targetVectorLength <= 0) {
            throw new Exception("Target vector length should be between 1 and " + vladAggregator.getVectorLength());
        }
        double[] vladVector = vladAggregator.aggregate(features);

        if (vladVector.length == targetVectorLength) {
            Log.i(TAG, "No PCA projection needed!");
            return vladVector;
        } else {
            // PCA projection
            doPCA = true;
            double[] projected = pca.sampleToEigenSpace(vladVector);
            return projected;
        }
    }

    public Answer search(int k, double[] vladVector) throws Exception {
        return pq.computeNearestNeighbors(k, vladVector);
    }

    public boolean isDoPCA() {
        return doPCA;
    }

    public boolean isDoWhitening() {
        return doWhitening;
    }
}
