// Copyright (C) 2012 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

var currentTestName = undefined;
var divId = 0;

function newChartDiv(opt_height) {

  debugger;

  var container = document.createElement('div');
  var label = document.createElement('div');
  var content = document.createElement('div');

  document.getElementById('chartdiv').appendChild(container);
  container.appendChild(label);
  container.appendChild(content);

  container.style.marginTop = '10px';

  label.innerHTML = currentTestName;
  label.style.margin = '5px';
  label.style.padding = '5px';
  label.style.fontFamily = 'Courier';
  label.style.fontWeight = 'bold';

  content.style.width = '600px';
  if (opt_height) { content.style.height = opt_height; }
  content.style.border = '1px solid black';
  content.style.backgroundColor = '#c3e3e3';
  content.style.margin = '5px';
  content.style.padding = '5px';
  content.setAttribute('id', 'chartDiv-' + divId++);

  if (window.isStandalone) {
    // attempt to duplicate the "inner opaque <div>" arrangement
    // of the Caja-sandboxed case
    var innerContent = document.createElement('div');
    content.appendChild(innerContent);
    innerContent.style.width = content.style.width;
    innerContent.style.height = content.style.height;
    innerContent.setAttribute('id', content.getAttribute('id') + '-inner');
    return innerContent;
  } else {
    return content;
  }
}

function assertTrue(cond, msg) {
  if (!cond) {
    log('<font color="orange">expected true but was false:' +
        ' (' + msg + ')</font>');
  }
}

function assertEquals(a, b, msg) {
  if (a !== b) {
    log('<font color="orange">expected \u00ab' + a + '\u00bb' +
        ' but found \u00ab' + b + '\u00bb (' + msg + ')</font>');
  }
}

google.load(
  'visualization', 
  '1.0',
  {
    packages: [
      'corechart',
    ]
  });

google.setOnLoadCallback(function() {

  // Ensure that 'corechart' has been loaded at this point
  assertTrue(!!google.visualization.DataTable, 'DataTable ctor is loaded');
  // But ensure that other stuff is not *yet* loaded
  assertTrue(!google.visualization.Gauge, 'Gauge ctor is NOT loaded');

  // Now load the remainder of what we need, and run our tests
  google.load(
    'visualization', 
    '1.0',
    {
      packages: [
        'corechart',
        'gauge',
        'geochart',
        'table',
        'treemap',
        'annotatedtimeline',
        'geomap',
        'intensitymap',
        'orgchart',
        'map',
        'motionchart',
        'controls',
        'charteditor'
      ],
      callback: runtests
    });
});

function runtests() {
  for (var i = 0; i < tests.length; i++) {
//    if (!/testGeoChart.*/.test(tests[i].name)) { continue; }
    if (currentTestName) {
      throw 'Test name expected empty; found ' + currentTestName;
    }
    currentTestName = tests[i].name;
    try {
      log('<strong>start test ' + tests[i].name + '</strong>');
      tests[i]();
      log('<font color="green">test ' + tests[i].name + ' done without throwing</font>');
    } catch (e) {
      log('<font color="red">test ' + tests[i].name + ' threw ' + e + ' - ' + e.stack + '</font>');
    } finally {
      currentTestName = undefined;
    }
  }
}

var tests = [];

