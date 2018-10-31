/*!
 * Copyright (c) 2018 Eclipse Foundation, Inc.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * Contributors:
 *   Eric Poirier <eric.poirier@eclipse-foundation.org>
 * 
 * SPDX-License-Identifier: EPL-2.0
*/

(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    define(['ef'], factory(root));
  }
  else if (typeof exports === 'object') {
    module.exports = factory(require('ef'));
  }
  else {
    root.eclipseFdnVideos = factory(root, root.ef);
  }
})(this, function(ef) {

  'use strict';

  // Define object
  var eclipseFdnVideos = {};

  // Default settings
  var default_options = {
    selector: ".eclipsefdn-video",
    resolution: "16by9",
    cookie: {
      name: "eclipse_cookieconsent_status",
      value: "allow"
    }
  };

  /**
   * Replace some videos
   * @public
   * @param {Object} options Videos attributes
   */
  eclipseFdnVideos.replace = function (options) {

    // Go through the parameters of Options if its defined and is an object
    if (typeof(options) !== 'undefined' && typeof(options) === 'object') {
      for (var optionName in default_options) {
        if (optionName === "cookie" && typeof(options["cookie"]) !== "object") {
          continue;
        }
        if (typeof(options[optionName]) === 'undefined' || typeof(options[optionName]) !== 'string') {
          continue;
        }
        if (optionName === "resolution" && options["cookie"] !== "16by9" || options["cookie"] !== "4by3") {
          continue;
        }
        default_options[optionName] = options[optionName];
      }
    }

    // Select all the videos containing the selector Class
    this.el = document.querySelectorAll(default_options['selector']);

    var cookie_value = "";
    if (typeof(default_options['cookie']['name']) !== 'undefined') {
      // Get the cookie consent value
      var value = "; " + document.cookie;
      var parts = value.split("; " + default_options['cookie']['name'] + "=");
      if (parts.length >= 2) {
        cookie_value = parts.pop().split(";").shift();
      }
    }

    for (var i = 0; i < this.el.length; i++) {

      // Give the proper height to the element
      var width = this.el[i].offsetWidth;
      var resolution_sizes = default_options['resolution'].split("by");
      var percentage = resolution_sizes[1] / resolution_sizes[0] * 100;
      
      // Set the appropriate height for the video
      this.el[i].setAttribute("style","height:" + width * percentage / 100 + "px;");

      if (cookie_value === default_options['cookie']['value']){
        // Create a div to wrap the viewo
        var video_container = document.createElement("div");
        video_container.className = "eclipsefdn-video embed-responsive embed-responsive-" + default_options['resolution'];
        video_container.innerHTML = '<iframe src="' + this.el[i].getAttribute("href").replace(/watch\?v\=/i, "embed/") + '"></iframe>';

        // Replace the original element with the embeded video
        this.el[i].parentNode.replaceChild(video_container, this.el[i]);
      }
    }
  };

  return eclipseFdnVideos;

});