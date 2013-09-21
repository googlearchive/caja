#!/usr/bin/python

# Copyright (C) 2013 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Simple HTTP server for saving files from test harnesses.

Provides an endpoint for POST-ing files that get saved in the current
directory. Accessible on port 8000 or the port specified as the first argument.

Examples:

  postfile.py      -- Serve "." on port 8000
  postfile.py 9090 -- Serve "." on port 9090
"""

import os
import sys
import time
import urllib2

from BaseHTTPServer import HTTPServer
from BaseHTTPServer import BaseHTTPRequestHandler
    
########################################################################

class PostRequestHandler(BaseHTTPRequestHandler):

    def write_file(self, path, content):
        f = open(path, 'w')
        f.write(content)
        f.close()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'POST')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Content-Length')
        self.send_header('Access-Control-Max-Age', '86400')
        self.end_headers()

    def do_POST(self):
        self.send_response(200)
        self.end_headers()
        self.write_file(
            urllib2.unquote(self.path)[1:].replace(os.sep, '_'),
            self.rfile.read(int(self.headers['Content-Length'])))

########################################################################

port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
httpd = HTTPServer(('', port), PostRequestHandler)
httpd.serve_forever()
