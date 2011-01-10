from google.appengine.api import users
from google.appengine.ext import db
from google.appengine.ext import webapp
from google.appengine.ext.webapp import template
webapp.template.register_template_library('templateext')

import os, urllib, urllib2

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
    "recaptcha_public_key": getCaptchaKeys().public,
  }
  moreValues.update(values)
  return template.render(path, moreValues)

# ------------------------------------------------------------------------------
#
# Captcha

class ReCaptchaKeys(db.Model):
  private = db.TextProperty()
  public = db.TextProperty()

_captchaKeys = None

def getCaptchaKeys():
  global _captchaKeys
  if _captchaKeys is None:
    records = ReCaptchaKeys.all().fetch(1)
    if len(records) > 1:
      for junk in records[1:]:
        junk.delete()
    if len(records) < 1:
      _captchaKeys = ReCaptchaKeys()
      _captchaKeys.public = ""
      _captchaKeys.private = ""
      _captchaKeys.put()
    else:
      _captchaKeys = records[0]
    if _captchaKeys.public == "" or _captchaKeys.private == "":
      raise Exception(
        "ReCaptchaKeys not filled in -- fix on admin page.")
  return _captchaKeys
    
def verifyCaptcha(request):
  challenge = request.get("recaptcha_challenge_field")
  response = request.get("recaptcha_response_field")
  result = urllib2.urlopen(urllib2.Request(
    "http://www.google.com/recaptcha/api/verify",
    urllib.urlencode({
      "privatekey": getCaptchaKeys().private,
      "remoteip": request.remote_addr,
      "challenge": challenge,
      "response": response,
    })))
  lines = result.read().split("\n")
  # XXX make use of error code response
  return {"true": True, "false": False}[lines[0]]
