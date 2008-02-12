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
    <xsl:apply-templates select="report/tasks"/>
  </xsl:template>

  <xsl:template match="tasks">
    <html>
      <head>
        <title>Tasks TODO [<xsl:value-of select="../@id"/>]</title>
        <style type="text/css"><![CDATA[
          body { background: white }
          h3 { font-size: 150% }
        ]]></style>
      </head>
      <body>
        <h1>Tasks TODO [<xsl:value-of select="../@id"/>]</h1>

        <xsl:call-template name="owner-index"/>

        <xsl:call-template name="file-index"/>

        <xsl:call-template name="by-owner"/>

        <xsl:call-template name="by-file"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="owner-index">
    <xsl:variable name="owners"
     select="task/@owner[not(.=following::task/@owner)]"/>

    <div id="owner-index">
      <h2>By Owner</h2>
      <ul>
        <xsl:for-each select="$owners">
          <xsl:sort/>
          <xsl:variable name="owner" select="."/>
          <li>
            <a href="#owner-{$owner}">
              <xsl:value-of select="."/>
              <xsl:text> (</xsl:text>
              <xsl:value-of select="count(/report/tasks/task[@owner=$owner])"/>
              <xsl:text>)</xsl:text>
            </a>
          </li>
        </xsl:for-each>
      </ul>
    </div>
  </xsl:template>

  <xsl:template name="file-index">
    <xsl:variable name="files"
     select="task/@file[not(.=following::task/@file)]"/>

    <div id="file-index">
      <h2>By File</h2>
      <ul>
        <xsl:for-each select="$files">
          <xsl:sort/>
          <xsl:variable name="file" select="."/>
          <li>
            <a href="#file-{$file}">
              <tt>
                <abbr title=".">
                  <xsl:call-template name="basename">
                    <xsl:with-param name="path" select="."/>
                  </xsl:call-template>
                </abbr>
              </tt>
            </a>
            <xsl:text> (</xsl:text>
            <xsl:value-of select="count(/report/tasks/task[@file=$file])"/>
            <xsl:text>)</xsl:text>
          </li>
        </xsl:for-each>
      </ul>
    </div>
  </xsl:template>

  <xsl:template name="by-owner">
    <xsl:variable name="owners"
     select="task/@owner[not(.=following::task/@owner)]"/>

    <div id="by-owner">
      <h2>By Owner</h2>
      <xsl:for-each select="$owners">
        <xsl:sort/>
        <xsl:variable name="owner" select="."/>
        <h3 id="owner-{$owner}"><xsl:value-of select="$owner"/></h3>
        <xsl:apply-templates select="/report/tasks/task[@owner=$owner]"/>
      </xsl:for-each>
    </div>
  </xsl:template>

  <xsl:template name="by-file">
    <xsl:variable name="files"
     select="task/@file[not(.=following::task/@file)]"/>

    <div id="by-file">
      <h2>By File</h2>
      <xsl:for-each select="$files">
        <xsl:sort/>
        <xsl:variable name="file" select="."/>
        <h3 id="file-{$file}">
          <tt>
            <abbr title="{$file}">
              <xsl:call-template name="basename">
                <xsl:with-param name="path" select="$file"/>
              </xsl:call-template>
            </abbr>
          </tt>
        </h3>
        <xsl:apply-templates select="/report/tasks/task[@file=$file]"/>
      </xsl:for-each>
    </div>
  </xsl:template>

  <xsl:template match="task">
    <div>
      <a>
        <xsl:if test="contains(@file, '.java') and contains(@file, 'src/')">
          <xsl:attribute name="href">
            <xsl:text>docs/src-html/</xsl:text>
            <xsl:value-of
             select="substring-after(
                         substring-after(
                             substring-before(@file, '.java'), 'src/'), '/')"/>
            <xsl:text>.html#line.</xsl:text>
            <xsl:value-of select="@line"/>
          </xsl:attribute>
        </xsl:if>
        <tt>
          <abbr title="{@file}">
            <xsl:call-template name="basename">
              <xsl:with-param name="path" select="@file"/>
            </xsl:call-template>
          </abbr>
          <xsl:text>:</xsl:text>
          <xsl:value-of select="@line"/>
        </tt>
      </a>
      <xsl:text> (</xsl:text>
      <xsl:value-of select="@owner"/>
      <xsl:text>)</xsl:text>
      <blockquote>
        <xsl:apply-templates select="text()"/>
      </blockquote>
    </div>
  </xsl:template>

  <xsl:template name="basename">
    <xsl:param name="path"/>
    <xsl:choose>
      <xsl:when test="contains($path, '/')">
        <xsl:call-template name="basename">
          <xsl:with-param name="path" select="substring-after($path, '/')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$path"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
