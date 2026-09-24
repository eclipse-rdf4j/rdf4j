<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$repository-delete.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql">
		<form id="delete-form" class="workbench-island" action="delete" method="post" onsubmit="checkIsSafeToDelete(event);">
			<div class="workbench-field">
				<label for="id"><xsl:value-of select="$repository.label" /></label>
				<select id="id" name="id">
					<option value=""></option>
					<xsl:for-each select="//sparql:result[sparql:binding[@name='id']/sparql:literal/text()!='SYSTEM']">
						<option value="{sparql:binding[@name='id']/sparql:literal}">
							<xsl:value-of select="sparql:binding[@name='id']/sparql:literal" />
							-
							<xsl:value-of select="sparql:binding[@name='description']/sparql:literal" />
						</option>
					</xsl:for-each>
				</select>
			</div>
			<div id="delete-actions" class="workbench-form-actions">
				<span class="workbench-action workbench-action--danger">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">delete</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label"><input type="submit" value="{$delete.label}" /></span>
					</label>
				</span>
			</div>
			<span id="delete-feedback" class="error" role="alert"></span>
		</form>
		<script src="../../scripts/delete.js" type="text/javascript">
		</script>
	</xsl:template>

</xsl:stylesheet>
