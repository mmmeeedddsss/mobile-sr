package com.senior_project.group_1.mobilesr.configurations;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SRModelConfigurationManager {
    private SRModelConfigurationManager() {
    }

    private static Map<String, SRModelConfiguration> configurationMap;
    private static SRModelConfiguration currentConfiguration;

    public static void initilizeConfigurations( InputStream inputStream){
        XmlPullParserFactory parserFactory;
        configurationMap = new HashMap<>();
        try {
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);

            processParsing(parser);

            Log.i("SRModelConfigurationManager","Default conf:"+currentConfiguration.toString());

        } catch (XmlPullParserException e) {
            Log.i("ConfigInit", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processParsing(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        SRModelConfiguration newConfiguration = new SRModelConfiguration();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String currentTag = parser.getName();
            if( eventType == XmlPullParser.START_TAG ) // like <configuration>
            {
                // Fun fact : I learned that java uses .equals when strings are used in switch statements
                switch (currentTag)
                {
                    case "model_name":
                        newConfiguration.setModelName( parser.nextText() );
                        break;
                    case "model_path":
                        newConfiguration.setModelPath( parser.nextText() );
                        break;
                    case "input_tensor_name":
                        newConfiguration.setInputTensorName( parser.nextText() );
                        break;
                    case "model_rescales":
                        newConfiguration.setModelRescales( Boolean.parseBoolean(parser.nextText()) );
                        break;
                    case "use_nnapi":
                        newConfiguration.setNNAPISetting( Boolean.parseBoolean(parser.nextText()) );
                        break;
                    case "rescaling_factor":
                        newConfiguration.setRescalingFactor( Integer.parseInt(parser.nextText()) );
                        break;
                    case "input_image_width":
                        newConfiguration.setInputImageWidth( Integer.parseInt(parser.nextText()) );
                        break;
                    case "input_image_height":
                        newConfiguration.setInputImageHeight( Integer.parseInt(parser.nextText()) );
                        break;
                    case "num_parallel_batch":
                        newConfiguration.setNumParallelBatch(Integer.parseInt(parser.nextText()));
                        break;
                    case "default_selection":
                        if( Boolean.parseBoolean( parser.nextText() ) )
                            currentConfiguration = newConfiguration;
                        break;
                }

            } else if( eventType == XmlPullParser.END_TAG && currentTag.equals("configuration")){
                configurationMap.put(newConfiguration.getModelName(),newConfiguration);
                newConfiguration = new SRModelConfiguration();
            }
            eventType = parser.next();
        }

    }

    public static SRModelConfiguration getConfiguration( String key ){
        currentConfiguration = configurationMap.get(key);
        return currentConfiguration;
    }

    public static SRModelConfiguration getCurrentConfiguration(){
        return currentConfiguration;
    }

    public static String[] getConfigurationMapKeys()
    {
        return configurationMap.keySet().toArray(new String[0]);
    }

}
