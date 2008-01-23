// Copyright (C) 2008 Google Inc.
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

/**
 * @fileoverview
 * Testcases for hcalendar.js.  These are derived from the hcalendar acceptance
 * tests at http://hg.microformats.org/tests
 *
 * @author mikesamuel@gmail.com
 */


ICAL_PROD_ID = '$PRODID$';

function runTest(expectedIcs, inputHcal) {
  var html = inputHcal.join('\n');
  // Cheat a little bit so that we can check url resolution relative to a
  // known base url.
  if (!/<base/i.test(html)) {
    html = html.replace(
        /<\/head/i, '<base href="http://test-source/" /></head');
  }

  var calendar;
  loadHtml(
      html,
      function (doc) {
        console.time('extraction');
        calendar = extractHcal(doc.body);
        console.timeEnd('extraction');
      });

  for (var i = 0; i < calendar.length; ++i) {
    var values = calendar[i].values_;
    for (var j = 0; j < values.length; ++j) {
      if (values[j] === 'http://test-source/') {
        values[j] = '$SOURCE$';
      } else {
        values[j] = values[j].replace(
            /^http:\/\/test-source\//g, '$$SOURCE$$/');
      }
    }
  }
  var actualIcs = calendar.join('\n').replace(/\r\n /g, '');
  assertEquals(expectedIcs.join('\n'), actualIcs);
}


function test_01ComponentVeventDtstartDate() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent dtstart date",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "DTSTART;VALUE=DATE:19970903",  // added VALUE attr
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\" content=\"text/html; charset="
       + "utf-8\" />",
       "  <title>component vevent dtstart date</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div>Dates: <abbr class=\"dtstart\" title=\"19970903\">"
       + "Septempter 3, 1997</abbr></div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function test_02ComponentVeventDtstartDatetime() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent dtstart datetime",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "DTSTART;VALUE=DATE-TIME:19970903T163000Z",  // added VALUE attr
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent dtstart datetime</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div>Dates: <abbr class=\"dtstart\" title="
       + "\"19970903T163000Z\">Septempter 3, 1997, 16:30</abbr></div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function test_03ComponentVeventDtendDate() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent dtend date",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // swapped start and end
       "DTSTART;VALUE=DATE:19970903",  // added VALUE attr
       "DTEND;VALUE=DATE:19970904",  // added VALUE attr.
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent dtend date</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div>Dates: <abbr class=\"dtstart\" title=\"19970903\">"
       + "Septempter 3, 1997</abbr>",
       "    <abbr class=\"dtend\" title=\"19970904\">( all day )"
       + "</abbr></div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function test_04ComponentVeventDtendDatetime() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent dtend datetime",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "DTSTART;VALUE=DATE-TIME:19970903T160000Z",  // added VALUE attr
       "DTEND;VALUE=DATE-TIME:19970903T180000Z",  // added VALUE attr
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent dtend datetime</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div>Date: <abbr class=\"dtstart\" title=\"19970903T160000Z\">"
       + "Septempter 3, 1997 at 4pm</abbr>",
       "    <abbr class=\"dtend\" title=\"19970903T180000Z\"> for 2 hours."
       + "</abbr></div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function test_05CalendarSimple() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:calendar simple",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // changed order of lines
       "URL:http://www.web2con.com/",
       "SUMMARY:Web 2.0 Conference",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr
       "LOCATION:Argent Hotel\\, San Francisco\\, CA",
       // end reordered lines
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "    <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "    <title>calendar simple</title>",
       "</head>",
       "<body>",
       "    <div class=\"vcalendar\">",
       "      <span class=\"vevent\">",
       "        <a class=\"url\" href=\"http://www.web2con.com/\">",
       "          <span class=\"summary\">Web 2.0 Conference</span>: ",
       "          <abbr class=\"dtstart\" title=\"2005-10-05\">"
       + "October 5</abbr>-",
       "          <abbr class=\"dtend\" title=\"2005-10-08\">7</abbr>,",
       "          at the <span class=\"location\">Argent Hotel,"
       + " San Francisco, CA</span>",
       "        </a>",
       "      </span>",
       "    </div>",
       "</body>",
       "</html>"]
      );
}

function test_06ComponentVeventUriRelative() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent uri relative",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "URL:$SOURCE$/squidlist/calendar/12279/2006/1/15",  // removed $???
       "SUMMARY:Bad Movie Night - Gigli (blame mike spiegelman)",
       "DTSTART;VALUE=DATE-TIME:20060115T000000",  // added VALUE attrib
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent uri relative</title>",
       "</head>",
       "<body>",
       "  <p class=\"vevent\">",
       "    <a class=\"url summary\""
       + " href=\"/squidlist/calendar/12279/2006/1/15\">"
       + "Bad Movie Night - Gigli (blame mike spiegelman)</a>",
       "    <br />",
       "    <abbr class=\"dtstart\" title=\"20060115T000000\">"
       + "Sun, Jan 15 : 8pm</abbr>",
       "    <br />",
       "  </p>",
       "</body>",
       "</html>"]
      );
}

function test_07ComponentVeventDescriptionSimple() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent description simple",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "DESCRIPTION:Project xyz Review Meeting Minutes",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent description simple</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div class=\"description\">Project xyz Review Meeting Minutes"
       + "</div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}


