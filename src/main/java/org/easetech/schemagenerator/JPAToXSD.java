
package org.easetech.schemagenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.namespace.QName;
import org.easetech.schema.Annotation;
import org.easetech.schema.Attribute;
import org.easetech.schema.ComplexType;
import org.easetech.schema.Documentation;
import org.easetech.schema.Element;
import org.easetech.schema.ExtensionSimpleContent;
import org.easetech.schema.RestrictionSimpleType;
import org.easetech.schema.Schema;
import org.easetech.schema.SimpleContent;
import org.easetech.schema.SimpleType;
import org.easetech.schema.UseValues;
import org.easetech.schema.util.XSDGenerator;
import org.reflections.Reflections;

/**
 * A simple class that is capable of generating as well as creating well formatted XSD files from JPA classes. The main
 * benefit of doing such a thing is that the XSD files can be used to create XML data that can be then easily converted
 * to INSERT statements to be loaded in the database. The user does not have to maintain any XSD or XSLT file. Only XML
 * file that closely resembles the JPA class it represents.
 * 
 */
public class JPAToXSD {

    private static final String COLUMN_NAME = "columnName";

    private static final String COLUMN_TYPE = "columnType";

    private static final String IS_NULLABLE = "nullable";

    private static final String ID_COLUMN = "idColumn";

    private static final String TABLE_NAME = "tableName";

    private static final String RECORD_ID = "recordId";

    private static final String LENGTH = "length";

    private static Schema createSchemaWithDocumentation(String content) {
        Schema schema = new Schema();
        Annotation annotation = new Annotation();
        Documentation documentation = new Documentation();
        documentation.setContent(content);
        annotation.getDocumentation().add(documentation);
        schema.setAnnotation(annotation);
        return schema;
    }

    /**
     * Generate a list of XSD files from the JPA entities that can be found in the specified package
     * 
     * @param packageName the package name of the JPA Entities
     * @return list of XSD files where each XSD file corresponds to one JPA entity
     */
    public static List<String> generate(String packageName) {
        List<String> xsdList = new ArrayList<String>();
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> jpaClasses = reflections.getTypesAnnotatedWith(Entity.class);
        for (Class<?> jpaClass : jpaClasses) {
            xsdList.add(generate(jpaClass));
        }
        return xsdList;
    }

