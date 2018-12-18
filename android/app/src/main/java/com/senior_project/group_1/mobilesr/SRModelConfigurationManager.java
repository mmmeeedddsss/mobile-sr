package com.senior_project.group_1.mobilesr;

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

    public static void initilizeConfigurations( InputStream inputStream, String initialModelName ){
        XmlPullParserFactory parserFactory;
        configurationMap = new HashMap<>();
        try {
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);

            processParsing(parser);
            currentConfiguration = configurationMap.get(initialModelName);

        } catch (XmlPullParserException e) {
            Log.i("ConfigInit", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processParsing(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        SRModelConfiguration currentConfiguration = new SRModelConfiguration();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String currentTag = parser.getName();
            if( eventType == XmlPullParser.START_TAG ) // like <configuration>
            {
                // Fun fact : I learned that java uses .equals when strings are used in switch statements
                switch (currentTag)
                {
                    case "model_path":
                        currentConfiguration.setModelPath( parser.nextText() );
                        break;
                    case "input_tensor_name":
                        currentConfiguration.setInputTensorName( parser.nextText() );
                        break;
                    case "model_rescales":
                        currentConfiguration.setModelRescales( Boolean.parseBoolean(parser.nextText()) );
                        break;
                    case "use_nnapi":
                        currentConfiguration.setNNAPISetting( Boolean.parseBoolean(parser.nextText()) );
                        break;
                    case "rescaling_factor":
                        currentConfiguration.setRescalingFactor( Integer.parseInt(parser.nextText()) );
                        break;
                    case "input_image_width":
                        currentConfiguration.setInputImageWidth( Integer.parseInt(parser.nextText()) );
                        break;
                    case "input_image_height":
                        currentConfiguration.setInputImageHeight( Integer.parseInt(parser.nextText()) );
                        break;
                }

            } else if( eventType == XmlPullParser.END_TAG && currentTag.equals("configuration")){
                configurationMap.put(currentConfiguration.getInputTensorName(),currentConfiguration);
                currentConfiguration = new SRModelConfiguration();
            }
            eventType = parser.next();
        }

    }

    public static SRModelConfiguration getConfiguration( String key ){
        currentConfiguration = configurationMap.get(key);
        return configurationMap.get(key);
    }

    public static SRModelConfiguration getCurrentConfiguration(){
        return currentConfiguration;
    }
}