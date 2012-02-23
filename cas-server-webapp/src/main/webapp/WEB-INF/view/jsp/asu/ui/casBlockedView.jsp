<jsp:directive.include file="includes/top.jsp" />

	<div id="login">
		<h2><spring:message code="screen.blocked.header" /></h2>
		<div class="content internal_error"><spring:message code="screen.blocked.message" /></div>
	</div>
		
<jsp:directive.include file="includes/bottom.jsp" />

	<script type="text/javascript" src="js/asu-signin.js"></script>
	<script type="text/javascript" src="themes/<spring:theme code="name" />/onload.js"></script>
</body>
</html>