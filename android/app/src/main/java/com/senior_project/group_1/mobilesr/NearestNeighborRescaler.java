package com.senior_project.group_1.mobilesr;

public class NearestNeighborRescaler implements ImageRescaler {
    public void resizePixels(int[] inputPixels, int inputWidth, int[] outputPixels, int rescaleFactor) {
        // fill a row for each input row, leaving the rest empty
        int outputWidth = inputWidth * rescaleFactor;
        int blockWidth = outputWidth * rescaleFactor;
        int k = 0;
        for (int i = 0; i < inputPixels.length; i += inputWidth) { // for each row
            for (int j = i, lim = i + inputWidth; j < lim; ++j) { // for each pixel
                for (int c = 0; c < rescaleFactor; ++c) // copy it rescaleFactor times
                    outputPixels[k++] = inputPixels[j];
            }
            k += blockWidth - outputWidth; // skip the next block in the output
        }
        // now, copy each existing row using system.arraycopy for efficiency
        for (int i = 0; i < outputPixels.length; i += blockWidth) { // for each row
            // copy it to the following empty rows
            for (int j = i + outputWidth, lim = i + blockWidth; j < lim; j += outputWidth)
                System.arraycopy(outputPixels, i, outputPixels, j, outputWidth);
        }
    }
}
