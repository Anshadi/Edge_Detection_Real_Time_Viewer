#include <jni.h>
#include <cstdint>
#include <cmath>

/*
 Simple native processing with modes:
 0 = original (no-op), 1 = grayscale, 2 = blur, 3 = edge (Sobel + threshold).
 In-place on RGBA bytes.
*/

extern "C"
JNIEXPORT void JNICALL
Java_com_example_edgedetection_NativeLib_processFrame(JNIEnv *env, jclass /*clazz*/, jbyteArray arr,
                                                      jint width, jint height, jint mode) {
    jboolean isCopy;
    jbyte *data = env->GetByteArrayElements(arr, &isCopy);
    if (!data) return;

    int w = width, h = height;
    int len = w * h;

    if (mode == 0) { // original passthrough
        env->ReleaseByteArrayElements(arr, data, 0);
        return;
    }

    // Build grayscale
    float *gray = new float[len]();
    for (int i = 0; i < len; i++) {
        int base = i*4;
        uint8_t r = (uint8_t)data[base];
        uint8_t g = (uint8_t)data[base+1];
        uint8_t b = (uint8_t)data[base+2];
        gray[i] = 0.299f * r + 0.587f * g + 0.114f * b;
    }

    if (mode == 1) { // grayscale
        for (int i = 0; i < len; i++) {
            int base = i*4;
            uint8_t v = (uint8_t) (gray[i] < 0 ? 0 : (gray[i] > 255 ? 255 : gray[i]));
            data[base] = v; data[base+1] = v; data[base+2] = v; data[base+3] = (jbyte)255;
        }
        delete[] gray;
        env->ReleaseByteArrayElements(arr, data, 0);
        return;
    }

    // Prepare a temp buffer for blur (also used by edge)
    float *blur = new float[len]();

    // 3x3 box blur (skip borders)
    for (int y = 1; y < h-1; y++) {
        for (int x = 1; x < w-1; x++) {
            float s=0.f;
            for (int ky=-1; ky<=1; ky++)
                for (int kx=-1; kx<=1; kx++)
                    s += gray[(y+ky)*w + (x+kx)];
            blur[y*w + x] = s/9.f;
        }
    }

    if (mode == 2) { // blur output
        for (int i = 0; i < len; i++) {
            int base = i*4;
            uint8_t v = (uint8_t) (blur[i] < 0 ? 0 : (blur[i] > 255 ? 255 : blur[i]));
            data[base] = v; data[base+1] = v; data[base+2] = v; data[base+3] = (jbyte)255;
        }
        delete[] gray; delete[] blur;
        env->ReleaseByteArrayElements(arr, data, 0);
        return;
    }

    // mode 3: edges (black background, white edges)
    int gxK[9] = {-1,0,1,-2,0,2,-1,0,1};
    int gyK[9] = {-1,-2,-1,0,0,0,1,2,1};
    const float thresh = 50.f;

    for (int y=1; y<h-1; y++) {
        for (int x=1; x<w-1; x++) {
            float gx=0.f, gy=0.f; int idx=0;
            for (int ky=-1; ky<=1; ky++) {
                for (int kx=-1; kx<=1; kx++) {
                    float v = blur[(y+ky)*w + (x+kx)];
                    gx += gxK[idx]*v; gy += gyK[idx]*v; idx++;
                }
            }
            float mag = sqrtf(gx*gx + gy*gy);
            uint8_t e = (mag > thresh) ? 255 : 0; // white edges, black background
            int base = (y*w + x)*4;
            data[base] = e; data[base+1] = e; data[base+2] = e; data[base+3] = (jbyte)255;
        }
    }

    delete[] gray; delete[] blur;
    env->ReleaseByteArrayElements(arr, data, 0);
}
