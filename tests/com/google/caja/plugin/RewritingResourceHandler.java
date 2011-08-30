// Copyright (C) 2011 Google Inc.
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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.util.Maps;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.resource.FileResource;
import org.mortbay.resource.Resource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * A ResourceHandler that can link resource paths to other paths (sort of like
 * an "ln -s" at the web server) and arrange to have the string content of
 * some paths rewritten via regular expressions.
 *
 * @author ihab.awad@gmail.com
 */
public class RewritingResourceHandler extends ResourceHandler {
  private final Map<String, Resource> links =  Maps.newHashMap();

  {
    setCacheControl("no-cache");
  }

  /**
   * Add a "symlink" in the path space.
   *
   * @param sourcePathInfo the source path.
   * @param targetPathInfo the target path.
   */
  public void link(String sourcePathInfo, String targetPathInfo) {
    links.put(
        targetPathInfo,
        getWrappedResource(sourcePathInfo));
  }

  /**
   * Arrange to have the string content of a resource rewritten.
   *
   * @param pathInfo the path of the resource.
   * @param match the match regular expression.
   * @param replace the replacement regular expression.
   */
  public void rewrite(String pathInfo, String match, String replace) {
    links.put(
        pathInfo,
        new RewriteResource(
            getWrappedResource(pathInfo),
            match,
            replace));
  }

  /**
   * Clear all links and rewrites.
   */
  public void clear() {
    links.clear();
  }

  private Resource getWrappedResource(String pathInfo) {
    if (links.containsKey(pathInfo)) {
      return links.get(pathInfo);
    }
    try {
      return new WrapperResource(
          new FileResource(
              URI.create(getResourceBase() + pathInfo).toURL()));
    } catch (IOException e) {
      throw new SomethingWidgyHappenedError(e);
    } catch (URISyntaxException e) {
      throw new SomethingWidgyHappenedError(e);
    }
  }

  @Override
  public Resource getResource(HttpServletRequest request)
      throws MalformedURLException {
    if (links.containsKey(request.getPathInfo())) {
      return links.get(request.getPathInfo());
    }
    return super.getResource(request);
  }

  @Override
  public void handle(String target,
                     HttpServletRequest request,
                     HttpServletResponse response,
                     int dispatch)
      throws IOException, ServletException {
    super.handle(target, request, response, dispatch);
    response.setHeader("Pragma", "no-cache");
  }
}