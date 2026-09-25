<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$change-server.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql">
		<form id="server-form" action="server" method="post" onsubmit="changeServer(event)">
			<div class="workbench-form-grid server-connection-fields">
				<div class="workbench-field">
					<label for="workbench-server">
						<xsl:value-of select="$change-server.label" />
					</label>
					<input id="workbench-server"
						name="workbench-server" type="text" size="40"
						value="{normalize-space(//sparql:binding[@name='server'])}" />
					<div class="hint"><xsl:value-of select="$change-server.desc" /></div>
					<span class="error" role="alert">
						<xsl:value-of select="//sparql:binding[@name='error-message']" />
					</span>
				</div>
			</div>
			<details id="server-auth" class="workbench-options">
				<summary>
					<span><xsl:value-of select="$advanced-settings.label" /></span>
					<xsl:call-template name="workbench-action-icon">
						<xsl:with-param name="name">chevron</xsl:with-param>
						<xsl:with-param name="additional-class">workbench-disclosure-chevron</xsl:with-param>
					</xsl:call-template>
				</summary>
				<div class="workbench-options__body">
					<div class="workbench-form-grid">
						<div class="workbench-field">
							<label for="server-user"><xsl:value-of select="$server-user.label" /></label>
							<input id="server-user" name="server-user" type="text"
								value="{normalize-space(//sparql:binding[@name='server-user'])}" />
						</div>
						<div class="workbench-field">
							<label for="server-password"><xsl:value-of select="$server-password.label" /></label>
							<input id="server-password" name="server-password" type="password" value="" />
						</div>
					</div>
				</div>
			</details>
			<div id="server-change-actions" class="workbench-form-actions">
				<span class="workbench-action workbench-action--primary">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">update</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label"><input type="submit" value="{$change.label}" /></span>
					</label>
				</span>
			</div>
		</form>
		<script src="../../scripts/server.js" type="text/javascript">
		</script>
	</xsl:template>

</xsl:stylesheet>
