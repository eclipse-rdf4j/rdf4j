<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	 xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title"><xsl:value-of select="$contexts.title"/></xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:include href="table.xsl" />

	<xsl:template match="sparql:sparql">
		<section id="contexts-results" class="workbench-island workbench-responsive-records">
			<xsl:if test="not(sparql:results/sparql:result)">
				<p class="workbench-empty" role="status"><xsl:value-of select="$no-results.label" /></p>
			</xsl:if>
			<table class="data"><xsl:apply-templates select="*" /></table>
		</section>
	</xsl:template>

</xsl:stylesheet>
