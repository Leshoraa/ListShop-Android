package com.leshoraa.listshop.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static Bitmap resizeBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (image == null) return null;
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return image;
        }
        float ratio = Math.min((float) maxWidth / originalWidth, (float) maxHeight / originalHeight);
        int newWidth = Math.round(originalWidth * ratio);
        int newHeight = Math.round(originalHeight * ratio);
        if (newWidth <= 0 || newHeight <= 0) {
            return image;
        }
        return Bitmap.createScaledBitmap(image, newWidth, newHeight, true);
    }

    public static String bitmapToBase64(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
    }

    public static String saveImageToInternalStorage(Context context, Bitmap bitmap, String filenameId) {
        String filename = filenameId + ".jpg";
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            return filename;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}