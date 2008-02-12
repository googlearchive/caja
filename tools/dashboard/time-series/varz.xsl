<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:math="http://exslt.org/math"
 extension-element-prefixes="math">

  <!-- Must be xhtml to embed SVG. -->
  <xsl:output
    method="xml"
    encoding="UTF-8"
    indent="yes"
    doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
    doctype-system="http://www.w3.org/1999/xhtml"
    media-type="application/xhtml+xml"
    />

  <xsl:template match="/">
    <xsl:apply-templates select="time-series"/>
  </xsl:template>

  <xsl:template match="time-series">
    <html>
      <head>
        <title>Time Series</title>
        <style type="text/css"><![CDATA[
          html { background: white }
          #chart { float: right }
          /* Make the graphs tile the available space. */
          .single-var-chart {
            float: left;
            display: inline;
            margin: 4px 4px 2em 4px
          }
        ]]></style>
      </head>
      <body>
        <h1 id="top">Time Series</h1>

        <p>These are VarZ variables extracted from build logs by looking for the
        pattern
          <tt>VarZ:<i>&lt;name&gt;</i>=<i>&lt;number&gt;</i></tt>
        in stderr and stdout.
        The build system keeps a historical record of values by name so that we
        can track performance over time.
        </p>

        <div id="chart">&#160;</div>

        <xsl:call-template name="index"/>

        <xsl:call-template name="graphs"/>
      </body>

      <!-- Load at end since it will get big -->
      <xsl:call-template name="series-script"/>
    </html>
  </xsl:template>

  <xsl:template name="index">
    <xsl:variable name="names"
     select="instant/varz/@name[not(.=following::instant/varz/@name)]"/>
    <div id="index">
      <h2>Index</h2>

      <!--
        - This form is examined by the charting code to render a chart using
        - SVG.  All checkboxes that don't start with $ are treated as varz
        - variable names.
       -->
      <form name="chartForm" onsubmit="return false">
        <ul>
          <xsl:for-each select="$names">
            <xsl:sort/>
            <li>
              <span>
                <label forname="{.}">
                  <a href="#{.}"><xsl:value-of select="."/></a>
                </label>
                <input type="checkbox" onclick="updateChart()" name="{.}"/>
              </span>
            </li>
          </xsl:for-each>
        </ul>
        <span title="Are values comparable across vars or does each's
                     range cover the whole y-axis.">
          <input type="checkbox" name="$sameAxis"
           onclick="updateChart()" checked="checked"/>
          <label forname="$sameAxis">Same Axis</label>
        </span>
        <button onclick="clearChartForm()">Clear</button>
      </form>
    </div>
  </xsl:template>

  <xsl:template name="graphs">
    <xsl:variable name="names"
     select="instant/varz/@name[not(.=following::instant/varz/@name)]"/>
    <div id="graphs">
      <h2>Graphs</h2>
      <xsl:for-each select="$names">
        <xsl:sort/>

        <xsl:variable name="name" select="."/>
        <xsl:variable name="samples"
         select="/time-series/instant/varz[@name=$name]"/>
        <xsl:variable name="sample_size" select="count($samples)"/>
        <xsl:variable name="values"
         select="/time-series/instant/varz[@name=$name]/@value"/>
        <xsl:variable name="min" select="math:min($values)"/>
        <xsl:variable name="max" select="math:max($values)"/>

        <xsl:variable name="width" select="500"/>
        <xsl:variable name="height" select="250"/>

        <!-- compute a range [y0, y1] centered on [min, max] -->
        <!-- s.t. y1 - y0 = max($max - $min, max(|$min| * .1, 1)) * 1.10 -->
        <xsl:variable name="slip">
          <xsl:choose>
            <xsl:when test="($max - $min) &gt; 0.1">
              <xsl:value-of select="($max - $min) * 0.1"/>
            </xsl:when>
            <xsl:when test="$min != 0">
              <xsl:value-of select="math:abs($min) * 0.1"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="1.0"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:variable name="y0" select="$min - ($slip * 0.5)"/>
        <xsl:variable name="y1" select="$max + ($slip * 0.5)"/>

        <div class="single-var-chart">
          <h3 id="{.}"><xsl:value-of select="."></xsl:value-of></h3>
          <div style="width: {$width}px; height: {$height}px">
            <svg width="{$width}px" height="{$height}px" version="1.1"
             viewBox="0 0 {$width} {$height}">
              <xsl:attribute name="xmlns"
               >http://www.w3.org/2000/svg</xsl:attribute>
              <rect x="1" y="1" width="{$width - 2}" height="{$height - 2}"
               fill="none" stroke="blue"/>
              <!-- Plot the values on the chart. -->
              <path stroke="red" stroke-width="2" fill="none">
                <xsl:attribute name="d">
                  <xsl:for-each select="$samples">
                    <xsl:choose>
                      <xsl:when test="position() = 1">
                        <xsl:text>M </xsl:text>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:text> L </xsl:text>
                      </xsl:otherwise>
                    </xsl:choose>
                    <xsl:value-of
                     select="round(($width * ((position() - 1)))
                                   div ($sample_size - 1))"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="round(
                        $height * (1 - (@value - $y0) div ($y1 - $y0)))"/>
                  </xsl:for-each>
                </xsl:attribute>
              </path>
              <!-- Add Legend for y-axis -->
              <text x="0" y="15" style="font-size:12px">
                <xsl:value-of select="$y1"/>
              </text>
              <text x="0" y="{$height * 0.5 + 6}" style="font-size:12px">
                <xsl:value-of select="($y1 + $y0) * 0.5"/>
              </text>
              <text x="0" y="{$height - 3}" style="font-size:12px">
                <xsl:value-of select="$y0"/>
              </text>
            </svg>
          </div>
        </div>
      </xsl:for-each>
    </div>
  </xsl:template>

  <xsl:template name="series-script">
    <xsl:variable name="names"
     select="instant/varz/@name[not(.=following::instant/varz/@name)]"/>

    <script type="text/javascript" src="charter.js"/>
    <script type="text/javascript">
      (function() {
        var series = {
          <xsl:for-each select="$names">
            <xsl:variable name="var-name" select="."/>
            <xsl:if test="position() != 1"><xsl:text>, </xsl:text></xsl:if>
            <xsl:call-template name="js-string">
              <xsl:with-param name="s" select="$var-name"/>
            </xsl:call-template>
            <xsl:text>: [</xsl:text>
            <xsl:for-each select="/time-series/instant/varz[@name=$var-name]">
              <xsl:sort select="parent::instant/@id"/>
              <xsl:if test="position() != 1"><xsl:text>, </xsl:text></xsl:if>
              <xsl:text>{ instant: </xsl:text>
              <xsl:call-template name="js-string">
                <xsl:with-param name="s" select="parent::instant/@id"/>
              </xsl:call-template>
              <xsl:text>, value: </xsl:text><xsl:value-of select="@value"/>
              <xsl:text> }</xsl:text>
            </xsl:for-each>
            <xsl:text>]</xsl:text>
          </xsl:for-each>
        };<![CDATA[

        var makeChart = charter(series);        

        /** Used to avoid rerendering the chart n times when changing n vars. */
        var adjusting = false;
        /** True iff updateChart was called while adjusting. */
        var updated = false;

        function getChartForm() { return document.forms.chartForm; }

        /** Redraw the chart and update styles on the inputs. */
        function updateChart() {
          if (adjusting) {
            updated = true;
            return;
          }

          var form = getChartForm();

          // Determine the names of variables to render.
          var sameAxis = form.elements.$sameAxis.checked;
          var vars = [];
          for (var i = form.elements.length; --i >= 0;) {
            var input = form.elements[i];
            if (!/^\$/.test(input.name) && input.checked) {
              vars.push(input.name);
            }
          }
          vars.sort();

          // Render the chart and inject it into the DOM.
          var chartContainer = document.getElementById('chart');
          var chartStyles = {};
          chartContainer.replaceChild(
              makeChart(
                  vars,
                  { width: 800,
                    height: 600,
                    sameAxis: sameAxis,
                    chartStyles: chartStyles }),
              chartContainer.firstChild);
          chartContainer.innerHTML = chartContainer.innerHTML;

          // Feed the styles back to the index.
          for (var i = form.elements.length; --i >= 0;) {
            var input = form.elements[i];
            var color
                = chartStyles[input.name] && chartStyles[input.name].color;
            var strokeWidth
                = chartStyles[input.name] && chartStyles[input.name].width;
            input.parentNode.style.borderLeft
                = color ? (strokeWidth + 'px solid ' + color) : '';
            input.parentNode.style.paddingLeft = color ? '2px' : '';
          }

          // Update the URL hash to match the scheme described below.
          try {
            var hash = vars.join(',');
            if (sameAxis) { hash += ',$sameAxis'; }
            document.location = '#chart-' + encodeURIComponent(hash);
          } catch (ex) {}
        }

        function setChartAdjusting(newAdjusting) {
          adjusting = newAdjusting;
          if (!adjusting && updated) {
            updated = false;
            updateChart();
          }
        }

        function clearChartForm() {
          var form = getChartForm();
          var inputs = [];
          for (var i = form.elements.length; --i >= 0;) {
            var input = form.elements[i];
            if (/^\$/.test(input.name) && input.checked) {
              inputs.push(input.name);
            }
          }
          setChartForm(inputs);
        }

        function setChartForm(inputs) {
          var inputSet = {};
          for (var i = inputs.length; --i >= 0;) { inputSet[inputs[i]] = null; }

          setChartAdjusting(true);
          try {
            var form = getChartForm();
            for (var i = form.elements.length; --i >= 0;) {
              var input = form.elements[i];
              input.checked = inputSet.hasOwnProperty(input.name);
            }
            updateChart();
          } finally {
            setChartAdjusting(false);
          }
        }

        this.clearChartForm = clearChartForm;
        this.setChartForm = setChartForm;
        this.updateChart = updateChart;
      })();

      if (document.location.hash) {
        // Examine the hash to determine whether or not form state is specified
        // in the URL fragment.
        // A fragment like "#chart-foo.bar,boo.baz" specifies that the variables
        // "foo.bar" and "boo.baz" should be plotted.
        (function () {
          var hash = decodeURIComponent(
              document.location.hash.replace(/^#/, ''));
          if (/^chart-/.test(hash)) {
            setChartForm(hash.replace(/^chart-/, '').split(','));
          }
        })();
        // If no variables are specified in the fragment, then allow the
        // browsers form autofill to go to town.
      }
      updateChart();
    ]]></script>
  </xsl:template>

  <xsl:template name="js-string">
    <xsl:param name="s"/>
    <xsl:text>'</xsl:text>
    <xsl:call-template name="js-string-body">
      <xsl:with-param name="s" select="$s"/>
    </xsl:call-template>
    <xsl:text>'</xsl:text>
  </xsl:template>

  <xsl:template name="js-string-body">
    <xsl:param name="s"/>

    <xsl:call-template name="replace">
      <xsl:with-param name="s">

        <xsl:call-template name="replace">
          <xsl:with-param name="s">

            <xsl:call-template name="replace">
              <xsl:with-param name="s">

                <xsl:call-template name="replace">
                  <xsl:with-param name="s" select="$s"/>
                  <xsl:with-param name="pattern" select="'\'"/>
                  <xsl:with-param name="repl" select="'\\'"/>
                </xsl:call-template>

              </xsl:with-param>
              <xsl:with-param name="pattern" select="&quot;'&quot;"/>
              <xsl:with-param name="repl" select="&quot;\'&quot;"/>
            </xsl:call-template>

          </xsl:with-param>
          <xsl:with-param name="pattern" select="&quot;&#10;&quot;"/>
          <xsl:with-param name="repl" select="&quot;\n&quot;"/>
        </xsl:call-template>
      </xsl:with-param>

      <xsl:with-param name="pattern" select="&quot;&#13;&quot;"/>
      <xsl:with-param name="repl" select="&quot;\r&quot;"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="replace">
    <xsl:param name="s"/>
    <xsl:param name="pattern"/>
    <xsl:param name="repl"/>
    <xsl:choose>
      <xsl:when test="contains($s, $pattern)">
        <xsl:value-of select="substring-before($s, $pattern)"/>
        <xsl:value-of select="$repl"/>
        <xsl:call-template name="replace">
          <xsl:with-param name="s">
            <xsl:value-of select="substring-after($s, $pattern)"/>
          </xsl:with-param>
          <xsl:with-param name="pattern" select="$pattern"/>
          <xsl:with-param name="repl" select="$repl"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$s"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
