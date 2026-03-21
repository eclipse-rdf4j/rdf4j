<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:sparql="http://www.w3.org/2005/sparql-results#"
                xmlns:workbench="https://rdf4j.org/schema/workbench#"
                xmlns="http://www.w3.org/1999/xhtml">

    <xsl:include href="../locale/messages.xsl"/>

    <xsl:variable name="title">
        <xsl:value-of select="$query.title"/>
    </xsl:variable>

    <xsl:include href="template.xsl"/>

    <xsl:template name="query-pane">
        <xsl:param name="paneId"/>
        <xsl:param name="paneClass"/>
        <xsl:param name="queryId"/>
        <xsl:param name="queryName"/>
        <xsl:param name="queryLabel"/>
        <xsl:param name="queryValue"/>
        <xsl:param name="errorId"/>
        <xsl:param name="errorValue"/>
        <xsl:param name="explanationRowId"/>
        <xsl:param name="explanationVisible" select="true()"/>
        <xsl:param name="statusId"/>
        <xsl:param name="overlayId"/>
        <xsl:param name="explanationId"/>
        <xsl:param name="explanationFormat"/>
        <xsl:param name="explanationLevel"/>
        <xsl:param name="explanationValue"/>
        <xsl:param name="dotViewId"/>
        <xsl:param name="jsonViewId"/>
        <xsl:param name="copyButtonId"/>
        <xsl:param name="showControls" select="false()"/>
        <xsl:param name="controlsRowId"/>

        <div id="{$paneId}" class="{$paneClass}">
            <div class="query-form__row query-form__row--stacked">
                <label class="query-form__label" for="{$queryId}">
                    <xsl:value-of select="$queryLabel"/>
                </label>
                <div class="query-form__field">
                    <textarea id="{$queryId}" rows="16" cols="80" wrap="soft">
                        <xsl:if test="string-length($queryName) &gt; 0">
                            <xsl:attribute name="name">
                                <xsl:value-of select="$queryName"/>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:value-of select="$queryValue"/>
                    </textarea>
                </div>
            </div>
            <div class="query-form__row">
                <span class="query-form__label query-form__label--blank"></span>
                <div class="query-form__field">
                    <span id="{$errorId}" class="error">
                        <xsl:value-of select="$errorValue"/>
                    </span>
                </div>
            </div>
            <div id="{$explanationRowId}" class="query-form__row query-form__row--stacked">
                <xsl:if test="not($explanationVisible)">
                    <xsl:attribute name="style">display:none;</xsl:attribute>
                </xsl:if>
                <span class="query-form__label">
                    <xsl:value-of select="$query-explanation.label"/>
                </span>
                <div class="query-form__field">
                    <div id="{$statusId}" class="query-explanation-status" aria-live="polite"></div>
                    <div class="query-explanation-surface">
                        <button id="{$copyButtonId}" class="query-explanation-copy" type="button"
                                aria-label="{$copy-explanation.label}" title="{$copy-explanation.label}">
                            <svg class="query-explanation-copy__svg" viewBox="0 0 24 24" focusable="false"
                                 aria-hidden="true">
                                <rect class="query-explanation-copy__stroke" x="9" y="9" width="10" height="10"
                                      rx="1.5"></rect>
                                <path class="query-explanation-copy__stroke" d="M7 15H6a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v1"></path>
                            </svg>
                        </button>
                        <div id="{$overlayId}" class="query-explanation-overlay" aria-hidden="true"></div>
                        <pre id="{$explanationId}" data-format="{$explanationFormat}">
                            <xsl:value-of select="$explanationValue"/>
                        </pre>
                        <div id="{$dotViewId}"></div>
                        <div id="{$jsonViewId}"></div>
                    </div>
                </div>
            </div>
            <xsl:if test="$showControls">
                <div id="{$controlsRowId}" class="query-explanation-controls-row-class">
                    <xsl:if test="not($explanationVisible)">
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
                            <xsl:if test="not($explanationVisible)">
                                <xsl:attribute name="disabled">disabled</xsl:attribute>
                            </xsl:if>
                        </input>
                        <input id="compare-toggle" type="button"
                               value="{$compare.label}" onclick="workbench.query.toggleCompareMode()"/>
                    </div>
                </div>
            </xsl:if>
        </div>
    </xsl:template>

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
        <xsl:variable name="trace"
                      select="sparql:results/sparql:result/sparql:binding[@name='trace']/sparql:literal"/>
        <link rel="stylesheet" type="text/css" href="../../styles/query.css"/>
        <form action="query" method="post" onsubmit="return workbench.query.doSubmit()">
            <input type="hidden" name="action" id="action"/>
            <input type="hidden" name="explain" id="explain"/>
            <input type="hidden" name="trace" id="trace" value="false"/>
            <input type="hidden" name="ref" value="text"/>
            <input type="hidden" name="include-query-text" id="include-query-text" value="false"/>
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
                    <xsl:call-template name="query-pane">
                        <xsl:with-param name="paneId">query-primary-pane</xsl:with-param>
                        <xsl:with-param name="paneClass">query-compare-pane query-compare-pane--primary</xsl:with-param>
                        <xsl:with-param name="queryId">query</xsl:with-param>
                        <xsl:with-param name="queryName">query</xsl:with-param>
                        <xsl:with-param name="queryLabel" select="$query-string.label"/>
                        <xsl:with-param name="queryValue" select="$query"/>
                        <xsl:with-param name="errorId">queryString.errors</xsl:with-param>
                        <xsl:with-param name="errorValue" select="//sparql:binding[@name='error-message']"/>
                        <xsl:with-param name="explanationRowId">query-explanation-row</xsl:with-param>
                        <xsl:with-param name="explanationVisible"
                                        select="string-length(normalize-space($explanation)) &gt; 0"/>
                        <xsl:with-param name="statusId">query-explanation-status</xsl:with-param>
                        <xsl:with-param name="overlayId">query-explanation-overlay</xsl:with-param>
                        <xsl:with-param name="explanationId">query-explanation</xsl:with-param>
                        <xsl:with-param name="explanationFormat" select="normalize-space($explanationFormat)"/>
                        <xsl:with-param name="explanationLevel" select="$explanationLevel"/>
                        <xsl:with-param name="explanationValue" select="$explanation"/>
                        <xsl:with-param name="dotViewId">query-explanation-dot-view</xsl:with-param>
                        <xsl:with-param name="jsonViewId">query-explanation-json-view</xsl:with-param>
                        <xsl:with-param name="copyButtonId">copy-explanation</xsl:with-param>
                        <xsl:with-param name="showControls" select="true()"/>
                        <xsl:with-param name="controlsRowId">query-explanation-controls-row</xsl:with-param>
                    </xsl:call-template>
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
                            <svg id="explain-compare-cancel-icon"
                                 class="query-compare-action__svg query-compare-action__svg--cancel"
                                 focusable="false" aria-hidden="true" viewBox="0 0 305.002 305.002">
                                <path class="query-compare-action__fill"
                                      d="M152.502,0.001C68.412,0.001,0,68.412,0,152.501s68.412,152.5,152.502,152.5c84.089,0,152.5-68.411,152.5-152.5S236.591,0.001,152.502,0.001z M152.502,280.001C82.197,280.001,25,222.806,25,152.501c0-70.304,57.197-127.5,127.502-127.5c70.304,0,127.5,57.196,127.5,127.5C280.002,222.806,222.806,280.001,152.502,280.001z"></path>
                                <path class="query-compare-action__fill"
                                      d="M170.18,152.5l43.13-43.129c4.882-4.882,4.882-12.796,0-17.678c-4.881-4.882-12.796-4.881-17.678,0l-43.13,43.13l-43.131-43.131c-4.882-4.881-12.796-4.881-17.678,0c-4.881,4.882-4.881,12.796,0,17.678l43.13,43.13l-43.131,43.131c-4.881,4.882-4.881,12.796,0,17.679c2.441,2.44,5.64,3.66,8.839,3.66c3.199,0,6.398-1.221,8.839-3.66l43.131-43.132l43.131,43.132c2.441,2.439,5.64,3.66,8.839,3.66s6.398-1.221,8.839-3.66c4.882-4.883,4.882-12.797,0-17.679L170.18,152.5z"></path>
                            </svg>
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
                    <xsl:call-template name="query-pane">
                        <xsl:with-param name="paneId">query-compare-pane</xsl:with-param>
                        <xsl:with-param name="paneClass">query-compare-pane query-compare-pane--secondary</xsl:with-param>
                        <xsl:with-param name="queryId">query-compare</xsl:with-param>
                        <xsl:with-param name="queryLabel" select="$compare-query.label"/>
                        <xsl:with-param name="queryValue" select="''"/>
                        <xsl:with-param name="errorId">queryString.errors-compare</xsl:with-param>
                        <xsl:with-param name="errorValue" select="''"/>
                        <xsl:with-param name="explanationRowId">query-explanation-row-compare</xsl:with-param>
                        <xsl:with-param name="statusId">query-explanation-status-compare</xsl:with-param>
                        <xsl:with-param name="overlayId">query-explanation-overlay-compare</xsl:with-param>
                        <xsl:with-param name="explanationId">query-explanation-compare</xsl:with-param>
                        <xsl:with-param name="explanationFormat" select="'text'"/>
                        <xsl:with-param name="explanationLevel" select="''"/>
                        <xsl:with-param name="explanationValue" select="''"/>
                        <xsl:with-param name="dotViewId">query-explanation-dot-view-compare</xsl:with-param>
                        <xsl:with-param name="jsonViewId">query-explanation-json-view-compare</xsl:with-param>
                        <xsl:with-param name="copyButtonId">copy-explanation-compare</xsl:with-param>
                    </xsl:call-template>
                </div>
                <div class="query-form__row query-form__row--stacked query-trace-row">
                    <span class="query-form__label query-form__label--blank">Trace</span>
                    <div class="query-form__field">
                        <div id="query-trace-row" class="query-trace-shell">
                            <div class="query-trace-topbar">
                                <div class="query-trace-titleblock">
                                    <span class="query-trace-title">Trace</span>
                                    <div id="query-trace-status" class="query-explanation-status query-trace-status"
                                         aria-live="polite"></div>
                                </div>
                                <details class="query-trace-meta">
                                    <summary class="query-trace-meta-toggle">Details</summary>
                                    <div class="query-trace-meta-body">
                                        <div id="query-trace-summary" class="query-trace-meta-summary"></div>
                                        <div id="query-trace-frame-label" class="query-trace-meta-frame"></div>
                                        <div class="query-trace-meta-actions">
                                            <input id="trace-reset" type="button" value="Start over" disabled="disabled"/>
                                            <input id="download-trace" type="button" value="Download JSON"
                                                   disabled="disabled"/>
                                        </div>
                                    </div>
                                </details>
                            </div>
                            <div class="query-trace-transport">
                                <div class="query-trace-transport-buttons">
                                    <input id="trace-previous" type="button" value="Back" disabled="disabled"/>
                                    <input id="trace-playback-toggle" type="button" value="Play" disabled="disabled"/>
                                    <input id="trace-next" type="button" value="Next" disabled="disabled"/>
                                </div>
                                <div id="query-trace-step-label" class="query-trace-step-label"
                                     aria-live="polite"></div>
                                <div class="query-trace-transport-scrubber">
                                    <label class="query-trace-scrubber-label" for="query-trace-scrubber">
                                        Step through execution
                                    </label>
                                    <input id="query-trace-scrubber" class="query-trace-scrubber" type="range"
                                           min="0" max="0" value="0" disabled="disabled"/>
                                </div>
                            </div>
                            <div class="query-trace-canvas">
                                <div id="query-trace-patterns" class="query-trace-patterns"></div>
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
                        <input id="preserve-query-order" name="preserve-query-order" type="checkbox" value="true"/>
                        <label for="preserve-query-order">
                            <xsl:value-of select="$preserve-query-order.label"/>
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
                        <input id="trace-trigger" type="button" value="Trace"
                               onclick="workbench.query.runTrace()"/>
                        <span id="trace-trigger-spinner" class="query-explain-spinner"
                              aria-hidden="true"></span>
                        <input id="trace-trigger-cancel" class="query-explain-cancel"
                               type="button" value="{$cancel.label}" onclick="workbench.query.cancelTrace()"
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
            var rdf4jTraceNamespaces =
            <xsl:choose>
                <xsl:when test="string-length(normalize-space(workbench:metadata/workbench:trace-namespaces/text())) &gt; 0">
                    <xsl:value-of select="workbench:metadata/workbench:trace-namespaces/text()"/>
                </xsl:when>
                <xsl:otherwise>{}</xsl:otherwise>
            </xsl:choose>;
            var sparqlNamespaces = {
            <xsl:for-each
                    select="document(//sparql:link[@href='namespaces']/@href)//sparql:results/sparql:result">
                <xsl:value-of
                        select="concat('&quot;', sparql:binding[@name='prefix']/sparql:literal, ':&quot;:&quot;', sparql:binding[@name='namespace']/sparql:literal, '&quot;,')"/>
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
            };
            var traceNamespaces = Object.assign({}, rdf4jTraceNamespaces, sparqlNamespaces);
        </script>
        <script src="../../scripts/codemirror.4.5.0.min.js" type="text/javascript"></script>
        <script src="../../scripts/yasqe.min.js" type="text/javascript"></script>
        <script src="../../scripts/yasqeHelper.js" type="text/javascript"></script>
        <script src="../../scripts/queryCancelPolicy.js" type="text/javascript"></script>
        <script src="../../scripts/diff.min.js" type="text/javascript"></script>
        <script src="../../scripts/viz/viz.js" type="text/javascript"></script>
        <script src="../../scripts/viz/full.render.js" type="text/javascript"></script>
        <script src="../../scripts/svg-pan-zoom.min.js" type="text/javascript"></script>
        <script src="../../scripts/query-trace-player.js" type="text/javascript"></script>
        <script src="../../scripts/query.js" type="text/javascript"></script>

    </xsl:template>
</xsl:stylesheet>
