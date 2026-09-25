<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$add.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />
	<xsl:variable name="selectedIsolation"
		select="//sparql:binding[@name='transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel']/sparql:literal" />

	<xsl:template match="sparql:sparql">
		<xsl:if
			test="$info//sparql:binding[@name='id']/sparql:literal/text() = 'SYSTEM'">
			<p class="WARN">
				<xsl:value-of select="$SYSTEM-warning.desc" />
			</p>
		</xsl:if>
		<xsl:if test="//sparql:binding[@name='error-message']">
			<p class="error" role="alert">
				<xsl:value-of select="//sparql:binding[@name='error-message']" />
			</p>
		</xsl:if>
		<form method="post" action="add" enctype="multipart/form-data">
			<fieldset id="add-source-tabs" class="workbench-source-tabs">
				<legend>Source</legend>
				<label for="source-file">
					<xsl:call-template name="workbench-action-icon">
						<xsl:with-param name="name">source-file</xsl:with-param>
					</xsl:call-template>
					<input type="radio" id="source-file" name="source" value="file" checked="checked"
						onchange="workbench.add.enabledInput('file')" />
					<span><xsl:value-of select="$upload-file.short" /></span>
				</label>
				<label for="source-url">
					<xsl:call-template name="workbench-action-icon">
						<xsl:with-param name="name">source-url</xsl:with-param>
					</xsl:call-template>
					<input type="radio" id="source-url" name="source" value="url"
						onchange="workbench.add.enabledInput('url')" />
					<span><xsl:value-of select="$upload-url.short" /></span>
				</label>
				<label for="source-text">
					<xsl:call-template name="workbench-action-icon">
						<xsl:with-param name="name">source-text</xsl:with-param>
					</xsl:call-template>
					<input type="radio" id="source-text" name="source" value="contents"
						onchange="workbench.add.enabledInput('text')" />
					<span><xsl:value-of select="$upload-text.short" /></span>
				</label>
			</fieldset>
			<div class="workbench-form-grid add-source-fields">
				<div id="add-source-file-panel" class="workbench-field add-source-panel" data-source="file">
					<label for="file"><xsl:value-of select="$upload-file.label" /></label>
					<input type="file" id="file" name="content"
						onchange="workbench.add.enabledInput('file')" />
					<div class="hint"><xsl:value-of select="$upload-file.hint" /></div>
				</div>
				<div id="add-source-url-panel" class="workbench-field add-source-panel" data-source="url" hidden="hidden">
					<label for="url"><xsl:value-of select="$upload-url.label" /></label>
					<input id="url" name="url" type="text" value=""
						onchange="workbench.add.enabledInput('url');" disabled="disabled" />
				</div>
				<div id="add-source-text-panel" class="workbench-field add-source-panel" data-source="text" hidden="hidden">
					<label for="text"><xsl:value-of select="$upload-text.label" /></label>
					<textarea id="text" name="content" rows="6" cols="70"
						onchange="workbench.add.enabledInput('text')" disabled="disabled"></textarea>
				</div>
				<div class="workbench-field add-source-format">
					<label for="Content-Type"><xsl:value-of select="$data-format.label" /></label>
					<select id="Content-Type" name="Content-Type"
						onchange="workbench.add.handleFormatSelection(this.value)">
						<option id="autodetect" value="autodetect" selected="selected">(autodetect)</option>
						<xsl:for-each select="document(//sparql:link/@href)//sparql:binding[@name='upload-format']">
							<option value="{substring-before(sparql:literal, ' ')}">
								<xsl:value-of select="substring-after(sparql:literal, ' ')" />
							</option>
						</xsl:for-each>
					</select>
				</div>
			</div>
			<details id="add-import-settings" class="workbench-options">
				<summary>
					<span><xsl:value-of select="$advanced-settings.label" /></span>
					<xsl:call-template name="workbench-action-icon">
						<xsl:with-param name="name">chevron</xsl:with-param>
						<xsl:with-param name="additional-class">workbench-disclosure-chevron</xsl:with-param>
					</xsl:call-template>
				</summary>
				<div class="workbench-options__body">
					<div class="workbench-field">
						<label for="baseURI"><xsl:value-of select="$base-uri.label" /></label>
						<input id="baseURI" name="baseURI" type="text"
							value="{//sparql:binding[@name='baseURI']/sparql:literal}"
							onchange="workbench.add.handleBaseURIUse()" />
						<label class="workbench-check" for="useForContext">
							<input type="checkbox" id="useForContext" name="useForContext"
								checked="true" onchange="workbench.add.handleBaseURIUse()" />
							<span>use base URI as context identifier</span>
						</label>
					</div>
					<div class="workbench-field">
						<label for="context"><xsl:value-of select="$context.label" /></label>
						<input id="context" readonly="readonly" name="context" type="text"
							value="{//sparql:binding[@name='context']/sparql:literal}" />
					</div>
					<div class="workbench-field">
						<label for="transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel">
							<xsl:value-of select="$isolation-level.label" />
						</label>
						<select id="transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel"
							name="transaction-setting__org.eclipse.rdf4j.common.transaction.IsolationLevel">
							<option value="">
								<xsl:if test="not($selectedIsolation)">
									<xsl:attribute name="selected">selected</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="$isolation-level.default" />
							</option>
							<xsl:for-each select="//sparql:result[sparql:binding[@name='isolation-level-option']]">
								<xsl:variable name="optionValue"
									select="sparql:binding[@name='isolation-level-option']/sparql:literal/text()" />
								<xsl:variable name="optionLabel"
									select="sparql:binding[@name='isolation-level-option-label']/sparql:literal/text()" />
								<option value="{$optionValue}">
									<xsl:if test="$selectedIsolation=$optionValue">
										<xsl:attribute name="selected">selected</xsl:attribute>
									</xsl:if>
									<xsl:choose>
										<xsl:when test="string-length(normalize-space($optionLabel)) &gt; 0">
											<xsl:value-of select="$optionLabel" />
										</xsl:when>
										<xsl:otherwise><xsl:value-of select="$optionValue" /></xsl:otherwise>
									</xsl:choose>
								</option>
								</xsl:for-each>
						</select>
						<div class="hint"><xsl:value-of select="$isolation-level.desc" /></div>
					</div>
				</div>
			</details>
			<div id="add-upload-actions" class="workbench-form-actions">
				<span class="workbench-action workbench-action--primary">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">upload</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label"><input type="submit" value="Upload" /></span>
					</label>
				</span>
			</div>
		</form>
	    <script src="../../scripts/add.js" type="text/javascript"></script>
	</xsl:template>

</xsl:stylesheet>
