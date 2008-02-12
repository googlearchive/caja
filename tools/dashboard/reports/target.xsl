<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output
    method="html"
    version="4.0"
    encoding="UTF-8"
    omit-xml-declaration="yes"
    indent="yes"
    doctype-public="-//W3C//DTD HTML 4.01//EN"
    doctype-system="http://www.w3.org/TR/html4/strict.dtd"
    media-type="text/html"
    />

  <xsl:template match="/">
    <xsl:apply-templates select="report"/>
  </xsl:template>

  <xsl:template match="report">
    <html>
      <head>
        <title>Caja Build Log [<xsl:value-of select="@id"/>]</title>
        <style type="text/css"><![CDATA[
          body { background: white }
        ]]></style>
      </head>
      <body>
        <h1>Caja Build Log [<xsl:value-of select="@id"/>]</h1>

        <xsl:apply-templates select="target"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="target">
    <div id="{@name}">
      <h2><xsl:value-of select="@name"/></h2>
      <pre>
        <xsl:apply-templates select="log"/>
      </pre>
    </div>
  </xsl:template>

</xsl:stylesheet>
