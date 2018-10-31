/*
 *  solstice-cookies.js
 *  Various functionality for the Eclipse donation forms
 *
 *  Made by Eric Poirier <eric.poirier@eclipse-foundation.org>
 *  
 *  Under EPL-v2 License
 */

/**
 * Create or Update a cookie
 *
 * @param string name - Name of the cookie
 * @param string value - Value of the cookie
 * @param string path - Path of the cookie
 */
function solsticeCreateCookie(name, value, path) {
  document.cookie = name+"=" + escape(value) + "; path=" + path + ";";
}

/**
 * Fetch a specific cookie based on the name
 *
 * @param string name - Name of the cookie
 *
 * @return string
 */
function solsticeFetchCookie(name) {

  var cookie_value = "";
  var current_cookie = "";
  var name_and_equal = name + "=";
  var all_cookies = document.cookie.split(";");
  var number_of_cookies = all_cookies.length;

  for(var i = 0; i < number_of_cookies; i++) {
    current_cookie = all_cookies[i].trim();
    if (current_cookie.indexOf(name_and_equal) == 0) {
      cookie_value = current_cookie.substring(name_and_equal.length, current_cookie.length);
      break;
    }
  }

  return cookie_value;
}