function test_08ComponentVeventMultipleClasses() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent multiple classes",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "URL:http://www.web2con.com/",
       "SUMMARY:Web 2.0 Conference",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr
       "LOCATION:Argent Hotel\\, San Francisco\\, CA",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent multiple classes</title>",
       "</head>",
       "<body>",
       "  <div class=\"aaa vevent\">",
       "    <a class=\"bbb url\" href=\"http://www.web2con.com/\">",
       "      <span class=\"ccc summary\">Web 2.0 Conference</span>: ",
       "      <abbr class=\"ddd dtstart\" title=\"2005-10-05\">October 5"
       + "</abbr>-",
       "      <abbr class=\"eee dtend\" title=\"2005-10-08\">7</abbr>,",
       "      at the <span class=\"fff location\">Argent Hotel,"
       + " San Francisco, CA</span>",
       "    </a>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function test_09ComponentVeventSummaryInImgAlt() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent summary in img alt",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "URL:http://conferences.oreillynet.com/et2006/",
       "SUMMARY:O\'Reilly Emerging Technology Conference",
       "DTSTART;VALUE=DATE:20060306",  // added VALUE attr
       "DTEND;VALUE=DATE:20060310",  // added VALUE attr
       "LOCATION:Manchester Grand Hyatt in San Diego\\, CA",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent summary in img alt</title>",
       "</head>",
       "<body>",
       "<ul>",
       "  <li class=\"vevent\">",
       "   <a class=\"url\""
       + " href=\"http://conferences.oreillynet.com/et2006/\">",
       "    <img style=\"display:block\" class=\"summary\" ",
       "     src=\"http://conferences.oreillynet.com/images/et2005/"
       + "logo_sm.gif\"",
       "     alt=\"O\'Reilly Emerging Technology Conference\" />",
       "    <abbr class=\"dtstart\" title=\"20060306\">",
       "      3/6</abbr>-<abbr class=\"dtend\""
       + " title=\"20060310\">9</abbr>",
       "    @",
       "    <span class=\"location\">",
       "       Manchester Grand Hyatt in San Diego, CA",
       "     </span>",
       "   </a>",
       "  </li>",
       "</ul>",
       "</body>",
       "</html>"]
      );
}

function test_10ComponentVeventEntity() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent entity",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // html escaped
       "SUMMARY:Cricket &amp\\; Tennis Centre",
       // html escaped
       "DESCRIPTION:Melbourne\'s Cricket &amp\\; Tennis Centres are in"
       + " the heart of the city",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent entity</title>",
       "</head>",
       "<body>",
       "  <div class=\"vcalendar\">",
       "    <div class=\"vevent\">",
       "      <div class=\"summary\">Cricket &amp; Tennis Centre</div>",
       "      <div class=\"description\">Melbourne&apos;s Cricket &amp;"
       + " Tennis Centres are in the heart of the city</div>",
       "    </div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function test_11ComponentVeventSummaryInSubelements() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent summary in subelements",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "SUMMARY:Welcome! John Battelle\\, Tim O\'Reilly",
       "DTSTART;VALUE=DATE-TIME:20051005T233000Z",  // added VALUE attr
       "DTEND;VALUE=DATE-TIME:20051005T234500Z",  // added VALUE attr
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent summary in subelements</title>",
       "</head>",
       "<body>",
       "  <p class=\"schedule vevent\">",
       "    <span class=\"summary\">",
       "      <span style=\"font-weight:bold; color: #3E4876;\">Welcome!"
       + "</span>",
       "      <a href=\"/cs/web2005/view/e_spkr/1852\">John Battelle</a>,",
       "      <a href=\"/cs/web2005/view/e_spkr/416\">Tim O\'Reilly</a>",
       "    </span>",
       "    <br />",
       "    <b>Time:</b>",
       "    <abbr class=\"dtstart\" title=\"20051005T1630-0700\">4:30pm"
       + "</abbr>-",
       "    <abbr class=\"dtend\" title=\"20051005T1645-0700\">4:45pm",
       "    </abbr>",
       "  </p>",
       "</body>",
       "</html>"]
      );
}

function test_12ComponentVeventSummaryUrlInSameClass() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent summary url in same class",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "URL:http://www.laughingsquid.com"
       + "/squidlist/calendar/12377/2006/1/25",
       "SUMMARY:Art Reception for Tom Schultz and Felix Macnee",
       "DTSTART;VALUE=DATE-TIME:20060125T000000",  // added VALUE attr
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent summary url in same class</title>",
       "</head>",
       "<body>",
       "  <p class=\"vevent\">",
       "    <a class=\"url summary\""
       + " href=\"http://www.laughingsquid.com"
       + "/squidlist/calendar/12377/2006/1/25\">"
       + "Art Reception for Tom Schultz and Felix Macnee</a>",
       "    <br />",
       "    <abbr class=\"dtstart\" title=\"20060125T000000\">"
       + "Wed, Jan 25 : 6:00 pm - 9:00 pm</abbr>",
       "    <br />",
       "  </p>",
       "</body>",
       "</html>"]
      );
}

