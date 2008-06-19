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
 * @fileoverview: This is a simple social gadget that allows a user to
 * click on a button and record his/her score. In addition it shows the
 * top scores of friends with the same gadget. It also posts an activity
 * every time a user completes n clicks.
 */


var myScore = 0;
var numClicksThisSession = 0;
var maxClicksPerSession = -1;
var viewer = null;
var friends = null;
var personAppData = null;

function foo() {
  alert(arguments);
  alert(this);
}

var init = function() {
  alert(arguments);
  document.getElementById('topScore').innerHTML = 'Requesting friends...';
  var req = opensocial.newDataRequest();
  req.add(req.newFetchPersonRequest('VIEWER'), 'viewer');
  req.add(req.newFetchPeopleRequest('VIEWER_FRIENDS'), 'viewerFriends');
  req.add(req.newFetchPersonAppDataRequest('VIEWER', 'Score'),
      'viewerAppData');
  req.add(req.newFetchPersonAppDataRequest('VIEWER_FRIENDS', 'Score'),
      'personAppData');
  req.add (req.newFetchInstanceAppDataRequest('MaxClicksPerSession'),
      'instanceAppData');
  req.send(onFetchFriendData);
};
_IG_RegisterOnloadHandler(init);


function onFetchFriendData(dataResponse) {
  // Fetch some initial values - 1 time thing
  friends = dataResponse.get('viewerFriends').getData().asArray();
  viewer = dataResponse.get('viewer').getData();
  if (viewer) {
    friends.push(viewer);
  }

  personAppData = dataResponse.get('personAppData').getData();
  var viewerAppData = dataResponse.get('viewerAppData').getData();
  if (viewerAppData) {
    personAppData[viewer.getId()] = viewerAppData[viewer.getId()];
  }

  myScore = getScore(viewer);

  var instanceAppData = dataResponse.get('instanceAppData').getData();
  if (instanceAppData) {
    maxClicksPerSession = Number(instanceAppData['MaxClicksPerSession'] || 0);
  }

  showTopScorers();
};


function getScore(person) {
  if (personAppData[person.getId()] == null) {
   return 0;
  }
  return Number(personAppData[person.getId()]['Score'] || 0);
};


function showTopScorers() {
  // Sort friends by highest score
  friends.sort(function(friend1, friend2) {
    return getScore(friend2) - getScore(friend1);
  });

  // Display top n scores
  var MAX_ITEMS_TO_DISPLAY = friends.length;
  var html = 'Top Scores:<br><ul>';
  for (var i = 0; i < MAX_ITEMS_TO_DISPLAY; i++) {
    var score = getScore(friends[i]);
    if (score > 0) {
      html += '<li>' + friends[i].getDisplayName() + '(' + score + ')</li>';
    }
  }
  html += '</ul>';
  document.getElementById('topScore').innerHTML = html;
  document.getElementById('myScore').innerHTML = 'Your score: ' + myScore;
};


function onMyClick() {
  if (maxClicksPerSession >= 0 &&
      numClicksThisSession > maxClicksPerSession) {
    var html = 'You have clicked enough times, take a break!<br>';
    html += 'Your score: ' + myScore;
    document.getElementById('myScore').innerHTML = html;
    return;
  }

  numClicksThisSession++;
  myScore++;
  var req = opensocial.newDataRequest();
  req.add(req.newUpdatePersonAppDataRequest('VIEWER', 'Score', myScore));
  req.send(function(dataResponse) {
    if (dataResponse.hadError()) {
      return; // Update failed
    }

    // Update our local copy if the request went through
    personAppData[viewer.getId()]['Score'] = myScore;
    showTopScorers();
  });

  // Post an activity for every 5 clicks
  if ((myScore % 5) == 0) {
    var activity = opensocial.newActivity(
        viewer.getDisplayName() + ' completed ' + myScore + ' clicks.',
        {'body' : 'What a click achievement! See if you can beat him/her.' }
    );
    opensocial.requestCreateActivity(activity, 'LOW');
  }
};
