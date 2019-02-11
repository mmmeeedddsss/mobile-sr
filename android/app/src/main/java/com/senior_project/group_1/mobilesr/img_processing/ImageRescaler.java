package com.senior_project.group_1.mobilesr.img_processing;

// As we plan on including models that leave the resizing to the application to
// leverage the GPU (by using only conv. layers), we want to be able to execute
// different rescaling strategies depending on the model. This interface is the
// one that will be implemented by different rescaling strategies.
public interface ImageRescaler {
    /**
     * This method is used to rescale an image by a given factor. The arrays are 1D and the output
     * is pre-allocated for efficiency.
     * @param inputPixels: an array holding the input pixels
     * @param inputWidth: an integer providing the width (in pixels) of the image
     * @param outputPixels: a pre-allocated array for holding output pixels
     * @param rescaleFactor: the rescaling factor
     */
    void resizePixels(int[] inputPixels, int inputWidth, int[] outputPixels, int rescaleFactor);
}
