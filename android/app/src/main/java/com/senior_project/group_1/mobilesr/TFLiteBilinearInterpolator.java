package com.senior_project.group_1.mobilesr;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Class representing the bilinear interpolator built for TFLite.
public class TFLiteBilinearInterpolator implements BitmapProcessor {
    private static final String MODEL_PATH = "basic_srcnn_nearestn_64.tflite";

    private static final int SIZEOF_FLOAT = 4;

    private int INPUT_IMAGE_WIDTH = ApplicationConstants.IMAGE_CHUNK_SIZE_X;
    private int INPUT_IMAGE_HEIGHT = ApplicationConstants.IMAGE_CHUNK_SIZE_Y;

    private int INPUT_TENSOR_BATCH = 1;
    private int INPUT_TENSOR_WIDTH = INPUT_IMAGE_WIDTH;
    private int INPUT_TENSOR_HEIGHT = INPUT_IMAGE_HEIGHT;
    private static final int INPUT_TENSOR_CHANNELS = 3;

    private int OUTPUT_TENSOR_BATCH = 1;
    private int OUTPUT_TENSOR_WIDTH = INPUT_IMAGE_WIDTH * 2;
    private int OUTPUT_TENSOR_HEIGHT = INPUT_IMAGE_WIDTH * 2;
    private static final int OUTPUT_TENSOR_CHANNELS = 3;

    // the tflite interpreter
    private Interpreter interpreter;
    // buffers to hold the data to feed into the model and return from the model
    private ByteBuffer inputImageData;
    private ByteBuffer outputImageData;
    // arrays of int to hold Colors from the input and output bitmaps
    private int[] inputImagePixels;
    private int[] outputImagePixels;


    public TFLiteBilinearInterpolator(Activity creatingActivity, int batchSize) {
        interpreter = new Interpreter(loadModelFile(creatingActivity));
        setBatchSize(batchSize);
    }

    @Override
    public Bitmap[] processBitmaps(final Bitmap[] inputBitmaps) {
        // do some logging
        /*
        for(final Bitmap bitmap : inputBitmaps)
            Log.i("TFLite processBitmaps", "Width:" + bitmap.getWidth() + " Height:" + bitmap.getHeight());
        */
        // buffer the input bitmaps, run the interpreter, unbuffer them back to bitmaps
        bufferInputBitmaps(inputBitmaps);
        interpreter.run(inputImageData, outputImageData);
        return unbufferOutput();
    }

    // required to free interpreter resources
    public void close() {
        interpreter.close();
    }

    // called from the constructor to set the batch size
    private void setBatchSize(int batchSize) {
        OUTPUT_TENSOR_BATCH = INPUT_TENSOR_BATCH = batchSize;
        allocBuffers();
        int[] newDims = new int[] {INPUT_TENSOR_BATCH, INPUT_TENSOR_HEIGHT, INPUT_TENSOR_WIDTH, INPUT_TENSOR_CHANNELS};
        interpreter.resizeInput(interpreter.getInputIndex("input_image"), newDims);
    }

    // allocate buffers depending on current sizes
    private void allocBuffers() {
        inputImageData = ByteBuffer.allocateDirect(SIZEOF_FLOAT * INPUT_TENSOR_BATCH * INPUT_TENSOR_HEIGHT * INPUT_TENSOR_WIDTH * INPUT_TENSOR_CHANNELS);
        inputImageData.order(ByteOrder.nativeOrder());
        inputImagePixels = new int[INPUT_IMAGE_WIDTH * INPUT_IMAGE_HEIGHT];
        outputImageData = ByteBuffer.allocateDirect(SIZEOF_FLOAT * OUTPUT_TENSOR_BATCH * OUTPUT_TENSOR_HEIGHT * OUTPUT_TENSOR_WIDTH * OUTPUT_TENSOR_CHANNELS);
        outputImageData.order(ByteOrder.nativeOrder());
        outputImagePixels = new int[OUTPUT_TENSOR_WIDTH * OUTPUT_TENSOR_HEIGHT];
    }

    // the tensorflow-for-poets-2 demo uses a MappedByteBuffer, but I believe a simple
    // ByteBuffer would suffice for this model since it is tiny
    private ByteBuffer loadModelFile(Activity creatingActivity) {
        ByteBuffer modelBuffer;
        byte[] buffer = new byte[0];
        try {
            // read the input file into a byte array
            InputStream stream = creatingActivity.getAssets().open(MODEL_PATH);
            int fileSize = stream.available();
            buffer = new byte[fileSize];
            int bytesRead = stream.read(buffer);
            stream.close();
            if (bytesRead != fileSize)
                throw new IOException("loadModelFile: could not read as many bytes as the file contains");
        }
        catch(IOException exception) {
            Log.e("TFLiteBilinearInterpolator", "Failed to read model file", exception);
        }
        // put the bytes into a direct byte stream
        modelBuffer = ByteBuffer.allocateDirect(buffer.length);
        modelBuffer.order(ByteOrder.nativeOrder());
        for(byte b : buffer)
            modelBuffer.put(b);
        return modelBuffer;
    }

    // method to transform an insert the bitmaps into a ByteBuffer for input to the interpreter
    private void bufferInputBitmaps(final Bitmap[] bitmaps) {
        // reset the buffer
        inputImageData.rewind();
        for(final Bitmap bitmap : bitmaps) {
            // read the bitmap into the integer array
            bitmap.getPixels(inputImagePixels, 0, bitmap.getWidth(), 0, 0, INPUT_IMAGE_WIDTH, INPUT_IMAGE_HEIGHT);
            // insert the data into the buffer
            for (int color : inputImagePixels) {
                inputImageData.putFloat(Color.red(color));
                inputImageData.putFloat(Color.green(color));
                inputImageData.putFloat(Color.blue(color));
            }
        }
    }

    // method to transform the output ByteBuffer into a bitmap
    private Bitmap[] unbufferOutput() {
        // create a bitmap array to store output bitmaps in
        Bitmap[] outputBitmaps = new Bitmap [OUTPUT_TENSOR_BATCH];
        // reset the buffer
        outputImageData.rewind();
        // loop through the image pixels: i is the byte counter, j is the pixel counter per patch
        // and k is the patch counter
        int j = 0, k = 0;
        int outputImageSize = outputImagePixels.length;
        int outputDataSize = outputImageSize * OUTPUT_TENSOR_BATCH;
        for(int i = 0; i < outputDataSize; ++i) {
            float r = outputImageData.getFloat();
            float g = outputImageData.getFloat();
            float b = outputImageData.getFloat();
            outputImagePixels[j++] = Color.rgb((int) r, (int) g, (int) b);
            if(j == outputImageSize) { // a full patch has been unbuffered
                // create a bitmap for the patch
                Bitmap bitmap = Bitmap.createBitmap(outputImagePixels, OUTPUT_TENSOR_WIDTH, OUTPUT_TENSOR_HEIGHT, Bitmap.Config.ARGB_8888);
                // add the bitmap to the array
                outputBitmaps[k++] = bitmap;
                // reset the per patch pixel counter
                j = 0;
            }
        }
        outputImageData.rewind(); // re-reset the output to prepare for next use
        return outputBitmaps;
    }
}
