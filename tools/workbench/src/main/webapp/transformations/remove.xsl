<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$remove.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql">
		<xsl:variable name="info"
			select="document(sparql:head/sparql:link[@href='info']/@href)" />
		<xsl:if
			test="$info//sparql:binding[@name='id']/sparql:literal/text() = 'SYSTEM'">
			<p class="WARN">
				<xsl:value-of select="$SYSTEM-warning.desc" />
			</p>
		</xsl:if>
		<p id="remove-warning" class="WARN" role="alert">
			<xsl:value-of select="$remove-warning.desc" />
		</p>
		<p>
			<xsl:value-of select="$value-encoding.desc" />
		</p>

		<details id="remove-examples" class="workbench-options">
			<summary>
				<span><xsl:value-of select="$examples.label" /></span>
				<xsl:call-template name="workbench-action-icon">
					<xsl:with-param name="name">chevron</xsl:with-param>
					<xsl:with-param name="additional-class">workbench-disclosure-chevron</xsl:with-param>
				</xsl:call-template>
			</summary>
		<ul>
			<li>
				URI:
				<tt>&lt;http://foo.com/bar&gt;</tt>
			</li>
			<li>
				BNode:
				<tt>_:nodeID</tt>
			</li>
			<li>
				Literal:
				<tt>"Hello"</tt>
				,
				<tt>"Hello"@en</tt>
				and
				<tt>"Hello"^^&lt;http://bar.com/foo&gt;</tt>
			</li>

		</ul>
		</details>
		<xsl:if test="//sparql:binding[@name='error-message']">
			<p class="error">
				<xsl:value-of select="//sparql:binding[@name='error-message']" />
			</p>
		</xsl:if>
		<form id="remove-form" method="post" action="remove">
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$subject.label" />
						</th>
						<td>
							<input id="subj" name="subj" type="text"
								value="{//sparql:binding[@name='subj']/sparql:literal}" />
						</td>
						<td></td>

					</tr>
					<tr>
						<th>
							<xsl:value-of select="$predicate.label" />
						</th>
						<td>
							<input id="pred" name="pred" type="text"
								value="{//sparql:binding[@name='pred']/sparql:literal}" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$object.label" />
						</th>

						<td>
							<textarea id="obj" name="obj" cols="70" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$context.label" />
						</th>

						<td>
							<input id="context" name="context" type="text"
								value="{//sparql:binding[@name='context']/sparql:literal}" />
						</td>
						<td></td>
					</tr>
					<tr>
						<td></td>
						<td>
							<span class="workbench-action workbench-action--danger">
								<label class="workbench-action-hit-area">
									<xsl:call-template name="workbench-action-icon">
										<xsl:with-param name="name">remove</xsl:with-param>
									</xsl:call-template>
									<span class="workbench-action-label"><input type="submit" value="{$remove.label}" /></span>
								</label>
							</span>
						</td>
						<td></td>
					</tr>
				</tbody>

			</table>
		</form>
	</xsl:template>

</xsl:stylesheet>
