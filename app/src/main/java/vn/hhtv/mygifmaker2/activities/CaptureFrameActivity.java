package vn.hhtv.mygifmaker2.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import vn.hhtv.mygifmaker2.R;
import vn.hhtv.mygifmaker2.utils.GifUtil;
import vn.hhtv.mygifmaker2.utils.LogUtil;

/**
 * Created by Nienlm on 6/16/16 12:03 PM.
 */
public class CaptureFrameActivity extends Activity {

    private static final String TAG = "CaptureFrameActivity";
    private boolean isRecoding = false;
    private int mFrameIndex = 0, mRotatedFrameIndex = 0;
    private Timer t;
    private int mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private String currentFileFolder = "";
    private Camera mCamera;
    private ExecutorService processingExecutor = Executors.newSingleThreadExecutor();
    private MyCameraSurfaceView mCameraSurfaceView;
    private long previewStart = 0, previewStop = 0;
    @BindView(R.id.frame)
    FrameLayout mFrame;
    @BindView(R.id.duration_text)
    TextView mDurationText;
    @BindView(R.id.capture_btn)
    ImageView mCaptureBtn;
    @BindView(R.id.changecamera_btn)
    ImageView mChangeCameraBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_capture_frame);
        ButterKnife.bind(this);
        this.mCamera = getCameraInstance();
        if(mCamera == null){
            Toast.makeText(this,
                    "Fail to get Camera",
                    Toast.LENGTH_LONG).show();
        }

        if (Camera.getNumberOfCameras() == 1) this.mChangeCameraBtn.setVisibility(View.GONE);
        this.mChangeCameraBtn.setTag(Camera.CameraInfo.CAMERA_FACING_BACK);
        this.mCameraSurfaceView = new MyCameraSurfaceView(this,mCamera);
        mFrame.addView(mCameraSurfaceView);
        this.currentFileFolder = Calendar.getInstance().getTimeInMillis() + "";
        this.t = new Timer();
    }

    @OnClick({R.id.capture_btn, R.id.changecamera_btn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.capture_btn:
                onCaptureBtnClicked.onClick(view);
                break;
            case R.id.changecamera_btn:
                onChangeCameraBtnClicked.onClick(view);
                break;
        }
    }

    private Camera getCameraInstance(){
        // TODO Auto-generated method stub
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            c.setDisplayOrientation(90);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera(){
        try{
            mCamera.release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    View.OnClickListener onChangeCameraBtnClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ((int)v.getTag() == Camera.CameraInfo.CAMERA_FACING_BACK){
                v.setTag(Camera.CameraInfo.CAMERA_FACING_FRONT);
                Picasso.with(CaptureFrameActivity.this).load(R.drawable.ic_camera_front).into(mChangeCameraBtn);
            }else{
                v.setTag(Camera.CameraInfo.CAMERA_FACING_BACK);
                Picasso.with(CaptureFrameActivity.this).load(R.drawable.ic_camera_rear).into(mChangeCameraBtn);
            }
            changeCamera((int)v.getTag());
        }
    };

    private void changeCamera(int cameraId) {
        this.mCurrentCameraId = cameraId;
        this.mCameraSurfaceView.removeCallback();
        this.mCameraSurfaceView.surfaceDestroyed(mCameraSurfaceView.getHolder());
        this.mFrame.removeAllViews();
        this.mCameraSurfaceView = null;
        this.mCamera.stopPreview();
        this.mCamera.release();
        this.mCamera = Camera.open(cameraId);
        this.mCameraSurfaceView = new MyCameraSurfaceView(this,mCamera);
        mFrame.addView(mCameraSurfaceView);
    }


    View.OnClickListener onCaptureBtnClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isRecoding){

                isRecoding = false;
                mCameraSurfaceView.removeCallback();
                previewStop = Calendar.getInstance().getTimeInMillis();
                Picasso.with(CaptureFrameActivity.this).load(R.drawable.ic_camera)
                        .into(mCaptureBtn);
                stopUpdateTextTime();
                //showDialog();
                //releaseCamera();
                Intent i = new Intent(CaptureFrameActivity.this, GifPreviewActivity.class);
                i.putExtra("count",mFrameIndex);
                i.putExtra("cameraid",mCurrentCameraId);
                i.putExtra("currentfolder",currentFileFolder);
                i.putExtra("time",previewStop - previewStart);
                startActivity(i);
                //hideDialog();
                releaseCamera();
                finish();

            }else{
                mChangeCameraBtn.setVisibility(View.GONE);
                isRecoding = true;
                previewStart = Calendar.getInstance().getTimeInMillis();
                Picasso.with(CaptureFrameActivity.this).load(R.drawable.ic_action_record)
                        .into(mCaptureBtn);
                updateTextTime();
            }
        }
    };


    private void updateTextTime(){
        try{
            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int t = Integer.parseInt(mDurationText.getText().toString());
                            mDurationText.setText("" + (t+1));
                        }
                    });
                }
            },1000, 1000);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void stopUpdateTextTime(){
        try{
            t.cancel();
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    public Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, final Camera camera) {
            if (!isRecoding) return;
            processingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    File f = GifUtil.getOutputMediaFile(mFrameIndex, currentFileFolder);
                    processSavingFrame(data, camera, f);
                }
            });
            camera.addCallbackBuffer(data);
        }
    };




    private void processSavingFrame(byte[] data, Camera arg1, File file){
        FileOutputStream outStream = null;
        try {
            int w = arg1.getParameters().getPreviewSize().width;
            int h = arg1.getParameters().getPreviewSize().height;
            YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21,w,h,null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0,0,w,h), 25, baos);
            outStream = new FileOutputStream(file);
            outStream.write(baos.toByteArray());
            outStream.close();
            /*new ProcessRotatingFrame(baos.toByteArray(), mFrameIndex).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR, ""
            );*/
            mFrameIndex++;


            LogUtil.wth("onPreviewFrame - wrote bytes: " + data.length + " - " + mFrameIndex + " - "
                    + arg1.getParameters().getPreviewSize().width + " - "
                    + arg1.getParameters().getPreviewSize().height);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public class ProcessRotatingFrame extends AsyncTask<String, String, Boolean>{
        byte[] data;
        int index;
        BitmapFactory.Options option;
        FileOutputStream out = null;
        public ProcessRotatingFrame(byte[] data, int index) {
            this.data = data;
            this.index = index;
            this.option = new BitmapFactory.Options();
            this.option.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }
        @Override
        protected Boolean doInBackground(String... params) {
            Bitmap b = GifUtil.decodeSampledBitmapFromResource(data, mFrameWidth,mFrameHeight);
            Bitmap b1 = GifUtil.rotateBitmap(b,90);
            try {
                out = new FileOutputStream(GifUtil.getOutputMediaFile(index, currentFileFolder));
                b1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }finally {
                Log.d(TAG, "run: finish rotate: " + index);
                mRotatedFrameIndex++;
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return (mRotatedFrameIndex >= mFrameIndex && !isRecoding);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean){

            }
        }
    }

    MaterialDialog mDialog;
    private void showDialog(){
        mDialog = new MaterialDialog.Builder(this)
                .title("Please wait")
                .content("Processing your content...")
                .cancelable(false)
                .progress(true, 1).build();
        mDialog.show();
    }

    private void hideDialog(){
        if (mDialog.isShowing())
            mDialog.hide();
    }





    private int mFrameWidth, mFrameHeight;
    public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public MyCameraSurfaceView(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void removeCallback(){
            mCamera.setPreviewCallback(null);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) return;
            try {
                mCamera.stopPreview();
            }catch (Exception e){
                e.printStackTrace();
            }

            try {
                mCamera.setPreviewDisplay(holder);
                settingCamera();
                mCamera.startPreview();
            }catch (Exception e){
                e.printStackTrace();
            }
        }


        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
                settingCamera();
                mCamera.startPreview();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

        void settingCamera(){
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> allSizes = params.getSupportedPictureSizes();
            Camera.Size size = allSizes.get(0); // get top size
            Camera.Size size2 = allSizes.get(0);
            for (int i = 0; i < allSizes.size(); i++) {
                LogUtil.wth("settingCamera: " + allSizes.get(i).width + " - " + allSizes.get(i).height);
                if (allSizes.get(i).width <= size.width)
                    size = allSizes.get(i);
                if (allSizes.get(i).width <= size2.width)
                    size2 = allSizes.get(i);
            }
            mFrameWidth = size.width;
            mFrameHeight = size.height;
            params.setPictureSize(size.width, size.height);
            params.setPreviewSize(size2.width, size2.height);
            params.setRotation(90);
            //params.setPreviewFrameRate(60);
            //LogUtil.wth(">> camera: " + mCamera.getNumberOfCameras());
            //mCamera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewCallback(previewCallback);
        }
    }
}
