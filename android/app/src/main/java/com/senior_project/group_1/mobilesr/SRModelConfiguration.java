package com.senior_project.group_1.mobilesr;

import java.util.Locale;

public class SRModelConfiguration{

    private String MODEL_NAME;
    private String MODEL_PATH;
    private String INPUT_TENSOR_NAME;
    private boolean MODEL_RESCALES;
    private boolean NNAPI_SETTING;
    private int RESCALING_FACTOR;
    private int INPUT_IMAGE_WIDTH;
    private int INPUT_IMAGE_HEIGHT;
    private int NUM_PARALLEL_BATCH;

    String getModelName() { return MODEL_NAME; }
    String getModelPath(){
        return MODEL_PATH;
    }
    String getInputTensorName(){
        return INPUT_TENSOR_NAME;
    }
    boolean getModelRescales(){
        return MODEL_RESCALES;
    }
    int getRescalingFactor(){
        return RESCALING_FACTOR;
    }
    int getInputImageWidth(){
        return INPUT_IMAGE_WIDTH;
    }
    int getInputImageHeight(){
        return INPUT_IMAGE_HEIGHT;
    }
    int getNumParallelBatch() { return NUM_PARALLEL_BATCH; }
    boolean getNNAPISetting(){
        return NNAPI_SETTING;
    }
    int getOutputTensorWidth(){
        return getInputImageWidth()*getRescalingFactor();
    }
    int getOutputTensorHeight(){
        return getInputImageHeight()*getRescalingFactor();
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
}