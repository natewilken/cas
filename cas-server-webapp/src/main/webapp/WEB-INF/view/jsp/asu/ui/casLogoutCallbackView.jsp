<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="spring" uri="http://www.springframework.org/tags"
%><%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"
%><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"
%><!DOCTYPE HTML>
<html>
<head>
    <title>CAS &#8211; Central Authentication Service</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
</head>
<body>
<c:if test="${not empty logoutResponse}">
	<form id="logoutForm" name="logoutForm" method="post" action="${logoutResponse.signoutUrl}">
		<c:if test="${not empty logoutResponse.postData}">
			<input type="hidden" name="logoutRequest" value="${fn:escapeXml(logoutResponse.postData)}" />
		</c:if>
	</form>
</c:if>
</body>
</html>