function test_13ComponentVeventSummaryUrlProperty() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent summary url property",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "SUMMARY:ORD-SFO/AA 1655",
       "URL:http://dps1.travelocity.com"
       + "/dparcobrand.ctl?smls=Y&Service=YHOE&.intl=us&aln_name=AA"
       + "&flt_num=1655&dep_arp_name=&arr_arp_name=&dep_dt_dy_1=23"
       + "&dep_dt_mn_1=Jan&dep_dt_yr_1=2006&dep_tm_1=9:00am",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent summary url property</title>",
       "</head>",
       "<body>",
       "  <div class=\"vcalendar\">",
       "    <div class=\"vevent\">",
       "      <span class=\"summary\">",
       "        <a class=\"url\" href=\"http://dps1.travelocity.com"
       + "/dparcobrand.ctl?smls=Y&amp;Service=YHOE&amp;.intl=us"
       + "&amp;aln_name=AA&amp;flt_num=1655&amp;dep_arp_name="
       + "&amp;arr_arp_name=&amp;dep_dt_dy_1=23&amp;dep_dt_mn_1=Jan"
       + "&amp;dep_dt_yr_1=2006&amp;dep_tm_1=9:00am\">ORD-SFO/AA 1655</a>",
       "      </span>",
       "    </div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function test_14CalendarAnniversary() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:calendar anniversary",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "SUMMARY:Our Blissful Anniversary",
       "DTSTAMP;VALUE=DATE-TIME:19970901T130000Z",  // added VALUE attr
       "UID:19970901T130000Z-123403@host.com",
       "DTSTART;VALUE=DATE:19971102",  // added VALUE attr
       "CLASS:CONFIDENTIAL",  // converted to upper case
       "CATEGORIES:Anniversary,PersonaL,Special Occassion",
       "RRULE:FREQ=YEARLY",  // converted to upper case
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
       + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "    <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "    <title>calendar anniversary</title>",
       "</head>",
       "<body>",
       "    <div class=\"vcalendar\">",
       "      <div class=\"vevent\">",
       "        <h5 class=\"summary\">Our Blissful Anniversary</h5>",
       "        <div>Posted on: <abbr class=\"dtstamp\""
       + " title=\"19970901T1300Z\">September 1, 1997</abbr></div>",
       "        <div class=\"uid\">19970901T130000Z-123403@host.com</div>",
       "        <div>Date: <abbr class=\"dtstart\" title=\"19971102\">"
       + "November 2, 1997</abbr></div>",
       "        <div>This event is <strong class=\"class\">confidential"
       + "</strong>.</div>",
       "        <div>Filed under:</div>",
       "        <ul>",
       "          <li class=\"category\">Anniversary</li>",
       "          <li class=\"category\">PersonaL</li>",
       "          <li class=\"category\">Special Occassion</li>",
       "        </ul>",
       "        <div class=\"rrule\">Repeat <span class=\"freq\">"
       + "yearly</span>.</div>",
       "      </div>",
       "    </div>",
       "</body>",
       "</html>"]
      );
}

function test_15CalendarXmlLang() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME;LANGUAGE=en:calendar xml lang",  // added language
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "URL:http://www.web2con.com/",
       "SUMMARY;LANGUAGE=en:Web 2.0 Conference",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr
       "LOCATION;LANGUAGE=en:Argent Hotel\\, San Francisco\\, CA",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>calendar xml lang</title>",
       "</head>",
       "<body>",
       "  <div class=\"vcalendar\">",
       "    <span class=\"vevent\">",
       "      <a class=\"url\" href=\"http://www.web2con.com/\">",
       "      <span class=\"summary\">Web 2.0 Conference</span>: ",
       "      <abbr class=\"dtstart\" title=\"2005-10-05\">"
       + "October 5</abbr>-",
       "      <abbr class=\"dtend\" title=\"2005-10-08\">7</abbr>,",
       "      at the <span class=\"location\">Argent Hotel,"
       + " San Francisco, CA</span>",
       "      </a>",
       "    </span>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function test_16CalendarForceOutlook() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:calendar force outlook",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "URL:http://www.web2con.com/",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr
       "LOCATION:Argent Hotel\\, San Francisco\\, CA",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!-- @TODO - do we need a special test for this? is x2v working"
       + " with outlook? -->",
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
       + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "    <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "    <title>calendar force outlook</title>",
       "</head>",
       "<body>",
       "<!-- http://www.inkdroid.org"
       + "/journal/2006/01/19/ical-and-outlook/-->",
       "    <div class=\"vcalendar\">",
       "      <span class=\"vevent\">",
       "        <a class=\"url\" href=\"http://www.web2con.com/\">",
       "          <abbr class=\"dtstart\" title=\"2005-10-05\">October 5"
       + "</abbr>-",
       "          <abbr class=\"dtend\" title=\"2005-10-08\">7</abbr>,",
       "          at the <span class=\"location\">Argent Hotel,"
       + " San Francisco, CA</span>",
       "        </a>",
       "      </span>",
       "    </div>",
       "</body>",
       "</html>"]
      );
}

function test_17ComponentVeventDescriptionValueInSubelements() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent description value in subelements",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // preserved HTML tags.
       "DESCRIPTION:RESOLUTION: to have a <b>3rd PAW ftf meeting</b>"
       + " 18-19 Jan in Maryland\\; location contingent on confirmation"
       + " from timbl",
       "SUMMARY:3rd PAW ftf meeting",
       "DTSTART;VALUE=DATE:20060118",  // added VALUE attr
       "DTEND;VALUE=DATE:20060120",  // added VALUE attr
       "LOCATION:Maryland",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent description value in subelements"
       + "</title>",
       "</head>",
       "<body>",
       "  <p class=\"vevent\" id=\"item18d44\">",
       "    <span class=\"description\">",
       "      RESOLUTION: to have a",
       "      <b class=\"summary\">3rd PAW ftf meeting</b> ",
       "      <abbr class=\"dtstart\" title=\"2006-01-18\">18</abbr>-"
       + "<abbr class=\"dtend\" title=\"2006-01-20\">19 Jan</abbr> in ",
       "      <em class=\"location\">Maryland</em>; location contingent"
       + " on confirmation from timbl</span>",
       "  </p>",
       "</body>",
       "</html>"]
      );
}

