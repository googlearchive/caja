package com.google.caja.flash {
  import flash.display.Loader;
  import flash.display.LoaderInfo;
  import flash.display.Sprite;
  import flash.events.Event;
  import flash.net.URLLoader;
  import flash.net.URLLoaderDataFormat;
  import flash.net.URLRequest;
  import flash.system.ApplicationDomain;
  import flash.system.LoaderContext;
  import flash.system.Security;
  import flash.system.SecurityDomain;
  import flash.utils.ByteArray;
  import flash.external.ExternalInterface;
  
  /**
   * Host for the code rewriter, should be hosted
   * in an untrusted domain.
   * @author evn@google.com (Eduardo Vela)
   */
  public class Rewriter extends Sprite {
    public static var movieLoaderInfo:LoaderInfo;
    public function Rewriter() {
      Security.allowInsecureDomain('*');
      if (stage) { init(); }
      else { addEventListener(Event.ADDED_TO_STAGE, init); }
    }
    public function getMovieLoaderInfo():LoaderInfo {
      return movieLoaderInfo;
    }
    private function init(e:Event = null):void {
      removeEventListener(Event.ADDED_TO_STAGE, init);
      var s:String = this.loaderInfo.parameters["src"];
      if (!s) {
        throw new Error("No 'src' parameter!");
      }
      var l:Loader = new Loader();
      var u:URLRequest = new URLRequest(s);
      movieLoaderInfo = this.loaderInfo;
      l.load(u, new LoaderContext(true,
          ApplicationDomain.currentDomain, 
          SecurityDomain.currentDomain));
      l.x = 0;
      l.y = 0;
      addChild(l);
    }
  }
}
