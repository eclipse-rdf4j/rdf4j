/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
/// <reference path="yasqe.d.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts sub-folder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

namespace workbench {

    export interface EditorSizingOptions {
        /** Smallest height in pixels (six lines by default). */
        minimum?: number;
        /** Line height used by the arrow keys. */
        step?: number;
    }

    /**
     * Editor height handling shared by the Query and Update editors (M3.2). Without a stored height the
     * editor grows with its content (CSS limits it to six lines and half the viewport); dragging the
     * handle or pressing ArrowUp/ArrowDown on it fixes a height between six lines and 80% of the viewport,
     * which is remembered in localStorage; double-click or Escape returns to the automatic height.
     */
    export namespace editorSizing {
        var STORAGE_UNAVAILABLE = -1;

        function readStoredHeight(storageKey: string): number {
            try {
                var value = window.localStorage ? window.localStorage.getItem(storageKey) : null;
                var height = value === null ? NaN : Number(value);
                return isFinite(height) && height > 0 ? height : STORAGE_UNAVAILABLE;
            } catch (error) {
                return STORAGE_UNAVAILABLE;
            }
        }

        function storeHeight(storageKey: string, height: number): void {
            try {
                if (!window.localStorage) {
                    return;
                }
                if (height > 0) {
                    window.localStorage.setItem(storageKey, String(Math.round(height)));
                } else {
                    window.localStorage.removeItem(storageKey);
                }
            } catch (error) {
                // Private windows can refuse storage; the height then lasts for this page only.
            }
        }

