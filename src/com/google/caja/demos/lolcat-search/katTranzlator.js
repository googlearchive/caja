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
  text = text.toUpperCase().replace(/\s+/g, ' ').replace(/^ +| +$/g, '');
  for (var i = 0, n = TRANSFORMATIONS.length; i < n; i += 2) {
    text = text.replace(TRANSFORMATIONS[i], TRANSFORMATIONS[i + 1]);
  }
  return text;
}

var TRANSFORMATIONS = [
  /\bCAN (I|YOU|HE|SHE|IT|WE|THEY)\b/g, '$1 CAN',

  /\bTO\b/g, '2',
  /\bFOR\b/g, '4',
  /\bYOUR\b/g, 'UR',
  /\bYOU\b/g, 'U',
  /\bTHIS\b/g, 'DIS',
  /\bWITH\b/g, 'WIF',
  /\bHAVE\b/g, 'HAZ',
  /\bARE\b/g, 'IS',
  /\bAM\b/g, 'IS',
  /\bPLEASE\b/g, 'PLZ',
  /\bTHANKS\b/g, 'THX',
  /\bOH MY (GOD|GOSH)\b/g, 'OMG',
  /\bATE\b/g, 'EATED',
  /\bSAID\b/g, 'SED',
  /\bSERIOUSLY\b/g, 'SRSLY',
  /\bKNOW\b/g, 'KNOE',
  /\bLOVE\b/g, 'LUV',
  /\bHELP\b/g, 'HALP',
  /\bMAYBE\b/g, 'MEBBE',
  /\bWAS\b/g, 'WUZ',
  /\bOF\b/g, 'OV',
  /\bOH\b/g, 'O',
  /\bREALLY\b/g, 'RLY',
  /\bGREAT\b/g, 'GRAET',
  /\bMY\b/g, 'MAH',
  /\b(HELLO|HI)\b/g, 'HAI',

  /THI/g, 'TI',
  /\bKN/g, 'N',

  /([^ ])SE(\b|[^AEIOU])/g, '$1ZE$2',
  /IES\b/g, 'EHS',
  /TION(S?)\b/g, 'SHUN',
  /LE\b/g, 'L',
  /IENDS\b/g, 'ENZ',
  /([^R])ING\b/g, '$1IN',
  /I([KM])E\b/g, 'IE$1',
  /ER( [^AEIOU]|$)/g, 'AH$1',
  /ORE\b/g, 'OAR',
  /IE\b/g, 'EE',
  /AIR\b/g, 'EH',
  /AIN\b/g, 'ANE',
  /IEF\b/g, 'EEF',
  /TY\b/g, 'TI',
  /NESS\b/g, 'NES',
  /([^ AEIOU])E([RD])\b/g, '$1$2',
  /IC\b/g, 'IK',
  /VE\b/g, 'V',
  /FORE\b/g, 'FOA',
  /(O[^ AEIOU])E\b/g, '$1',

  /\bPH([AEIOU])/g, 'F$1',
  /([^AEIOU ])IR/g, '$1UR',
  /([^AEIOU ])S\b/g, '$1Z',
  /([^ AEIOU]) OV\b/g, '$1A',

  /N\'T/g, 'NT',
  /OAR/g, 'OR',
  /IGHT/g, 'ITE',
  /([AEIOU])S([BDFJV])/g, '$1Z$2',
  /CEIV/g, 'CEEV',
  /AUGHT/g, 'AWT',
  /OO/g, 'U',
  /U([^ AEIOU])E/g, 'OO$1',
  /U([^ AEIOU]I)/g, 'OO$1',
  /CIOUS/g, 'SHUS',
  /OUCH/g, 'OWCH',
  /ISON/g, 'ISUN',
  /OIS/g, 'OYZ',

  /\bSEAR/g, 'SER',
  /\bSEA/g, 'SEE',
  /\bGOD/g, 'CEILING CAT',
  /\bHEAVEN/g, 'CEILING',

  /([AEIOU])[SZ]E/g, '$1Z',

  /\bI AM\b/g, 'I',
  /\bIZ A\b/g, 'IS',
  /\bHAZ NO\b/g, 'NO HAZ',
  /\bDO YOU\b/g, 'YOU',
  /\bA ([A-Z]+)\b/g, '$1',
  /\bI IS\b/g, 'IM'
  ];