function test_18ComponentVeventUid() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent uid",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "UID:http://example.com/foo.html",
       "END:VEVENT",
       // '",  // removed
       "BEGIN:VEVENT",
       "UID:http://example.com/foo.html",
       "END:VEVENT",
       // '",  // removed
       "BEGIN:VEVENT",
       "UID:http://example.com/foo.html",
       "END:VEVENT",
       // '",  // removed
       "BEGIN:VEVENT",
       "UID:http://example.com/foo.html",
       "END:VEVENT",
       // '",  // removed
       "BEGIN:VEVENT",
       "UID:http://example.com/foo.html",
       "END:VEVENT",
       // '",  // removed
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent uid</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div>UID: <span class=\"uid\">http://example.com/foo.html"
       + "</span></div>",
       "  </div>",
       "  <div class=\"vevent\">",
       "    UID: <a class=\"uid\" href=\"http://example.com/foo.html\">"
       + "another hcal event</a>",
       "  </div>",
       "  ",
       "  <div class=\"vevent\">",
       "    UID: <object class=\"uid\""
       + " data=\"http://example.com/foo.html\">another hcal event"
       + "</object>",
       "  </div>",
       "  ",
       "  <div class=\"vevent\">",
       "    UID: <map id=\"foo\"><area alt=\"uid\" class=\"uid\""
       + " href=\"http://example.com/foo.html\" /></map>",
       "  </div>",
       "  ",
       "  <div class=\"vevent\">",
       "    UID: <img class=\"uid\" alt=\"uid\""
       + " src=\"http://example.com/foo.html\" />",
       "  </div>",
       "  ",
       "  ",
       "</body>",
       "</html>"]
      );
}

function testCalendarAttachments() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:xyz",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:calendar attachments",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "DTSTAMP;VALUE=DATE-TIME:19970324T120000Z",
       "SEQUENCE:0",
       "UID:uid3@host1.com",
       "ORGANIZER:MAILTO:jdoe@host1.com",
       "DTSTART;VALUE=DATE-TIME:19970324T123000Z",  // added VALUE attr
       "DTEND;VALUE=DATE-TIME:19970324T210000Z",  // added VALUE attr
       "CATEGORIES:Meeting,Project",
       "CLASS:PUBLIC",  // upper cased
       "SUMMARY:Calendaring Interoperability Planning Meeting",
       // html escaped
       "DESCRIPTION:Discuss how we can test c&amp\\;s interoperability"
       + " using iCalendar and other IETF standards.",
       "LOCATION:LDB Lobby",
       "ATTACH;FMTTYPE=application/postscript:ftp://xyzcorp.com"
       + "/pub/conf/bkgrnd.ps",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
       + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "    <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "    <title>calendar attachments</title>",
       "</head>",
       "<body>",
       "    <div class=\"vcalendar\">",
       "      <div>Method: <span class=\"method\">xyz</span></div>",
       "      <div class=\"vevent\">",
       "        <div>Posted at: <span class=\"dtstamp\">19970324T1200Z"
       + "</span></div>",
       "        <div>Sequence: <span class=\"sequence\">0</span></div>",
       "        <div>UID: <span class=\"uid\">uid3@host1.com</span></div>",
       "        <div>Organzied by: <a class=\"organizer\""
       + " href=\"mailto:jdoe@host1.com\">jdoe@host1.com</a></div>",
       "        <div>Start Time: <abbr class=\"dtstart\""
       + " title=\"19970324T123000Z\">March 24, 1997 12:30 UTC"
       + "</abbr></div>",
       "        <div>End Time: <abbr class=\"dtend\""
       + " title=\"19970324T210000Z\">March 24, 1997, 21:00 UTC"
       + "</abbr></div>",
       "        <ul>",
       "          <li class=\"category\">Meeting</li>",
       "          <li class=\"category\">Project</li>",
       "        </ul>",
       "        <div>This event is <strong class=\"class\">Public"
       + "</strong></div>",
       "        <div class=\"summary\">Calendaring Interoperability"
       + " Planning Meeting</div>",
       "        <div class=\"description\">Discuss how we can test"
       + " c&amp;s interoperability using iCalendar and other IETF"
       + " standards.</div>",
       "        <div class=\"location\">LDB Lobby</div>",
       "        <div>Attachment: <a class=\"attach\""
       + " type=\"application/postscript\""
       // I lowercased the domain name here since browsers and env.js
       // disagree on whether the protocol and domain should be normalized.
       + " href=\"ftp://xyzcorp.com/pub/conf/bkgrnd.ps\">"
       + "ftp://xyzCorp.com/pub/conf/bkgrnd.ps</a></div>",
       "      </div>",
       "    </div>",
       "</body>",
       "</html>"]
      );
}

function testCalendarDel() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:calendar del",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "URL:http://www.web2con.com/",
       "SUMMARY:Web 2.0 Conference",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr
       "LOCATION:Argent Hotel\\, San Francisco\\, CA",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!--@TODO - wait on MarkM to figure out what\'s up with this test."
       + " <del>?  -->",
       "",
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
       + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "    <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "    <title>calendar del</title>",
       "</head>",
       "<body>",
       "    <div class=\"vcalendar\">",
       "      <span class=\"vevent\">",
       "        <a class=\"url\" href=\"http://www.web2con.com/\">",
       "          <span class=\"summary\">Web 2.0 Conference</span>: ",
       "          <abbr class=\"dtstart\" title=\"2005-10-05\">October 5"
       + "</abbr>-",
       "          <abbr class=\"dtend\" title=\"2005-10-08\">7</abbr>,",
       "          at the <del><span class=\"location\">Argent Hotel,"
       + " San Francisco, CA</span></del>",
       "        </a>",
       "      </span>",
       "    </div>",
       "</body>",
       "</html>"]
      );
}

