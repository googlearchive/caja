#!/usr/bin/python2.4

import re
import sys
import urllib

def makeInputs(summary, comment, status=None, owner=None, labels=()):
  return ([
            ('projectname', 'google-caja'),
            ('summary', summary),
            ('status', status or 'New'),
            ('owner', owner or ''),
            ('comment', comment),
            ]
          + [('label', label) for label in labels])

def fromGvnDescription(desc, stats, owner):
  m = re.search(
      r'^\*([a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+)@(\d+) | ([a-zA-Z0-9_-]+) |', desc)
  if m is None: raise Exception('Bad description:\n%s', desc)

  cl, rev, user = m.groups()

  if False:
    BROWSE = 'http://code.google.com/p/google-caja/source/browse/changes/%s/%s'
    desc = re.sub(r'(\n   [MA] )//(trunk/[\S]+)',
                  lambda m: '%s%s' % (m.group(1), BROWSE % (cl, m.group(2))),
                  desc)
  if stats:
    stats = '  (%s)' % stats
  else:
    stats = ''
  parts = { 'cl': cl, 'rev': rev, 'user': user, 'desc': desc, 'stats': stats }

  return makeInputs(
      summary='Code review: %(cl)s%(stats)s' % parts,
      comment='gvn review %(cl)s\n\n%(desc)s' % parts,
      status='New',
      owner=owner,
      labels=['Type-CodeReview'])

def toUrl(params):
  return 'http://code.google.com/p/google-caja/issues/entry?%s' % (
      urllib.urlencode(params))

def html(s):
  for ch, repl in (
      ('&', '&amp;'), ('<', '&lt;'), ('>', '&gt;'), ('"', '&quot;')):
    s = re.sub(ch, repl, s)
  return s

def toForm(params):
  return '''
<html>
<head></head>
<body>
<form action="http://code.google.com/p/google-caja/issues/entry"
 method="GET">
%s
<input type="submit">
</form>
</body>
</html>''' % '\n'.join(['<input type="hidden" name="%s" value="%s">' % (
    html(k), html(v)) for (k, v) in params])

def usage(msg):
  print >>sys.stderr, ('Usage: gvn describe | %s -m <email> -s <diff stats>'
                       % sys.argv[0])
  print >>sys.stderr, msg
  return -1

def main(argv):
  desc = sys.stdin.read().rstrip()

  if not desc:
    return usage('Please pipe changelist description to stdin')

  stats, owner, as_form = None, None, False

  i = 1
  while i < len(argv):
    if argv[i] == '-m':
      if owner is not None: return usage('Duplicate -m')
      i += 1
      owner = argv[i]
    elif argv[i] == '-s':
      if stats is not None: return stats('Duplicate -s')
      i += 1
      stats = argv[i]
    elif argv[i] == '-f':
      as_form = True
    else:
      return usage('Unexpected flag or argument: %s' % argv[i])
    i += 1

  params = fromGvnDescription(desc=desc, stats=stats, owner=owner)
  if as_form:
    print toForm(params)
  else:
    print toUrl(params)

if __name__ == '__main__':
  sys.exit(main(sys.argv))
