package catocatocato.epaper.conbadge.communication;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;

public class BluetoothBattery {
    //Class Vars
    private final Handler handler; // handler that gets info from Bluetooth service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static ConnectedThread btThread;
    private final Button batteryButton;

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        int MESSAGE_READ = 0;
    }

    public BluetoothBattery(BluetoothDevice btDevice, Handler handler, Button bluetoothButton){
        this.handler = handler;
        this.batteryButton = bluetoothButton;
        try
        {
            BluetoothSocket btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
            btSocket.connect();
            btThread = new ConnectedThread(btSocket);
            btThread.start();
        }
        catch (IOException e) {
            Log.d("ඞSOCKETCONNECTඞ",e.toString());
            batteryButton.setBackgroundColor(Color.parseColor("#E91E63"));
        }
    }

    public void cancel(){
        btThread.cancel();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.d("ඞTHREADඞ",e.toString());
                batteryButton.setBackgroundColor(Color.parseColor("#E91E63"));
            }

            mmInStream = tmpIn;
        }

        public void run() {
            // mmBuffer store for the stream
            byte[] mmBuffer = new byte[256];
            // Keep listening to the InputStream until an exception occurs.

            while (true) {
                try {
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = handler.obtainMessage(
                            MessageConstants.MESSAGE_READ, mmInStream.read(mmBuffer), -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d("ඞSOCKETඞ",e.toString());
                    batteryButton.setBackgroundColor(Color.parseColor("#E91E63"));
                    break;
                }
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d("ඞCANCELඞ",e.toString());
                batteryButton.setBackgroundColor(Color.parseColor("#E91E63"));
            }
        }
    }
}