function testCalendarFragment() {
  runTest(
      [
       // TODO not sure about this test
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",  // missing from test
       "X-ORIGINAL-URL:$SOURCE$",  // missing from test
       "X-WR-CALNAME:calendar-attachments",  // missing from test
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",  // whole event missing from test
       "URL:http://www.myevent1.com/",
       "SUMMARY:The First Event",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr
       "END:VEVENT",
       "BEGIN:VEVENT",
       "URL:http://www.myevent2.com/",
       "SUMMARY:The Second Event",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr. removed time
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr.  removed time
       "END:VEVENT",
       "BEGIN:VEVENT",
       "URL:http://www.myevent3.com/",  // whole event missing from test
       "SUMMARY:The Third Event",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr. removed time
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr.  removed time
       "END:VEVENT",
       "END:VCALENDAR"],  // removed trailing newline
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
       + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "    <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "    <title>calendar-attachments</title>",
       "</head>",
       "",
       "<body>",
       "    <!-- /calendar-fragment.xml#event2 -->",
       "    <!-- @TODO - not sure about this one -->",
       "    <div class=\"vcalendar\">",
       "        <a id=\"event1\" class=\"vevent url\""
       + " href=\"http://www.myevent1.com/\">",
       "          <span class=\"summary\">The First Event</span>: ",
       "          <abbr class=\"dtstart\" title=\"2005-10-05\">October 5"
       + "</abbr>-",
       "          <abbr class=\"dtend\" title=\"2005-10-08\">7</abbr>",
       "        </a>",
       "        <a id=\"event2\" class=\"vevent url\""
       + " href=\"http://www.myevent2.com/\">",
       "          <span class=\"summary\">The Second Event</span>: ",
       "          <abbr class=\"dtstart\" title=\"2005-10-05\">October 5"
       + "</abbr>-",
       "          <abbr class=\"dtend\" title=\"2005-10-08\">7</abbr>",
       "        </a>",
       "        <a id=\"event3\" class=\"vevent url\""
       + " href=\"http://www.myevent3.com/\">",
       "          <span class=\"summary\">The Third Event</span>: ",
       "          <abbr class=\"dtstart\" title=\"2005-10-05\">October 5"
       + "</abbr>-",
       "          <abbr class=\"dtend\" title=\"2005-10-08\">7</abbr>",
       "        </a>",
       "    </div>",
       "</body>",
       "</html>"]
      );
}

function testCalendarHtmlLang() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME;LANGUAGE=en:calendar html lang",  // added language
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "URL:http://www.web2con.com/",
       "SUMMARY;LANGUAGE=en:Web 2.0 Conference",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr
       "LOCATION;LANGUAGE=en:Argent Hotel\\, San Francisco\\, CA",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
       + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "    <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "    <title>calendar html lang</title>",
       "</head>",
       "<body>",
       "  <div class=\"vcalendar\">",
       "    <span class=\"vevent\">",
       "      <a class=\"url\" href=\"http://www.web2con.com/\">",
       "        <span class=\"summary\">Web 2.0 Conference</span>: ",
       "        <abbr class=\"dtstart\" title=\"2005-10-05\">October 5"
       + "</abbr>-",
       "        <abbr class=\"dtend\" title=\"2005-10-08\">7</abbr>,",
       "            at the <span class=\"location\">Argent Hotel,"
       + " San Francisco, CA</span>",
       "      </a>",
       "    </span>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testCalendarLangSubLang() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME;LANGUAGE=en:calendar lang sub lang", // added language
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "URL:http://www.web2con.com/",
       "SUMMARY;LANGUAGE=en:Web 2.0 Conference",
       "DTSTART;VALUE=DATE:20051005",  // added VALUE attr
       "DTEND;VALUE=DATE:20051008",  // added VALUE attr
       "LOCATION;LANGUAGE=de:Serrano Hotel\\, San Francisco\\, CA",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">",
       "<head>",
       "    <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "    <title>calendar lang sub lang</title>",
       "</head>",
       "<body>",
       "  <div class=\"vcalendar\">",
       "    <span class=\"vevent\">",
       "      <a class=\"url\" href=\"http://www.web2con.com/\">",
       "        <span class=\"summary\">Web 2.0 Conference</span>: ",
       "        <abbr class=\"dtstart\" title=\"2005-10-05\">October 5"
       + "</abbr>-",
       "        <abbr class=\"dtend\" title=\"2005-10-08\">7</abbr>,",
       "        at the <span class=\"location\" xml:lang=\"de\">"
       + "Serrano Hotel, San Francisco, CA</span>",
       "      </a>",
       "    </span>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testCalendarThreedayconference() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:calendar threedayconference",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "SUMMARY:Networld+Interop Conference",
       "DESCRIPTION:Networld+Interop Conference and Exhibit Atlanta World"
       + " Congress Center Atlanta\\, Georgia",
       "DTSTAMP;VALUE=DATE-TIME:19960704T120000Z",  // added VALUE attr
       "UID:uid1@host.com",
       "ORGANIZER:MAILTO:jsmith@host.com", // fixed. ; => : after ORGANIZER
       "DTSTART;VALUE=DATE-TIME:19960918T143000Z",  // added VALUE attr
       "DTEND;VALUE=DATE-TIME:19960920T220000Z",  // added VALUE attr
       "STATUS:CONFIRMED",
       "CATEGORIES:Conference",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "    <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "    <title>calendar threedayconference</title>",
       "</head>",
       "<body>",
       "    <div class=\"vcalendar\">",
       "      <div class=\"vevent\">",
       "        <h5 class=\"summary\">Networld+Interop Conference</h5>",
       "        <div class=\"description\">Networld+Interop Conference and"
       + " Exhibit Atlanta World Congress Center Atlanta, Georgia</div>",
       "        <div>Posted on: <abbr class=\"dtstamp\""
       + " title=\"19960704T120000Z\">July 4, 1996</abbr></div>",
       "        <div class=\"uid\">uid1@host.com</div>",
       "        <div>Organized by: <a class=\"organizer\""
       + " href=\"mailto:jsmith@host.com\">jsmith@host.com</a></div>",
       "        <div>Dates: <abbr class=\"dtstart\""
       + " title=\"19960918T143000Z\">September 18, 1996, 14:30 UTC"
       + "</abbr> -",
       "        <abbr class=\"dtend\" title=\"19960920T220000Z\">"
       + "September 20, 1996, 22:00 UTC</abbr></div>",
       "        <div>Status: <span class=\"status\">CONFIRMED"
       + "</span></div>",
       "        <div>Filed under:</div>",
       "        <ul>",
       "          <li class=\"category\">Conference</li>",
       "        </ul>",
       "      </div>",
       "    </div>",
       "</body>",
       "</html>"]
      );
}

