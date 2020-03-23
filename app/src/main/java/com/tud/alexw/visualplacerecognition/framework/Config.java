package com.tud.alexw.visualplacerecognition.framework;

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
    private File baseTestDir;
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
            int nQueriesForResult) throws IOException, IllegalArgumentException {


        this.testDatasetPath = testDatasetPath;
        this.testDatasetDir = testDatasetPath.isEmpty() ? null :  new File(context.getExternalFilesDir(null), testDatasetPath);
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
        if(nQueriesForResult <= 0){
            throw new IllegalArgumentException("nQueriesForResult must be positive and not zero! \n" + toString());
        }
        if(!codebookFiles.get(0).exists()){
            throw new IOException("Codebook required, but does not exist! \n" + codebookFiles.get(0));
        }

//        baseTestDir = new File(context.getExternalFilesDir(null), getBaseFilename());
//        if(!baseTestDir.exists()){
//            if(!baseTestDir.mkdirs()){
//                throw new IOException("Couldn't create base dir! \n" + baseTestDir.getAbsolutePath());
//            }
//        }

    }

    @NonNull
    @Override
    public String toString() {
        stringBuilder = new StringBuilder();
        return stringBuilder
            .append("[Test]").append("\n")
            .append("doRunTests=").append(doRunTests).append("\n")
            .append("testName=").append(testName).append("\n")
            .append("testDatasetDir=").append(testDatasetPath).append("\n")

            .append("\n[Vectorisation]").append("\n")
            .append("imageWidth=").append(width).append("\n")
            .append("imageHeight=").append(height).append("\n")
            .append("codebookFilePaths=").append(Arrays.toString(codebookFilePaths)).append("\n")
            .append("codebookSizes=").append(Arrays.toString(codebookSizes)).append("\n")
            .append("pcaPath=").append(pcaFilePath).append("\n")
            .append("doPCA=").append(doPCA).append("\n")
            .append("projectionLength=").append(projectionLength).append("\n")
            .append("doWhitening=").append(doWhitening).append("\n")

            .append("\n[Index]").append("\n")
            .append("indexSize=").append(indexSize).append("\n")
            .append("indexIsReadOnly=").append(readOnly).append("\n")
            .append("isPQIndex=").append(doPQ).append("\n")
            .append("linearIndexPath=").append(linearIndexDirPath).append("\n")
            .append("pqIndexPath=").append(pqIndexDirPath).append("\n")
            .append("pqCodebookPath=").append(pqCodebookFilePath).append("\n")
            .append("nSubvectors=").append(nSubVectors).append("\n")
            .append("nCentroidsPerSubvector=").append(nProductCentroids).append("\n")

            .append("\n[Search]").append("\n")
            .append("nNN=").append(nNearestNeighbors).append("\n")
            .append("nQueriesForResult=").append(nQueriesForResult).append("\n")
            .toString();
    }

    public static Config getConfigAndroid(Context context) throws IOException {
        int projectedVectorLength = 96;
        return new Config(
                context,
                true,
                "testDataset_mAP",
                "testDataset",
                false,
                960, //960x540 or 640x480
                540,
                new String[]{
                        "codebooks/codebook_split_0.csv",
                        "codebooks/codebook_split_1.csv",
                        "codebooks/codebook_split_2.csv",
                        "codebooks/codebook_split_3.csv",
                },
                new int[]{128,128,128,128},
                true,
                "pca96/pca_32768_to_96.txt",
                projectedVectorLength,
                true,
                "linearIndex4Codebooks128WithPCAw96/BDB_518400_surf_32768to96w/", //"linearIndex4Codebooks128WithPCA96/BDB_518400_surf_32768to96/", //linearIndex4Codebooks128WithPCAw96/BDB_518400_surf_32768to96w/
                "pqIndex4Codebooks128WithPCAw96/", //"pqIndex4Codebooks128WithPCA96/", //pqIndex4Codebooks128WithPCAw96/
                "pqIndex4Codebooks128WithPCAw96/pq_96_8x3_1244.csv", //"pqIndex4Codebooks128WithPCA96/pq_96_8x3_1244.csv", //pqIndex4Codebooks128WithPCAw96/pq_96_8x3_1244.csv
                8,
                10,
                projectedVectorLength,
                1244,
                true,
                10,
                1
        );
    }

    public static Config getConfigLoomo(Context context) throws IOException {
        int projectedVectorLength = 48;
        Config conf = new Config(
                context,
                true,
                "deployTest_nCodebooks_4_pca_48w_m8_k10_nNN_5_majorityCount_4",
                "deploy/testsets/deploy_testset",
                true,
                816, //960x540 or 640x480
                612,
                new String[]{
                        "deploy/codebooks/codebook_features_split_0_dim_64_centroids_128.csv",
                        "deploy/codebooks/codebook_features_split_1_dim_64_centroids_128.csv",
                        "deploy/codebooks/codebook_features_split_2_dim_64_centroids_128.csv",
                        "deploy/codebooks/codebook_features_split_3_dim_64_centroids_128.csv"
                },
                new int[]{128,128,128,128},
                true,
                "deploy/pca/pca_32768_to_48.txt",
                projectedVectorLength,
                true,
                "",
                "deploy/pqIndexes/codebook4/BDB_499392_surf_32768to48w_m8_k10",
                "deploy/pqIndexes/codebook4/BDB_499392_surf_32768to48w_m8_k10/pq_48_8x3_3831.csv",
                8,
                10,
                projectedVectorLength,
                3831,
                true,
                5,
                4
        );

        if(conf.getnQueriesForResult() == 1){
            conf.setTestName(conf.getTestName() + "_mAP");
        }

        return conf;
    }

    public int generateHashCode(){
        return toString().hashCode();
    }

    public String getBaseDirName(){
        return testName + "_" + generateHashCode() + "/";
    }

    public String getBaseFilename(){
        return getBaseDirName() + testName + "_" + generateHashCode();
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

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public File getTestDatasetDir() {
        return testDatasetDir;
    }

}
