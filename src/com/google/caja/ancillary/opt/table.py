#!/usr/bin/python2.5

import cgi

def makeTable(json_files):
  def parseJson(json):
    return eval(json, { 'false': False, 'true': True, 'null': None }, {})
  json_maps = [parseJson(json_file.read()) for json_file in json_files]
  keys = set()
  def checkkey(key):
    if key in keys: return False
    keys.add(key)
    return True
  key_lists = [[key for key in json.iterkeys() if checkkey(key)]
               for json in json_maps]
  keys = []
  for key_list in key_lists: keys.extend(key_list)

  collapse = ''.join
  html = cgi.escape
  def toJson(v):
    if type(v) is type(False): return v and 'true' or 'false'
    if v is None: return 'null'
    return '%r' % v

  half = len(json_maps) / 2

  counts = {}
  for key in keys:
    for json_map in json_maps:
      value = json_map.get(key, ())
      pair = (key, value)
      counts[pair] = counts.get(pair, 0) + 1

  return '<table>%s</table>' % (
      collapse(
          ['<tr><th>%s%s' % (
              html(toJson(key)),
              collapse([
                  '<td class=%s>%s' % (
                      (counts.get((key, json_map.get(key))) >= half
                       and 'half' or ''),
                      html(toJson(json_map.get(key, '\u2205'))))
                  for json_map in json_maps]))
           for key in keys]))

if '__main__' == __name__:
  import sys
  styles = '''
    <style>
      td, th { font-family: monospace; text-align: left }
      th { font-weight: bold; font-size: 110% }
      .half { background: #efe; font-weight: bold }
    </style>'''
  print '<head><title>Environments</title>%s<body>%s</body>' % (
      styles, makeTable([file(infile) for infile in sys.argv[1:]]))
