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

  jQuery(document).ready(function($) {
    function getUrlVars() {
        var vars = {};
        var parts = window.location.href.replace(/[?&]+([^=&]+)=([^&]*)/gi, function(m,key,value) {
            vars[key] = value;
        });
        return vars;
    }

    $('#collapseEinstaller').on('show.bs.collapse', function () {
      $("html, body").animate({ scrollTop: $('#collapseEinstaller1').offset().top }, 1000);
    });

    $('#collapseEinstaller').on('hidden.bs.collapse', function () {
        $("html, body").animate({ scrollTop: $('body').offset().top }, 1000);
      });

    $("#osSelect").change(function() {
      var release = getUrlVars()["release"];
      var src = '?osType=' + $("option:selected", this).val();
      if (release !== undefined && release !== null) {
        src += "&release=" + release;
      }
      window.location = src;
    });
  });

})( jQuery, window, document );
