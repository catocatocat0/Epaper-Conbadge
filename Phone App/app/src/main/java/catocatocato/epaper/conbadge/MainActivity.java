package catocatocato.epaper.conbadge;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import catocatocato.epaper.conbadge.communication.PermissionHelper;
import catocatocato.epaper.conbadge.image_processing.EPaperDisplay;
import catocatocato.epaper.conbadge.image_processing.EPaperPicture;

/**
 * @author  Waveshare team
 * @version 1.0
 * @since   8/16/2018
 *
 * @********Heavily_modified_by_Katxe*********
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    // Request codes
    //-----------------------------
    public static final int REQ_BLUETOOTH_CONNECTION = 2;
    public static final int REQ_UPLOADING            = 6;

    // Image file name and path
    //-----------------------------
    public static String fileName;
    public static String filePath;

    // Views
    //-----------------------------
    public TextView textBlue;
    public TextView textLoad;
    public TextView textSend;
    public Button button_file;
    public ImageView pictFile; // View of loaded image
    public ImageView pictFilt; // View of filtered image
    Log log ;
    // Data
    //-----------------------------
    public static Bitmap originalImage; // Loaded image with original pixel format
    public static Bitmap indTableImage; // Filtered image with indexed colors

    // Device
    //-----------------------------
    public static BluetoothDevice btDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Image file name (null by default)
        //-----------------------------------------------------
        fileName = null;

        // Image file path (external storage root by default)
        //-----------------------------------------------------
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath();

        // Views
        //-----------------------------------------------------
        textBlue = findViewById(R.id.text_blue);
        textLoad = findViewById(R.id.text_file);
        textSend = findViewById(R.id.text_send);
        pictFile = findViewById(R.id.pict_file);
        pictFilt = findViewById(R.id.pict_filt);
        button_file = findViewById(R.id.Button_file);
        // Data
        //-----------------------------
        originalImage = null;
        indTableImage = null;
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
        if (btDevice == null) PermissionHelper.note(this, R.string.no_blue);

        // Check if any palette is selected
        //-----------------------------------------------------
        else if (indTableImage == null) PermissionHelper.note(this, R.string.no_filt);

        // Open uploading activity
        //-----------------------------------------------------
        else startActivityForResult(
            new Intent(this, UploadActivity.class),
            REQ_UPLOADING);
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //-----------------------------------------------------
        //  Messages form ScanningActivity
        //-----------------------------------------------------
        if (requestCode == REQ_BLUETOOTH_CONNECTION)
        {
            // Bluetooth device was found and selected
            //-------------------------------------------------
            if (resultCode == RESULT_OK)
            {
                // Get selected bluetooth device
                //---------------------------------------------
                btDevice = data.getParcelableExtra("DEVICE");

                // Show name and address of the device
                //---------------------------------------------
                textBlue.setText(btDevice.getName() + " (" + btDevice.getAddress() + ")");
            }
        }

        //-----------------------------------------------------
        //  Message from image selection activity
        //-----------------------------------------------------
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            File temp = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Android/data/" + getPackageName()
                    + "/temp_img.png");
            if (resultCode == RESULT_OK) {
                Uri contentURI = result.getUri();
                log.e(" ", " "+contentURI);
                try {
                    bmp_raw = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    FileOutputStream fos = new FileOutputStream(temp);
                    bmp_raw.compress(Bitmap.CompressFormat.PNG, 30, fos);
                    fos.flush();
                    fos.close();

                    log.e("getHeight "," "+ bmp_raw.getHeight());
                    log.e("getWidth "," "+ bmp_raw.getWidth());
                    log.e("getHeight 1"," "+ EPaperDisplay.getDisplay().width);
                    log.e("getWidth 1"," "+ EPaperDisplay.getDisplay().height);

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
                    int size = textLoad.getWidth();
                    pictFilt.setMaxHeight(size);
                    pictFilt.setMinimumHeight(size / 2);
                    pictFilt.setImageBitmap(indTableImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
                error.printStackTrace();
            }

        }
    }
    public  Bitmap bmp_raw;
}
