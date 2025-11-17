package com.example.edgedetection;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicReference;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;
import android.opengl.GLSurfaceView.Renderer;

/**
 * Renders RGBA byte[] frames uploaded from Java (processed by native).
 */
public class CameraRenderer implements Renderer {
    private int textureId = -1;
    private int program = -1;

    private final float[] squareCoords = {
            -1f,  1f, 0f,
            -1f, -1f, 0f,
             1f,  1f, 0f,
             1f, -1f, 0f
    };
    private final float[] texCoords = {
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
    };
    private FloatBuffer vertexBuffer, texBuffer;
    private AtomicReference<byte[]> pendingFrame = new AtomicReference<>();
    private int frameW = 0, frameH = 0;
    private long lastTime = System.nanoTime();
    private int frames = 0;
    private float fps = 0f;
    private byte[] lastFrame;

    public CameraRenderer() {
        vertexBuffer = ByteBuffer.allocateDirect(squareCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(squareCoords).position(0);
        texBuffer = ByteBuffer.allocateDirect(texCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texBuffer.put(texCoords).position(0);
    }

    public void updateFrame(byte[] rgba, int w, int h) {
        pendingFrame.set(rgba);
        frameW = w; frameH = h;
        // Keep a reference to the last frame for capture
        lastFrame = rgba.clone();
    }
    
    /**
     * Captures the current frame as a Bitmap
     * @return Bitmap of the current frame or null if no frame is available
     */
    public android.graphics.Bitmap captureFrame() {
        if (lastFrame == null || frameW <= 0 || frameH <= 0) {
            return null;
        }
        
        try {
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(frameW, frameH, android.graphics.Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(lastFrame));
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        textureId = generateTexture();
        GLES20.glUseProgram(program);
    }

    @Override public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0,0,w,h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        byte[] frame = pendingFrame.getAndSet(null);
        if (frame != null && frameW>0 && frameH>0) {
            ByteBuffer bb = ByteBuffer.allocateDirect(frame.length).order(ByteOrder.nativeOrder());
            bb.put(frame).position(0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,frameW,frameH,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,bb);
        }

        GLES20.glUseProgram(program);

        int posLoc = GLES20.glGetAttribLocation(program, "aPosition");
        int texLoc = GLES20.glGetAttribLocation(program, "aTexCoord");
        int texUniform = GLES20.glGetUniformLocation(program, "uTexture");

        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(texUniform, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);

        // FPS overlay calculation (simple)
        frames++;
        long now = System.nanoTime();
        if (now - lastTime >= 1_000_000_000L) {
            fps = frames * 1e9f / (now - lastTime);
            lastTime = now;
            frames = 0;
        }
        android.util.Log.d("CameraRenderer", "FPS: " + fps);
    }

    private int generateTexture() {
        int[] tid = new int[1];
        GLES20.glGenTextures(1, tid, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tid[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return tid[0];
    }

    private int createProgram(String vs, String fs) {
        int v = loadShader(GLES20.GL_VERTEX_SHADER, vs);
        int f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        return p;
    }

    private int loadShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = aPosition;\n" +
            "  vTexCoord = aTexCoord;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
            "}\n";
}
