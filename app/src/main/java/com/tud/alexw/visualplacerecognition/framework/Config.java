package com.tud.alexw.visualplacerecognition.framework;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Large data object which holds file paths and parameterisation for VLAD, PQ, Linear index, Codebooks, testing process (test set path, test Name, ...). Generates a hash to identify a test ran under the same configuration.
 */
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

    /**
     * Constructor with all information about VLAD, indexes, PCA, testing, image input
     * @param context the application context
     * @param doRunTests whether to conduct tests
     * @param testName the name of the test, is appended "_mAP", if nQueries for result equals 1
     * @param testDatasetPath path to the test set, which annotated images for each place in a own folder
     * @param doPQ whether the index is a PQ index
     * @param width image width to scale input images to
     * @param height image height to scale input images to
     * @param codebookFilePaths array of paths to the codebook files
     * @param codebookSizes array of codebook sizes for paths given in codebookFilePaths
     * @param doPCA whether to conduct PCA
     * @param pcaFilePath path to the pca file
     * @param projectionLength projection length after PCA
     * @param doWhitening whether the projected vectors are whitened after PCA
     * @param linearIndexDirPath path to the linear index (may be empty Stirng if doPQ)
     * @param pqIndexDirPath path to the pq index (may be empty Stirng if not doPQ)
     * @param pqCodebookFilePath path to the PQ codebook (may be empty Stirng if not doPQ)
     * @param nSubVectors number of subvectors to split a VLAD vector into by PQ
     * @param nProductCentroids number of centroids for each subvector
     * @param vectorLength vector length after VLAD and PCA
     * @param indexSize maximum number of index entries to load (usually set to size of the index)
     * @param readOnly if the index is read only
     * @param nNearestNeighbors number of nearest neighbours to retrieve for the nearest neighbour search
     * @param nQueriesForResult number of images to use for issuing a single place recognition belief (i.e. controls majority count voting). must be positive and not zero
     * @throws IOException thrown if a file path given does not exist/ could not be read
     * @throws IllegalArgumentException thrown if given parameters are incorrect
     */
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

    /**
     * returns config in windows ini format
     * @return config string in windows ini format
     */
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

    /**
     * convinient method used to load a config and not pollute main activity with parameterisation.
     * @param context Application context
     * @return config object
     * @throws IOException if a file specified in config couldn't be found or read
     */
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

    /**
     * Caluclate the hash code of the config object by hashing the config string (windows ini)
     * @return
     */
    public int generateHashCode(){
        return toString().hashCode();
    }

    /**
     * Returns root test folder path with hash reference
     * @return root test folder path with hash reference
     */
    public String getBaseDirName(){
        return testName + "_" + generateHashCode() + "/";
    }

    /**
     * Returns basic test filename path within root test folder without extension but with hash reference
     * @return
     */
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
