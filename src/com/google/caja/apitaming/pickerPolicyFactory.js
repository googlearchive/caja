// Copyright (C) 2012 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview
 * Policy factory for "google.picker" API.
 *
 * @author ihab.awad@gmail.com
 * @requires caja
 * @overrides window
 */
caja.tamingGoogleLoader.addPolicyFactory('picker', function(frame, utils) {

  var p = {};

  p.DocsUploadView = function() {};
  p.DocsUploadView.__super__ = Object;
  p.DocsUploadView.prototype.setIncludeFolders = function(aBoolean) {};

  p.View = function() {};
  p.View.__super__ = Object;
  p.View.prototype.getId = function() {};
  p.View.prototype.setMimeTypes = function() {};
  p.View.prototype.setQuery = function() {};

  p.DocsView = function() {};
  p.DocsView.__super__ = ['google', 'picker', 'View'];
  p.DocsView.prototype.setIncludeFolders = function() {};
  p.DocsView.prototype.setMode = function() {};
  p.DocsView.prototype.setOwnedByMe = function() {};
  p.DocsView.prototype.setStarred = function() {};

  p.DocsViewMode = {};
  p.DocsViewMode.GRID = 1;
  p.DocsViewMode.LIST = 1;

  p.Feature = {};
  p.Feature.MINE_ONLY = 1;
  p.Feature.MULTISELECT_ENABLED = 1;
  p.Feature.NAV_HIDDEN = 1;
  p.Feature.SIMPLE_UPLOAD_ENABLED = 1;

  p.ImageSearchView = function() {};
  p.ImageSearchView.__super__ = ['google', 'picker', 'View'];
  p.ImageSearchView.prototype.setLicense = function() {};
  p.ImageSearchView.prototype.setSite = function() {};
  p.ImageSearchView.prototype.setSize = function() {};

  p.ImageSearchView.License = {};
  p.ImageSearchView.License.NONE = 1;
  p.ImageSearchView.License.COMMERCIAL_REUSE = 1;
  p.ImageSearchView.License.COMMERCIAL_REUSE_WITH_MODIFICATION = 1;
  p.ImageSearchView.License.REUSE = 1;
  p.ImageSearchView.License.REUSE_WITH_MODIFICATION = 1;

  p.ImageSearchView.Size = {};
  p.ImageSearchView.Size.SIZE_QSVGA = 1;
  p.ImageSearchView.Size.SIZE_VGA = 1;
  p.ImageSearchView.Size.SIZE_SVGA = 1;
  p.ImageSearchView.Size.SIZE_XGA = 1;
  p.ImageSearchView.Size.SIZE_WXGA = 1;
  p.ImageSearchView.Size.SIZE_WXGA2 = 1;
  p.ImageSearchView.Size.SIZE_2MP = 1;
  p.ImageSearchView.Size.SIZE_4MP = 1;
  p.ImageSearchView.Size.SIZE_6MP = 1;
  p.ImageSearchView.Size.SIZE_8MP = 1;
  p.ImageSearchView.Size.SIZE_10MP = 1;
  p.ImageSearchView.Size.SIZE_12MP = 1;
  p.ImageSearchView.Size.SIZE_15MP = 1;
  p.ImageSearchView.Size.SIZE_20MP = 1;
  p.ImageSearchView.Size.SIZE_40MP = 1;
  p.ImageSearchView.Size.SIZE_70MP = 1;
  p.ImageSearchView.Size.SIZE_140MP = 1;

  p.MapsView = function() {};
  p.MapsView.__super__ = ['google', 'picker', 'View'];
  p.MapsView.prototype.setCenter = function() {};
  p.MapsView.prototype.setZoom = function() {};

  p.PhotoAlbumsView = function() {};
  p.PhotoAlbumsView.__super__ = ['google', 'picker', 'View'];

  p.PhotosView = function() {};
  p.PhotosView.__super__ = ['google', 'picker', 'View'];
  p.PhotosView.prototype.setType = function() {};

  p.PhotosView.Type = {};
  p.PhotosView.Type.FEATURED = 1;
  p.PhotosView.Type.UPLOADED = 1;

  if (window && window.google && window.google.picker && window.google.picker.Picker) {
     window.google.picker.Picker.prototype.constructor =
        window.google.picker.Picker;
  }

  /*@constructor*/
  p.Picker = function() {};
  p.Picker.__super__ = Object;
  p.Picker.prototype.isVisible = function() {};
  p.Picker.prototype.setCallback = function(c) {};
  p.Picker.prototype.setRelayUrl = function(u) {};
  p.Picker.prototype.setVisible = function(b) {};

/*
  p.PickerBuilder = function() {};
  p.PickerBuilder.__super__ = Object;
  p.PickerBuilder.prototype.0= function() {};
  p.PickerBuilder.prototype.addViewGroup = function() {};
  p.PickerBuilder.prototype.build = function() {};
  p.PickerBuilder.prototype.disableFeature = function() {};
  p.PickerBuilder.prototype.enableFeature = function() {};
  p.PickerBuilder.prototype.getRelayUrl = function() {};
  p.PickerBuilder.prototype.getTitle = function() {};
  p.PickerBuilder.prototype.hideTitleBar = function() {};
  p.PickerBuilder.prototype.isFeatureEnabled = function() {};
  p.PickerBuilder.prototype.setAppId = function() {};
  p.PickerBuilder.prototype.setAuthUser = function() {};
  p.PickerBuilder.prototype.setCallback = function() {};
  p.PickerBuilder.prototype.setDocument = function() {};
  p.PickerBuilder.prototype.setLocale = function() {};
  p.PickerBuilder.prototype.setRelayUrl = function() {};
  p.PickerBuilder.prototype.setSelectableMimeTypes = function() {};
  p.PickerBuilder.prototype.setSize = function() {};
  p.PickerBuilder.prototype.setTitle = function() {};  // TODO: Add "trusted path" annotation
  p.PickerBuilder.prototype.setUploadToAlbumId = function() {};
  p.PickerBuilder.prototype.toUri = function() {};
*/

  if (window && window.google && window.google.picker && window.google.picker.PickerBuilder) {
     window.google.picker.PickerBuilder.prototype.constructor =
        window.google.picker.PickerBuilder;
  }

  p.PickerBuilder = function() {};
  p.PickerBuilder.__super__ = Object;
  p.PickerBuilder.prototype.addView = function() {};
  p.PickerBuilder.prototype.addViewGroup = function() {};
  p.PickerBuilder.prototype.disableFeature = function() {};
  p.PickerBuilder.prototype.enableFeature = function() {};
  p.PickerBuilder.prototype.getRelayUrl = function() {};
  p.PickerBuilder.prototype.getTitle = function() {};
  p.PickerBuilder.prototype.hideTitleBar = function() {};
  p.PickerBuilder.prototype.isFeatureEnabled = function() {};
  p.PickerBuilder.prototype.setAppId = function() {};
  p.PickerBuilder.prototype.setAuthUser = function() {};
  p.PickerBuilder.prototype.setCallback = function() {};
  p.PickerBuilder.prototype.setDocument = function() {};
  p.PickerBuilder.prototype.setLocale = function() {};
  p.PickerBuilder.prototype.setRelayUrl = function() {};
  p.PickerBuilder.prototype.setSelectableMimeTypes = function() {};
  p.PickerBuilder.prototype.setSize = function() {};
  p.PickerBuilder.prototype.setTitle = function() {};
  p.PickerBuilder.prototype.setUploadToAlbumId = function() {};
  p.PickerBuilder.prototype.toUri = function() {};
  p.PickerBuilder.prototype.build = function() {};
  p.PickerBuilder.prototype.build.__around__ = [
    function(f, self, args) {
      debugger;
      var result = f(self, args);
      return result;
    }
  ];

  p.ResourceId = {};
  p.ResourceId.generate = function() {};

  p.VideoSearchView = function() {};
  p.VideoSearchView.__super__ = ['google', 'picker', 'View'];
  p.VideoSearchView.prototype.setSite = function() {};

  p.VideoSearchView.YOUTUBE = 1;

  p.ViewGroup = function() {};
  p.ViewGroup.__super__ = Object;
  p.ViewGroup.prototype.addLabel = function() {};
  p.ViewGroup.prototype.addView = function() {};
  p.ViewGroup.prototype.addViewGroup = function() {};

  p.ViewId = {};
  p.ViewId.DOCS = 1;
  p.ViewId.DOCS_IMAGES = 1;
  p.ViewId.DOCS_IMAGES_AND_VIDEOS = 1;
  p.ViewId.DOCS_VIDEOS = 1;
  p.ViewId.DOCUMENTS = 1;
  p.ViewId.FOLDERS = 1;
  p.ViewId.FORMS = 1;
  p.ViewId.IMAGE_SEARCH = 1;
  p.ViewId.PDFS = 1;
  p.ViewId.PHOTO_ALBUMS = 1;
  p.ViewId.PHOTO_UPLOAD = 1;
  p.ViewId.PHOTOS = 1;
  p.ViewId.PRESENTATIONS = 1;
  p.ViewId.RECENTLY_PICKED = 1;
  p.ViewId.SPREADSHEETS = 1;
  p.ViewId.VIDEO_SEARCH = 1;
  p.ViewId.WEBCAM = 1;
  p.ViewId.YOUTUBE = 1;

  p.WebCamView = function() {};
  p.WebCamView.__super__ = ['google', 'picker', 'View'];

  p.WebCamViewType = {};
  p.WebCamViewType.STANDARD = 1;
  p.WebCamViewType.VIDEOS = 1;

  p.Action = {};
  p.Action.CANCEL = 1;
  p.Action.PICKED = 1;

  p.Audience = {};
  p.Audience.OWNER_ONLY = 1;
  p.Audience.LIMITED = 1;
  p.Audience.ALL_PERSONAL_CIRCLES = 1;
  p.Audience.EXTENDED_CIRCLES = 1;
  p.Audience.DOMAIN_PUBLIC = 1;
  p.Audience.PUBLIC = 1;

  p.Document = {};
  p.Document.ADDRESS_LINES = 1;
  p.Document.AUDIENCE = 1;
  p.Document.DESCRIPTION = 1;
  p.Document.DURATION = 1;
  p.Document.EMBEDDABLE_URL = 1;
  p.Document.ICON_URL = 1;
  p.Document.ID = 1;
  p.Document.IS_NEW = 1;
  p.Document.LAST_EDITED_UTC = 1;
  p.Document.LATITUDE = 1;
  p.Document.LONGITUDE = 1;
  p.Document.MIME_TYPE = 1;
  p.Document.NAME = 1;
  p.Document.NUM_CHILDREN = 1;
  p.Document.PARENT_ID = 1;
  p.Document.PHONE_NUMBERS = 1;
  p.Document.SERVICE_ID = 1;
  p.Document.THUMBNAILS = 1;
  p.Document.TYPE = 1;
  p.Document.URL = 1;

  p.Response = {};
  p.Response.ACTION = 1;
  p.Response.DOCUMENTS = 1;
  p.Response.PARENTS = 1;
  p.Response.VIEW = 1;

  p.ServiceId = {};
  p.ServiceId.DOCS = 1;
  p.ServiceId.MAPS = 1;
  p.ServiceId.PHOTOS = 1;
  p.ServiceId.SEARCH_API = 1;
  p.ServiceId.URL = 1;
  p.ServiceId.YOUTUBE = 1;

  p.Thumbnail = {};
  p.Thumbnail.HEIGHT = 1;
  p.Thumbnail.WIDTH = 1;
  p.Thumbnail.URL = 1;

  p.Type = {};
  p.Type.ALBUM = 1;
  p.Type.DOCUMENT = 1;
  p.Type.PHOTO = 1;
  p.Type.URL = 1;
  p.Type.VIDEO = 1;

  return {
    version: '1',
    value: p
  };
});
