data-manager
============

A light weight library that can generate XSD files on the fly for your JPA classes. All you have to do is provide the JPA class package name. The tool will automatically search for all the JPA entities and create an XSD for each Entity. You can then use the XSD to quickly create your XML based data. Once you have the XML data, you can then use XMLtoSQLTransformer class to convert the XML to sql INSERT statements that are saved in a file.
