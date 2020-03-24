package com.tud.alexw.visualplacerecognition.head;

import android.util.Log;

import com.segway.robot.sdk.locomotion.head.Head;
import com.tud.alexw.visualplacerecognition.Utils;

import java.util.LinkedList;
import java.util.List;

import static com.tud.alexw.visualplacerecognition.Utils.degreeToRad;
import static com.tud.alexw.visualplacerecognition.Utils.isClose;
import static com.tud.alexw.visualplacerecognition.Utils.radToDegree;


public class MoveHead {

    private String TAG = "MoveHead";
    private MoveHeadListener moveHeadListener;
    private List<Integer> yaws, pitches;
    private Head mHead;
    private int counter = 0;

    /**
     * Constructor. Creates new MoveHead object by specifying listener, robot SDK head object and setting intial head movement list by yaws and pitches (same size required).
     * @param head reference to robot SDK head object
     * @param listener reference to listener object
     * @param yaws yaw values in degree. Values between -174 and 174 allowed
     * @param pitches pitch values in degree. Values between -90 and 174 allowed
     */
    public MoveHead(Head head, MoveHeadListener listener, int[] yaws, int[] pitches){
        mHead = head;
        moveHeadListener = listener;
        this.yaws = new LinkedList<>();
        this.pitches = new LinkedList<>();
        addMoves(yaws, pitches);
    }

    /**
     * extent head movement lists with more head movements. Arrays must have same size.
     * @param yaws yaw values in degree. Values between -174 and 174 allowed
     * @param pitches pitch values in degree. Values between -90 and 174 allowed
     */
    public void addMoves(int[] yaws, int[] pitches){
        if(yaws.length == pitches.length){
            for(int i = 0; i < yaws.length; i++){
                this.pitches.add(pitches[i]);
                this.yaws.add(yaws[i]);
            }
        }
    }

    /**
     * resets movement list i.e. points to first movement
     */
    private void reset(){
        counter = 0;
    }

    /**
     * retry last movement i.e. point to previous movement and call next
     */
    public void retry(){
        Log.i(TAG, "Retrying to move head");
        counter--;
        next();
    }

    /**
     * iterates to next head pose, waits until head moved into this position and informs listener with onHeadMovementDone
     * if no next head move position in list reset head pose and look upwards to ease user interaction and informs listener with onAllHeadMovementsDone
     * Automatically resets movement poses list pointer if all head movements done
     */
    public void next(){
        if(counter < yaws.size()){
            moveHead(yaws.get(counter), pitches.get(counter));
            moveHeadListener.onHeadMovementDone(yaws.get(counter), pitches.get(counter));
            counter++;
        }
        else{
            reset();
            mHead.resetOrientation();
            mHead.setWorldPitch(Utils.degreeToRad(45));
            moveHeadListener.onAllHeadMovementsDone();
        }
    }

    /**
     * Moves the head to the given pitch and yaw value (both in degree) and waits until there (blocking).
     * @param yaw_deg yaw value in degree. Values between -174 and 174 allowed
     * @param pitch_deg pitch value in degree. Values between -90 and 174 allowed
     */
    private void moveHead(int yaw_deg, int pitch_deg){
        if(yaw_deg > 144 || yaw_deg < -144 || pitch_deg < 0 || pitch_deg > 174){
            Log.e(TAG, String.format("Yaw: %d not in [-144, 144] or pitch: %d not in [0, 174]", yaw_deg, pitch_deg));
            return;
        }

        mHead.setHeadJointYaw(degreeToRad(yaw_deg));
        mHead.setWorldPitch(degreeToRad(pitch_deg));
        Log.i(TAG,String.format("Current motor pitch and yaw values: %f, %f", mHead.getHeadJointYaw().getAngle(), mHead.getWorldPitch().getAngle()));
        Log.i(TAG,String.format("Set motor pitch and yaw values: %f, %f (%d, %d)", degreeToRad(yaw_deg), degreeToRad(pitch_deg), yaw_deg, pitch_deg));
        while (
                !(isClose(radToDegree(mHead.getHeadJointYaw().getAngle()), yaw_deg) &&
                        isClose(radToDegree(mHead.getWorldPitch().getAngle()), pitch_deg))
        ) {
            Log.v(TAG, String.format("Waiting for Head to turn from (%d, %d) to (%d, %d)", radToDegree(mHead.getHeadJointYaw().getAngle()), radToDegree(mHead.getWorldPitch().getAngle()), yaw_deg, pitch_deg));
        }
        Log.i(TAG, String.format("Move to (%d, %d) done", yaw_deg, pitch_deg));
    }
}