    private static String createXSDFile(String outputLocation, Class<?> jpaClass, String xsd) {
        File xsdFile = new File(outputLocation.concat("/").concat(jpaClass.getSimpleName()).concat(".xsd"));

        if (xsdFile.exists()) {
            xsdFile.delete();
        }
        try {
            
            xsdFile.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Writer writer = null;
        try {
            if (xsdFile.exists()) {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xsdFile), "utf-8"));
                writer.write(xsd);
            }

        } catch (IOException ex) {
            // Do nothing for now
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (Exception ex) {

                throw new RuntimeException(ex);
            }
        }
        return xsdFile.getAbsolutePath();
    }

    /**
     * Generate and the create XSD files for the given JPA entities inside the package name specified
     * 
     * @param jpaClassPackage the jpa package name
     * @param outputLocation the location where the xsd files need to be generated, This is a directory location
     * @return list of path where the XSD files have been generated
     */
    public static List<String> generateAndCreate(String jpaClassPackage, String outputLocation) {
        List<String> xsdPathList = new ArrayList<String>();
        Reflections reflections = new Reflections(jpaClassPackage);
        Set<Class<?>> jpaClasses = reflections.getTypesAnnotatedWith(Entity.class);
        for (Class<?> jpaClass : jpaClasses) {
            String xsd = generate(jpaClass);
            xsdPathList.add(createXSDFile(outputLocation, jpaClass, xsd));
        }
        return xsdPathList;

    }

    /**
     * Generate and create XSD file for a single JPA entity class
     * 
     * @param jpaClass the jpa class for which to create XSD file.
     * @param location the location where the file should be saved
     * @return the location of the saved file
     */
    public static String generateAndCreate(Class<?> jpaClass, String location) {
        String xsd = generate(jpaClass);
        return createXSDFile(location, jpaClass, xsd);
    }

    /**
     * Generate the XSD String for the given JPA entity Class
     * 
     * @param jpaClass the jpa class
     * @return the string representation of XSD file
     */
    public static String generate(Class<?> jpaClass) {
        String result = null;
        Entity entity = jpaClass.getAnnotation(Entity.class);
        if (entity == null) {
            return null;
        } else {
            Schema schema = createSchemaWithDocumentation("An XSD class for " + jpaClass.getName());
            addDocumentNode(schema, jpaClass);
            Element[] elements = createElements(jpaClass.getDeclaredFields(), schema);
            addRootNode(schema, jpaClass, elements);
            try {
                result = XSDGenerator.generateSchema(schema);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return result;
    }

    private static Element[] createElements(Field[] jpaFields, Schema schema) {
        List<Element> elements = new ArrayList<Element>();
        for (Field field : jpaFields) {
            field.setAccessible(true);
            Element createdElement = null;
            Element[] createdElements = null;
            if(field.getAnnotation(Id.class) != null && field.getAnnotation(Column.class) == null) {
                Class<?> idClass = field.getType();
                createdElements = createElements(idClass.getDeclaredFields(), schema);
            }
            else {
                createdElement = createElement(field, schema);
            } 
            
            if (createdElement != null) {
                elements.add(createdElement);
            }
            if(createdElements != null && createdElements.length > 0) {
                for(int i=0 ; i< createdElements.length ; i++) {
                    elements.add(createdElements[i]);
                }
                
            }

        }
        return elements.toArray(new Element[jpaFields.length]);
    }

    private static void addRootNode(Schema schema, Class<?> jpaClass, Element[] elements) {
        ComplexType rootNode = XSDUtil.generateComplexType(jpaClass.getSimpleName(), elements);
        Table tableAnnotation = jpaClass.getAnnotation(Table.class);
        Attribute tableNameAttribute = null;
        if (tableAnnotation != null) {
            String tableName = tableAnnotation.name();
            tableNameAttribute = XSDUtil.getAttribute(TABLE_NAME, new QName("xsd:string"), UseValues.OPTIONAL,
                tableName, null);
        }
        Attribute recordIdAttribute = XSDUtil.getAttribute(RECORD_ID, new QName("xsd:ID"), UseValues.REQUIRED, null, null);
        XSDUtil.addAttributesToComplexType(rootNode, tableNameAttribute, recordIdAttribute);
        schema.getComplexType().add(rootNode);
    }

    private static void addDocumentNode(Schema schema, Class<?> jpaClass) {
        schema.getElement().add(XSDUtil.generateElement(jpaClass.getSimpleName().concat("List"), null));
        Element element = XSDUtil.generateElement(jpaClass.getSimpleName(), jpaClass.getSimpleName(), "1", "unbounded");
        ComplexType rootComplexType = XSDUtil.generateComplexType(jpaClass.getSimpleName().concat("List"), element);
        schema.getComplexType().add(rootComplexType);
    }

    private static Element handleStandardJavaTypes(Field field, Schema schema) {
        Element element = null;
        if (field.isAnnotationPresent(Column.class)) {
            element = getElementInstance(field);
            String xsdType = getType(field);
            if (xsdType == null) {
                handleEnumTypes(element, field, schema);
                // addAttributes(element, field);
            } else {
                ComplexType annonymousCT = new ComplexType();
                SimpleContent sc = new SimpleContent();
                annonymousCT.setSimpleContent(sc);
                element.setComplexType(annonymousCT);
                ExtensionSimpleContent esc = new ExtensionSimpleContent();
                sc.setExtension(esc);
                esc.setBase(new QName(null, xsdType));
                addAttributes(esc, field);
            }

        } 
        return element;
    }

    private static Element getElementInstance(Field field) {
        Element element = new Element();
        element.setName(field.getName());
        Boolean fieldNullable = fieldNillable(field);
        element.setMinOccurs(fieldNullable ? new BigInteger("0") : new BigInteger("1"));
        Boolean multiValueField = multiValueField(field);
        element.setMaxOccurs(multiValueField ? "unbounded" : "1");
        return element;
    }

    private static void handleEnumTypes(Element element, Field field, Schema schema) {
        if (field != null && field.getType() != null && field.getType().getSuperclass() != null
            && Enum.class.isAssignableFrom(field.getType().getSuperclass())) {
            try {
                Field[] fields = field.getType().getDeclaredFields();
                List<String> enumValues = new ArrayList<String>();
                for (Field fieldEnum : fields) {
                    if (fieldEnum.isEnumConstant()) {
                        enumValues.add(fieldEnum.getName());
                    }

                }
                SimpleType st = new SimpleType();
                st.setName(field.getName());
                RestrictionSimpleType rst = new RestrictionSimpleType();
                rst.setBase(new QName(null, "xsd:string"));
                rst.getEnumeration().addAll(enumValues);
                st.setRestriction(rst);
                schema.getSimpleType().add(st);
                // next create a complex type
                ComplexType compType = new ComplexType();
                compType.setName(field.getName().concat("ComplexType"));
                SimpleContent simCont = new SimpleContent();
                compType.setSimpleContent(simCont);
                ExtensionSimpleContent esc = new ExtensionSimpleContent();
                esc.setBase(new QName("et:".concat(field.getName())));
                simCont.setExtension(esc);
                addAttributes(esc, field);
                schema.getComplexType().add(compType);
                element.setType(new QName("et:".concat(field.getName()).concat("ComplexType")));
                // element.getSimpleType().add(st);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    private static Element createElement(Field field, Schema schema) {
        Element element = handleStandardJavaTypes(field, schema);
        if (element == null) {
            element = handleAnyToMany(field);
        }
        if (element == null) {
            element = handleManyToOne(field);
        }
        if (element == null) {
            element = handleOneToOne(field);
        }

        return element;

    }

    private static Element handleOneToOne(Field field) {
        Element element = null;
        if (field.isAnnotationPresent(OneToOne.class)) {
            if(field.isAnnotationPresent(JoinColumn.class) && !field.getAnnotation(JoinColumn.class).insertable()) {
                return element;
            }
            OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);
            Boolean isOptional = oneToOneAnnotation.optional();
            element = new Element();
            element.setName(field.getName());
            if (isOptional) {
                element.setMinOccurs(new BigInteger("0"));
            } else {
                element.setMinOccurs(new BigInteger("1"));
            }
            element.setMaxOccurs("1");
            Attribute pathAttribute = XSDUtil.getAttribute("filePath", new QName("xsd:string"), UseValues.REQUIRED,
                null, field.getName());
            Attribute idAttribute = XSDUtil.getAttribute("recordIdRef", new QName("xsd:IDREF"), UseValues.REQUIRED,
                null, field.getName());
            XSDUtil.addAttributesToElement(element, pathAttribute, idAttribute);

        }
        return element;
    }

    private static Element handleManyToOne(Field field) {
        Element element = null;
        if (field.isAnnotationPresent(ManyToOne.class)) {
            if(field.isAnnotationPresent(JoinColumn.class) && !field.getAnnotation(JoinColumn.class).insertable()) {
                return element;
            }
            element = getElementInstance(field);
            ManyToOne manyToOneAnnotation = field.getAnnotation(ManyToOne.class);
            JoinColumn joinCol = field.getAnnotation(JoinColumn.class);
            ComplexType annonymousCT = new ComplexType();
            SimpleContent sc = new SimpleContent();
            annonymousCT.setSimpleContent(sc);
            element.setComplexType(annonymousCT);
            ExtensionSimpleContent esc = new ExtensionSimpleContent();
            sc.setExtension(esc);
            String type = getType(field);
            if (type == null) {
                type = getType(getTypeFromClass(field));
            }
            esc.setBase(new QName(null, type));
            // addAttributes(esc, field);

            String columnName = joinCol.name();
            Boolean isOptional = manyToOneAnnotation.optional();

            if (isOptional) {
                element.setMinOccurs(new BigInteger("0"));
            } else {
                element.setMinOccurs(new BigInteger("1"));
            }
            element.setMaxOccurs("1");
            Attribute pathAttribute = XSDUtil.getAttribute("filePath", new QName("xsd:string"), UseValues.REQUIRED,
                null, field.getName());
            Attribute idAttribute = XSDUtil.getAttribute("recordIdRef", new QName("xsd:IDREF"), UseValues.REQUIRED,
                null, field.getName());
            Attribute columnNameAttribute = XSDUtil.getAttribute(COLUMN_NAME, new QName("xsd:string"),
                UseValues.OPTIONAL, columnName, field.getName());
            Attribute columnTypeAttribute = XSDUtil.getAttribute(COLUMN_TYPE, new QName("xsd:string"), null, type, field.getName());
            esc.getAttribute().add(columnNameAttribute);
            esc.getAttribute().add(idAttribute);
            esc.getAttribute().add(pathAttribute);
            esc.getAttribute().add(columnTypeAttribute);
            // SeedXSDFilesGenerator.addAttributesToElement(element, pathAttribute, idAttribute, columnNameAttribute);

        }
        return element;
    }

    private static Class getTypeFromClass(Field field) {
        Class<?> result = null;
        Class<?> clazz = field.getType();
        Field[] fields = clazz.getDeclaredFields();
        for (Field declaredField : fields) {
            Id idAnnotation = declaredField.getAnnotation(Id.class);
            if (idAnnotation != null) {
                result = declaredField.getType();
                break;
            }
        }
        return result;
    }

    private static Element handleAnyToMany(Field field) {
        Element element = null;
        if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
            if(field.isAnnotationPresent(JoinColumn.class) && !field.getAnnotation(JoinColumn.class).insertable()) {
                return element;
            }
                element = new Element();
                element.setName(field.getName());
                element.setMinOccurs(new BigInteger("0"));
                element.setMaxOccurs("1");
                Attribute globalPathAttribute = XSDUtil.getAttribute("globalFilePath", new QName("xsd:string"),
                    UseValues.OPTIONAL, null , field.getName());

                Element oneToManyElement = new Element();
                oneToManyElement.setName("value");
                oneToManyElement.setMinOccurs(new BigInteger("1"));
                oneToManyElement.setMaxOccurs("unbounded");
                Attribute pathAttribute = XSDUtil.getAttribute("filePath", new QName("xsd:string"), UseValues.OPTIONAL,
                    null, field.getName());
                Attribute idAttribute = XSDUtil.getAttribute("recordIdRef", new QName("xsd:IDREF"), UseValues.REQUIRED,
                    null, field.getName());
                XSDUtil.addAttributesToElement(oneToManyElement, idAttribute, pathAttribute);
                ComplexType oneToManyComplexType = XSDUtil.generateComplexType((String) null, oneToManyElement);
                element.setComplexType(oneToManyComplexType);
                XSDUtil.addAttributesToElement(element, globalPathAttribute); 
            
            

        }
        return element;
    }

//    private static void addAttributes(Element element, Field field) {
//        ComplexType complexType = new ComplexType();
//        element.setComplexType(complexType);
//
//        Column column = field.getAnnotation(Column.class);
//        if (column != null) {
//            complexType.getAttribute().add(addColumnNameAttribute(field, column));
//            complexType.getAttribute().add(addIsNullAttribute(field, column));
//            complexType.getAttribute().add(addLengthAttribute(field, column));
//        }
//        if (field.getAnnotation(Id.class) != null) {
//            complexType.getAttribute().add(
//                XSDUtil.getAttribute(ID_COLUMN, new QName("xsd:boolean"), null, String.valueOf(true)));
//        }
//    }

    private static void addAttributes(ExtensionSimpleContent esc, Field field) {

        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            esc.getAttribute().add(addColumnNameAttribute(field, column));
            esc.getAttribute().add(addIsNullAttribute(field, column));
            esc.getAttribute().add(addLengthAttribute(field, column));
            esc.getAttribute().add(addTypeAttribute(field, column));
            if (field.getAnnotation(Id.class) != null) {
                esc.getAttribute().add(
                    XSDUtil.getAttribute(ID_COLUMN, new QName("xsd:boolean"), null, String.valueOf(true), field.getName()));
            }
        } else {
            if(field.getAnnotation(Id.class) != null) {
                Class<?> idClass = field.getType();
                Field[] idFields = idClass.getDeclaredFields();
                for(Field idField : idFields) {
                    addAttributes(esc, idField);
                }
            }
        }
        
    }

    private static Attribute addTypeAttribute(Field field, Column column) {
        String type = getType(field);
        if (type == null) {
            type = "xsd:enum";
        }
        return XSDUtil.getAttribute(COLUMN_TYPE, new QName("xsd:string"), null, type, field.getName() );
    }

    private static Attribute addColumnNameAttribute(Field field, Column column) {
        String columnValue = column.name() == "" ? field.getName() : column.name();
        return XSDUtil.getAttribute(COLUMN_NAME, new QName("xsd:string"), null, columnValue, field.getName());
    }

    private static Attribute addIsNullAttribute(Field field, Column column) {
        return XSDUtil.getAttribute(IS_NULLABLE, new QName("xsd:boolean"), null, String.valueOf(column.nullable()), field.getName());
    }

    private static Attribute addLengthAttribute(Field field, Column column) {
        return XSDUtil.getAttribute(LENGTH, new QName("xsd:int"), null, String.valueOf(column.length()), field.getName());
    }

    private static String getType(Field field) {
        return getType(field.getType());
    }

    private static String getType(Class clazz) {
        Map<Class, String> javaToXSDType = new HashMap<Class, String>();
        javaToXSDType.put(Boolean.class, "xsd:boolean");
        javaToXSDType.put(boolean.class, "xsd:boolean");
        javaToXSDType.put(Byte.class, "xsd:byte");
        javaToXSDType.put(byte.class, "xsd:byte");
        javaToXSDType.put(Double.class, "xsd:double");
        javaToXSDType.put(double.class, "xsd:double");
        javaToXSDType.put(Float.class, "xsd:float");
        javaToXSDType.put(float.class, "xsd:float");
        javaToXSDType.put(Integer.class, "xsd:integer");
        javaToXSDType.put(int.class, "xsd:integer");
        javaToXSDType.put(Long.class, "xsd:long");
        javaToXSDType.put(long.class, "xsd:long");
        javaToXSDType.put(Short.class, "xsd:short");
        javaToXSDType.put(short.class, "xsd:short");
        javaToXSDType.put(String.class, "xsd:string");
        javaToXSDType.put(BigDecimal.class, "xsd:decimal");
        javaToXSDType.put(BigInteger.class, "xsd:int");
        javaToXSDType.put(URI.class, "xsd:anyURI");
        javaToXSDType.put(Calendar.class, "xsd:dateTime");
        javaToXSDType.put(Date.class, "xsd:dateTime");
        javaToXSDType.put(QName.class, "xsd:QName");
        return javaToXSDType.get(clazz);
    }

    private static Boolean multiValueField(Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            return true;
        }
        return false;
    }

    private static Boolean fieldNillable(Field field) {
        Boolean result = true;
        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            result = column.nullable();
        }
        return result;
    }

}
