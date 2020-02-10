package com.tud.alexw.visualplacerecognition;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

//Params, the type of the parameters sent to the task upon execution.
//Progress, the type of the progress units published during the background computation.
//Result, the type of the result of the background computation.
class AsyncSetup extends AsyncTask<Void, Void, String> {

    public static String TAG = "AsyncSetup";
    private Context mContext;
    private TextView mTextView;
    private Button mButton;
    private VLADPQFramework mVladpqFramework;

    public AsyncSetup (VLADPQFramework vladpqFramework, TextView textView, Button button, Context context){
        mVladpqFramework = vladpqFramework;
        mContext = context;
        mTextView = textView;
        mButton = button;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try{

            File codebookFile = new File(mContext.getExternalFilesDir(null), "codebook/features.csv_codebook-64A-64C-100I-1S_power+l2.csv");
            File pcaFile = new File(mContext.getExternalFilesDir(null), "pca/linearIndexBDB_307200_surf_4096pca_245_128_10453ms.txt");
            File linearIndexDir = new File(mContext.getExternalFilesDir(null), "linearIndex/BDB_307200_surf_4096/");
            File pqIndexDir = new File(mContext.getExternalFilesDir(null), "pqIndex/");
            File pqCodebookFile = new File(mContext.getExternalFilesDir(null), "pqCodebook/pq_4096_8x3_244.csv");

            long start = System.currentTimeMillis();
            mVladpqFramework.setup(new File[]{codebookFile}, new int[]{64}, pcaFile, 128);
            mVladpqFramework.loadPQIndex(pqIndexDir, pqCodebookFile);
            return "Pipeline setup successful: " + Utils.blue((System.currentTimeMillis() - start) + " ms");


        }catch (Exception e){
            String msg = Log.getStackTraceString(e);
            Log.e(TAG, e.getMessage() + "\n" + msg);
            return msg;
        }
    }

    protected void onPostExecute(String result) {

         mButton.setEnabled(!result.isEmpty());
         if(result.startsWith("Pipeline setup successful")){
             Toast.makeText(mContext, "Index loaded", Toast.LENGTH_LONG).show();
             Utils.addStatus(mTextView, result);
         }
         else{
             Utils.addStatusRed(mTextView, result);
         }

    }
}