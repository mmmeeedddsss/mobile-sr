package exampleclient;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * TCPCLient class implemented for test purposes*/
public class TCPClient {
    private String  serverMessage;
    // Computer's IP address for trial purposes, else the server's IP
    public String serverIp = null;
    public static final int SERVERPORT = 5054;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;

    private PrintWriter out = null;
    private BufferedReader in = null;

    /*Constructor*/
    public TCPClient(final OnMessageReceived listener, String ipAddressOfServerDevice) {
        mMessageListener = listener;
        serverIp = ipAddressOfServerDevice;
    }

    /*Send a message entered by client to server*/
    public void sendMessage(String message){
        if (out != null && !out.checkError()) {
            System.out.println("message: "+ message);
            out.println(message);
            out.flush();
        }
    }

    /*stop the client from sending messages to server*/
    public void stopClient(){
        mRun = false;
    }

    /*There will be a asynctask and this method will be implemented in doInBackground()*/
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }


    public void run() {
        mRun = true;
        try {

            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(serverIp);

            Log.e("TCP SI Client", "SI: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, SERVERPORT);
            try {

                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                Log.e("TCP SI Client", "SI: Sent.");

                Log.e("TCP SI Client", "SI: Done.");

                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //in this while the client listens for the messages sent by the server
                while (mRun) {
                    serverMessage = in.readLine();

                    if (serverMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(serverMessage);
                        Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
                    }
                    serverMessage = null;
                }
            }
            catch (Exception e) {
                Log.e("TCP SI Error", "SI: Error", e);
                e.printStackTrace();
            }
            finally {
                socket.close();
            }
        } catch (Exception e) {
            Log.e("TCP SI Error", "SI: Error", e);

        }

    }
}
