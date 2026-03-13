<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

    <xsl:include href="../locale/messages.xsl"/>

    <xsl:variable name="title">
        <xsl:value-of select="$query.title"/>
    </xsl:variable>

    <xsl:include href="template.xsl"/>

    <xsl:template match="sparql:sparql">
        <xsl:variable name="queryLn"
                      select="sparql:results/sparql:result/sparql:binding[@name='queryLn']"/>
        <xsl:variable name="query"
                      select="sparql:results/sparql:result/sparql:binding[@name='query']"/>
        <xsl:variable name="queryTimeout"
                      select="sparql:results/sparql:result/sparql:binding[@name='query-timeout']/sparql:literal/text()"/>
        <xsl:variable name="hideQueryLanguageRow"
                      select="count($info//sparql:binding[@name='query-format']) = 1 and substring-before(normalize-space($info//sparql:binding[@name='query-format'][1]/sparql:literal), ' ') = 'SPARQL'"/>
        <xsl:variable name="defaultQueryTimeout"
                      select="$info//sparql:binding[@name='default-query-timeout']/sparql:literal/text()"/>
        <xsl:variable name="explanation"
                      select="sparql:results/sparql:result/sparql:binding[@name='explanation']/sparql:literal"/>
        <xsl:variable name="explanationFormat"
                      select="sparql:results/sparql:result/sparql:binding[@name='explanation-format']/sparql:literal"/>
        <xsl:variable name="explanationLevel"
                      select="sparql:results/sparql:result/sparql:binding[@name='explanation-level']/sparql:literal"/>
        <form action="query" method="post" onsubmit="return workbench.query.doSubmit()">
            <input type="hidden" name="action" id="action"/>
            <input type="hidden" name="explain" id="explain"/>
            <input type="hidden" name="ref" value="text"/>
            <input type="hidden" name="include-query-text" id="include-query-text" value="false"/>
            <style type="text/css">
                .query-form {
                    --query-form-label-width: 12rem;
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    gap: 0.75em;
                }

                .query-form__row {
                    display: grid;
                    grid-template-columns:max-content minmax(0, 1fr);
                    grid-template-columns:var(--query-form-label-width) minmax(0, 1fr);
                    align-items: start;
                    column-gap: 1em;
                    row-gap: 0.5em;
                }

                .query-form__row--stacked {
                    grid-template-columns:minmax(0, 1fr);
                }

                #query-explanation-row,
                #query-explanation-row-compare {
                    margin-top: 1.2em;
                }

                .query-form__label {
                    font-family: Roboto, Libre Franklin, Helvetica Neue, Helvetica, Arial, sans-serif;
                    font-weight: bold;
                    white-space: nowrap;
                    margin-top: auto;
                    margin-bottom: auto;
                    padding-left: 1px;
                }

                .query-form__label:after {
                    content: ':';
                }

                .query-form__label--blank:after {
                    content: '';
                }

                .query-form__row--stacked .query-form__label {
                    padding-top: 0;
                }

                .query-form__row--stacked .query-form__label:after {
                    content: '';
                }

                .query-form__field {
                    min-width: 0;
                }

                .query-form__field input[type='text'],
                .query-form__field textarea,
                .query-form__field select {
                    box-sizing: border-box;
                    max-width: 100%;
                }

                .query-form__field--controls,
                .query-form__field--options,
                .query-form__field--actions {
                    display: flex;
                    flex-wrap: wrap;
                    align-items: center;
                    gap: 0.1em;
                }

                .query-form__field--controls-group {
                    display: flex;
                    flex-wrap: wrap;
                    align-items: center;
                    gap: 0.1em;
                }

                #query-sidebar-toggle {
                    display: none;
                    position: fixed;
                    top: 6.75rem;
                    left: 0.2rem;
                    z-index: 1001;
                    width: 1.5rem;
                    height: 1.5rem;
                    padding: 0;
                    border: none;
                    border-radius: 0.9rem;
                    background-color: rgba(255, 255, 255, 0);
                    color: #234e73;
                    cursor: pointer;
                    transform: translateX(0);
                    transition: transform 0.22s ease, background 0.16s ease, box-shadow 0.16s ease;
                }

                #query-sidebar-toggle:hover {
                    background: #eef5fb;
                }

                body.query-compare-mode #query-sidebar-toggle {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                }

                .query-sidebar-toggle__icon {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    width: 1.1rem;
                    height: 1.1rem;
                }

                .query-sidebar-toggle__svg {
                    display: block;
                    width: 100%;
                    height: 100%;
                }

                .query-sidebar-toggle__stroke {
                    fill: none;
                    stroke: currentColor;
                    stroke-width: 2.25;
                    stroke-linecap: round;
                    stroke-linejoin: round;
                    vector-effect: non-scaling-stroke;
                }

                body.query-compare-mode #navigation {
                    transform: translateX(-220px);
                    z-index: 1002;
                    opacity: 0;
                    visibility: hidden;
                    pointer-events: none;
                    transition: transform 0.22s ease, opacity 0.22s ease, visibility 0s linear 0.22s;
                }

                body.query-compare-mode #content {
                    margin-left: 1rem;
                    transition: margin-left 0.22s ease;
                }

                body.query-compare-mode #title_heading,
                body.query-compare-mode #noscript-message,
                body.query-compare-mode .query-form {
                    transform: translateX(0);
                    transition: transform 0.22s ease;
                }

                body.query-compare-mode.query-compare-nav-open #navigation {
                    transform: translateX(0);
                    opacity: 1;
                    visibility: visible;
                    pointer-events: auto;
                    transition-delay: 0s;
                    box-shadow: 0 14px 30px rgba(12, 25, 36, 0.2);
                }

                body.query-compare-mode.query-compare-nav-open #title_heading,
                body.query-compare-mode.query-compare-nav-open #noscript-message,
                body.query-compare-mode.query-compare-nav-open .query-form {
                    transform: translateX(184px);
                }

                body.query-compare-mode.query-compare-nav-open #query-sidebar-toggle {
                    transform: translateX(192px);
                }

                .query-form__field--actions #query-name {
                    min-width: 14em;
                }

                .query-explanation-controls-row-class {
                    margin-top: 0.5em;
                    margin-left: -0.1em;
                }

                .query-explanation-status {
                    display: none;
                    margin: 0 0 0.55em 0;
                    padding: 0.45em 0.7em;
                    border-radius: 0.35em;
                    border: 1px solid transparent;
                    font-size: 0.92em;
                    line-height: 1.35;
                }

                .query-explanation-status--visible {
                    display: block;
                }

                .query-explanation-status--loading {
                    background: #eef5fb;
                    border-color: #c7d8ea;
                    color: #234e73;
                }

                .query-explanation-status--stale {
                    background: #fff7dc;
                    border-color: #e6d28d;
                    color: #715400;
                }

                .query-explanation-status--error {
                    background: #fdeaea;
                    border-color: #efc2c2;
                    color: #8d1f1f;
                }

                .query-explanation-surface {
                    position: relative;
                }

                .query-explanation-overlay {
                    display: none;
                    position: absolute;
                    inset: 0;
                    z-index: 2;
                    padding: 0.75em;
                    box-sizing: border-box;
                    background: rgba(255, 255, 255, 0.72);
                    color: #234e73;
                    font-weight: bold;
                    align-items: flex-start;
                    justify-content: flex-end;
                    pointer-events: none;
                }

                .query-explanation-overlay--visible {
                    display: flex;
                }

                .query-explain-spinner {
                    display: none;
                    width: 0.95em;
                    height: 0.95em;
                    box-sizing: border-box;
                    border: 2px solid #c8d1db;
                    border-top-color: #356893;
                    border-radius: 50%;
                    vertical-align: middle;
                }

                .query-explain-spinner--visible {
                    display: inline-block;
                    animation: query-explain-spinner-spin 0.8s linear infinite;
                }

                .query-explain-cancel {
                    display: none;
                }

                .query-explain-cancel--visible {
                    display: inline-block;
                }

                .query-compare-action.query-explain-cancel.query-explain-cancel--visible {
                    display: inline-flex;
                }

                @keyframes query-explain-spinner-spin {
                    from {
                        transform: rotate(0deg);
                    }

                    to {
                        transform: rotate(360deg);
                    }
                }

                #query {
                    width: 100%;
                    max-width: 100%;
                }

                #query-compare {
                    width: 100%;
                    max-width: 100%;
                }

                #query-explanation,
                #query-explanation-compare {
                    max-height: none;
                    width: 100%;
                    max-width: 100%;
                    overflow: auto;
                    white-space: pre;
                    word-break: normal;
                    overflow-wrap: normal;
                    background: #ffffff;
                    color: #000000;
                    border: 1px solid #d0d0d0;
                    padding: 0.75em;
                    box-sizing: border-box;
                }

                #query-explanation-dot-view,
                #query-explanation-dot-view-compare {
                    display: none;
                    background: #ffffff;
                    border: 1px solid #d0d0d0;
                    padding: 0;
                    font-size: 1em;
                    width: 100%;
                    box-sizing: border-box;
                    overflow: auto;
                    height: 75vh;
                }

                #query-explanation-json-view,
                #query-explanation-json-view-compare {
                    display: none;
                    background: #ffffff;
                    border: 1px solid #d0d0d0;
                    padding: 0.75em;
                    width: 100%;
                    box-sizing: border-box;
                    overflow: auto;
                    max-height: 75vh;
                    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
                    line-height: 1.45;
                }

                .query-json-view__raw {
                    margin: 0.6em 0 0 0;
                    padding: 0.6em;
                    background: #f8f8f8;
                    border: 1px solid #e0e0e0;
                    white-space: pre;
                    overflow: auto;
                }

                .query-json-tree {
                    font-size: 0.95em;
                    color: #1f2328;
                }

                .query-json-node {
                    margin: 0.15em 0;
                }

                .query-json-node__line {
                    display: flex;
                    align-items: flex-start;
                    gap: 0.35em;
                    white-space: nowrap;
                }

                .query-json-node__toggle {
                    border: 1px solid #c5d1de;
                    background: #f3f7fb;
                    color: #285d8f;
                    border-radius: 3px;
                    font-size: 0.72em;
                    line-height: 1;
                    width: 1.5em;
                    height: 1.5em;
                    padding: 0;
                    cursor: pointer;
                    flex: 0 0 auto;
                }

                .query-json-node__toggle:hover {
                    background: #e6f1fa;
                }

                .query-json-node__toggle:focus {
                    outline: 2px solid #5a9ad6;
                    outline-offset: 1px;
                }

                .query-json-node__toggle--spacer {
                    border: none;
                    background: transparent;
                    cursor: default;
                }

                .query-json-node__children {
                    margin-left: 1.15em;
                    padding-left: 0.75em;
                    border-left: 1px dotted #c7d0d9;
                }

                .query-json-node--collapsed > .query-json-node__children {
                    display: none;
                }

                .query-json-node__key {
                    color: #0b5da8;
                }

                .query-json-node__label {
                    color: #5a6775;
                }

                .query-json-node__summary {
                    color: #6e7781;
                }

                .query-json-node__percentage {
                    color: #7a3e0a;
                    font-weight: 600;
                }

                .query-json-node__value--string {
                    color: #0a7a36;
                }

                .query-json-node__value--number {
                    color: #7a3e0a;
                }

                .query-json-node__value--boolean {
                    color: #8a2a8c;
                }

                .query-json-node__value--null {
                    color: #6e7781;
                    font-style: italic;
                }

                .query-compare-layout {
                    display: grid;
                    grid-template-columns:minmax(0, 1fr);
                    gap: 1rem;
                    align-items: start;
                }

                .query-compare-pane {
                    min-width: 0;
                }

                .query-compare-pane--secondary,
                #query-compare-controls {
                    display: none;
                }

                .query-compare-layout--active {
                    grid-template-columns:minmax(0, 1fr) auto minmax(0, 1fr);
                }

                .query-compare-layout--active .query-compare-pane--secondary {
                    display: block;
                }

                .query-compare-layout--active #query-compare-controls {
                    display: flex;
                    position: sticky;
                    top: 10px;
                }

                #query-compare-controls {
                    flex-direction: column;
                    gap: 0.45rem;
                    align-items: center;
                    justify-content: center;
                    width: 2.6rem;
                    margin-top: 1.5rem;
                }

                .query-compare-action {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    width: 2.3rem;
                    height: 2.3rem;
                    padding: 0;
                    border: 1px solid #c7d3df;
                    border-radius: 0.75rem;
                    background: #ffffff;
                    color: #234e73;
                    box-shadow: 0 8px 18px rgba(12, 25, 36, 0.12);
                    cursor: pointer;
                    transition: transform 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
                }

                .query-compare-action.query-explain-cancel {
                    display: none;
                }

                .query-compare-action:hover {
                    background: #eef5fb;
                }

                .query-compare-action:focus {
                    outline: 2px solid #5a9ad6;
                    outline-offset: 2px;
                }

                .query-compare-action[disabled] {
                    opacity: 0.45;
                    cursor: not-allowed;
                    box-shadow: none;
                    transform: none;
                }

                .query-compare-action__icon {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    line-height: 1;
                    color: currentColor;
                }

                .query-compare-action__svg {
                    display: block;
                    width: 1.2rem;
                    height: 1.2rem;
                }

                .query-compare-action__stroke {
                    fill: none;
                    stroke: currentColor;
                    stroke-width: 2.1;
                    stroke-linecap: round;
                    stroke-linejoin: round;
                    vector-effect: non-scaling-stroke;
                }

                .query-compare-action__fill {
                    fill: currentColor;
                }

                .query-compare-action__icon--spinning {
                    animation: query-explain-spinner-spin 0.8s linear infinite;
                    transform-origin: center;
                    transform-box: fill-box;
                }

                .query-compare-action--spinning .query-compare-action__svg--refresh {
                    animation: query-explain-spinner-spin 0.8s linear infinite;
                    transform-origin: center;
                    transform-box: fill-box;
                }

                .query-compare-action__svg--refresh {
                    width: 1.35rem;
                    height: 1.35rem;
                }

                .query-compare-action__icon--cancel {
                    font-size: 1.45rem;
                    line-height: 1;
                }

                .query-compare-action__svg--diff {
                    width: 1.5rem;
                    height: 1.7rem;
                    margin-top: -0.25rem;
                    padding-left: 0.05rem;
                }


                .query-diff-modal {
                    --query-diff-modal-padding: clamp(1rem, 3vw, 2rem);
                    display: none;
                    position: fixed;
                    inset: 0;
                    z-index: 999;
                    background: rgba(12, 25, 36, 0.7);
                    align-items: stretch;
                    justify-content: center;
                    padding: var(--query-diff-modal-padding);
                    box-sizing: border-box;
                }

                .query-diff-modal--open {
                    display: flex;
                }

                .query-diff-modal__dialog {
                    background: #ffffff;
                    color: #1f2328;
                    width: calc(100vw - (var(--query-diff-modal-padding) * 2));
                    height: calc(100vh - (var(--query-diff-modal-padding) * 2));
                    max-width: 1400px;
                    max-height: calc(100vh - (var(--query-diff-modal-padding) * 2));
                    margin: 0 auto;
                    border-radius: 12px;
                    box-shadow: 0 18px 50px rgba(15, 23, 42, 0.35);
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                    box-sizing: border-box;
                }

                .query-diff-modal__header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 1rem;
                    padding: 1rem 1.25rem;
                    border-bottom: 1px solid #d0d7de;
                }

                .query-diff-modal__title {
                    font-size: 1.15rem;
                    font-weight: 700;
                }

                .query-diff-modal__body {
                    display: flex;
                    flex-direction: column;
                    gap: 1rem;
                    padding: 1rem 1.25rem 1.25rem 1.25rem;
                    flex: 1 1 auto;
                    min-height: 0;
                    overflow: hidden;
                }

                .query-diff-section {
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                    min-height: 0;
                    overflow: hidden;
                }

                .query-diff-section--query {
                    flex: 0 1 auto;
                }

                .query-diff-section--explanation {
                    flex: 0 1 auto;
                    max-height: 70%;
                }

                .query-diff-section__title {
                    font-weight: 700;
                    color: #234e73;
                }

                .query-diff-view {
                    border: 1px solid #d0d7de;
                    border-radius: 8px;
                    background: #f8fafc;
                    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
                    font-size: 0.9rem;
                    line-height: 1.45;
                    flex: 1 1 auto;
                    min-height: 0;
                    overflow: auto;
                    max-height: none;
                }

                .query-diff-row {
                    display: grid;
                    grid-template-columns:auto 1fr;
                    gap: 0.75rem;
                    padding: 0.15rem 0.75rem;
                    white-space: pre;
                }

                .query-diff-row__marker {
                    font-weight: 700;
                }

                .query-diff-row--added {
                    background: #ecfdf3;
                    color: #116329;
                }

                .query-diff-row--removed {
                    background: #fff1f0;
                    color: #9f1d1d;
                }

                .query-diff-row--context {
                    color: #334155;
                }

                @media (max-width: 900px) {
                    .query-form__row {
                        grid-template-columns:minmax(0, 1fr);
                    }

                    .query-form__label {
                        padding-top: 0;
                    }

                    .query-form__label:after {
                        content: '';
                    }

                    #query-sidebar-toggle {
                        top: 5.75rem;
                        left: 0.75rem;
                    }

                    .query-compare-layout--active {
                        grid-template-columns:minmax(0, 1fr);
                    }

                    #query-compare-controls {
                        flex-direction: row;
                        justify-content: flex-start;
                        width: auto;
                    }

                }

                .yasqe .CodeMirror {
                    height: auto;
                }

                .yasqe .CodeMirror-scroll {
                    height: auto;
                    min-height: 300px;
                    max-height: 55vh;
                    overflow-y: auto;
                    overflow-x: auto;
                }
            </style>
            <button id="query-sidebar-toggle" type="button"
                    aria-hidden="true" tabindex="-1"
                    data-show-label="{$show-menu.label}"
                    data-hide-label="{$hide-menu.label}"
                    onclick="workbench.query.toggleCompareSidebar()">
                <span id="query-sidebar-toggle-icon" class="query-sidebar-toggle__icon" aria-hidden="true">
                    <svg class="query-sidebar-toggle__svg" viewBox="0 0 24 24" focusable="false"
                         aria-hidden="true">
                        <path class="query-sidebar-toggle__stroke" d="M5 7.5H19"></path>
                        <path class="query-sidebar-toggle__stroke" d="M5 12H19"></path>
                        <path class="query-sidebar-toggle__stroke" d="M5 16.5H19"></path>
                    </svg>
                </span>
            </button>
            <div class="query-form">
                <div id="query-language-row" class="query-form__row">
                    <xsl:if test="$hideQueryLanguageRow">
                        <xsl:attribute name="style">display:none;</xsl:attribute>
                    </xsl:if>
                    <label class="query-form__label" for="queryLn">
                        <xsl:value-of select="$query-language.label"/>
                    </label>
                    <div class="query-form__field">
                        <select id="queryLn" name="queryLn" onchange="workbench.query.onQlChange()">
                            <xsl:for-each select="$info//sparql:binding[@name='query-format']">
                                <option value="{substring-before(sparql:literal, ' ')}">
                                    <xsl:choose>
                                        <xsl:when
                                                test="$info//sparql:binding[@name='default-queryLn']/sparql:literal = substring-before(sparql:literal, ' ')">
                                            <xsl:attribute name="selected">true</xsl:attribute>
                                        </xsl:when>
                                        <xsl:when test="$queryLn = substring-before(sparql:literal, ' ')">
                                            <xsl:attribute name="selected">true</xsl:attribute>
                                        </xsl:when>
                                    </xsl:choose>
                                    <xsl:value-of select="substring-after(sparql:literal, ' ')"/>
                                </option>
                            </xsl:for-each>
                        </select>
                    </div>
                </div>
                <div id="query-compare-layout" class="query-compare-layout">
                    <div id="query-primary-pane" class="query-compare-pane query-compare-pane--primary">
                        <div class="query-form__row query-form__row--stacked">
                            <label class="query-form__label" for="query">
                                <xsl:value-of select="$query-string.label"/>
                            </label>
                            <div class="query-form__field">
                                <textarea id="query" name="query" rows="16" cols="80" wrap="soft">
                                    <xsl:value-of select="$query"/>
                                </textarea>
                            </div>
                        </div>
                        <div class="query-form__row">
                            <span class="query-form__label query-form__label--blank"></span>
                            <div class="query-form__field">
                                <span id="queryString.errors" class="error">
                                    <xsl:value-of select="//sparql:binding[@name='error-message']"/>
                                </span>
                            </div>
                        </div>
                        <div id="query-explanation-row" class="query-form__row query-form__row--stacked">
                            <xsl:if test="string-length(normalize-space($explanation)) = 0">
                                <xsl:attribute name="style">display:none;</xsl:attribute>
                            </xsl:if>
                            <span class="query-form__label">
                                <xsl:value-of select="$query-explanation.label"/>
                            </span>
                            <div class="query-form__field">
                                <div id="query-explanation-status" class="query-explanation-status" aria-live="polite"></div>
                                <div class="query-explanation-surface">
                                    <div id="query-explanation-overlay" class="query-explanation-overlay"
                                         aria-hidden="true"></div>
                                    <pre id="query-explanation"
                                         style="margin-top:0; margin-bottom: 0; max-height:75vh; overflow:scroll;"
                                         data-format="{normalize-space($explanationFormat)}">
                                        <xsl:value-of select="$explanation"/>
                                    </pre>
                                    <div id="query-explanation-dot-view"></div>
                                    <div id="query-explanation-json-view"></div>
                                </div>
                            </div>
                        </div>
                        <div id="query-explanation-controls-row" class="query-explanation-controls-row-class">
                            <xsl:if test="string-length(normalize-space($explanation)) = 0">
                                <xsl:attribute name="style">display:none;</xsl:attribute>
                            </xsl:if>
                            <div class="query-form__field query-form__field--controls">
                                <span id="primary-explain-settings" class="query-form__field--controls-group">
                                    <select id="explain-format" name="explain-format">
                                        <option value="text">
                                            <xsl:if test="normalize-space($explanationFormat) = '' or normalize-space($explanationFormat) = 'text'">
                                                <xsl:attribute name="selected">selected</xsl:attribute>
                                            </xsl:if>
                                            Text
                                        </option>
                                        <option value="dot">
                                            <xsl:if test="normalize-space($explanationFormat) = 'dot'">
                                                <xsl:attribute name="selected">selected</xsl:attribute>
                                            </xsl:if>
                                            DOT
                                        </option>
                                        <option value="json">
                                            <xsl:if test="normalize-space($explanationFormat) = 'json'">
                                                <xsl:attribute name="selected">selected</xsl:attribute>
                                            </xsl:if>
                                            JSON
                                        </option>
                                    </select>
                                    <select id="explain-level">
                                        <option value="Unoptimized">
                                            <xsl:if test="normalize-space($explanationLevel) = 'Unoptimized'">
                                                <xsl:attribute name="selected">selected</xsl:attribute>
                                            </xsl:if>
                                            Unoptimized
                                        </option>
                                        <option value="Optimized">
                                            <xsl:if test="normalize-space($explanationLevel) = '' or normalize-space($explanationLevel) = 'Optimized'">
                                                <xsl:attribute name="selected">selected</xsl:attribute>
                                            </xsl:if>
                                            Optimized
                                        </option>
                                        <option value="Executed">
                                            <xsl:if test="normalize-space($explanationLevel) = 'Executed'">
                                                <xsl:attribute name="selected">selected</xsl:attribute>
                                            </xsl:if>
                                            Executed
                                        </option>
                                        <option value="Telemetry">
                                            <xsl:if test="normalize-space($explanationLevel) = 'Telemetry'">
                                                <xsl:attribute name="selected">selected</xsl:attribute>
                                            </xsl:if>
                                            Telemetry
                                        </option>
                                        <option value="Timed">
                                            <xsl:if test="normalize-space($explanationLevel) = 'Timed'">
                                                <xsl:attribute name="selected">selected</xsl:attribute>
                                            </xsl:if>
                                            Timed
                                        </option>
                                    </select>
                                </span>
                                <span id="primary-explain-repeat-controls" class="query-form__field--controls-group">
                                    <input id="rerun-explanation" type="button"
                                           value="{$explain-query.label}"
                                           onclick="workbench.query.runExplain(null, 'rerun-explanation')"/>
                                    <span id="rerun-explanation-spinner" class="query-explain-spinner"
                                          aria-hidden="true"></span>
                                    <input id="rerun-explanation-cancel" class="query-explain-cancel"
                                           type="button" value="{$cancel.label}" onclick="workbench.query.cancelExplain()"
                                           aria-hidden="true" disabled="disabled"/>
                                </span>
                                <input id="download-explanation" type="button"
                                       value="{$download-explanation.label}">
                                    <xsl:if test="string-length(normalize-space($explanation)) = 0">
                                        <xsl:attribute name="disabled">disabled</xsl:attribute>
                                    </xsl:if>
                                </input>
                                <input id="compare-toggle" type="button"
                                       value="{$compare.label}" onclick="workbench.query.toggleCompareMode()"/>
                            </div>
                        </div>
                    </div>
                    <div id="query-compare-controls">
                        <button id="explain-compare-trigger" class="query-compare-action" type="button"
                                aria-label="{$refresh-explanations.label}" title="{$refresh-explanations.label}"
                                onclick="workbench.query.runCompareExplain()">
                            <svg id="explain-compare-trigger-icon"
                                 class="query-compare-action__svg query-compare-action__svg--refresh"
                                 focusable="false" aria-hidden="true" viewBox="0 0 118.04 122.88">
                                <path class="query-compare-action__fill"
                                      d="M16.08,59.26A8,8,0,0,1,0,59.26a59,59,0,0,1,97.13-45V8a8,8,0,1,1,16.08,0V33.35a8,8,0,0,1-8,8L80.82,43.62a8,8,0,1,1-1.44-15.95l8-.73A43,43,0,0,0,16.08,59.26Zm22.77,19.6a8,8,0,0,1,1.44,16l-10.08.91A42.95,42.95,0,0,0,102,63.86a8,8,0,0,1,16.08,0A59,59,0,0,1,22.3,110v4.18a8,8,0,0,1-16.08,0V89.14h0a8,8,0,0,1,7.29-8l25.31-2.3Z"></path>
                            </svg>
                        </button>
                        <button id="explain-compare-cancel" class="query-compare-action query-explain-cancel"
                                type="button" aria-label="{$cancel.label}" title="{$cancel.label}"
                                onclick="workbench.query.cancelCompareExplain()" aria-hidden="true"
                                disabled="disabled">
                            <span class="query-compare-action__icon query-compare-action__icon--cancel"
                                  aria-hidden="true">&#x2715;</span>
                        </button>
                        <button id="query-diff-trigger" class="query-compare-action" type="button"
                                aria-label="{$diff.label}" title="{$diff.label}"
                                onclick="workbench.query.openDiffModal()" disabled="disabled">
                            <span id="query-diff-trigger-icon" class="query-compare-action__icon"
                                  aria-hidden="true">
                                <svg class="query-compare-action__svg query-compare-action__svg--diff" focusable="false" aria-hidden="true" viewBox="0 0 26 24">
                                    <g style="transform: translateX(0.1rem) translateY(0.1rem);">
                                        <path class="query-compare-action__stroke" d="M2 8H16"></path>
                                        <path class="query-compare-action__stroke" d="M12.5 4.5L16 8L12.5 11.5"></path>
                                    </g>
                                    <g style="transform: translateX(-0.1rem) translateY(-0.1rem);">
                                        <path class="query-compare-action__stroke" d="M22 20H8"></path>
                                        <path class="query-compare-action__stroke" d="M11.5 16.5L8 20L11.5 23.5"></path>
                                    </g>
                                </svg>
                            </span>
                        </button>
                    </div>
                    <div id="query-compare-pane" class="query-compare-pane query-compare-pane--secondary">
                        <div class="query-form__row query-form__row--stacked">
                            <label class="query-form__label" for="query-compare">
                                <xsl:value-of select="$compare-query.label"/>
                            </label>
                            <div class="query-form__field">
                                <textarea id="query-compare" rows="16" cols="80" wrap="soft"></textarea>
                            </div>
                        </div>
                        <div class="query-form__row">
                            <span class="query-form__label query-form__label--blank"></span>
                            <div class="query-form__field">
                                <span id="queryString.errors-compare" class="error"></span>
                            </div>
                        </div>
                        <div id="query-explanation-row-compare" class="query-form__row query-form__row--stacked">
                            <span class="query-form__label">
                                <xsl:value-of select="$query-explanation.label"/>
                            </span>
                            <div class="query-form__field">
                                <div id="query-explanation-status-compare" class="query-explanation-status"
                                     aria-live="polite"></div>
                                <div class="query-explanation-surface">
                                    <div id="query-explanation-overlay-compare" class="query-explanation-overlay"
                                         aria-hidden="true"></div>
                                    <pre id="query-explanation-compare"
                                         style="margin-top:0; margin-bottom: 0; max-height:75vh; overflow:scroll;"
                                         data-format="text"></pre>
                                    <div id="query-explanation-dot-view-compare"></div>
                                    <div id="query-explanation-json-view-compare"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="query-form__row">
                    <span class="query-form__label">
                        <xsl:value-of select="$result-limit.label"/>
                    </span>
                    <div class="query-form__field">
                        <xsl:call-template name="limit-select">
                            <xsl:with-param name="limit_id">limit_query</xsl:with-param>
                        </xsl:call-template>
                    </div>
                </div>
                <div class="query-form__row">
                    <label class="query-form__label" for="query-timeout">
                        <xsl:value-of select="$query-timeout.label"/>
                    </label>
                    <div class="query-form__field">
                        <input id="query-timeout" name="query-timeout" type="number" min="0" step="1">
                            <xsl:attribute name="value">
                                <xsl:choose>
                                    <xsl:when test="string-length(normalize-space($queryTimeout)) &gt; 0">
                                        <xsl:value-of select="$queryTimeout"/>
                                    </xsl:when>
                                    <xsl:when test="string-length(normalize-space($defaultQueryTimeout)) &gt; 0">
                                        <xsl:value-of select="$defaultQueryTimeout"/>
                                    </xsl:when>
                                    <xsl:otherwise>0</xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>
                        </input>
                    </div>
                </div>
                <div class="query-form__row">
                    <span class="query-form__label">
                        <xsl:value-of select="$query-options.label"/>
                    </span>
                    <div class="query-form__field query-form__field--options">
                        <input id="infer" name="infer" type="checkbox" value="true">
                            <xsl:if
                                    test="$info//sparql:binding[@name='default-infer']/sparql:literal = 'true'">
                                <xsl:attribute name="checked">true</xsl:attribute>
                            </xsl:if>
                        </input>
                        <label for="infer">
                            <xsl:value-of select="$include-inferred.label"/>
                        </label>
                        <input id="save-private" name="save-private" type="checkbox" value="true"/>
                        <label for="save-private">
                            <xsl:value-of select="$save-private.label"/>
                        </label>
                    </div>
                </div>
                <div class="query-form__row">
                    <span class="query-form__label">
                        <xsl:value-of select="$query-actions.label"/>
                    </span>
                    <div class="query-form__field query-form__field--actions">
                        <input type="button" onclick="workbench.query.resetNamespaces()" value="Clear"/>
                        <input id="exec" type="submit" value="{$execute.label}"/>
                        <input id="explain-trigger" type="button"
                               value="{$explain-query.label}" onclick="workbench.query.runExplain(null, 'explain-trigger')"/>
                        <span id="explain-trigger-spinner" class="query-explain-spinner"
                              aria-hidden="true"></span>
                        <input id="explain-trigger-cancel" class="query-explain-cancel"
                               type="button" value="{$cancel.label}" onclick="workbench.query.cancelExplain()"
                               aria-hidden="true" disabled="disabled"/>
                        <input id="save" type="submit" value="{$save.label}" disabled="disabled"/>
                        <input id="query-name" name="query-name" type="text" size="32"
                               maxlength="32" value=""/>
                        <span id="save-feedback"></span>
                    </div>
                </div>
            </div>
        </form>
        <div id="query-diff-modal" class="query-diff-modal" aria-hidden="true">
            <div class="query-diff-modal__dialog" role="dialog" aria-modal="true"
                 aria-labelledby="query-diff-modal-title">
                <div class="query-diff-modal__header">
                    <div id="query-diff-modal-title" class="query-diff-modal__title">
                        <xsl:value-of select="$diff.label"/>
                    </div>
                    <input id="query-diff-close" type="button" value="&#x2715;"
                           onclick="workbench.query.closeDiffModal()" style="font-size: 1rem"/>
                </div>
                <div class="query-diff-modal__body">
                    <div class="query-diff-section query-diff-section--query">
                        <div class="query-diff-section__title">
                            <xsl:value-of select="$query-diff.title"/>
                        </div>
                        <div id="query-diff-query" class="query-diff-view"></div>
                    </div>
                    <div class="query-diff-section query-diff-section--explanation">
                        <div class="query-diff-section__title">
                            <xsl:value-of select="$explanation-diff.title"/>
                        </div>
                        <div id="query-diff-explanation" class="query-diff-view">
                            <xsl:value-of select="$diff-not-ready.label"/>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <script type="text/javascript">
            var sparqlNamespaces = {
            <xsl:for-each
                    select="document(//sparql:link[@href='namespaces']/@href)//sparql:results/sparql:result">
                <xsl:value-of
                        select="concat('&quot;', sparql:binding[@name='prefix']/sparql:literal, ':&quot;:&quot;', sparql:binding[@name='namespace']/sparql:literal, '&quot;,')"/>
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
            };
        </script>
        <script src="../../scripts/codemirror.4.5.0.min.js" type="text/javascript"></script>
        <script src="../../scripts/yasqe.min.js" type="text/javascript"></script>
        <script src="../../scripts/yasqeHelper.js" type="text/javascript"></script>
        <script src="../../scripts/queryCancelPolicy.js" type="text/javascript"></script>
        <script src="../../scripts/viz/viz.js" type="text/javascript"></script>
        <script src="../../scripts/viz/full.render.js" type="text/javascript"></script>
        <script src="../../scripts/svg-pan-zoom.min.js" type="text/javascript"></script>
        <script src="../../scripts/query.js" type="text/javascript"></script>

    </xsl:template>
</xsl:stylesheet>
