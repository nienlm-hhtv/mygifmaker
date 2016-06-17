package vn.hhtv.mygifmaker2.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import vn.hhtv.mygifmaker2.magick.ImageInfo;
import vn.hhtv.mygifmaker2.magick.MagickImage;
import vn.hhtv.mygifmaker2.magick.QuantizeInfo;

/**
 * Created by Nienlm on 6/16/16 1:58 PM.
 */
public class GifUtil {

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(byte[] data, int   reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        //options.inPurgeable = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        options.inPurgeable = true;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }


    public static File getOutputMediaFile(int index, String folder){
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/myGifMaker/"+ "/Files/" + folder);
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        File mediaFile;
        String mImageName="MI_"+ index +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    public static File getRotatedOutputMediaFile(int index, String folder){
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/myGifMaker/"+ "/Files/" + folder);
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        File mediaFile;
        String mImageName="MRI_"+ index +".png";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }


    public static byte[] rotateNV21(byte[] input,  int width, int height, int rotation) {
        byte[] output = new byte[input.length];
        boolean swap = (rotation == 90 || rotation == 270);
        boolean yflip = (rotation == 90 || rotation == 180);
        boolean xflip = (rotation == 270 || rotation == 180);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int xo = x, yo = y;
                int w = width, h = height;
                int xi = xo, yi = yo;
                if (swap) {
                    xi = w * yo / h;
                    yi = h * xo / w;
                }
                if (yflip) {
                    yi = h - yi - 1;
                }
                if (xflip) {
                    xi = w - xi - 1;
                }
                output[w * yo + xo] = input[w * yi + xi];
                int fs = w * h;
                int qs = (fs >> 2);
                xi = (xi >> 1);
                yi = (yi >> 1);
                xo = (xo >> 1);
                yo = (yo >> 1);
                w = (w >> 1);
                h = (h >> 1);
                // adjust for interleave here
                int ui = fs + (w * yi + xi) * 2;
                int uo = fs + (w * yo + xo) * 2;
                // and here
                int vi = ui + 1;
                int vo = uo + 1;
                output[uo] = input[ui];
                output[vo] = input[vi];
            }
        }
        return output;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    public static MagickImage getMagicImage(String path){
        try{
            MagickImage image = new MagickImage(new ImageInfo(path));
            QuantizeInfo quantizeInfo = new QuantizeInfo();
            quantizeInfo.setNumberColors(256);
            quantizeInfo.setTreeDepth(4); // 8 is max
            image.quantizeImage(quantizeInfo);
            return image;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }



    public static String generateGif(List<MagickImage> frames, String filename,  int delay){
        try{
            FileOutputStream out = null;
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath(), "myGifMaker/gif");
            File gifFile = new File(dir, filename + ".gif");

            final MagickImage[] images = frames.toArray(new MagickImage[frames.size()]);
            final MagickImage finalGif = new MagickImage(images);
            finalGif.setDelay(delay);
            finalGif.setImageFormat("gif");
            finalGif.setIterations(1000);
            final ImageInfo resultInfo = new ImageInfo(gifFile.getAbsolutePath());
            resultInfo.setMagick("gif");
            final byte[] img = finalGif.imageToBlob(resultInfo);
            return  gifFile.getAbsolutePath();
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }
}
