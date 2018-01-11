package com.example.masters.mixpic2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.BiometricsActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends BiometricsActivity {

    Uri uri;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_FILE_FORMAT = 3;
    // Capture image type
    public static final int CAPTURE_RAW = 1;
    public static final int CAPTURE_WSQ = 2;

    // Key names received from the BluetoothDataService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String SHOW_MESSAGE = "show_message";
    public static final String TOAST = "toast";

    private Button mButtonOpen;
    private Button mButtonCaptureRAW;
    private Button mButtonCaptureWSQ;
    private Button mButtonStop;
    private Button mButtonSave;
    private TextView mMessage;
    private ImageView mFingerImage;
    private ProgressBar mProgressbar1;
    public static boolean mStop = true;
    public static boolean mConnected = false;
    public static int mStep = 0;
    public static int mCaptureType = 0;
    public static boolean mOpened = false;
    private String mConnectedDeviceName = null;
//    // Local Bluetooth adapter
//    private BluetoothAdapter mBluetoothAdapter = null;
//    private BluetoothDataService mBTService = null;

    public static byte[] mImageFP = new byte[153602];
    private static Bitmap mBitmapFP;
    public static byte[] mWsqImageFP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMessage = (TextView) findViewById(R.id.tvMessage);
        mFingerImage = (ImageView) findViewById(R.id.imageFinger);
        mProgressbar1 = (ProgressBar) findViewById(R.id.progressBar1);
        mProgressbar1.setMax(100);
        // Initialize the send button with a listener that for click events
        mButtonOpen = (Button) findViewById(R.id.btn_open_bt);
        mButtonCaptureRAW = (Button) findViewById(R.id.btn_capture_raw);
        mButtonCaptureWSQ = (Button) findViewById(R.id.btn_capture_wsq);
        mButtonStop = (Button) findViewById(R.id.btn_stop);
        mButtonSave = (Button) findViewById(R.id.btn_save);
        mButtonCaptureRAW.setEnabled(false);
        mButtonCaptureWSQ.setEnabled(false);
        mButtonSave.setEnabled(false);
        mButtonStop.setEnabled(false);

        mButtonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mOpened)
                {
                    mMessage.setText("Grab Fingerprint Start");
                    mFingerImage.setImageDrawable(null);
                    grabFingerprint();
//                    startDeviceListActivity();
                }
//                else
//                {
//                    mStop = true;
//                    if( mBTService != null )
//                    {
//                        mBTService.stop();
//                        mBTService = null;
//                    }
                    mButtonOpen.setText("Open SCANNER");
                    mOpened = false;
                    mButtonCaptureRAW.setEnabled(false);
                    mButtonCaptureWSQ.setEnabled(false);
                    mButtonStop.setEnabled(false);
                    mButtonSave.setEnabled(false);
                }
        });

//        mButtonCaptureRAW.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCaptureType = CAPTURE_RAW;
//                mButtonCaptureRAW.setEnabled(false);
//                mButtonCaptureWSQ.setEnabled(false);
//                mButtonSave.setEnabled(false);
//                mButtonStop.setEnabled(true);
//                startCapture();
//            }
//        });
//
//        mButtonCaptureWSQ.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCaptureType = CAPTURE_WSQ;
//                mButtonCaptureRAW.setEnabled(false);
//                mButtonCaptureWSQ.setEnabled(false);
//                mButtonSave.setEnabled(false);
//                mButtonStop.setEnabled(true);
//                startCapture();
//            }
//        });

//                mButtonStop.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        stopCapture();
//                    }
//                });

                mButtonSave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SaveImage();
                    }
                });

            }

    @Override
    public void onFingerprintGrabbed(ResultCode result, Bitmap bitmap,
                                     byte[] iso, String filepath, String status) {

        if(status != null){
            mMessage.setText(status);
        }
        if(bitmap != null){
            mFingerImage.setImageBitmap(bitmap);
            SaveImage();
//            }

        }

    }

    private void SaveImage()
    {
        Intent serverIntent = new Intent(this, SelectFileFormatActivity.class);
        startActivityForResult(serverIntent, REQUEST_FILE_FORMAT);
    }

    private void SaveImageByFileFormat(String fileFormat, String fileName)
    {
        if( fileFormat.compareTo("WSQ") == 0 )	//save wsq file
        {
            if( mWsqImageFP != null )
            {
                File file = new File(fileName);
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(mWsqImageFP, 0, mWsqImageFP.length);	// save the wsq_size bytes data to file
                    out.close();
                    mMessage.setText("Image is saved as " + fileName);
                } catch (Exception e) {
                    mMessage.setText("Exception in saving file");
                }
            }
            else
                mMessage.setText("Invalid WSQ image!");
            return;
        }
        // 0 - save bitmap file
        File file = new File(fileName);
        try {
            FileOutputStream out = new FileOutputStream(file);
            MyBitmapFile fileBMP = new MyBitmapFile(320, 480, mImageFP);
            out.write(fileBMP.toBytes());
            out.close();
            mMessage.setText("Image is saved as " + fileName);
        } catch (Exception e) {
            mMessage.setText("Exception in saving file");
        }
    }

    private void ShowBitmap()
    {
        int[] pixels = new int[153600];
        for( int i=0; i<153600; i++)
            pixels[i] = mImageFP[i];
        Bitmap emptyBmp = Bitmap.createBitmap(pixels, 320, 480, Bitmap.Config.RGB_565);

        int width, height;
        height = emptyBmp.getHeight();
        width = emptyBmp.getWidth();

        mBitmapFP = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(mBitmapFP);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(emptyBmp, 0, 0, paint);

        mFingerImage.setImageBitmap(mBitmapFP);
    }

    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
