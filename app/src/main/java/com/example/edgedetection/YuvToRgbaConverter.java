package com.example.edgedetection;

import android.media.Image;
import java.nio.ByteBuffer;

public class YuvToRgbaConverter {
    public static byte[] yuv420ToRgba(Image image, int width, int height) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuf = planes[0].getBuffer();
        ByteBuffer uBuf = planes[1].getBuffer();
        ByteBuffer vBuf = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        byte[] rgba = new byte[width * height * 4];
        byte[] row = new byte[yRowStride];

        for (int j = 0; j < height; j++) {
            yBuf.position(j * yRowStride);
            yBuf.get(row, 0, Math.min(yRowStride, width));
            for (int i = 0; i < width; i++) {
                int y = row[i] & 0xff;
                int uvIndex = (j / 2) * uvRowStride + (i / 2) * uvPixelStride;
                int u = uBuf.get(uvIndex) & 0xff;
                int v = vBuf.get(uvIndex) & 0xff;

                int c = y - 16;
                int d = u - 128;
                int e = v - 128;

                int r = clamp((298 * c + 409 * e + 128) >> 8);
                int g = clamp((298 * c - 100 * d - 208 * e + 128) >> 8);
                int b = clamp((298 * c + 516 * d + 128) >> 8);

                int base = (j * width + i) * 4;
                rgba[base] = (byte) r;
                rgba[base + 1] = (byte) g;
                rgba[base + 2] = (byte) b;
                rgba[base + 3] = (byte) 0xFF;
            }
        }
        return rgba;
    }
    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }
}
