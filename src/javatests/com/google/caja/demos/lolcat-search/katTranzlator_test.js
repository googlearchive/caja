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

function testDickens() {
  assertEquals(
      'IT WUZ THE BESTA TIMES, IT WUZ THE WORSTA TIMES, IT WUZ THE AGE OV'
      + ' WIZDOM, IT WUZ THE AGE OV FOOLISHNES, IT WUZ THE EPOCHA BELEEF, IT'
      + ' WUZ THE EPOCHA INCREDOOLITI, IT WUZ THE SEESONA LITE, IT WUZ THE'
      + ' SEESONA DARKNES, IT WUZ THE SPRINGA HOP, IT WUZ THE WINTRA DESPEH, WE'
      + ' HAD EVERYTIN BEFOR US, WE HAD NOTIN BEFOR US, WE WERE ALL GOIN DOORCT'
      + ' 2 CEILING, WE WERE ALL GOIN DOORCT THE OTHAH WAY - IN SHORT, THE'
      + ' PERIOD WUZ SO FAR LIEK THE PREZNT PERIOD, THAT SOMA ITZ NOYZIEST'
      + ' AUTHORITEHZ INSISTD ON ITZ BEIN RECEEVD, 4 GUD OR 4 EVIL, IN THE'
      + ' SOOPRLATIV DEGREE OV COMPARISUN ONLY.',

      katTranzlator(
          'It was the best of times, it was the worst of times, it was the age'
          + ' of wisdom, it was the age of foolishness, it was the epoch of'
          + ' belief, it was the epoch of incredulity, it was the season of'
          + ' Light, it was the season of Darkness, it was the spring of hope,'
          + ' it was the winter of despair, we had everything before us, we had'
          + ' nothing before us, we were all going direct to heaven, we were'
          + ' all going direct the other way - in short, the period was so far'
          + ' like the present period, that some of its noisiest authorities'
          + ' insisted on its being received, for good or for evil, in the'
          + ' superlative degree of comparison only.')
      );
}
