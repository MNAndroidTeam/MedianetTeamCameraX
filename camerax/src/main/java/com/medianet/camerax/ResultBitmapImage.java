package com.medianet.camerax;

import android.graphics.Bitmap;

public class ResultBitmapImage {
    private static Bitmap bitmap;

    public static Bitmap getBitmap() {
        return bitmap;
    }

    public static void setBitmap(Bitmap bitmap) {
        ResultBitmapImage.bitmap = bitmap;
    }
}
