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
   * Cajoles input and returns cajoled output and error messages
   * @param uri Set input source to uri (used only for error messages)
   * @param input Source to cajole
   * @return cajoled html, js and cajoling messages as an array
   *         as follows:
   *           return[0] -> html
   *           return[1] -> js
   *           return[2 .. (return.length - 1)] -> messages
   */
  // TODO(jasvir): Coax gwt to serialize and return CajolingResult instead
  String[] cajole(String uri, String input);
  
  /**
   * Returns build info as a string
   */
  String getBuildInfo();

  /**
   * Fetches the document located at {@code uri} as a string
   * @param url
   * @return the document if it exists, null else
   */
  // TODO(jasvir): Fetching ought to be done via a separate service
  String fetch(String url);
}
