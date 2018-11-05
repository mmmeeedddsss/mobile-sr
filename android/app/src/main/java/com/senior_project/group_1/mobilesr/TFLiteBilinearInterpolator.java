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
    private static final String MODEL_PATH = "bilinear512_conv.tflite";

    private static final int SIZEOF_FLOAT = 4;

    private int INPUT_IMAGE_WIDTH = 512;
    private int INPUT_IMAGE_HEIGHT = 512;

    private static final int INPUT_TENSOR_BATCH = 1;
    private int INPUT_TENSOR_WIDTH = 1024;
    private int INPUT_TENSOR_HEIGHT = 1024;
    private static final int INPUT_TENSOR_CHANNELS = 3;

    private static final int OUTPUT_TENSOR_BATCH = 1;
    private int OUTPUT_TENSOR_WIDTH = 1024;
    private int OUTPUT_TENSOR_HEIGHT = 1024;
    private static final int OUTPUT_TENSOR_CHANNELS = 3;

    // the tflite interpreter
    private Interpreter interpreter;
    // buffers to hold the data to feed into the model and return from the model
    private ByteBuffer inputImageData;
    private ByteBuffer outputImageData;
    // arrays of int to hold Colors from the input and output bitmaps
    private int[] inputImagePixels;
    private int[] outputImagePixels;


    public TFLiteBilinearInterpolator(Activity creatingActivity) {
        interpreter = new Interpreter(loadModelFile(creatingActivity));
        interpreter.setUseNNAPI(true);
        reallocBuffers();
    }

    @Override
    public Bitmap processBitmap(final Bitmap inputBitmap) {
        // do some logging
        Log.i("TFLite processBitmap", "Width:" + inputBitmap.getWidth() + " Height:" + inputBitmap.getHeight());

        // Experimental resizing trial, does not work
        // If I understand correctly, we do resize the input but this does not propagate to the rest of the tensors,
        // and there is currently no other method provided by Interpreter to handle this stuff
        /*
        // huge waste of time here, for reallocations
        // first, set sizes depending on the input bitmap
        setSizes(inputBitmap);
        // reallocate everything depending on these sizes (might find a way to circumvent this in the future)
        reallocBuffers();
        // reallocate the input tensor
        int[] newDims = new int[] {INPUT_TENSOR_BATCH, INPUT_TENSOR_HEIGHT, INPUT_TENSOR_WIDTH, INPUT_TENSOR_CHANNELS};
        interpreter.resizeInput(interpreter.getInputIndex("input_image"), newDims);
        */

        // the usual operation, same with constant input size
        bufferInputBitmap(inputBitmap);
        interpreter.run(inputImageData, outputImageData);
        return unbufferOutput();
    }

    // required to free interpreter resources
    public void close() {
        interpreter.close();
    }

    // resizing trial: set parameters
    private void setSizes(final Bitmap inputBitmap) {
        INPUT_TENSOR_HEIGHT = inputBitmap.getHeight();
        INPUT_TENSOR_WIDTH = inputBitmap.getWidth();
        OUTPUT_TENSOR_HEIGHT = 2 * INPUT_TENSOR_HEIGHT;
        OUTPUT_TENSOR_WIDTH = 2 * INPUT_TENSOR_WIDTH;
    }

    // reallocate buffers depending on current sizes
    private void reallocBuffers() {
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

    private static void putFloats(ByteBuffer byteBuffer, float value, int n) {
        for(int i = 0; i < n; ++i)
            byteBuffer.putFloat(value);
    }

    // method to transform an insert the bitmap into a ByteBuffer for input to the interpreter
    private void bufferInputBitmap(final Bitmap bitmap) {
        // get the top-left 32x32 patch
        final Bitmap subsetBitmap = Bitmap.createBitmap(bitmap, 0, 0, INPUT_IMAGE_WIDTH, INPUT_IMAGE_HEIGHT);
        // read the bitmap into the integer array
        subsetBitmap.getPixels(inputImagePixels, 0, subsetBitmap.getWidth(), 0, 0, INPUT_IMAGE_WIDTH, INPUT_IMAGE_HEIGHT);
        // reset the buffer
        inputImageData.rewind();
        // insert the data into the buffer
        // this is the old, proper style
        /*
        for(int color : inputImagePixels) {
            inputImageData.putFloat(Color.red(color));
            inputImageData.putFloat(Color.green(color));
            inputImageData.putFloat(Color.blue(color));
        }
        */
        // now, we have to add zero padding since we use conv. for interpolation
        for(int i = 0; i < INPUT_IMAGE_HEIGHT; ++i) {
            for(int j = 0; j < INPUT_IMAGE_WIDTH; ++j) {
                // insert a pixel
                int color = inputImagePixels[i*INPUT_IMAGE_WIDTH+j];
                inputImageData.putFloat(Color.red(color));
                inputImageData.putFloat(Color.green(color));
                inputImageData.putFloat(Color.blue(color));
                // insert a zero pixel
                putFloats(inputImageData, 0.0f, 3);
            }
            // insert a full zero row
            for(int j = 0; j < INPUT_IMAGE_WIDTH; ++j)
                putFloats(inputImageData, 0.0f, 6);
        }
    }

    // method to transform the output ByteBuffer into a bitmap
    private Bitmap unbufferOutput() {
        outputImageData.rewind();
        for(int i = 0; i < outputImagePixels.length; ++i) {
            float r = outputImageData.getFloat();
            float g = outputImageData.getFloat();
            float b = outputImageData.getFloat();
            outputImagePixels[i] = Color.rgb((int) r, (int) g, (int) b);
        }
        outputImageData.rewind(); // re-reset the output to prepare for next use
        return Bitmap.createBitmap(outputImagePixels, OUTPUT_TENSOR_WIDTH, OUTPUT_TENSOR_HEIGHT, Bitmap.Config.ARGB_8888);
    }
}
