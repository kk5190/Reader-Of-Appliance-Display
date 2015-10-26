package com.example.android.road;


import android.content.ContentValues;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, TextToSpeech.OnInitListener{

    private final static String TAG = "OpenCVTEST:MainActivity";

    TextToSpeech tts;
    private JavaCameraView mOpenCvCameraView;
    protected ImageButton bClickImage;

    private boolean imageTaken = false; //to check whether to save next camera frame or not
    private Mat mBgr;
    private Mat temp;
    private String photoPath ="";
    private int anyValue = 8; //to increase decrease inner window size on click of buttons
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ROAD/assets/";
    public static final String LANG = "letsgodigital";



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loading Success");
                    mOpenCvCameraView.enableView();
                    mBgr = new Mat();
                    break;
                }
                default:
                    super.onManagerConnected(status);
                    break;
            }


        }
    };


    public  void onResume(){
        super.onResume();

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }

        }



        if (!(new File(DATA_PATH + "tessdata/" + LANG + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + LANG + ".traineddata");

                OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/" + LANG + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                Log.v(TAG, "Copied " + LANG + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + LANG + " traineddata " + e.toString());
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tts = new TextToSpeech(this, this);
        mOpenCvCameraView = (JavaCameraView)findViewById(R.id.mJavaCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //To display the result recognised by tesserect
        TextView field = (TextView)findViewById(R.id.field);

        /*
        //Removed Now the ROI can be controlled using Volume Button
        //Buttons to modify ROI
        //
        Button incrementButton = (Button)findViewById(R.id.incrmentButton);
        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(anyValue>0)
                    anyValue--;
            }
        });

        Button decrementButton = (Button)findViewById(R.id.decrementButton);
        decrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anyValue++;
            }
        });
        */

        bClickImage = (ImageButton)findViewById(R.id.bClickImage);
        bClickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (!imageTaken)
                    imageTaken = true;
                else
                    imageTaken = false;
            }
        });





    }

    @Override
    public void onDestroy(){

        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }

        super.onDestroy();
    }

    public void onPause(){
        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        temp = new Mat();

   }

    @Override
    public void onCameraViewStopped() {
        temp.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Size sizeRgba = rgba.size();

        Mat rgbaInnerWindow = rgba.submat(25, 50, 25, 50);  // inner window to select region of interest

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;
        int screenWidth = mOpenCvCameraView.getWidth();
        int screenHeight = mOpenCvCameraView.getHeight();

        int left = cols / anyValue;
        int top = rows / anyValue;

        int width = cols * 3/anyValue;
        int height = rows * 3/anyValue;

        if(width <= screenWidth && height <= screenHeight){
            rgbaInnerWindow = rgba.submat(top, top+height, left, left+width);

        }
        imageProcessor(rgbaInnerWindow);



        if(imageTaken == true){
            takePhoto(rgbaInnerWindow);
            ocr();
            imageTaken = false;
        }

        return rgba;
    }

    /*
    * to click the picture from opencv camera view
     */
    private void takePhoto(Mat rgba){
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + File.separator + appName;
        photoPath = albumPath + File.separator + currentTimeMillis + ".png";
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis);

        //Checking that the album directory exists or note
        File album = new File(albumPath);
        if(!album.isDirectory() && !album.mkdirs()){
            Log.e(TAG, "Failed to create album directory at " + albumPath);
            onTakePhotoFailed();
            return;
        }

        //tey to create the photo
        Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if(!Imgcodecs.imwrite(photoPath, mBgr)){
            Log.e(TAG, "Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }

        Log.d(TAG, "Photo saved Successfully to " + photoPath);

        //Try to insert the photo into the MEdia Store
        Uri uri;
        try{
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        } catch (final Exception e){
            Log.e(TAG, "failed to insert photo into MediaStore");
            e.printStackTrace();

            onTakePhotoFailed();
            return;
        }

    }



    private void onTakePhotoFailed(){
        final String errorMessage = "onTakePhotFailed() called";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }



    /*
    * Function to process the image before sending it to tesserect engine
     */
    private void imageProcessor(Mat rgba){

        Imgproc.cvtColor(rgba, temp, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(temp, temp, new Size(5, 5), 5);
        Imgproc.threshold(temp, temp, 0, 255, Imgproc.THRESH_OTSU+Imgproc.THRESH_BINARY);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(17, 3));
        Imgproc.morphologyEx(temp, temp, 3, element);
        Imgproc.cvtColor(temp, rgba, Imgproc.COLOR_GRAY2RGBA, 4);


    }

    /*
    * To call tesserect ocr engine
     */
    protected void ocr() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);

        try {
            ExifInterface exif = new ExifInterface(photoPath);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Log.v(TAG, "Orient: " + exifOrientation);

            int rotate = 0;
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {

                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
                // tesseract req. ARGB_8888
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            }

        } catch (IOException e) {
            Log.e(TAG, "Rotate or coversion failed: " + e.toString());
        }



        Log.v(TAG, "Before baseApi");

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, LANG);
        baseApi.setImage(bitmap);
        final String textFinal;
        String recognizedText = baseApi.getUTF8Text();
        baseApi.end();

        Log.v(TAG, "OCR Result: " + recognizedText);

        // clean up and show
        if (LANG.equalsIgnoreCase("eng")) {
            recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
        }
        if (recognizedText.length() != 0) {
            textFinal = recognizedText;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) findViewById(R.id.field)).setText(textFinal.trim());
                    tts.speak(textFinal,TextToSpeech.QUEUE_FLUSH,null);
                }
            });

        }
    }




    @Override
    public void onInit(int status) {

    }

    //To select the area of ROI using volume buttons
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (anyValue > 5)
                 anyValue--;
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                anyValue++;
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }
}
