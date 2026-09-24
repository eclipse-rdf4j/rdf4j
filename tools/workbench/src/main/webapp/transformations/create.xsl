<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
   <!ENTITY xsd  "http://www.w3.org/2001/XMLSchema#" >
 ]>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$repository-create.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql">
		<form action="create">
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$repository-type.label" />
						</th>
						<td>
							<select id="type" name="type">
								<xsl:for-each select="sparql:results/sparql:result">
									<option value="{sparql:binding[@name='type']/sparql:literal}">
										<xsl:value-of select="sparql:binding[@name='label']/sparql:literal" />
									</option>
								</xsl:for-each>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-id.label" />
						</th>
						<td>
							<input type="text" id="id" name="id" size="16" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-title.label" />
						</th>
						<td>
							<input type="text" id="title" name="title" size="48" />
						</td>
						<td></td>
					</tr>
					<tr>
						<td></td>
						<td>
							<span class="workbench-action workbench-action--secondary">
								<label class="workbench-action-hit-area">
									<xsl:call-template name="workbench-action-icon">
										<xsl:with-param name="name">cancel</xsl:with-param>
									</xsl:call-template>
									<span class="workbench-action-label">
										<input type="button" value="{$cancel.label}"
											data-href="repositories"
											onclick="document.location.href=this.getAttribute('data-href')" />
									</span>
								</label>
							</span>
							<span class="workbench-action workbench-action--primary">
								<label class="workbench-action-hit-area">
									<xsl:call-template name="workbench-action-icon">
										<xsl:with-param name="name">execute</xsl:with-param>
									</xsl:call-template>
									<span class="workbench-action-label">
										<input type="submit" name="next" value="{$next.label}" />
									</span>
								</label>
							</span>
						</td>
					</tr>
				</tbody>
			</table>
		</form>
	</xsl:template>

</xsl:stylesheet>
