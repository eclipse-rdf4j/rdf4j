/*!
 * Copyright (c) 2018 Eclipse Foundation, Inc.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * Contributors:
 *   Christopher Guindon <chris.guindon@eclipse-foundation.org>
 * 
 * SPDX-License-Identifier: EPL-2.0
*/
window.addEventListener("load", function() {
  /**
   * Override revokeChoice()
   * 
   * Avoid cookie resets when the user
   * clicks on cookie settings
   */
  window.cookieconsent.Popup.prototype.revokeChoice = function(preventOpen) {
    this.options.enabled = true;
    this.options.onRevokeChoice.call(this);
    if (!preventOpen) {
      this.autoOpen();
    }
    this.open();
  };
  
  /**
   * Remove Cookies 
   * 
   * Remove cookies except whitelist
   */
  window.cookieconsent.Popup.prototype.removeCookies = function() {
    var whitelist = ["eclipse_cookieconsent_status", "has_js"];
    var cookies = document.cookie.split(";");
    for (var i = 0; i < cookies.length; i++) {
      var cookie = cookies[i];
      var cookie_index = cookie.indexOf("=");
      var cookie_name = cookie_index > -1 ? cookie.substr(0, cookie_index) : cookie;
      cookie_name = cookie_name.trim();
      if (whitelist === undefined || whitelist.length == 0 || whitelist.indexOf(cookie_name) == -1) {
        document.cookie = cookie_name + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;';
      }
    }
  };

  /**
   * Initialise cookieconsent
   */
  window.cookieconsent.initialise({
    type: "opt-in",
    position: "bottom",
    revokable: true,
    enabled: true,
   //animateRevokable: false,
    cookie: {
      name: "eclipse_cookieconsent_status",
      expiryDays: 90,
      domain: "." + location.hostname.split('.').reverse()[1] + "." + location.hostname.split('.').reverse()[0]
    },
    compliance: {
      "opt-in": '<div class="cc-compliance cc-highlight">{{deny}}{{allow}}</div>',
    },
    onStatusChange: function(status, chosenBefore) {
      // Cookies are not allowed, delete them
      document.cookie = 'eclipse_cookieconsent_status=' + status +'; expires=0; path=/;';
      if (status !== 'allow') {
        this.removeCookies();
      }
    },
    onInitialise: function(status, options) {
      var toolbar = document.getElementsByClassName("toolbar-manage-cookies");
      if (toolbar.length <= 0) {
        setTimeout(function() {
          document.getElementsByClassName("cc-revoke")[0].style.display = "block";
        });
      }
    },
    revokeBtn: '<div class="cc-revoke {{classes}}">Cookie settings</div>',
    palette: {
      popup: {
        background: "#353434",
        text: "#ffffff"
      },
      highlight: {
        background: "#fff",
        text: "#000000"
      },
      button: {
        background: "#da7a08",
        text: "#ffffff"
      }
    },
    content: {
      href: "https://www.eclipse.org/legal/privacy.php",
      dismiss: "Dismiss",
      link: "click here.",
      message: "Some Eclipse Foundation pages use cookies to better serve you when you return to the site. You can set your browser to notify you before you receive a cookie or turn off cookies. If you do so, however, some areas of some sites may not function properly. To read Eclipse Foundation Privacy Policy"
    }
  })
});
