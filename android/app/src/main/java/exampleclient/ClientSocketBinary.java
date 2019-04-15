import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * The client will read the image file from its filesystem, then send the binary data through socket
 */
public class ClientSocketBinary {
    private Socket socket;
    private CustomFileReader fileReader;
    private ClientSocketBinary(InetAddress serverAddress, int serverPort) throws Exception {
        this.socket = new Socket(serverAddress, serverPort);
    }

    private void sendFile(String fileName) throws IOException {
        //
        // Read file from disk
        //
        fileReader = new CustomFileReader();
        byte[] data = fileReader.readFile(fileName);
        //
        // Send binary data over the TCP/IP socket connection
        //
        for (byte i : data) {
            this.socket.getOutputStream().write(i);
        }

        System.out.println("\r\nSent " + data.length + " bytes to server.");
    }
    /**
     * Requires 3 arguments:
     *     1: TCP/IP server host name or IP address
     *     2: TCP/IP server port number
     *     3: Absolute path and file name of file to send
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ClientSocketBinary client = new ClientSocketBinary(
                InetAddress.getByName(args[0]),
                Integer.parseInt(args[1]));
        System.out.println("\r\nConnected to Server: " + client.socket.getInetAddress());
        client.sendFile(args[2]);
    }
}