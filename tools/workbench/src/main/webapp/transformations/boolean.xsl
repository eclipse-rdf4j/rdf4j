<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:workbench="https://rdf4j.org/schema/workbench#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$query-result.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<!-- Matched explicitly so that workbench metadata is not copied out as text by
	     the built-in template rules. -->
	<xsl:template match="sparql:sparql">
		<xsl:call-template name="query-duration" />
		<xsl:apply-templates select="sparql:boolean" />
	</xsl:template>

	<xsl:template match="sparql:boolean">
		<div class="queryResult">
			<xsl:choose>
				<xsl:when test="text() = 'true'">
					<img src="../../images/affirmative.png"
						alt="{$true.label}" title="{$true.label}" />
					<xsl:value-of select="$true.label" />
				</xsl:when>
				<xsl:otherwise>
					<img src="../../images/negative.png"
						alt="{$false.label}" title="{$false.label}" />
					<xsl:value-of select="$false.label" />
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>

</xsl:stylesheet>
