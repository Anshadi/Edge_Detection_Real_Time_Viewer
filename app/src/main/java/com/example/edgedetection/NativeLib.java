package com.example.edgedetection;

public class NativeLib {
    static {
        System.loadLibrary("native-lib");
    }

    // Processing modes
    public static final int MODE_ORIGINAL = 0;
    public static final int MODE_GRAYSCALE = 1;
    public static final int MODE_BLUR = 2;
    public static final int MODE_EDGE = 3;

    public static native void processFrame(byte[] rgba, int width, int height, int mode);
}
