package com.senior_project.group_1.mobilesr;

/*
 * Adding a new configuration:
 * 1 - Add a new class as an inner class of SRModelConfigurationFactory
 *      which extends SRModelConfiguration
 * 2 - Implements required methods
 * 3 - Create a public static final String as the identifier of the configuration
 * 4 - Implement singleton classes, as in one of the other configurations( rename required parts )
 * 5 - Create a new case statement for the SRModelConfigurationFactory.getConfiguration() method
 */


public class SRModelConfigurationFactory{
    private SRModelConfigurationFactory() {
    }

    public static SRModelConfiguration getConfiguration( String model_type ){
        switch (model_type){
            case SRModelConfigurationFactory.BASIC_SRNN:
                return SRModelConfiguration_BASIC_SRCNN.getInstance();
        }
        return null;
    }


    // CONFIGURATIONS

    public static final String BASIC_SRNN = "BACIS_SRNN";
    static class SRModelConfiguration_BASIC_SRCNN extends SRModelConfiguration {
        // Singleton pattern
        private static SRModelConfiguration instance = null;

        public static SRModelConfiguration getInstance()
        {
            if (instance == null)
                instance = new SRModelConfiguration_BASIC_SRCNN(); // Override when creating a new config

            return instance;
        }

        private SRModelConfiguration_BASIC_SRCNN(){} // Override when creating a new config

        // Configurations
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
        boolean getNNAPISetting() { return false; }
    }
}