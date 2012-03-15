<%@ page pageEncoding="UTF-8"
%><%@ page contentType="text/html; charset=UTF-8"
%><jsp:directive.include file="includes/top.jsp" />

	<div id="lockout">
		<div class="content">
			<div class="headline"><spring:message code="screen.lockout.headline" /></div>
			<div class="subtext"><spring:message code="screen.lockout.subtext" /></div>
			<div class="more"><spring:message code="screen.lockout.more" /></div>
		</div>
	</div>

<jsp:directive.include file="includes/bottom.jsp" />

	<script type="text/javascript" src="js/asu-signin.js"></script>
	<script type="text/javascript" src="themes/<spring:theme code="name" />/onload.js"></script>
</body>
</html>