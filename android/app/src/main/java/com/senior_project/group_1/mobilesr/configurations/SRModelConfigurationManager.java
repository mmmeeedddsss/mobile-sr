package com.senior_project.group_1.mobilesr.configurations;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


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
                    case "supports_nnapi":
                        newConfiguration.setNNAPISupported( Boolean.parseBoolean(parser.nextText() ));
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
                    case "remote":
                        newConfiguration.setRemote( Boolean.parseBoolean(parser.nextText()) );
                        break;
                    case "server_ip":
                        newConfiguration.setIPAddress( parser.nextText() );
                        break;
                    case "server_port":
                        newConfiguration.setPort( Integer.parseInt(parser.nextText()) );
                        break;
                    case "default_selection":
                        if( Boolean.parseBoolean( parser.nextText() ) )
                            currentConfiguration = newConfiguration;
                        break;
                }

            } else if( eventType == XmlPullParser.END_TAG && currentTag.equals("configuration")){
                Log.i("SRModelConfigurationManager.processParsing",
                        String.format("Conf with name %s is read", newConfiguration.getModelName() ));
                configurationMap.put(newConfiguration.getModelName(),newConfiguration);
                newConfiguration = new SRModelConfiguration();
            }
            eventType = parser.next();
        }

    }

    public static SRModelConfiguration switchConfiguration(String key){
        currentConfiguration = configurationMap.get(key);
        return currentConfiguration;
    }

    public static SRModelConfiguration getCurrentConfiguration(){
        return currentConfiguration;
    }

    public static void setNNAPI(Boolean use) {
        currentConfiguration.setNNAPISetting(use);
        editXmlFile("use_nnapi", use.toString());
    }

    public static void setBatch(Integer batch) {
        currentConfiguration.setNumParallelBatch(batch);
        editXmlFile("num_parallel_batch", batch.toString());
    }

    public static void setConfiguration(String type, String value) {
        switch (type) {
            case "nnapi":
                currentConfiguration.setNNAPISetting(Boolean.parseBoolean(value));
                editXmlFile("use_nnapi", value);
                break;
            case "batch":
                currentConfiguration.setNumParallelBatch(Integer.parseInt(value));
                editXmlFile("num_parallel_batch", value);
                break;
            default:
                throw new IllegalArgumentException("not supported. cannot change in xml");
        }
    }

    public static String[] getConfigurationMapKeys()
    {
        return configurationMap.keySet().toArray(new String[0]);
    }

    // Operates on the current configuration, updates the value in the given XML tag.
    private static void editXmlFile(String key, String value) {
      try {
        String modelName = currentConfiguration.getModelName();
        File root = android.os.Environment.getExternalStorageDirectory();
        String filePath =
                "file://"+root.getAbsolutePath()+"/"+ApplicationConstants.CONFIGURATION_FILE_NAME;
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(filePath);

        // Get the root element
        NodeList configs = doc.getElementsByTagName("configuration");
        // Find the node
        Node n = null;
        for (int i = 0; i < configs.getLength(); i++) {
          n = configs.item(i);
          if (n.getChildNodes().item(0).getTextContent().equals(modelName))
            break;
        }
        
        NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
          n = nl.item(i);
          if (n.getNodeName().equals(key))
            break;
        }
        // Update the value
        n.setTextContent(value);
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath.substring(7)));
        transformer.transform(source, result);
      } catch (ParserConfigurationException pce) {
        pce.printStackTrace();
      } catch (TransformerException tfe) {
        tfe.printStackTrace();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      } catch (SAXException sae) {
        sae.printStackTrace();
      }
    }
}
