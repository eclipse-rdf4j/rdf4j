/// <reference path="jquery.d.ts" />

// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

module workbench {

    export module queryTracePlayer {

        interface TraceNamespaceDefinition {
            prefix: string;
            namespace: string;
        }

        export interface TracePattern {
            id: string;
            index: number;
            text: string;
        }

        export interface TraceFrame {
            index: number;
            event: string;
            patternId?: string;
            patternIndex: number;
            inputBindings?: { [key: string]: string };
            outputBindings?: { [key: string]: string };
            resultBindings?: { [key: string]: string };
        }

        export interface TraceError {
            code: string;
            message: string;
        }

        export interface QueryTrace {
            patterns: TracePattern[];
            frames: TraceFrame[];
            distinct?: boolean;
            filters?: string[];
            error?: TraceError;
        }

        export interface TracePlayerState {
            trace: QueryTrace;
            frameIndex: number;
            playing: boolean;
        }

        export interface TracePatternSnapshot {
            pattern: TracePattern;
            active: boolean;
            inputBindings: { [key: string]: string };
            outputBindings: { [key: string]: string };
        }

        export interface TraceQueryToken {
            text: string;
            variableName?: string;
            bindingValue?: string;
        }

        export interface TraceQueryLine {
            pattern?: TracePattern;
            gutterLabel: string;
            active: boolean;
            pending: boolean;
            sparqlText: string;
            tooltipBindings: { [key: string]: string };
            tokens: TraceQueryToken[];
        }

        export interface TraceSnapshot {
            frame: TraceFrame;
            patterns: TracePatternSnapshot[];
            queryHead: string;
            queryTail: string;
            queryLines: TraceQueryLine[];
            resultBindings: { [key: string]: string };
        }

        var configuredNamespaces: TraceNamespaceDefinition[] = [];

