<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
   <!ENTITY xsd  "http://www.w3.org/2001/XMLSchema#" >
 ]>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$repository-list.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:include href="table.xsl" />

	<xsl:template match="sparql:sparql">
		<section id="repositories-results" class="workbench-island workbench-responsive-records">
			<table class="data"><xsl:apply-templates select="*" /></table>
		</section>
	</xsl:template>

	<xsl:template match="sparql:variable[@name='readable']">
		<th>
			<xsl:call-template name="workbench-status-icon">
				<xsl:with-param name="status">readable</xsl:with-param>
				<xsl:with-param name="label" select="$readable.label" />
			</xsl:call-template>
		</th>
	</xsl:template>

	<xsl:template match="sparql:variable[@name='writeable']">
		<th>
			<xsl:call-template name="workbench-status-icon">
				<xsl:with-param name="status">writeable</xsl:with-param>
				<xsl:with-param name="label" select="$writeable.label" />
			</xsl:call-template>
		</th>
	</xsl:template>

	<xsl:template match="sparql:binding[@name='id']">
		<a href="../{sparql:literal}/">
			<xsl:value-of select="sparql:literal" />
		</a>
	</xsl:template>

	<xsl:template match="sparql:literal[@datatype = '&xsd;boolean']">
		<xsl:choose>
			<xsl:when test="text() = 'true'">
				<xsl:call-template name="workbench-status-icon">
					<xsl:with-param name="status">positive</xsl:with-param>
					<xsl:with-param name="label" select="$true.label" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="workbench-status-icon">
					<xsl:with-param name="status">negative</xsl:with-param>
					<xsl:with-param name="label" select="$false.label" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="sparql:literal">
		<xsl:value-of select="text()" />
	</xsl:template>

</xsl:stylesheet>
