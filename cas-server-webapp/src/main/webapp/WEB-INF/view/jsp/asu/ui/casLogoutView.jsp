<jsp:directive.include file="includes/top.jsp" />

	<div id="logout">
		<div id="logout_pending" class="content">
			<div class="headline loading">Do not close this window.</div>
			<div class="subtext">You are being securely signed out.</div>
		</div>
		<div id="logout_complete" class="content">
			<div class="headline success">You have been signed out.</div>
			<div class="subtext"><a href="https://my.asu.edu/">Sign In</a> to My ASU</div>
		<c:if test="${param['eventSource'] != 'google' and foundGoogleSession == true}">
			<div class="headline warning">You are still signed in to Google.</div>
			<div class="subtext" title="This includes non-ASU Google accounts."><a href="https://www.google.com/accounts/Logout">Sign Out</a> of <em>all</em> Google services</div>
		</c:if>
		</div>
	</div>
	
<jsp:directive.include file="includes/bottom.jsp" />
    
        <iframe src="https://hr.oasis.asu.edu/psp/asuhrprd/?cmd=logout&logoutBounce=" height="0" width="0" frameborder="0"></iframe>
        <iframe src="https://cs.oasis.asu.edu/psp/asucsprd/?cmd=logout&logoutBounce=" height="0" width="0" frameborder="0"></iframe>
<!--
        <iframe src="https://hrsa.oasis.asu.edu/psp/asusaprd/?cmd=logout&logoutBounce=" height="0" width="0" frameborder="0"></iframe>
        <iframe src="https://crm.oasis.asu.edu/psp/asucmprd/?cmd=logout&logoutBounce=" height="0" width="0" frameborder="0"></iframe>
        <iframe src="https://webapp3.asu.edu/myapps/Login/Logout" height="0" width="0" frameborder="0"></iframe>
-->
	
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
							if (typeof window.scrollTo == 'function') window.scrollTo(0, 0);
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
				if (typeof window.scrollTo == 'function') window.scrollTo(0, 0);
			}
		});
	</script>
	<script type="text/javascript" src="themes/<spring:theme code="name" />/onload.js"></script>
	
</body>
</html>