        // Flow: normalize raw JSON -> keep active frame index -> derive a UI snapshot per render.
        export function normalizeTrace(raw: any): QueryTrace {
            var patterns: TracePattern[] = [];
            var frames: TraceFrame[] = [];
            var rawPatterns = raw && raw.patterns ? raw.patterns : [];
            var rawFrames = raw && raw.frames ? raw.frames : [];
            for (var i = 0; i < rawPatterns.length; i++) {
                patterns.push({
                    id: rawPatterns[i].id || ('sp-' + i),
                    index: typeof rawPatterns[i].index === 'number' ? rawPatterns[i].index : i,
                    text: rawPatterns[i].text || ''
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

        export function createState(trace: QueryTrace): TracePlayerState {
            return {
                trace: trace,
                frameIndex: 0,
                playing: false
            };
        }

        export function setNamespaces(namespaces: { [key: string]: string }) {
            configuredNamespaces = normalizeNamespaces(namespaces);
        }

        export function formatTraceValue(value: string): string {
            if (value === null || value === undefined) {
                return value;
            }
            return compactDisplayText(value);
        }

        export function getFrameCount(state: TracePlayerState): number {
            return state && state.trace ? state.trace.frames.length : 0;
        }

        export function getActiveFrame(state: TracePlayerState): TraceFrame {
            if (!state || !state.trace || !state.trace.frames.length) {
                return null;
            }
            var safeIndex = clampFrameIndex(state, state.frameIndex);
            return state.trace.frames[safeIndex];
        }

        export function canStepForward(state: TracePlayerState): boolean {
            return getFrameCount(state) > 0 && state.frameIndex < getFrameCount(state) - 1;
        }

        export function canStepBackward(state: TracePlayerState): boolean {
            return getFrameCount(state) > 0 && state.frameIndex > 0;
        }

        export function next(state: TracePlayerState): TracePlayerState {
            return updateFrameIndex(state, state.frameIndex + 1);
        }

        export function previous(state: TracePlayerState): TracePlayerState {
            return updateFrameIndex(state, state.frameIndex - 1);
        }

        export function reset(state: TracePlayerState): TracePlayerState {
            return updateFrameIndex(state, 0);
        }

        export function seek(state: TracePlayerState, frameIndex: number): TracePlayerState {
            return updateFrameIndex(state, frameIndex);
        }

        export function setPlaying(state: TracePlayerState, playing: boolean): TracePlayerState {
            return {
                trace: state.trace,
                frameIndex: clampFrameIndex(state, state.frameIndex),
                playing: playing
            };
        }

        export function snapshot(state: TracePlayerState): TraceSnapshot {
            var activeFrame = getActiveFrame(state);
            var patternSnapshots: TracePatternSnapshot[] = [];
            var queryLines: TraceQueryLine[] = [];
            var tracePatterns = state && state.trace ? state.trace.patterns : [];
            var traceFilters = state && state.trace && state.trace.filters ? state.trace.filters : [];
            var patternBindings = buildPatternBindingsByPatternId(state);
            var currentBindings = buildCurrentBindings(patternBindings, tracePatterns, activeFrame);
            for (var i = 0; i < tracePatterns.length; i++) {
                var isActive = !!activeFrame && tracePatterns[i].id === activeFrame.patternId;
                var isPending = isPatternPending(tracePatterns[i], activeFrame);
                var sparqlText = renderPatternAsSparqlLine(tracePatterns[i].text);
                var lineBindings = isPending
                    ? {}
                    : filterBindingsForQueryLine(
                        sparqlText,
                        formatBindings(patternBindings[tracePatterns[i].id] || {})
                    );
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
                    pattern: tracePatterns[i],
                    gutterLabel: String(tracePatterns[i].index + 1),
                    active: isActive,
                    pending: isPending,
                    sparqlText: sparqlText,
                    tooltipBindings: lineBindings,
                    tokens: tokenizeQueryLine(sparqlText, lineBindings)
                });
            }
            for (var j = 0; j < traceFilters.length; j++) {
                var filterLineText = renderFilterAsSparqlLine(traceFilters[j]);
                var filterBindings = filterBindingsForQueryLine(filterLineText, currentBindings);
                queryLines.push({
                    gutterLabel: '',
                    active: false,
                    pending: false,
                    sparqlText: filterLineText,
                    tooltipBindings: filterBindings,
                    tokens: tokenizeQueryLine(filterLineText, filterBindings)
                });
            }
            return {
                frame: activeFrame,
                patterns: patternSnapshots,
                queryHead: state && state.trace && state.trace.distinct ? 'SELECT DISTINCT * WHERE {' : 'SELECT * WHERE {',
                queryTail: '}',
                queryLines: queryLines,
                resultBindings: activeFrame && activeFrame.resultBindings ? activeFrame.resultBindings : {}
            };
        }

        function updateFrameIndex(state: TracePlayerState, nextFrameIndex: number): TracePlayerState {
            return {
                trace: state.trace,
                frameIndex: clampFrameIndex(state, nextFrameIndex),
                playing: false
            };
        }

        function clampFrameIndex(state: TracePlayerState, frameIndex: number): number {
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

        function buildPatternBindingsByPatternId(state: TracePlayerState): { [patternId: string]: { [key: string]: string } } {
            var bindingsByPattern: { [patternId: string]: { [key: string]: string } } = {};
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

        function buildCurrentBindings(
            bindingsByPattern: { [patternId: string]: { [key: string]: string } },
            patterns: TracePattern[],
            activeFrame: TraceFrame
        ): { [key: string]: string } {
            var currentBindings: { [key: string]: string } = {};
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

        function getPatternIndexesById(patterns: TracePattern[]): { [patternId: string]: number } {
            var patternIndexesById: { [patternId: string]: number } = {};
            if (!patterns) {
                return patternIndexesById;
            }
            for (var i = 0; i < patterns.length; i++) {
                patternIndexesById[patterns[i].id] = patterns[i].index;
            }
            return patternIndexesById;
        }

        function resetDeeperPatternBindings(
            bindingsByPattern: { [patternId: string]: { [key: string]: string } },
            patternIndexesById: { [patternId: string]: number },
            currentPatternIndex: number
        ) {
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

        function getFrameBindings(frame: TraceFrame): { [key: string]: string } {
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

        function mergeBindings(target: { [key: string]: string }, source: { [key: string]: string }) {
            if (!source) {
                return;
            }
            for (var bindingName in source) {
                if (source.hasOwnProperty(bindingName)) {
                    target[bindingName] = source[bindingName];
                }
            }
        }

        function hasBindings(bindings: { [key: string]: string }): boolean {
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

        function isPatternPending(pattern: TracePattern, activeFrame: TraceFrame): boolean {
            return !!activeFrame
                && typeof activeFrame.patternIndex === 'number'
                && activeFrame.patternIndex >= 0
                && pattern.index > activeFrame.patternIndex;
        }

        function filterBindingsForQueryLine(
            sparqlText: string,
            currentBindings: { [key: string]: string }
        ): { [key: string]: string } {
            var lineBindings: { [key: string]: string } = {};
            var variablePattern = /\?([A-Za-z_][A-Za-z0-9_-]*)/g;
            var match: RegExpExecArray;
            while ((match = variablePattern.exec(sparqlText)) !== null) {
                if (currentBindings && Object.prototype.hasOwnProperty.call(currentBindings, match[1])) {
                    lineBindings[match[1]] = currentBindings[match[1]];
                }
            }
            return lineBindings;
        }

        function renderPatternAsSparqlLine(patternText: string): string {
            var contextMatch = patternText.match(/^(.*)\s+\[([^\]]+)\]$/);
            if (contextMatch) {
                return '  GRAPH ' + formatTraceValue(contextMatch[2]) + ' { '
                    + compactTraceText(contextMatch[1]) + ' . }';
            }
            return '  ' + compactTraceText(patternText) + ' .';
        }

        function renderFilterAsSparqlLine(filterText: string): string {
            return '  ' + compactTraceText(filterText);
        }

        function tokenizeQueryLine(sparqlText: string, tooltipBindings: { [key: string]: string }): TraceQueryToken[] {
            var tokens: TraceQueryToken[] = [];
            var variablePattern = /\?([A-Za-z_][A-Za-z0-9_-]*)/g;
            var match: RegExpExecArray;
            var nextIndex = 0;
            while ((match = variablePattern.exec(sparqlText)) !== null) {
                if (match.index > nextIndex) {
                    tokens.push({ text: sparqlText.substring(nextIndex, match.index) });
                }
                var token: TraceQueryToken = {
                    text: match[0],
                    variableName: match[1]
                };
                if (tooltipBindings && Object.prototype.hasOwnProperty.call(tooltipBindings, match[1])) {
                    token.bindingValue = tooltipBindings[match[1]];
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

        function formatBindings(bindings: { [key: string]: string }): { [key: string]: string } {
            var formattedBindings: { [key: string]: string } = {};
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

        function normalizeNamespaces(namespaces: { [key: string]: string }): TraceNamespaceDefinition[] {
            var normalized: TraceNamespaceDefinition[] = [];
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
            normalized.sort(function(left, right) {
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

        function normalizeNamespacePrefix(prefix: string): string {
            if (!prefix) {
                return '';
            }
            return prefix.charAt(prefix.length - 1) === ':' ? prefix.substring(0, prefix.length - 1) : prefix;
        }

        function compactTraceText(value: string): string {
            return compactText(value, true);
        }

        function compactDisplayText(value: string): string {
            return compactText(value, false);
        }

        function compactText(value: string, requireSafeLocalName: boolean): string {
            return value
                .replace(/\^\^<([^>]+)>/g, function(_: string, iri: string) {
                    return '^^' + compactIriReference(iri, requireSafeLocalName);
                })
                .replace(/<([^>]+)>/g, function(_: string, iri: string) {
                    return compactIriReference(iri, requireSafeLocalName);
                });
        }

        function compactIriReference(iri: string, requireSafeLocalName: boolean): string {
            var compacted = compactIri(iri, requireSafeLocalName);
            if (compacted) {
                return compacted;
            }
            return '<' + iri + '>';
        }

        function compactIri(iri: string, requireSafeLocalName: boolean): string {
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

        function isSafePrefixedLocalName(localName: string): boolean {
            return !!localName && /^[A-Za-z0-9_][A-Za-z0-9._~-]*$/.test(localName);
        }
    }
}
