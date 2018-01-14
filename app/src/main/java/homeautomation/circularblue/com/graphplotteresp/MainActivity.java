package homeautomation.circularblue.com.graphplotteresp;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private final Handler mHandler = new Handler();
    //    private Runnable mTimer1;
    private Runnable mTimer2;

    Queue message_queue;
    //    private LineGraphSeries<DataPoint> mSeries1;
    private LineGraphSeries<DataPoint> mSeries2;
    private double graph2LastXValue = 5d;

    Button button;
    Button connect_button;
    EditText edittext_send_msg;
    TextView temperature_textView;
    TextView status_textView;

    int PORT = 80;
    String HOST = "192.168.4.1";
    String status;
    // Tag for logging
    private final String TAG = getClass().getSimpleName();

    // AsyncTask object that manages the connection in a separate thread
    WiFiSocketTask wifiTask = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        GraphView graph = (GraphView) findViewById(R.id.graph);
        //       mSeries1 = new LineGraphSeries<>(generateData());
        //       graph.addSeries(mSeries1);

        GraphView graph2 = (GraphView) findViewById(R.id.graph2);
        mSeries2 = new LineGraphSeries<>();
        graph2.addSeries(mSeries2);
        graph2.getViewport().setXAxisBoundsManual(true);
        graph2.getViewport().setMinX(0);
        graph2.getViewport().setMaxX(40);

