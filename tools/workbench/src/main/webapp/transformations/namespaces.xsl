<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$namespaces.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:include href="table.xsl" />

	<xsl:template match="sparql:literal">
		<xsl:value-of select="." />
	</xsl:template>

	<xsl:template match="sparql:sparql">
		<form id="namespaces-form" class="workbench-island" action="namespaces" method="post">
			<div class="workbench-form-grid">
				<div class="workbench-field">
					<label for="prefix"><xsl:value-of select="$prefix.label" /></label>
					<div class="workbench-inline-controls">
						<input type="text" id="prefix" name="prefix" size="8" />
						<select id="prefix-select" onchange="workbench.namespaces.updatePrefix()">
							<option></option>
							<xsl:for-each select="//sparql:result">
								<option value="{sparql:binding[@name='namespace']/sparql:literal}">
									<xsl:value-of select="sparql:binding[@name='prefix']/sparql:literal" />
								</option>
							</xsl:for-each>
						</select>
					</div>
				</div>
				<div class="workbench-field">
					<label for="namespace"><xsl:value-of select="$namespace.label" /></label>
					<input type="text" id="namespace" name="namespace" size="48" />
				</div>
			</div>
			<div class="workbench-form-actions">
				<span class="workbench-action workbench-action--primary">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">update</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label"><input type="submit" value="{$update.label}" /></span>
					</label>
				</span>
				<span class="workbench-action workbench-action--danger">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">delete</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label"><input type="submit" onclick="$('#namespace').val('');return true"
							value="{$delete.label}" /></span>
					</label>
				</span>
			</div>
		</form>
		<section id="namespaces-results" class="workbench-island workbench-responsive-records">
			<xsl:if test="not(sparql:results/sparql:result)">
				<p class="workbench-empty" role="status"><xsl:value-of select="$no-results.label" /></p>
			</xsl:if>
			<table class="data"><xsl:apply-templates select="*" /></table>
		</section>
		<script src="../../scripts/namespaces.js" type="text/javascript">
		</script>
	</xsl:template>

</xsl:stylesheet>
