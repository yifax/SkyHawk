package yifax10.uci.SkyHawk;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;

import static org.opencv.android.Utils.bitmapToMat;

public class OpenCVActivity extends AppCompatActivity {
    private static final String TAG = "OpenCVActivity";
    Bitmap screenshot32;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate Done!");
        setContentView(R.layout.opencv_result);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Button button = findViewById(R.id.selButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        "image/*");
                startActivityForResult(intent, 0x1);
            }
        });


    }

    private void showImg(Mat img, ImageView a) {
        Bitmap bm = Bitmap.createBitmap(img.cols(), img.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bm);
        a.setImageBitmap(bm);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if (requestCode == 0x1 && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Bitmap screenshot = null;
                try {
                    screenshot = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Mat input = new Mat();
                screenshot32 = screenshot.copy(Bitmap.Config.ARGB_8888, true);
                bitmapToMat(screenshot32, input);

                Mat result = null;
                ImageView imageView = findViewById(R.id.ocvResult);
                try {
                    result = ImageProcessor.process(input);
                }
                catch (Exception e) {
                    Log.e(TAG, "ImageProcessor Exception!");
                    e.printStackTrace();
                }

                if (result != null){
                    showImg(result, imageView);
                }
                else {
                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.oops));
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}