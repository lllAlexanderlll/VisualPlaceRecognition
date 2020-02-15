package com.tud.alexw.visualplacerecognition;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;

public class Config {

    private static final String TAG = "Config";

    public int width;
    public int height;
    public List<File> codebookFiles;
    public List<Integer> codebookSizes;

    public boolean doPCA;
    public File pcaFile;
    public int projectionLength;
    public boolean doWhitening;

    public File linearIndexDir;
    public File pqIndexDir;
    public File pqCodebookFile;

    public int nNearestNeighbors;
    public int nMaxAnswers;
    public int numSubVectors,  numProductCentroids;
    public int maxIndexSize;
    public boolean readOnly;
    public int vectorLength;
    public boolean doPQ;
    public boolean doRunTests;
    public String testName;


    StringBuilder stringBuilder;


    public Config(
            Context context,
            boolean doRunTests,
            String testName,
            boolean doPQ,
            int width, int height, //image
            String[] codebookFilePaths, Integer[] codebookSizes, //codebook
            boolean doPCA, String pcaFilePath, int projectionLength, boolean doWhitening, //pca
            String linearIndexDirPath, //linear index
            String pqIndexDirPath, String pqCodebookFilePath, int numSubVectors, int numProductCentroids,//PQ
            int vectorLength, int maxIndexSize, // index general
            boolean readOnly, //TODO  test this
            int nNearestNeighbors,
            int nMaxAnswers) throws IOException {

        stringBuilder = new StringBuilder();
        stringBuilder.append("Configuration:\n")
        .append("DoRunTests: ").append(doRunTests)
        .append("Test name: ").append(testName)
        .append(String.format("Image size: %dx%d", width, height))
        .append("Codebook file paths: ").append(Arrays.toString(codebookFilePaths)).append("\n")
        .append("Codebook sizes: ").append(Arrays.toString(codebookSizes)).append("\n")
        .append("DoPCA: ").append(doPCA).append("\n")
        .append("Projection length: ").append(projectionLength).append("\n")
        .append("Whitening: ").append(doWhitening).append("\n")
        .append("PCA path: ").append(pcaFilePath).append("\n")
        .append("Max index size: ").append(maxIndexSize).append("\n")
        .append("Index is read only: ").append(readOnly).append("\n")
        .append("Index to load:").append(doPQ ? "PQ" : "Linear").append("\n")
        .append("Linear index path: ").append(linearIndexDirPath).append("\n")
        .append("PQ index path: ").append(pqIndexDirPath).append("\n")
        .append("PQ codebook path: ").append(pqCodebookFilePath).append("\n")
        .append("Number of subvectors: ").append(numSubVectors).append("\n")
        .append("Number of centroids per subvector: ").append(numProductCentroids).append("\n")
        .append("Number of nearest neighbors to retrieve: ").append(nNearestNeighbors).append("\n")
        .append("Number of maximum Answers in Majority Count: ").append(nMaxAnswers).append("\n");

        this.doRunTests = doRunTests;
        this.testName = testName;
        this.nNearestNeighbors = nNearestNeighbors;
        this.doPQ = doPQ;
        this.maxIndexSize = maxIndexSize;
        this.numSubVectors = numSubVectors;
        this.readOnly = readOnly;
        this.numProductCentroids = numProductCentroids;
        this.vectorLength = vectorLength;
        this.width = width;
        this.height = height;
        this.doPCA = doPCA;
        if(codebookFilePaths.length != codebookSizes.length && codebookFilePaths.length <= 0){
            throw new IllegalArgumentException("Faulty configuration: Number of codebooks and number of their sizes must be greater zero and not differ!\n" + toString());
        }
        this.codebookFiles = new LinkedList<>();
        for(String codebookFilePath : codebookFilePaths){
            codebookFiles.add(new File(context.getExternalFilesDir(null), codebookFilePath));
        }
        this.projectionLength = projectionLength;
        this.doWhitening = doWhitening;
        this.codebookSizes = new LinkedList<>(Arrays.asList(codebookSizes));
        this.pcaFile = pcaFilePath.isEmpty() ? null :  new File(context.getExternalFilesDir(null), pcaFilePath);
        this.linearIndexDir = linearIndexDirPath.isEmpty() ? null : new File(context.getExternalFilesDir(null), linearIndexDirPath);
        this.pqIndexDir = pqIndexDirPath.isEmpty() ? null : new File(context.getExternalFilesDir(null), pqIndexDirPath);
        this.pqCodebookFile = pqCodebookFilePath.isEmpty() ? null : new File(context.getExternalFilesDir(null), pqCodebookFilePath);

        this.nMaxAnswers = nMaxAnswers;

        if(!codebookFiles.get(0).exists()){
            throw new IOException("Required files not found! \n" + toString());
        }
    }

    @NonNull
    @Override
    public String toString() {
        return stringBuilder.toString();
    }

    public int generateHashCode(){
        return toString().hashCode();
    }
}
