<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$export.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:include href="table.xsl" />

	<xsl:template match="sparql:sparql">
		<form id="export-download-form" class="workbench-island" action="export">
			<div class="workbench-form-grid">
				<div class="workbench-field">
					<label for="Accept"><xsl:value-of select="$download-format.label" /></label>
					<select id="Accept" name="Accept">
						<xsl:for-each select="$info//sparql:binding[@name='graph-download-format']">
							<option value="{substring-before(sparql:literal, ' ')}">
								<xsl:if test="$info//sparql:binding[@name='default-Accept']/sparql:literal = substring-before(sparql:literal, ' ')">
									<xsl:attribute name="selected">true</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="substring-after(sparql:literal, ' ')" />
							</option>
						</xsl:for-each>
					</select>
				</div>
			</div>
			<div class="workbench-form-actions">
				<span class="workbench-action workbench-action--primary">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">download</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label"><input type="submit" value="{$download.label}" /></span>
					</label>
				</span>
			</div>
		</form>
		<details id="export-result-options" class="workbench-island workbench-options">
			<summary><xsl:value-of select="$result-options.label" /></summary>
			<div class="workbench-options__body">
			<form id="export-result-options-form" action="export">
			<div class="workbench-field">
				<label for="limit_explore"><xsl:value-of select="$result-limit.label" /></label>
				<xsl:call-template name="limit-select">
					<xsl:with-param name="onchange">this.form.submit();</xsl:with-param>
					<xsl:with-param name="limit_id">limit_explore</xsl:with-param>
				</xsl:call-template>
				<span id="result-limited">
					<xsl:if test="$info//sparql:binding[@name='default-limit']/sparql:literal = count(//sparql:result)">
						<xsl:value-of select="$result-limited.desc" />
					</xsl:if>
				</span>
			</div>
			</form>
			</div>
		</details>
		<section id="export-results" class="workbench-island workbench-responsive-records">
			<xsl:if test="not(sparql:results/sparql:result)">
				<p class="workbench-empty" role="status"><xsl:value-of select="$no-results.label" /></p>
			</xsl:if>
			<table class="data">
				<xsl:apply-templates select="*" />
			</table>
		</section>
        <script src="../../scripts/paging.js" type="text/javascript">  </script>
        <script src="../../scripts/export.js" type="text/javascript">  </script>
	</xsl:template>

</xsl:stylesheet>
