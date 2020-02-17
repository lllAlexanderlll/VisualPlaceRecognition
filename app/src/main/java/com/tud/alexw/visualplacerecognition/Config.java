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

    private int width;
    private int height;
    private List<File> codebookFiles;
    private String[] codebookFilePaths;
    private int[] codebookSizes;

    private boolean doPCA;
    private String pcaFilePath;
    private File pcaFile;
    private int projectionLength;
    private boolean doWhitening;

    private File linearIndexDir;
    private File pqIndexDir;
    private File pqCodebookFile;

    private String linearIndexDirPath, pqIndexDirPath, pqCodebookFilePath;

    private int nNearestNeighbors;
    private int nQueriesForResult;
    private int nSubVectors,  nProductCentroids;
    private int indexSize;
    private boolean readOnly;
    private int vectorLength;
    private boolean doPQ;

    private boolean doRunTests;
    private String testName;
    private File testDatasetDir;
    private String testDatasetPath;

    private StringBuilder stringBuilder;


    public Config(
            Context context,
            boolean doRunTests,
            String testName,
            String testDatasetPath,
            boolean doPQ,
            int width, int height, //image
            String[] codebookFilePaths, int[] codebookSizes, //codebook
            boolean doPCA, String pcaFilePath, int projectionLength, boolean doWhitening, //pca
            String linearIndexDirPath, //linear index
            String pqIndexDirPath, String pqCodebookFilePath, int nSubVectors, int nProductCentroids,//PQ
            int vectorLength, int indexSize, // index general
            boolean readOnly, //TODO  test this
            int nNearestNeighbors,
            int nQueriesForResult) throws IOException {


        this.testDatasetPath = testDatasetPath;
        this.testDatasetDir = testDatasetPath.isEmpty() ? null :  new File(context.getExternalFilesDir(null), testDatasetPath);;
        this.doRunTests = doRunTests;
        this.testName = testName;
        this.nNearestNeighbors = nNearestNeighbors;
        this.doPQ = doPQ;
        this.indexSize = indexSize;
        this.nSubVectors = nSubVectors;
        this.readOnly = readOnly;
        this.nProductCentroids = nProductCentroids;
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
        this.codebookSizes = codebookSizes;
        this.pcaFilePath = pcaFilePath;
        this.linearIndexDirPath = linearIndexDirPath;
        this.pqCodebookFilePath = pqCodebookFilePath;
        this.pqIndexDirPath = pqIndexDirPath;
        this.pcaFile = pcaFilePath.isEmpty() ? null :  new File(context.getExternalFilesDir(null), pcaFilePath);
        this.linearIndexDir = linearIndexDirPath.isEmpty() ? null : new File(context.getExternalFilesDir(null), linearIndexDirPath);
        this.pqIndexDir = pqIndexDirPath.isEmpty() ? null : new File(context.getExternalFilesDir(null), pqIndexDirPath);
        this.pqCodebookFile = pqCodebookFilePath.isEmpty() ? null : new File(context.getExternalFilesDir(null), pqCodebookFilePath);

        this.nQueriesForResult = nQueriesForResult;

        if(!codebookFiles.get(0).exists()){
            throw new IOException("Required files not found! \n" + toString());
        }

    }

    @NonNull
    @Override
    public String toString() {
        stringBuilder = new StringBuilder();
        return stringBuilder
                        //Evaluation
                        .append("doRunTests=").append(doRunTests).append("\n")
                        .append("testName=").append(testName).append("\n")
                        .append("testDatasetDir=").append(testDatasetPath).append("\n")
                        //Vectorisation
                        .append("imageWidth=").append(width).append("\n")
                        .append("imageHeight=").append(height).append("\n")
                        .append("codebookFilePaths=").append(Arrays.toString(codebookFilePaths)).append("\n")
                        .append("codebookSizes=").append(Arrays.toString(codebookSizes)).append("\n")
                        //PCA
                        .append("pcaPath=").append(pcaFilePath).append("\n")
                        .append("doPCA=").append(doPCA).append("\n")
                        .append("projectionLength=").append(projectionLength).append("\n")
                        .append("doWhitening=").append(doWhitening).append("\n")
                        //Index
                        .append("indexSize=").append(indexSize).append("\n")
                        .append("indexIsReadOnly=").append(readOnly).append("\n")
                        .append("isPQIndex=").append(doPQ).append("\n")
                        .append("linearIndexPath=").append(linearIndexDirPath).append("\n")
                        .append("pqIndexPath=").append(pqIndexDirPath).append("\n")
                        .append("pqCodebookPath=").append(pqCodebookFilePath).append("\n")
                        .append("nSubvectors=").append(nSubVectors).append("\n")
                        .append("nCentroidsPerSubvector=").append(nProductCentroids).append("\n")
                        //Search
                        .append("nNN=").append(nNearestNeighbors).append("\n")
                        .append("nQueriesForResult=").append(nQueriesForResult).append("\n")
                        .toString();
    }

    public int generateHashCode(){
        return toString().hashCode();
    }

    public String getBaseFilename(){
        return testName + "_" + generateHashCode();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<File> getCodebookFiles() {
        return codebookFiles;
    }

    public int[] getCodebookSizes() {
        return codebookSizes;
    }

    public boolean isDoPCA() {
        return doPCA;
    }

    public File getPcaFile() {
        return pcaFile;
    }

    public int getProjectionLength() {
        return projectionLength;
    }

    public boolean isDoWhitening() {
        return doWhitening;
    }

    public File getLinearIndexDir() {
        return linearIndexDir;
    }

    public File getPqIndexDir() {
        return pqIndexDir;
    }

    public File getPqCodebookFile() {
        return pqCodebookFile;
    }

    public int getnNearestNeighbors() {
        return nNearestNeighbors;
    }

    public int getnQueriesForResult() {
        return nQueriesForResult;
    }

    public void setnQueriesForResult(int nQueriesForResult) {
        this.nQueriesForResult = nQueriesForResult;
    }

    public int getnSubVectors() {
        return nSubVectors;
    }

    public int getnProductCentroids() {
        return nProductCentroids;
    }

    public int getIndexSize() {
        return indexSize;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public int getVectorLength() {
        return vectorLength;
    }

    public boolean isDoPQ() {
        return doPQ;
    }

    public boolean isDoRunTests() {
        return doRunTests;
    }

    public String getTestName() {
        return testName;
    }

    public File getTestDatasetDir() {
        return testDatasetDir;
    }

}
