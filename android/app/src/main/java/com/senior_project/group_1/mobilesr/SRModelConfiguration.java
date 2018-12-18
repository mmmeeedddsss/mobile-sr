package com.senior_project.group_1.mobilesr;

public class SRModelConfiguration{

    private String MODEL_PATH;
    private String INPUT_TENSOR_NAME;
    private boolean MODEL_RESCALES;
    private boolean NNAPI_SETTING;
    private int RESCALING_FACTOR;
    private int INPUT_IMAGE_WIDTH;
    private int INPUT_IMAGE_HEIGHT;

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
    boolean getNNAPISetting(){
        return NNAPI_SETTING;
    }
    int getOutputTensorWidth(){
        return getInputImageWidth()*getRescalingFactor();
    }
    int getOutputTensorHeight(){
        return getInputImageHeight()*getRescalingFactor();
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
}