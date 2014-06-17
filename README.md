data-manager
============

A light weight library that can generate XSD files on the fly for your JPA classes. All you have to do is provide the JPA class package name. The tool will automatically search for all the JPA entities and create an XSD for each Entity. You can then use the XSD to quickly create your XML based data. Once you have the XML data, you can then use XMLtoSQLTransformer class to convert the XML to sql INSERT statements that are saved in a file.

The following Classes are available, currently as standalone classes.

* <B>JPAToXSD.java</B> - This class is responsible for converting you JPA classes into corresponding XSD classes. The generated XSD classes have a lot of optional information that is used by the tool to generate INSERT statements. You can use four methods on this class :
  *   <B><i>public static List<String> generate(String packageName)</B></i> -> This method is used to generate XSD classes for all the JPA entities that are part of the packageName. It creates one XSD String for each JPA Entity. It returns a list of XSD Strings each one corresponding to one JPA Entity.
  *   <B><i>public static String generate(Class<?> jpaClass)</B></i> -> This method is used to generate a single XSD file for the given JPA Class. It returns the string representation of the XSD generated.
  *   <B><i>public static List<String> generateAndCreate(String jpaClassPackage, String outputLocation)</B></i> -> Same like generate but this time it also saves the generated XSD files to the specified output location
  *   <B><i>public static String generateAndCreate(Class<?> jpaClass, String location)</B></i> -> Same like generate but this time it also saves the generated XSD files to the specified output location

* <B>XMLToSQLTransformer.java</B> - As the name suggests it converts the XML, conforming to the XSD created as part of JPAtoXSD class, to a SQL file containing the INSERT statements. All the necessary dependencies, like OneToMany, ManyToMany, ManyToOne and OneToOne are taken care of by the tool while generating the INSERT statements. Note that the generated SQL file escapes the Foreign Check constraint by defining <B>SET foreign_key_checks = 0</B> at the start of the file and <B>SET foreign_key_checks = 1</B> this at the end of the file to reenable the constraints. Thus it is upto the user to validate the foreign key constraints.
