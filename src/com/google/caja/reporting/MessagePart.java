// Copyright (C) 2005 Google Inc.
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

package com.google.caja.reporting;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * A part that may substitute for a placeholder in an error or logging message.
 *
 * @author mikesamuel@gmail.com
 */
public interface MessagePart {

  /**
   * Formats this part to out.
   * @param out receives the formatted form of this.
   */
  void format(MessageContext context, Appendable out) throws IOException;

  public static class Factory {
    private Factory() {
      // namespace for static methods
    }

    public static MessagePart valueOf(final String s) {
      if (null == s) { throw new NullPointerException(); }
      return new MessagePart() {
        public void format(MessageContext context, Appendable out)
        throws IOException {
          out.append(s);
        }
        @Override
        public String toString() { return s; }
      };
    }

    public static MessagePart valueOf(final Number n) {
      if (null == n) { throw new NullPointerException(); }
      return new MessagePart() {
        public void format(MessageContext context, Appendable out)
        throws IOException {
          out.append(n.toString());
        }
        @Override
        public String toString() { return n.toString(); }
      };
    }

    public static MessagePart valueOf(int n) {
      return valueOf(Integer.valueOf(n));
    }

    public static MessagePart valueOf(long n) {
      return valueOf(Long.valueOf(n));
    }

    public static MessagePart valueOf(double n) {
      return valueOf(Double.valueOf(n));
    }

    public static MessagePart valueOf(Collection<?> parts) {
      final MessagePart[] partArr = new MessagePart[parts.size()];
      int k = 0;
      for (Object p : parts) {
        if (!(p instanceof MessagePart)) {
          if (p instanceof Number) {
            p = valueOf((Number) p);
          } else {
            p = valueOf(p.toString());
          }
        }
        partArr[k++] = (MessagePart) p;
      }
      return new ArrayPart(partArr);
    }

    private static class ArrayPart implements MessagePart {
      private MessagePart[] partArr;

      ArrayPart(MessagePart[] partArr) { this.partArr = partArr; }

      public void format(MessageContext context, Appendable out)
          throws IOException {
        for (int i = 0; i < partArr.length; ++i) {
          if (0 != i) { out.append(", "); }
          partArr[i].format(context, out);
        }
      }
      @Override
      public String toString() { return Arrays.asList(partArr).toString(); }
    }

  }
}
