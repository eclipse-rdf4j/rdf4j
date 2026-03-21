/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var queryTracePlayer;
    (function (queryTracePlayer) {
        var configuredNamespaces = [];
        // Flow: normalize raw JSON -> keep active frame index -> derive a UI snapshot per render.
        function normalizeTrace(raw) {
            var patterns = [];
            var frames = [];
            var rawPatterns = raw && raw.patterns ? raw.patterns : [];
            var rawFrames = raw && raw.frames ? raw.frames : [];
            for (var i = 0; i < rawPatterns.length; i++) {
                patterns.push({
                    id: rawPatterns[i].id || ('sp-' + i),
                    index: typeof rawPatterns[i].index === 'number' ? rawPatterns[i].index : i,
                    text: rawPatterns[i].text || '',
                    optionalDepth: typeof rawPatterns[i].optionalDepth === 'number' && rawPatterns[i].optionalDepth > 0
                        ? rawPatterns[i].optionalDepth
                        : 0
                });
            }
            for (var j = 0; j < rawFrames.length; j++) {
                frames.push({
                    index: typeof rawFrames[j].index === 'number' ? rawFrames[j].index : j,
                    event: rawFrames[j].event || '',
                    patternId: rawFrames[j].patternId || '',
                    patternIndex: typeof rawFrames[j].patternIndex === 'number' ? rawFrames[j].patternIndex : -1,
                    inputBindings: rawFrames[j].inputBindings || {},
                    outputBindings: rawFrames[j].outputBindings || {},
                    resultBindings: rawFrames[j].resultBindings || {}
                });
            }
            return {
                patterns: patterns,
                frames: frames,
                distinct: !!(raw && raw.distinct),
                filters: raw && raw.filters ? raw.filters.slice(0) : [],
                error: raw && raw.error ? {
                    code: raw.error.code || '',
                    message: raw.error.message || ''
                } : null
            };
        }
        queryTracePlayer.normalizeTrace = normalizeTrace;
        function createState(trace) {
            return {
                trace: trace,
                frameIndex: 0,
                playing: false
            };
        }
        queryTracePlayer.createState = createState;
        function setNamespaces(namespaces) {
            configuredNamespaces = normalizeNamespaces(namespaces);
        }
        queryTracePlayer.setNamespaces = setNamespaces;
        function formatTraceValue(value) {
            if (value === null || value === undefined) {
                return value;
            }
            return compactDisplayText(value);
        }
        queryTracePlayer.formatTraceValue = formatTraceValue;
        function getFrameCount(state) {
            return state && state.trace ? state.trace.frames.length : 0;
        }
        queryTracePlayer.getFrameCount = getFrameCount;
        function getActiveFrame(state) {
            if (!state || !state.trace || !state.trace.frames.length) {
                return null;
            }
            var safeIndex = clampFrameIndex(state, state.frameIndex);
            return state.trace.frames[safeIndex];
        }
        queryTracePlayer.getActiveFrame = getActiveFrame;
        function canStepForward(state) {
            return getFrameCount(state) > 0 && state.frameIndex < getFrameCount(state) - 1;
        }
        queryTracePlayer.canStepForward = canStepForward;
        function canStepBackward(state) {
            return getFrameCount(state) > 0 && state.frameIndex > 0;
        }
        queryTracePlayer.canStepBackward = canStepBackward;
        function next(state) {
            return updateFrameIndex(state, state.frameIndex + 1);
        }
        queryTracePlayer.next = next;
        function previous(state) {
            return updateFrameIndex(state, state.frameIndex - 1);
        }
        queryTracePlayer.previous = previous;
        function reset(state) {
            return updateFrameIndex(state, 0);
        }
        queryTracePlayer.reset = reset;
        function seek(state, frameIndex) {
            return updateFrameIndex(state, frameIndex);
        }
        queryTracePlayer.seek = seek;
        function setPlaying(state, playing) {
            return {
                trace: state.trace,
                frameIndex: clampFrameIndex(state, state.frameIndex),
                playing: playing
            };
        }
        queryTracePlayer.setPlaying = setPlaying;
        function snapshot(state) {
            var activeFrame = getActiveFrame(state);
            var previousFrame = getPreviousFrame(state);
            var patternSnapshots = [];
            var queryLines = [];
            var tracePatterns = state && state.trace ? state.trace.patterns : [];
            var traceFilters = state && state.trace && state.trace.filters ? state.trace.filters : [];
            var patternBindings = buildPatternBindingsByPatternId(state);
            var currentBindings = buildCurrentBindings(patternBindings, tracePatterns, activeFrame);
            var direction = getTraceDirection(previousFrame, activeFrame);
            var changedBindingStates = buildChangedBindingStates(state, activeFrame);
            var currentOptionalDepth = 0;
            for (var i = 0; i < tracePatterns.length; i++) {
                var isActive = !!activeFrame && tracePatterns[i].id === activeFrame.patternId;
                var isPending = isPatternPending(tracePatterns[i], activeFrame);
                var optionalDepth = getOptionalDepth(tracePatterns[i]);
                while (currentOptionalDepth < optionalDepth) {
                    queryLines.push(createOptionalBoundaryLine('optionalStart', currentOptionalDepth));
                    currentOptionalDepth += 1;
                }
                while (currentOptionalDepth > optionalDepth) {
                    currentOptionalDepth -= 1;
                    queryLines.push(createOptionalBoundaryLine('optionalEnd', currentOptionalDepth));
                }
                var sparqlText = renderPatternAsSparqlLine(tracePatterns[i].text, optionalDepth);
                var lineBindings = isPending
                    ? {}
                    : filterBindingsForQueryLine(sparqlText, formatBindings(patternBindings[tracePatterns[i].id] || {}));
                patternSnapshots.push({
                    pattern: tracePatterns[i],
                    active: isActive,
                    inputBindings: isActive
                        ? (activeFrame.inputBindings || {})
                        : {},
                    outputBindings: isActive
                        ? (activeFrame.outputBindings || {})
                        : {}
                });
                queryLines.push({
                    kind: 'pattern',
                    pattern: tracePatterns[i],
                    gutterLabel: String(tracePatterns[i].index + 1),
                    active: isActive,
                    pending: isPending,
                    sparqlText: sparqlText,
                    tooltipBindings: lineBindings,
                    tokens: tokenizeQueryLine(sparqlText, lineBindings, buildBindingStates(lineBindings, isActive ? changedBindingStates : null))
                });
            }
            while (currentOptionalDepth > 0) {
                currentOptionalDepth -= 1;
                queryLines.push(createOptionalBoundaryLine('optionalEnd', currentOptionalDepth));
            }
            for (var j = 0; j < traceFilters.length; j++) {
                var filterLineText = renderFilterAsSparqlLine(traceFilters[j]);
                var filterBindings = filterBindingsForQueryLine(filterLineText, currentBindings);
                queryLines.push({
                    kind: 'filter',
                    gutterLabel: '',
                    active: false,
                    pending: false,
                    sparqlText: filterLineText,
                    tooltipBindings: filterBindings,
                    tokens: tokenizeQueryLine(filterLineText, filterBindings, buildBindingStates(filterBindings, null))
                });
            }
            return {
                frame: activeFrame,
                direction: direction,
                activePatternIndex: activeFrame && typeof activeFrame.patternIndex === 'number'
                    ? activeFrame.patternIndex
                    : -1,
                patterns: patternSnapshots,
                queryHead: state && state.trace && state.trace.distinct ? 'SELECT DISTINCT * WHERE {' : 'SELECT * WHERE {',
                queryTail: '}',
                queryLines: queryLines,
                resultBindings: activeFrame && activeFrame.resultBindings ? activeFrame.resultBindings : {}
            };
        }
        queryTracePlayer.snapshot = snapshot;
        function createOptionalBoundaryLine(kind, optionalDepth) {
            return {
                kind: kind,
                gutterLabel: '',
                active: false,
                pending: false,
                sparqlText: kind === 'optionalStart'
                    ? renderOptionalStartLine(optionalDepth)
                    : renderOptionalEndLine(optionalDepth),
                tooltipBindings: {},
                tokens: [{ text: kind === 'optionalStart' ? renderOptionalStartLine(optionalDepth)
                            : renderOptionalEndLine(optionalDepth) }]
            };
        }
        function updateFrameIndex(state, nextFrameIndex) {
            return {
                trace: state.trace,
                frameIndex: clampFrameIndex(state, nextFrameIndex),
                playing: false
            };
        }
        function clampFrameIndex(state, frameIndex) {
            var frameCount = getFrameCount(state);
            if (frameCount <= 0) {
                return 0;
            }
            if (frameIndex < 0) {
                return 0;
            }
            if (frameIndex >= frameCount) {
                return frameCount - 1;
            }
            return frameIndex;
        }
        function getPreviousFrame(state) {
            if (!state || !state.trace || !state.trace.frames.length || state.frameIndex <= 0) {
                return null;
            }
            return state.trace.frames[clampFrameIndex(state, state.frameIndex) - 1];
        }
        function buildPatternBindingsByPatternId(state) {
            var bindingsByPattern = {};
            if (!state || !state.trace || !state.trace.frames.length) {
                return bindingsByPattern;
            }
            var patternIndexesById = getPatternIndexesById(state.trace.patterns);
            var lastIndex = clampFrameIndex(state, state.frameIndex);
            for (var i = 0; i <= lastIndex; i++) {
                var frame = state.trace.frames[i];
                if (!frame || !frame.patternId) {
                    continue;
                }
                resetDeeperPatternBindings(bindingsByPattern, patternIndexesById, frame.patternIndex);
                if (!bindingsByPattern[frame.patternId]) {
                    bindingsByPattern[frame.patternId] = {};
                }
                mergeBindings(bindingsByPattern[frame.patternId], getFrameBindings(frame));
            }
            return bindingsByPattern;
        }
        function buildCurrentBindings(bindingsByPattern, patterns, activeFrame) {
            var currentBindings = {};
            if (!patterns) {
                return currentBindings;
            }
            for (var i = 0; i < patterns.length; i++) {
                if (isPatternPending(patterns[i], activeFrame)) {
                    continue;
                }
                mergeBindings(currentBindings, bindingsByPattern[patterns[i].id] || {});
            }
            return currentBindings;
        }
        function getPatternIndexesById(patterns) {
            var patternIndexesById = {};
            if (!patterns) {
                return patternIndexesById;
            }
            for (var i = 0; i < patterns.length; i++) {
                patternIndexesById[patterns[i].id] = patterns[i].index;
            }
            return patternIndexesById;
        }
        function resetDeeperPatternBindings(bindingsByPattern, patternIndexesById, currentPatternIndex) {
            if (typeof currentPatternIndex !== 'number' || currentPatternIndex < 0) {
                return;
            }
            for (var patternId in bindingsByPattern) {
                if (!bindingsByPattern.hasOwnProperty(patternId)) {
                    continue;
                }
                if (patternIndexesById[patternId] > currentPatternIndex) {
                    delete bindingsByPattern[patternId];
                }
            }
        }
        function getFrameBindings(frame) {
            if (!frame) {
                return {};
            }
            if (hasBindings(frame.resultBindings)) {
                return frame.resultBindings || {};
            }
            if (frame.event === 'match' && hasBindings(frame.outputBindings)) {
                return frame.outputBindings || {};
            }
            if (frame.event === 'probe' && hasBindings(frame.inputBindings)) {
                return frame.inputBindings || {};
            }
            if (hasBindings(frame.outputBindings)) {
                return frame.outputBindings || {};
            }
            return frame.inputBindings || {};
        }
        function getTraceDirection(previousFrame, activeFrame) {
            if (!activeFrame || typeof activeFrame.patternIndex !== 'number' || activeFrame.patternIndex < 0) {
                return 'initial';
            }
            if (!previousFrame || typeof previousFrame.patternIndex !== 'number' || previousFrame.patternIndex < 0) {
                return 'initial';
            }
            if (activeFrame.patternIndex > previousFrame.patternIndex) {
                return 'forward';
            }
            if (activeFrame.patternIndex < previousFrame.patternIndex) {
                return 'rollback';
            }
            return 'steady';
        }
        function buildChangedBindingStates(state, activeFrame) {
            var bindingStates = {};
            if (!activeFrame) {
                return bindingStates;
            }
            var previousBindings = formatBindings(findPreviousPatternBindings(state, activeFrame));
            var currentBindings = formatBindings(getFrameBindings(activeFrame));
            for (var bindingName in currentBindings) {
                if (!currentBindings.hasOwnProperty(bindingName)) {
                    continue;
                }
                bindingStates[bindingName] = !previousBindings.hasOwnProperty(bindingName)
                    || previousBindings[bindingName] !== currentBindings[bindingName]
                    ? 'changed'
                    : 'stable';
            }
            return bindingStates;
        }
        function findPreviousPatternBindings(state, activeFrame) {
            if (!state || !state.trace || !state.trace.frames.length || !activeFrame) {
                return {};
            }
            var activeIndex = clampFrameIndex(state, state.frameIndex);
            for (var i = activeIndex - 1; i >= 0; i--) {
                var candidate = state.trace.frames[i];
                if (isSamePatternFrame(candidate, activeFrame)) {
                    return getFrameBindings(candidate);
                }
            }
            return {};
        }
        function isSamePatternFrame(left, right) {
            if (!left || !right) {
                return false;
            }
            if (left.patternId && right.patternId) {
                return left.patternId === right.patternId;
            }
            return left.patternIndex === right.patternIndex;
        }
        function buildBindingStates(tooltipBindings, activeBindingStates) {
            var bindingStates = {};
            if (!tooltipBindings) {
                return bindingStates;
            }
            for (var bindingName in tooltipBindings) {
                if (!tooltipBindings.hasOwnProperty(bindingName)) {
                    continue;
                }
                bindingStates[bindingName] = activeBindingStates && activeBindingStates[bindingName]
                    ? activeBindingStates[bindingName]
                    : 'stable';
            }
            return bindingStates;
        }
        function mergeBindings(target, source) {
            if (!source) {
                return;
            }
            for (var bindingName in source) {
                if (source.hasOwnProperty(bindingName)) {
                    target[bindingName] = source[bindingName];
                }
            }
        }
        function hasBindings(bindings) {
            if (!bindings) {
                return false;
            }
            for (var bindingName in bindings) {
                if (bindings.hasOwnProperty(bindingName)) {
                    return true;
                }
            }
            return false;
        }
        function isPatternPending(pattern, activeFrame) {
            return !!activeFrame
                && typeof activeFrame.patternIndex === 'number'
                && activeFrame.patternIndex >= 0
                && pattern.index > activeFrame.patternIndex;
        }
        function filterBindingsForQueryLine(sparqlText, currentBindings) {
            var lineBindings = {};
            var variablePattern = /\?([A-Za-z_][A-Za-z0-9_-]*)/g;
            var match;
            while ((match = variablePattern.exec(sparqlText)) !== null) {
                if (currentBindings && Object.prototype.hasOwnProperty.call(currentBindings, match[1])) {
                    lineBindings[match[1]] = currentBindings[match[1]];
                }
            }
            return lineBindings;
        }
        function getOptionalDepth(pattern) {
            return pattern && typeof pattern.optionalDepth === 'number' && pattern.optionalDepth > 0
                ? pattern.optionalDepth
                : 0;
        }
        function renderPatternAsSparqlLine(patternText, optionalDepth) {
            var indent = buildQueryIndent(optionalDepth);
            var contextMatch = patternText.match(/^(.*)\s+\[([^\]]+)\]$/);
            if (contextMatch) {
                return indent + 'GRAPH ' + formatTraceValue(contextMatch[2]) + ' { '
                    + compactTraceText(contextMatch[1]) + ' . }';
            }
            return indent + compactTraceText(patternText) + ' .';
        }
        function renderFilterAsSparqlLine(filterText) {
            return buildQueryIndent(0) + compactTraceText(filterText);
        }
        function renderOptionalStartLine(optionalDepth) {
            return buildQueryIndent(optionalDepth) + 'OPTIONAL {';
        }
        function renderOptionalEndLine(optionalDepth) {
            return buildQueryIndent(optionalDepth) + '}';
        }
        function buildQueryIndent(optionalDepth) {
            var indent = '';
            for (var i = 0; i <= optionalDepth; i++) {
                indent += '  ';
            }
            return indent;
        }
        function tokenizeQueryLine(sparqlText, tooltipBindings, bindingStates) {
            var tokens = [];
            var variablePattern = /\?([A-Za-z_][A-Za-z0-9_-]*)/g;
            var match;
            var nextIndex = 0;
            while ((match = variablePattern.exec(sparqlText)) !== null) {
                if (match.index > nextIndex) {
                    tokens.push({ text: sparqlText.substring(nextIndex, match.index) });
                }
                var token = {
                    text: match[0],
                    variableName: match[1]
                };
                if (tooltipBindings && Object.prototype.hasOwnProperty.call(tooltipBindings, match[1])) {
                    token.bindingValue = tooltipBindings[match[1]];
                    token.bindingState = bindingStates && Object.prototype.hasOwnProperty.call(bindingStates, match[1])
                        ? bindingStates[match[1]]
                        : 'stable';
                }
                tokens.push(token);
                nextIndex = match.index + match[0].length;
            }
            if (nextIndex < sparqlText.length) {
                tokens.push({ text: sparqlText.substring(nextIndex) });
            }
            if (!tokens.length) {
                tokens.push({ text: sparqlText });
            }
            return tokens;
        }
        function formatBindings(bindings) {
            var formattedBindings = {};
            if (!bindings) {
                return formattedBindings;
            }
            for (var bindingName in bindings) {
                if (bindings.hasOwnProperty(bindingName)) {
                    formattedBindings[bindingName] = formatTraceValue(bindings[bindingName]);
                }
            }
            return formattedBindings;
        }
        function normalizeNamespaces(namespaces) {
            var normalized = [];
            if (!namespaces) {
                return normalized;
            }
            for (var prefix in namespaces) {
                if (!Object.prototype.hasOwnProperty.call(namespaces, prefix) || !namespaces[prefix]) {
                    continue;
                }
                normalized.push({
                    prefix: normalizeNamespacePrefix(prefix),
                    namespace: namespaces[prefix]
                });
            }
            normalized.sort(function (left, right) {
                if (right.namespace.length !== left.namespace.length) {
                    return right.namespace.length - left.namespace.length;
                }
                if (left.prefix < right.prefix) {
                    return -1;
                }
                if (left.prefix > right.prefix) {
                    return 1;
                }
                return 0;
            });
            return normalized;
        }
        function normalizeNamespacePrefix(prefix) {
            if (!prefix) {
                return '';
            }
            return prefix.charAt(prefix.length - 1) === ':' ? prefix.substring(0, prefix.length - 1) : prefix;
        }
        function compactTraceText(value) {
            return compactText(value, true);
        }
        function compactDisplayText(value) {
            return compactText(value, false);
        }
        function compactText(value, requireSafeLocalName) {
            return value
                .replace(/\^\^<([^>]+)>/g, function (_, iri) {
                return '^^' + compactIriReference(iri, requireSafeLocalName);
            })
                .replace(/<([^>]+)>/g, function (_, iri) {
                return compactIriReference(iri, requireSafeLocalName);
            });
        }
        function compactIriReference(iri, requireSafeLocalName) {
            var compacted = compactIri(iri, requireSafeLocalName);
            if (compacted) {
                return compacted;
            }
            return '<' + iri + '>';
        }
        function compactIri(iri, requireSafeLocalName) {
            for (var i = 0; i < configuredNamespaces.length; i++) {
                var namespace = configuredNamespaces[i];
                if (iri.indexOf(namespace.namespace) !== 0) {
                    continue;
                }
                var localName = iri.substring(namespace.namespace.length);
                if (!localName) {
                    continue;
                }
                if (requireSafeLocalName && !isSafePrefixedLocalName(localName)) {
                    continue;
                }
                return namespace.prefix + ':' + localName;
            }
            return null;
        }
        function isSafePrefixedLocalName(localName) {
            return !!localName && /^[A-Za-z0-9_][A-Za-z0-9._~-]*$/.test(localName);
        }
    })(queryTracePlayer = workbench.queryTracePlayer || (workbench.queryTracePlayer = {}));
})(workbench || (workbench = {}));
//# sourceMappingURL=query-trace-player.js.map