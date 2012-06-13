// Copyright (C) 2009 Google Inc.
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

package com.google.caja.lang.html;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.util.Criterion;
import com.google.caja.util.Strings;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A string criterion that can be represented as a regular expression.
 *
 * @author mikesamuel@gmail.com
 */
public interface RegularCriterion extends Criterion<String> {
  String toRegularExpression();

  static final class Factory {
    static RegularCriterion fromPattern(String regex) {
      final Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
      return new RegularCriterion() {
        public String toRegularExpression() {
          StringBuilder sb = new StringBuilder();
          sb.append('/');
          Escaping.normalizeRegex(p.pattern(), sb);
          sb.append('/');
          if ((p.flags() & Pattern.CASE_INSENSITIVE) != 0) { sb.append('i'); }
          return sb.toString();
        }

        public boolean accept(String candidate) {
          return p.matcher(candidate).find();
        }
      };
    }

    /**
     * Creates a criterion that matches only strings that match an input value
     * case-insensitively.
     */
    static RegularCriterion fromValueSet(Iterable<String> values) {
      final Set<String> valueSet = new LinkedHashSet<String>();
      for (String value : values) {
        valueSet.add(Strings.toLowerCase(value));
      }
      return new RegularCriterion() {
        public String toRegularExpression() {
          StringBuilder sb = new StringBuilder();
          sb.append("/^(?:");
          boolean first = true;
          for (String value : valueSet) {
            if (first) {
              first = false;
            } else {
              sb.append('|');
            }
            Escaping.escapeRegex(value, true, false, sb);
          }
          sb.append(")$/i");
          return sb.toString();
        }

        public boolean accept(String candidate) {
          return valueSet.contains(Strings.toLowerCase(candidate));
        }
      };
    }

    static RegularCriterion and(
        final RegularCriterion a, final RegularCriterion b) {
      if ("".equals(a.toRegularExpression())) { return b; }
      if ("".equals(b.toRegularExpression())) { return a; }
      return new RegularCriterion() {
        public String toRegularExpression() {
          String ra = stripDelims(a.toRegularExpression());
          String rb = stripDelims(b.toRegularExpression());
          // Use a positive lookahead assertion to require both to match.
          return "/^(?=" + ra + "$)(?:" + rb + "$)/i";
        }

        public boolean accept(String candidate) {
          return a.accept(candidate) && b.accept(candidate);
        }

        private String stripDelims(String regex) {
          if (!(regex.startsWith("/") && regex.endsWith("/i"))) {
            throw new SomethingWidgyHappenedError(
                "Incorrect regular expression format");
          }
          return regex.substring(1, regex.length() - 2);
        }
      };
    }

    static RegularCriterion or(
        final RegularCriterion a, final RegularCriterion b) {
      return new RegularCriterion() {
        public String toRegularExpression() {
          return "(?:" + a.toRegularExpression()
              + "|" + b.toRegularExpression() + ")";
        }

        public boolean accept(String candidate) {
          return a.accept(candidate) || b.accept(candidate);
        }
      };
    }

    static RegularCriterion optimist() {
      return new RegularCriterion() {
        public String toRegularExpression() { return ""; }
        public boolean accept(String candidate) { return true; }
      };
    }
  }
}
