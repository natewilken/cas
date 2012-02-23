<%@ page pageEncoding="UTF-8"
%><%@ page contentType="text/html; charset=UTF-8"
%><jsp:directive.include file="/WEB-INF/view/jsp/asu/ui/includes/top.jsp" />

			<div id="login">
				<h2>Sign In Error</h2>
				<div class="content internal_error">
					There was an error trying to complete your request. <br />
					Please <a href="${fn:escapeXml(request.requestURI)}">try again</a>.
				</div>
			</div>

<jsp:directive.include file="/WEB-INF/view/jsp/asu/ui/includes/bottom.jsp" />

	<script type="text/javascript" src="js/asu-signin.js"></script>
	<script type="text/javascript" src="themes/asu/onload.js"></script>
</body>
</html>