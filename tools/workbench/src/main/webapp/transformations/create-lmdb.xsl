<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$repository-create.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:key name="lmdb-field" match="sparql:result"
		use="sparql:binding[@name='fieldId']/sparql:literal" />

	<xsl:template match="sparql:sparql">
		<form action="create" method="post">
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$repository-type.label" />
						</th>
						<td>
							<select id="type" name="type">
								<option value="lmdb">
									LMDB Store
								</option>
							</select>
						</td>
						<td></td>
					</tr>
					<xsl:for-each
						select="sparql:results/sparql:result[generate-id() = generate-id(key('lmdb-field', sparql:binding[@name='fieldId']/sparql:literal)[1])]">
						<xsl:variable name="fieldId"
							select="sparql:binding[@name='fieldId']/sparql:literal" />
						<xsl:variable name="fieldName"
							select="sparql:binding[@name='fieldName']/sparql:literal" />
						<xsl:variable name="fieldType"
							select="sparql:binding[@name='fieldType']/sparql:literal" />
						<xsl:variable name="fieldRows" select="key('lmdb-field', $fieldId)" />
						<tr>
							<th>
								<xsl:call-template name="lmdb-field-label">
									<xsl:with-param name="fieldId" select="$fieldId" />
									<xsl:with-param name="fieldName" select="$fieldName" />
								</xsl:call-template>
							</th>
							<td>
								<xsl:choose>
									<xsl:when test="$fieldType = 'select'">
										<select id="{$fieldId}" name="{$fieldName}">
											<xsl:for-each select="$fieldRows">
												<xsl:variable name="optionValue"
													select="sparql:binding[@name='value']/sparql:literal" />
												<option value="{$optionValue}">
													<xsl:if
														test="sparql:binding[@name='selected']/sparql:literal = 'true'">
														<xsl:attribute name="selected">selected</xsl:attribute>
													</xsl:if>
													<xsl:call-template name="lmdb-option-label">
														<xsl:with-param name="fieldId" select="$fieldId" />
														<xsl:with-param name="optionValue" select="$optionValue" />
													</xsl:call-template>
												</option>
											</xsl:for-each>
										</select>
									</xsl:when>
									<xsl:otherwise>
										<input type="text" id="{$fieldId}" name="{$fieldName}"
											size="{($fieldId = 'title') * 48 + not($fieldId = 'title') * 16}"
											value="{sparql:binding[@name='value']/sparql:literal}" />
									</xsl:otherwise>
								</xsl:choose>
							</td>
							<td></td>
						</tr>
					</xsl:for-each>
					<tr>
						<td></td>
						<td>
							<input type="button" value="{$cancel.label}" style="float:right"
								data-href="repositories"
                                onclick="document.location.href=this.getAttribute('data-href')" />
							<input id="create" type="button" value="{$create.label}"
								onclick="checkOverwrite()" />
						</td>
					</tr>
				</tbody>
			</table>
		</form>
		<script src="../../scripts/create.js" type="text/javascript">
		</script>
	</xsl:template>

	<xsl:template name="lmdb-field-label">
		<xsl:param name="fieldId" />
		<xsl:param name="fieldName" />
		<xsl:choose>
			<xsl:when test="$fieldId = 'id'">
				<xsl:value-of select="$repository-id.label" />
			</xsl:when>
			<xsl:when test="$fieldId = 'title'">
				<xsl:value-of select="$repository-title.label" />
			</xsl:when>
			<xsl:when test="$fieldId = 'indexes'">
				<xsl:value-of select="$repository-indexes.label" />
			</xsl:when>
			<xsl:when test="$fieldId = 'queryEvalMode'">
				<xsl:value-of select="$repository-evaluation-mode.label" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$fieldName" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="lmdb-option-label">
		<xsl:param name="fieldId" />
		<xsl:param name="optionValue" />
		<xsl:choose>
			<xsl:when test="$optionValue = 'true'">
				<xsl:value-of select="$true.label" />
			</xsl:when>
			<xsl:when test="$optionValue = 'false'">
				<xsl:value-of select="$false.label" />
			</xsl:when>
			<xsl:when test="$fieldId = 'queryEvalMode'">
				<xsl:value-of
					select="translate($optionValue, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$optionValue" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>
