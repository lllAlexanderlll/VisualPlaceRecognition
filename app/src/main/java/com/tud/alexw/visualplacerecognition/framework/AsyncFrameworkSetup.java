package com.tud.alexw.visualplacerecognition.framework;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tud.alexw.visualplacerecognition.Utils;



/**
 * Sets up the framework i.e. loads the index and PCA file asynchronously.
 * Void     -   Params, the type of the parameters sent to the task upon execution.
 * Void     -   Progress, the type of the progress units published during the background computation.
 * String   -   Result, the type of the result of the background computation.
 */
public class AsyncFrameworkSetup extends AsyncTask<Void, Void, String> {

    public static String TAG = "AsyncSetup";
    private Context mContext;
    private TextView mTextView;
    private Button mButton;
    private VLADPQFramework mVladpqFramework;
    private boolean mIsTest;

    /**
     * Constructor for VLADFRamework setup
     * @param vladpqFramework the vladFramework object to set up
     * @param textView the textView to write status messages to
     * @param button reference to the capturing button, which is enabled after successful framework setup. Expected to be null for test conduction
     * @param context application context
     */
    public AsyncFrameworkSetup(VLADPQFramework vladpqFramework, TextView textView, Button button, Context context) {
        mVladpqFramework = vladpqFramework;
        mContext = context;
        mTextView = textView;
        mButton = button;
    }

    /**
     * Sets up vlad framework according to the config referenced by the vlad framework object
     * @param voids
     * @return
     */
    @Override
    protected String doInBackground(Void... voids) {
        try {

            long start = System.currentTimeMillis();
            mVladpqFramework.setup();
            if (mVladpqFramework.mConfig.isDoPQ()) {
                mVladpqFramework.loadPQIndex();
            } else {
                mVladpqFramework.loadLinearIndex();
            }
            return (mVladpqFramework.mConfig.isDoPQ() ? "PQ" : "Linear") + " index loaded in " + Utils.blue((System.currentTimeMillis() - start) + " ms");


        } catch (Exception e) {
            String msg = Log.getStackTraceString(e);
            Log.e(TAG, e.getMessage() + "\n" + msg);
            return "Error! " + msg;
        }
    }

    /**
     * Reports success of the setuo progress and logs memory usage of the app with index and pca file loaded
     * @param result result message indication error or success message
     */
    protected void onPostExecute(String result) {

        Utils.logMemory();
        if(mButton != null){
            mButton.setVisibility(View.VISIBLE);
            mButton.setEnabled(true);
        }
        if (!result.startsWith("Error")) {
            Utils.addText(mTextView, result);
        } else {
            Utils.addTextRed(mTextView, result);
        }

    }
}