<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:workbench="https://rdf4j.org/schema/workbench#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$query-result.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<!-- Keep metadata out of the visible result; the root template's built-in
	     processing would otherwise emit every metadata value as text. -->
	<xsl:template match="sparql:sparql">
		<xsl:apply-templates select="sparql:boolean" />
	</xsl:template>

	<xsl:template match="sparql:boolean">
		<xsl:if test="/sparql:sparql/workbench:metadata/workbench:embedded = 'true'">
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
			</div>
		</xsl:if>
		<div class="queryResult">
			<xsl:choose>
				<xsl:when test="text() = 'true'">
					<xsl:call-template name="workbench-status-icon">
						<xsl:with-param name="status">positive</xsl:with-param>
						<xsl:with-param name="label" select="$true.label" />
					</xsl:call-template>
					<xsl:value-of select="$true.label" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="workbench-status-icon">
						<xsl:with-param name="status">negative</xsl:with-param>
						<xsl:with-param name="label" select="$false.label" />
					</xsl:call-template>
					<xsl:value-of select="$false.label" />
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>

</xsl:stylesheet>
