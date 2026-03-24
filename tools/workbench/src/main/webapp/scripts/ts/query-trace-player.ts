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
            optionalDepth: number;
        }

        export interface TraceLine {
            id: string;
            displayIndex: number;
            stepIndex: number;
            kind: 'pattern' | 'filter' | 'optionalStart' | 'optionalEnd' | 'bind' | 'values' | 'minus' | 'minusEnd';
            text: string;
            indentDepth: number;
        }

        export interface TraceFrame {
            index: number;
            event: string;
            lineId?: string;
            stepIndex: number;
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
            lines: TraceLine[];
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
            bindingState?: 'changed' | 'stable';
        }

        export interface TraceQueryLine {
            kind: 'pattern' | 'filter' | 'optionalStart' | 'optionalEnd' | 'bind' | 'values' | 'minus' | 'minusEnd';
            line?: TraceLine;
            pattern?: TracePattern;
            displayIndex: number;
            stepIndex: number;
            gutterLabel: string;
            active: boolean;
            pending: boolean;
            sparqlText: string;
            tooltipBindings: { [key: string]: string };
            tokens: TraceQueryToken[];
        }

        export interface TraceSnapshot {
            frame: TraceFrame;
            direction: 'initial' | 'forward' | 'rollback' | 'steady';
            activePatternIndex: number;
            activeDisplayIndex: number;
            patterns: TracePatternSnapshot[];
            queryHead: string;
            queryTail: string;
            queryLines: TraceQueryLine[];
            resultBindings: { [key: string]: string };
        }

        interface SnapshotOptions {
            activePatternIndex?: number;
            direction?: 'initial' | 'forward' | 'rollback' | 'steady';
            clearedPatternIndexes?: number[];
        }

        var configuredNamespaces: TraceNamespaceDefinition[] = [];

        // Flow: normalize raw JSON -> keep active frame index -> derive a UI snapshot per render.
        export function normalizeTrace(raw: any): QueryTrace {
            var lines: TraceLine[] = [];
            var patterns: TracePattern[] = [];
            var frames: TraceFrame[] = [];
            var rawPatterns = raw && raw.patterns ? raw.patterns : [];
            var rawLines = raw && raw.lines ? raw.lines : [];
            var rawFrames = raw && raw.frames ? raw.frames : [];
            var rawFilters = raw && raw.filters ? raw.filters.slice(0) : [];
            for (var i = 0; i < rawLines.length; i++) {
                lines.push({
                    id: rawLines[i].id || ('line-' + i),
                    displayIndex: typeof rawLines[i].displayIndex === 'number' ? rawLines[i].displayIndex : i,
                    stepIndex: typeof rawLines[i].stepIndex === 'number' ? rawLines[i].stepIndex : -1,
                    kind: rawLines[i].kind || 'pattern',
                    text: rawLines[i].text || '',
                    indentDepth: typeof rawLines[i].indentDepth === 'number' && rawLines[i].indentDepth > 0
                        ? rawLines[i].indentDepth
                        : 1
                });
            }
            if (!lines.length) {
                lines = synthesizeLegacyLines(rawPatterns, rawFilters);
            }
            patterns = buildStepPatterns(lines, rawLines.length ? null : rawPatterns);
            var lineIdsByStepIndex = getLineIdsByStepIndex(lines);
            var patternIndexesById = getPatternIndexesById(patterns);
            for (var j = 0; j < rawFrames.length; j++) {
                var normalizedStepIndex = typeof rawFrames[j].stepIndex === 'number'
                    ? rawFrames[j].stepIndex
                    : (typeof rawFrames[j].patternIndex === 'number' ? rawFrames[j].patternIndex : -1);
                var normalizedLineId = rawFrames[j].lineId || lineIdsByStepIndex[normalizedStepIndex] || '';
                var normalizedPatternId = rawLines.length
                    ? normalizedLineId
                    : (rawFrames[j].patternId || normalizedLineId || '');
                frames.push({
                    index: typeof rawFrames[j].index === 'number' ? rawFrames[j].index : j,
                    event: rawFrames[j].event || '',
                    lineId: normalizedLineId,
                    stepIndex: normalizedStepIndex,
                    patternId: normalizedPatternId,
                    patternIndex: typeof patternIndexesById[normalizedPatternId] === 'number'
                        ? patternIndexesById[normalizedPatternId]
                        : normalizedStepIndex,
                    inputBindings: rawFrames[j].inputBindings || {},
                    outputBindings: rawFrames[j].outputBindings || {},
                    resultBindings: rawFrames[j].resultBindings || {}
                });
            }
            return {
                lines: lines,
                patterns: patterns,
                frames: frames,
                distinct: !!(raw && raw.distinct),
                filters: rawFilters,
                error: raw && raw.error ? {
                    code: raw.error.code || '',
                    message: raw.error.message || ''
                } : null
            };
        }

        function buildStepPatterns(lines: TraceLine[], rawPatterns?: any[]): TracePattern[] {
            var patterns: TracePattern[] = [];
            if (!lines) {
                return patterns;
            }
            var rawPatternIndex = 0;
            for (var i = 0; i < lines.length; i++) {
                if (typeof lines[i].stepIndex !== 'number' || lines[i].stepIndex < 0) {
                    continue;
                }
                var rawPattern = rawPatterns && rawPatternIndex < rawPatterns.length
                    ? rawPatterns[rawPatternIndex]
                    : null;
                patterns.push({
                    id: rawPattern && rawPattern.id ? rawPattern.id : lines[i].id,
                    index: rawPattern && typeof rawPattern.index === 'number' ? rawPattern.index : lines[i].stepIndex,
                    text: rawPattern && rawPattern.text ? rawPattern.text : (lines[i].text || ''),
                    optionalDepth: rawPattern && typeof rawPattern.optionalDepth === 'number' && rawPattern.optionalDepth > 0
                        ? rawPattern.optionalDepth
                        : 0
                });
                rawPatternIndex += 1;
            }
            patterns.sort(function(left, right) {
                return left.index - right.index;
            });
            return patterns;
        }

        function synthesizeLegacyLines(rawPatterns: any[], rawFilters: string[]): TraceLine[] {
            var lines: TraceLine[] = [];
            var currentOptionalDepth = 0;
            var nextStepIndex = 0;
            for (var i = 0; i < rawPatterns.length; i++) {
                var optionalDepth = typeof rawPatterns[i].optionalDepth === 'number' && rawPatterns[i].optionalDepth > 0
                    ? rawPatterns[i].optionalDepth
                    : 0;
                while (currentOptionalDepth < optionalDepth) {
                    lines.push(createTraceLine(lines.length, -1, 'optionalStart', 'OPTIONAL {', currentOptionalDepth + 1));
                    currentOptionalDepth += 1;
                }
                while (currentOptionalDepth > optionalDepth) {
                    currentOptionalDepth -= 1;
                    lines.push(createTraceLine(lines.length, -1, 'optionalEnd', '}', currentOptionalDepth + 1));
                }
                lines.push(createTraceLine(lines.length, nextStepIndex++, 'pattern', rawPatterns[i].text || '', optionalDepth + 1));
            }
            while (currentOptionalDepth > 0) {
                currentOptionalDepth -= 1;
                lines.push(createTraceLine(lines.length, -1, 'optionalEnd', '}', currentOptionalDepth + 1));
            }
            for (var j = 0; j < rawFilters.length; j++) {
                lines.push(createTraceLine(lines.length, -1, 'filter', rawFilters[j] || '', 1));
            }
            return lines;
        }

        function createTraceLine(
            displayIndex: number,
            stepIndex: number,
            kind: 'pattern' | 'filter' | 'optionalStart' | 'optionalEnd' | 'bind' | 'values' | 'minus' | 'minusEnd',
            text: string,
            indentDepth: number
        ): TraceLine {
            return {
                id: 'line-' + displayIndex,
                displayIndex: displayIndex,
                stepIndex: stepIndex,
                kind: kind,
                text: text,
                indentDepth: indentDepth
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
            return buildSnapshot(state);
        }

        export function createRollbackBridgeSnapshots(
            fromState: TracePlayerState,
            toState: TracePlayerState
        ): TraceSnapshot[] {
            var bridgeSnapshots: TraceSnapshot[] = [];
            if (!fromState || !toState || !fromState.trace || !toState.trace) {
                return bridgeSnapshots;
            }
            var fromFrame = getActiveFrame(fromState);
            var toFrame = getActiveFrame(toState);
            var tracePatterns = fromState.trace.patterns || [];
            var fromPattern = resolveActivePattern(fromState, tracePatterns, fromFrame);
            var toPattern = resolveActivePattern(toState, tracePatterns, toFrame);
            if (!fromPattern || !toPattern || fromPattern.index <= toPattern.index + 1) {
                return bridgeSnapshots;
            }
            var rollbackPatterns = getRollbackBridgePatterns(tracePatterns, fromPattern.index, toPattern.index);
            for (var i = 0; i < rollbackPatterns.length; i++) {
                bridgeSnapshots.push(buildSnapshot(fromState, {
                    activePatternIndex: rollbackPatterns[i].index,
                    direction: 'rollback',
                    clearedPatternIndexes: [rollbackPatterns[i].index]
                }));
            }
            return bridgeSnapshots;
        }

        function buildSnapshot(state: TracePlayerState, options?: SnapshotOptions): TraceSnapshot {
            var actualActiveFrame = getActiveFrame(state);
            var previousFrame = getPreviousFrame(state);
            var patternSnapshots: TracePatternSnapshot[] = [];
            var queryLines: TraceQueryLine[] = [];
            var traceLines = state && state.trace ? state.trace.lines : [];
            var tracePatterns = state && state.trace ? state.trace.patterns : [];
            var patternBindings = buildPatternBindingsByPatternId(state);
            var activePattern = resolveActivePattern(state, tracePatterns, actualActiveFrame, options);
            var activeLine = resolveActiveLine(state, traceLines, actualActiveFrame, options);
            if (!activeLine && activePattern) {
                activeLine = findLineByStepIndex(traceLines, activePattern.index);
            }
            var activePatternIndex = activePattern ? activePattern.index : getSnapshotActivePatternIndex(actualActiveFrame, options);
            var activeDisplayIndex = activeLine && typeof activeLine.displayIndex === 'number'
                ? activeLine.displayIndex
                : -1;
            var currentBindings = buildCurrentBindings(patternBindings, tracePatterns, activePatternIndex);
            var direction = options && options.direction
                ? options.direction
                : getTraceDirection(previousFrame, actualActiveFrame, activePatternIndex);
            var changedBindingStates = shouldUseActualBindingDelta(actualActiveFrame, activePattern)
                ? buildChangedBindingStates(state, actualActiveFrame, activePattern)
                : {};
            for (var i = 0; i < traceLines.length; i++) {
                var stepPattern = findPatternById(tracePatterns, traceLines[i].id)
                    || findPatternByIndex(tracePatterns, traceLines[i].stepIndex);
                var isActive = !!activeLine && traceLines[i].id === activeLine.id;
                var isPending = !!stepPattern && isPatternPending(stepPattern, activePatternIndex);
                var sparqlText = renderTraceLineAsSparqlLine(traceLines[i]);
                var lineBindings = isPending || shouldClearPatternBindings(stepPattern, options)
                    ? {}
                    : filterBindingsForQueryLine(
                        sparqlText,
                        formatBindings(currentBindings)
                    );
                if (stepPattern) {
                    patternSnapshots.push({
                        pattern: stepPattern,
                        active: isActive,
                        inputBindings: isActive && shouldUseActualBindingDelta(actualActiveFrame, activePattern)
                            ? (actualActiveFrame.inputBindings || {})
                            : {},
                        outputBindings: isActive && shouldUseActualBindingDelta(actualActiveFrame, activePattern)
                            ? (actualActiveFrame.outputBindings || {})
                            : {}
                    });
                }
                queryLines.push({
                    kind: traceLines[i].kind,
                    line: traceLines[i],
                    pattern: stepPattern,
                    displayIndex: traceLines[i].displayIndex,
                    stepIndex: traceLines[i].stepIndex,
                    gutterLabel: stepPattern ? String(stepPattern.index + 1) : '',
                    active: isActive,
                    pending: isPending,
                    sparqlText: sparqlText,
                    tooltipBindings: lineBindings,
                    tokens: tokenizeQueryLine(
                        sparqlText,
                        lineBindings,
                        buildBindingStates(lineBindings, isActive ? changedBindingStates : null)
                    )
                });
            }
            return {
                frame: createSnapshotFrame(actualActiveFrame, activePattern, activeLine),
                direction: direction,
                activePatternIndex: activePatternIndex,
                activeDisplayIndex: activeDisplayIndex,
                patterns: patternSnapshots,
                queryHead: state && state.trace && state.trace.distinct ? 'SELECT DISTINCT * WHERE {' : 'SELECT * WHERE {',
                queryTail: '}',
                queryLines: queryLines,
                resultBindings: actualActiveFrame && actualActiveFrame.resultBindings
                    && (hasBindings(actualActiveFrame.resultBindings) || shouldUseActualBindingDelta(actualActiveFrame, activePattern))
                    ? actualActiveFrame.resultBindings
                    : {}
            };
        }

        function createOptionalBoundaryLine(
            kind: 'optionalStart' | 'optionalEnd',
            optionalDepth: number
        ): TraceQueryLine {
            return {
                kind: kind,
                displayIndex: -1,
                stepIndex: -1,
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

        function getPreviousFrame(state: TracePlayerState): TraceFrame {
            if (!state || !state.trace || !state.trace.frames.length || state.frameIndex <= 0) {
                return null;
            }
            return state.trace.frames[clampFrameIndex(state, state.frameIndex) - 1];
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
            activePatternIndex: number
        ): { [key: string]: string } {
            var currentBindings: { [key: string]: string } = {};
            if (!patterns) {
                return currentBindings;
            }
            for (var i = 0; i < patterns.length; i++) {
                if (isPatternPending(patterns[i], activePatternIndex)) {
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

        function getLineIdsByStepIndex(lines: TraceLine[]): { [stepIndex: number]: string } {
            var lineIdsByStepIndex: { [stepIndex: number]: string } = {};
            if (!lines) {
                return lineIdsByStepIndex;
            }
            for (var i = 0; i < lines.length; i++) {
                if (typeof lines[i].stepIndex === 'number' && lines[i].stepIndex >= 0) {
                    lineIdsByStepIndex[lines[i].stepIndex] = lines[i].id;
                }
            }
            return lineIdsByStepIndex;
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
            if (frame.event === 'drop' && hasBindings(frame.inputBindings)) {
                return frame.inputBindings || {};
            }
            if (frame.event === 'probe' && hasBindings(frame.inputBindings)) {
                return frame.inputBindings || {};
            }
            if (hasBindings(frame.outputBindings)) {
                return frame.outputBindings || {};
            }
            return frame.inputBindings || {};
        }

        function getTraceDirection(
            previousFrame: TraceFrame,
            activeFrame: TraceFrame,
            activePatternIndex?: number
        ): 'initial' | 'forward' | 'rollback' | 'steady' {
            var resolvedActivePatternIndex = getFramePatternIndex(activeFrame, activePatternIndex);
            if (resolvedActivePatternIndex < 0) {
                return 'initial';
            }
            if (!previousFrame || typeof previousFrame.patternIndex !== 'number' || previousFrame.patternIndex < 0) {
                return 'initial';
            }
            if (resolvedActivePatternIndex > previousFrame.patternIndex) {
                return 'forward';
            }
            if (resolvedActivePatternIndex < previousFrame.patternIndex) {
                return 'rollback';
            }
            return 'steady';
        }

        function buildChangedBindingStates(
            state: TracePlayerState,
            activeFrame: TraceFrame,
            activePattern: TracePattern
        ): { [key: string]: 'changed' | 'stable' } {
            var bindingStates: { [key: string]: 'changed' | 'stable' } = {};
            if (!activeFrame) {
                return bindingStates;
            }
            var previousBindings = formatBindings(findPreviousPatternBindings(state, activeFrame, activePattern));
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

        function findPreviousPatternBindings(
            state: TracePlayerState,
            activeFrame: TraceFrame,
            activePattern?: TracePattern
        ): { [key: string]: string } {
            if (!state || !state.trace || !state.trace.frames.length || !activeFrame) {
                return {};
            }
            var activeIndex = clampFrameIndex(state, state.frameIndex);
            for (var i = activeIndex - 1; i >= 0; i--) {
                var candidate = state.trace.frames[i];
                if (isSameResolvedPatternFrame(candidate, activeFrame, activePattern)) {
                    return getFrameBindings(candidate);
                }
            }
            return {};
        }

        function isSameResolvedPatternFrame(
            candidate: TraceFrame,
            activeFrame: TraceFrame,
            activePattern?: TracePattern
        ): boolean {
            if (!candidate || !activeFrame) {
                return false;
            }
            if (activePattern) {
                if (candidate.patternId && activePattern.id) {
                    return candidate.patternId === activePattern.id;
                }
                return candidate.patternIndex === activePattern.index;
            }
            return isSamePatternFrame(candidate, activeFrame);
        }

        function isSamePatternFrame(left: TraceFrame, right: TraceFrame): boolean {
            if (!left || !right) {
                return false;
            }
            if (left.patternId && right.patternId) {
                return left.patternId === right.patternId;
            }
            return left.patternIndex === right.patternIndex;
        }

        function buildBindingStates(
            tooltipBindings: { [key: string]: string },
            activeBindingStates: { [key: string]: 'changed' | 'stable' }
        ): { [key: string]: 'changed' | 'stable' } {
            var bindingStates: { [key: string]: 'changed' | 'stable' } = {};
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

        function getSnapshotActivePatternIndex(activeFrame: TraceFrame, options?: SnapshotOptions): number {
            if (options && typeof options.activePatternIndex === 'number' && options.activePatternIndex >= 0) {
                return options.activePatternIndex;
            }
            return activeFrame && typeof activeFrame.patternIndex === 'number' && activeFrame.patternIndex >= 0
                ? activeFrame.patternIndex
                : -1;
        }

        function findPatternByIndex(patterns: TracePattern[], patternIndex: number): TracePattern {
            if (!patterns || patternIndex < 0) {
                return null;
            }
            for (var i = 0; i < patterns.length; i++) {
                if (patterns[i].index === patternIndex) {
                    return patterns[i];
                }
            }
            return null;
        }

        function findPatternById(patterns: TracePattern[], patternId: string): TracePattern {
            if (!patterns || !patternId) {
                return null;
            }
            for (var i = 0; i < patterns.length; i++) {
                if (patterns[i].id === patternId) {
                    return patterns[i];
                }
            }
            return null;
        }

        function findLineById(lines: TraceLine[], lineId: string): TraceLine {
            if (!lines || !lineId) {
                return null;
            }
            for (var i = 0; i < lines.length; i++) {
                if (lines[i].id === lineId) {
                    return lines[i];
                }
            }
            return null;
        }

        function findLineByStepIndex(lines: TraceLine[], stepIndex: number): TraceLine {
            if (!lines || stepIndex < 0) {
                return null;
            }
            for (var i = 0; i < lines.length; i++) {
                if (lines[i].stepIndex === stepIndex) {
                    return lines[i];
                }
            }
            return null;
        }

        function resolveActivePattern(
            state: TracePlayerState,
            patterns: TracePattern[],
            activeFrame: TraceFrame,
            options?: SnapshotOptions
        ): TracePattern {
            if (options && typeof options.activePatternIndex === 'number' && options.activePatternIndex >= 0) {
                return findPatternByIndex(patterns, options.activePatternIndex);
            }
            var resolvedPattern = findPatternById(patterns, activeFrame && (activeFrame.lineId || activeFrame.patternId))
                || findPatternByIndex(patterns, getSnapshotActivePatternIndex(activeFrame));
            if (resolvedPattern) {
                return resolvedPattern;
            }
            if (isResultFrame(activeFrame)) {
                return findPreviousResolvablePattern(state, patterns);
            }
            return null;
        }

        function resolveActiveLine(
            state: TracePlayerState,
            lines: TraceLine[],
            activeFrame: TraceFrame,
            options?: SnapshotOptions
        ): TraceLine {
            if (options && typeof options.activePatternIndex === 'number' && options.activePatternIndex >= 0) {
                return findLineByStepIndex(lines, options.activePatternIndex);
            }
            var resolvedLine = findLineById(lines, activeFrame && activeFrame.lineId)
                || findLineByStepIndex(lines, getSnapshotActivePatternIndex(activeFrame));
            if (resolvedLine) {
                return resolvedLine;
            }
            if (isResultFrame(activeFrame)) {
                return findPreviousResolvableLine(state, lines);
            }
            return null;
        }

        function getRollbackBridgePatterns(
            patterns: TracePattern[],
            fromPatternIndex: number,
            toPatternIndex: number
        ): TracePattern[] {
            var rollbackPatterns: TracePattern[] = [];
            if (!patterns) {
                return rollbackPatterns;
            }
            for (var i = 0; i < patterns.length; i++) {
                if (patterns[i].index < fromPatternIndex && patterns[i].index > toPatternIndex) {
                    rollbackPatterns.push(patterns[i]);
                }
            }
            rollbackPatterns.sort(function(left, right) {
                return right.index - left.index;
            });
            return rollbackPatterns;
        }

        function shouldUseActualBindingDelta(activeFrame: TraceFrame, activePattern: TracePattern): boolean {
            return !!activeFrame
                && !!activePattern
                && (
                    isResultFrame(activeFrame)
                    || (
                    (activeFrame.patternId && activeFrame.patternId === activePattern.id)
                    || activeFrame.patternIndex === activePattern.index
                    )
                );
        }

        function isResultFrame(frame: TraceFrame): boolean {
            return !!frame
                && frame.event === 'result'
                && hasBindings(frame.resultBindings);
        }

        function findPreviousResolvablePattern(state: TracePlayerState, patterns: TracePattern[]): TracePattern {
            if (!state || !state.trace || !state.trace.frames.length) {
                return null;
            }
            var activeIndex = clampFrameIndex(state, state.frameIndex);
            for (var i = activeIndex - 1; i >= 0; i--) {
                var candidatePattern = findPatternById(patterns,
                    state.trace.frames[i] && (state.trace.frames[i].lineId || state.trace.frames[i].patternId))
                    || findPatternByIndex(patterns, getSnapshotActivePatternIndex(state.trace.frames[i]));
                if (candidatePattern) {
                    return candidatePattern;
                }
            }
            return null;
        }

        function findPreviousResolvableLine(state: TracePlayerState, lines: TraceLine[]): TraceLine {
            if (!state || !state.trace || !state.trace.frames.length) {
                return null;
            }
            var activeIndex = clampFrameIndex(state, state.frameIndex);
            for (var i = activeIndex - 1; i >= 0; i--) {
                var candidateLine = findLineById(lines, state.trace.frames[i] && state.trace.frames[i].lineId)
                    || findLineByStepIndex(lines, getSnapshotActivePatternIndex(state.trace.frames[i]));
                if (candidateLine) {
                    return candidateLine;
                }
            }
            return null;
        }

        function getFramePatternIndex(frame: TraceFrame, fallbackPatternIndex?: number): number {
            if (frame && typeof frame.patternIndex === 'number' && frame.patternIndex >= 0) {
                return frame.patternIndex;
            }
            if (typeof fallbackPatternIndex === 'number' && fallbackPatternIndex >= 0) {
                return fallbackPatternIndex;
            }
            return -1;
        }

        function shouldClearPatternBindings(pattern: TracePattern, options?: SnapshotOptions): boolean {
            if (!pattern || !options || !options.clearedPatternIndexes) {
                return false;
            }
            for (var i = 0; i < options.clearedPatternIndexes.length; i++) {
                if (options.clearedPatternIndexes[i] === pattern.index) {
                    return true;
                }
            }
            return false;
        }

        function createSnapshotFrame(activeFrame: TraceFrame, activePattern: TracePattern, activeLine: TraceLine): TraceFrame {
            if (!activePattern || !activeLine) {
                return null;
            }
            if (activeFrame && activeFrame.lineId === activeLine.id) {
                return activeFrame;
            }
            return {
                index: activeFrame && typeof activeFrame.index === 'number' ? activeFrame.index : -1,
                event: activeFrame && activeFrame.event ? activeFrame.event : '',
                lineId: activeLine.id,
                stepIndex: activeLine.stepIndex,
                patternId: activePattern.id,
                patternIndex: activePattern.index,
                inputBindings: {},
                outputBindings: {},
                resultBindings: {}
            };
        }

        function isPatternPending(pattern: TracePattern, activePatternIndex: number): boolean {
            return typeof activePatternIndex === 'number'
                && activePatternIndex >= 0
                && pattern.index > activePatternIndex;
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

        function getOptionalDepth(pattern: TracePattern): number {
            return pattern && typeof pattern.optionalDepth === 'number' && pattern.optionalDepth > 0
                ? pattern.optionalDepth
                : 0;
        }

        function renderTraceLineAsSparqlLine(line: TraceLine): string {
            if (!line) {
                return '';
            }
            if (line.kind === 'pattern') {
                return renderPatternAsSparqlLine(line.text, line.indentDepth);
            }
            if (line.kind === 'filter' || line.kind === 'bind' || line.kind === 'values' || line.kind === 'minus') {
                return buildQueryIndentForDepth(line.indentDepth) + compactTraceText(line.text);
            }
            return buildQueryIndentForDepth(line.indentDepth) + line.text;
        }

        function renderPatternAsSparqlLine(patternText: string, indentDepth: number): string {
            var indent = buildQueryIndentForDepth(indentDepth);
            var contextMatch = patternText.match(/^(.*)\s+\[([^\]]+)\]$/);
            if (contextMatch) {
                return indent + 'GRAPH ' + formatTraceValue(contextMatch[2]) + ' { '
                    + compactTraceText(contextMatch[1]) + ' . }';
            }
            return indent + compactTraceText(patternText) + ' .';
        }

        function renderFilterAsSparqlLine(filterText: string): string {
            return buildQueryIndentForDepth(1) + compactTraceText(filterText);
        }

        function renderOptionalStartLine(optionalDepth: number): string {
            return buildQueryIndentForDepth(optionalDepth + 1) + 'OPTIONAL {';
        }

        function renderOptionalEndLine(optionalDepth: number): string {
            return buildQueryIndentForDepth(optionalDepth + 1) + '}';
        }

        function buildQueryIndentForDepth(indentDepth: number): string {
            var indent = '';
            for (var i = 0; i < indentDepth; i++) {
                indent += '  ';
            }
            return indent;
        }

        function tokenizeQueryLine(
            sparqlText: string,
            tooltipBindings: { [key: string]: string },
            bindingStates: { [key: string]: 'changed' | 'stable' }
        ): TraceQueryToken[] {
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
