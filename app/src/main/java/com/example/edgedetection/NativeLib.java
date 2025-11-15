package com.example.edgedetection;

public class NativeLib {
    static {
        System.loadLibrary("native-lib");
    }
    public static native void processFrame(byte[] rgba, int width, int height);
}
