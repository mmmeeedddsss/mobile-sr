package com.senior_project.group_1.mobilesr;


public class SRModelConfigurationFactory{
    private SRModelConfigurationFactory() {
    }

    public static SRModelConfiguration getConfiguration( String model_type ){
        switch (model_type){
            case SRModelConfigurationFactory.BASIC_SRNN:
                return new SRModelConfiguration_BASIC_SRCNN();
        }
        return null;
    }


    // CONFIGURATIONS

    public static final String BASIC_SRNN = "BACIS_SRNN";
    static class SRModelConfiguration_BASIC_SRCNN extends SRModelConfiguration {

        @Override
        String getModelPath() { return "basic_srcnn_nearestn_noresize_64.tflite"; }

        @Override
        String getInputTensorName() { return "resized_image"; }

        @Override
        boolean getModelRescales() { return false; }

        @Override
        int getRescalingFactor() { return 2; }

        @Override
        int getInputImageWidth() { return 64; }

        @Override
        int getInputImageHeight() { return 64; }

        @Override
        boolean getNNAPISetting() { return true; }
    }
}