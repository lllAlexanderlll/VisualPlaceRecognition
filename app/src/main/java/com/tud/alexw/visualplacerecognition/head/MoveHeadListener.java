package com.tud.alexw.visualplacerecognition.head;

public interface MoveHeadListener {
    void onHeadMovementDone(int yaw, int pitch);

    void onAllHeadMovementsDone();
}
