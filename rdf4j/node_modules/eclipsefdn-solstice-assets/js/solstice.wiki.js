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

(function($, document) {
  var $nav = $('#page-header ul.navigation'),
      $searchLink = $nav.find('a.search-link'),
      $search = $('#nav-searchform .search-query');

  var nav = {
    focus: function(ev) {
      nav.toggle(true);
    },
    blur: function(ev) {
      nav.toggle(false);
    },
    toggle: function(bool) {
      $nav.toggleClass('searchform-enabled', bool).toggleClass('searchform-disabled', !bool);
    }
  };

  $searchLink.on({
    'click': function() {
      nav.focus();
      setTimeout(function(){
        $search.focus().select();
      }, 100);
    }
  });

  $search.on({
    'blur': nav.blur,
    'keypress': function(ev) {
      // Convert <esc> into a blur
      if (ev.keyCode === 27) { this.blur(); }
    }
  });
})(jQuery, document);