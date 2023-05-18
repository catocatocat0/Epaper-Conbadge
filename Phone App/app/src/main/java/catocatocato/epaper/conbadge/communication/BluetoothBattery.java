package catocatocato.epaper.conbadge.communication;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothBattery {
    //Class Vars
    private final Handler handler; // handler that gets info from Bluetooth service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static ConnectedThread btThread;

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        int MESSAGE_READ = 0;
    }

    public BluetoothBattery(BluetoothDevice btDevice, Handler handler){
        this.handler = handler;
        try
        {
            BluetoothSocket btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
            btSocket.connect();
            btThread = new ConnectedThread(btSocket);
            btThread.start();
        }
        catch (IOException e) {
            Log.d("ඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞ",e.toString());
        }
    }

    public void cancel(){
        btThread.cancel();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.d("ඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞ",e.toString());
            }

            mmInStream = tmpIn;
        }

        public void run() {
            mmBuffer = new byte[256];
            // Keep listening to the InputStream until an exception occurs.

            while (true) {
                try {
                    // Read from the InputStream.
                    Log.d("ඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞ","1");
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = handler.obtainMessage(
                            MessageConstants.MESSAGE_READ, mmInStream.read(mmBuffer), -1,
                            mmBuffer);
                    Log.d("ඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞ","2");
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d("ඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞ",e.toString());
                    break;
                }
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d("ඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞඞ",e.toString());
            }
        }
    }

}
