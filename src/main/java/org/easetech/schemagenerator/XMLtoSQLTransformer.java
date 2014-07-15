package org.easetech.schemagenerator;

import java.io.FileOutputStream;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.easetech.easytest.io.Resource;
import org.easetech.easytest.io.ResourceLoaderStrategy;

/**
 * Class that can be used to generate a SQL file with INSERT statements 
 * from an XML file that conforms to the XSD generated using {@link JPAToXSD} class
 *
 */
public class XMLtoSQLTransformer {
    
    private static Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{.*\\}");

    private final Resource xsltFile;
    
    private static final String LINE_SEPERATOR = System.getProperty("line.separator");
    
    private Map<String , List<String>> loadedRecords = new HashMap<String, List<String>>();
    
    private static final String SUPPLIED_RECORD_IDS = "suppliedRecordIds";
    
    private static final String UTF_8 = "UTF-8"; 
    
    private static final String COMMA = ",";
    
    private static final String EQUAL = "=";
    
    private static final String AMPERSAND = "&";
    
    private static final String FOREIGN_KEY_CONSTRAINT_0 = "SET foreign_key_checks = 0";
    private static final String FOREIGN_KEY_CONSTRAINT_1 = "SET foreign_key_checks = 1";
    
    private final List<String> STRING_TYPE = new ArrayList<String>();
    
    public XMLtoSQLTransformer() {
        ResourceLoaderStrategy rls = new ResourceLoaderStrategy();
        xsltFile = rls.getResource("classpath:sqlConverter.xslt");
        STRING_TYPE.add("xsd:string");
        STRING_TYPE.add("xsd:anyURI");
        STRING_TYPE.add("xsd:dateTime");
        STRING_TYPE.add("xsd:enum");
    }
    
    public static void main(String[] args) throws Exception {
//        File savedFile = new File();
        XMLtoSQLTransformer transformer = new XMLtoSQLTransformer();
        String filePath = "/Users/anuj/easetech/data-manager/src/main/resources/InsertStatements.sql";
        transformer.transform("Cost.xml" , filePath);
    }

