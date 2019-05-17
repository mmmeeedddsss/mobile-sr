package com.senior_project.group_1.mobilesr.configurations;

import android.renderscript.RSInvalidStateException;

import java.util.Locale;

public class SRModelConfiguration{

    // Edit by Deniz: I added an 'NNAPI_SUPPORTED' entry, because it might not always be
    // depending on the model, which was my original intent. In that case, we should not
    // be able to enable the GPU.

    private String MODEL_NAME;
    private String MODEL_PATH;
    private String INPUT_TENSOR_NAME;
    private boolean MODEL_RESCALES;
    private boolean NNAPI_SETTING;
    private boolean NNAPI_SUPPORTED;
    private int RESCALING_FACTOR;
    private int INPUT_IMAGE_WIDTH;
    private int INPUT_IMAGE_HEIGHT;
    private int NUM_PARALLEL_BATCH;
    private boolean REMOTE;
    private String IPAddress;
    private int Port = 0;

    public String getModelName() { return MODEL_NAME; }
    public String getModelPath(){
        return MODEL_PATH;
    }
    public String getInputTensorName(){
        return INPUT_TENSOR_NAME;
    }
    public boolean getModelRescales(){
        return MODEL_RESCALES;
    }
    public int getRescalingFactor(){
        return RESCALING_FACTOR;
    }
    public int getInputImageWidth(){
        return INPUT_IMAGE_WIDTH;
    }
    public int getInputImageHeight(){
        return INPUT_IMAGE_HEIGHT;
    }
    public int getNumParallelBatch() { return NUM_PARALLEL_BATCH; }
    public boolean getNNAPISetting(){
        return NNAPI_SETTING;
    }
    public int getOutputTensorWidth(){
        return getInputImageWidth()*getRescalingFactor();
    }
    public int getOutputTensorHeight(){
        return getInputImageHeight()*getRescalingFactor();
    }

    public boolean supportsNNAPI() {
        return NNAPI_SUPPORTED;
    }

    public void setNNAPISupported(boolean NNAPISupported) {
        this.NNAPI_SUPPORTED = NNAPISupported;
        this.NNAPI_SETTING = true;
    }

    public void setModelName(String MODEL_NAME) {
        this.MODEL_NAME = MODEL_NAME;
    }

    public void setModelPath(String MODEL_PATH) {
        this.MODEL_PATH = MODEL_PATH;
    }

    public void setInputTensorName(String INPUT_TENSOR_NAME) {
        this.INPUT_TENSOR_NAME = INPUT_TENSOR_NAME;
    }

    public void setModelRescales(boolean MODEL_RESCALES) {
        this.MODEL_RESCALES = MODEL_RESCALES;
    }

    public void setNumParallelBatch(int numParellelBatch) {
        this.NUM_PARALLEL_BATCH = numParellelBatch;
    }

    public void setNNAPISetting(boolean NNAPISetting) {
        if(!NNAPI_SUPPORTED && NNAPISetting)
            throw new IllegalStateException("Cannot set NNAPI setting on a model that does not support NNAPI.");
        this.NNAPI_SETTING = NNAPISetting;
    }

    public void setRescalingFactor(int RESCALING_FACTOR) {
        this.RESCALING_FACTOR = RESCALING_FACTOR;
    }

    public void setInputImageWidth(int INPUT_IMAGE_WIDTH) {
        this.INPUT_IMAGE_WIDTH = INPUT_IMAGE_WIDTH;
    }

    public void setInputImageHeight(int INPUT_IMAGE_HEIGHT) {
        this.INPUT_IMAGE_HEIGHT = INPUT_IMAGE_HEIGHT;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Model: %s, parellel_batch: %d",getModelName(), getNumParallelBatch());
    }

    public boolean isRemote() {
        return REMOTE;
    }

    public void setRemote(boolean remote) {
        this.REMOTE = remote;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public void setIPAddress(String IPAddress) {
        this.IPAddress = IPAddress;
    }

    public int getPort() {
        return Port;
    }

    public void setPort(int port) {
        Port = port;
    }
}