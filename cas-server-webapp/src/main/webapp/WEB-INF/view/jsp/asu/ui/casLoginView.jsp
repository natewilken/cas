<jsp:directive.include file="includes/top.jsp" />

	<form:form method="post" id="login" commandName="${commandName}" htmlEscape="true">
		<h2>Sign In</h2>
	    <form:errors path="*" cssClass="error" id="status" element="div" htmlEscape="false" />
		<fieldset class="content">
			<div class="section">
				<label for="username">ASURITE User ID:</label>
				<input class="text" type="text" name="username" id="username" tabindex="1" autocorrect="off" autocapitalize="off" />
				<div class="hint"><a href="https://selfsub.asu.edu/activation">Activate</a> or <a href="http://links.asu.edu/asuriteid-request">Request an ID</a></div>
			</div>
			<div class="section">
				<label for="password">Password:</label>
				<input class="text" type="password" name="password" id="password" tabindex="2" autocorrect="off" autocapitalize="off"  />
				<div class="hint"><a href="https://selfsub.asu.edu/lostpassword">Forgot ID / Password?</a></div>
			</div>
			<div id="login_submit">
				<input type="submit" class="submit" value="Sign In" tabindex="3" />
				<label id="rememberid_label" for="rememberid">
					<input class="checkbox" type="checkbox" name="rememberid" id="rememberid" tabindex="4" />
					<span>Remember My User ID</span>
				</label>
			</div>
			<input type="hidden" name="lt" value="${loginTicket}" />
			<input type="hidden" name="execution" value="${flowExecutionKey}" />
			<input type="hidden" name="_eventId" value="submit" />
		</fieldset>
	</form:form>

<jsp:directive.include file="includes/bottom.jsp" />
	
	<script type="text/javascript" src="js/asu-signin.js"></script>
	<script type="text/javascript" src="themes/<spring:theme code="name" />/onload.js"></script>
</body>
</html>