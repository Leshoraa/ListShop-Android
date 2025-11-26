package com.leshoraa.listshop.utils;

import android.app.Activity;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.util.List;

public class CameraPreviewManager implements SurfaceHolder.Callback {

    private final Activity activity;
    private final SurfaceView surfaceView;
    private Camera camera;

    public CameraPreviewManager(Activity activity, SurfaceView surfaceView) {
        this.activity = activity;
        this.surfaceView = surfaceView;
        surfaceView.getHolder().addCallback(this);
    }

    public void startCamera() {
        if (surfaceView.getHolder().getSurface() == null) return;
        try {
            openCamera(surfaceView.getHolder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void releaseCamera() {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.release();
            } catch (Exception e) {
                // Ignore errors during release
            }
            camera = null;
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        openCamera(holder);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (camera == null) return; // Guard clause: if camera isn't open, don't touch it

        try {
            camera.stopPreview();
        } catch (Exception e) {
        }

        try {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                Camera.Size optimalSize = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), 4.0 / 3.0);
                if (optimalSize != null) {
                    parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                }
                camera.setParameters(parameters);
            }

            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        releaseCamera();
    }

    private void openCamera(SurfaceHolder holder) {
        try {
            if (camera != null) releaseCamera();

            camera = Camera.open(); // Open default back camera

            if (camera == null) {
                Toast.makeText(activity, "Failed to open camera", Toast.LENGTH_SHORT).show();
                return;
            }

            Camera.Parameters parameters = camera.getParameters();

            if (parameters == null) {
                return;
            }

            Camera.Size optimalSize = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), 4.0 / 3.0);
            if (optimalSize != null) {
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            }

            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            camera.setParameters(parameters);

            setCameraDisplayOrientation(activity, Camera.CameraInfo.CAMERA_FACING_BACK, camera);
            camera.setPreviewDisplay(holder);
            camera.startPreview();

            adjustAspectRatio(4f/3f);

        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera(); // Clean up if opening failed
            Toast.makeText(activity, "Camera Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void adjustAspectRatio(float targetRatio) {
        int parentWidth = surfaceView.getWidth();
        int parentHeight = surfaceView.getHeight();

        if (parentWidth == 0 || parentHeight == 0) return;

        int newWidth, newHeight;

        if ((float) parentWidth / parentHeight > targetRatio) {
            newHeight = parentHeight;
            newWidth = (int) (parentHeight * targetRatio);
        } else {
            newWidth = parentWidth;
            newHeight = (int) (parentWidth / targetRatio);
        }
        ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
        params.width = newWidth;
        params.height = newHeight;
        surfaceView.setLayoutParams(params);
        surfaceView.requestLayout();
    }

    private void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private android.hardware.Camera.Size getOptimalPreviewSize(List<android.hardware.Camera.Size> sizes, double targetRatio) {
        if (sizes == null) return null; // Null check

        final double ASPECT_TOLERANCE = 0.1;
        android.hardware.Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        for (android.hardware.Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) < ASPECT_TOLERANCE) {
                if (Math.abs(size.height - 720) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - 720);
                }
            }
        }
        if (optimalSize == null && !sizes.isEmpty()) {
            optimalSize = sizes.get(0);
        }
        return optimalSize;
    }
}