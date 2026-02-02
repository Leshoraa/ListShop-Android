package com.leshoraa.listshop.utils;

import android.app.Activity;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.util.List;

public class CameraPreviewManager implements SurfaceHolder.Callback {

    private final Activity activity;
    private final SurfaceView surfaceView;
    private Camera camera;
    private boolean isPreviewRunning = false;

    public CameraPreviewManager(Activity activity, SurfaceView surfaceView) {
        this.activity = activity;
        this.surfaceView = surfaceView;
        surfaceView.getHolder().addCallback(this);
    }

    public void startCamera() {
        SurfaceHolder holder = surfaceView.getHolder();
        if (holder.getSurface() != null && holder.getSurface().isValid()) {
            openCamera(holder);
            setupCamera(holder);
        }
    }

    public void releaseCamera() {
        if (camera != null) {
            try {
                if (isPreviewRunning) {
                    camera.stopPreview();
                }
                camera.release();
            } catch (Exception e) {
                // Ignore errors during release
            }
            camera = null;
            isPreviewRunning = false;
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        openCamera(holder);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        setupCamera(holder);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        releaseCamera();
    }

    private void openCamera(SurfaceHolder holder) {
        if (camera != null) return;

        try {
            camera = Camera.open(); // Open default back camera

            if (camera == null) {
                Toast.makeText(activity, "Failed to open camera", Toast.LENGTH_SHORT).show();
                return;
            }

            setCameraDisplayOrientation(activity, Camera.CameraInfo.CAMERA_FACING_BACK, camera);
            camera.setPreviewDisplay(holder);

            // Adjust aspect ratio after a short delay to ensure layout is ready
            surfaceView.post(() -> adjustAspectRatio(4f / 3f));

        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
            Toast.makeText(activity, "Camera Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCamera(SurfaceHolder holder) {
        if (camera == null) return;

        try {
            if (isPreviewRunning) {
                camera.stopPreview();
                isPreviewRunning = false;
            }

            Camera.Parameters parameters = null;
            try {
                parameters = camera.getParameters();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (parameters != null) {
                List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
                Camera.Size optimalSize = getOptimalPreviewSize(supportedSizes, 4.0 / 3.0);
                if (optimalSize != null) {
                    parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                }

                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }

                // Call setParameters only if the camera is in a stable state
                camera.setParameters(parameters);
            }

            camera.setPreviewDisplay(holder);
            camera.startPreview();
            isPreviewRunning = true;

        } catch (Exception e) {
            e.printStackTrace();
            // Some vendor-specific hooks (like Xiaomi) might throw NPE internally.
            // We catch it here to prevent app crash, though the camera might not work.
        }
    }

    private void adjustAspectRatio(float targetRatio) {
        ViewGroup parent = (ViewGroup) surfaceView.getParent();
        if (parent == null) return;

        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();

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
        if (params.width != newWidth || params.height != newHeight) {
            params.width = newWidth;
            params.height = newHeight;
            surfaceView.setLayoutParams(params);
            surfaceView.requestLayout();
        }
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
        if (sizes == null) return null;

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
