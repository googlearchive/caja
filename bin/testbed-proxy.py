#!/usr/bin/python2.4

# Copyright (C) 2008 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Usage: %s <portnum>

Launches an http server that receives messages on port 8000 and that responds to
the following requests.

/proxy
    Logs the request and redirects to the url parameter.  A real
    implementation should proxy the response so that cookies are stripped and
    match the mimeType CGI parameter against the URLs mimetype.

    Will not redirect to i_eat_your_cookie.
/log
    Logs the body
/i_eat_your_cookie
    A breach of security.
    Logs the request and returns 200 You ate my cookie.
"""

__author__ = 'mikesamuel@gmail.com'


import BaseHTTPServer
import cgi
import re
import sys


class TestBedProxy(BaseHTTPServer.BaseHTTPRequestHandler):

  def do_GET(self):
    if re.match(r'/proxy(?:[?#]|$)', self.path):
      i = self.path.find('?')
      if i >= 0:
        params = cgi.parse_qs(self.path[i + 1:])
        if ('url' in params and len(params['url']) == 1
            and 'mimeType' in params and len(params['mimeType']) == 1):
          url = params['url'][0]
          mime_type = params['mimeType'][0]
          expanded_url = re.sub(
              r'\+|%([0-9A-Fa-f]{2})',
              lambda m: m.group(1) and chr(int(m.group(1), 16)) or ' ',
              url)
          if (mime_type in ('image/*', '*/*')
              and 'i_eat_your_cookie' not in expanded_url):
            self.send_response(301)
            self.send_header('Location', url)
            self.end_headers()
            return
    elif self.path == '/i_eat_your_cookie':
      self.send_response(200, 'CookieEaten')
      self.send_header('Content-type', 'text/html;charset=UTF-8')
      self.end_headers()
      self.wfile.write(u'YOU ATE MY COOKIE!'.encode('UTF-8'))
      return
    self.send_response(404)
    self.end_headers()

  def do_POST(self):
    if self.path == '/log':
      ctype, pdict = cgi.parse_header(self.headers.getheader('Content-Type'))
      if ctype == 'application/x-www-form-urlencoded':
        qs = self.rfile.read(int(self.headers.getheader('Content-Length')))
        print '%r' % cgi.parse_qs(qs)
        self.send_response(204)
        self.end_headers()
        return
    self.send_response(404)
    self.end_headers()


def main(argv):
  try:
    if len(argv) != 2:
      print >>sys.stderr, 'Please supply a port number'
      return -1
    port = int(argv[1])

    server = BaseHTTPServer.HTTPServer(('', port), TestBedProxy)
    print 'starting %s on port %s' % (argv[0], port)
    server.serve_forever()
  except KeyboardInterrupt:
    print 'exiting'
    server.socket.close()
  return 0


if '__main__' == __name__:
  sys.exit(main(sys.argv))
