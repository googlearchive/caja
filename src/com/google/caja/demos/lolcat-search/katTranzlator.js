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
 * Translates English into LOLCat.  Inspired by lolspeak.org.
 *
 * @author mikesamuel@gmail.com
 */

/**
 * @param {string} text English
 * @return {string} LOLCAT
 */
function katTranzlator(text) {
  text = text.toUpperCase()
      .replace(new RegExp('\\s+', 'g', ' '))
      .replace(new RegExp('^ +| +$', 'g', ''));
  for (var i = 0, n = TRANSFORMATIONS.length; i < n; i += 2) {
    text = text.replace(TRANSFORMATIONS[i], TRANSFORMATIONS[i + 1]);
  }
  return text;
}

var TRANSFORMATIONS = [
  new RegExp('\bCAN (I|YOU|HE|SHE|IT|WE|THEY)\b', 'g'), '$1 CAN',

  new RegExp('\bTO\b', 'g'), '2',
  new RegExp('\bFOR\b', 'g'), '4',
  new RegExp('\bYOUR\b', 'g'), 'UR',
  new RegExp('\bYOU\b', 'g'), 'U',
  new RegExp('\bTHIS\b', 'g'), 'DIS',
  new RegExp('\bWITH\b', 'g'), 'WIF',
  new RegExp('\bHAVE\b', 'g'), 'HAZ',
  new RegExp('\bARE\b', 'g'), 'IS',
  new RegExp('\bAM\b', 'g'), 'IS',
  new RegExp('\bPLEASE\b', 'g'), 'PLZ',
  new RegExp('\bTHANKS\b', 'g'), 'THX',
  new RegExp('\bOH MY (GOD|GOSH)\b', 'g'), 'OMG',
  new RegExp('\bATE\b', 'g'), 'EATED',
  new RegExp('\bSAID\b', 'g'), 'SED',
  new RegExp('\bSERIOUSLY\b', 'g'), 'SRSLY',
  new RegExp('\bKNOW\b', 'g'), 'KNOE',
  new RegExp('\bLOVE\b', 'g'), 'LUV',
  new RegExp('\bHELP\b', 'g'), 'HALP',
  new RegExp('\bMAYBE\b', 'g'), 'MEBBE',
  new RegExp('\bWAS\b', 'g'), 'WUZ',
  new RegExp('\bOF\b', 'g'), 'OV',
  new RegExp('\bOH\b', 'g'), 'O',
  new RegExp('\bREALLY\b', 'g'), 'RLY',
  new RegExp('\bGREAT\b', 'g'), 'GRAET',
  new RegExp('\bMY\b', 'g'), 'MAH',
  new RegExp('\b(HELLO|HI)\b', 'g'), 'HAI',

  new RegExp('THI', 'g'), 'TI',
  new RegExp('\bKN', 'g'), 'N',

  new RegExp('([^ ])SE(\b|[^AEIOU])', 'g'), '$1ZE$2',
  new RegExp('IES\b', 'g'), 'EHS',
  new RegExp('TION(S?)\b', 'g'), 'SHUN',
  new RegExp('LE\b', 'g'), 'L',
  new RegExp('IENDS\b', 'g'), 'ENZ',
  new RegExp('([^R])ING\b', 'g'), '$1IN',
  new RegExp('I([KM])E\b', 'g'), 'IE$1',
  new RegExp('ER( [^AEIOU]|$)', 'g'), 'AH$1',
  new RegExp('ORE\b', 'g'), 'OAR',
  new RegExp('IE\b', 'g'), 'EE',
  new RegExp('AIR\b', 'g'), 'EH',
  new RegExp('AIN\b', 'g'), 'ANE',
  new RegExp('IEF\b', 'g'), 'EEF',
  new RegExp('TY\b', 'g'), 'TI',
  new RegExp('NESS\b', 'g'), 'NES',
  new RegExp('([^ AEIOU])E([RD])\b', 'g'), '$1$2',
  new RegExp('IC\b', 'g'), 'IK',
  new RegExp('VE\b', 'g'), 'V',
  new RegExp('FORE\b', 'g'), 'FOA',
  new RegExp('(O[^ AEIOU])E\b', 'g'), '$1',

  new RegExp('\bPH([AEIOU])', 'g'), 'F$1',
  new RegExp('([^AEIOU ])IR', 'g'), '$1UR',
  new RegExp('([^AEIOU ])S\b', 'g'), '$1Z',
  new RegExp('([^ AEIOU]) OV\b', 'g'), '$1A',

  new RegExp('N\'T', 'g'), 'NT',
  new RegExp('OAR', 'g'), 'OR',
  new RegExp('IGHT', 'g'), 'ITE',
  new RegExp('([AEIOU])S([BDFJV])', 'g'), '$1Z$2',
  new RegExp('CEIV', 'g'), 'CEEV',
  new RegExp('AUGHT', 'g'), 'AWT',
  new RegExp('OO', 'g'), 'U',
  new RegExp('U([^ AEIOU])E', 'g'), 'OO$1',
  new RegExp('U([^ AEIOU]I)', 'g'), 'OO$1',
  new RegExp('CIOUS', 'g'), 'SHUS',
  new RegExp('OUCH', 'g'), 'OWCH',
  new RegExp('ISON', 'g'), 'ISUN',
  new RegExp('OIS', 'g'), 'OYZ',

  new RegExp('\bSEAR', 'g'), 'SER',
  new RegExp('\bSEA', 'g'), 'SEE',
  new RegExp('\bGOD', 'g'), 'CEILING CAT',
  new RegExp('\bHEAVEN', 'g'), 'CEILING',

  new RegExp('([AEIOU])[SZ]E', 'g'), '$1Z',

  new RegExp('\bI AM\b', 'g'), 'I',
  new RegExp('\bIZ A\b', 'g'), 'IS',
  new RegExp('\bHAZ NO\b', 'g'), 'NO HAZ',
  new RegExp('\bDO YOU\b', 'g'), 'YOU',
  new RegExp('\bA ([A-Z]+)\b', 'g'), '$1',
  new RegExp('\bI IS\b', 'g'), 'IM'
  ];
