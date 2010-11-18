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

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A part that may substitute for a placeholder in an error or logging message.
 *
 * @author mikesamuel@gmail.com
 */
@ParametersAreNonnullByDefault
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

    public static MessagePart valueOf(String s) {
      return new MessagePartWrapper(s);
    }

    public static MessagePart valueOf(Number n) {
      return new MessagePartWrapper(n);
    }

    public static MessagePart valueOf(int n) {
      return new MessagePartWrapper(n);
    }

    public static MessagePart valueOf(long n) {
      return new MessagePartWrapper(n);
    }

    public static MessagePart valueOf(double n) {
      return new MessagePartWrapper(n);
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

    private static class MessagePartWrapper implements MessagePart {
      private final Object wrapped;
      MessagePartWrapper(Object wrapped) {
        if (wrapped == null) { throw new NullPointerException(); }
        this.wrapped = wrapped;
      }
      public void format(MessageContext context, Appendable out)
          throws IOException {
        out.append(wrapped.toString());
      }
      @Override
      public boolean equals(Object o) {
        if (!(o instanceof MessagePartWrapper)) { return false; }
        return this.wrapped.equals(((MessagePartWrapper) o).wrapped);
      }
      @Override
      public int hashCode() {
        return wrapped.hashCode() ^ 0x2ed53af2;
      }
      @Override
      public String toString() { return wrapped.toString(); }
    }

    private static class ArrayPart implements MessagePart {
      private final MessagePart[] partArr;

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

      @Override
      public int hashCode() {
        return Arrays.hashCode(partArr) ^ 0x78abcd35;
      }

      @Override
      public boolean equals(Object o) {
        if (!(o instanceof ArrayPart)) { return false; }
        ArrayPart that = (ArrayPart) o;
        if (that.partArr.length != this.partArr.length) { return false; }
        for (int i = partArr.length; --i >= 0;) {
          if (!this.partArr[i].equals(that.partArr[i])) { return false; }
        }
        return true;
      }
    }
  }
}
