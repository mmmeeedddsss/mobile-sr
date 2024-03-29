package com.senior_project.group_1.mobilesr.networking;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * The client will read the image file from its filesystem, then send the binary data through socket
 */
public class ClientSocketBinary {
    private Socket socket;

    public ClientSocketBinary(InetAddress serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
    }

    public void sendBitmap(Bitmap bm) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG,99, stream);

        Log.i("ClientSocketBinary.sendBitmap file size", String.format("%10d", stream.size()));

        socket.getOutputStream().write(String.format("%10d", stream.size()).getBytes());
        socket.getOutputStream().write(stream.toByteArray());
        Log.i("ClientSocketBinary","Sent bitmap to server");
    }

    public Bitmap getBitmap() throws IOException{
        return BitmapFactory.decodeStream( socket.getInputStream() );
    }

    public void endConnection() throws IOException {
        socket.getOutputStream().write(String.format("%10d", 0).getBytes());
    }
}