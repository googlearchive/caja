#!/usr/bin/python

"""
Simple HTTP server for debugging and testing.

Serves up the current directory as the root of the HTTP URL tree on
port 8000 or the port specified as the first argument.

Examples:

  httpdhere.py      -- Serve "." on port 8000
  httpdhere.py 9090 -- Serve "." on port 9090
"""

import sys
from BaseHTTPServer import HTTPServer
from SimpleHTTPServer import SimpleHTTPRequestHandler

port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
httpd = HTTPServer(('', port), SimpleHTTPRequestHandler)
httpd.serve_forever()
