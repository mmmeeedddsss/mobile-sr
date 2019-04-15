import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A custome file writer
 */
public class CustomFileWriter {

    public int writeFile(String fileName, InputStream inputStream) throws IOException {
        File f = new File(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
        int bytesWritten = 0;
        int b;
        while ((b = inputStream.read()) != -1) {
            bos.write(b);
            bytesWritten++;
        }
        bos.close();

        return bytesWritten;
    }
    /**
     * For testing:
     *    args[0] = Name of binary file to make a copy of.
     */
    public static void main(String[] args) throws Exception {
        //
        // Read file
        //
        CustomFileReader mfr = new CustomFileReader();
        String fileName = args[0];
        byte[] fileData = mfr.readFile(fileName);
        //
        // Write copy of file
        //
        String[] parts = fileName.split("\\.");
        InputStream is = new ByteArrayInputStream(fileData);
        new CustomFileWriter().writeFile(fileName + "_COPY." + parts[1], is);
    }
}