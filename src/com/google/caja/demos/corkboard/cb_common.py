from google.appengine.api import users
from google.appengine.ext import db
from google.appengine.ext import webapp
from google.appengine.ext.webapp import template
webapp.template.register_template_library('templateext')

import os

# application-defined modules
import cajole

# ------------------------------------------------------------------------------
#
# Data model

class Posting(db.Model):
  author = db.UserProperty()
  content = db.TextProperty()
  dateCreated = db.DateTimeProperty(auto_now_add=True)
  dateModified = db.DateTimeProperty(auto_now_add=True)
  _cajoled = None
  
  # Cajoled content is cached here as well as in memcache by cajole.py --
  # not entirely useless as we save the cost of hashing and API calls, but this
  # should be revisited.
  def cajole(self):
    if self._cajoled is None:
      self._cajoled = cajole.cajole(self.content)
    return self._cajoled
  
  def editable(self):
    """Should this posting be editable by the current user?"""
    return self.author is None or self.author == users.get_current_user()

# ------------------------------------------------------------------------------
#
# Page generation

def doTemplate(name, values, request):
  """Render a template and supply some standard variables"""
  if users.get_current_user():
    account_url = users.create_logout_url(request.uri)
    account_link = 'Logout'
  else:
    account_url = users.create_login_url(request.uri)
    account_link = 'Login'

  path = os.path.join(os.path.dirname(__file__), name + ".t.html")
  moreValues = {
    "cajaServer": cajole.cajaServer,
    "user": users.get_current_user(),
    "account_url": account_url,
    "account_link": account_link,
    "user_is_admin": users.is_current_user_admin(),
  }
  moreValues.update(values)
  return template.render(path, moreValues)

