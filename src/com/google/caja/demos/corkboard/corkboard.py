# See the file LICENSE.txt in this directory for license information for the
# Caja Corkboard demo.

"""
Entry point for the corkboard demo. Everything but the static files is defined
here.

@author kpreid@switchb.org
"""

from google.appengine.api import users

from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db
from google.appengine.ext.webapp import template
webapp.template.register_template_library('templateext')

import cgi
import os
import datetime
import re
import logging

# application-defined modules
from paged_query import paged_query
import cajole
from cb_common import Posting, doTemplate, verifyCaptcha

# ------------------------------------------------------------------------------
#
# Page generation

class MainPage(webapp.RequestHandler):
  def get(self):
    (postings, olderLink, newerLink) = paged_query(
        model=Posting,
        orderProp="dateModified",
        baseURL="/",
        pageSize=10,
        before=self.request.get("before"),
        after=self.request.get("after"))

    template_values = {
      'postings': postings,
      'newer': newerLink,
      'older': olderLink,
    }

    self.response.out.write(doTemplate("index", template_values, self.request))

class PostHandler(webapp.RequestHandler):
  def post(self):
    keystr = self.request.get('posting')
    newPost = keystr == ""
    if newPost:
      posting = Posting()
    else:
      posting = Posting.get(keystr)

    if not posting.editable() or not verifyCaptcha(self.request):
      # TODO: nice retry page on captcha
      self.error(403)
      return

    posting.content = self.request.get('content')
    posting.dateModified = datetime.datetime.today()
    if newPost:
      posting.author = users.get_current_user()
    
    posting.put()
    
    # preload cache; writing is expected to take longer than viewing
    posting.cajole()
    
    self.redirect('/')

class DeleteHandler(webapp.RequestHandler):
  def post(self, keystr):
    posting = Posting.get(keystr)
    if not posting.editable():
      self.error(403)
      return
    posting.delete()
    self.redirect('/')

class EditForm(webapp.RequestHandler):
  def get(self, keystr):
    posting = Posting.get(keystr)
    template_values = {
      'posting': posting,
      'user': users.get_current_user(),
    }
    self.response.out.write(doTemplate("edit", template_values, self.request))

# ------------------------------------------------------------------------------
#
# Application definition

application = webapp.WSGIApplication([
  ('/', MainPage),
  ('/post', PostHandler),
  (r'/posting/([^/]*)/edit', EditForm),
  (r'/posting/([^/]*)/delete', DeleteHandler)
], debug=True)

# ------------------------------------------------------------------------------
#
# Startup

def main():
    run_wsgi_app(application)

if __name__ == "__main__":
    main()