<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:workbench="https://rdf4j.org/schema/workbench#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:include href="template.xsl" />

	<xsl:variable name="result-layout-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-layout' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-wrap-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-wrap' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-totals-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-totals' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-paging-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-paging' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-page-size-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-page-size' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-page-previous-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-page-previous' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-page-next-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-page-next' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-download-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-download' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-download-format-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-download-format' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-download-format-tuple-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-download-format-tuple' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-download-limit-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-download-limit' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-show-datatypes-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-show-datatypes' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-fullscreen-disabled"
		select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = 'result-fullscreen' and sparql:binding[@name='query-feature-enabled']/sparql:literal = 'false']" />
	<xsl:variable name="result-download-format-visible"
		select="not($result-download-format-disabled) and not($result-download-format-tuple-disabled)" />
	<xsl:variable name="result-download-controls-visible"
		select="not($result-download-disabled) or $result-download-format-visible or not($result-download-limit-disabled)" />
	<xsl:variable name="result-options-visible"
		select="not($result-layout-disabled) or not($result-wrap-disabled) or not($result-page-size-disabled) or not($result-show-datatypes-disabled) or (not($result-paging-disabled) and (not($result-page-previous-disabled) or not($result-page-next-disabled)))" />

	<xsl:variable name="title">
		<xsl:value-of select="$query-result.title" />
		<xsl:if test="not($result-totals-disabled)">
			<xsl:text> (</xsl:text>
			<xsl:value-of select="count(//sparql:result)" />
			<xsl:text>)</xsl:text>
		</xsl:if>
	</xsl:variable>

	<xsl:variable name="nextX.label">
		<xsl:value-of select="$next.label" />
		<xsl:if test="not($result-totals-disabled)">
			<xsl:text> </xsl:text>
			<xsl:value-of select="count(//sparql:result)" />
		</xsl:if>
	</xsl:variable>

	<xsl:variable name="previousX.label">
		<xsl:value-of select="$previous.label" />
		<xsl:if test="not($result-totals-disabled)">
			<xsl:text> </xsl:text>
			<xsl:value-of select="count(//sparql:result)" />
		</xsl:if>
	</xsl:variable>

	<xsl:include href="table.xsl" />

	<xsl:template match="sparql:sparql">
		<xsl:if test="/sparql:sparql/workbench:metadata/workbench:total-result-count">
			<input type="hidden" id="workbench-total-result-count"
				value="{/sparql:sparql/workbench:metadata/workbench:total-result-count}" />
		</xsl:if>
		<xsl:if test="/sparql:sparql/workbench:metadata/workbench:query-text">
			<textarea id="wb-query-text" style="display:none;"><xsl:value-of
				select="/sparql:sparql/workbench:metadata/workbench:query-text" /></textarea>
		</xsl:if>
		<xsl:choose>
			<xsl:when test="/sparql:sparql/workbench:metadata/workbench:embedded = 'true'">
				<div class="query-result-toolbar">
					<div id="query-result-embedded-header" class="query-result-toolbar__header">
						<h2 id="title_heading"><xsl:value-of select="$title" /></h2>
							<xsl:if test="not($result-fullscreen-disabled)">
							<button id="query-result-fullscreen" class="query-results__fullscreen" type="button"
							aria-label="{$full-screen.label}" title="{$full-screen.label}" aria-pressed="false"
							onclick="window.rdf4jQueryResultToggleFullscreen()">
							<svg class="query-results__fullscreen-icon" viewBox="0 0 24 24" focusable="false"
								aria-hidden="true">
								<path d="M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5"></path>
							</svg>
							<span class="query-results__fullscreen-label"><xsl:value-of select="$full-screen.label" /></span>
							</button>
							</xsl:if>
						</div>
                    <div class="query-result-toolbar__disclosures">
	                        <xsl:if test="$result-download-controls-visible">
	                        <div id="query-result-download-disclosure" class="query-result-disclosure">
                            <button id="query-result-download-toggle" class="query-disclosure__toggle" type="button"
                                    aria-controls="query-result-download-panel" aria-expanded="false">
                                <svg class="query-action-icon" viewBox="0 0 24 24" focusable="false" aria-hidden="true">
                                    <path d="M12 4v12M7 11l5 5 5-5M5 20h14"></path>
                                </svg>
                                <span><xsl:value-of select="$download.label" /></span>
                                <xsl:call-template name="workbench-action-icon">
                                    <xsl:with-param name="name">chevron</xsl:with-param>
                                    <xsl:with-param name="additional-class">workbench-disclosure-chevron</xsl:with-param>
                                </xsl:call-template>
                            </button>
                        </div>
	                        </xsl:if>
	                        <xsl:if test="$result-options-visible">
	                        <div id="query-result-options-disclosure" class="query-result-disclosure">
                            <button id="query-result-options-toggle" class="query-disclosure__toggle" type="button"
                                    aria-controls="query-result-options-panel" aria-expanded="false">
                                <svg class="query-action-icon" viewBox="0 0 24 24" focusable="false" aria-hidden="true">
									<path d="M4 7h16M4 12h16M4 17h16"></path>
									<circle cx="9" cy="7" r="2"></circle>
									<circle cx="15" cy="12" r="2"></circle>
									<circle cx="11" cy="17" r="2"></circle>
                                </svg>
                                <span><xsl:value-of select="$result-options.label" /></span>
                                <xsl:call-template name="workbench-action-icon">
                                    <xsl:with-param name="name">chevron</xsl:with-param>
                                    <xsl:with-param name="additional-class">workbench-disclosure-chevron</xsl:with-param>
                                </xsl:call-template>
                            </button>
	                        </div>
	                        </xsl:if>
                    </div>
				</div>
				<div class="query-result-disclosure-panels">
					<xsl:if test="$result-download-controls-visible">
					<div id="query-result-download-panel" class="query-disclosure__panel" role="region"
						aria-labelledby="query-result-download-toggle" hidden="hidden">
						<xsl:call-template name="tuple-download-controls" />
					</div>
					</xsl:if>
					<xsl:if test="$result-options-visible">
					<div id="query-result-options-panel" class="query-disclosure__panel" role="region"
						aria-labelledby="query-result-options-toggle" hidden="hidden">
						<xsl:call-template name="tuple-result-options" />
					</div>
					</xsl:if>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="$result-download-controls-visible">
					<xsl:call-template name="tuple-download-controls" />
				</xsl:if>
				<xsl:if test="$result-options-visible">
					<xsl:call-template name="tuple-result-options">
						<xsl:with-param name="includeNavigation" select="true()" />
					</xsl:call-template>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:choose>
			<xsl:when test="/sparql:sparql/workbench:metadata/workbench:embedded = 'true'">
				<div id="query-result-layout" class="query-result-layout" data-layout="auto" data-wrap="true">
					<div id="query-result-table-wrap" class="query-result-table-wrap">
						<table class="data">
							<xsl:apply-templates select="sparql:head | sparql:results" />
						</table>
					</div>
					<div id="query-result-records" class="query-result-records" hidden="hidden">
						<xsl:call-template name="tuple-records" />
					</div>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<table class="data">
					<xsl:apply-templates select="sparql:head | sparql:results" />
				</table>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="/sparql:sparql/workbench:metadata/workbench:embedded = 'true' and not($result-paging-disabled) and (not($result-page-previous-disabled) or not($result-page-next-disabled))">
			<div class="query-result-navigation" role="group" aria-label="{$result-offset.label}">
				<xsl:if test="not($result-page-previous-disabled)">
				<span class="workbench-action workbench-action--secondary" data-workbench-action="previous">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">previous</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label"><input id="previousX" type="button" value="{$previousX.label}"
							onclick="workbench.paging.previousOffset('query');" /></span>
					</label>
					</span>
				</xsl:if>
				<xsl:if test="not($result-page-next-disabled)">
				<span class="workbench-action workbench-action--secondary" data-workbench-action="next">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">next</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label"><input id="nextX" type="button" value="{$nextX.label}"
							onclick="workbench.paging.nextOffset('query');" /></span>
					</label>
					</span>
				</xsl:if>
			</div>
		</xsl:if>
		<script src="../../scripts/paging.js" type="text/javascript">
		</script>
		<script src="../../scripts/tuple.js" type="text/javascript">
		</script>
	</xsl:template>

	<xsl:template name="tuple-download-controls">
		<form>
			<xsl:choose>
				<xsl:when test="/sparql:sparql/workbench:metadata/workbench:embedded = 'true'">
					<div class="query-result-fields query-result-download-fields">
						<xsl:if test="$result-download-format-visible">
						<div class="query-result-field">
							<label for="Accept"><xsl:value-of select="$download-format.label" /></label>
							<xsl:call-template name="tuple-download-format-select" />
						</div>
						</xsl:if>
						<xsl:if test="not($result-download-format-visible) and not($result-download-disabled)">
							<xsl:call-template name="tuple-download-format-select">
								<xsl:with-param name="hidden" select="true()" />
							</xsl:call-template>
						</xsl:if>
						<xsl:if test="not($result-download-limit-disabled)">
						<div class="query-result-field">
							<label for="download_limit"><xsl:value-of select="$download-limit.label" /></label>
			<xsl:call-template name="limit-select">
				<xsl:with-param name="limit_id">download_limit</xsl:with-param>
				<xsl:with-param name="limit_default"
					select="$info//sparql:binding[@name='default-download-limit']/sparql:literal/text()" />
			</xsl:call-template>
						</div>
						</xsl:if>
						<xsl:if test="not($result-download-disabled)">
						<div class="query-result-download-action">
							<span class="workbench-action workbench-action--secondary" data-workbench-action="download">
								<label class="workbench-action-hit-area">
									<xsl:call-template name="workbench-action-icon">
										<xsl:with-param name="name">download</xsl:with-param>
									</xsl:call-template>
									<span class="workbench-action-label"><input type="submit"
										onclick="workbench.paging.addGraphParam('Accept');return false"
										value="{$download.label}" /></span>
								</label>
							</span>
						</div>
						</xsl:if>
					</div>
				</xsl:when>
				<xsl:otherwise>
					<table class="dataentry query-result-controls">
						<tbody>
							<xsl:if test="$result-download-format-visible or not($result-download-disabled)">
							<tr>
								<xsl:if test="$result-download-format-visible">
								<th>
									<xsl:value-of select="$download-format.label" />
								</th>
								<td>
									<xsl:call-template name="tuple-download-format-select" />
								</td>
								</xsl:if>
								<xsl:if test="not($result-download-disabled)">
								<td class="query-result-download-action">
									<xsl:if test="not($result-download-format-visible)">
										<xsl:call-template name="tuple-download-format-select">
											<xsl:with-param name="hidden" select="true()" />
										</xsl:call-template>
									</xsl:if>
									<span class="workbench-action workbench-action--secondary" data-workbench-action="download">
										<label class="workbench-action-hit-area">
											<xsl:call-template name="workbench-action-icon">
												<xsl:with-param name="name">download</xsl:with-param>
											</xsl:call-template>
											<span class="workbench-action-label"><input type="submit"
												onclick="workbench.paging.addGraphParam('Accept');return false"
												value="{$download.label}" /></span>
										</label>
										</span>
								</td>
								</xsl:if>
							</tr>
							</xsl:if>
							<xsl:if test="not($result-download-limit-disabled)">
							<tr>
								<th>
									<xsl:value-of select="$download-limit.label" />
								</th>
								<td>
					<xsl:call-template name="limit-select">
						<xsl:with-param name="limit_id">download_limit</xsl:with-param>
						<xsl:with-param name="limit_default"
							select="$info//sparql:binding[@name='default-download-limit']/sparql:literal/text()" />
					</xsl:call-template>
								</td>
								<td></td>
							</tr>
							</xsl:if>
						</tbody>
					</table>
				</xsl:otherwise>
			</xsl:choose>
		</form>
	</xsl:template>

	<xsl:template name="tuple-download-format-select">
		<xsl:param name="hidden" select="false()" />
		<select id="Accept" name="Accept">
			<xsl:if test="$hidden">
				<xsl:attribute name="hidden">hidden</xsl:attribute>
			</xsl:if>
			<xsl:for-each select="$info//sparql:binding[@name='tuple-download-format']">
				<option value="{substring-before(sparql:literal, ' ')}">
					<xsl:if test="$info//sparql:binding[@name='default-Accept']/sparql:literal = substring-before(sparql:literal, ' ')">
						<xsl:attribute name="selected">true</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="substring-after(sparql:literal, ' ')" />
				</option>
			</xsl:for-each>
		</select>
	</xsl:template>

	<xsl:template name="tuple-result-options">
		<xsl:param name="includeNavigation" select="false()" />
		<form>
			<xsl:choose>
				<xsl:when test="/sparql:sparql/workbench:metadata/workbench:embedded = 'true'">
					<div class="query-result-fields query-result-option-fields">
						<xsl:if test="not($result-layout-disabled)">
						<div class="query-result-field">
							<label for="result-layout"><xsl:value-of select="$result-layout.label" /></label>
							<select id="result-layout" name="result-layout">
								<option value="auto" selected="selected"><xsl:value-of select="$result-layout-auto.label" /></option>
								<option value="table"><xsl:value-of select="$result-layout-table.label" /></option>
								<option value="records"><xsl:value-of select="$result-layout-records.label" /></option>
							</select>
						</div>
						</xsl:if>
						<xsl:if test="not($result-page-size-disabled)">
						<div class="query-result-field">
							<label for="limit_query"><xsl:value-of select="$result-limit.label" /></label>
							<xsl:call-template name="limit-select">
								<xsl:with-param name="onchange">workbench.paging.addLimit('query');</xsl:with-param>
								<xsl:with-param name="limit_id">limit_query</xsl:with-param>
							</xsl:call-template>
							<span id="result-limited">
								<xsl:if
									test="$info//sparql:binding[@name='default-limit']/sparql:literal = count(//sparql:result)">
									<xsl:value-of select="$result-limited.desc" />
								</xsl:if>
							</span>
						</div>
						</xsl:if>
						<xsl:if test="not($result-wrap-disabled)">
						<label class="query-result-check" for="result-wrap-values">
							<input id="result-wrap-values" type="checkbox" name="wrap-values" value="true"
								checked="checked" />
							<span><xsl:value-of select="$result-wrap.label" /></span>
						</label>
						</xsl:if>
						<xsl:if test="not($result-show-datatypes-disabled)">
						<label class="query-result-check" for="show-datatypes">
							<input id="show-datatypes" type="checkbox" name="show-datatypes" value="show-dataypes"
								checked="checked" />
							<span><xsl:value-of select="$show-datatypes.label" /></span>
						</label>
						</xsl:if>
					</div>
				</xsl:when>
				<xsl:otherwise>
					<table class="dataentry query-result-controls">
						<tbody>
							<xsl:if test="not($result-page-size-disabled)">
							<tr>
								<th><xsl:value-of select="$result-limit.label" /></th>
								<td>
									<xsl:call-template name="limit-select">
										<xsl:with-param name="onchange">workbench.paging.addLimit('query');</xsl:with-param>
										<xsl:with-param name="limit_id">limit_query</xsl:with-param>
									</xsl:call-template>
								</td>
								<td id="result-limited">
									<xsl:if
										test="$info//sparql:binding[@name='default-limit']/sparql:literal = count(//sparql:result)">
										<xsl:value-of select="$result-limited.desc" />
									</xsl:if>
								</td>
							</tr>
							</xsl:if>
							<xsl:if test="$includeNavigation and not($result-paging-disabled) and (not($result-page-previous-disabled) or not($result-page-next-disabled))">
								<tr>
									<th><xsl:value-of select="$result-offset.label" /></th>
									<xsl:if test="not($result-page-previous-disabled)">
									<td><input id="previousX" type="button" value="{$previousX.label}"
										onclick="workbench.paging.previousOffset('query');" /></td>
									</xsl:if>
									<xsl:if test="not($result-page-next-disabled)">
									<td><input id="nextX" type="button" value="{$nextX.label}"
										onclick="workbench.paging.nextOffset('query');" /></td>
									</xsl:if>
								</tr>
							</xsl:if>
							<xsl:if test="not($result-show-datatypes-disabled)">
							<tr>
								<th><xsl:value-of select="$show-datatypes.label" /></th>
								<td><input id="show-datatypes" type="checkbox" name="show-datatypes" value="show-dataypes"
									checked="checked" /></td>
							</tr>
							</xsl:if>
						</tbody>
					</table>
				</xsl:otherwise>
			</xsl:choose>
		</form>
	</xsl:template>

	<xsl:template name="tuple-records">
		<xsl:for-each select="sparql:results/sparql:result">
			<article class="query-result-record">
				<h3 class="query-result-record__title">
					<xsl:text>Record </xsl:text><xsl:value-of select="position()" />
				</h3>
				<dl class="query-result-record__fields">
					<xsl:variable name="result" select="." />
					<xsl:for-each select="../../sparql:head/sparql:variable">
						<xsl:variable name="name" select="@name" />
						<dt><xsl:value-of select="$name" /></dt>
						<dd>
							<xsl:choose>
								<xsl:when test="$result/sparql:binding[@name=$name]">
									<xsl:apply-templates select="$result/sparql:binding[@name=$name]" />
								</xsl:when>
								<xsl:otherwise><span class="query-result-unbound">&#x2014;</span></xsl:otherwise>
							</xsl:choose>
						</dd>
					</xsl:for-each>
				</dl>
			</article>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="sparql:head">
		<thead>
			<tr>
				<xsl:apply-templates select="sparql:variable" />
			</tr>
		</thead>
	</xsl:template>

	<xsl:template match="sparql:results">
		<tbody>
			<xsl:apply-templates select="sparql:result" />
		</tbody>
	</xsl:template>

	<xsl:template match="sparql:result">
		<xsl:variable name="result" select="." />
		<tr>
			<xsl:for-each select="../../sparql:head/sparql:variable">
				<xsl:variable name="name" select="@name" />
				<td>
					<xsl:apply-templates select="$result/sparql:binding[@name=$name]" />
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>

</xsl:stylesheet>