        export function install(cm: any, handle: HTMLElement, storageKey: string, options?: EditorSizingOptions): () => void {
            if (!cm || !handle) {
                return function() {};
            }
            var settings = options || {};
            var minimum = settings.minimum || 142;
            var step = settings.step || 21;
            var wrapper: HTMLElement = cm.getWrapperElement();
            var maximum = function() {
                return Math.max(minimum, Math.round(window.innerHeight * 0.8));
            };
            var clamp = function(height: number) {
                return Math.max(minimum, Math.min(maximum(), Math.round(height)));
            };
            var currentHeight = function() {
                return wrapper.getBoundingClientRect().height;
            };
            var apply = function(height: number, remember: boolean) {
                if (height > 0) {
                    var fixed = clamp(height);
                    wrapper.setAttribute('data-workbench-editor-height', String(fixed));
                    cm.setSize(null, fixed);
                    handle.setAttribute('aria-valuenow', String(fixed));
                    if (remember) {
                        storeHeight(storageKey, fixed);
                    }
                } else {
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
            var startFixed: string = null;
            // Only a drag past this distance resizes: a click on the handle keeps the automatic height.
            var dragThreshold = 3;
            var moved = false;
            var onPointerMove = function(event: PointerEvent) {
                if (!moved && Math.abs(event.clientY - startY) < dragThreshold) {
                    return;
                }
                moved = true;
                apply(startHeight + (event.clientY - startY), false);
            };
            var endDrag = function(event: PointerEvent) {
                handle.removeAttribute('data-dragging');
                handle.removeEventListener('pointermove', onPointerMove);
                handle.removeEventListener('pointerup', onPointerUp);
                handle.removeEventListener('pointercancel', onPointerCancel);
                if (handle.releasePointerCapture && event.pointerId !== undefined) {
                    try {
                        handle.releasePointerCapture(event.pointerId);
                    } catch (error) {
                        // The capture was already released by the browser.
                    }
                }
            };
            var onPointerUp = function(event: PointerEvent) {
                endDrag(event);
                if (moved) {
                    apply(currentHeight(), true);
                }
            };
            var onPointerCancel = function(event: PointerEvent) {
                endDrag(event);
                if (moved) {
                    apply(startFixed ? Number(startFixed) : 0, false);
                }
            };
            var onPointerDown = function(event: PointerEvent) {
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
            var onKeyDown = function(event: KeyboardEvent) {
                if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
                    event.preventDefault();
                    apply(currentHeight() + (event.key === 'ArrowDown' ? step : -step), true);
                } else if (event.key === 'Escape') {
                    event.preventDefault();
                    apply(0, true);
                }
            };
            var onDoubleClick = function() {
                apply(0, true);
            };
            handle.addEventListener('pointerdown', onPointerDown);
            handle.addEventListener('keydown', onKeyDown);
            handle.addEventListener('dblclick', onDoubleClick);
            return function() {
                handle.removeEventListener('pointerdown', onPointerDown);
                handle.removeEventListener('keydown', onKeyDown);
                handle.removeEventListener('dblclick', onDoubleClick);
                handle.removeEventListener('pointermove', onPointerMove);
                handle.removeEventListener('pointerup', onPointerUp);
                handle.removeEventListener('pointercancel', onPointerCancel);
            };
        }
    }

    export namespace yasqeHelper {

        /** The page's one request for the fallback namespaces, shared by its editors. */
        var fallbackNamespaces: Promise<{ [prefix: string]: string }> = null;

        /**
         * The namespaces to fall back on for prefixes that the repository does not define: the deployment's list
         * (namespaces.fallback; the bundled list is empty); none when they cannot be loaded.
         */
        function loadFallbackNamespaces(): Promise<{ [prefix: string]: string }> {
            if (!fallbackNamespaces) {
                var app = typeof document !== 'undefined' && document.getElementById
                    ? document.getElementById('workbench-app') : null;
                var basePath = app ? app.getAttribute('data-workbench-base-path') || '' : '';
                fallbackNamespaces = typeof fetch !== 'function' ? Promise.resolve({})
                    : fetch(basePath + '/fallback-namespaces', { credentials: 'same-origin' }).then(function(response) {
                        return response.ok ? response.json() : {};
                    }).then(function(namespaces: any) {
                        return namespaces && typeof namespaces === 'object' ? namespaces : {};
                    }, function() {
                        return {};
                    });
            }
            return fallbackNamespaces;
        }

        /**
         * The prefix completions: the given namespaces (the repository's, with the configured ones the server adds),
         * then the fallback namespaces for the prefixes those do not define.
         */
        function prefixCompletions(namespaces: any, fallback: { [prefix: string]: string }): Array<string> {
            var completions: Array<string> = [];
            var defined: { [prefix: string]: boolean } = {};
            for (var key in namespaces) {
                completions.push(key + " <" + namespaces[key] + ">");
                defined[key.charAt(key.length - 1) === ':' ? key.slice(0, -1) : key] = true;
            }
            Object.keys(fallback).forEach(function(prefix) {
                if (!Object.prototype.hasOwnProperty.call(defined, prefix) && typeof fallback[prefix] === 'string') {
                    completions.push(prefix + ": <" + fallback[prefix] + ">");
                }
            });
            return completions;
        }

        export function setupCompleters(namespaces: any) {//namespace in form {"rdf:":"http://bla"}
            var newPrefixCompleterName = "customPrefixCompleter";
            //take the current prefix completer as base, to present our own namespaces for prefix autocompletion
            YASQE.registerAutocompleter(newPrefixCompleterName, function(yasqe:any, name:string){
                //also, auto-append prefixes if needed
                yasqe.on("change", function() {
                    YASQE.Autocompleters.prefixes.appendPrefixIfNeeded(yasqe, name);
                });
                return {
                    bulk: true,
                    // YASQE stores every list handed to the callback: the repository's namespaces at once, then
                    // with the fallback namespaces when they arrive. Typing a prefixed name adds its PREFIX line.
                    async: true,
                    autoShow: true,
                    get: function(token: any, callback: (completions: Array<string>) => void) {
                        callback(prefixCompletions(namespaces, {}));
                        loadFallbackNamespaces().then(function(fallback) {
                            if (Object.keys(fallback).length) {
                                callback(prefixCompletions(namespaces, fallback));
                            }
                        });
                    },
                    isValidCompletionPosition: function() {
                        return YASQE.Autocompleters.prefixes.isValidCompletionPosition(yasqe);},
                    preProcessToken: function(token: Token) {
                        return YASQE.Autocompleters.prefixes.preprocessPrefixTokenForCompletion(yasqe, token);}
                }
            });

            //i.e., disable the property/class autocompleters
            YASQE.defaults.autocompleters = [newPrefixCompleterName, "variables"];
        }

        /** YASQE's toolbar controls, which it draws as plain divs, and the names they get as buttons. */
        var toolbarControls: { [selector: string]: string } = {
            '.yasqe_share': 'Share query',
            '.yasqe_fullscreenBtn': 'Full screen',
            '.yasqe_smallscreenBtn': 'Exit full screen'
        };

        /**
         * Show a quiet line where the text of an empty editor starts, saying what goes in it. Screen readers skip it
         * (the editor's field has its own name), and it is hidden whenever the editor has text.
         */
        export function showEmptyHint(editor: any, text: string): void {
            var wrapper: HTMLElement = editor && editor.getWrapperElement ? editor.getWrapperElement() : null;
            if (!wrapper) {
                return;
            }
            var hint = document.createElement('div');
            hint.className = 'workbench-editor-hint';
            hint.setAttribute('aria-hidden', 'true');
            hint.textContent = text;
            wrapper.appendChild(hint);
            var update = function() {
                hint.hidden = editor.getValue() !== '';
                if (!hint.hidden) {
                    // A line's text starts 4px (CodeMirror's line padding) after the gutters.
                    var gutters: HTMLElement = editor.getGutterElement ? editor.getGutterElement() : null;
                    hint.style.left = ((gutters ? gutters.offsetWidth : 0) + 4) + 'px';
                }
            };
            editor.on('change', update);
            editor.on('refresh', update);
            update();
        }

        /** Make the editor's Share and Full screen controls buttons that a keyboard can reach and press. */
        export function enableToolbarKeyboard(editor: any): void {
            var wrapper: HTMLElement = editor && editor.getWrapperElement ? editor.getWrapperElement() : null;
            if (!wrapper) {
                return;
            }
            Object.keys(toolbarControls).forEach(function(selector) {
                var control = <HTMLElement>wrapper.querySelector(selector);
                if (!control || control.getAttribute('role') === 'button') {
                    return;
                }
                control.setAttribute('role', 'button');
                control.setAttribute('tabindex', '0');
                control.setAttribute('aria-label', toolbarControls[selector]);
                control.addEventListener('keydown', function(event: KeyboardEvent) {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        control.click();
                    }
                });
            });
        }
    }
}
