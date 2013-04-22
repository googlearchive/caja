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
// limitations under the License.package com.google.caja.util;

package com.google.caja.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;

public class ThisHostName {
  private static String _value = null;

  /**
   * Try to return a non-loopback hostname for this host,
   * which other hosts can use to contact it.
   */
  public static String value() {
    if (_value == null) {
      _value = computeValue();
    }
    return _value;
  }

  private static String computeValue() {
    InetAddress localhost;
    try {
      localhost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      return "localhost";
    }
    if (!localhost.isLoopbackAddress()) {
      return localhost.getHostAddress();
    }
    try {
      Enumeration<NetworkInterface> interfaces =
          NetworkInterface.getNetworkInterfaces();
      for (NetworkInterface intf : Collections.list(interfaces)) {
        for (InetAddress ia : Collections.list(intf.getInetAddresses())) {
          if (!ia.isLoopbackAddress() && !ia.isLinkLocalAddress()) {
            return ia.getHostAddress();
          }
        }
      }
    } catch (SocketException e) {
      // ignore
    }
    return localhost.getHostAddress();
  }
}
