package com.google.caja.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Set;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.util.Pair;

/**
 * Resolves config file imports to {@code file:///} URLs using a whitelist of
 * files.  If a URI is not a {@code file:///} URI then it will fall back to
 * the default resolver: {@link ConfigUtil#RESOURCE_RESOLVER}.
 *
 * @author mikesamuel@gmail.com
 */
public final class AllowedFileResolver implements ImportResolver {
  private final Set<File> allowedFiles;

  /**
   * @param allowedFiles a set of absolute paths.  This should include all
   *   files resolved by this resolver, i.e. both the inputs and all
   *   dependencies of a {@link com.google.caja.tools.BuildCommand}.
   */
  public AllowedFileResolver(Set<File> allowedFiles) {
    this.allowedFiles = allowedFiles;
  }

  public Pair<Reader, FilePosition> resolve(
      URI uri, URI base, FilePosition refPos)
      throws IOException {
    if (base != null && "file".equals(base.getScheme())
        && uri.getScheme() == null && uri.getAuthority() == null
        && uri.getQuery() == null && uri.getPath() != null) {
      uri = base.resolve(uri);
    }
    if ("file".equals(uri.getScheme())) {
      File f = new File(uri);
      if (allowedFiles.contains(f)) {
        return Pair.pair(
            (Reader) new InputStreamReader(new FileInputStream(f), "UTF-8"),
            FilePosition.startOfFile(new InputSource(uri)));
      }
    }
    return ConfigUtil.RESOURCE_RESOLVER.resolve(uri, base, refPos);
  }
}
