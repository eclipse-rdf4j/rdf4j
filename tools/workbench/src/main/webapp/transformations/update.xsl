<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$update.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql">
		<form id="update-form" class="workbench-island" action="update" method="post" onsubmit="return workbench.update.doSubmit()">
			<div id="update-editor" class="workbench-field">
				<label for="update"><xsl:value-of select="$update-string.label" /></label>
				<textarea id="update" name="update" rows="16" cols="80"><xsl:text>
				</xsl:text></textarea>
				<span id="updateString.errors" class="error" role="alert">
					<xsl:value-of select="//sparql:binding[@name='error-message']" />
				</span>
			</div>
			<div id="update-actions" class="workbench-form-actions">
				<span class="workbench-action workbench-action--primary">
					<label class="workbench-action-hit-area">
						<xsl:call-template name="workbench-action-icon">
							<xsl:with-param name="name">update</xsl:with-param>
						</xsl:call-template>
						<span class="workbench-action-label"><input type="submit" value="{$execute.label}" /></span>
					</label>
				</span>
			</div>
		</form>
		<script type="text/javascript">
        var namespaces = {
            <xsl:for-each
                select="document(//sparql:link[@href='namespaces']/@href)//sparql:results/sparql:result">
                <xsl:value-of
                    select="concat('&quot;', sparql:binding[@name='prefix']/sparql:literal, ':&quot;:&quot;', sparql:binding[@name='namespace']/sparql:literal, '&quot;,')" />
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
        };
        </script>
		<script src="../../scripts/codemirror.4.5.0.min.js" type="text/javascript"></script>
        <script src="../../scripts/yasqe.min.js" type="text/javascript"></script>
        <script src="../../scripts/yasqeHelper.js" type="text/javascript"></script>
		<script src="../../scripts/update.js" type="text/javascript"></script>
	</xsl:template>

</xsl:stylesheet>
