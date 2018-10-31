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

  $(window).on("load", function() {
    if (window.location.hash && $(window.location.hash).hasClass("tab-pane")) {
      window.scrollTo(0, 0);
      setTimeout(function() {
        window.scrollTo(0, 0);
      }, 1);
    }
  });

  $(document).ready(function() {

    var href_hash = window.location.hash;
    // Add a class if right column is non-existant.
    if ($("#rightcolumn").length == 0) {
      $("#midcolumn").attr("class", "no-right-sidebar");
      if (href_hash) {
        window.location.hash = href_hash;
      }
    }
    // add a class if left column is non-existant.
    if ($('#main-sidebar').length == 0) {
      $("#midcolumn").attr("class", "no-left-nav");
      if (href_hash) {
        window.location.hash = href_hash;
      }
    }

    $('#showalltabs').click(function() {
      $('.tabs li').each(function(i, t) {
        $(this).removeClass('active');
      });
      $('.tab-pane').each(function(i, t) {
        $(this).addClass('active');
      });
    });

    href_hash && $('ul.nav a[href="' + href_hash + '"]').tab('show');
  });

  // This code will prevent unexpected menu close when
  // using some components (like accordion, forms, etc).
  $(document).on("click", ".yamm .dropdown-menu", function(e) {
    e.stopPropagation()
  });

  // scroll button.
  $(window).on("load resize scroll", function() {
    if ($(window).width() < 1270) {
      $('.scrollup').hide();
      return false;
    }
    if ($(this).scrollTop() > 100) {
      $('.scrollup').fadeIn();
    } else {
      $('.scrollup').fadeOut();
    }
  });

  // scroll back to the top of the page.
  $('.scrollup').click(function() {
    $("html, body").animate({
      scrollTop: 0
    }, 600);
    return false;
  });

  $('.nav-tabs a').click(function(e) {
    $(this).tab('show');
    history.pushState({}, "", this.href);
    $('.alert:not(.stay-visible)').remove();
  });

  $("a[data-tab-destination]").on('click', function() {
    var tab = $(this).attr('data-tab-destination');
    $("#" + tab).click();
  });

  $('.solstice-collapse').click(function() {
    $(this).find('i').toggleClass('fa-chevron-down fa-chevron-up');
  });

  feather.replace();

  $('.featured-highlights-item').matchHeight();
  $('.featured-story-block').matchHeight();
  $('.news-list-match-height .media-link').matchHeight({
    byRow: false
  });

  // Focus on the Google search bar when dropdown menu is being shown
  $('.main-menu-search').on('shown.bs.dropdown', function() {
    $('.gsc-input').focus();
  });

  // Hide search on ESC key.
  // @todo: Find a way to make it work when focus is on an input field.
  $(document).bind('keydown', '27', function(e) {
    $('.eclipse-search a').dropdown("toggle");
  });

  // If the Manage Cookies button from the toolbar is clicked,
  // open the cookie consent popup.
  $('.toolbar-manage-cookies').click(function() {
    $('.cc-window').show();
    setTimeout(function() {
      $('.cc-window').removeClass('cc-invisible');
    }, 20);
  });

  eclipseFdnVideos.replace();


  // Toggle Text of an HTML element
  var view_more_button_text = $('.toggle-text').html();
  $('.toggle-text').click(function() {
    if ($(this).hasClass('toggle-text-close')) {
      $(this).removeClass('toggle-text-close').html(view_more_button_text);
    } else {
      $(this).addClass('toggle-text-close').html($(this).attr('data-toggle-text'));
    }
  });

  // Infra 2791 - Send events to Google Analytics
  $('a[href]').click(function() {
    if (typeof ga === "function") {
      // Get the file name out of the href attribute
      var fileName = $(this).attr('href').split('/').pop();

      // Get the file extension
      var fileExtension = fileName.split('.').pop();

      // Quit here if the extension of the clicked file isn't part of the following list
      // and if the Google Analytics is not loaded
      var tracker = ga.getAll()[0].get('name');
      if (tracker && $.inArray(fileExtension, ['pdf', 'jpg', 'png', 'zip', 'dmg', 'gz']) !== -1) {
        // Send the event to Google Analytics
        ga(tracker + '.send', 'event', {
          'eventCategory': 'solstice-event-tracker',
          'eventAction': window.location.href,
          'eventLabel': fileName
        });
      }
    }
  });

})(jQuery, document);