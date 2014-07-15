package org.easetech.schemagenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.easetech.easytest.io.Resource;
import org.easetech.easytest.io.ResourceLoaderStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XSDParser {
    
    private Map<String, String> elementTypeToColumnNameMap = new HashMap<String, String>();
    
    private Map<String, String> elementTypeToColumnTypeMap = new HashMap<String, String>();
    
    private String tableName;

    public void parse(String xsdFileName) throws Exception { 
        try { 
            // parse the document
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            ResourceLoaderStrategy rls = new ResourceLoaderStrategy();
            Resource xsdResource;
            try {
                xsdResource = rls.getResource(xsdFileName.trim());
            } catch (Error e) {
                xsdResource = rls.getResource("xsd/".concat(xsdFileName.trim()));
            }
            
            Document doc = docBuilder.parse (xsdResource.getFile()); 
            NodeList childNodes = doc.getChildNodes();
            getAttributes(childNodes);
            System.out.println(elementTypeToColumnNameMap.toString());
            System.out.println(elementTypeToColumnTypeMap.toString());
            System.out.println(tableName);
//            NodeList list = doc.getElementsByTagName("xsd:element"); 
//
//            //loop to print data
//            for(int i = 0 ; i < list.getLength(); i++)
//            {
//                Element first = (Element)list.item(i);
//                if(first.hasAttributes())
//                {
//                    String nm = first.getAttribute("name"); 
//                    System.out.println(nm); 
//                    String nm1 = first.getAttribute("type"); 
//                    System.out.println(nm1); 
//                    
//                }
//
//                
//            }
        } 
        catch (ParserConfigurationException e) 
        {
            e.printStackTrace();
        }
        catch (SAXException e) 
        { 
            e.printStackTrace();
        }
        catch (IOException ed) 
        {
            ed.printStackTrace();
        }
    }

    private void getAttributes(NodeList nodeList) {
        for(int i = 0 ; i < nodeList.getLength(); i++)
        {
            Node first = nodeList.item(i);
            if(first.hasAttributes())
            {
                NamedNodeMap attrs = first.getAttributes();
                for(int j=0; j< attrs.getLength() ; j++) {
                    String attrName = attrs.item(j).getNodeName();
                    String attrVal = attrs.item(j).getNodeValue();
                    if("name".equals(attrName) && "columnName".equals(attrVal)) {
                        String elementTypeToColumnName = ((Element)first).getAttribute("default");
                        String[] splitValues = elementTypeToColumnName.split("@");
                        elementTypeToColumnNameMap.put(splitValues[0], splitValues[1]);
                        
                    }
                    if("name".equals(attrName) && "columnType".equals(attrVal)) {
                        String elementTypeToColumnType = ((Element)first).getAttribute("default");
                        String[] splitValuesAgain = elementTypeToColumnType.split("@");
                        elementTypeToColumnTypeMap.put(splitValuesAgain[0], splitValuesAgain[1]);
                    }
                    
                    if("name".equals(attrName) && "tableName".equals(attrVal)) {
                        tableName = ((Element)first).getAttribute("default");
                    }
                }

                
            }
            if(first.hasChildNodes()) {
                getAttributes(first.getChildNodes());
            }

            
        }
    }

    /**
     * @return the elementTypeToColumnNameMap
     */
    public Map<String, String> getElementTypeToColumnNameMap() {
        return elementTypeToColumnNameMap;
    }

    /**
     * @return the elementTypeToColumnTypeMap
     */
    public Map<String, String> getElementTypeToColumnTypeMap() {
        return elementTypeToColumnTypeMap;
    }

    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }
    
    
}
