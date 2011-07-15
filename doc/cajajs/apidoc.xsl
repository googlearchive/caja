<?xml version="1.0"?>
<!--
  == Copyright (C) 2011 Google Inc.
  ==
  == Licensed under the Apache License, Version 2.0 (the "License");
  == you may not use this file except in compliance with the License.
  == You may obtain a copy of the License at
  ==
  == http://www.apache.org/licenses/LICENSE-2.0
  ==
  == Unless required by applicable law or agreed to in writing, software
  == distributed under the License is distributed on an "AS IS" BASIS,
  == WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  == See the License for the specific language governing permissions and
  == limitations under the License.
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html" omit-xml-declaration="yes"/>

  <!-- ================================================== -->

  <xsl:template match="/">
    <html>
      <head>
        <link href="prettify/prettify.css" type="text/css" rel="stylesheet" />
        <script type="text/javascript" src="prettify/prettify.js" />

        <link href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/themes/base/jquery-ui.css" type="text/css" rel="stylesheet" />
        <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.5/jquery.min.js" />
        <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/jquery-ui.min.js" />

        <link href="apidoc.css" type="text/css" rel="stylesheet" />
        <script type="text/javascript" src="apidoc.js" />

        <title><xsl:value-of select="objects/title" /></title>
      </head>
      <body onload="initialize();">
        <xsl:apply-templates/>
      </body>
    </html>
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template match="objects">
    <div class="container">
      <div class="page-name">
        <xsl:value-of select="title"/>
      </div>
      <div class="doc"><xsl:copy-of select="doc/node()"/></div>
      <xsl:for-each select="object">
        <xsl:call-template name="object">
          <xsl:with-param name="ctx"><xsl:value-of select="name"/></xsl:with-param>
        </xsl:call-template>
      </xsl:for-each>
    </div>
    <div class="footer">
      Copyright &#169; 2011, Google Inc. All Rights Reserved.
    </div>
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="object">
    <xsl:param name="ctx" />
    <div class="container">
      <div class="object-name">
        <xsl:attribute name="hidecontrol"><xsl:value-of select="$ctx" /></xsl:attribute>
        <code><xsl:value-of select="name"/></code>
      </div>
      <div class="content">
        <xsl:attribute name="id"><xsl:value-of select="$ctx" /></xsl:attribute>
        <div class="doc"><xsl:copy-of select="doc/node()"/></div>      

        <xsl:call-template name="properties">
          <xsl:with-param name="ctx"><xsl:value-of select="name" />_properties</xsl:with-param>
        </xsl:call-template>

        <xsl:call-template name="methods">
          <xsl:with-param name="ctx"><xsl:value-of select="name" />_methods</xsl:with-param>
        </xsl:call-template>

        <xsl:call-template name="examples">
          <xsl:with-param name="ctx"><xsl:value-of select="name" />_examples</xsl:with-param>
        </xsl:call-template>
      </div>
    </div>
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="properties">
    <xsl:param name="ctx" />
    <xsl:if test="count(property) > 0"> 
      <div class="container">
        <div class="section-header">
          <xsl:attribute name="hidecontrol"><xsl:value-of select="$ctx" /></xsl:attribute>
          Properties
        </div>
        <div class="content">
          <xsl:attribute name="id"><xsl:value-of select="$ctx" /></xsl:attribute>
          <xsl:for-each select="property">
            <xsl:call-template name="property">
              <xsl:with-param name="ctx"><xsl:value-of select="$ctx" />_<xsl:value-of select="name" /></xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
        </div>
      </div>
    </xsl:if>  
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="methods">
    <xsl:param name="ctx" />
    <xsl:if test="count(method) > 0">
      <div class="container">
        <div class="section-header">
          <xsl:attribute name="hidecontrol"><xsl:value-of select="$ctx" /></xsl:attribute>
          Methods
        </div>
        <div class="content">
          <xsl:attribute name="id"><xsl:value-of select="$ctx" /></xsl:attribute>
          <xsl:for-each select="method">
            <xsl:call-template name="method">
              <xsl:with-param name="ctx"><xsl:value-of select="$ctx" />_<xsl:value-of select="name" /></xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
        </div>
      </div>
    </xsl:if>  
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="examples">
    <xsl:param name="ctx" />
    <xsl:if test="count(example) > 0">
      <div class="container">
        <div class="section-header">
          <xsl:attribute name="hidecontrol"><xsl:value-of select="$ctx" /></xsl:attribute>
          Examples
        </div>
        <div class="content">
          <xsl:attribute name="id"><xsl:value-of select="$ctx" /></xsl:attribute>
          <xsl:for-each select="example">
            <xsl:call-template name="example">
              <xsl:with-param name="ctx"><xsl:value-of select="$ctx" />_<xsl:value-of select="position()" /></xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
        </div>
      </div>
    </xsl:if>
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="property">
    <xsl:param name="ctx" />
    <div class="container">
      <div class="item-header">
        <xsl:attribute name="hidecontrol"><xsl:value-of select="$ctx" /></xsl:attribute>
        <code><xsl:value-of select="name"/></code>
      </div>
      <div class="content">
        <xsl:attribute name="id"><xsl:value-of select="$ctx" /></xsl:attribute>
        <div class="doc"><xsl:copy-of select="doc/node()"/></div>
      </div>
    </div>
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="method">
    <xsl:param name="ctx" />
    <div class="container">
      <div class="item-header">
        <xsl:attribute name="hidecontrol"><xsl:value-of select="$ctx" /></xsl:attribute>
        <code><xsl:call-template name="method-synopsis"/></code>
      </div>
      <div class="content">
        <xsl:attribute name="id"><xsl:value-of select="$ctx" /></xsl:attribute>
        <div class="doc"><xsl:copy-of select="doc/node()"/></div>
        <xsl:for-each select="arg">
          <xsl:call-template name="arg">
            <xsl:with-param name="ctx"><xsl:value-of select="$ctx" />_<xsl:value-of select="name" /></xsl:with-param>
          </xsl:call-template>
        </xsl:for-each>
        <xsl:if test="count(returndoc) > 0">
          <xsl:for-each select="returndoc/node()">
            <xsl:call-template name="returns">
              <xsl:with-param name="ctx"><xsl:value-of select="$ctx" />__returnvalue__</xsl:with-param>
            </xsl:call-template>
          </xsl:for-each>
        </xsl:if>
      </div>
    </div>
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="returns">
    <xsl:param name="ctx" />
    <div class="container">
      <div class="sub-item-header">
        <xsl:attribute name="hidecontrol"><xsl:value-of select="$ctx" /></xsl:attribute>
        Returns
      </div>
      <div class="content">
        <xsl:attribute name="id"><xsl:value-of select="$ctx" /></xsl:attribute>
        <div class="doc"><xsl:copy-of select="."/></div>
      </div>
    </div>
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="arg">
    <xsl:param name="ctx" />
    <div class="container">
      <div class="sub-item-header">
        <xsl:attribute name="hidecontrol"><xsl:value-of select="$ctx" /></xsl:attribute>
      <code><xsl:value-of select="name"/></code></div>
      <div class="content">
        <xsl:attribute name="id"><xsl:value-of select="$ctx" /></xsl:attribute>
        <div class="doc"><xsl:copy-of select="doc/node()"/></div>
      </div>
    </div>
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="method-synopsis">
    <xsl:value-of select="name"/>
    (
    <xsl:for-each select="arg">
      <xsl:value-of select="name"/><xsl:if test="position() != last()">, </xsl:if>
    </xsl:for-each>
    )
  </xsl:template>

  <!-- ================================================== -->

  <xsl:template name="example">
    <xsl:param name="ctx" />
    <div class="container">
      <div class="item-header">
        <xsl:attribute name="hidecontrol"><xsl:value-of select="$ctx" /></xsl:attribute>
        <xsl:copy-of select="title/node()"/>
      </div>
      <div class="content">
        <xsl:attribute name="id"><xsl:value-of select="$ctx" /></xsl:attribute>
        <div class="doc">
          <xsl:copy-of select="doc/node()"/>
        </div>
      </div>
    </div>
  </xsl:template>

</xsl:stylesheet>
