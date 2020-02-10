package com.tud.alexw.visualplacerecognition;

import androidx.appcompat.app.AppCompatActivity;
import gr.iti.mklab.visual.datastructures.PQ;
import gr.iti.mklab.visual.utilities.Answer;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "MainActivity";
    VLADPQFramework vladpqFramework;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button clickButton = (Button) findViewById(R.id.capture);
        clickButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);
                try{
                    long start = System.currentTimeMillis();
                    double[] vladVector = vladpqFramework.inference(bitmap, 960, 540,4096);
                    if(vladpqFramework.isDoPCA()){
                        addStatus("PCA: " + blue("true"));
                        addStatus(String.format("Whitening: %s", blue(Boolean.toString(vladpqFramework.isDoWhitening()))));
                    }
                    addStatus(String.format("Vectorized image to VLAD vector! (Length: %d) %s", vladVector.length, blue((System.currentTimeMillis() - start) + " ms")));

                    start = System.currentTimeMillis();
                    Answer answer = vladpqFramework.search(5, vladVector);
                    addStatusBlue("Search result:");
                    addStatus(answer.toString());
                    addStatus(String.format("Search took %s", blue((System.currentTimeMillis() - start) + " ms")));
                }
                catch(Exception e){
                    String msg = Log.getStackTraceString(e);
                    addStatusRed(msg);
                    Log.e(TAG, e.getMessage() + "\n" + msg);
                }

            }
        });



        try{

            File codebookFile = new File(getExternalFilesDir(null), "codebook/features.csv_codebook-64A-64C-100I-1S_power+l2.csv");
            File pcaFile = new File(getExternalFilesDir(null), "pca/linearIndexBDB_307200_surf_4096pca_245_128_10453ms.txt");
            File linearIndexDir = new File(getExternalFilesDir(null), "linearIndex/BDB_307200_surf_4096/");
            File pqIndexDir = new File(getExternalFilesDir(null), "pqIndex/");
            File pqCodebookFile = new File(getExternalFilesDir(null), "pqCodebook/pq_4096_8x3_244.csv");

            long start = System.currentTimeMillis();
            vladpqFramework = new VLADPQFramework(new File[]{codebookFile}, new int[]{64}, pcaFile, 128);
            vladpqFramework.loadPQIndex(pqIndexDir, pqCodebookFile);
            addStatus("Pipeline setup successful: " + blue((System.currentTimeMillis() - start) + " ms"));



        }catch (Exception e){
            String msg = Log.getStackTraceString(e);
            addStatusRed(msg);
            Log.e(TAG, e.getMessage() + "\n" + msg);
        }


    }


    private void addStatus(String msg){
        TextView textView = (TextView)findViewById(R.id.status);
        textView.append(Html.fromHtml("<br/>" + msg));
    }

    private void addStatusRed(String msg){
        TextView textView = (TextView)findViewById(R.id.status);
        textView.append(Html.fromHtml( "<br/>" + red(msg)));
    }

    private String blue(String msg){
        return "<font color=#42baff>" + msg +"</font>";
    }

    private String red(String msg){
        return "<font color=#ff0000>" + msg +"</font>";
    }

    private void addStatusBlue(String msg){
        TextView textView = (TextView)findViewById(R.id.status);
        textView.append(Html.fromHtml("<br/>" + blue(msg)));
    }
}
