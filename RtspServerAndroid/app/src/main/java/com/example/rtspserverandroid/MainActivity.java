package com.example.rtspserverandroid;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends Activity implements SurfaceHolder.Callback,PreviewCallback{

    private SurfaceView surfaceview;

    private SurfaceHolder surfaceHolder;

    private Camera camera;

    private Parameters parameters;

    int width = 1280;

    int height = 720;

    int framerate = 30;

    int biterate = 8500*1000;

    private static int yuvqueuesize = 10;

    //Video buffer queue to decode, static member!
    public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(yuvqueuesize);

    private AvcEncoder avcCodec;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // if camera permission is not given it will ask for it on device
        getPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceview = (SurfaceView)findViewById(R.id.surfaceview);
        surfaceHolder = surfaceview.getHolder();
        surfaceHolder.addCallback(this);
    }

    public void getPermission(){
        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        int MY_PERMISSIONS_REQUEST_WRITE_STORAGE=0;
        int MY_PERMISSIONS_REQUEST_INTERNET=0;

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

    }
    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = getBackCamera();
        startcamera(camera);
        //Create an avencoder object
        avcCodec = new AvcEncoder(width,height,framerate,biterate);
        //Start encoding thread
        avcCodec.StartEncoderThread();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != camera) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
            avcCodec.StopThread();
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
        //Saves the current frame image in the queue
        putYUVData(data,data.length);
    }

    public void putYUVData(byte[] buffer, int length) {
        if (YUVQueue.size() >= 10) {
            YUVQueue.poll();
        }
        YUVQueue.add(buffer);
    }


    private void startcamera(Camera mCamera){
        if(mCamera != null){
            try {
                mCamera.setPreviewCallback(this);
                mCamera.setDisplayOrientation(90);
                if(parameters == null){
                    parameters = mCamera.getParameters();
                }
                //Get the default camera configuration
                parameters = mCamera.getParameters();
                //Format Preview
                parameters.setPreviewFormat(ImageFormat.NV21);
                //Set preview image resolution
                parameters.setPreviewSize(width, height);
                //Configure camera parameters
                mCamera.setParameters(parameters);
                //Pass the fully initialized surfaceholder into setpreviewdisplay (surfaceholder)
                //Without a surface, the camera will not turn on preview preview
                mCamera.setPreviewDisplay(surfaceHolder);
                //Call startpreview() to update the surface of preview. You must start preview before taking photos
                mCamera.startPreview();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Camera getBackCamera() {
        Camera c = null;
        try {
            //Get an instance of camera
            c = Camera.open(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }


}