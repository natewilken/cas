<%@ page pageEncoding="UTF-8"
%><%@ page contentType="text/html; charset=UTF-8"
%><jsp:directive.include file="includes/top.jsp" />

	<div id="password_warning">
		<div class="content">
			<div class="headline"><spring:message code="screen.passwordwarning.headline" /></div>
			<div class="subtext"><spring:message code="screen.passwordwarning.subtext" arguments="${passwordDaysRemaining}" /></div>
			<div class="more">
				<c:set var="pwExpireDate"><fmt:formatDate type="date" value="${passwordExpirationDate}" dateStyle="long"/></c:set>
				<c:set var="pwExpireTime"><fmt:formatDate type="time" value="${passwordExpirationDate}" timeStyle="short"/></c:set>
				<c:choose>
				<c:when test="${not empty passwordLastChangeDate}">
					<c:set var="pwChangeDate"><fmt:formatDate type="date" value="${passwordLastChangeDate}" dateStyle="long"/></c:set>
					<spring:message code="screen.passwordwarning.more" argumentSeparator=";"
										arguments="${passwordDaysRemaining};${pwChangeDate};${pwExpireDate};${pwExpireTime}" />
				</c:when>
				<c:otherwise>
					<spring:message code="screen.passwordwarning.more.nullchange" argumentSeparator=";"
										arguments="${passwordDaysRemaining};${pwExpireDate};${pwExpireTime}" />
				</c:otherwise>
				</c:choose>
			</div>

			<div id="button_container">
				<form:form id="chpwd" method="post">
					<input type="submit" class="submit" value="Update Now" tabindex="1" />
					<input type="hidden" name="execution" value="${flowExecutionKey}" />
					<input type="hidden" name="_eventId" value="chpwd" />
				</form:form>

				<form:form id="skip" method="post">
					<input type="submit" class="submit" value="Remind me later" tabindex="2" />
					<input type="hidden" name="execution" value="${flowExecutionKey}" />
					<input type="hidden" name="_eventId" value="skip" />
				</form:form>
			</div>
			
		</div>
	</div>

<jsp:directive.include file="includes/bottom.jsp" />

	<script type="text/javascript" src="js/asu-signin.js"></script>
	<script type="text/javascript" src="themes/<spring:theme code="name" />/onload.js"></script>
</body>
</html>