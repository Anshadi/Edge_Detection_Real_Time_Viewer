package com.example.edgedetection;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GLSurfaceViewWithRenderer extends GLSurfaceView {
    private final CameraRenderer renderer;
    public GLSurfaceViewWithRenderer(Context ctx) {
        super(ctx);
        setEGLContextClientVersion(2);
        renderer = new CameraRenderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }
    public CameraRenderer getRenderer() { return renderer; }
}