function testCalendarTransparentReminder() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",  // missing method
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:calendar-transparent-reminder",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "SUMMARY:Laurel is in sensitivity awareness class.",
       "DTSTAMP;VALUE=DATE-TIME:19970901T130000Z",  // added VALUE attr
       "UID:19970901T130000Z-123402@host.com",
       "DTSTART;VALUE=DATE-TIME:19970401T163000Z",  // added VALUE attr
       "DTEND;VALUE=DATE-TIME:19970402T010000Z",  // added VALUE attr
       "CLASS:PUBLIC",  // to upper case
       "TRANSP:TRANSPARENT",  // to upper case
       "CATEGORIES:Business,Human Resources",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>calendar-transparent-reminder</title>",
       "  ",
       "</head>",
       "<body>",
       "  <div class=\"vcalendar\">",
       "    <div class=\"vevent\">",
       "      <h5 class=\"summary\">Laurel is in sensitivity awareness"
       + " class.</h5>",
       "      <div>Posted on: <abbr class=\"dtstamp\""
       + " title=\"19970901T1300Z\">September 1, 1997</abbr></div>",
       "      <div class=\"uid\">19970901T130000Z-123402@host.com</div>",
       "      <div>Dates: <abbr class=\"dtstart\""
       + " title=\"19970401T163000Z\">April 1, 1997, 16:30 UTC</abbr>-",
       "      <abbr class=\"dtend\" title=\"19970402T010000Z\">"
       + "April 2, 1997 01:00 UTC</abbr></div>",
       "      <div>This event is <span class=\"class\">public</span>"
       + " and <span class=\"transp\">transparent</span> to"
       + " free/busy scheduling.</div>",
       "      <div>Filed under:</div>",
       "      <ul>",
       "        <li class=\"category\">Business</li>",
       "        <li class=\"category\">Human Resources</li>",
       "      </ul>",
       "    </div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventAttachment() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent attachment",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // normalized case of hostname
       "ATTACH;FMTTYPE=application/postscript:ftp://xyzcorp.com"
       + "/pub/conf/bkgrnd.ps",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent attachment</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div>Attachment: ",
       "      <a class=\"attach\" ",
       "        type=\"application/postscript\" ",
       "        href=\"ftp://xyzcorp.com/pub/conf/bkgrnd.ps\"",
       "      >",
       "      ftp://xyzcorp.com/pub/conf/bkgrnd.ps",
       "      </a>",
       "    </div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventAttendeeMultiple() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent attendee multiple",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // normed case
       "ATTENDEE;PARTSTAT=TENTATIVE:mailto:jqpublic@host.com",
       "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED"
       + ";DELEGATED-FROM=\"mailto:jimdo@host1.com\""
       + ":mailto:jdoe@host1.com",  // normed case
       "END:VEVENT",
       "END:VCALENDAR"],  // trimmed trailing newline
      [
       "<!-- @TODO - I don\'t think attendee stuff is implemented in X2V"
       + " ryan -->",
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent attendee multiple</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div>Attending: ",
       "    <span class=\"attendee\">",
       "      <a class=\"value\" href=\"mailto:jqpublic@host.com\">"
       + "jqpublic@host.com</a>",
       "      (<span class=\"partstat\">Tentative</span>)    ",
       "    </span>    ",
       "    <span class=\"attendee\">",
       "      <!--  @TODO - should \'REQ-PARTICIPANT be lc\'ed?   -->",
       "      <abbr class=\"role\" title=\"REQ-PARTICIPANT\">"
       + "Required Pariticipant</abbr>,",
       "      <a class=\"value\" href=\"mailto:jdoe@host1.com\">"
       + "jdoe@host1.com</a> ",
       "      (<span class=\"partstat\">Accepted</span>)",
       "    <!--    @TODO - should mailto: be here?    -->",
       "      <abbr class=\"delegated-from\""
       + " title=\"mailto:jimdo@host1.com\">delegated from Jim Do</abbr>)",
       "    </span>",
       "    </div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventAttendeeValue() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",  // added
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent attendee value",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // added : and normed case
       "ATTENDEE;RSVP=TRUE:mailto:jsmith@host1.com",
       "END:VEVENT",
       "END:VCALENDAR"],  // removed trailing newline
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent attendee value</title>",
       "</head>",
       "<body>",
       "  <div class=\"vcalendar\">",
       "    <div class=\"vevent\">",
       "      <div>Attending: ",
       "        <span class=\"attendee\">",
       "          <a class=\"value\" href=\"mailto:jsmith@host1.com\">"
       + "jsmith@host1.com</a> ",
       "          RSVPed? ",
       "          <span class=\"rsvp\">TRUE</span>",
       "        </span>",
       "      </div>",
       "    </div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventCalendarProperty() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:xyz",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent calendar property",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "SUMMARY:Calendaring Interoperability Planning Meeting",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent calendar property</title>",
       "</head>",
       "<body>",
       "  <div class=\"vcalendar\">",
       "    <div>Method: ",
       "      <span class=\"method\">xyz</span>",
       "    </div>",
       "    <div class=\"vevent\">",
       "      <div class=\"summary\">"
       + "Calendaring Interoperability Planning Meeting</div>",
       "    </div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventCategory() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent category",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "CATEGORIES:Business,Human Resources",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent category</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <ul>",
       "      <li class=\"category\">Business</li>",
       "      <li class=\"category\">Human Resources</li>",
       "    </ul>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventDel() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent del",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "SUMMARY:Having a nice day",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!-- @TODO - ask MarkM about this. Did he mean to use <del>? -->",
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent del</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "\t<del>",
       "    <ul class=\"del\">",  // means that the elements are deleted
       "      <li class=\"category\">Business</li>",
       "      <li class=\"category\">Human Resources</li>",
       "    </ul>",
       "\t</del>",
       "    <div class=\"summary\">Having a nice day</div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventDescriptionBulletPointsAlternate() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent description bullet points alternate",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // Previous version was run through w3m?
       "DESCRIPTION:Project xyz Review Meeting Minutes <br> Agenda <br> "
       + "<ol> <li> Review of project version 1.0 requirements.</li>"
       + " <li> Definition of project processes.</li>"
       + " <li> Review of project schedule.</li> </ol>"
       + " Participants: John Smith\\, Jane Doe\\, Jim Dandy <br> "
       + "<ul> <li>It was decided that the requirements need to be signed"
       + " off by product marketing.</li> <li>Project processes were"
       + " accepted.</li> <li>Project schedule needs to account for"
       + " scheduled holidays and employee vacation time. Check with HR"
       + " for specific dates.</li> <li>New schedule will be distributed"
       + " by Friday.</li> <li>Next weeks meeting is cancelled. No meeting"
       + " until 3/23.</li> </ul>",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!-- @TODO - review the x2v formatting stuff for DESCRIPTION"
       + " - need to be careful about whitespace -->",
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent description bullet points alternate"
       + "</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div class=\"description\">",
       "      Project xyz Review Meeting Minutes",
       "      <br />",
       "      Agenda",
       "      <br />",
       "      <ol>",
       "        <li> Review of project version 1.0 requirements.</li>",
       "        <li> Definition of project processes.</li>",
       "        <li> Review of project schedule.</li>",
       "      </ol>",
       "      Participants: John Smith, Jane Doe, Jim Dandy",
       "      <br />",
       "      <ul>      ",
       "        <li>It was decided that the requirements need to be signed"
       + " off by product marketing.</li>",
       "        <li>Project processes were accepted.</li>",
       "        <li>Project schedule needs to account for scheduled"
       + " holidays and employee vacation time. Check with HR for specific"
       + " dates.</li>",
       "        <li>New schedule will be distributed by Friday.</li>",
       "        <li>Next weeks meeting is cancelled. No meeting until"
       + " 3/23.</li>",
       "      </ul>  ",
       "    </div>",
       "  </div>",
       "  </body>",
       "</html>"]
      );
}

