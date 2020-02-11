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

            long start = System.currentTimeMillis();
            mVladpqFramework.setup();
            mVladpqFramework.loadPQIndex();
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