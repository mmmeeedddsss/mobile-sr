package com.senior_project.group_1.mobilesr.img_processing;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.util.Log;

import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Class representing the bilinear interpolator built for TFLite.
public class TFLiteSuperResolver implements BitmapProcessor {

    private final String MODEL_PATH;
    private final String INPUT_TENSOR_NAME;
    // extra for models that do not apply rescaling, should be true if the model does the rescaling itself
    private final boolean MODEL_RESCALES;

    private final int RESCALING_FACTOR;

    private final int INPUT_IMAGE_WIDTH;
    private final int INPUT_IMAGE_HEIGHT;

    private static final int SIZEOF_FLOAT = 4;

    private static final float OFFSET = 1.0f;
    private static final float SCALE = 127.5f;
    private static final float INV_SCALE = (float) (1.0 / 127.5);

    private int inputTensorBatch;
    private int inputTensorWidth;
    private int inputTensorHeight;
    private static final int INPUT_TENSOR_CHANNELS = 3;

    private int outputTensorBatch;
    private final int OUTPUT_TENSOR_WIDTH;
    private final int OUTPUT_TENSOR_HEIGHT;
    private static final int OUTPUT_TENSOR_CHANNELS = 3;

    // the tflite interpreter
    private Interpreter interpreter;
    // buffers to hold the data to feed into the model and return from the model
    private ByteBuffer inputTensorData;
    private ByteBuffer outputTensorData;
    // arrays of int to hold Colors from the input and output bitmaps
    private int[] inputImagePixels;
    private int[] outputImagePixels;

    // new buffer for rescaled images, when using models with no rescaling
    private int[] rescaledImagePixels;
    private ImageRescaler imageRescaler;


    public TFLiteSuperResolver(Activity creatingActivity, int batchSize, SRModelConfiguration srModelConfiguration) {
        MODEL_PATH = srModelConfiguration.getModelPath();
        INPUT_TENSOR_NAME = srModelConfiguration.getInputTensorName();
        MODEL_RESCALES = srModelConfiguration.getModelRescales();
        RESCALING_FACTOR = srModelConfiguration.getRescalingFactor();
        INPUT_IMAGE_WIDTH = srModelConfiguration.getInputImageWidth();
        INPUT_IMAGE_HEIGHT = srModelConfiguration.getInputImageHeight();
        OUTPUT_TENSOR_WIDTH = srModelConfiguration.getOutputTensorWidth();
        OUTPUT_TENSOR_HEIGHT = srModelConfiguration.getOutputTensorHeight();


        Interpreter.Options options = new Interpreter.Options();
        options.setUseNNAPI(srModelConfiguration.getNNAPISetting());
        interpreter = new Interpreter(loadModelFile(creatingActivity), options);
        // the ordering of the calls here are critical!
        allocPixelArrays();
        setupRescaling();
        allocForBatchSize(batchSize);
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
        interpreter.run(inputTensorData, outputTensorData);
        return unbufferOutput();
    }

    // required to free interpreter resources
    public void close() {
        interpreter.close();
    }

    // allocate pixel arrays for holding bitmap pixels
    private void allocPixelArrays() {
        inputImagePixels = new int[INPUT_IMAGE_WIDTH * INPUT_IMAGE_HEIGHT];
        outputImagePixels = new int[OUTPUT_TENSOR_WIDTH * OUTPUT_TENSOR_HEIGHT];
    }

    // sets up external rescaling if required
    private void setupRescaling() {
        if(MODEL_RESCALES) {
            inputTensorHeight = INPUT_IMAGE_HEIGHT;
            inputTensorWidth = INPUT_IMAGE_WIDTH;
            imageRescaler = null;
            rescaledImagePixels = inputImagePixels;
        }
        else {
            // TODO: add some sort of factory if we add new rescalers, also might make rescalers singleton
            inputTensorHeight = OUTPUT_TENSOR_HEIGHT;
            inputTensorWidth = OUTPUT_TENSOR_WIDTH;
            imageRescaler = new NearestNeighborRescaler();
            rescaledImagePixels = new int [inputTensorHeight * inputTensorWidth];
        }
    }

