<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:n1="urn:hl7-org:v3">


	<xsl:template match="/">
		<xsl:apply-templates select="/n1:section" />
	</xsl:template>


	<xsl:template match="n1:section">
<html>
			<body>
				<h2>
					<xsl:value-of select="n1:title" />
				</h2>
				<table border="1">

					<xsl:if test="count(n1:entry/n1:act)&gt;0">
						<tr>
							<td width="100%">
								<table border='2'>
									<thead>
										<tr>
											<th>Type</th>
											<th>Description</th>
											<th>Effective Date</th>
											<th>Status</th>
										</tr>
									</thead>
									<xsl:apply-templates select="n1:entry/n1:act" />
								</table>
							</td>
						</tr>
					</xsl:if>

					<xsl:if test="count(n1:entry/n1:encounter)&gt;0">
						<tr>
							<td width="100%">
								<table border='2'>
									<thead>
										<tr>
											<th>Encounters Dates</th>
											<th>Location</th>
											<th>Diagnoses</th>
											<th>Providers</th>
										</tr>
									</thead>
									<xsl:apply-templates select="n1:entry/n1:encounter" />
								</table>
							</td>
						</tr>
					</xsl:if>


					<xsl:if test="count(n1:entry/n1:substanceAdministration)&gt;0">
						<tr>
							<td width="100%">
								<table border='2'>
									<thead>
										<tr>
											<th>Medication</th>
											<th>Instructions</th>
											<th>Effective Date</th>
											<th>Status</th>
										</tr>
									</thead>
									<xsl:apply-templates select="n1:entry/n1:substanceAdministration" />
								</table>
							</td>
						</tr>
					</xsl:if>

					<xsl:if test="count(n1:entry/n1:observation)&gt;0">
						<tr>
							<td width="100%">
								<table border='2'>
									<thead>
										<tr>
											<th>Type</th>
											<th>Value</th>
											<th>Effective Date</th>
											<th>Status</th>
										</tr>
									</thead>
									<xsl:apply-templates select="n1:entry/n1:observation" />
								</table>
							</td>
						</tr>
					</xsl:if>

					<xsl:if test="count(n1:entry/n1:observationMedia)&gt;0">
						<tr>
							<td width="100%">
								<table border='2'>
									<thead>
										<tr>
											<th>Medication</th>
											<th>Instructions</th>
											<th>Start Date</th>
											<th>Stop Date</th>
											<th>Status</th>
										</tr>
									</thead>
									<xsl:apply-templates select="n1:entry/n1:observationMedia" />
								</table>
							</td>
						</tr>
					</xsl:if>

					<xsl:if test="count(n1:entry/n1:organizer)&gt;0">
						<tr>
							<td width="100%">
								<table border='2'>
									<thead>
										<tr>
										<th>Description</th>
										<th>Value/Unit</th>
										<th>Description</th>
										<th>Interpretation</th>
										<th>Reference Range</th>
										</tr>
									</thead>
									<xsl:apply-templates select="n1:entry/n1:organizer" />
								</table>
							</td>
						</tr>
					</xsl:if>


					<xsl:if test="count(n1:entry/n1:procedure)&gt;0">
						<tr>
							<td width="100%">
								<table border='2'>
									<thead>
										<tr>
											<th>Medication</th>
											<th>Instructions</th>
											<th>Start Date</th>
											<th>Stop Date</th>
											<th>Status</th>
										</tr>
									</thead>
									<xsl:apply-templates select="n1:entry/n1:procedure" />
								</table>
							</td>
						</tr>
					</xsl:if>

					<xsl:if test="count(n1:entry/n1:regionOfInterest)&gt;0">
						<tr>
							<td width="100%">
								<table border='2'>
									<thead>
										<tr>
											<th>Medication</th>
											<th>Instructions</th>
											<th>Start Date</th>
											<th>Stop Date</th>
											<th>Status</th>
										</tr>
									</thead>
									<xsl:apply-templates select="n1:entry/n1:regionOfInterest" />
								</table>
							</td>
						</tr>
					</xsl:if>


					<xsl:if test="count(n1:entry/n1:supply)&gt;0">
						<tr>
							<td width="100%">
								<table border='2'>
									<thead>
										<tr>
											<th>Medication</th>
											<th>Instructions</th>
											<th>Start Date</th>
											<th>Stop Date</th>
											<th>Status</th>
										</tr>
									</thead>
									<xsl:apply-templates select="n1:entry/n1:supply" />
								</table>
							</td>
						</tr>
					</xsl:if>
				</table>
			</body>
		</html>
	</xsl:template>



	<xsl:template match="n1:entryRelationship">

		<xsl:apply-templates />

	</xsl:template>

	<xsl:template match="n1:act">
		<tr>
			<td>
				<xsl:call-template name="showCode" />
			</td>
			<td>
				<xsl:value-of select="n1:text" />
			</td>
			<td>
				<xsl:call-template name="showEffectiveTime" />
			</td>
			<td>
				<xsl:for-each select="n1:statusCode">

					<xsl:call-template name="showStatusCode" />
				</xsl:for-each>
			</td>
		</tr>
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="n1:encounter">
		<tr>
			<td>
				<xsl:call-template name="showEffectiveTime" />
			</td>
			<td>
				<xsl:call-template name="showCode" />
			</td>
			<td>
				<xsl:value-of select="n1:text" />
			</td>

			<td>
				<xsl:for-each select="n1:statusCode">

					<xsl:call-template name="showStatusCode" />
				</xsl:for-each>
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="n1:observation">
		<tr>
			<td>
				<xsl:call-template name="showCode" />
			</td>
			<td>
				<xsl:for-each select="n1:value">
					<xsl:call-template name="showObservationValue" />
				</xsl:for-each>
			</td>
			<td>
				<xsl:call-template name="showEffectiveTime" />
			</td>
			<td>
 					<xsl:call-template name="showInterpretationCode" />
			</td>
			
				<td>
				<xsl:for-each select="n1:referenceRange/n1:observationRange">
				<xsl:value-of select="n1:text" /><xsl:text> </xsl:text>
 					 </xsl:for-each>
			</td>
			
		</tr>
	</xsl:template>

	<xsl:template match="n1:observationMedia">
		<tr>
			<td>
				<xsl:call-template name="showCode" />
			</td>
			<td>
				<xsl:value-of select="n1:text" />
			</td>
			<td>
				<xsl:call-template name="showEffectiveTime" />
			</td>
			<td>
				<xsl:for-each select="n1:statusCode">
					<xsl:call-template name="showCode" />
				</xsl:for-each>
			</td>
		</tr>
	</xsl:template>


	<xsl:template match="n1:organizer">
	
		
		<tr>
			<td colspan="5">
				<xsl:call-template name="showCode" />
			</td>
		</tr>
	
	<xsl:apply-templates select="n1:component/n1:observation" />
	</xsl:template>

	<xsl:template match="n1:procedure">
		<tr>
			<td>
				<xsl:call-template name="showCode" />
			</td>
			<td>
				<xsl:value-of select="n1:text" />
			</td>
			<td>
				<xsl:call-template name="showEffectiveTime" />
			</td>
			<td>
				<xsl:for-each select="n1:statusCode">
					<xsl:call-template name="showCode" />
				</xsl:for-each>
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="n1:regionOfInterest">
		<tr>
			<td>
				<xsl:call-template name="showCode" />
			</td>
			<td>
				<xsl:value-of select="n1:text" />
			</td>
			<td>
				<xsl:call-template name="showEffectiveTime" />
			</td>
			<td>
				<xsl:for-each select="n1:statusCode">
					<xsl:call-template name="showCode" />
				</xsl:for-each>
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="n1:substanceAdministration">
		<tr>
			<td>
				<xsl:for-each
					select="n1:consumable/n1:manufacturedProduct/n1:manufacturedMaterial">
					<xsl:call-template name="showCode" />
				</xsl:for-each>
			</td>
			<td>
				<xsl:value-of select="n1:text" />
			</td>
			<td>
				<xsl:call-template name="showEffectiveTime" />
			</td>
			<td>
				<xsl:for-each select="n1:statusCode">
					<xsl:call-template name="showCode" />
				</xsl:for-each>
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="n1:supply">
		<tr>
			<td>
				<xsl:call-template name="showCode" />
			</td>
			<td>
				<xsl:value-of select="n1:displayName" />
			</td>
			<td>
				<xsl:call-template name="showEffectiveTime" />
			</td>
			<td>
				<xsl:for-each select="n1:statusCode">
					<xsl:call-template name="showCode" />
				</xsl:for-each>
			</td>
		</tr>
	</xsl:template>
	
	
		<xsl:template name="showObservationValue">
		
		<xsl:choose>
			<xsl:when test="@xsi:type='PQ'">
				<xsl:value-of select="@value" /><xsl:text> </xsl:text><xsl:value-of select="@unit" />
			</xsl:when>
			<xsl:when test="@xsi:type='IVL_PQ'">
			
			<xsl:for-each select="n1:low">
					<xsl:value-of select="@value" /><xsl:text> </xsl:text><xsl:value-of select="@unit" />
				</xsl:for-each>
				
				
				<xsl:text> to </xsl:text>
					<xsl:for-each select="n1:high">
					<xsl:value-of select="@value" /><xsl:text> </xsl:text><xsl:value-of select="@unit" />
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="@code" />
			</xsl:otherwise>
		</xsl:choose>
		
		
	</xsl:template>

	<xsl:template name="showValue">
 
		<xsl:choose>
			<xsl:when test="@displayName">
				<xsl:value-of select="@displayName" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="@code" />
			</xsl:otherwise>
		</xsl:choose>



	</xsl:template>



	<xsl:template name="showReference">
		<xsl:param name="ID" />
		<xsl:for-each select="//n1:content[@ID=$ID]">
			<xsl:value-of select="." />
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="showText">


		<xsl:choose>
			<xsl:when test="n1:text/n1:reference/@value">
				<xsl:value-of select="n1:text/n1:reference/@value" />
				<xsl:call-template name="showReference">
					<xsl:with-param name="ID" select="n1:text/n1:reference/@value" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="n1:text" />
			</xsl:otherwise>
		</xsl:choose>



	</xsl:template>


 

