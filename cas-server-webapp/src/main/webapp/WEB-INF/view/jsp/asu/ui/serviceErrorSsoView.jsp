<jsp:directive.include file="includes/top.jsp" />

	<div id="login">
		<h2><spring:message code="screen.service.sso.error.header" /></h2>
		<div class="content internal_error">
			<div class="headline"><spring:message code="screen.service.sso.error.message" /></div>
			<div class="subtext"><spring:message code="screen.service.sso.error.retry" arguments="${fn:escapeXml(request.requestURI)}" /></div>
		</div>
	</div>

<jsp:directive.include file="includes/bottom.jsp" />

	<script type="text/javascript" src="js/asu-signin.js"></script>
	<script type="text/javascript" src="themes/<spring:theme code="name" />/onload.js"></script>
</body>
</html>