function testComponentVeventDescriptionBulletPoints() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",  // missing
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent description bullet points",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // Previous version was run through w3m?
       "DESCRIPTION:Project xyz Review Meeting Minutes<br> Agenda<br>"
       + " <ol> <li> Review of project version 1.0 requirements.</li>"
       + " <li> Definition of project processes.</li>"
       + " <li> Review of project schedule.</li> </ol>"
       + "Participants: John Smith\\, Jane Doe\\, Jim Dandy<br>"
       + " <ul> <li>It was decided that the requirements need to be signed"
       + " off by product marketing.</li> <li>Project processes were"
       + " accepted.</li> <li>Project schedule needs to account for"
       + " scheduled holidays and employee vacation time. Check with HR"
       + " for specific dates.</li> <li>New schedule will be distributed"
       + " by Friday.</li> <li>Next weeks meeting is cancelled. No meeting"
       + " until 3/23.</li> </ul>",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!-- @TODO -->",
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent description bullet points</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div class=\"description\">Project xyz Review Meeting Minutes"
       + "<br />",
       "    Agenda<br />",
       "    <ol>",
       "      <li> Review of project version 1.0 requirements.</li>",
       "      <li> Definition of project processes.</li>",
       "      <li> Review of project schedule.</li>",
       "    </ol>Participants: John Smith, Jane Doe, Jim Dandy<br />",
       "    <ul>",
       "      <li>It was decided that the requirements need to be signed"
       + " off by product marketing.</li>",
       "      <li>Project processes were accepted.</li>",
       "      <li>Project schedule needs to account for scheduled holidays"
       + " and employee vacation time. Check with HR for specific dates."
       + "</li>",
       "      <li>New schedule will be distributed by Friday.</li>",
       "      <li>Next weeks meeting is cancelled. No meeting until 3/23."
       + "</li>",
       "    </ul>  ",
       "    </div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventDescriptionSubAndSup() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent description sub and sup",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // Preserve html
       "DESCRIPTION:Project <sub>xyz Review</sub> <sup>Meeting</sup>"
       + " Minutes",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!-- @TODO - eh?  -->",
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent description sub and sup</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div class=\"description\">Project <sub>xyz Review</sub>"
       + " <sup>Meeting</sup> Minutes</div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventDescriptionValueInSubelements_2() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent description value in subelements 2",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // preserve html.
       "DESCRIPTION:RESOLUTION: to have a <b>3rd PAW ftf meeting</b> 18-19"
       + " Jan in Maryland\\; location contingent on confirmation from"
       + " timbl",
       "SUMMARY:3rd PAW ftf meeting",
       "DTSTART;VALUE=DATE:20060118",  // added VALUE attr
       "DTEND;VALUE=DATE:20060120",  // added VALUE attr
       "LOCATION:Maryland",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent description value in subelements 2"
       + "</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <span class=\"description\">RESOLUTION: to have a",
       "    <b class=\"summary\">3rd PAW ftf meeting</b> <abbr",
       "    class=\"dtstart\" title=\"2006-01-18\">18</abbr>-<abbr",
       "    class=\"dtend\" title=\"2006-01-20\">19 Jan</abbr> in <em",
       "    class=\"location\">Maryland</em>; location contingent on",
       "    confirmation from timbl</span>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventDtstamp() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent dtstamp",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "DTSTAMP;VALUE=DATE-TIME:19970901T130000Z",  // added VALUE attr
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent dtstamp</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div>posted on <abbr class=\"dtstamp\""
       + " title=\"19970901T1300Z\">September 1, 1997</abbr></div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventLang() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME;LANGUAGE=en:component vevent lang",  // added LANGUAGE
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "SUMMARY;LANGUAGE=en:This is my English Summary",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\""
       + " lang=\"en\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent lang</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div class=\"summary\">This is my English Summary</div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventRdate() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent rdate",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       "SUMMARY:IEEE-754 Working Group Meeting",
       "DTSTART;VALUE=DATE:20060118",  // added VALUE attr
       "RDATE;VALUE=PERIOD:20060118T210000Z/20060119T010000Z"
       + ",20060119T210000Z/20060120T010000Z",
       "LOCATION:HP Cupertino",
       "END:VEVENT",
       "BEGIN:VEVENT",
       "SUMMARY:IEEE-754 Working Group Meeting",
       "DTSTART;VALUE=DATE:20060215",  // added VALUE attr
       "DTEND;VALUE=DATE:20060217",  // added VALUE attr
       "RDATE;VALUE=PERIOD:20060215T210000Z/20060216T010000Z"
       + ",20060216T210000Z/20060217T010000Z",
       "LOCATION:Sun Menlo Park",
       "END:VEVENT",
       "BEGIN:VEVENT",
       "SUMMARY:IEEE-754 Working Group Meeting",
       "DTSTART;VALUE=DATE:20060315",  // added VALUE attr
       "DTEND;VALUE=DATE:20060317",  // added VALUE attr
       "RDATE;VALUE=PERIOD:20060315T210000Z/20060316T010000Z"
       + ",20060316T210000Z/20060317T010000Z",
       "LOCATION:Sun Santa Clara",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent rdate</title>",
       "</head>",
       "<body>",
       "  <ul class=\"schedule vcalendar\">",
       "    <li class=\"vevent\">",
       "      <span class=\"summary invisible\">IEEE-754 Working Group"
       + " Meeting</span>",
       "      <abbr class=\"dtstart\" title=\"20060118\">18</abbr>"
       + " January, 2006 ",
       "      <abbr class=\"rdate\""
       + " title=\"20060118T1300-0800/20060118T1700-0800"
       + ", 20060119T1300-0800/20060119T1700-0800\">1-5PM</abbr>",
       "       at <span class=\"location\">HP Cupertino</span>"
       + " (<a href=\"email/msg01957.html\">directions</a>, ",
       "       <a href=\"email/msg01961.html\">agenda</a>)",
       "    </li>",
       "    <li class=\"vevent\">",
       "      <span class=\"summary invisible\">"
       + "IEEE-754 Working Group Meeting</span>",
       "      <abbr class=\"dtstart\" title=\"20060215\">15</abbr>-"
       + "<abbr class=\"dtend\" title=\"20060217\">16</abbr>",
       "       February, 2006",
       "      <abbr class=\"rdate\""
       + " title=\"20060215T1300-0800/20060215T1700-0800"
       + ", 20060216T1300-0800/20060216T1700-0800\">1-5PM</abbr> ",
       "      at <span class=\"location\">Sun Menlo Park</span>",
       "    </li>",
       "    <li class=\"vevent\">",
       "      <span class=\"summary invisible\">"
       + "IEEE-754 Working Group Meeting</span>",
       "      <abbr class=\"dtstart\" title=\"20060315\">15</abbr>-"
       + "<abbr class=\"dtend\" title=\"20060317\">16</abbr>",
       "       March, 2006 <abbr class=\"rdate\" ",
       "       title=\"20060315T1300-0800/20060315T1700-0800,"
       + " 20060316T1300-0800/20060316T1700-0800\">1-5PM</abbr> ",
       "       at <span class=\"location\">Sun Santa Clara</span>",
       "     </li>",
       "  </ul>",
       "</body>",
       "</html>"]
      );
}

