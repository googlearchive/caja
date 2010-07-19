# See the file LICENSE.txt in this directory for license information for the
# Caja Corkboard demo.

"""
Interface for using the cajoling service.

Note that this makes use of the App Engine memcache service, so it will need
changes to work outside of App Engine or if you don't want it to touch memcache.

@author kpreid@switchb.org
"""

from google.appengine.api import memcache

from django.utils import simplejson as json

import urllib2
import hashlib
import logging

cajaServer = "http://caja.appspot.com/"
memcacheNamespace = "cajoled"

def cajole(html):
  """ Given HTML, return a dict of its cajoled form with keys 'html' and 'js'.
  """
  if html == "":
    # workaround for http://code.google.com/p/google-caja/issues/detail?id=1248
    return {"html":"","js":""}
  key = hashlib.sha1(html).digest()
  value = memcache.get(key, namespace=memcacheNamespace)
  if value is None:
    try:
      result = urllib2.urlopen(urllib2.Request(
        (cajaServer + "cajole"
         + "?input-mime-type=text/html"
         + "&output-mime-type=application/json"),
        html,
        {"Content-Type": "text/html"}))
      value = json.load(result)
    except Exception, e:
      # TODO(kpreid): catch HTTP 400 errors (the cajoler input was rejected)
      # and permit the user to view or reproduce them.
      logging.exception("Error in invoking cajoler.")
      return {"html": "<strong>(Caja error)</strong>", "js": ""}
    memcache.add(key, value, namespace=memcacheNamespace)
  return value
