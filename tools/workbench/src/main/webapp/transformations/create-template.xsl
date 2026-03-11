<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$repository-create.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:key name="field" match="sparql:result"
		use="sparql:binding[@name='fieldId']/sparql:literal" />

	<xsl:template match="sparql:sparql">
		<xsl:variable name="templateType"
			select="sparql:results/sparql:result[1]/sparql:binding[@name='templateType']/sparql:literal" />
		<xsl:variable name="templateLabel"
			select="sparql:results/sparql:result[1]/sparql:binding[@name='templateLabel']/sparql:literal" />
		<form action="create" method="post">
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$repository-type.label" />
						</th>
						<td>
							<select id="type" name="type">
								<option value="{$templateType}">
									<xsl:value-of select="$templateLabel" />
								</option>
							</select>
						</td>
						<td></td>
					</tr>
					<xsl:for-each
						select="sparql:results/sparql:result[generate-id() = generate-id(key('field', sparql:binding[@name='fieldId']/sparql:literal)[1])]">
						<xsl:variable name="fieldId"
							select="sparql:binding[@name='fieldId']/sparql:literal" />
						<xsl:variable name="fieldName"
							select="sparql:binding[@name='fieldName']/sparql:literal" />
						<xsl:variable name="fieldProperty"
							select="sparql:binding[@name='fieldProperty']/sparql:literal" />
						<xsl:variable name="fieldRole"
							select="sparql:binding[@name='fieldRole']/sparql:literal" />
						<xsl:variable name="fieldType"
							select="sparql:binding[@name='fieldType']/sparql:literal" />
						<xsl:variable name="fieldRows" select="key('field', $fieldId)" />
						<xsl:variable name="defaultValue"
							select="$fieldRows[sparql:binding[@name='selected']/sparql:literal = 'true'][1]/sparql:binding[@name='value']/sparql:literal" />
						<xsl:variable name="size"
							select="sparql:binding[@name='size']/sparql:literal" />
						<xsl:variable name="rows"
							select="sparql:binding[@name='rows']/sparql:literal" />
						<xsl:variable name="cols"
							select="sparql:binding[@name='cols']/sparql:literal" />
						<xsl:variable name="placeholder"
							select="sparql:binding[@name='placeholder']/sparql:literal" />
						<tr>
							<th>
								<xsl:value-of select="$fieldName" />
							</th>
							<td>
								<xsl:choose>
									<xsl:when test="$fieldType = 'select'">
										<select id="{$fieldId}" name="{$fieldName}">
											<xsl:call-template name="field-metadata-attributes">
												<xsl:with-param name="fieldProperty" select="$fieldProperty" />
												<xsl:with-param name="fieldRole" select="$fieldRole" />
											</xsl:call-template>
											<xsl:for-each select="$fieldRows">
												<xsl:variable name="value"
													select="sparql:binding[@name='value']/sparql:literal" />
												<option value="{$value}"><xsl:if
														test="sparql:binding[@name='selected']/sparql:literal = 'true'"><xsl:attribute
															name="selected">selected</xsl:attribute></xsl:if>
													<xsl:call-template name="option-label">
														<xsl:with-param name="value" select="$value" />
													</xsl:call-template>
												</option>
											</xsl:for-each>
										</select>
									</xsl:when>
									<xsl:when test="$fieldType = 'radio'">
										<xsl:choose>
											<xsl:when
												test="count($fieldRows) = 2 and count($fieldRows[sparql:binding[@name='value']/sparql:literal = 'true']) = 1 and count($fieldRows[sparql:binding[@name='value']/sparql:literal = 'false']) = 1">
												<xsl:for-each select="$fieldRows">
													<xsl:sort
														select="sparql:binding[@name='value']/sparql:literal = 'true'"
														order="descending" />
													<xsl:call-template name="render-radio-option">
														<xsl:with-param name="fieldId" select="$fieldId" />
														<xsl:with-param name="fieldName" select="$fieldName" />
														<xsl:with-param name="fieldProperty" select="$fieldProperty" />
														<xsl:with-param name="fieldRole" select="$fieldRole" />
													</xsl:call-template>
												</xsl:for-each>
											</xsl:when>
											<xsl:otherwise>
												<xsl:for-each select="$fieldRows">
													<xsl:call-template name="render-radio-option">
														<xsl:with-param name="fieldId" select="$fieldId" />
														<xsl:with-param name="fieldName" select="$fieldName" />
														<xsl:with-param name="fieldProperty" select="$fieldProperty" />
														<xsl:with-param name="fieldRole" select="$fieldRole" />
													</xsl:call-template>
												</xsl:for-each>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="$fieldType = 'textarea'">
										<textarea id="{$fieldId}" name="{$fieldName}" rows="{$rows}"
											cols="{$cols}">
											<xsl:call-template name="field-metadata-attributes">
												<xsl:with-param name="fieldProperty" select="$fieldProperty" />
												<xsl:with-param name="fieldRole" select="$fieldRole" />
											</xsl:call-template><xsl:if test="string-length($placeholder) &gt; 0"><xsl:attribute
													name="placeholder"><xsl:value-of select="$placeholder" /></xsl:attribute></xsl:if>
											<xsl:value-of select="$defaultValue" />
										</textarea>
									</xsl:when>
									<xsl:otherwise>
										<input type="text" id="{$fieldId}" name="{$fieldName}"
											size="{$size}" value="{$defaultValue}">
											<xsl:call-template name="field-metadata-attributes">
												<xsl:with-param name="fieldProperty" select="$fieldProperty" />
												<xsl:with-param name="fieldRole" select="$fieldRole" />
											</xsl:call-template><xsl:if
												test="string-length($placeholder) &gt; 0"><xsl:attribute
													name="placeholder"><xsl:value-of select="$placeholder" /></xsl:attribute></xsl:if></input>
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
		<script src="../../scripts/create.js" type="text/javascript"></script>
	</xsl:template>

	<xsl:template name="option-label">
		<xsl:param name="value" />
		<xsl:choose>
			<xsl:when test="$value = 'true'">
				<xsl:value-of select="$true.label" />
			</xsl:when>
			<xsl:when test="$value = 'false'">
				<xsl:value-of select="$false.label" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$value" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="render-radio-option">
		<xsl:param name="fieldId" />
		<xsl:param name="fieldName" />
		<xsl:param name="fieldProperty" />
		<xsl:param name="fieldRole" />
		<xsl:variable name="value" select="sparql:binding[@name='value']/sparql:literal" />
		<input type="radio" id="{concat($fieldId, '-', position())}" name="{$fieldName}"
			value="{$value}">
			<xsl:call-template name="field-metadata-attributes">
				<xsl:with-param name="fieldProperty" select="$fieldProperty" />
				<xsl:with-param name="fieldRole" select="$fieldRole" />
			</xsl:call-template><xsl:if
				test="sparql:binding[@name='selected']/sparql:literal = 'true'"><xsl:attribute
					name="checked">checked</xsl:attribute></xsl:if></input>
		<xsl:call-template name="option-label">
			<xsl:with-param name="value" select="$value" />
		</xsl:call-template>
		<xsl:text> </xsl:text>
	</xsl:template>

	<xsl:template name="field-metadata-attributes">
		<xsl:param name="fieldProperty" />
		<xsl:param name="fieldRole" />
		<xsl:if test="string-length($fieldProperty) &gt; 0">
			<xsl:attribute name="data-config-property"><xsl:value-of select="$fieldProperty" /></xsl:attribute>
		</xsl:if>
		<xsl:if test="string-length($fieldRole) &gt; 0">
			<xsl:attribute name="data-field-role"><xsl:value-of select="$fieldRole" /></xsl:attribute>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
