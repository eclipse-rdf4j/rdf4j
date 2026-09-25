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
                    <div class="query-explanation-toolbar">
                        <span class="workbench-action workbench-action--secondary" data-workbench-action="copy">
                            <span class="workbench-action-label">
                                <button id="{$copyButtonId}" class="query-explanation-copy" type="button"
                                        aria-label="{$copy-explanation.label}" title="{$copy-explanation.label}">
                                    <svg class="workbench-action-icon query-explanation-copy__svg" viewBox="0 0 24 24"
                                         focusable="false" aria-hidden="true">
                                        <rect class="query-explanation-copy__stroke" x="9" y="9" width="10" height="10"
                                              rx="1.5"></rect>
                                        <path class="query-explanation-copy__stroke" d="M7 15H6a1 1 0 0 1-1-1V6a1 1 0 0 1 1 1h8a1 1 0 0 1 1 1v1"></path>
                                    </svg>
                                    <xsl:value-of select="$copy.label"/>
                                </button>
                            </span>
                        </span>
                    </div>
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
                            <span id="explanation-settings" class="query-explanation-settings">
								<span class="workbench-action workbench-action--secondary" data-workbench-action="settings">
									<span class="workbench-action-label">
										<button id="explanation-settings-toggle"
											class="query-explanation-settings__toggle" type="button"
											aria-controls="explanation-settings-panel" aria-expanded="false">
											<xsl:call-template name="workbench-action-icon">
												<xsl:with-param name="name">settings</xsl:with-param>
											</xsl:call-template>
											<span>Config</span>
											<xsl:call-template name="workbench-action-icon">
												<xsl:with-param name="name">chevron</xsl:with-param>
												<xsl:with-param name="additional-class">workbench-disclosure-chevron</xsl:with-param>
											</xsl:call-template>
										</button>
									</span>
								</span>
                                <div id="explanation-settings-panel"
                                     class="query-explanation-settings__panel" role="group"
                                     aria-label="Explanation display settings" hidden="hidden">
                                    <div class="query-explanation-settings__section">
                                        <div class="query-explanation-settings__header">
                                            <strong>Highlighting</strong>
                                        </div>
                                        <div class="query-explanation-settings__highlighting">
                                            <span id="explanation-highlight-mode"
                                                  class="query-explanation-highlight-mode" role="radiogroup"
                                                  aria-label="Text explanation highlighting" aria-hidden="false">
                                                <input id="explanation-highlight-syntax"
                                                       name="explanation-highlight-mode" type="radio"
                                                       value="syntax" checked="checked"/>
                                                <label for="explanation-highlight-syntax">Normal</label>
                                                <input id="explanation-highlight-hotspot"
                                                       name="explanation-highlight-mode" type="radio"
                                                       value="hotspot"/>
                                                <label for="explanation-highlight-hotspot">Heatmap</label>
                                            </span>
                                            <span id="explanation-hotspot-legend"
                                                  class="query-explanation-hotspot-legend"
                                                  aria-live="polite"></span>
                                        </div>
                                    </div>
                                    <div id="explanation-property-config"
                                         class="query-explanation-property-config query-explanation-settings__section">
                                        <div class="query-explanation-property-config__header">
                                            <span class="query-explanation-property-config__title">
                                                <strong>Visible properties</strong>
                                                <span id="explanation-property-count"
                                                      class="query-explanation-property-config__count"
                                                      aria-live="polite"></span>
                                            </span>
                                            <span class="query-explanation-property-config__actions">
                                                <button id="explanation-properties-all" type="button">All</button>
                                                <button id="explanation-properties-none" type="button">None</button>
                                            </span>
                                        </div>
                                        <div id="explanation-property-options"
                                             class="query-explanation-property-config__options"
                                             role="group" aria-label="Visible query plan properties"></div>
                                        <p class="query-explanation-property-config__hint">
                                            Plan structure always remains visible.
                                        </p>
                                    </div>
                                </div>
                            </span>
                        </span>
						<span id="primary-explain-repeat-controls" class="query-form__field--controls-group">
							<span class="workbench-action workbench-action--secondary" data-workbench-action="explain">
								<label class="workbench-action-hit-area">
									<xsl:call-template name="workbench-action-icon">
										<xsl:with-param name="name">explain</xsl:with-param>
									</xsl:call-template>
									<span class="workbench-action-label"><input id="rerun-explanation" type="button"
										value="{$explain-query.label}"
										onclick="workbench.query.runExplain(null, 'rerun-explanation')"/></span>
								</label>
							</span>
							<span id="rerun-explanation-spinner" class="query-explain-spinner"
								  aria-hidden="true"></span>
							<span class="workbench-action workbench-action--danger query-explain-cancel" data-workbench-action="cancel">
								<label class="workbench-action-hit-area">
									<xsl:call-template name="workbench-action-icon">
										<xsl:with-param name="name">cancel</xsl:with-param>
									</xsl:call-template>
									<span class="workbench-action-label"><input id="rerun-explanation-cancel" class="query-explain-cancel" type="button"
										value="{$cancel.label}" onclick="workbench.query.cancelExplain()"
										aria-hidden="true" disabled="disabled"/></span>
								</label>
							</span>
						</span>
						<span class="workbench-action workbench-action--secondary" data-workbench-action="download">
							<label class="workbench-action-hit-area">
								<xsl:call-template name="workbench-action-icon">
									<xsl:with-param name="name">download</xsl:with-param>
								</xsl:call-template>
								<span class="workbench-action-label"><input id="download-explanation" type="button"
									value="{$download-explanation.label}">
									<xsl:if test="not($explanationVisible)">
										<xsl:attribute name="disabled">disabled</xsl:attribute>
									</xsl:if>
								</input></span>
							</label>
						</span>
						<span class="workbench-action workbench-action--secondary" data-workbench-action="compare">
							<label class="workbench-action-hit-area">
								<xsl:call-template name="workbench-action-icon">
									<xsl:with-param name="name">compare</xsl:with-param>
								</xsl:call-template>
								<span class="workbench-action-label"><input id="compare-toggle" type="button"
									value="{$compare.label}" onclick="workbench.query.toggleCompareMode()"/></span>
							</label>
						</span>
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
        <div id="query-page" class="query-page">
        <form action="query" method="post" onsubmit="return workbench.query.doSubmit()">
            <input type="hidden" name="action" id="action"/>
            <input type="hidden" name="explain" id="explain"/>
            <input type="hidden" name="ref" value="text"/>
            <input type="hidden" name="include-query-text" id="include-query-text" value="false"/>
            <input type="hidden" name="query-request-id" id="query-request-id" value=""/>
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
                <div id="query-compare-toolbar" class="query-compare-toolbar" hidden="hidden">
                    <span class="workbench-action workbench-action--secondary" data-workbench-action="copy">
                        <span class="workbench-action-label">
                            <button id="query-compare-copy" type="button" aria-label="{$copy.label}"
                                    title="{$copy-explanation.label}">
                                <xsl:call-template name="workbench-action-icon">
                                    <xsl:with-param name="name">copy</xsl:with-param>
                                </xsl:call-template>
                                <span><xsl:value-of select="$copy.label"/></span>
                            </button>
                        </span>
                    </span>
                    <span class="workbench-action workbench-action--secondary" data-workbench-action="swap">
                        <span class="workbench-action-label">
                            <button id="query-compare-swap" type="button" aria-label="{$swap.label}"
                                    title="{$swap.label}">
                                <xsl:call-template name="workbench-action-icon">
                                    <xsl:with-param name="name">swap</xsl:with-param>
                                </xsl:call-template>
                                <span><xsl:value-of select="$swap.label"/></span>
                            </button>
                        </span>
                    </span>
                    <div id="query-compare-controls" class="query-compare-toolbar__actions">
                        <span class="workbench-action workbench-action--secondary" data-workbench-action="refresh">
                            <span class="workbench-action-label">
                                <button id="explain-compare-trigger" class="query-compare-action" type="button"
                                        aria-label="{$refresh-explanations.label}" title="{$refresh-explanations.label}"
                                        onclick="workbench.query.runCompareExplain()">
                                    <svg id="explain-compare-trigger-icon"
                                         class="query-compare-action__svg query-compare-action__svg--refresh"
                                         focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                        <path class="query-compare-action__stroke" d="M20 10a8 8 0 0 0-14-4L4 8"></path>
                                        <path class="query-compare-action__stroke" d="M4 4v4h4"></path>
                                        <path class="query-compare-action__stroke" d="M4 14a8 8 0 0 0 14 4l2-2"></path>
                                        <path class="query-compare-action__stroke" d="M20 20v-4h-4"></path>
                                    </svg>
                                    <span><xsl:value-of select="$refresh-explanations.label" /></span>
                                </button>
                            </span>
                        </span>
                        <span class="workbench-action workbench-action--danger query-explain-cancel"
                              data-workbench-action="cancel">
                            <span class="workbench-action-label">
                                <button id="explain-compare-cancel" class="query-compare-action query-explain-cancel"
                                        type="button" aria-label="{$cancel.label}" title="{$cancel.label}"
                                        onclick="workbench.query.cancelCompareExplain()" aria-hidden="true"
                                        disabled="disabled">
                                    <svg id="explain-compare-cancel-icon"
                                         class="query-compare-action__svg query-compare-action__svg--cancel"
                                         focusable="false" aria-hidden="true" viewBox="0 0 24 24">
                                        <path d="M6 6l12 12M18 6 6 18"></path>
                                    </svg>
                                    <span><xsl:value-of select="$cancel.label" /></span>
                                </button>
                            </span>
                        </span>
                        <span class="workbench-action workbench-action--secondary" data-workbench-action="diff">
                            <span class="workbench-action-label">
                                <button id="query-diff-trigger" class="query-compare-action" type="button"
                                        aria-label="{$diff.label}" title="{$diff.label}"
                                        onclick="workbench.query.openDiffModal()" disabled="disabled">
                                    <span id="query-diff-trigger-icon" class="query-compare-action__icon"
                                          aria-hidden="true">
                                        <svg class="query-compare-action__svg query-compare-action__svg--diff"
                                             focusable="false" aria-hidden="true" viewBox="0 0 26 24">
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
                                    <span><xsl:value-of select="$diff.label" /></span>
                                </button>
                            </span>
                        </span>
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
                <div class="query-actions-toolbar">
                    <div class="query-form__field query-form__field--actions query-actions-toolbar__primary">
                        <button id="exec" class="query-action query-action--primary" type="submit">
                            <xsl:call-template name="workbench-action-icon">
                                <xsl:with-param name="name">execute</xsl:with-param>
                                <xsl:with-param name="additional-class">query-action-icon</xsl:with-param>
                            </xsl:call-template>
                            <span><xsl:value-of select="$execute.label" /></span>
                        </button>
                        <input id="query-cancel" class="query-cancel" type="button"
                               value="{$cancel.label}" onclick="workbench.query.cancelQuery()"
                               aria-hidden="true" disabled="disabled"/>
                        <button id="explain-trigger" class="query-action" type="button"
                                onclick="workbench.query.runExplain(null, 'explain-trigger')">
                            <xsl:call-template name="workbench-action-icon">
                                <xsl:with-param name="name">explain</xsl:with-param>
                                <xsl:with-param name="additional-class">query-action-icon</xsl:with-param>
                            </xsl:call-template>
                            <span><xsl:value-of select="$explain-query.label" /></span>
                        </button>
                        <span id="explain-trigger-spinner" class="query-explain-spinner"
                              aria-hidden="true"></span>
                        <span id="explain-trigger-cancel-action"
                              class="workbench-action workbench-action--danger query-explain-cancel"
                              data-workbench-action="cancel">
                            <xsl:call-template name="workbench-action-icon">
                                <xsl:with-param name="name">cancel</xsl:with-param>
                            </xsl:call-template>
                            <span class="workbench-action-label">
                                <input id="explain-trigger-cancel" class="query-explain-cancel"
                                       type="button" value="{$cancel.label}" onclick="workbench.query.cancelExplain()"
                                       aria-hidden="true" disabled="disabled"/>
                            </span>
                        </span>
                    </div>
                <div id="save-query-disclosure" class="query-disclosure query-save-disclosure">
                    <button id="save-query-toggle" class="query-disclosure__toggle" type="button"
                            aria-controls="save-query-panel" aria-expanded="false">
                        <svg class="query-action-icon" viewBox="0 0 24 24" focusable="false" aria-hidden="true">
                            <path d="M6 4h12v16l-6-3-6 3V4Z"></path>
                        </svg>
                        <span><xsl:value-of select="$save.label"/></span>
                        <xsl:call-template name="workbench-action-icon">
                            <xsl:with-param name="name">chevron</xsl:with-param>
                            <xsl:with-param name="additional-class">workbench-disclosure-chevron</xsl:with-param>
                        </xsl:call-template>
                    </button>
                    <div id="save-query-panel" class="query-disclosure__body query-disclosure__panel query-save-disclosure__body"
                         role="region" aria-labelledby="save-query-toggle" hidden="hidden">
                        <input id="save" type="submit" value="{$save.label}" disabled="disabled"/>
                        <label class="query-form__label" for="query-name">
                            <xsl:value-of select="$query-name.label"/>
                        </label>
                        <input id="query-name" name="query-name" type="text" size="32"
                               maxlength="32" value=""/>
                        <span class="query-option">
                            <input id="save-private" name="save-private" type="checkbox" value="true"/>
                            <label for="save-private">
                                <xsl:value-of select="$save-private.label"/>
                            </label>
                        </span>
                        <span id="save-feedback"></span>
                    </div>
                </div>
                <div id="query-options-disclosure" class="query-disclosure query-options-disclosure">
                    <button id="query-options-toggle" class="query-disclosure__toggle" type="button"
                            aria-controls="query-options-panel" aria-expanded="false">
                        <svg class="query-action-icon" viewBox="0 0 24 24" focusable="false" aria-hidden="true">
                            <path d="M4 7h16M4 12h16M4 17h16"></path>
                            <circle cx="9" cy="7" r="2"></circle>
                            <circle cx="15" cy="12" r="2"></circle>
                            <circle cx="11" cy="17" r="2"></circle>
                        </svg>
                        <span><xsl:value-of select="$query-options.label"/></span>
                        <xsl:call-template name="workbench-action-icon">
                            <xsl:with-param name="name">chevron</xsl:with-param>
                            <xsl:with-param name="additional-class">workbench-disclosure-chevron</xsl:with-param>
                        </xsl:call-template>
                    </button>
                    <div id="query-options-panel" class="query-disclosure__body query-disclosure__panel"
                         role="region" aria-labelledby="query-options-toggle" hidden="hidden">
                        <div class="query-settings">
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
                                <div class="query-form__field query-form__field--options">
                                    <span class="query-option">
                                        <input id="infer" name="infer" type="checkbox" value="true">
                                            <xsl:if
                                                    test="$info//sparql:binding[@name='default-infer']/sparql:literal = 'true'">
                                                <xsl:attribute name="checked">true</xsl:attribute>
                                            </xsl:if>
                                        </input>
                                        <label for="infer">
                                            <xsl:value-of select="$include-inferred.label"/>
                                        </label>
                                    </span>
                                </div>
                            </div>
                            <div class="query-disclosure__actions">
                                <input type="button" onclick="workbench.query.resetNamespaces()" value="Clear"/>
                            </div>
                        </div>
                    </div>
                </div>
                </div>
            </div>
        </form>
        <section id="query-results" class="query-results" aria-busy="false" hidden="hidden"
                 aria-labelledby="query-results-heading">
            <div class="query-results__header">
                <h2 id="query-results-heading">
                    <xsl:value-of select="$query-result.title"/>
                </h2>
                <button id="query-results-fullscreen" class="query-results__fullscreen" type="button"
                        aria-label="{$full-screen.label}" title="{$full-screen.label}" hidden="hidden"
                        aria-pressed="false" onclick="workbench.query.toggleResultsFullscreen()">
                    <svg class="query-results__fullscreen-icon" viewBox="0 0 24 24" focusable="false"
                         aria-hidden="true">
                        <path d="M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5"></path>
                    </svg>
                    <span class="query-results__fullscreen-label"><xsl:value-of select="$full-screen.label" /></span>
                </button>
            </div>
            <div id="query-results-loading" class="query-results__loading" hidden="hidden"
                 role="status" aria-live="polite">Loading query results...</div>
            <div id="query-results-status" class="query-results__status" role="status"
                 aria-live="polite"></div>
            <iframe id="query-results-frame" name="query-results-frame" class="query-results__frame"
                    title="Query results" hidden="hidden"></iframe>
        </section>
        <div id="query-diff-modal" class="query-diff-modal" aria-hidden="true">
            <div class="query-diff-modal__dialog" role="dialog" aria-modal="true"
                 aria-labelledby="query-diff-modal-title">
				<div class="query-diff-modal__header">
					<div id="query-diff-modal-title" class="query-diff-modal__title">
						<xsl:value-of select="$diff.label"/>
					</div>
					<span class="workbench-action workbench-action--secondary">
						<label class="workbench-action-hit-area">
							<xsl:call-template name="workbench-action-icon">
								<xsl:with-param name="name">close</xsl:with-param>
							</xsl:call-template>
							<span class="workbench-action-label">
								<input id="query-diff-close" type="button" value="{$close.label}"
									   onclick="workbench.query.closeDiffModal()" />
							</span>
						</label>
					</span>
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
        <script src="../../scripts/queryExplanationHighlighter.js" type="text/javascript"></script>
        <script src="../../scripts/query.js" type="text/javascript"></script>

    </xsl:template>
</xsl:stylesheet>