    public void transform(InputStream xmlFile , String xmlFilePath, OutputStream sqlFile) {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(sqlFile, UTF_8));
            writer.write(FOREIGN_KEY_CONSTRAINT_0);
            writer.write(LINE_SEPERATOR);
            transform(xmlFile, xmlFilePath, writer);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            try {
                if (writer != null)
                    writer.write(FOREIGN_KEY_CONSTRAINT_1);
                    writer.close();
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        }
    }
    /**
     * Transform an XML file to a SQL file containing INSERT statements and save it at the specified path
     * @param xmlFilePath the xml file from which to generate INSERT statements
     * @param path the path where to save the file
     * @throws Exception 
     */
    public void transform(String xmlFilePath , String path) throws Exception {
        ResourceLoaderStrategy rls = new ResourceLoaderStrategy();
        Resource xmlResource = rls.getResource(xmlFilePath);
        InputStream xmlInputStream = null;
        if(xmlResource.exists()) {
            xmlInputStream = xmlResource.getInputStream();
        }
        
        OutputStream sqlOutputStream = null;
        
        try {
            Resource sqlResource = rls.getResource(path);
            if(sqlResource.exists()) {
                sqlOutputStream = sqlResource.getOutputStream();
            }
        } catch (Error e) {
            //do nothing
        }
        if(sqlOutputStream == null) {
            File sqlFile = new File(path);
            if (!sqlFile.exists()) {
                sqlFile.createNewFile();               
            }
            sqlOutputStream = new FileOutputStream(sqlFile);
        }
        
        transform(xmlInputStream, xmlFilePath, sqlOutputStream);
        
    }
    
    /**
     * 
     * @param xmlFile
     * @param insertWriter
     * @param recordIds
     * @throws IOException
     */
    public void transform(InputStream xmlFile , String xmlFilePath, Writer insertWriter , String... recordIds) throws IOException {
        
        Map<String , List<String>> filePathToRecordIdsMap = new HashMap<String , List<String>>();
        StringBuilder insertStatements = new StringBuilder();        
        String resultStr = getTransformedValues(xmlFile, xmlFilePath, recordIds);
        String[] splitValues = resultStr.split(LINE_SEPERATOR);
        String xmlSchemaLocation = null;
        for(String line : splitValues) {
            if(line.startsWith("urn:org:easetech:easytest:schema")) {
                xmlSchemaLocation = loadSchema(line);
                
                //System.out.println(xmlSchema.getElements().getNames().next());
                
            }
            if(line.startsWith("INSERT")) {
                line = normailzeInsertStatement(line, xmlSchemaLocation);
                insertStatements.append(line).append(LINE_SEPERATOR);
            } else if(line.startsWith("FILE")){  
                handleRelationship(line, filePathToRecordIdsMap);
            } else {
                handleLoadedRecords(xmlFilePath, line);
            }
        }
        if(!insertStatements.toString().isEmpty()) {
            insertWriter.write(insertStatements.toString());
        }
        if(!filePathToRecordIdsMap.isEmpty()) {
            for(Entry<String, List<String>> entry : filePathToRecordIdsMap.entrySet()) {
                ResourceLoaderStrategy rls = new ResourceLoaderStrategy();
                Resource resource = rls.getResource(entry.getKey());
                if(resource.exists()) {
                    try {
                        transform(resource.getInputStream(), entry.getKey(), insertWriter, entry.getValue().toArray(new String[entry.getValue().size()]));
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                
            }
        }
        
    }
    
    private String loadSchema(String schemaLocation) {
        //XmlSchema schema = null;
         return schemaLocation.replace("urn:org:easetech:easytest:schema ", "");
//         XSModel xsModel = new XSParser().parse(schemaName.trim());
//         XSInstance xsInstance = new XSInstance();
//         xsInstance.minimumElementsGenerated = 2;
         
//         ResourceLoaderStrategy rls = new ResourceLoaderStrategy();
//         Resource resource = rls.getResource(schemaName.trim());
//         if(resource.exists()) {
//             try {
//                InputStream is = resource.getInputStream();
//                XmlSchemaCollection schemaCol = new XmlSchemaCollection();
//                schema = schemaCol.read(new StreamSource(is), null);
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//         }
//         return schema;
    }
    
    private String normailzeInsertStatement(String insertQuery , String xmlSchemaLocation) {
        XSDParser parser = new XSDParser();
        String columnNames, columnValues;
        try {
            parser.parse(xmlSchemaLocation);
            insertQuery = insertQuery.replace("{table_name}", parser.getTableName());
            insertQuery = insertQuery.replace(", )", " )");
            String queryWithoutInsert = insertQuery.replace("INSERT INTO ".concat(parser.getTableName()), "");
            int beginIndex = queryWithoutInsert.indexOf(")");
            columnNames = queryWithoutInsert.substring(0, beginIndex).replace("(", "").replace(")", "");
            columnValues = queryWithoutInsert.substring(beginIndex).replace("values ", "").replace("(", "").replace(")", "");
            String[] splitColumnNames = columnNames.split(",");
            String[] splitColumnValues = columnValues.split(",");
            
            for(int index = 0 ; index < splitColumnNames.length ; index++) {
                String columnName = splitColumnNames[index].trim();
                String columnValue = splitColumnValues[index].trim();
                Map<String, String> elementTypeToColumnNameMap = parser.getElementTypeToColumnNameMap();
                Map<String, String> elementTypeToColumnTypeMap = parser.getElementTypeToColumnTypeMap();
                
                String elementType = columnName.replace("{", "").replace("}", "");
                String actualColumnName = elementTypeToColumnNameMap.get(elementType.trim());
                insertQuery = insertQuery.replace(columnName, actualColumnName);
                String valueType = elementTypeToColumnTypeMap.get(elementType); 
                String finalColumnValue = columnValue;
                if(STRING_TYPE.contains(valueType)) {
                    finalColumnValue = "'".concat(columnValue).concat("'");
                }
                insertQuery = insertQuery.replace(columnValue, finalColumnValue);
            }
            System.out.println("insertQuery="+insertQuery);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
       return insertQuery;
    }
    private void handleLoadedRecords(String absolutePath, String line) {
        String key = absolutePath;
        String value = getLoadedRecord(line);
        if(loadedRecords.containsKey(key)) {
           List<String> values = loadedRecords.get(key); 
           values.add(value);
        } else {
            List<String> values = new ArrayList<String>();
            values.add(value);
            loadedRecords.put(key, values);
        }
    }
    
    private String getLoadedRecord(String line) {
        String result = null;
        String[] loadedValues = line.split(EQUAL);
        if(loadedValues.length > 1) {
            result = loadedValues[1];
        }
        return result;
    }
    private void handleRelationship(String line , Map<String , List<String>> filePathToRecordIdsMap) {
        String filePath = getFilePath(line);
        String recordId = getRecordId(line);
        if(filePath != null && filePathToRecordIdsMap.containsKey(filePath)) {
            List<String> rcrdIds = filePathToRecordIdsMap.get(filePath);
            rcrdIds.add(recordId);
        } else {
            List<String> rcrdIds = new ArrayList<String>();
            rcrdIds.add(recordId);
            filePathToRecordIdsMap.put(filePath, rcrdIds);
        } 
    }
    
    private String getFilePath(String line) {
        String result = null;
        String [] pathAndIdValues = line.split(AMPERSAND);
        if(pathAndIdValues.length > 1) {
            String[] filePathWithValue = pathAndIdValues[0].split(EQUAL);
            if (filePathWithValue.length > 1) {
                result = filePathWithValue[1];
            }
        }
        return result;
    }
    
    private String getRecordId(String line) {
        String result = null;
        String [] pathAndIdValues = line.split(AMPERSAND);
        if(pathAndIdValues.length > 1) {
            String[] recordIdWithValue = pathAndIdValues[1].split(EQUAL);
            if (recordIdWithValue.length > 1) {
                result = recordIdWithValue[1];
            }
        }
        return result;
    }
    
    private String getTransformedValues(InputStream xmlFile , String absolutePath, String... recordIds) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        String resultStr = null;
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            StreamSource xsltSource = new StreamSource(xsltFile.getInputStream());
            StreamResult resultStream = new StreamResult(result);
            Transformer transformer = tFactory.newTransformer(xsltSource);
            StreamSource xmlDataSource = new StreamSource(xmlFile);
            if(recordIds != null && recordIds.length > 0) {
                StringBuilder recordIdBuilder = new StringBuilder();
                for(String recordId : recordIds) {
                    if(loadedRecords.containsKey(absolutePath)) {
                        List<String> values = loadedRecords.get(absolutePath);
                        if(!values.contains(recordId)) {
                            recordIdBuilder.append(recordId).append(COMMA);
                        }
                    } else {
                        recordIdBuilder.append(recordId).append(COMMA);
                    }
                   
                }
              transformer.setParameter(SUPPLIED_RECORD_IDS, recordIdBuilder.toString());
            }
            transformer.transform(xmlDataSource, resultStream);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] resultByteArray = result.toByteArray();
        try {
            resultStr = new String(resultByteArray , UTF_8);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return resultStr;
    }
}
