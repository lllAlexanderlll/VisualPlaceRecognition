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

    StringBuilder stringBuilder;


    public Config(Context context, int width, int height, String[] codebookFilePaths, Integer[] codebookSizes, boolean doPCA, String pcaFilePath, int projectionLength, boolean doWhitening, String linearIndexDirPath, String pqIndexDirPath, String pqCodebookFilePath) throws IOException {

        stringBuilder = new StringBuilder();
        stringBuilder.append("Configuration:\n");
        stringBuilder.append(String.format("Image size: %dx%d", width, height));
        stringBuilder.append("Codebook file paths: " + Arrays.toString(codebookFilePaths)  + "\n");
        stringBuilder.append("Codebook sizes: " + Arrays.toString(codebookSizes) + "\n");
        stringBuilder.append("doPCA: " + doPCA + "\n");
        stringBuilder.append("Projection length: " + projectionLength + "\n");
        stringBuilder.append("Whitening: " + doWhitening + "\n");
        stringBuilder.append("PCA path: " + pcaFilePath + "\n");
        stringBuilder.append("Linear index path: " + linearIndexDirPath + "\n");
        stringBuilder.append("PQ index path: " + pqIndexDirPath + "\n");
        stringBuilder.append("PQ codebook path: " + pqCodebookFilePath + "\n");

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

        if(!codebookFiles.get(0).exists()){
            throw new IOException("Required files not found! \n" + toString());
        }
    }

    @NonNull
    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
