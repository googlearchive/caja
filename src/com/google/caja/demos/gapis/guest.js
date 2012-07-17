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

function newChartDiv() {
  var d = document.createElement('div');
  document.getElementById('chartdiv').appendChild(d);
  return d;
}

function assertTrue(cond, msg) {
  if (!cond) {
    log('<font color="orange">expected true but was false: (' + msg + ')</font>');
  }
}

function assertEquals(a, b, msg) {
  if (a !== b) {
    log('<font color="orange">expected \u00ab' + a + '\u00bb but found \u00ab' + b + '\u00bb (' + msg + ')</font>');
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
        'treemap'
      ],
      callback: runtests
    });
});

function runtests() {
  for (var i = 0; i < tests.length; i++) {
    try {
      log('<strong>start test ' + tests[i].name + '</strong>');
      tests[i]();
      log('<font color="green">test ' + tests[i].name + ' done without throwing</font>');
    } catch (e) {
      log('<font color="red">test ' + tests[i].name + ' threw ' + e + '</font>');
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

tests.push(function newColumnChart() {
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

tests.push(function newComboChart() {
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


////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////

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

  // Does not work because taming layer can't virtualize property creation
  // data.getRowProperties(3).bar = 'barvalue';
  data.setRowProperty(3, 'bar', 'barvalue');
  assertEquals('barvalue', data.getRowProperty(3, 'bar'), 'row property 3, bar');
  assertEquals('barvalue', data.getRowProperties(3).bar, 'row properties 3 . bar');

  // Does not work because taming layer can't virtualize property creation
  // data.getProperties(3, 3).baz = 'bazvalue';
  data.setProperty(3, 3, 'baz', 'bazvalue');
  assertEquals('bazvalue', data.getProperty(3, 3, 'baz'), 'property 3, 3, baz');
  assertEquals('bazvalue', data.getProperties(3, 3).baz, 'properties 3, 3 . baz');

  assertEquals(33, data.getColumnRange(4).min, 'column range 4 . min');
  assertEquals(33, data.getColumnRange(4).max, 'column range 4 . max');

  assertEquals('domain', data.getColumnRole(0), 'column 0 role');
  assertEquals('data', data.getColumnRole(1), 'column 1 role');

  assertEquals('string', data.getColumnType(0), 'column 0 type');
  assertEquals('number', data.getColumnType(1), 'column 1 type');

  assertEquals(5, data.getDistinctValues(0).length, 'column 0 # of distinct values');
  assertEquals(null, data.getDistinctValues(0)[0], 'column 0 distinct value 0');
  assertEquals('a', data.getDistinctValues(0)[1], 'column 0 distinct value 1');

  assertEquals(1, data.getFilteredRows([{column: 0, value: 'c'}]).length, 'filtered rows length');
  assertEquals(2, data.getFilteredRows([{column: 0, value: 'c'}])[0], 'the 0th filtered row index');

  assertEquals('2/28/08 12:31 AM', data.getFormattedValue(0, 2), 'formatted value 0,2');

  assertEquals(5, data.getNumberOfColumns(), 'number of columns');
  assertEquals(8, data.getNumberOfRows(), 'number of rows');

  // var data2 = data.clone();

  // TODO(ihab.awad): Continue with DataTable testing

  var j = data.toJSON();
  log('json = ' + j);
});

tests.push(function testPicker() {
  // Create and render a Picker object for searching images.
  function createPicker() {
    var picker = new google.picker.PickerBuilder()
          .addView(google.picker.ViewId.DOCS)
          .addView(google.picker.ViewId.DOCS_IMAGES)
          .addView(google.picker.ViewId.DOCS_IMAGES_AND_VIDEOS)
          .addView(google.picker.ViewId.DOCS_VIDEOS)
          .addView(google.picker.ViewId.DOCUMENTS)
          .addView(google.picker.ViewId.FOLDERS)
          .addView(google.picker.ViewId.FORMS)
          .addView(google.picker.ViewId.IMAGE_SEARCH)
          .addView(google.picker.ViewId.PDFS)
          .addView(google.picker.ViewId.PHOTO_ALBUMS)
          .addView(google.picker.ViewId.PHOTO_UPLOAD)
          .addView(google.picker.ViewId.PHOTOS)
          .addView(google.picker.ViewId.PRESENTATIONS)
          .addView(google.picker.ViewId.RECENTLY_PICKED)
          .addView(google.picker.ViewId.SPREADSHEETS)
          .addView(google.picker.ViewId.VIDEO_SEARCH)
          .addView(google.picker.ViewId.WEBCAM)
          .addView(google.picker.ViewId.YOUTUBE)
          .setCallback(pickerCallback)
          .build();
    picker.setVisible(true);
  }

  // A simple callback implementation.
  function pickerCallback(data) {
    var url = 'nothing';
    if (data[google.picker.Response.ACTION] == google.picker.Action.PICKED) {
      var doc = data[google.picker.Response.DOCUMENTS][0];
      url = doc[google.picker.Document.URL];
    }
    log('You picked: ' + url);
  }

  google.load('picker', '1', {callback: createPicker});
});
