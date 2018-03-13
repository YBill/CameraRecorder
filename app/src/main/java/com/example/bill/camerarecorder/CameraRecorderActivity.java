package com.example.bill.camerarecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class CameraRecorderActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1000;
    private static final int MY_PERMISSIONS_REQUEST_RECORD = 1001;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private int cameraDirection = 0; // 摄像头方向
    private CamcorderProfile mProfile;
    private int preViewWidth, preViewHeight;
    private MediaRecorder mMediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_recorder);
        cameraDirection = Camera.CameraInfo.CAMERA_FACING_BACK;
        _initView();
    }

    private void _initView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.addCallback(mCallback);
    }

    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (ContextCompat.checkSelfPermission(CameraRecorderActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CameraRecorderActivity.this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            } else {
                _initCamera();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            _stopCamera();
        }
    };

    private void _stopCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void _initCamera() {
        if (mCamera != null) {
            _stopCamera();
        }
        try {
            mCamera = Camera.open(cameraDirection);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            _setCameraParams();
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            _stopCamera();
        }

    }

    private void _setCameraParams() {
        if (mCamera != null) {
            if (cameraDirection == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera.setDisplayOrientation(270);
            } else {
                mCamera.setDisplayOrientation(90);
            }
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
            Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes, mSupportedPreviewSizes, mSurfaceView.getWidth(), mSurfaceView.getHeight());
            preViewWidth = optimalSize.width;
            preViewHeight = optimalSize.height;
            Log.e("Bill", preViewWidth + "|" + preViewHeight);
            mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mProfile.videoFrameWidth = optimalSize.width;
            mProfile.videoFrameHeight = optimalSize.height;
            mProfile.videoBitRate = 2 * optimalSize.width * optimalSize.height;

            parameters.setPreviewSize(preViewWidth, preViewHeight);
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            if (parameters.isVideoStabilizationSupported()) {
                parameters.setVideoStabilization(true);
            }
            mCamera.setParameters(parameters);
        }
    }

    private void _startRecord() {
        try {
            mCamera.unlock();
            _setConfigRecord();
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            _stopRecord();
        }
    }

    private void _setConfigRecord() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setOnErrorListener(onErrorListener);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        Log.e("Bill", "setOutputFormat:" + mProfile.fileFormat);
        Log.e("Bill", "setVideoFrameRate:" + mProfile.videoFrameRate);
        Log.e("Bill", "setVideoSize:" + mProfile.videoFrameWidth + "|" + mProfile.videoFrameHeight);
        Log.e("Bill", "setVideoEncodingBitRate:" + mProfile.videoBitRate);
        Log.e("Bill", "setVideoEncoder:" + mProfile.videoCodec);
        Log.e("Bill", "profile.quality:" + mProfile.quality);
        Log.e("Bill", "setAudioEncodingBitRate:" + mProfile.audioBitRate);
        Log.e("Bill", "setAudioChannels:" + mProfile.audioChannels);
        Log.e("Bill", "setAudioSamplingRate:" + mProfile.audioSampleRate);
        Log.e("Bill", "setAudioEncoder:" + mProfile.audioCodec);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setProfile(mProfile);

        mMediaRecorder.setOrientationHint(90);
        mMediaRecorder.setOutputFile("/sdcard/Video/test.mp4");
    }

    private void _stopRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            if (mCamera != null)
                mCamera.lock();
        }
    }

    private MediaRecorder.OnErrorListener onErrorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mr, int what, int extra) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _initCamera();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_RECORD) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _startRecord();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void handleSwitch(View view) {
        if (cameraDirection == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            cameraDirection = Camera.CameraInfo.CAMERA_FACING_BACK;
        } else {
            cameraDirection = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        _initCamera();
    }

    public void handleRecord(View view) {
        if (ContextCompat.checkSelfPermission(CameraRecorderActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CameraRecorderActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD);
        } else {
            _startRecord();
        }
    }

    public void handleStop(View view) {
        _stopRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _stopRecord();
        _stopCamera();
    }
}
