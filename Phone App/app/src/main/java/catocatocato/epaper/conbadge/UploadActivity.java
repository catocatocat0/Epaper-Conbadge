package catocatocato.epaper.conbadge;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import catocatocato.epaper.conbadge.communication.BluetoothHelper;
import java.util.Objects;

public class UploadActivity extends AppCompatActivity
{
    private TextView textView;
    private SocketHandler handler;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_activity);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.dlg_send);

        // View
        //--------------------------------------
        textView = findViewById(R.id.upload_text);
        textView.setText("Uploading: 0%");

        // Bluetooth helper and its handler
        //--------------------------------------
        BluetoothHelper.initialize(MainActivity.btDevice, handler = new SocketHandler());
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume()
    {
        super.onResume();

        // Bluetooth socket connection
        //--------------------------------------
        if (!BluetoothHelper.connect() || !handler.init(MainActivity.indTableImage))
        {
            setResult(RESULT_CANCELED);
            finish();
        }
        else textView.setText("Uploading 0 %");
    }

    @Override
    protected void onPause()
    {
        BluetoothHelper.close();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        BluetoothHelper.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        BluetoothHelper.close();
        setResult(RESULT_OK);
        finish();
    }

    public void onCancel(View view)
    {
        onBackPressed();
    }

    // Uploaded data buffer
    //---------------------------------------------------------
    private static final int BUFF_SIZE = 512;
    private static final byte[] buffArr = new byte[BUFF_SIZE];
    private static int buffInd;
    private static int prevDSize;
    private static int prevPxInd;
    //---------------------------------------------------------
    //  Socket Handler
    //---------------------------------------------------------
    class SocketHandler extends Handler
    {
        private int   pxInd; // Pixel index in picture
        private int   stInd; // Stage index of uploading
        private int   dSize; // Size of uploaded data by LOAD command
        private int[] array; // Values of picture pixels

        public SocketHandler()
        {
            super();
        }

        // Converts picture pixels into selected pixel format
        // and sends EPDx command
        //-----------------------------------------------------
        private boolean init(Bitmap bmp)
        {
            int w = bmp.getWidth(); // Picture with
            int h = bmp.getHeight();// Picture height
            array = new int[w*h]; // Array of pixels
            int i = 0;            // Index of pixel in the array of pixels

            // Loading pixels into array
            //-------------------------------------------------
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++, i++)
                    array[i] = getColorHex(bmp.getPixel(x, y));

            pxInd = 0;
            stInd = 0;
            dSize = 0;
            prevDSize = 0;
            prevPxInd = 0;
            buffInd = 2;                             // Size of command in bytes
            buffArr[0] = (byte)'I';                  // Name of command (Initialize)
            buffArr[1] = (byte) 0; // Index of display

            return u_send(false);
        }

        // The function is executed after every "Ok!" response
        // obtained from esp32, which means a previous command
        // is complete and esp32 is ready to get the new one.
        //-----------------------------------------------------
        private boolean handleUploadingStage()
        {
            // 5.65f colored e-Paper displays
            //-------------------------------------------------
            if(stInd == 0) return u_data();
            if(stInd == 1) return u_show();

            return true;
        }

        public int getColorHex(int color)
        {
            int r = (Color.red(color) << 16) & 0x00FF0000;
            int g = (Color.green(color) << 8) & 0x0000FF00;
            int b = Color.blue(color) & 0x000000FF;

            int colorValue = 0xFF000000 | r | g | b;
            int hexValue;

            switch (colorValue) {
                case 0xFF000000:
                    hexValue = 0;
                    break;

                case 0xFFFFFFFF:
                    hexValue = 1;
                    break;

                case 0xFF00FF00:
                    hexValue = 2;
                    break;

                case 0xFF0000FF:
                    hexValue = 3;
                    break;

                case 0xFFFF0000:
                    hexValue = 4;
                    break;

                case 0xFFFFFF00:
                    hexValue = 5;
                    break;

                case 0xFFFF8000:
                    hexValue = 6;
                    break;

                case 0xFFFFE1C8:
                    hexValue = 7;
                    break;

                default:
                    hexValue = 8;
                    break;
            }

            return hexValue;
        }

        // Sends command cmd
        //-----------------------------------------------------
        private boolean u_send(boolean next)
        {
            if (!BluetoothHelper.btThread.write(buffArr, buffInd))
                return false; // Command sending is failed

            if(next) stInd++; // Go to next stage if it is needed
            return true;      // Command is sent successful
        }

        // The finishing command
        //-----------------------------------------------------
        private boolean u_show()
        {
            buffInd = 4;           // Size of command in bytes
            buffArr[0] = (byte)255;// Name of command (Show picture)
            buffArr[1] = (byte)254;// Second Command to mitigate false positives
            buffArr[2] = (byte)253;// Third Command to mitigate false positives
            buffArr[3] = (byte)252;// Fourth Command to mitigate false positives

            // Return false if the SHOW command is not sent
            //-------------------------------------------------
            return u_send(true);

            // Otherwise exit the uploading activity.
            //-------------------------------------------------
        }

        // Sends pixels of picture and shows uploading progress
        //-----------------------------------------------------
        private boolean u_load()
        {

            // Uploading progress message
            //-------------------------------------------------
            String x = "" + (100*pxInd/array.length);
            if (x.length() > 5) x = x.substring(0, 5);
            handleUserInterfaceMessage(x);

            // Size of uploaded data
            //-------------------------------------------------
            dSize += buffInd;

            // Request message contains:
            //     data (maximum BUFF_SIZE bytes),
            //     size of uploaded data (4 bytes),
            //     length of data
            //     command name "LOAD"
            //-------------------------------------------------
            buffArr[0] = (byte)'L';

            // Size of packet
            //-------------------------------------------------
            buffArr[1] = (byte)(buffInd     );
            buffArr[2] = (byte)(buffInd >> 8);

            //-------------------------------------------------
            return u_send(pxInd >= array.length);
        }

        // Pixel format converting
        //-----------------------------------------------------
        private boolean u_data()
        {
            buffInd = 3; // pixels' data offset

            while ((pxInd < array.length) && (buffInd + 1 < BUFF_SIZE))
            {
                int v = 0;

                for(int i = 0; i < 16; i += 4)
                {
                    if (pxInd < array.length) v |= (array[pxInd] << i);
                    pxInd++;
                }

                buffArr[buffInd++] = (byte)(v     );
                buffArr[buffInd++] = (byte)(v >> 8);
            }
            return u_load();
        }

        //-------------------------------------------
        //  Handles socket message
        //-------------------------------------------
        public void handleMessage(android.os.Message msg)
        {
            // "Fatal error" event
            //-------------------------------------------------
            if (msg.what == BluetoothHelper.BT_FATAL_ERROR)
            {
                setResult(RESULT_CANCELED);
                finish();
            }

            // "Data is received" event
            //-------------------------------------------------
            else if (msg.what == BluetoothHelper.BT_RECEIVE_DATA)
            {
                // Convert data to string
                //---------------------------------------------
                String line = new String((byte[]) msg.obj, 0, msg.arg1);

                // If esp32 is ready for new command
                //---------------------------------------------
                if (line.contains("Ok!"))
                {
                    // Sets the data values to last successful packet
                    prevDSize = dSize;
                    prevPxInd = pxInd;

                    // Try to handle received data.
                    // If it's failed, restart the uploading
                    //-----------------------------------------
                    if (handleUploadingStage()) return;
                }

                // Otherwise restart the uploading
                //-----------------------------------------
                if(!(pxInd == 0 || dSize == 0)) {
                    Log.d("ඞUploadActivityඞ", "ඞByte was sussy and was voted out!\nඞ\nඞ");
                    dSize = prevDSize;
                    pxInd = prevPxInd;
                    u_load();
                }else{
                    // Restart the connection if upload failed on first packet
                    Log.d("ඞUploadActivityඞ", "ඞFirst byte was sussy!\nඞ\nඞ");
                    BluetoothHelper.initialize(MainActivity.btDevice, handler = new SocketHandler());

                    // Call the garbage collector since we make a new SocketHandler class
                    System.gc();
                }
            }
        }
    }

    //---------------------------------------------------------
    //  User Interface Handler
    //---------------------------------------------------------
    public void handleUserInterfaceMessage(String msg)
    {
        runOnUiThread(new UserInterfaceHandler(msg));
    }

    private class UserInterfaceHandler implements Runnable
    {
        public String msg;

        public UserInterfaceHandler(String msg)
        {
            this.msg = "Uploading: " + msg + "%";
        }

        @Override
        public void run()
        {
            textView.setText(msg);
        }
    }
}
