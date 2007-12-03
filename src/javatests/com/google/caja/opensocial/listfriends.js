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
  document.getElementById('message').innerHTML = 'Requesting friends...';
  var req = opensocial.newDataRequest();
  req.add(req.newFetchPersonRequest('VIEWER'), 'viewer');
  req.add(req.newFetchPeopleRequest ('VIEWER_FRIENDS'), 'viewerFriends');
  req.send(onLoadFriends);
};
_IG_RegisterOnloadHandler(init);


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

  document.getElementById('message').innerHTML = html;
};
