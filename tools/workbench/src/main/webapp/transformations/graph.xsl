<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:workbench="https://rdf4j.org/schema/workbench#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$query-result.title" />
		<xsl:text> (</xsl:text>
		<xsl:value-of select="count(//sparql:result)" />
		<xsl:text>)</xsl:text>
	</xsl:variable>

	<xsl:include href="template.xsl" />

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
						<h2><xsl:value-of select="$title" /></h2>
						<button id="query-result-fullscreen" class="query-results__fullscreen" type="button"
							aria-label="{$full-screen.label}" title="{$full-screen.label}" aria-pressed="false"
							onclick="window.rdf4jQueryResultToggleFullscreen()">
							<svg class="query-results__fullscreen-icon" viewBox="0 0 24 24" focusable="false"
								aria-hidden="true">
								<path d="M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5"></path>
							</svg>
							<span class="query-results__fullscreen-label"><xsl:value-of select="$full-screen.label" /></span>
						</button>
					</div>
					<div class="query-result-toolbar__disclosures">
						<div id="query-result-download-disclosure" class="query-result-disclosure">
							<button id="query-result-download-toggle" class="query-disclosure__toggle" type="button"
								aria-controls="query-result-download-panel" aria-expanded="false">
								<svg class="query-action-icon" viewBox="0 0 24 24" focusable="false" aria-hidden="true">
									<path d="M12 4v12M7 11l5 5 5-5M5 20h14"></path>
								</svg>
								<span><xsl:value-of select="$download.label" /></span>
							</button>
							<div id="query-result-download-panel" class="query-disclosure__panel" role="region"
								aria-labelledby="query-result-download-toggle" hidden="hidden">
								<xsl:call-template name="graph-download-controls" />
							</div>
						</div>
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
							</button>
							<div id="query-result-options-panel" class="query-disclosure__panel" role="region"
								aria-labelledby="query-result-options-toggle" hidden="hidden">
								<xsl:call-template name="graph-result-options" />
							</div>
						</div>
					</div>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="graph-download-controls" />
				<xsl:call-template name="graph-result-options" />
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
						<xsl:call-template name="graph-records" />
					</div>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<table class="data">
					<xsl:apply-templates select="sparql:head | sparql:results" />
				</table>
			</xsl:otherwise>
		</xsl:choose>
		<script src="../../scripts/paging.js" type="text/javascript">
		</script>
	</xsl:template>

	<xsl:template name="graph-download-controls">
		<form>
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$download-format.label" />
						</th>
						<td>
							<select id="Accept" name="Accept">
								<xsl:for-each
									select="$info//sparql:binding[@name='graph-download-format']">
									<option value="{substring-before(sparql:literal, ' ')}">
										<xsl:if
											test="$info//sparql:binding[@name='default-Accept']/sparql:literal = substring-before(sparql:literal, ' ')">
											<xsl:attribute name="selected">true</xsl:attribute>
										</xsl:if>
										<xsl:value-of select="substring-after(sparql:literal, ' ')" />
									</option>
								</xsl:for-each>
						</select>
					</td>
					<td>
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
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$download-limit.label" />
						</th>
						<td>
							<xsl:call-template name="limit-select">
								<xsl:with-param name="limit_id">download_limit</xsl:with-param>
							</xsl:call-template>
						</td>
						<td></td>
					</tr>
				</tbody>
			</table>
		</form>
	</xsl:template>

	<xsl:template name="graph-result-options">
		<form>
			<table class="dataentry">
				<tbody>
					<xsl:if test="/sparql:sparql/workbench:metadata/workbench:embedded = 'true'">
						<tr>
							<th><label for="result-layout"><xsl:value-of select="$result-layout.label" /></label></th>
							<td>
								<select id="result-layout" name="result-layout">
									<option value="auto" selected="selected"><xsl:value-of select="$result-layout-auto.label" /></option>
									<option value="table"><xsl:value-of select="$result-layout-table.label" /></option>
									<option value="records"><xsl:value-of select="$result-layout-records.label" /></option>
								</select>
							</td>
						</tr>
					</xsl:if>
					<tr>
						<th>
							<xsl:value-of select="$result-limit.label" />
						</th>
						<td>
							<xsl:call-template name="limit-select">
								<xsl:with-param name="onchange">
									workbench.paging.addGraphParam('limit_query');
								</xsl:with-param>
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
					<xsl:if test="/sparql:sparql/workbench:metadata/workbench:embedded = 'true'">
						<tr>
							<th><label for="result-wrap-values"><xsl:value-of select="$result-wrap.label" /></label></th>
							<td><input id="result-wrap-values" type="checkbox" name="wrap-values" value="true"
								checked="checked" /></td>
						</tr>
					</xsl:if>
				</tbody>
			</table>
		</form>
	</xsl:template>

	<xsl:template name="graph-records">
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
