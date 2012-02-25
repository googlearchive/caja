// Copyright (C) 2009 Google Inc.
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

package com.google.caja.demos.playground.client.ui;

/**
 * List of caja examples
 * @author jasvir@google.com (Jasvir Nagra)
 */
public enum Example {
  HISTORY("examples/history.html", 
      Type.ATTACK, "Sniffing history"),
  REDIRECTION("examples/redirection.html", 
      Type.ATTACK, "Redirecting the window"),
  COOKIES("examples/cookies.html", 
      Type.ATTACK, "Stealing cookies"),
  CLOCK("examples/clock.html", 
      Type.APPS, "Canvas Clock"),
  UNBOXED("examples/unboxed/index.html", 
      Type.APPS, "Unboxed Game"),
  MARKDOWN("examples/markdown.html", 
      Type.APPS, "Markdown Editor"),
  FLASH("examples/flash.html", 
      Type.APPS, "Embed Flash"),
  FLASH2("examples/flash2.html", 
      Type.APPS, "Embed Flash 2"),
  LIFE("examples/cajalife.html", 
      Type.APPS, "Game of Life"),

  // JQuery and JQueryUI
  JQUERY_DATEPICKER("examples/jquery-datepicker.html",
      Type.JQUERY, "Date Picker"),
  JQUERY_DRAGGABLE("examples/jquery-draggable.html",
      Type.JQUERY, "Draggable"),

  // Benchmarks
  COMBINED("sunspider/combined.html", Type.BENCHMARK, "combined"),
  THREED_CUBE("sunspider/3d-cube.html", Type.BENCHMARK, "3d-cube"),
  THREED_MORPH("sunspider/3d-morph.html", Type.BENCHMARK, "3d-morph"),
  THREED_RAYTRACE("sunspider/3d-raytrace.html", Type.BENCHMARK, "3d-raytrace"),
  ACCESS_BINARY_TREES("sunspider/access-binary-trees.html",
      Type.BENCHMARK, "access-binary-trees"),
  ACCESS_FANNKUCH("sunspider/access-fannkuch.html",
      Type.BENCHMARK, "access-fannkuch"),
  ACCESS_NBODY("sunspider/access-nbody.html", Type.BENCHMARK, "access-nbody"),
  ACCESS_NSIEVE("sunspider/access-nsieve.html",
      Type.BENCHMARK, "access-nsieve"),
  BITOPS_3BIT_BITS_IN_BYTE("sunspider/bitops-3bit-bits-in-byte.html",
      Type.BENCHMARK, "bitops-3bit-bits-in-byte"),
  BITOPS_BITS_IN_BYTE("sunspider/bitops-bits-in-byte.html",
      Type.BENCHMARK, "bitops-bits-in-byte"),
  BITOPS_BITWISE_AND("sunspider/bitops-bitwise-and.html",
      Type.BENCHMARK, "bitops-bitwise-and"),
  BITOPS_NSIEVE_BITS("sunspider/bitops-nsieve-bits.html",
      Type.BENCHMARK, "bitops-nsieve-bits"),
  CONTROLFLOW_RECURSIVE("sunspider/controlflow-recursive.html",
      Type.BENCHMARK, "controlflow-recursive"),
  CRYPTO_AES("sunspider/crypto-aes.html", Type.BENCHMARK, "crypto-aes"),
  CRYPTO_MD5("sunspider/crypto-md5.html", Type.BENCHMARK, "crypto-md5"),
  CRYPTO_SHA1("sunspider/crypto-sha1.html", Type.BENCHMARK, "crypto-sha1"),
  DATE_FORMAT_TOFTE("sunspider/date-format-tofte.html",
      Type.BENCHMARK, "date-format-tofte"),
  // date-format-xparb relies on eval
  // DATE_FORMAT_XPARB("sunspider/date-format-xparb.html",
  //    Type.BENCHMARK, "date-format-xparb"),
  MATH_CORDIC("sunspider/math-cordic.html", Type.BENCHMARK, "math-cordic"),
  MATH_PARTIAL_SUMS("sunspider/math-partial-sums.html",
      Type.BENCHMARK, "math-partial-sums"),
  MATH_SPECTRAL_NORM("sunspider/math-spectral-norm.html",
      Type.BENCHMARK, "math-spectral-norm"),
  REGEXP_DNA("sunspider/regexp-dna.html", Type.BENCHMARK, "regexp-dna"),
  STRING_BASE64("sunspider/string-base64.html",
      Type.BENCHMARK, "string-base64"),
  STRING_FASTA("sunspider/string-fasta.html", Type.BENCHMARK, "string-fasta"),
  STRING_TAGCLOUD("sunspider/string-tagcloud.html",
      Type.BENCHMARK, "string-tagcloud"),
  STRING_UNPACK_CODE("sunspider/string-unpack-code.html",
      Type.BENCHMARK, "string-unpack-code"),
  STRING_VALIDATE_INPUT("sunspider/string-validate-input.html",
      Type.BENCHMARK, "string-validate-input");
  
  public final String url;
  public final Type type;
  public final String description;
  
  Example(String url, Type type, String description) {
    this.url = url;
    this.type = type;
    this.description = description;
  }
  public enum Type {
    HOWTO("How do I.."),
    WEB("Web pages"),
    APPS("Applications"),
    ATTACK("Attacks"),
    JQUERY("jQuery"),
    BENCHMARK("Benchmarks"),
    TAMING("Taming");
    
    public final String description;
    
    Type(String description) {
      this.description = description;
    }
  }
}


