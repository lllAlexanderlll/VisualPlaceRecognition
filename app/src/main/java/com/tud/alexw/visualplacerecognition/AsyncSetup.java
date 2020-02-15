package com.tud.alexw.visualplacerecognition;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

//Params, the type of the parameters sent to the task upon execution.
//Progress, the type of the progress units published during the background computation.
//Result, the type of the result of the background computation.
class AsyncSetup extends AsyncTask<Void, Void, String> {

    public static String TAG = "AsyncSetup";
    private Context mContext;
    private TextView mTextView;
    private Button mButton;
    private VLADPQFramework mVladpqFramework;
    private boolean mIsTest;

    public AsyncSetup (VLADPQFramework vladpqFramework, TextView textView, Button button, boolean isTest, Context context){
        mVladpqFramework = vladpqFramework;
        mContext = context;
        mTextView = textView;
        mButton = button;
        mIsTest = isTest;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try{

            long start = System.currentTimeMillis();
            mVladpqFramework.setup();
            if(mVladpqFramework.mConfig.doPQ){
                mVladpqFramework.loadPQIndex();
            }
            else{
                mVladpqFramework.loadLinearIndex();
            }
            return (mVladpqFramework.mConfig.doPQ ? "PQ" : "Linear") + " index loaded in " + Utils.blue((System.currentTimeMillis() - start) + " ms");


        }catch (Exception e){
            String msg = Log.getStackTraceString(e);
            Log.e(TAG, e.getMessage() + "\n" + msg);
            return msg;
        }
    }

    protected void onPostExecute(String result) {

         mButton.setEnabled(!(result.isEmpty() || mIsTest));
         if(result.startsWith("Pipeline setup successful")){
             Utils.addText(mTextView, result);
         }
         else{
             Utils.addTextRed(mTextView, result);
         }

    }
}