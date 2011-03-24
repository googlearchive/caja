#!/usr/bin/env python

import os
import re
import sys

def mangle(testName):
    html = file(testName + '.html').read()
    test_name = re.sub(r'-', '_', testName)
    html = re.sub(r'console', 'console-' + testName, html)
    html = re.sub(r'record\(', 'record_' + test_name + '(', html)
    html = re.sub(r'(?m)^\<title\>.*$', '', html);
    html = re.sub(r'(?m)^\<link .*$', '', html);
    return html

print('<style>')
print('  h3 { font-size: small; font-weight: normal; margin: .1em 0 0 0; }')
print('  h3 + div { width: 5em; text-align: right; }')
print('</style>')
for line in file('LIST-COMBINE'):
    print(mangle(line.strip()))
