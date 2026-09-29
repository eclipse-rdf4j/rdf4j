(function () {
	'use strict';

	var preferenceKey = 'rdf4j-workbench-theme';
	var validThemes = ['system', 'light', 'dark'];
	var root = document.documentElement;
	var media = typeof window.matchMedia === 'function'
		? window.matchMedia('(prefers-color-scheme: dark)') : null;
	var defaultMeta = document.querySelector('meta[name="rdf4j-workbench-theme-default"]');
	var configuredDefault = defaultMeta ? defaultMeta.getAttribute('content') : 'system';
	var savedPreference = readPreference();
	var hasExplicitPreference = savedPreference !== null;
	var preference = hasExplicitPreference ? savedPreference : readDefault();
	var connectedControl = null;
	var controlHandler = null;

	function isValidTheme(value) {
		return validThemes.indexOf(value) !== -1;
	}

	function readPreference() {
		try {
			var saved = window.localStorage.getItem(preferenceKey);
			return isValidTheme(saved) ? saved : null;
		} catch (error) {
			return null;
		}
	}

	function readDefault() {
		return isValidTheme(configuredDefault) ? configuredDefault : 'system';
	}

	function systemTheme() {
		return media && media.matches ? 'dark' : 'light';
	}

	function applyTheme() {
		if (root && root.setAttribute) {
			root.setAttribute('data-theme', preference === 'system' ? systemTheme() : preference);
		}
	}

	function syncControl() {
		if (connectedControl) {
			connectedControl.value = preference;
		}
	}

	function setPreference(value, persist, explicit) {
		if (!isValidTheme(value)) {
			return;
		}
		preference = value;
		if (explicit !== false) {
			hasExplicitPreference = true;
		}
		if (persist) {
			try {
				window.localStorage.setItem(preferenceKey, value);
			} catch (error) {
				// Keep the selected theme for this page when storage is unavailable.
			}
		}
		applyTheme();
		syncControl();
	}

	function configureDefault(value) {
		if (isValidTheme(value)) {
			configuredDefault = value;
		}
		if (!hasExplicitPreference) {
			preference = readDefault();
		}
		applyTheme();
		syncControl();
	}

	function connectControl(control) {
		var nextControl = control || document.getElementById('workbench-theme');
		if (!nextControl) {
			return false;
		}
		if (connectedControl !== nextControl) {
			if (connectedControl && controlHandler && connectedControl.removeEventListener) {
				connectedControl.removeEventListener('change', controlHandler);
			}
			connectedControl = nextControl;
			controlHandler = function () {
				setPreference(connectedControl.value, true, true);
			};
			connectedControl.addEventListener('change', controlHandler);
		}
		syncControl();
		return true;
	}

	applyTheme();
	if (media && typeof media.addEventListener === 'function') {
		media.addEventListener('change', function () {
			if (preference === 'system') {
				applyTheme();
			}
		});
	} else if (media && typeof media.addListener === 'function') {
		media.addListener(function () {
			if (preference === 'system') {
				applyTheme();
			}
		});
	}

	if (window.addEventListener) {
		window.addEventListener('storage', function (event) {
			if (event.key !== preferenceKey) {
				return;
			}
			if (isValidTheme(event.newValue)) {
				hasExplicitPreference = true;
				setPreference(event.newValue, false, true);
			} else {
				hasExplicitPreference = false;
				preference = readDefault();
				applyTheme();
				syncControl();
			}
		});
	}

	var api = {
		configure: configureDefault,
		connectControl: connectControl,
		getPreference: function () { return preference; },
		setPreference: function (value) { setPreference(value, true, true); }
	};
	window.RDF4JWorkbenchTheme = api;

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', function () { connectControl(); }, { once: true });
	} else {
		connectControl();
	}
}());
