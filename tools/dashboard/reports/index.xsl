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
        <title>Caja Build [<xsl:value-of select="@id"/>]</title>
        <style type="text/css"><![CDATA[
          body { background: white }
          caption, h2 {
            font-size: 125%;
            background: #f0f0ff;
            margin: 2px;
            border: 1px solid #c0c0ff
          }
          h2 {
            width: 40em;
            font-weight: normal
          }
          #reports {
            left: 5%;
            width: 90%;
            position: absolute;
            top: 0px
          }
          #reports caption { display: none }
          #reports td {
            text-align: center;
            background: #f0f0f0;
            border: 1px dotted #888;
            margin: 1px;
            font-size: 80%;
            font-weight: bold
          }
          #logs {
            float: right;
          }
          #logs h2 { width: 7em }
          #logs ul {
            list-style: none;
            padding: .5em;
            margin: 0;
            border: 1px dotted #888
          }
          #tests table, #coverage table { border: 1px dotted #888 }
          #tests th, #coverage th { background: #f0f0f0 }
          #tests td, #coverage td {
            text-align: right; padding: 4px
          }

          #tests, #coverage, #varz { float: left; width: 33% }

          .problem caption { background: #ffe0e0; border: 1px solid #ffc0c0 }
          .problem { background: #fff0f0; border: 2px solid red }

          #tasks { margin-bottom: 1em }
        ]]></style>
      </head>
      <body>
        <h1>Caja Build [<xsl:value-of select="@id"/>]</h1>

        <xsl:call-template name="outputs"/>

        <xsl:call-template name="targets"/>

        <xsl:call-template name="tasks"/>

        <xsl:call-template name="test-summary"/>

        <xsl:call-template name="coverage-summary"/>

        <xsl:call-template name="varz"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="outputs">
    <table id="reports" title="reports" cols="{count(output)}">
      <caption>Reports</caption>
      <tr>
        <xsl:for-each select="output">
          <td>
            <a href="{@href}"><xsl:value-of select="@name"/></a>
          </td>
        </xsl:for-each>
      </tr>
    </table>
  </xsl:template>

  <xsl:template name="targets">
    <div id="logs">
      <h2>ANT Logs</h2>
      <ul>
        <xsl:for-each select="target">
          <li>
            <xsl:variable name="status-var-name"
             >target.<xsl:value-of select="@name"/>.status</xsl:variable>
            <xsl:variable name="time-var-name"
             >target.<xsl:value-of select="@name"/>.time</xsl:variable>
            <xsl:if test="/report/varz[@name=$status-var-name ]/@value = '0'">
              <xsl:attribute name="class">problem</xsl:attribute>
            </xsl:if>
            <a href="target.html#{@name}">
              <xsl:value-of select="@name"/>
              <xsl:text> (</xsl:text>
              <xsl:value-of
               select="/report/varz[@name=$time-var-name ]/@value"/>
              <xsl:text>s)</xsl:text>
            </a>
          </li>
        </xsl:for-each>
      </ul>
    </div>
  </xsl:template>

  <xsl:template name="tasks">
    <div id="tasks">
      <h2><a href="tasks.html">Tasks</a></h2>
      <xsl:variable name="limit" select="10"/>
      <ul>
        <xsl:for-each select="tasks/task">
          <xsl:if test="position() &lt;= $limit">
            <li>
              <tt>
                <abbr title="{@file}">
                  <xsl:call-template name="basename">
                    <xsl:with-param name="path" select="@file"/>
                  </xsl:call-template>
                  <xsl:text>:</xsl:text>
                  <xsl:value-of select="@line"/>
                </abbr>
              </tt>
              <xsl:text> (</xsl:text>
              <xsl:value-of select="@owner"/>
              <xsl:text>) </xsl:text>
              <xsl:value-of select="."/>
            </li>
          </xsl:if>
        </xsl:for-each>
      </ul>
      <xsl:if test="count(tasks/task) &gt; $limit">
        <xsl:text>+</xsl:text>
        <xsl:value-of select="count(tasks/task) - $limit"/>
        <xsl:text> more</xsl:text>
      </xsl:if>
    </div>
  </xsl:template>

  <xsl:template name="test-summary">
    <table title="test summary" cols="4" id="tests">
      <xsl:if test="varz[@name='junit.pct']/@value != 0.0">
        <xsl:attribute name="class">problem</xsl:attribute>
      </xsl:if>
      <caption><a href="tests/index.html">Test Summary</a></caption>
      <tr>
        <th>Total</th>
        <th>Failures</th>
        <th>Errors</th>
        <th>%</th>
      </tr>
      <tr>
        <td><xsl:value-of select="varz[@name='junit.total']/@value"/></td>
        <td><xsl:value-of select="varz[@name='junit.failures']/@value"/></td>
        <td><xsl:value-of select="varz[@name='junit.errors']/@value"/></td>
        <td><xsl:value-of select="varz[@name='junit.pct']/@value"/>%</td>
      </tr>
    </table>
  </xsl:template>

  <xsl:template name="coverage-summary">
    <table title="coverage summary" cols="3" id="coverage">
      <xsl:if test="varz[@name='emma.pct']/@value &lt; 80.0">
        <xsl:attribute name="class">problem</xsl:attribute>
      </xsl:if>
      <caption><a href="coverage/index.html">Coverage Summary</a></caption>
      <tr>
        <th>Total</th>
        <th>Covered</th>
        <th>%</th>
      </tr>
      <tr>
        <td><xsl:value-of select="varz[@name='emma.total']/@value"/></td>
        <td><xsl:value-of select="varz[@name='emma.covered']/@value"/></td>
        <td><xsl:value-of select="varz[@name='emma.pct']/@value"/>%</td>
      </tr>
    </table>
  </xsl:template>

  <xsl:template name="varz">
    <table id="varz">
      <caption><a href="varz.xhtml">VarZ</a></caption>
      <xsl:for-each select="varz">
        <xsl:sort select="@name" data-type="text"/>
        <tr id="@name">
          <td class="key"><xsl:value-of select="@name"/></td>
          <td class="value"><xsl:value-of select="@value"/></td>
          <td class="history">
            <a href="varz.xhtml#{@name}">[history]</a>
          </td>
        </tr>
      </xsl:for-each>
    </table>
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
