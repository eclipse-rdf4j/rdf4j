(function () {
	'use strict';

	var preferenceKey = 'rdf4j-workbench-theme';
	var validThemes = ['system', 'light', 'dark'];
	var root = document.documentElement;
	var media = window.matchMedia('(prefers-color-scheme: dark)');
	var defaultMeta = document.querySelector('meta[name="rdf4j-workbench-theme-default"]');
	var configuredDefault = defaultMeta ? defaultMeta.getAttribute('content') : 'system';

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

	var preference = readPreference() || readDefault();

	function applyTheme() {
		root.setAttribute('data-theme', preference === 'system'
			? (media.matches ? 'dark' : 'light')
			: preference);
	}

	function setPreference(value, persist) {
		if (!isValidTheme(value)) {
			return;
		}
		preference = value;
		if (persist) {
			try {
				window.localStorage.setItem(preferenceKey, value);
			} catch (error) {
				// Keep the selected theme for this page even when storage is unavailable.
			}
		}
		applyTheme();
	}

	applyTheme();
	if (typeof media.addEventListener === 'function') {
		media.addEventListener('change', function () {
			if (preference === 'system') {
				applyTheme();
			}
		});
	} else if (typeof media.addListener === 'function') {
		media.addListener(function () {
			if (preference === 'system') {
				applyTheme();
			}
		});
	}

	window.addEventListener('storage', function (event) {
		if (event.key === preferenceKey) {
			setPreference(isValidTheme(event.newValue) ? event.newValue : readDefault(), false);
		}
	});

	function connectControl() {
		var control = document.getElementById('workbench-theme');
		if (!control) {
			return;
		}
		control.value = preference;
		control.addEventListener('change', function () {
			setPreference(control.value, true);
		});
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', connectControl, { once: true });
	} else {
		connectControl();
	}
}());
