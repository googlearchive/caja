/**
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @fileoverview: This is a trivial social gadget that lists the friends of
 * the viewer along with their photos.
 *
 */

/**
 * Request for friend information when the page loads.
 */
var init = function() {
  nextFriends(0);
};
_IG_RegisterOnloadHandler(init);


function nextFriends(opt_start, opt_withData) {
  document.getElementById('message').innerHTML = 'Requesting friends...';
  var req = opensocial.newDataRequest();
  req.add(req.newFetchPersonRequest('VIEWER'), 'viewer');
  var peopleKeys = {first: opt_start, max: 10,
      filter: opt_withData ? "hasApp" : "all"};
  req.add(req.newFetchPeopleRequest('VIEWER_FRIENDS', peopleKeys),
      'viewerFriends');

  req.send(onLoadFriends);
}

/**
 * Parses the response to the friend information request and generates
 * html to list the friends along with their display name and picture.
 *
 * @param {Object} dataResponse Friend information that was requested.
 */
function onLoadFriends(dataResponse) {
  var viewer = dataResponse.get('viewer').getData();
  var html = 'Friends of ' + viewer.getField('name');

  html += ':<br><ul>';
  var viewerFriends = dataResponse.get('viewerFriends').getData();
  viewerFriends.each(function(person) {
    html += '<li>' + person.getField('name') + '</li>';
  });
  html += '</ul>';

  var nextBatch = viewerFriends.getOffset() + viewerFriends.size();
  if (nextBatch < viewerFriends.getTotalSize()) {
    html += "<br><a href='#' onclick='nextFriends(" + nextBatch +
            "); return false;'>Next</a>";

  }

  if (viewerFriends.getTotalSize() > 20) {
    html += "<br><a href='#' onclick='nextFriends(0, true); return false;'>" +
        "With Data</a>";
  }

  document.getElementById('message').innerHTML = html;
}
