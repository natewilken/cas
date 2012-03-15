<jsp:directive.include file="includes/top.jsp" />

	<form:form method="post" id="login" commandName="${commandName}" htmlEscape="true">
	    <form:errors path="*" cssClass="error" id="status" element="div" htmlEscape="false" />
		<h2>Sign In</h2>
		<fieldset class="content">
			<div class="section">
				<div class="label">
					<label for="username">ASURITE User ID:</label>
					<span><a href="https://selfsub.asu.edu/activation">Activate</a> | <a href="http://asu.edu/asuriterequest">Request ID</a></span>
				</div>
				<input class="text" type="text" name="username" id="username" tabindex="1" autocorrect="off" autocapitalize="off" />
			</div>
			<!--  this clear is for some older handhelds and ie6, that don't support overflow:hidden to reset floats -->
			<div style="clear:both"></div>
			<div class="section">
				<div class="label">
					<label for="password">Password:</label>
					<span><a href="https://selfsub.asu.edu/lostpassword">Forgot ID &#047; Password?</a></span>
				</div>
				<input class="text" type="password" name="password" id="password" tabindex="2" autocorrect="off" autocapitalize="off"  />
			</div>
			<!--  this clear is for some older handhelds and ie6, that don't support overflow:hidden to reset floats -->
			<div style="clear:both"></div>
						
			<div id="submit_wrapper">
				<input type="submit" class="submit" value="Sign In" tabindex="3" />
				<label id="remember"><input type="checkbox" id="rememberid" tabindex="4" class="checkbox" /> Remember my User ID</label>
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