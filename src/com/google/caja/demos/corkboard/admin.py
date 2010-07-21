# See the file LICENSE.txt in this directory for license information for the
# Caja Corkboard demo.

"""
Entry point for administration tools for the corkboard demo.

app.yaml specifies that this is restricted to admins, so it doesn't do any
authorization checks itself.

@author kpreid@switchb.org
"""

from google.appengine.api import users
from google.appengine.api import memcache

from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app

import logging

# application-defined modules
import cb_common

class AdminPage(webapp.RequestHandler):
  def get(self):
    self.response.out.write(cb_common.doTemplate(
      "admin", {}, self.request))

class FlushCacheHandler(webapp.RequestHandler):
  def post(self):
    memcache.flush_all()
    self.redirect('/admin/')

application = webapp.WSGIApplication([
  ('/admin/', AdminPage),
  ('/admin/flush', FlushCacheHandler),
], debug=True)

def main():
    run_wsgi_app(application)

if __name__ == "__main__":
    main()