////        if(D) Log.d(TAG, "onActivityResult " + resultCode);
//        switch (requestCode) {
////            case REQUEST_ENABLE_BT:
////                // When the request to enable Bluetooth returns
////                if (resultCode == Activity.RESULT_OK) {
////                    // Bluetooth is now enabled, so set up a session
////                } else {
////                    // User did not enable Bluetooth or an error occured
////                    Log.d(TAG, "BT is not enabled");
////                    Toast.makeText(this, "BT is not enabled", Toast.LENGTH_SHORT).show();
////                    finish();
////                }
////                break;
////            case REQUEST_CONNECT_DEVICE:
////                // When DeviceListActivity returns with a device to connect
////                if (resultCode == Activity.RESULT_OK) {
////                    // Get the device MAC address
////                    String address = data.getExtras()
////                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
////                    mStop = false;
////                    mButtonOpen.setText("Close BT Comm");
////                    // Get the BLuetoothDevice object
////                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
////                    // Attempt to connect to the device
////                    mBTService = new BluetoothDataService(this, mHandler);
////                    mBTService.connect(device);
////                }
////                else
////                    mMessage.setText("Not connected");
////                break;
//            case REQUEST_FILE_FORMAT:
//                if (resultCode == Activity.RESULT_OK) {
//                    // Get the file format
//                    String[] extraString = data.getExtras().getStringArray(SelectFileFormatActivity.EXTRA_FILE_FORMAT);
//                    String fileFormat = extraString[0];
//                    String fileName = extraString[1];
//                    SaveImageByFileFormat(fileFormat, fileName);
//                }
//                else
//                    mMessage.setText("Cancelled!");
//                break;
//        }
//    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 111) {  // take photo

                int orientation = -1;
                ExifInterface exif;

                Uri selectedImage = uri;
                getContentResolver().notifyChange(selectedImage, null);
                Bitmap reducedSizeBitmap = getBitmap(uri.getPath()); // convert source picture to bitmap 500KB

                //+
                try {
                    exif = new ExifInterface(selectedImage.getPath());
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                switch (requestCode) {
//            case REQUEST_ENABLE_BT:
//                // When the request to enable Bluetooth returns
//                if (resultCode == Activity.RESULT_OK) {
//                    // Bluetooth is now enabled, so set up a session
//                } else {
//                    // User did not enable Bluetooth or an error occured
//                    Log.d(TAG, "BT is not enabled");
//                    Toast.makeText(this, "BT is not enabled", Toast.LENGTH_SHORT).show();
//                    finish();
//                }
//                break;
//            case REQUEST_CONNECT_DEVICE:
//                // When DeviceListActivity returns with a device to connect
//                if (resultCode == Activity.RESULT_OK) {
//                    // Get the device MAC address
//                    String address = data.getExtras()
//                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
//                    mStop = false;
//                    mButtonOpen.setText("Close BT Comm");
//                    // Get the BLuetoothDevice object
//                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//                    // Attempt to connect to the device
//                    mBTService = new BluetoothDataService(this, mHandler);
//                    mBTService.connect(device);
//                }
//                else
//                    mMessage.setText("Not connected");
//                break;
                    case REQUEST_FILE_FORMAT:
                        if (resultCode == Activity.RESULT_OK) {
                            // Get the file format
                            String[] extraString = data.getExtras().getStringArray(SelectFileFormatActivity.EXTRA_FILE_FORMAT);
                            String fileFormat = extraString[0];
                            String fileName = extraString[1];
                            SaveImageByFileFormat(fileFormat, fileName);
                        }
                        else
                            mMessage.setText("Cancelled!");
                        break;
                }

                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:   // 6
                        reducedSizeBitmap = rotateImage(reducedSizeBitmap, 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:  // 3
                        reducedSizeBitmap = rotateImage(reducedSizeBitmap, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:  // 8
                        reducedSizeBitmap = rotateImage(reducedSizeBitmap, 270);
                        break;
                    default:   // 0
                        //reducedSizeBitmap = rotateImage(reducedSizeBitmap, 0);
                }
                //-

                ImageView imgView = (ImageView) findViewById(R.id.imageFinger);
                imgView.setImageBitmap(reducedSizeBitmap);

//                delete.setVisibility(View.VISIBLE);
//                galleryAddPic();
            }
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {  // rotate bitmap
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),matrix, true);
//        return Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888);
//        return Bitmap.createBitmap(source.getWidth(), source.getHeight(),Bitmap.Config.ARGB_8888);
    }

    public Bitmap getBitmap(String path) {            // return Bitmap from file path

        Uri uri = Uri.fromFile(new File(path));
        InputStream in = null;
        try {
            final int IMAGE_MAX_SIZE = 500000; // 500 KB
            in = getContentResolver().openInputStream(uri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();


            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }
            //Log.d("", "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

            Bitmap b = null;
            in = getContentResolver().openInputStream(uri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = b.getHeight();
                int width = b.getWidth();
                //Log.d("", "1th scale operation dimenions - width: " + width + ", height: " + height);

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,
                        (int) y, true);
                b.recycle();
                b = scaledBitmap;

                System.gc();
            } else {
                b = BitmapFactory.decodeStream(in);
            }
            in.close();

            //Log.d("", "bitmap size - width: " + b.getWidth() + ", height: " + b.getHeight());
            return b;
        } catch (IOException e) {
            //Log.e("", e.getMessage(), e);
            return null;
        }
    }
}
