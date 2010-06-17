// Copyright (C) 2010 Google Inc.
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

package com.google.caja.plugin;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.ParseException;

import org.apache.commons.codec.binary.Base64;

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;

/**
 * Supports cross-browser support for fetching content from data uri
 * 
 * @author Jasvir Nagra <jasvir@gmail.com>
 */
public class DataUriFetcher implements UriFetcher {
  
  /**
   * From http://tools.ietf.org/html/rfc2397
   *   dataurl    := "data:" [ mediatype ] [ ";base64" ] "," data
   *   mediatype  := [ type "/" subtype ] *( ";" parameter )
   *   data       := *urlchar
   *   parameter  := attribute "=" value
   */
  private final Pattern DATA_URI_RE = 
    Pattern.compile("([^,]*?)(;base64)?,(.*)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private enum DATA_URI { ALL, TYPE, BASE64, DATA; }
  private final String DATA_URI_DEFAULT_CHARSET = "US-ASCII";
  
  private boolean isDataUri(URI uri) {
    if (null != uri  && "data".equals(uri.getScheme())
        && uri.isOpaque()) {
      return true;
    }
    return false;
  }
  
  private String charsetFromMime(String mime) {
    String charset = null;
    try {
      javax.mail.internet.ContentType parsedType = 
        new javax.mail.internet.ContentType(mime);
      charset = parsedType.getParameter("charset");
    } catch (ParseException e) {
      charset = null;
    }
    if (null == charset || "".equals(charset)) {
      return DATA_URI_DEFAULT_CHARSET;
    } else {
      return charset;
    }
  }

  public FetchedData fetch(ExternalReference ref, String mimeType)
      throws UriFetchException {
    byte[] raw = fetchBinary(ref, mimeType);
    return FetchedData.fromBytes(raw, mimeType,
        charsetFromMime(mimeType), new InputSource(ref.getUri()));
  }

  public final byte[] fetchBinary(ExternalReference ref, String mimeType)
      throws UriFetchException {
    URI uri = ref.getUri();
    if (!isDataUri(uri)) {
      throw new UriFetchException(ref, mimeType);
    }
    
    String dataUri = uri.getSchemeSpecificPart();
    // We split the data uri into the mimetype and the data portion by
    // searching for the first comma (whether encoded or not).  This is
    // not a complete parse of data URIs - specifically if the mime-type
    // contains a comma
    Matcher m = DATA_URI_RE.matcher(dataUri);
    if (m.find()) {
      try {
        String charset = charsetFromMime(m.group(DATA_URI.TYPE.ordinal()));
        boolean base64 = null != m.group(DATA_URI.BASE64.ordinal());
        byte[] encoded = m.group(DATA_URI.DATA.ordinal()).getBytes(charset);
        byte[] decoded = base64 ? new Base64().decode(encoded) : encoded;
        return decoded;
      } catch (UnsupportedEncodingException e) {
        throw new UriFetcher.UriFetchException(ref, mimeType, e);
      }
    }
    throw new UriFetcher.UriFetchException(ref, mimeType);
  }
}
