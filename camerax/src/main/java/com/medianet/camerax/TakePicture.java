package com.medianet.camerax;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraIdFilter;
import androidx.camera.core.CameraIdFilterSet;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.constraintlayout.solver.widgets.Analyzer;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TakePicture extends AppCompatActivity implements View.OnClickListener {


    private final int REQUEST_CODE_PERMISSIONS = 10;
    CameraX.LensFacing switchFrontBack= CameraX.LensFacing.FRONT;
    private String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextureView viewFinder;
    private boolean openTorch=false;
    private String filePath="";
    private Preview preview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);
        viewFinder = findViewById(R.id.texture);
        findViewById(R.id.action_turn_camera).setOnClickListener(this);
        findViewById(R.id.action_flash_on_off).setOnClickListener(this);

        if (getIntent().hasExtra("absolutePath")){
            filePath=getIntent().getStringExtra("absolutePath");
        }
        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post(this::startCamera);
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener((view, i, i1, i2, i3, i4, i5, i6, i7) -> updateTransform());
    }

    @SuppressLint("RestrictedApi")
    private void startCamera() {
        CameraX.unbindAll();
        try {
            CameraX.getCameraWithLensFacing(switchFrontBack);
        } catch (Exception e) {
            e.printStackTrace();
        }
        DisplayMetrics metrics = new DisplayMetrics();
      //  getWindowManager().getDefaultDisplay().getMetrics(metrics);
        viewFinder.getDisplay().getMetrics(metrics);
        Size screenSize =new  Size(metrics.widthPixels, metrics.heightPixels);
        // Create configuration object for the viewfinder use case
        PreviewConfig previewConfig = new PreviewConfig.Builder().
                setLensFacing(switchFrontBack)
               // .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetResolution(screenSize)
                .build();


        // Build the viewfinder use case
        preview = new Preview(previewConfig);



        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(@NonNull Preview.PreviewOutput output) {
                // To update the SurfaceTexture, we have to remove it and re-add it
                ViewGroup parent = (ViewGroup) viewFinder.getParent();
                parent.removeView(viewFinder);
                parent.addView(viewFinder, 0);

                viewFinder.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });

        // Create configuration object for the image capture use case
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY).setLensFacing(switchFrontBack).build();

        // Build the image capture use case and attach button click listener
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfig);
        findViewById(R.id.action_take_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File file ;
                if (filePath.length()>0){
                    file=new File(filePath);
                }else{
                    file = new File(getExternalMediaDirs()[0], System.currentTimeMillis()+".jpg");

                }

                imageCapture.takePicture(file, executor,
                        new ImageCapture.OnImageSavedListener() {

                            @Override
                            public void onImageSaved(@NonNull File file) {
                                String msg = "Photo capture succeeded: ${file.absolutePath}";
                                Log.e("CameraXApp", msg);

                                viewFinder.post(new Runnable() {
                                    @Override
                                    public void run() {

                                        Intent returnIntent = new Intent();
                                        returnIntent.putExtra("image_absolute_path",file.getAbsolutePath());
                                        setResult(4413,returnIntent);
                                        finish();
                                    }
                                });
                            }

                            @Override
                            public void onError(@NonNull ImageCapture.ImageCaptureError imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                String msg = "Photo capture failed: $message";
                                Log.e("CameraXApp", msg);
                                viewFinder.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent returnIntent = new Intent();
                                        returnIntent.putExtra("image_absolute_path","");
                                        setResult(4413,returnIntent);
                                        finish();
                                    }
                                });
                            }
                        });
            }
        });

        // Setup image analysis pipeline that computes average pixel luminance in real time
        ImageAnalysisConfig analyzerConfig =new ImageAnalysisConfig.Builder()
           .setLensFacing(switchFrontBack)
            // In our analysis, we care more about the latest image than analyzing *every* image
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(viewFinder.getDisplay().getRotation())
        .build();



/*
        ImageAnalysisConfig config =
                new ImageAnalysisConfig.Builder()
                        .setLensFacing(switchFrontBack)
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                        .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(config);

        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(ImageProxy image, int rotationDegrees) {
                Log.e("aaaaaaaaaa",rotationDegrees+"eeee");
            }
        });*/




        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, imageCapture);

    }

    private ImageAnalysis setImageAnalysis() {

     return null;

    }


    private void updateTransform() {
        Matrix matrix = new Matrix();

        // Compute the center of the view finder
        float centerX = viewFinder.getWidth() / 2f;
        float centerY = viewFinder.getHeight() / 2f;

        // Correct preview output to account for display rotation
        int rotationDegrees = 0;
        switch (viewFinder.getDisplay().getRotation()) {
            case Surface.ROTATION_0:
                rotationDegrees = 0;
                break;
            case Surface.ROTATION_90:
                rotationDegrees = 90;
                break;
            case Surface.ROTATION_180:
                rotationDegrees = 180;
                break;
            case Surface.ROTATION_270:
                rotationDegrees = 270;
                break;
                default: updateTransform();
        }

        matrix.postRotate(-(float) rotationDegrees, centerX, centerY);

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix);
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post(this::startCamera);
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private boolean allPermissionsGranted() {
        boolean test = false;
        for (String permission : REQUIRED_PERMISSIONS) {
            test = ContextCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_GRANTED;
        }
        return test;
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.action_turn_camera){
            if (switchFrontBack == CameraX.LensFacing.FRONT) {
                switchFrontBack = CameraX.LensFacing.BACK;
            } else {
                switchFrontBack = CameraX.LensFacing.FRONT;
            }
            if (allPermissionsGranted()) {
                viewFinder.post(this::startCamera);
            } else {
                ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
            viewFinder.addOnLayoutChangeListener((e, i, i1, i2, i3, i4, i5, i6, i7) -> updateTransform());
        }else if(view.getId()==R.id.action_flash_on_off){
            openTorch=!openTorch;
            preview.enableTorch(openTorch);
            if (openTorch) ((FloatingActionButton)view).setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_light), PorterDuff.Mode.SRC_IN);
            else  ((FloatingActionButton)view).setColorFilter(ContextCompat.getColor(this, android.R.color.white), android.graphics.PorterDuff.Mode.SRC_IN);


        }
    }


    public interface LumaListener {
        void listener(Double d);
    }

}