package com.example.edgedetection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.hardware.camera2.*;
import android.view.Surface;
import android.os.Handler;
import android.os.HandlerThread;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 1001;

    private GLSurfaceViewWithRenderer glView;
    private CameraRenderer renderer;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private ImageReader imageReader;

    static {
        System.loadLibrary("native-lib");
    }

    private final int PREVIEW_W = 640;
    private final int PREVIEW_H = 480;
    private int rotationDegrees = 0;
    private volatile boolean savedSample = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glView = new GLSurfaceViewWithRenderer(this);
        FrameLayout root = findViewById(android.R.id.content);
        root.addView(glView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        renderer = glView.getRenderer();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            setupAndStartCamera();
        }
    }

    private void setupAndStartCamera() {
        startBackgroundThread();
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            
            // Get camera characteristics for orientation
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            
            // Calculate device rotation in degrees
            int deviceRotation = 0;
            switch (rotation) {
                case Surface.ROTATION_0: deviceRotation = 0; break;
                case Surface.ROTATION_90: deviceRotation = 90; break;
                case Surface.ROTATION_180: deviceRotation = 180; break;
                case Surface.ROTATION_270: deviceRotation = 270; break;
            }
            
            // Calculate final rotation
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                rotationDegrees = (sensorOrientation + deviceRotation) % 360;
                rotationDegrees = (360 - rotationDegrees) % 360;  // Compensate for mirror
            } else {  // Back-facing camera
                rotationDegrees = (sensorOrientation - deviceRotation + 360) % 360;
            }
            
            // Swap width and height if needed (90 or 270 degrees rotation)
            boolean swapDimensions = rotationDegrees == 90 || rotationDegrees == 270;
            int previewWidth = swapDimensions ? PREVIEW_H : PREVIEW_W;
            int previewHeight = swapDimensions ? PREVIEW_W : PREVIEW_H;
            
            imageReader = ImageReader.newInstance(previewWidth, previewHeight, android.graphics.ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(this::onImageAvailable, backgroundHandler);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        android.view.Surface surface = imageReader.getSurface();
                        final CaptureRequest.Builder req = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        req.addTarget(surface);
                        cameraDevice.createCaptureSession(java.util.Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                            @Override public void onConfigured(@NonNull CameraCaptureSession session) {
                                captureSession = session;
                                try {
                                    req.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                    session.setRepeatingRequest(req.build(), null, backgroundHandler);
                                } catch (CameraAccessException e) { e.printStackTrace(); }
                            }
                            @Override public void onConfigureFailed(@NonNull CameraCaptureSession session) { }
                        }, backgroundHandler);
                    } catch (CameraAccessException e) { e.printStackTrace(); }
                }
                @Override public void onDisconnected(@NonNull CameraDevice camera) { camera.close(); cameraDevice=null; }
                @Override public void onError(@NonNull CameraDevice camera, int error) { camera.close(); cameraDevice=null; }
            }, backgroundHandler);
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private void onImageAvailable(ImageReader reader) {
        android.media.Image image = null;
        try {
            image = reader.acquireLatestImage();
            if (image == null) return;
            byte[] rgba = YuvToRgbaConverter.yuv420ToRgba(image, PREVIEW_W, PREVIEW_H);
            
            // Rotate the image data if needed
            if (rotationDegrees != 0) {
                rgba = rotateImageData(rgba, PREVIEW_W, PREVIEW_H, rotationDegrees);
            }
            
            // Use the correct dimensions based on rotation
            int outputWidth = (rotationDegrees == 90 || rotationDegrees == 270) ? PREVIEW_H : PREVIEW_W;
            int outputHeight = (rotationDegrees == 90 || rotationDegrees == 270) ? PREVIEW_W : PREVIEW_H;
            
            NativeLib.processFrame(rgba, outputWidth, outputHeight);
            renderer.updateFrame(rgba, outputWidth, outputHeight);

            if (savedSample) return;
            savedSample = true;
            try {
                java.io.File out = new java.io.File(getExternalFilesDir(null), "sample_processed.raw");
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(rgba);
                fos.close();
            } catch (Exception ex) { ex.printStackTrace(); }

        } catch (Exception ex) { ex.printStackTrace(); }
        finally { if (image != null) image.close(); }
    }

    @Override 
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) {
                setupAndStartCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startBackgroundThread() {
        if (backgroundThread!=null) return;
        backgroundThread = new HandlerThread("CameraBg");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread!=null) {
            backgroundThread.quitSafely();
            try { backgroundThread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
            backgroundThread = null; backgroundHandler = null;
        }
    }

    private byte[] rotateImageData(byte[] input, int width, int height, int rotation) {
        if (rotation == 0) return input.clone();
        
        byte[] output = new byte[input.length];
        int pixelSize = 4; // RGBA
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceIndex = (y * width + x) * pixelSize;
                int destIndex;
                
                switch (rotation) {
                    case 90: // 90 degrees clockwise
                        destIndex = (x * height + (height - 1 - y)) * pixelSize;
                        break;
                    case 180: // 180 degrees
                        destIndex = ((height - 1 - y) * width + (width - 1 - x)) * pixelSize;
                        break;
                    case 270: // 270 degrees clockwise (or 90 counter-clockwise)
                        destIndex = ((width - 1 - x) * height + y) * pixelSize;
                        break;
                    default: // No rotation
                        return input.clone();
                }
                
                // Copy RGBA pixel
                System.arraycopy(input, sourceIndex, output, destIndex, pixelSize);
            }
        }
        return output;
    }
    
    @Override protected void onPause() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        stopBackgroundThread();
        super.onPause();
    }
}
