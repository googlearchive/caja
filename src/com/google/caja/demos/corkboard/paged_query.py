# See the file LICENSE.txt in this directory for license information for the
# Caja Corkboard demo.

"""
Elaborated from the paging discussion at
 http://code.google.com/appengine/articles/paging.html

@author kpreid@switchb.org
"""


import re
import datetime
from urllib import quote_plus
import logging

def paged_query(model, orderProp, baseURL, pageSize, before, after):
  """Query the App Engine data store for a set of paged results ("next N"/
  "previous N" links) based on reverse date order.

  model: The model class to query for.
  orderProp: The name of the datetime-valued property.
  baseURL: The URL to generate links using, which should take ?before and ?after
           parameters.
  pageSize: The number of results to show per page.
  before, after: The values of the "before" and "after" parameters to the page,
                 or the empty string if absent.
  """
  datestr = before or after
  if datestr != "":
    #date = datetime.datetime.strptime(datestr, "%Y-%m-%dT%H:%M:%S.%f")
    # above not supported in py 2.5, so chop subsecond info and readd
    (datestr, subsec) = re.match(r'^(.+?)(\.\d+)$', datestr).groups()
    date = datetime.datetime.strptime(datestr, "%Y-%m-%dT%H:%M:%S") \
         + datetime.timedelta(seconds=float(subsec))
  else:
    date = None

  # handle paging
  query = model.all()
  if date is None:
    query = query.order('-'+orderProp)
  elif before:
    query = query.filter(orderProp+" <=", date).order('-'+orderProp)
  elif after:
    query = query.filter(orderProp+" >=", date).order(orderProp)

  postings = query.fetch(pageSize + 1)

  def dateurl(direction, dt):
    return "%s?%s=%s" % (baseURL, direction, quote_plus(dt.isoformat()))

  if after:
    olderLink = dateurl("before", date - datetime.timedelta(microseconds=1))
  elif len(postings) > pageSize:
    olderLink = dateurl("before", postings[-1].dateModified)
  else:
    olderLink = None
  if before:
    newerLink = dateurl("after", date + datetime.timedelta(microseconds=1))
  elif after and len(postings) > pageSize:
    newerLink = dateurl("after", postings[-1].dateModified)
  else:
    newerLink = None

  postings = postings[:pageSize] # strip extra for link generation
  if after:
    # results are reversed
    postings = postings[::-1]

  return (postings, olderLink, newerLink)
