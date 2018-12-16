package com.senior_project.group_1.mobilesr;

public abstract class SRModelConfiguration{
    abstract String getModelPath();
    abstract String getInputTensorName();
    abstract boolean getModelRescales();
    abstract int getRescalingFactor();
    abstract int getInputImageWidth();
    abstract int getInputImageHeight();
    abstract boolean getNNAPISetting();

    int getOutputTensorWidth()
    {
        return getInputImageWidth()*getRescalingFactor();
    }

    int getOutputTensorHeight()
    {
        return getInputImageHeight()*getRescalingFactor();
    }
}