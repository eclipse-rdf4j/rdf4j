/**
 * downloads.js
 *
 * Copyright (c) 2015 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christopher Guindon (Eclipse Foundation)- initial API and implementation
 */
(function( jQuery, window, document ) {
  // Reposition hover boxes so they are always centered
  $(window).on('load resize', function(){
    var itemBoxWidth = $('.downloads-items').width() + 30;
    var hoverBoxWidth = 230;
    var spacing = (itemBoxWidth - hoverBoxWidth) / 2;
    $('.downloads-items-hover-box').each(function() {
      $(this).css({ "left": spacing });
    });
  });

  // [Bug 496502] New: Eclipse Packages Platform Selection Drop-down does not update links/change anything
  jQuery(document).ready(function($) {
    function getUrlVars() {
        var vars = {};
        var parts = window.location.href.replace(/[?&]+([^=&]+)=([^&]*)/gi, function(m,key,value) {
            vars[key] = value;
        });
        return vars;
    }

    var release = getUrlVars()["release"];
    $("#osSelect").change(function() {
      var src = '?osType=' + $("option:selected", this).val();
      if(release != "undefined"){
        src += "&release=" + release;
      }
      window.location = src;
    });
  });
})( jQuery, window, document );