tests.push(function testAreaChart() {
  var data = google.visualization.arrayToDataTable([
    ['Year', 'Sales', 'Expenses'],
    ['2004',  1000,      400],
    ['2005',  1170,      460],
    ['2006',  660,       1120],
    ['2007',  1030,      540]
  ]);

  var options = {
    title: 'Company Performance',
    hAxis: {title: 'Year',  titleTextStyle: {color: 'red'}}
  };

  var chart = new google.visualization.AreaChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testBarChart() {
  var data = google.visualization.arrayToDataTable([
    ['Year', 'Sales', 'Expenses'],
    ['2004',  1000,      400],
    ['2005',  1170,      460],
    ['2006',  660,       1120],
    ['2007',  1030,      540]
  ]);

  var options = {
    title: 'Company Performance',
    vAxis: {title: 'Year',  titleTextStyle: {color: 'red'}}
  };

  var chart = new google.visualization.BarChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testBubbleChart() {
  var data = google.visualization.arrayToDataTable([
    ['ID', 'Life Expectancy', 'Fertility Rate', 'Region',     'Population'],
    ['CAN',    80.66,              1.67,      'North America',  33739900],
    ['DEU',    79.84,              1.36,      'Europe',         81902307],
    ['DNK',    78.6,               1.84,      'Europe',         5523095],
    ['EGY',    72.73,              2.78,      'Middle East',    79716203],
    ['GBR',    80.05,              2,         'Europe',         61801570],
    ['IRN',    72.49,              1.7,       'Middle East',    73137148],
    ['IRQ',    68.09,              4.77,      'Middle East',    31090763],
    ['ISR',    81.55,              2.96,      'Middle East',    7485600],
    ['RUS',    68.6,               1.54,      'Europe',         141850000],
    ['USA',    78.09,              2.05,      'North America',  307007000]
  ]);

  var options = {
    title: 'Correlation between life expectancy, fertility rate and population of some world countries (2010)',
    hAxis: {title: 'Life Expectancy'},
    vAxis: {title: 'Fertility Rate'},
    bubble: {textStyle: {fontSize: 11}}
  };

  var chart = new google.visualization.BubbleChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testCandlestickChart() {
  var data = google.visualization.arrayToDataTable([
    ['Mon', 20, 28, 38, 45],
    ['Tue', 31, 38, 55, 66],
    ['Wed', 50, 55, 77, 80],
    ['Thu', 77, 77, 66, 50],
    ['Fri', 68, 66, 22, 15]
    // Treat first row as data as well.
  ], true);

  var options = {
    legend:'none'
  };

  var chart = new google.visualization.CandlestickChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testColumnChart() {
  var data = google.visualization.arrayToDataTable([
    ['Year', 'Sales', 'Expenses'],
    ['2004',  1000,      400],
    ['2005',  1170,      460],
    ['2006',  660,       1120],
    ['2007',  1030,      540]
  ]);

  var options = {
    title: 'Company Performance',
    hAxis: {title: 'Year', titleTextStyle: {color: 'red'}}
  };

  var chart = new google.visualization.ColumnChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testComboChart() {
  // Some raw data (not necessarily accurate)
  var data = google.visualization.arrayToDataTable([
    ['Month', 'Bolivia', 'Ecuador', 'Madagascar', 'Papua New Guinea', 'Rwanda', 'Average'],
    ['2004/05',  165,      938,         522,             998,           450,      614.6],
    ['2005/06',  135,      1120,        599,             1268,          288,      682],
    ['2006/07',  157,      1167,        587,             807,           397,      623],
    ['2007/08',  139,      1110,        615,             968,           215,      609.4],
    ['2008/09',  136,      691,         629,             1026,          366,      569.6]
  ]);

  var options = {
    title : 'Monthly Coffee Production by Country',
    vAxis: {title: "Cups"},
    hAxis: {title: "Month"},
    seriesType: "bars",
    series: {5: {type: "line"}}
  };

  var chart = new google.visualization.ComboChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testGaugeChart() {
  var data = google.visualization.arrayToDataTable([
    ['Label', 'Value'],
    ['Memory', 80],
    ['CPU', 55],
    ['Network', 68]
  ]);

  var options = {
    width: 400, height: 120,
    redFrom: 90, redTo: 100,
    yellowFrom:75, yellowTo: 90,
    minorTicks: 5
  };

  var chart = new google.visualization.Gauge(newChartDiv());
  chart.draw(data, options);
});


tests.push(function testGeoChartRegions() {
  var data = google.visualization.arrayToDataTable([
    ['Country', 'Popularity'],
    ['Germany', 200],
    ['United States', 300],
    ['Brazil', 400],
    ['Canada', 500],
    ['France', 600],
    ['RU', 700]
  ]);

  var options = {};

  var chart = new google.visualization.GeoChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testGeoChartMarkers() {
  var data = google.visualization.arrayToDataTable([
    ['City',  'Population', 'Area'],
    ['Rome',     2761477,    1285.31],
    ['Milan',    1324110,    181.76],
    ['Naples',   959574,     117.27],
    ['Turin',    907563,     130.17],
    ['Palermo',  655875,     158.9],
    ['Genoa',    607906,     243.60],
    ['Bologna',  380181,     140.7],
    ['Florence', 371282,     102.41]
  ]);

  var options = {
    region: 'IT',
    displayMode: 'markers',
    colorAxis: {colors: ['green', 'blue']}
  };

  var chart = new google.visualization.GeoChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testLineChart() {
  var data = google.visualization.arrayToDataTable([
    ['Year', 'Sales', 'Expenses'],
    ['2004',  1000,      400],
    ['2005',  1170,      460],
    ['2006',  660,       1120],
    ['2007',  1030,      540]
  ]);

  var options = {
    title: 'Company Performance'
  };

  var chart = new google.visualization.LineChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testPieChart() {
  var data = google.visualization.arrayToDataTable([
    ['Task', 'Hours per Day'],
    ['Work',     11],
    ['Eat',      2],
    ['Commute',  2],
    ['Watch TV', 2],
    ['Sleep',    7]
  ]);

  var options = {
    title: 'My Daily Activities'
  };

  var chart = new google.visualization.PieChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testScatterChart() {
  var data = google.visualization.arrayToDataTable([
    ['Age', 'Weight'],
    [ 8,      12],
    [ 4,      5.5],
    [ 11,     14],
    [ 4,      5],
    [ 3,      3.5],
    [ 6.5,    7]
  ]);

  var options = {
    title: 'Age vs. Weight comparison',
    hAxis: {title: 'Age', minValue: 0, maxValue: 15},
    vAxis: {title: 'Weight', minValue: 0, maxValue: 15},
    legend: 'none'
  };

  var chart = new google.visualization.ScatterChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testSteppedAreaChart() {
  var data = google.visualization.arrayToDataTable([
    ['Director (Year)',  'Rotten Tomatoes', 'IMDB'],
    ['Alfred Hitchcock (1935)', 8.4,         7.9],
    ['Ralph Thomas (1959)',     6.9,         6.5],
    ['Don Sharp (1978)',        6.5,         6.4],
    ['James Hawes (2008)',      4.4,         6.2]
  ]);

  var options = {
    title: 'The decline of \'The 39 Steps\'',
    vAxis: {title: 'Accumulated Rating'},
    isStacked: true
  };

  var chart = new google.visualization.SteppedAreaChart(newChartDiv());
  chart.draw(data, options);
});

tests.push(function testTable() {
  var data = new google.visualization.DataTable();
  data.addColumn('string', 'Name');
  data.addColumn('number', 'Salary');
  data.addColumn('boolean', 'Full Time Employee');
  data.addRows([
    ['Mike',  {v: 10000, f: '$10,000'}, true],
    ['Jim',   {v:8000,   f: '$8,000'},  false],
    ['Alice', {v: 12500, f: '$12,500'}, true],
    ['Bob',   {v: 7000,  f: '$7,000'},  true]
  ]);

  var table = new google.visualization.Table(newChartDiv());
  table.draw(data, {showRowNumber: true});
});

tests.push(function testTreeMap() {
  // Create and populate the data table.
  var data = google.visualization.arrayToDataTable([
    ['Location', 'Parent', 'Market trade volume (size)', 'Market increase/decrease (color)'],
    ['Global',    null,                 0,                               0],
    ['America',   'Global',             0,                               0],
    ['Europe',    'Global',             0,                               0],
    ['Asia',      'Global',             0,                               0],
    ['Australia', 'Global',             0,                               0],
    ['Africa',    'Global',             0,                               0],
    ['Brazil',    'America',            11,                              10],
    ['USA',       'America',            52,                              31],
    ['Mexico',    'America',            24,                              12],
    ['Canada',    'America',            16,                              -23],
    ['France',    'Europe',             42,                              -11],
    ['Germany',   'Europe',             31,                              -2],
    ['Sweden',    'Europe',             22,                              -13],
    ['Italy',     'Europe',             17,                              4],
    ['UK',        'Europe',             21,                              -5],
    ['China',     'Asia',               36,                              4],
    ['Japan',     'Asia',               20,                              -12],
    ['India',     'Asia',               40,                              63],
    ['Laos',      'Asia',               4,                               34],
    ['Mongolia',  'Asia',               1,                               -5],
    ['Israel',    'Asia',               12,                              24],
    ['Iran',      'Asia',               18,                              13],
    ['Pakistan',  'Asia',               11,                              -52],
    ['Egypt',     'Africa',             21,                              0],
    ['S. Africa', 'Africa',             30,                              43],
    ['Sudan',     'Africa',             12,                              2],
    ['Congo',     'Africa',             10,                              12],
    ['Zair',      'Africa',             8,                               10]
  ]);

  // Create and draw the visualization.
  var tree = new google.visualization.TreeMap(newChartDiv());
  tree.draw(data, {
    minColor: '#f00',
    midColor: '#ddd',
    maxColor: '#0d0',
    headerHeight: 15,
    fontColor: 'black',
    showScale: true});
});

tests.push(function testAnnotatedTimeLine() {
  var data = new google.visualization.DataTable();
  data.addColumn('date', 'Date');
  data.addColumn('number', 'Sold Pencils');
  data.addColumn('string', 'title1');
  data.addColumn('string', 'text1');
  data.addColumn('number', 'Sold Pens');
  data.addColumn('string', 'title2');
  data.addColumn('string', 'text2');
  data.addRows([
    [new Date(2008, 1 ,1), 30000, undefined, undefined, 40645, undefined, undefined],
    [new Date(2008, 1 ,2), 14045, undefined, undefined, 20374, undefined, undefined],
    [new Date(2008, 1 ,3), 55022, undefined, undefined, 50766, undefined, undefined],
    [new Date(2008, 1 ,4), 75284, undefined, undefined, 14334, 'Out of Stock','Ran out of stock on pens at 4pm'],
    [new Date(2008, 1 ,5), 41476, 'Bought Pens','Bought 200k pens', 66467, undefined, undefined],
    [new Date(2008, 1 ,6), 33322, undefined, undefined, 39463, undefined, undefined]
  ]);

  var chart = new google.visualization.AnnotatedTimeLine(newChartDiv('400px'));
  chart.draw(data, {displayAnnotations: true});

  var table = new google.visualization.Table(newChartDiv());
  table.draw(data, {showRowNumber: true});
});

tests.push(function testGeoMap() {
 var data = google.visualization.arrayToDataTable([
    ['Country', 'Popularity'],
    ['Germany', 200],
    ['United States', 300],
    ['Brazil', 400],
    ['Canada', 500],
    ['France', 600],
    ['RU', 700]
  ]);

  var options = {};
  options['dataMode'] = 'regions';

  var geomap = new google.visualization.GeoMap(newChartDiv());
  geomap.draw(data, options);
});

tests.push(function testIntensityMap() {
 var data = google.visualization.arrayToDataTable([
    ['Country', 'Population (mil)', 'Area (km2)'],
    ['CN',            1324,           9640821],
    ['IN',            1133,           3287263],
    ['US',            304,            9629091],
    ['ID',            232,            1904569],
    ['BR',            187,            8514877]
  ]);

  var chart = new google.visualization.IntensityMap(newChartDiv());
  chart.draw(data, {});
});

tests.push(function testOrgChart() {
  var data = new google.visualization.DataTable();
  data.addColumn('string', 'Name');
  data.addColumn('string', 'Manager');
  data.addColumn('string', 'ToolTip');
  data.addRows([
    [{v:'Mike', f:'Mike<div style="color:red; font-style:italic">President</div>'}, '', 'The President'],
    [{v:'Jim', f:'Jim<div style="color:red; font-style:italic">Vice President</div>'}, 'Mike', 'VP'],
    ['Alice', 'Mike', ''],
    ['Bob', 'Jim', 'Bob Sponge'],
    ['Carol', 'Bob', '']
  ]);
  var chart = new google.visualization.OrgChart(newChartDiv());
  chart.draw(data, {allowHtml:true});
});

// TODO(ihab.awad): See visualizationMetadata.js
//tests.push(function testMap() {
//  var data = google.visualization.arrayToDataTable([
//    ['Lat', 'Lon', 'Name'],
//    [37.4232, -122.0853, 'Work'],
//    [37.4289, -122.1697, 'University'],
//    [37.6153, -122.3900, 'Airport'],
//    [37.4422, -122.1731, 'Shopping']
//  ]);
//
//  var map = new google.visualization.Map(newChartDiv());
//  map.draw(data, {showTip: true});
//});

tests.push(function testMotionChart() {
  var data = new google.visualization.DataTable();
  data.addColumn('string', 'Fruit');
  data.addColumn('date', 'Date');
  data.addColumn('number', 'Sales');
  data.addColumn('number', 'Expenses');
  data.addColumn('string', 'Location');
  data.addRows([
    ['Apples',  new Date (1988,0,1), 1000, 300, 'East'],
    ['Oranges', new Date (1988,0,1), 1150, 200, 'West'],
    ['Bananas', new Date (1988,0,1), 300,  250, 'West'],
    ['Apples',  new Date (1989,6,1), 1200, 400, 'East'],
    ['Oranges', new Date (1989,6,1), 750,  150, 'West'],
    ['Bananas', new Date (1989,6,1), 788,  617, 'West']
  ]);
  var chart = new google.visualization.MotionChart(newChartDiv());
  chart.draw(data, {width: 600, height:300});
});

tests.push(function testDashboard() {
  var data = google.visualization.arrayToDataTable([
    ['Name', 'Gender', 'Age', 'Donuts eaten'],
    ['Michael' , 'Male', 12, 5],
    ['Elisa', 'Female', 20, 7],
    ['Robert', 'Male', 7, 3],
    ['John', 'Male', 54, 2],
    ['Jessica', 'Female', 22, 6],
    ['Aaron', 'Male', 3, 1],
    ['Margareth', 'Female', 42, 8],
    ['Miranda', 'Female', 33, 6]
  ]);

  // Define a slider control for the Age column.
  var slider = new google.visualization.ControlWrapper({
    'controlType': 'NumberRangeFilter',
    'containerId': newChartDiv().getAttribute('id'),
    'options': {
      'filterColumnLabel': 'Age',
    'ui': {'labelStacking': 'vertical'}
    }
  });

  // Define a category picker control for the Gender column
  var categoryPicker = new google.visualization.ControlWrapper({
    'controlType': 'CategoryFilter',
    'containerId': newChartDiv().getAttribute('id'),
    'options': {
      'filterColumnLabel': 'Gender',
      'ui': {
      'labelStacking': 'vertical',
        'allowTyping': false,
        'allowMultiple': false
      }
    }
  });

  // Define a Pie chart
  var pie = new google.visualization.ChartWrapper({
    'chartType': 'PieChart',
    'containerId': newChartDiv().getAttribute('id'),
    'options': {
      'width': 300,
      'height': 300,
      'legend': 'none',
      'title': 'Donuts eaten per person',
      'chartArea': {'left': 15, 'top': 15, 'right': 0, 'bottom': 0},
      'pieSliceText': 'label'
    },
    // Instruct the piechart to use colums 0 (Name) and 3 (Donuts Eaten)
    // from the 'data' DataTable.
    'view': {'columns': [0, 3]}
  });

  // Define a table
  var table = new google.visualization.ChartWrapper({
    'chartType': 'Table',
    'containerId': newChartDiv().getAttribute('id'),
    'options': {
      'width': '300px'
    }
  });

  // Create a dashboard
  var d = new google.visualization.Dashboard(newChartDiv());
  // Establish bindings, declaring the both the slider and the category
  // picker will drive both charts.
  d.bind([slider, categoryPicker], [pie, table]);
  // Draw the entire dashboard.
  d.draw(data);
});

tests.push(function testDataTable() {
  var data = new google.visualization.DataTable({
    cols: [
      {id: 'A', label: 'A-label', type: 'string'},
      {id: 'B', label: 'B-label', type: 'number'},
      {id: 'C', label: 'C-label', type: 'date'}
    ],
    rows: [
      {c:[{v: 'a'}, {v: 1.0, f: 'One'}, {v: new Date(2008, 1, 28, 0, 31, 26), f: '2/28/08 12:31 AM'}]},
      {c:[{v: 'b'}, {v: 2.0, f: 'Two'}, {v: new Date(2008, 2, 30, 0, 31, 26), f: '3/30/08 12:31 AM'}]},
      {c:[{v: 'c'}, {v: 3.0, f: 'Three'}, {v: new Date(2008, 3, 30, 0, 31, 26), f: '4/30/08 12:31 AM'}]}
    ],
    p: {foo: 'hello', bar: 'world!'}
  });

  data.addColumn('boolean', 'D-label', 'D');
  data.addColumn({id: 'E', label: 'E-label', type: 'number', pattern: 'E{0}E'});

  data.addRow(['anE', 42, {v: new Date(), f: 'Right Now'}, false, 33]);
  data.addRows([
    ['anE', 42, {v: new Date(), f: 'Right Now'}, false, 33],
    ['anE', 42, {v: new Date(), f: 'Right Now'}, false, 33]
  ]);

  data.addRows(2);

  assertEquals('a', data.getValue(0, 0), 'value 0, 0');
  assertEquals(null, data.getValue(7, 0), 'value 7, 0')

  assertEquals('A', data.getColumnId(0));
  assertEquals('D', data.getColumnId(3));

  assertEquals('A-label', data.getColumnLabel(0), 'column label 0');
  assertEquals('D-label', data.getColumnLabel(3), 'column label 3');

  assertEquals(undefined, data.getColumnPattern(0), 'column pattern 0');
  assertEquals('E{0}E', data.getColumnPattern(4), 'column pattern 4');

  // Does not work because taming layer can't virtualize property creation
  // data.getColumnProperties(3).foo = 'foovalue';
  data.setColumnProperty(3, 'foo', 'foovalue');
  assertEquals('foovalue', data.getColumnProperty(3, 'foo'), 'column property 3, foo');
  assertEquals('foovalue', data.getColumnProperties(3).foo, 'column properties 3 . foo');
  data.setColumnProperties(3, { foo: 'barvalue' });
  assertEquals('barvalue', data.getColumnProperty(3, 'foo'), 'column property 3, foo');
  assertEquals('barvalue', data.getColumnProperties(3).foo, 'column properties 3 . foo');

  // Does not work because taming layer can't virtualize property creation
  // data.getRowProperties(3).bar = 'barvalue';
  data.setRowProperty(3, 'bar', 'barvalue');
  assertEquals('barvalue', data.getRowProperty(3, 'bar'), 'row property 3, bar');
  assertEquals('barvalue', data.getRowProperties(3).bar, 'row properties 3 . bar');
  data.setRowProperties(3, { bar: 'foovalue' });
  assertEquals('foovalue', data.getRowProperty(3, 'bar'), 'row property 3, bar');
  assertEquals('foovalue', data.getRowProperties(3).bar, 'row properties 3 . bar');

  // Does not work because taming layer can't virtualize property creation
  // data.getProperties(3, 3).baz = 'bazvalue';
  data.setProperty(3, 3, 'baz', 'bazvalue');
  assertEquals('bazvalue', data.getProperty(3, 3, 'baz'), 'property 3, 3, baz');
  assertEquals('bazvalue', data.getProperties(3, 3).baz, 'properties 3, 3 . baz');
  data.setProperties(3, 3, { baz: 'boovalue' });
  assertEquals('boovalue', data.getProperty(3, 3, 'baz'), 'property 3, 3, baz');
  assertEquals('boovalue', data.getProperties(3, 3).baz, 'properties 3, 3 . baz');

  assertEquals(33, data.getColumnRange(4).min, 'column range 4 . min');
  assertEquals(33, data.getColumnRange(4).max, 'column range 4 . max');

  assertEquals('string', data.getColumnType(0), 'column 0 type');
  assertEquals('number', data.getColumnType(1), 'column 1 type');

  assertEquals(1, data.getFilteredRows([{column: 0, value: 'c'}]).length, 'filtered rows length');
  assertEquals(2, data.getFilteredRows([{column: 0, value: 'c'}])[0], 'the 0th filtered row index');

  assertEquals(5, data.getNumberOfColumns(), 'number of columns');
  assertEquals(8, data.getNumberOfRows(), 'number of rows');

  assertEquals('string', data.getColumnType(0), 'column 0 type');

  assertEquals(5, data.getDistinctValues(0).length, 'column 0 # of distinct values');
  assertEquals(null, data.getDistinctValues(0)[0], 'column 0 distinct value 0');
  assertEquals('a', data.getDistinctValues(0)[1], 'column 0 distinct value 1');

  var fr = data.getFilteredRows([
    { column: 0, minValue: 'a', maxValue: 'b' },
    { column: 1, minValue: 2.0, maxValue: 3.0 }
  ]);
  assertEquals(1, fr.length, 'number of filtered rows');
  assertEquals(1, fr[0], '0th filtered row number');

  var sr = data.getSortedRows([
    { column: 0, desc: false }
  ]);
  assertEquals(8, sr.length, 'number of sorted rows');
  assertEquals(6, sr[0], '0th sorted row number');
  assertEquals(7, sr[1], '1th sorted row number');
  assertEquals(0, sr[2], '2th sorted row number');

  assertEquals('2/28/08 12:31 AM', data.getFormattedValue(0, 2), 'formatted value 0,2');

  data.setTableProperty('foo', 33);
  assertEquals(33, data.getTableProperty('foo'), 'table property foo');
  data.setTableProperties({ bar: 99  });
  assertEquals(99, data.getTableProperties().bar, 'table property bar');

  data.setCell(0, 0, 'xanh', 'xanhfmt', { xx: 42, yy: 13 });
  assertEquals('xanh', data.getValue(0, 0), 'value 0, 0')
  assertEquals('xanhfmt', data.getFormattedValue(0, 0), 'formatted value 0, 0');
  assertEquals(42, data.getProperties(0, 0).xx, 'property 0, 0, xx');

  data.setColumnLabel(0, 'col0lbl');
  assertEquals('col0lbl', data.getColumnLabel(0), 'col 0 label');

  data.setValue(0, 0, 'abba');
  assertEquals('abba', data.getValue(0, 0), 'value 0, 0');

  // ** Assumed to work based on the above **
  // data.insertColumn(columnIndex, type, [, label [, id]])
  // data.insertRows(rowIndex, numberOrArray)
  // data.removeColumn(columnIndex)
  // data.removeColumns(columnIndex, numberOfColumns)
  // data.removeRow(rowIndex)
  // data.removeRows(rowIndex, numberOfRows)
  // data.setFormattedValue(rowIndex, columnIndex, formattedValue)
  // data.sort(sortColumns)

  // TODO(ihab.awad): This fails in ES5/3 with a stack overflow.
  //
  // var data2 = data.clone();
  // assertEquals(5, data2.getNumberOfColumns(), 'data2 number of columns');
  // assertEquals(8, data2.getNumberOfRows(), 'data2 number of rows');
  //
  // var j = data.toJSON();
  // assertTrue(new RegExp('\{\"cols\":.*').test(j), 'data json');
  // var data3 = new google.visualization.DataTable(j);
  // assertEquals(5, data3.getNumberOfColumns(), 'data 3 number of cols');
  // assertEquals(8, data3.getNumberOfRows(), 'data 3 number of rows');
});

tests.push(function testChartWrapper() {
  // Draw a column chart
  var wrapper = new google.visualization.ChartWrapper({
    chartType: 'ColumnChart',
    dataTable: [['Germany', 'USA', 'Brazil', 'Canada', 'France', 'RU'],
                [700, 300, 400, 500, 600, 800]],
    options: {'title': 'Countries'},
    containerId: newChartDiv().getAttribute('id')
  });

  // Never called.
  google.visualization.events.addListener(wrapper, 'onmouseover', uselessHandler);

  // Must wait for the ready event in order to
  // request the chart and subscribe to 'onmouseover'.
  google.visualization.events.addListener(wrapper, 'ready', onReady);

  wrapper.draw();

  // Never called
  function uselessHandler() {
    log("I am never called!");
  }

  function onReady() {
    google.visualization.events.addListener(wrapper.getChart(), 'onmouseover', usefulHandler);
  }

  // Called
  function usefulHandler() {
    log("Mouseover event!");
  }
});

tests.push(function testChartEditor() {
  var chartEditor = null;
  var chartDiv = newChartDiv();

  // Create the chart to edit.
  var wrapper = new google.visualization.ChartWrapper({
     'chartType':'LineChart',
     'dataSourceUrl':'http://spreadsheets.google.com/tq?key=pCQbetd-CptGXxxQIG7VFIQ&pub=1',
     'query':'SELECT A,D WHERE D > 100 ORDER BY D',
     'options': {'title':'Population Density (people/km^2)', 'legend':'none'}
     });

  chartEditor = new google.visualization.ChartEditor();
  google.visualization.events.addListener(chartEditor, 'ok', redrawChart);

  // On "OK" save the chart to a <div> on the page.
  function redrawChart(){
    chartEditor.getChartWrapper().draw(chartDiv);
  }

  var btn = document.createElement('input');
  btn.setAttribute('type', 'button');
  btn.setAttribute('value', 'Edit');
  btn.onclick = function() {
    chartEditor.openDialog(wrapper, {});
  };

  chartDiv.appendChild(btn);
});

tests.push(function testDataView() {
  var data = new google.visualization.arrayToDataTable([
    ['A', 'B'],
    [  0,   3],
    [  1,   2],
    [  3,   0]
  ]);
  var view = new google.visualization.DataView(data);
  view.setRows([0, 2]);

  assertEquals(2, view.getNumberOfRows(), 'view num of rows');
  assertEquals(0, view.getValue(0, 0), 'view value 0, 0');
  assertEquals(3, view.getValue(1, 0), 'view value 1, 0');


  view = google.visualization.DataView.fromJSON(data, view.toJSON());

  assertEquals(2, view.getNumberOfRows(), 'view num of rows');
  assertEquals(0, view.getValue(0, 0), 'view value 0, 0');
  assertEquals(3, view.getValue(1, 0), 'view value 1, 0');
});

tests.push(function testQuery() {
  // To see the data that this visualization uses, browse to
  // http://spreadsheets.google.com/ccc?key=pCQbetd-CptGXxxQIG7VFIQ
  var query = new google.visualization.Query(
      'http://spreadsheets.google.com/tq?key=pCQbetd-CptGXxxQIG7VFIQ&pub=1');

  // Apply query language.
  query.setQuery('SELECT A,D WHERE D > 100 ORDER BY D');

  // Send the query with a callback function.
  query.send(handleQueryResponse);

  function handleQueryResponse(response) {
    if (response.isError()) {
      log('Error in query: ' + response.getMessage() + ' ' + response.getDetailedMessage());
      return;
    }

    var data = response.getDataTable();
    var visualization = new google.visualization.LineChart(newChartDiv());
    visualization.draw(data, {legend: 'bottom'});
  }
});

tests.push(function testFormatters() {
  var data = new google.visualization.DataTable();

  data.addColumn('number', 'Arrow');
  data.addColumn('number', 'Bar');
  data.addColumn('number', 'Color range');
  data.addColumn('number', 'Color gradient');
  data.addColumn('date', 'Date');
  data.addColumn('number', 'Number');
  data.addColumn('string', 'Pattern');

  data.addRows([
    [ +4, +250,  50,  50, new Date(1977, 1, 1), -1234.87654321, '' ],
    [  1,  -50, 150, 150, new Date(1978, 2, 2),  1234.87654321, '' ],
    [ +3, +200, 250, 250, new Date(1979, 3, 3), -8765.12345678, '' ],
    [  0,    0, 350, 350, new Date(1980, 4, 4),  8765.12345678, '' ],
  ]);

  new google.visualization.ArrowFormat({
        base: 2
      })
      .format(data, 0);
  new google.visualization.BarFormat({
        base: 100,
        colorNegative: 'blue',
        colorPositive: 'red',
        drawZeroLine: true,
        min: -100,
        max: +300,
        showValue: false,
        width: 50
      })
      .format(data, 1);
  var cf1 = new google.visualization.ColorFormat();
  cf1.addRange(100, 300, '#9C2542', '#A1CAF1');
  cf1.format(data, 2);
  var cf2 = new google.visualization.ColorFormat();
  cf2.addGradientRange(0, 400, '#9C2542', '#ff0000', '#ffffff');
  cf2.format(data, 3);
  new google.visualization.DateFormat({
        formatType: 'medium',
        timeZone: -5
      })
      .format(data, 4);
  new google.visualization.NumberFormat({
        decimalSymbol: ',',
        fractionDigits: 3,
        groupingSymbol: '.',
        negativeColor: 'red',
        negativeParens: false,
        prefix: '\'Bout ',
        suffix: ' big \'uns.'
      })
      .format(data, 5);
  new google.visualization.PatternFormat(
        '<strong>{0}</strong> for <em>{2}</em>!<script>alert(42);</script>' +
        '<br>Gefurfifier!')
      .format(data, [0, 1, 2, 3, 4, 5], 6);

  var table = new google.visualization.Table(newChartDiv());
  table.draw(data, {showRowNumber: true, allowHtml: true });
});

tests.push(function testEvents() {
  var str = '';
  var src = new google.visualization.DataTable();

  var la = google.visualization.events.addListener(src, 'ping', function(e) {
    str += 'a' + e.data;
  });
  var lb = google.visualization.events.addListener(src, 'ping', function(e) {
    str += 'b' + e.data;
  });

  google.visualization.events.trigger(src, 'ping', { data: 'x' });
  google.visualization.events.removeListener(la);
  google.visualization.events.trigger(src, 'ping', { data: 'y' });
  google.visualization.events.removeAllListeners(src);
  google.visualization.events.trigger(src, 'ping', { data: 'z' });

  assertEquals('axbxby', str, 'event results');
});

tests.push(function testErrors() {
  var c = newChartDiv();

  function add() {
    return [
      google.visualization.errors.addError(c, 'e0 text', 'e0 tooltip', {
          showInTooltip: false,
          type: 'error',
          style: 'background-color: #0000ff; color: #000000;',
          removable: true
        }),
      google.visualization.errors.addError(c, 'e1 text', 'e1 tooltip')
    ];
  }

  add();
  google.visualization.errors.removeAll(c);
  var e = add();
  google.visualization.errors.removeError(e[0]);
  assertEquals(c, google.visualization.errors.getContainer(e[1]), 'e1 error container');
  add();
});

tests.push(function testDrawChart() {
  google.visualization.drawChart({
      'containerId': newChartDiv().getAttribute('id'),
      'dataSourceUrl': 'https://spreadsheets.google.com/a/google.com/tq?key=pCQbetd-CptGXxxQIG7VFIQ&pub=1',
      'query':'SELECT A,D WHERE D > 100 ORDER BY D',
      'refreshInterval': 5,
      'chartType': 'Table',
      'options': {
         'alternatingRowStyle': true,
         'showRowNumber' : true
      }
    });
});

tests.push(function testData() {
  var data;

  data = google.visualization.data.group(
    google.visualization.arrayToDataTable([
      ['a', 'b'],
      [ +2,   1],
      [ +3,   2],
      [ -4,   3],
      [ -5,   4]
    ]),
    [{
      column: 0,
      modifier: function(x) { return x > 0; },
      type: 'boolean'
    }],
    [{
      column: 1,
      aggregation: google.visualization.data.sum,
      type: 'number'
    }]);

  assertEquals(2, data.getNumberOfRows());
  assertEquals(2, data.getNumberOfColumns());
  assertEquals(false, data.getValue(0, 0));
  assertEquals(true, data.getValue(1, 0));
  assertEquals(7, data.getValue(0, 1));
  assertEquals(3, data.getValue(1, 1));

  data = google.visualization.data.join(
    google.visualization.arrayToDataTable([
      ['k', 'b'],
      [  0,   1],
      [  1,   2],
      [  2,   3]
    ]),
    google.visualization.arrayToDataTable([
      ['k', 'b'],
      [  1,   4],
      [  2,   5],
      [  3,   6]
    ]),
    'inner',
    [ [0, 0] ],
    [1],
    [1]);
  assertEquals(2, data.getNumberOfRows());
  assertEquals(3, data.getNumberOfColumns());
  assertEquals(1, data.getValue(0, 0));
  assertEquals(2, data.getValue(1, 0));
  assertEquals(2, data.getValue(0, 1));
  assertEquals(3, data.getValue(1, 1));
  assertEquals(4, data.getValue(0, 2));
  assertEquals(5, data.getValue(1, 2));

  assertEquals(5, google.visualization.data.month(new Date(Date.parse('May 20, 1993'))));
  assertEquals(2, google.visualization.data.avg([1, 3]));
  assertEquals(2, google.visualization.data.count(['a', 'b']));
  assertEquals(2, google.visualization.data.max([0, 2, 1]));
  assertEquals(2, google.visualization.data.min([3, 2, 4]));
  assertEquals(2, google.visualization.data.sum([-1, 3]));
});

tests.push(function testDataSourceUrl() {
  var o;

  o = new google.visualization.ChartWrapper({
      chartType:'LineChart',
      dataSourceUrl:'http://spreadsheets.google.com/tq?key=pCQbetd-CptGXxxQIG7VFIQ&pub=1'
    });
  assertEquals(
      'http://spreadsheets.google.com/tq?key=pCQbetd-CptGXxxQIG7VFIQ&pub=1',
      o.getDataSourceUrl(),
      'Chart wrapper non-null data source url');

  try {
    o = new google.visualization.ChartWrapper({
        chartType:'LineChart',
        dataSourceUrl:'http://evil.com/evil'
      });
    assertTrue(false, 'Evil URL rejected by policy');
  } catch (e) { /* pass */ }

  try {
    o = new google.visualization.ChartWrapper(JSON.stringify({
        chartType:'LineChart',
        dataSourceUrl:'http://evil.com/evil'
      }));
    assertTrue(false, 'Evil URL rejected by policy');
  } catch (e) { /* pass */ }

  o.setDataSourceUrl(
      'http://spreadsheets.google.com/tq?key=some-other-key&pub=1');
  assertEquals(
      'http://spreadsheets.google.com/tq?key=some-other-key&pub=1',
      o.getDataSourceUrl(),
      'Chart wrapper non-null data source url');
  try {
    o.setDataSourceUrl('http://evil.com/evil');
    assertTrue(false, 'Evil URL rejected by policy');
  } catch (e) { /* pass */ }

  // Should succeed
  new google.visualization.Query(
    'http://spreadsheets.google.com/tq?key=pCQbetd-CptGXxxQIG7VFIQ&pub=1');

  try {
    new google.visualization.Query('http://evil.com/evil');
    assertTrue(false, 'Evil URL rejected by policy');
  } catch (e) { /* pass */ }

  // Should succeed
  google.visualization.drawChart({
      'containerId': newChartDiv().getAttribute('id'),
      'dataSourceUrl': 'https://spreadsheets.google.com/a/google.com/tq?key=pCQbetd-CptGXxxQIG7VFIQ&pub=1',
      'chartType': 'Table'
    });

  try {
    google.visualization.drawChart({
        'containerId': newChartDiv().getAttribute('id'),
        'dataSourceUrl': 'http://evil.com/evil',
        'chartType': 'Table'
      });
    assertTrue(false, 'Evil URL rejected by policy');
  } catch (e) { /* pass */ }

  try {
    google.visualization.drawChart(JSON.stringify({
        'containerId': newChartDiv().getAttribute('id'),
        'dataSourceUrl': 'http://evil.com/evil',
        'chartType': 'Table'
      }));
    assertTrue(false, 'Evil URL rejected by policy');
  } catch (e) { /* pass */ }
});