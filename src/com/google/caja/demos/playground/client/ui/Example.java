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
  HISTORY("http://www.thinkfu.com/history.html", 
      Type.ATTACK, "Sniffing history"),
  REDIRECTION("http://www.thinkfu.com/redirection.html", 
      Type.ATTACK, "Redirecting the window"),
  COOKIES("http://www.thinkfu.com/cookies.html", 
      Type.ATTACK, "Stealing cookies"),
  LIFE("http://www.thinkfu.com/cajalife/index.php", 
      Type.APPS, "Game of Life"),
  FLASH("http://www.thinkfu.com/flash.html", 
        Type.APPS, "Embed Flash"),
  MARKDOWN("http://www.thinkfu.com/markdown.html", 
      Type.APPS, "Markdown Editor"),
  UNBOXED("http://www.thinkfu.com/unboxed/index.html", 
          Type.APPS, "Unboxed Game"),
  CLOCK("http://www.thinkfu.com/clock.html", 
          Type.APPS, "Canvas Clock"),

  // Benchmarks
  THREED_CUBE("3d-cube.html", Type.BENCHMARK, "3d-cube"),
  THREED_MORPH("3d-morph.html", Type.BENCHMARK, "3d-morph"),
  THREED_RAYTRACE("3d-raytrace.html", Type.BENCHMARK, "3d-raytrace"),
  ACCESS_BINARY_TREES("access-binary-trees.html",
      Type.BENCHMARK, "access-binary-trees"),
  ACCESS_FANNKUCH("access-fannkuch.html", Type.BENCHMARK, "access-fannkuch"),
  ACCESS_NBODY("access-nbody.html", Type.BENCHMARK, "access-nbody"),
  ACCESS_NSIEVE("access-nsieve.html", Type.BENCHMARK, "access-nsieve"),
  BITOPS_3BIT_BITS_IN_BYTE("bitops-3bit-bits-in-byte.html",
      Type.BENCHMARK, "bitops-3bit-bits-in-byte"),
  BITOPS_BITS_IN_BYTE("bitops-bits-in-byte.html",
      Type.BENCHMARK, "bitops-bits-in-byte"),
  BITOPS_BITWISE_AND("bitops-bitwise-and.html",
      Type.BENCHMARK, "bitops-bitwise-and"),
  BITOPS_NSIEVE_BITS("bitops-nsieve-bits.html",
      Type.BENCHMARK, "bitops-nsieve-bits"),
  CONTROLFLOW_RECURSIVE("controlflow-recursive.html",
      Type.BENCHMARK, "controlflow-recursive"),
  CRYPTO_AES("crypto-aes.html", Type.BENCHMARK, "crypto-aes"),
  CRYPTO_MD5("crypto-md5.html", Type.BENCHMARK, "crypto-md5"),
  CRYPTO_SHA1("crypto-sha1.html", Type.BENCHMARK, "crypto-sha1"),
  DATE_FORMAT_TOFTE("date-format-tofte.html",
      Type.BENCHMARK, "date-format-tofte"),
  DATE_FORMAT_XPARB("date-format-xparb.html",
      Type.BENCHMARK, "date-format-xparb"),
  MATH_CORDIC("math-cordic.html", Type.BENCHMARK, "math-cordic"),
  MATH_PARTIAL_SUMS("math-partial-sums.html",
      Type.BENCHMARK, "math-partial-sums"),
  MATH_SPECTRAL_NORM("math-spectral-norm.html",
      Type.BENCHMARK, "math-spectral-norm"),
  REGEXP_DNA("regexp-dna.html", Type.BENCHMARK, "regexp-dna"),
  STRING_BASE64("string-base64.html", Type.BENCHMARK, "string-base64"),
  STRING_FASTA("string-fasta.html", Type.BENCHMARK, "string-fasta"),
  STRING_TAGCLOUD("string-tagcloud.html", Type.BENCHMARK, "string-tagcloud"),
  STRING_UNPACK_CODE("string-unpack-code.html",
      Type.BENCHMARK, "string-unpack-code"),
  STRING_VALIDATE_INPUT("string-validate-input.html",
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
    BENCHMARK("Benchmarks"),
    TAMING("Taming");
    
    public final String description;
    
    Type(String description) {
      this.description = description;
    }
  }
}


