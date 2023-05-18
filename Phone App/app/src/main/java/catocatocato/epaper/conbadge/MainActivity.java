package catocatocato.epaper.conbadge;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import catocatocato.epaper.conbadge.communication.PermissionHelper;
import catocatocato.epaper.conbadge.image_processing.EPaperDisplay;
import catocatocato.epaper.conbadge.image_processing.EPaperPicture;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import catocatocato.epaper.conbadge.communication.BluetoothBattery;

public class MainActivity extends AppCompatActivity
{
    // Request codes
    //-----------------------------
    private static final int REQ_BLUETOOTH_CONNECTION = 1;
    private static final int REQ_UPLOADING            = 2;

    // Views
    //-----------------------------
    private TextView textBlue;
    private TextView textLoad;
    private ImageView pictFile; // View of loaded image
    private TextView batteryLevel;

    // Data
    //-----------------------------
    public static Bitmap originalImage; // Loaded image with original pixel format
    public static Bitmap indTableImage; // Filtered image with indexed colors

    // Device
    //-----------------------------
    public static BluetoothDevice btDevice;
    private static BluetoothBattery btBattery;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Views
        //-----------------------------------------------------
        textBlue = findViewById(R.id.text_blue);
        textLoad = findViewById(R.id.text_file);
        pictFile = findViewById(R.id.pict_file);
        batteryLevel = findViewById(R.id.battery_level);

        // Data
        //-----------------------------
        originalImage = null;
        indTableImage = null;

        // Shared Prefs
        //-----------------------------
        SharedPreferences prefs = getSharedPreferences("LAST_SAVED", MODE_PRIVATE);

        // Load the last paired btDevice
        //-----------------------------
        String btAddress = prefs.getString("BT_NAME", null);

        if(btAddress != null){
            btDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(btAddress);
            if(btDevice != null){
                textBlue.setText(btDevice.getName() + " (" + btDevice.getAddress() + ")");
            }
        }

        // Load the last selected image
        //-----------------------------
        File temp = null;
        File tempFilt = null;
        try {
            temp = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + getPackageName()
                    + "/temp_img.png");
            tempFilt = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + getPackageName()
                    + "/temp_filt.png");
        }catch (Exception e){
            PermissionHelper.note(this, "Unable to load last image!");
        }
        if(temp != null && tempFilt != null) {
            if ((temp.exists() && tempFilt.exists())) {
                originalImage = BitmapFactory.decodeFile(temp.getPath());
                indTableImage = BitmapFactory.decodeFile(tempFilt.getPath());

                pictFile.setMaxHeight(originalImage.getWidth());
                pictFile.setMinimumHeight(originalImage.getWidth() / 2);
                pictFile.setImageBitmap(originalImage);

                textLoad.setText("Last Image Loaded.");
            }
        }

        //Update the battery level
        //-----------------------------
        if(btDevice != null){
            //Creates a socket with the conbadge
            try {
                btBattery = new BluetoothBattery(btDevice, new BatHandler());
            }catch (Exception e){
                PermissionHelper.note(this, "Unable to Read Battery Level");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Update the battery level
        //-----------------------------
        if (btDevice != null) {
            //Creates a socket with the conbadge
            try {
                btBattery = new BluetoothBattery(btDevice, new BatHandler());
            } catch (Exception e) {
                PermissionHelper.note(this, "Unable to Read Battery Level");
            }
        }
    }

    //Handle Bluetooth battery messages
    class BatHandler extends Handler
    {
        public BatHandler()
        {
            super();
        }
        public void handleMessage(android.os.Message msg)
        {
            String batteryLevel = new String((byte[]) msg.obj, 0, msg.arg1);
            runOnUiThread(new BatteryIndicator(batteryLevel));
        }
    }
    private class BatteryIndicator implements Runnable
    {
        public String msg;

        public BatteryIndicator(String msg)
        {
            this.msg = "Battery: " + msg + " Volts";
        }

        @Override
        public void run()
        {
            batteryLevel.setText(msg);
        }
    }


    public void onScan(View view)
    {
        // Open bluetooth devices scanning activity
        //-----------------------------------------------------
        startActivityForResult(
            new Intent(this, ScanningActivity.class),
            REQ_BLUETOOTH_CONNECTION);
    }

    public void onLoad(View view)
    {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setFixAspectRatio(true)
                .setAspectRatio(EPaperDisplay.getDisplay().width, EPaperDisplay.getDisplay().height)
                .start(this);

    }

    public void onUpload(View view)
    {
        // Check if any devices is found
        //-----------------------------------------------------
        if (btDevice == null) PermissionHelper.note(this, "Conbadge not selected.");

        // Open uploading activity
        //-----------------------------------------------------
        else {
            if(btBattery != null) {
                btBattery.cancel();
            }
            startActivityForResult(new Intent(this, UploadActivity.class), REQ_UPLOADING);
        }
    }

    @Override
    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        //-----------------------------------------------------
        //  Messages form ScanningActivity
        //-----------------------------------------------------
        if (requestCode == REQ_BLUETOOTH_CONNECTION) {
            // Bluetooth device was found and selected
            //-------------------------------------------------
            if (resultCode == RESULT_OK) {
                // Get selected bluetooth device
                //---------------------------------------------
                btDevice = data.getParcelableExtra("DEVICE");

                // Show name and address of the device
                //---------------------------------------------
                textBlue.setText(btDevice.getName() + " (" + btDevice.getAddress() + ")");

                // Pair with the bluetooth device
                //---------------------------------------------
                btDevice.createBond();

                // Save the bluetooth device to SharedPrefs
                //---------------------------------------------
                SharedPreferences.Editor editor = getSharedPreferences("LAST_SAVED", MODE_PRIVATE).edit();
                editor.putString("BT_NAME", btDevice.getAddress());
                editor.apply();
            }
        }

        //-----------------------------------------------------
        //  Message from image selection activity
        //-----------------------------------------------------
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            File temp = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + getPackageName()
                    + "/temp_img.png");
            File tempFilt = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + getPackageName()
                    + "/temp_filt.png");
            if (resultCode == RESULT_OK) {
                Uri contentURI = result.getUri();
                try {
                    bmp_raw = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    FileOutputStream fos = new FileOutputStream(temp);
                    bmp_raw.compress(Bitmap.CompressFormat.PNG, 30, fos);
                    fos.flush();
                    fos.close();

                    bmp_raw = Bitmap.createScaledBitmap(bmp_raw, EPaperDisplay.getDisplay().width, EPaperDisplay.getDisplay().height, false);
                    originalImage = bmp_raw;
                    textLoad.setText(contentURI.getLastPathSegment());
                    int pictSize = bmp_raw.getWidth();
                    pictFile.setMaxHeight(pictSize);
                    pictFile.setMinimumHeight(pictSize / 2);
                    pictFile.setImageBitmap(bmp_raw);

                    // Image processing
                    //-----------------------------------------------------
                    MainActivity.indTableImage = EPaperPicture.createIndexedImage();

                    // Save processed image to file
                    fos = new FileOutputStream(tempFilt);
                    indTableImage.compress(Bitmap.CompressFormat.PNG, 100, fos);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
                error.printStackTrace();
            }
        }
    }
    public  Bitmap bmp_raw;
}