    // allocate bytebuffers that are used as input/output to the model
    private void allocByteBuffers() {
        inputTensorData = ByteBuffer.allocateDirect(SIZEOF_FLOAT * inputTensorBatch * inputTensorHeight * inputTensorWidth * INPUT_TENSOR_CHANNELS);
        inputTensorData.order(ByteOrder.nativeOrder());
        outputTensorData = ByteBuffer.allocateDirect(SIZEOF_FLOAT * outputTensorBatch * OUTPUT_TENSOR_HEIGHT * OUTPUT_TENSOR_WIDTH * OUTPUT_TENSOR_CHANNELS);
        outputTensorData.order(ByteOrder.nativeOrder());
    }

    // called from the constructor to set the batch size
    private void allocForBatchSize(int batchSize) {
        outputTensorBatch = inputTensorBatch = batchSize;
        allocByteBuffers();
        int[] newDims = new int[] {inputTensorBatch, inputTensorHeight, inputTensorWidth, INPUT_TENSOR_CHANNELS};
        interpreter.resizeInput(interpreter.getInputIndex(INPUT_TENSOR_NAME), newDims);
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
            Log.e("TFLiteSuperResolver", "Failed to read model file", exception);
        }
        // put the bytes into a direct byte stream
        modelBuffer = ByteBuffer.allocateDirect(buffer.length);
        modelBuffer.order(ByteOrder.nativeOrder());
        for(byte b : buffer)
            modelBuffer.put(b);
        return modelBuffer;
    }

    // method to convert a float in the range [0, 255] to the [-1, 1] suitable for our models
    private static float full2network(float val) {
        return (val * INV_SCALE) - OFFSET;
    }

    // method to convert an output float in the range [-1, 1] to [0, 1], also performs clamping
    private static float network2full(float val) {
        float clamped = Math.max(-1.0f, Math.min(val, 1.0f)); // clamp to the range limits
        return (clamped + 1.0f) * 0.5f;
    }

    // method to transform an insert the bitmaps into a ByteBuffer for input to the interpreter
    private void bufferInputBitmaps(final Bitmap[] bitmaps) {
        // reset the buffer
        inputTensorData.rewind();
        for(final Bitmap bitmap : bitmaps) {
            if(bitmap == null)
                continue;
            // read the bitmap into the integer array
            bitmap.getPixels(inputImagePixels, 0, bitmap.getWidth(), 0, 0, INPUT_IMAGE_WIDTH, INPUT_IMAGE_HEIGHT);
            // rescale the bitmap if necessary, else the two arrays are the same
            if(imageRescaler != null)
                imageRescaler.resizePixels(inputImagePixels, INPUT_IMAGE_WIDTH, rescaledImagePixels, RESCALING_FACTOR);
            // insert the data into the buffer
            for (int color : rescaledImagePixels) {
                float red = full2network(Color.red(color)); // from [0, 255] to [-1, 1]
                float green = full2network(Color.green(color));
                float blue = full2network(Color.blue(color));
                inputTensorData.putFloat(red);
                inputTensorData.putFloat(green);
                inputTensorData.putFloat(blue);
            }
        }
    }

    // method to transform the output ByteBuffer into a bitmap
    private Bitmap[] unbufferOutput() {
        // create a bitmap array to store output bitmaps in
        Bitmap[] outputBitmaps = new Bitmap [outputTensorBatch];
        // reset the buffer
        outputTensorData.rewind();
        // loop through the image pixels: i is the byte counter, j is the pixel counter per patch
        // and k is the patch counter
        int j = 0, k = 0;
        int outputImageSize = outputImagePixels.length;
        int outputDataSize = outputImageSize * outputTensorBatch;
        for(int i = 0; i < outputDataSize; ++i) {
            float r = network2full(outputTensorData.getFloat()); // range [0, 1]
            float g = network2full(outputTensorData.getFloat());
            float b = network2full(outputTensorData.getFloat());
            outputImagePixels[j++] = Color.rgb(r, g, b);
            if(j == outputImageSize) { // a full patch has been unbuffered
                // create a bitmap for the patch
                Bitmap bitmap = Bitmap.createBitmap(outputImagePixels, OUTPUT_TENSOR_WIDTH, OUTPUT_TENSOR_HEIGHT, Bitmap.Config.ARGB_8888);
                // add the bitmap to the array
                outputBitmaps[k++] = bitmap;
                // reset the per patch pixel counter
                j = 0;
            }
        }
        outputTensorData.rewind(); // re-reset the output to prepare for next use
        return outputBitmaps;
    }
}
