<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

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
        <link rel="stylesheet" type="text/css" href="../../styles/query.css"/>
        <form action="query" method="post" onsubmit="return workbench.query.doSubmit()">
            <input type="hidden" name="action" id="action"/>
            <input type="hidden" name="explain" id="explain"/>
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
                    </xsl:call-template>
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
        <script src="../../scripts/diff.min.js" type="text/javascript"></script>
        <script src="../../scripts/viz/viz.js" type="text/javascript"></script>
        <script src="../../scripts/viz/full.render.js" type="text/javascript"></script>
        <script src="../../scripts/svg-pan-zoom.min.js" type="text/javascript"></script>
        <script src="../../scripts/query.js" type="text/javascript"></script>

    </xsl:template>
</xsl:stylesheet>
