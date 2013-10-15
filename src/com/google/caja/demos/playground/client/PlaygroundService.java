package com.google.caja.demos.playground.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC cajoling service.
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
@RemoteServiceRelativePath("cajole")
public interface PlaygroundService extends RemoteService {
  /*
   * GWT doesn't support enums
   */
  public final int HTML = 0;
  public final int JAVASCRIPT = 1;
  public final int ERRORS = 2;

  /**
   * Returns build info as a string
   */
  String getBuildInfo();

  /**
   * Fetches the document located at {@code uri} as a string
   * @param url the URL to fetch.
   * @return the document if it exists, null else
   */
  // TODO(jasvir): Fetching ought to be done via a separate service
  String fetch(String base, String url);
}
