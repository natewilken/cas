<jsp:directive.include file="includes/top.jsp" />

	<div id="logout">
		<div class="content">
			<div id="logout_pending">
				<div class="headline"><img src="images/loader_trans.gif" /> <span>Do not close this window.</span></div>
				<div class="subtext">You are being securely signed out.</div>
			</div>
			<div id="logout_complete">
				<div class="headline"><img src="images/green_checkmark.png" /> <span>You have been signed out.</span></div>
				<div class="subtext"><a href="https://my.asu.edu/">Sign In</a> <span>to My ASU</span></div>
				<c:if test="${param['eventSource'] != 'google' and foundGoogleSession == true}">
					<div class="headline"><img src="images/icon_attn.png" /> <span>You are still signed in to Google.</span></div>
					<div class="subtext" title="This includes non-ASU Google accounts."><a href="https://www.google.com/accounts/Logout">Sign Out</a> of <em>all</em> Google services</span></div>
				</c:if>
			</div>
		</div>
	</div>
	
<jsp:directive.include file="includes/bottom.jsp" />
    
	<iframe src="https://hrsa.oasis.asu.edu/psp/asusaprd/?cmd=logout&logoutBounce=" height="0" width="0" frameborder="0"></iframe>
	<iframe src="https://crm.oasis.asu.edu/psp/asucmprd/?cmd=logout&logoutBounce=" height="0" width="0" frameborder="0"></iframe>
	<iframe src="https://webapp3.asu.edu/myapps/Login/Logout" height="0" width="0" frameborder="0"></iframe>
	
	<c:if test="${not empty logoutResponseKeys}">
		<c:forEach var="logoutResponseKey" items="${logoutResponseKeys}">
			<c:url var="frameUrl" value="logoutCallback">
				<c:param name="k" value="${logoutResponseKey}"/>
			</c:url>
			<iframe class="cas-logout-callback" src="${frameUrl}" height="0" width="0" frameborder="0"></iframe>
		</c:forEach>
	</c:if>

	<script type="text/javascript" src="js/asu-signin.js"></script>
	<script type="text/javascript">
		addLoadEvent(function() {
			var frames = document.getElementsByTagName('iframe');
			var framesDone = 0;
			var watchCount = 0;
			for (var i = 0; i < frames.length; i++) {
				var fE = frames.item(i);
				if (fE.className == 'cas-logout-callback') {
					watchCount++;
					var updateProgress = function() {
						framesDone++;
						if (framesDone == watchCount) {
							document.getElementById('logout_pending').style.display = 'none';
							document.getElementById('logout_complete').style.display = 'block';
						}
					};
					if (fE.attachEvent) fE.attachEvent('onload', updateProgress);
					else {
						var oldfn = fE.onload;
						if (typeof oldfn != 'function') fE.onload = updateProgress;
						else fE.onload = function() {
							oldfn();
							updateProgress();
						};
					}
					var odoc = (fE.contentWindow || fE.contentDocument);
					if (odoc.document) odoc = odoc.document;
					odoc.forms.logoutForm.submit();
				}
			}
			if (watchCount == 0) {
				document.getElementById('logout_pending').style.display = 'none';
				document.getElementById('logout_complete').style.display = 'block';
			}
		});
	</script>
	<script type="text/javascript" src="themes/<spring:theme code="name" />/onload.js"></script>
	
</body>
</html>