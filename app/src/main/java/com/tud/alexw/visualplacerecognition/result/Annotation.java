package com.tud.alexw.visualplacerecognition.result;

public class Annotation{
    public int x, y, yaw, pitch;
    public String label;

    public Annotation(int x, int y, int yaw, int pitch, String label) {
        this.x = x;
        this.y = y;
        this.yaw = yaw;
        this.pitch = pitch;
        this.label = label;
    }
}