function testComponentVeventRrule_1() {
  runTest(
      [
       "BEGIN:VCALENDAR",
       "METHOD:PUBLISH",
       "X-ORIGINAL-URL:$SOURCE$",
       "X-WR-CALNAME:component vevent rrule 1",
       "PRODID:$PRODID$",
       "VERSION:2.0",
       "BEGIN:VEVENT",
       // normed case.  reordered
       "RRULE:FREQ=WEEKLY;BYDAY=TU,TH;UNTIL=19971007",
       "END:VEVENT",
       "END:VCALENDAR"],
      [
       "<!-- @TODO implement RRULE -->",
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"",
       "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
       "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
       "<head>",
       "  <meta http-equiv=\"Content-type\""
       + " content=\"text/html; charset=utf-8\" />",
       "  <title>component vevent rrule 1</title>",
       "</head>",
       "<body>",
       "  <div class=\"vevent\">",
       "    <div class=\"rrule\">",
       "      <span class=\"freq\">Weekly</span>",
       "      on",
       "      <abbr class=\"byday\" title=\"TU\">Tuesday</abbr>",
       "      and",
       "      <abbr class=\"byday\" title=\"TH\">Thursday</abbr>",
       "      for <abbr class=\"until\" title=\"19971007\">5 weeks</abbr>",
       "      <abbr class=\"notused\" title=\"W00t\">stuff</abbr>",
       "    </div>",
       "  </div>",
       "</body>",
       "</html>"]
      );
}
