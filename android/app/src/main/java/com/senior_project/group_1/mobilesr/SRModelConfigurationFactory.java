package com.senior_project.group_1.mobilesr;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class SRModelConfigurationFactory{
    private SRModelConfigurationFactory() {
    }

    private static Map<String, SRModelConfiguration> configurationMap;

    public static void initilizeConfigurations( InputStream inputStream ){
        XmlPullParserFactory parserFactory;
        try {
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);

            processParsing(parser);

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
            if( eventType == XmlPullParser.START_TAG ) // like <configuration>
            {
                String currentTag = parser.getName().toLowerCase();
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

            } else if( eventType == XmlPullParser.END_TAG){
                configurationMap.put(currentConfiguration.getInputTensorName(),currentConfiguration);
                currentConfiguration = new SRModelConfiguration();
            }
            eventType = parser.next();
        }

    }

    public static SRModelConfiguration getConfiguration( String key ){
        return configurationMap.get(key);
    }


}