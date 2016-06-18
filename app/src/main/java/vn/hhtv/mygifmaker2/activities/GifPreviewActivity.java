package vn.hhtv.mygifmaker2.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import vn.hhtv.mygifmaker2.R;
import vn.hhtv.mygifmaker2.magick.ImageMagick;
import vn.hhtv.mygifmaker2.magick.MagickImage;
import vn.hhtv.mygifmaker2.utils.AnimatedGifEncoder;
import vn.hhtv.mygifmaker2.utils.GifUtil;
import vn.hhtv.mygifmaker2.utils.LogUtil;

/**
 * Created by Nienlm on 6/16/16 1:44 PM.
 */
public class GifPreviewActivity extends Activity implements SeekBar.OnSeekBarChangeListener{

    @BindView(R.id.preview_image)
    ImageView mPreviewImage;
    @BindView(R.id.retry_btn)
    ImageView mRetryBtn;
    @BindView(R.id.share_btn)
    ImageView mShareBtn;
    @BindView(R.id.linearLayout)
    LinearLayout linearLayout;
    @BindView(R.id.seekBar)
    SeekBar mSeekbar;
    private String mGeneratedGifPath = "";
    List<Bitmap> mBitmapList = new ArrayList<>();
    List<Bitmap> mRotatedBitmapList = new ArrayList<>();
    List<String> mBitmapPath = new ArrayList<>();
    private int mRotationValue;
    int mCount;
    String mFolder;
    int mDelayTime = 0, mPreviousDelayTime = 0,  mFixedDelaytime = 0;
    int mPlaytime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif_preview);
        ButterKnife.bind(this);
        showDialog();
        mCount = getIntent().getIntExtra("count",0);
        mFolder = getIntent().getStringExtra("currentfolder");
        mRotationValue = (getIntent().getIntExtra("cameraid",1) == Camera.CameraInfo.CAMERA_FACING_BACK) ? 90 : -90;
        mPlaytime = (int) getIntent().getLongExtra("time",0);
        mDelayTime = mPlaytime / mCount;
        mFixedDelaytime = mDelayTime;

        LogUtil.wth("mCount: " + mCount);
        LogUtil.wth("mFolder: " + mFolder);
        LogUtil.wth("mPlaytime: " + mPlaytime);
        LogUtil.wth("mDelayTime: " + mDelayTime);

        for (int i = 0; i < mCount; i++){
            //mBitmapList.add(rotateBitmap(BitmapFactory.decodeFile(getOutputMediaFile(i).getAbsolutePath()),90));
            mBitmapList.add(BitmapFactory.decodeFile(GifUtil.getOutputMediaFile(i,mFolder).getAbsolutePath()));
            mBitmapPath.add(GifUtil.getOutputMediaFile(i, mFolder).getAbsolutePath());
        }
        mPreviewImage.setRotation(mRotationValue);
        mSeekbar.setMax(100);
        mSeekbar.setProgress(50);
        mSeekbar.setOnSeekBarChangeListener(this);
        hideDialog();
        playBitmap();
        new ProcessRotatingAllBitmap().execute();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser){
            mDelayTime = convertDelaytime(progress);
            LogUtil.wth("progress: " + progress +   " mDelayTime: " + mDelayTime
             + " fixDelayTime: " + mFixedDelaytime);
            cancelPlayBitmap();
            playBitmap();
        }
    }

    private int convertDelaytime(int progress){
        int oldRange = 100;
        int newRange = mFixedDelaytime * 2 - mFixedDelaytime / 2;
        int tmp = (progress * newRange   / oldRange) + mFixedDelaytime / 2;
        if (tmp > mFixedDelaytime){
            return mFixedDelaytime - (tmp - mFixedDelaytime)/2;
        }
        else {
            return mFixedDelaytime + (mFixedDelaytime - tmp)*2;
        }
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void showDialog(){
        try{
            d = new MaterialDialog.Builder(this)
                    .title("Please wait")
                    .content("Processing your content...")
                    .cancelable(false)
                    .progress(true, 1).build();
            d.show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void hideDialog(){
        if (d.isShowing())
            d.hide();
    }


    private boolean isAllFrameRotated = false;
    private boolean isUserRequestCreateGif = false;
    private class ProcessRotatingAllBitmap extends AsyncTask<Void, String, Boolean>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            LogUtil.wth("processsing rotate frame finished !");
            isAllFrameRotated = true;
            if (isUserRequestCreateGif){
                //showCreateGifDialog();
                //createGif();
                new ProcessCreateGif().execute();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Resources resources = getResources();
            float scale = resources.getDisplayMetrics().density;
            for (int i = 0; i < mBitmapList.size(); i++){
                if (i % 2 == 0) continue; // drop 50% of frame ?
                Bitmap rotatedBitmap = GifUtil.rotateBitmap(mBitmapList.get(i),mRotationValue);
                File outFile = GifUtil.getRotatedOutputMediaFile(i,mFolder);
                try {

                    Canvas c = new Canvas(rotatedBitmap);
                    Paint p = new Paint();
                    p.setColor(Color.WHITE);
                    p.setStrokeWidth((int) (50 * scale));
                    p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
                    c.drawBitmap(rotatedBitmap, 0, 0, p);
                    c.drawText("Hát hát tê vê", 10, 10, p);

                    FileOutputStream out = new FileOutputStream(outFile);
                    mRotatedBitmapList.add(rotatedBitmap);
                    rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 30, out);
                    out.flush();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            return null;
        }
    }


    private class ProcessCreateGif extends AsyncTask<Void, Void, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //showCreateGifDialog();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            hideCreateGifDialog();
            mGeneratedGifPath = result;
            shareGif(result);
        }

        @Override
        protected String doInBackground(Void... params) {
            mPreviousDelayTime = mDelayTime;
            return createGif();
        }
    }


    private void shareGif(String path){
        //File f = new File(path);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        sharingIntent.setType("image/*");
        Uri uri = Uri.parse("file://"+path);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    private String createGif(){
        String out = "";
        AnimatedGifEncoder mEncoder = new AnimatedGifEncoder();
        mEncoder.setRepeat(0);
        mEncoder.setQuality(20);
        //mEncoder.setColor(128);
        mEncoder.setDelay(mDelayTime);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        mEncoder.start(bos);
        for (int i = 0; i < mRotatedBitmapList.size(); i ++){
            mEncoder.addFrame(mRotatedBitmapList.get(i));
            if (d.isShowing()) d.setProgress(i);
        }
        mEncoder.finish();
        byte[] b = bos.toByteArray();
        FileOutputStream outStream = null;
        try{
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath(), "myGifMaker/Gif");
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, mFolder+".gif");
            file.createNewFile();
            outStream = new FileOutputStream(file);
            outStream.write(b);
            outStream.close();
            LogUtil.wth("doInBackground: finish !: " + file.getAbsolutePath());
            out = file.getAbsolutePath();
        }catch(Exception e){
            e.printStackTrace();
        }

        hideCreateGifDialog();
        return out;
    }

    MaterialDialog d;
    private void showCreateGifDialog(){
        d = new MaterialDialog.Builder(this)
                .title("Plz wait...")
                .content("Processing your gif")
                .cancelable(false)
                .progress(false, mRotatedBitmapList.size())
                .build();
        if (!d.isShowing()) d.show();
    }
    private void hideCreateGifDialog(){
        if (d != null && d.isShowing()) d.dismiss();
    }


    int pIndex = 0;
    Timer t;
    public void playBitmap(){
        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //LogUtil.wth("mDelayTime: " + mDelayTime);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPreviewImage.setImageBitmap(mBitmapList.get(pIndex));
                        //Log.d(TAG, "run: " + pIndex);
                        pIndex++;
                        if (pIndex == mBitmapList.size()) pIndex = 0;
                    }
                });
            }
        },0, mDelayTime);
    }

    public void cancelPlayBitmap(){
        if (t != null)
            t.cancel();
    }

    private void generateGif(){
        List<MagickImage> l = new ArrayList<>();
        for (String p : mBitmapPath){
            l.add(GifUtil.getMagicImage(p));
        }
        GifUtil.generateGif(l,"gif123.gif",mDelayTime);
        Toast.makeText(this,"done !",Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelPlayBitmap();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Intent i = new Intent(GifPreviewActivity.this, CaptureFrameActivity.class);
        startActivity(i);
        finish();
    }

    @OnClick({R.id.retry_btn, R.id.share_btn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.retry_btn:
                Intent i = new Intent(GifPreviewActivity.this, CaptureFrameActivity.class);
                startActivity(i);
                finish();
                break;
            case R.id.share_btn:
                //generateGif();
                if (!isAllFrameRotated){
                    isUserRequestCreateGif = true;
                    showCreateGifDialog();
                }
                else{
                    if (mGeneratedGifPath.equals("") || mPreviousDelayTime != mDelayTime) {
                        showCreateGifDialog();
                        new ProcessCreateGif().execute();
                    }
                    else{
                        shareGif(mGeneratedGifPath);
                    }

                }
                //shareGif("/storage/emulated/0/myGifMaker/Gif/1466137557256.gif");
                break;
        }
    }


}
