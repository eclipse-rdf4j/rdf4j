/**
 * Checksum ajax call
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
    $(".btn-ajax-checksum").click(function() {
      var location = $(this).attr('href');
      var file = $(this).data('checksum-file');
      var type = $(this).data('checksum-type');
      if ($.inArray(type, ["md5", "sha512", "sha1"]) === -1 || file === undefined) {
        return false;
      }
      $.ajax({
        url : 'sums.php',
        data: {'file': file, 'type': type},
        dataType: 'text',
        type: 'GET',
        beforeSend: function(){
          $(location + ' p').html('<i class="fa fa-spinner fa-spin"></i>');
        },
        success: function(response){ 
          $(location + ' p').html(response.replace(/\s/g, '&nbsp;'));
        },
        error: function(){
          $(location + ' p').text('An error has occurred.');
        }
      });
    });

    $('.mirror-continent').slice(1).hide();
    $('#show_all_mirrors').click( function(e) { 
      e.preventDefault();
      if ($('.mirror-section-2').is(":visible")) {
        $('.mirror-continent').slice(1).hide();
        $('html, body').animate({
            scrollTop: $(".mirror-section-1").offset().top
        }, 500);
        $(this).text('Show all');
      }
      else{
        $('.mirror-continent').show();
        $('html, body').animate({
            scrollTop: $(".mirror-section-2").offset().top
        }, 500, 'swing', function() {
          $('#show_all_mirrors').text('Hide');
        });
      }
      return FALSE;
    });
    
    $('.close-choose-mirror-well').click(function() {
      $('.collapse.in').collapse('hide');
    });
    
  });
})( jQuery, window, document );