//        new Thread(new ClientThread()).start();
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendButtonPressed(v);
            }
        });

        connect_button = (Button) findViewById(R.id.connect_button);
        connect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status =  getStatus();
                switch (status){
                    case "Connected":
                        disconnectButtonPressed(v);
                        break;
                    case "Disconnected":
                        connectButtonPressed(v);
                        break;
                }
            }
        });

        edittext_send_msg = (EditText) findViewById(R.id.EditText01);
        temperature_textView = (TextView) findViewById(R.id.temperature_value_textView);
        status_textView      = (TextView) findViewById(R.id.status_value_textView);
        message_queue = new LinkedList();
        connectButtonPressed(null);

    }

    @Override
    public void onResume() {
        super.onResume();
//        mTimer1 = new Runnable() {
//            @Override
//            public void run() {
//                mSeries1.resetData(generateData());
//                mHandler.postDelayed(this, 300);
//            }
//        };
//        mHandler.postDelayed(mTimer1, 300);

        mTimer2 = new Runnable() {
            @Override
            public void run() {
                graph2LastXValue += 1d;
                mSeries2.appendData(new DataPoint(graph2LastXValue,(double)message_queue.element()), true, 40);
                message_queue.remove();
                mHandler.postDelayed(this, 200);
            }
        };
        //mHandler.postDelayed(mTimer2, 1000);
    }

    @Override
    public void onPause() {
        //    mHandler.removeCallbacks(mTimer1);
       // mHandler.removeCallbacks(mTimer2);
        super.onPause();
    }

    private DataPoint[] generateData() {
        int count = 30;
        DataPoint[] values = new DataPoint[count];
        for (int i = 0; i < count; i++) {
            double x = i;
            double f = mRand.nextDouble() * 0.15 + 0.3;
            double y = Math.sin(i * f + 2) + mRand.nextDouble() * 0.3;
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        return values;
    }

    double mLastRandom = 2;
    Random mRand = new Random();

    private double getRandom() {
        return mLastRandom += mRand.nextDouble() * 0.5 - 0.25;
    }


    /**
     * Helper function, print a status to both the UI and program log.
     */
    void setStatus(String s) {
        Log.v(TAG, s);
        status_textView.setText(s);
        status = s;
   //     Toast.makeText(this,"STATUS" + s, Toast.LENGTH_SHORT).show();
    }
    String getStatus(){
        return status;
    }

    /**
     * Try to start a connection with the specified remote host.
     */
    public void connectButtonPressed(View v) {

        if(wifiTask != null) {
            setStatus("Already connected!");
            return;
        }

        try {
            // Get the remote host from the UI and start the thread
            String host = HOST;// editTextAddress.getText().toString();
            int port    = PORT;// Integer.parseInt(editTextPort.getText().toString());

            // Start the asyncronous task thread
            setStatus("Attempting to connect...");
            wifiTask = new WiFiSocketTask(host, port);
            wifiTask.execute();

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Invalid address/port!");
        }
    }

    /**
     * Disconnect from the connection.
     */
    public void disconnectButtonPressed(View v) {

        if(wifiTask == null) {
            setStatus("Already disconnected!");
            return;
        }

        wifiTask.disconnect();
        setStatus("Disconnecting...");
    }

    /**
     * Invoked by the AsyncTask when the connection is successfully established.
     */
    private void connected() {
        setStatus("Connected");
        connect_button.setText("DISCONNECT");
        button.setEnabled(true);
    }

    /**
     * Invoked by the AsyncTask when the connection ends..
     */
    private void disconnected() {
        setStatus("Disconnected");
        connect_button.setText("CONNECT");
        button.setEnabled(false);
    //    textRX.setText("");
     //   textTX.setText("");
        wifiTask = null;
    }

    /**
     * Invoked by the AsyncTask when a newline-delimited message is received.
     */
    private void gotMessage(String msg) {
    //    textRX.setText(msg);
        Log.v(TAG, "[RX] " + msg);
        String data = msg.replace("SOCKET_PING","");
        Log.d(TAG,"[DATA] " + data);
       // message_queue.add(Double.parseDouble(data));
        temperature_textView.setText(data + " Degree C");
        graph2LastXValue += 1d;
        mSeries2.appendData(new DataPoint(graph2LastXValue,Double.parseDouble(data)), true, 40);

    }

    /**
     * Send the message typed in the input field using the AsyncTask.
     */
    public void sendButtonPressed(View v) {

        if(wifiTask == null) return;

        String msg =  edittext_send_msg.getText().toString();
        if(msg.length() == 0) return;

        wifiTask.sendMessage(msg);
        edittext_send_msg.setText("");

       // textTX.setText(msg);
        Log.v(TAG, "[TX] " + msg);
    }
    /**
     * AsyncTask that connects to a remote host over WiFi and reads/writes the connection
     * using a socket. The read loop of the AsyncTask happens in a separate thread, so the
     * main UI thread is not blocked. However, the AsyncTask has a way of sending data back
     * to the UI thread. Under the hood, it is using Threads and Handlers.
     */
    public class WiFiSocketTask extends AsyncTask<Void, String, Void> {

        // Location of the remote host
        String address;
        int port;

        // Special messages denoting connection status
        private static final String PING_MSG = "SOCKET_PING";
        private static final String CONNECTED_MSG = "SOCKET_CONNECTED";
        private static final String DISCONNECTED_MSG = "SOCKET_DISCONNECTED";

        Socket socket = null;
        BufferedReader inStream = null;
        OutputStream outStream = null;

        // Signal to disconnect from the socket
        private boolean disconnectSignal = false;

        // Socket timeout - close if no messages received (ms)
        private int timeout = 5000;

        // Constructor
        WiFiSocketTask(String address, int port) {
            this.address = address;
            this.port = port;
        }

        /**
         * Main method of AsyncTask, opens a socket and continuously reads from it
         */
        @Override
        protected Void doInBackground(Void... arg) {

            try {

                // Open the socket and connect to it
                socket = new Socket();
                socket.connect(new InetSocketAddress(address, port), timeout);

                // Get the input and output streams
                inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outStream = socket.getOutputStream();

                // Confirm that the socket opened
                if(socket.isConnected()) {

                    // Make sure the input stream becomes ready, or timeout
                    long start = System.currentTimeMillis();
                    while(!inStream.ready()) {
                        long now = System.currentTimeMillis();
                        if(now - start > timeout) {
                            Log.e(TAG, "Input stream timeout, disconnecting!");
                            disconnectSignal = true;
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "Socket did not connect!");
                    disconnectSignal = true;
                }

                // Read messages in a loop until disconnected
                while(!disconnectSignal) {

                    // Parse a message with a newline character
                    String msg = inStream.readLine();

                    // Send it to the UI thread
                    publishProgress(msg);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error in socket thread!");
            }

            // Send a disconnect message
            publishProgress(DISCONNECTED_MSG);

            // Once disconnected, try to close the streams
            try {
                if (socket != null) socket.close();
                if (inStream != null) inStream.close();
                if (outStream != null) outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * This function runs in the UI thread but receives data from the
         * doInBackground() function running in a separate thread when
         * publishProgress() is called.
         */
        @Override
        protected void onProgressUpdate(String... values) {

            String msg = values[0];
            if(msg == null) return;

            // Handle meta-messages
            if(msg.equals(CONNECTED_MSG)) {
                connected();
            } else if(msg.equals(DISCONNECTED_MSG))
                disconnected();
            else if(msg.equals(PING_MSG))
            {}

            // Invoke the gotMessage callback for all other messages
            else
                gotMessage(msg);

            super.onProgressUpdate(values);
        }

        /**
         * Write a message to the connection. Runs in UI thread.
         */
        public void sendMessage(String data) {

            try {
                outStream.write(data.getBytes());
                outStream.write('\n');
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Set a flag to disconnect from the socket.
         */
        public void disconnect() {
            disconnectSignal = true;
        }
    }
}

