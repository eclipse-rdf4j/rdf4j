/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="yasqe.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts sub-folder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    /**
     * Editor height handling shared by the Query and Update editors (M3.2). Without a stored height the
     * editor grows with its content (CSS limits it to six lines and half the viewport); dragging the
     * handle or pressing ArrowUp/ArrowDown on it fixes a height between six lines and 80% of the viewport,
     * which is remembered in localStorage; double-click or Escape returns to the automatic height.
     */
    var editorSizing;
    (function (editorSizing) {
        var STORAGE_UNAVAILABLE = -1;
        function readStoredHeight(storageKey) {
            try {
                var value = window.localStorage ? window.localStorage.getItem(storageKey) : null;
                var height = value === null ? NaN : Number(value);
                return isFinite(height) && height > 0 ? height : STORAGE_UNAVAILABLE;
            }
            catch (error) {
                return STORAGE_UNAVAILABLE;
            }
        }
        function storeHeight(storageKey, height) {
            try {
                if (!window.localStorage) {
                    return;
                }
                if (height > 0) {
                    window.localStorage.setItem(storageKey, String(Math.round(height)));
                }
                else {
                    window.localStorage.removeItem(storageKey);
                }
            }
            catch (error) {
                // Private windows can refuse storage; the height then lasts for this page only.
            }
        }
        function install(cm, handle, storageKey, options) {
            if (!cm || !handle) {
                return function () { };
            }
            var settings = options || {};
            var minimum = settings.minimum || 142;
            var step = settings.step || 21;
            var wrapper = cm.getWrapperElement();
            var maximum = function () {
                return Math.max(minimum, Math.round(window.innerHeight * 0.8));
            };
            var clamp = function (height) {
                return Math.max(minimum, Math.min(maximum(), Math.round(height)));
            };
            var currentHeight = function () {
                return wrapper.getBoundingClientRect().height;
            };
            var apply = function (height, remember) {
                if (height > 0) {
                    var fixed = clamp(height);
                    wrapper.setAttribute('data-workbench-editor-height', String(fixed));
                    cm.setSize(null, fixed);
                    handle.setAttribute('aria-valuenow', String(fixed));
                    if (remember) {
                        storeHeight(storageKey, fixed);
                    }
                }
                else {
                    wrapper.removeAttribute('data-workbench-editor-height');
                    wrapper.style.height = '';
                    handle.removeAttribute('aria-valuenow');
                    if (remember) {
                        storeHeight(storageKey, 0);
                    }
                }
                cm.refresh();
            };
            handle.setAttribute('aria-valuemin', String(minimum));
            var stored = readStoredHeight(storageKey);
            if (stored !== STORAGE_UNAVAILABLE) {
                apply(stored, false);
            }
            var startY = 0;
            var startHeight = 0;
            // The fixed height before the drag ('' for the automatic height), restored when the drag is cancelled.
            var startFixed = null;
            // Only a drag past this distance resizes: a click on the handle keeps the automatic height.
            var dragThreshold = 3;
            var moved = false;
            var onPointerMove = function (event) {
                if (!moved && Math.abs(event.clientY - startY) < dragThreshold) {
                    return;
                }
                moved = true;
                apply(startHeight + (event.clientY - startY), false);
            };
            var endDrag = function (event) {
                handle.removeAttribute('data-dragging');
                handle.removeEventListener('pointermove', onPointerMove);
                handle.removeEventListener('pointerup', onPointerUp);
                handle.removeEventListener('pointercancel', onPointerCancel);
                if (handle.releasePointerCapture && event.pointerId !== undefined) {
                    try {
                        handle.releasePointerCapture(event.pointerId);
                    }
                    catch (error) {
                        // The capture was already released by the browser.
                    }
                }
            };
            var onPointerUp = function (event) {
                endDrag(event);
                if (moved) {
                    apply(currentHeight(), true);
                }
            };
            var onPointerCancel = function (event) {
                endDrag(event);
                if (moved) {
                    apply(startFixed ? Number(startFixed) : 0, false);
                }
            };
            var onPointerDown = function (event) {
                if (event.button !== 0) {
                    return;
                }
                event.preventDefault();
                startY = event.clientY;
                startHeight = currentHeight();
                startFixed = wrapper.getAttribute('data-workbench-editor-height') || '';
                moved = false;
                handle.setAttribute('data-dragging', 'true');
                if (handle.setPointerCapture && event.pointerId !== undefined) {
                    handle.setPointerCapture(event.pointerId);
                }
                handle.addEventListener('pointermove', onPointerMove);
                handle.addEventListener('pointerup', onPointerUp);
                handle.addEventListener('pointercancel', onPointerCancel);
            };
            var onKeyDown = function (event) {
                if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
                    event.preventDefault();
                    apply(currentHeight() + (event.key === 'ArrowDown' ? step : -step), true);
                }
                else if (event.key === 'Escape') {
                    event.preventDefault();
                    apply(0, true);
                }
            };
            var onDoubleClick = function () {
                apply(0, true);
            };
            handle.addEventListener('pointerdown', onPointerDown);
            handle.addEventListener('keydown', onKeyDown);
            handle.addEventListener('dblclick', onDoubleClick);
            return function () {
                handle.removeEventListener('pointerdown', onPointerDown);
                handle.removeEventListener('keydown', onKeyDown);
                handle.removeEventListener('dblclick', onDoubleClick);
                handle.removeEventListener('pointermove', onPointerMove);
                handle.removeEventListener('pointerup', onPointerUp);
                handle.removeEventListener('pointercancel', onPointerCancel);
            };
        }
        editorSizing.install = install;
    })(editorSizing = workbench.editorSizing || (workbench.editorSizing = {}));
    var yasqeHelper;
    (function (yasqeHelper) {
        function setupCompleters(namespaces) {
            var newPrefixCompleterName = "customPrefixCompleter";
            //take the current prefix completer as base, to present our own namespaces for prefix autocompletion
            YASQE.registerAutocompleter(newPrefixCompleterName, function (yasqe, name) {
                //also, auto-append prefixes if needed
                yasqe.on("change", function () {
                    YASQE.Autocompleters.prefixes.appendPrefixIfNeeded(yasqe, name);
                });
                return {
                    bulk: true,
                    async: false,
                    autoShow: true,
                    get: function () {
                        var completerArray = [];
                        for (var key in namespaces) {
                            completerArray.push(key + " <" + namespaces[key] + ">");
                        }
                        return completerArray;
                    },
                    isValidCompletionPosition: function () {
                        return YASQE.Autocompleters.prefixes.isValidCompletionPosition(yasqe);
                    },
                    preProcessToken: function (token) {
                        return YASQE.Autocompleters.prefixes.preprocessPrefixTokenForCompletion(yasqe, token);
                    }
                };
            });
            //i.e., disable the property/class autocompleters
            YASQE.defaults.autocompleters = [newPrefixCompleterName, "variables"];
        }
        yasqeHelper.setupCompleters = setupCompleters;
    })(yasqeHelper = workbench.yasqeHelper || (workbench.yasqeHelper = {}));
})(workbench || (workbench = {}));
//# sourceMappingURL=yasqeHelper.js.map