<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:java="http://xml.apache.org/xslt/java" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
 <xsl:output method="text" encoding="UTF-8" />
 <xsl:param name="suppliedRecordIds" select="0" />
 <xsl:variable name="outermostElementName" select="local-name(/*)" />
 <xsl:template match="/">
 <xsl:value-of select="$outermostElementName"/>
 <xsl:text>&#10;</xsl:text>
 <xsl:value-of select="/node()/@*[local-name()='schemaLocation']"/>
 <xsl:text>&#10;</xsl:text>
  <xsl:for-each select="/*/*">
   <xsl:if test="contains($suppliedRecordIds,@recordId) or $suppliedRecordIds = 0">
    <xsl:variable name="INSERT_QUERY">
    <xsl:text>INSERT INTO </xsl:text>
    <xsl:choose>
            <xsl:when test="@tableName">
                <xsl:value-of select="@tableName" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>{table_name}</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    
    <xsl:text> (</xsl:text>
    </xsl:variable>
    <xsl:value-of select="$INSERT_QUERY"></xsl:value-of>
    <!-- <xsl:for-each select="*[@columnName]"> -->
    <xsl:for-each select="*">       
        <xsl:choose>
            <xsl:when test="@columnName">
                <xsl:value-of select="@columnName" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>{</xsl:text> <xsl:value-of select ="local-name()"/> <xsl:text>}</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
     <xsl:if test="position()!=last()">
      <xsl:text>, </xsl:text>
     </xsl:if>
    </xsl:for-each>

    <xsl:text>) values (</xsl:text>
    <!-- <xsl:for-each select="*[@columnName]"> -->
    <xsl:for-each select="*">
    <xsl:choose>
        <xsl:when test="@columnType = 'xsd:string' or @columnType = 'xsd:anyURI' or @columnType = 'xsd:dateTime' or @columnType = 'xsd:enum'">
            <xsl:text>'</xsl:text>
            <xsl:value-of select="." />
            <xsl:text>'</xsl:text>
     </xsl:when>
     <xsl:otherwise>
        <xsl:value-of select="." />
     </xsl:otherwise>
    </xsl:choose>
    
     <xsl:if test="position()!=last()">
      <xsl:text>, </xsl:text>
     </xsl:if>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
    <!-- Start a new line -->
    <xsl:text>&#10;</xsl:text>
    <xsl:for-each select="*[@globalFilePath]">
     <xsl:variable name="globalFilePath">
      <xsl:value-of select="@globalFilePath" />
     </xsl:variable>
     <xsl:text>FILE Path=</xsl:text>
     <xsl:choose>
      <xsl:when test="string-length($globalFilePath) > 0">
       <xsl:value-of select="$globalFilePath" />
      </xsl:when>
      <xsl:otherwise>
       <xsl:value-of select="./value/@filePath" />
      </xsl:otherwise>
     </xsl:choose>
     <xsl:text>&amp;RECORDID=</xsl:text>
     <xsl:value-of select="./value/@recordIdRef" />
     <xsl:text>&#10;</xsl:text>
     
    </xsl:for-each>
    
    <xsl:for-each select="*[@filePath]">
     <xsl:variable name="filePath">
      <xsl:value-of select="@filePath" />
     </xsl:variable>
     <xsl:text>FILE Path=</xsl:text>
     <xsl:choose>
      <xsl:when test="string-length($filePath) > 0">
       <xsl:value-of select="$filePath" />
      </xsl:when>
      <xsl:otherwise>
       <xsl:value-of select="@filePath" />
      </xsl:otherwise>
     </xsl:choose>
     <xsl:text>&amp;RECORDID=</xsl:text>
     <xsl:value-of select="@recordIdRef" />
     <xsl:text>&#10;</xsl:text>
     
    </xsl:for-each>
    <xsl:text>LOADED RECORDID=</xsl:text>
    <xsl:value-of select="@recordId" />
     <xsl:text>&#10;</xsl:text>
   </xsl:if>
  </xsl:for-each>
 </xsl:template>
</xsl:stylesheet>