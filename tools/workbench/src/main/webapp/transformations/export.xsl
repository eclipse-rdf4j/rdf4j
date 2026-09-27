<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:workbench="https://rdf4j.org/schema/workbench#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$export.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:include href="table.xsl" />

	<xsl:template match="sparql:sparql">
		<xsl:variable name="preview-limit">
			<xsl:choose>
				<xsl:when test="/sparql:sparql/workbench:metadata/workbench:statement-preview-limit">
					<xsl:value-of select="/sparql:sparql/workbench:metadata/workbench:statement-preview-limit" />
				</xsl:when>
				<xsl:otherwise>100</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<form id="export-form" class="workbench-island" action="export">
			<div class="workbench-form-grid">
				<div class="workbench-field">
					<label for="Accept"><xsl:value-of select="$download-format.label" /></label>
					<div class="workbench-select-control">
						<select id="Accept" name="Accept">
							<xsl:for-each select="$info//sparql:binding[@name='graph-download-format']">
								<option value="{substring-before(sparql:literal, ' ')}">
									<xsl:if test="$info//sparql:binding[@name='default-export-format']/sparql:literal = substring-before(sparql:literal, ' ')">
										<xsl:attribute name="selected">selected</xsl:attribute>
									</xsl:if>
									<xsl:value-of select="substring-after(sparql:literal, ' ')" />
								</option>
							</xsl:for-each>
						</select>
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">chevron</xsl:with-param>
							<xsl:with-param name="additional-class">workbench-select-chevron</xsl:with-param>
						</xsl:call-template>
					</div>
				</div>
				<div class="workbench-field">
					<label for="compression"><xsl:value-of select="$compression.label" /></label>
					<div class="workbench-select-control">
						<select id="compression" name="compression">
							<option value="none"><xsl:value-of select="$none.label" /></option>
							<option value="gzip" selected="selected"><xsl:value-of select="$gzip.label" /></option>
							<option value="zip"><xsl:value-of select="$zip.label" /></option>
						</select>
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">chevron</xsl:with-param>
							<xsl:with-param name="additional-class">workbench-select-chevron</xsl:with-param>
						</xsl:call-template>
					</div>
				</div>
				<div class="workbench-field">
					<label for="timeout"><xsl:value-of select="$export-timeout.label" /></label>
					<input id="timeout" name="timeout" type="number" min="0" step="1" required="required">
						<xsl:attribute name="value">
							<xsl:choose>
								<xsl:when test="/sparql:sparql/workbench:metadata/workbench:export-timeout">
									<xsl:value-of select="/sparql:sparql/workbench:metadata/workbench:export-timeout" />
								</xsl:when>
								<xsl:otherwise>43200</xsl:otherwise>
							</xsl:choose>
						</xsl:attribute>
					</input>
					<span class="hint"><xsl:value-of select="$export-timeout.desc" /></span>
				</div>
			</div>
			<details id="export-result-options" class="workbench-options">
				<summary>
					<span><xsl:value-of select="$result-options.label" /></span>
					<xsl:call-template name="workbench-action-icon">
						<xsl:with-param name="name">chevron</xsl:with-param>
						<xsl:with-param name="additional-class">workbench-disclosure-chevron</xsl:with-param>
					</xsl:call-template>
				</summary>
				<div class="workbench-options__body">
					<div class="workbench-field">
						<label for="limit_export"><xsl:value-of select="$result-limit.label" /></label>
						<xsl:call-template name="limit-select">
							<xsl:with-param name="limit_id">limit_export</xsl:with-param>
							<xsl:with-param name="limit_default" select="$preview-limit" />
						</xsl:call-template>
						<span id="result-limited">
							<xsl:if test="/sparql:sparql/workbench:metadata/workbench:statement-preview-requested = 'true' and $preview-limit != '0' and number($preview-limit) = count(sparql:results/sparql:result)">
								<xsl:value-of select="$result-limited.desc" />
							</xsl:if>
						</span>
						<span class="hint"><xsl:value-of select="$export-preview-limit.desc" /></span>
					</div>
				</div>
			</details>
			<div class="workbench-form-actions">
				<span class="workbench-action workbench-action--primary">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">download</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label">
							<button type="submit" name="action" value="download"><xsl:value-of select="$download.label" /></button>
						</span>
					</label>
				</span>
			</div>
		</form>
		<section id="export-results" class="workbench-island workbench-responsive-records">
			<p class="workbench-page-meta"><xsl:value-of select="$export-preview-hint.desc" /></p>
			<div class="workbench-form-actions">
				<span class="workbench-action workbench-action--secondary">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">refresh</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label">
							<button type="submit" form="export-form" name="action" value="preview">
								<xsl:value-of select="$retrieve-statements.label" />
							</button>
						</span>
					</label>
				</span>
			</div>
			<xsl:if test="not(sparql:results/sparql:result)">
				<p class="workbench-empty" role="status">
					<xsl:choose>
						<xsl:when test="/sparql:sparql/workbench:metadata/workbench:statement-preview-requested = 'true'">
							<xsl:value-of select="$no-results.label" />
						</xsl:when>
						<xsl:otherwise><xsl:value-of select="$export-preview-empty.desc" /></xsl:otherwise>
					</xsl:choose>
				</p>
			</xsl:if>
			<table class="data">
				<xsl:apply-templates select="sparql:head | sparql:results" />
			</table>
		</section>
		<script src="../../scripts/paging.js" type="text/javascript">  </script>
		<script src="../../scripts/export.js" type="text/javascript">  </script>
	</xsl:template>

</xsl:stylesheet>
