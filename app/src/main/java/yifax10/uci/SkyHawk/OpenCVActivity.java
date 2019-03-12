package yifax10.uci.SkyHawk;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;

public class OpenCVActivity extends AppCompatActivity {
    private static final String TAG = "OpenCVActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate Done!");
        setContentView(R.layout.opencv_result);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Mat input = null;
        try {
            input = Utils.loadResource(this, R.drawable.test1, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Input Success!");
        showImg(input, (ImageView) findViewById(R.id.ocvOriginal));
        Mat result = ImageProcessor.process(input);
        showImg(result, (ImageView) findViewById(R.id.ocvResult));
    }

    private void showImg(Mat img, ImageView a) {
        Bitmap bm = Bitmap.createBitmap(img.cols(), img.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bm);
        a.setImageBitmap(bm);

    }
}