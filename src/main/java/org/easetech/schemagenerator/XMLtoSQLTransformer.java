package org.easetech.schemagenerator;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Class that can be used to generate a SQL file with INSERT statements 
 * from an XML file that conforms to the XSD generated using {@link JPAToXSD} class
 *
 */
public class XMLtoSQLTransformer {
    
    private final File xsltFile = new File("src/main/resources/xslt/sqlConverter.xslt");
    
    private static final String LINE_SEPERATOR = System.getProperty("line.separator");
    
    private Map<String , List<String>> loadedRecords = new HashMap<String, List<String>>();
    
    private static final String SUPPLIED_RECORD_IDS = "suppliedRecordIds";
    
    private static final String UTF_8 = "UTF-8"; 
    
    private static final String COMMA = ",";
    
    private static final String EQUAL = "=";
    
    private static final String AMPERSAND = "&";
    
    private static final String FOREIGN_KEY_CONSTRAINT_0 = "SET foreign_key_checks = 0";
    private static final String FOREIGN_KEY_CONSTRAINT_1 = "SET foreign_key_checks = 1";
    
//    public static void main(String[] args) {
//        File savedFile = new File("/Users/anuj/easetech/schema-generator/src/main/resources/ItemRecord.xml");
//        XMLtoSQLTransformer transformer = new XMLtoSQLTransformer();
//        String filePath = "/Users/anuj/easetech/schema-generator/src/main/resources/InsertStatements.sql";
//        transformer.transform(savedFile , filePath);
//    }

    /**
     * Transform an XML file to a SQL file containing INSERT statements and save it at the specified path
     * @param xmlFile the xml file from which to generate INSERT statements
     * @param path the path where to save the file
     * @throws IOException 
     */
    public void transform(File xmlFile , String path) throws IOException {
        File sqlFile = new File(path);
        if (!sqlFile.exists()) {
            sqlFile.createNewFile();
            
        }
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sqlFile), UTF_8));
            writer.write(FOREIGN_KEY_CONSTRAINT_0);
            writer.write(LINE_SEPERATOR);
            transform(xmlFile, writer);
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
     * 
     * @param xmlFile
     * @param insertWriter
     * @param recordIds
     * @throws IOException
     */
    public void transform(File xmlFile , Writer insertWriter , String... recordIds) throws IOException {
        
        Map<String , List<String>> filePathToRecordIdsMap = new HashMap<String , List<String>>();
        StringBuilder insertStatements = new StringBuilder();        
        String resultStr = getTransformedValues(xmlFile, recordIds);
        String[] splitValues = resultStr.split(LINE_SEPERATOR);
        for(String line : splitValues) {
            if(line.startsWith("INSERT")) {
                insertStatements.append(line).append(LINE_SEPERATOR);
            } else if(line.startsWith("FILE")){  
                handleRelationship(line, filePathToRecordIdsMap);
            } else {
                handleLoadedRecords(xmlFile, line);
            }
        }
        if(!insertStatements.toString().isEmpty()) {
            insertWriter.write(insertStatements.toString());
        }
        if(!filePathToRecordIdsMap.isEmpty()) {
            for(Entry<String, List<String>> entry : filePathToRecordIdsMap.entrySet()) {
                transform(new File(entry.getKey()), insertWriter, entry.getValue().toArray(new String[entry.getValue().size()]));
            }
        }
        
    }
    
    private void handleLoadedRecords(File xmlFile , String line) {
        String key = xmlFile.getAbsolutePath();
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
    
    private String getTransformedValues(File xmlFile , String... recordIds) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        String resultStr = null;
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            StreamSource xsltSource = new StreamSource(xsltFile);
            StreamResult resultStream = new StreamResult(result);
            Transformer transformer = tFactory.newTransformer(xsltSource);
            StreamSource xmlDataSource = new StreamSource(xmlFile);
            if(recordIds != null && recordIds.length > 0) {
                StringBuilder recordIdBuilder = new StringBuilder();
                for(String recordId : recordIds) {
                    if(loadedRecords.containsKey(xmlFile.getAbsolutePath())) {
                        List<String> values = loadedRecords.get(xmlFile.getAbsolutePath());
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
            throw new RuntimeException(e.getMessage());
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