<xsl:template name="showInterpretationCode">
 

	<xsl:choose>
			<xsl:when test="n1:interpretationCode/@code='N'">
				<xsl:text>NORMAL</xsl:text>
			</xsl:when>
			<xsl:when test="n1:interpretationCode/@code='A'">
				<xsl:text>ABNORMAL</xsl:text>
			</xsl:when>
			<xsl:when test="n1:interpretationCode/@code='H'">
				<xsl:text>HIGH</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>MISSINGFOR</xsl:text><xsl:value-of select="n1:interpretationCode/@code" />
			</xsl:otherwise>
		</xsl:choose>
		
</xsl:template>

	<xsl:template name="showCode">

		<xsl:choose>
			<xsl:when test="n1:code/@displayName">
				<xsl:value-of select="n1:code/@displayName" />
			</xsl:when>
			<xsl:when test="n1:code/n1:originalText">
				<xsl:value-of select="n1:code/n1:originalText" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="n1:code/@code" />
			</xsl:otherwise>
		</xsl:choose>



	</xsl:template>


	<xsl:template name="showStatusCode">

		<xsl:choose>
			<xsl:when test="@displayName">
				<xsl:value-of select="@displayName" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="@code" />
			</xsl:otherwise>
		</xsl:choose>



	</xsl:template>

	<xsl:template name="showEffectiveTime">

		<xsl:choose>
			<xsl:when test="n1:effectiveTime/@value">
				<xsl:call-template name="show-time">
					<xsl:with-param name="datetime" select="n1:effectiveTime" />
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="n1:effectiveTime/n1:low">
				<xsl:call-template name="show-time">
					<xsl:with-param name="datetime" select="n1:effectiveTime/n1:low" />
				</xsl:call-template>
				<xsl:if test="n1:effectiveTime/n1:high">
					<xsl:text> to </xsl:text>
					<xsl:call-template name="show-time">
						<xsl:with-param name="datetime" select="n1:effectiveTime/n1:high" />
					</xsl:call-template>
				</xsl:if>
			</xsl:when>
		</xsl:choose>

	</xsl:template>

	<xsl:template name="show-time">
		<xsl:param name="datetime" />
		<xsl:choose>
			<xsl:when test="not($datetime)">
				<xsl:call-template name="formatDateTime">
					<xsl:with-param name="date" select="@value" />
				</xsl:call-template>
				<xsl:text />
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="formatDateTime">
					<xsl:with-param name="date" select="$datetime/@value" />
				</xsl:call-template>
				<xsl:text />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<xsl:template name="formatDateTime">
		<xsl:param name="date" />
		<!-- month -->
		<xsl:variable name="month" select="substring ($date, 5, 2)" />
		<xsl:choose>
			<xsl:when test="$month='01'">
				<xsl:text>January </xsl:text>
			</xsl:when>
			<xsl:when test="$month='02'">
				<xsl:text>February </xsl:text>
			</xsl:when>
			<xsl:when test="$month='03'">
				<xsl:text>March </xsl:text>
			</xsl:when>
			<xsl:when test="$month='04'">
				<xsl:text>April </xsl:text>
			</xsl:when>
			<xsl:when test="$month='05'">
				<xsl:text>May </xsl:text>
			</xsl:when>
			<xsl:when test="$month='06'">
				<xsl:text>June </xsl:text>
			</xsl:when>
			<xsl:when test="$month='07'">
				<xsl:text>July </xsl:text>
			</xsl:when>
			<xsl:when test="$month='08'">
				<xsl:text>August </xsl:text>
			</xsl:when>
			<xsl:when test="$month='09'">
				<xsl:text>September </xsl:text>
			</xsl:when>
			<xsl:when test="$month='10'">
				<xsl:text>October </xsl:text>
			</xsl:when>
			<xsl:when test="$month='11'">
				<xsl:text>November </xsl:text>
			</xsl:when>
			<xsl:when test="$month='12'">
				<xsl:text>December </xsl:text>
			</xsl:when>
		</xsl:choose>
		<!-- day -->
		<xsl:choose>
			<xsl:when test='substring ($date, 7, 1)="0"'>
				<xsl:value-of select="substring ($date, 8, 1)" />
				<xsl:text>, </xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="substring ($date, 7, 2)" />
				<xsl:text>, </xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<!-- year -->
		<xsl:value-of select="substring ($date, 1, 4)" />
		<!-- time and US timezone -->
		<xsl:if test="string-length($date) &gt; 8">
			<xsl:text>, </xsl:text>
			<!-- time -->
			<xsl:variable name="time">
				<xsl:value-of select="substring($date,9,6)" />
			</xsl:variable>
			<xsl:variable name="hh">
				<xsl:value-of select="substring($time,1,2)" />
			</xsl:variable>
			<xsl:variable name="mm">
				<xsl:value-of select="substring($time,3,2)" />
			</xsl:variable>
			<xsl:variable name="ss">
				<xsl:value-of select="substring($time,5,2)" />
			</xsl:variable>
			<xsl:if test="string-length($hh)&gt;1">
				<xsl:value-of select="$hh" />
				<xsl:if
					test="string-length($mm)&gt;1 and not(contains($mm,'-')) and not (contains($mm,'+'))">
					<xsl:text>:</xsl:text>
					<xsl:value-of select="$mm" />
					<xsl:if
						test="string-length($ss)&gt;1 and not(contains($ss,'-')) and not (contains($ss,'+'))">
						<xsl:text>:</xsl:text>
						<xsl:value-of select="$ss" />
					</xsl:if>
				</xsl:if>
			</xsl:if>
			<!-- time zone -->
			<xsl:variable name="tzon">
				<xsl:choose>
					<xsl:when test="contains($date,'+')">
						<xsl:text>+</xsl:text>
						<xsl:value-of select="substring-after($date, '+')" />
					</xsl:when>
					<xsl:when test="contains($date,'-')">
						<xsl:text>-</xsl:text>
						<xsl:value-of select="substring-after($date, '-')" />
					</xsl:when>
				</xsl:choose>
			</xsl:variable>
			<xsl:choose>
				<!-- reference: http://www.timeanddate.com/library/abbreviations/timezones/na/ -->
				<xsl:when test="$tzon = '-0500' ">
					<xsl:text>, EST</xsl:text>
				</xsl:when>
				<xsl:when test="$tzon = '-0600' ">
					<xsl:text>, CST</xsl:text>
				</xsl:when>
				<xsl:when test="$tzon = '-0700' ">
					<xsl:text>, MST</xsl:text>
				</xsl:when>
				<xsl:when test="$tzon = '-0800' ">
					<xsl:text>, PST</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text />
					<xsl:value-of select="$tzon" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>