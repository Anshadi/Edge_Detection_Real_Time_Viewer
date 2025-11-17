package com.example.edgedetection;

import android.content.Context;
import android.util.AttributeSet;
import android.opengl.GLSurfaceView;

public class GLSurfaceViewWithRenderer extends GLSurfaceView {
    private CameraRenderer renderer;

    public GLSurfaceViewWithRenderer(Context ctx) {
        super(ctx);
        init();
    }

    public GLSurfaceViewWithRenderer(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        renderer = new CameraRenderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public CameraRenderer getRenderer() { return renderer; }
}
