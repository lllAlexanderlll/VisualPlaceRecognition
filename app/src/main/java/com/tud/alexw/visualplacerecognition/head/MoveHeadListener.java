package com.tud.alexw.visualplacerecognition.head;

public interface MoveHeadListener {
    /**
     * Callback called if a single head movement is done. Reports yaw and pitch values in degree.
     * @param yaw yaw value in degree
     * @param pitch pitch value in degree
     */
    void onHeadMovementDone(int yaw, int pitch);

    /**
     * Callback called if a all head movements are done.
     */
    void onAllHeadMovementsDone();
}
