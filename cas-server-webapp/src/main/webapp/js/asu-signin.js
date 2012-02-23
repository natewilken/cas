var Cookies = {
	set: function(name, value, expires, path, domain, secure) {
		document.cookie = name + "=" + escape (value) +
        ((expires) ? "; expires=" + expires : "") +
        ((path) ? "; path=" + path : "") +
        ((domain) ? "; domain=" + domain : "") +
        ((secure) ? "; secure" : "");
	},
	get: function(name) {
		var arg = name + "=";
	    var alen = arg.length;
	    var clen = document.cookie.length;
	    var i = 0;
	    while (i < clen) {
	        var j = i + alen;
	        if (document.cookie.substring(i, j) == arg) {
	            return this.getVal(j);
	        }
	        i = document.cookie.indexOf(" ", i) + 1;
	        if (i == 0) break; 
	    }
	    return null;
	},
	getVal: function(offset) {
	    var endstr = document.cookie.indexOf (";", offset);
	    if (endstr == -1) {
	        endstr = document.cookie.length;
	    }
	    return unescape(document.cookie.substring(offset, endstr));
	},
	del: function(name, path, domain, secure) {
		if (this.get(name)) {
			this.set(name, "", "Thu, 01-Jan-70 00:00:01 GMT", path, domain, secure);
		}
	},
	getDate: function(addDays) {
		var exdate=new Date();
		exdate.setDate(exdate.getDate()+addDays);
		return exdate.toUTCString();
	}
};

function addLoadEvent(fn) {
	if (window.addEventListener) window.addEventListener('load',fn,false);
	else if (document.addEventListener) document.addEventListener('load',fn,false);
	else if (window.attachEvent) window.attachEvent('onload',fn);
	else {
		var oldfn = window.onload;
		if (typeof oldfn != 'function') window.onload = fn;
		else window.onload = function() {
			oldfn();
			fn();
		};
	}
}

var ASULogin = {
	// constants
	SSO_COOKIE_USER: "SSO_USERID",
	SSO_COOKIE_REMEMBER: "SSO_REMEMBER",
	CHECKBOX_ID: "rememberid",
	FORM_ID: "login",
	USERINPUT_ID: "username",
	PASSWORD_ID: "password",
	
	// variables
	save_id: false,
	
	// methods
	onLoad: function() {
		if (document.getElementById("remember")) {
			document.getElementById("remember").style.display = "inline-block";
		}
		
		this.checkCookies();
		this.attachListners();
	},
	checkCookies: function() {
		if (Cookies.get(this.SSO_COOKIE_REMEMBER) != null) {
			this.setCheckBox(true);
			this.save_id = true;
			this.focusOnPassword();
		} else {
			this.focusOnUsername();
			return;
		}
		
		var userid = Cookies.get(this.SSO_COOKIE_USER);
		if (userid != null) {
			this.setUserID(userid);
		}
	},
	setCheckBox: function(value) {
		if (document.getElementById(this.CHECKBOX_ID)) {
			document.getElementById(this.CHECKBOX_ID).checked = value;
		}
	},
	attachListners: function() {
		if (document.getElementById(this.CHECKBOX_ID)) {
			document.getElementById(this.CHECKBOX_ID).onclick = function() {
				ASULogin.checkBoxListner(this);
			};
		}
		if (document.getElementById(this.FORM_ID)) {
			document.getElementById(this.FORM_ID).onsubmit = function() {
				ASULogin.formSubmission();
			};
		}
	},
	checkBoxListner: function(checkbox) {
		if (checkbox.checked) {
			Cookies.set(this.SSO_COOKIE_REMEMBER, "true", Cookies.getDate(1826)/*, "", window.location.hostname, false*/);
			this.save_id = true;
		} else {
			Cookies.del(this.SSO_COOKIE_REMEMBER);
			Cookies.del(this.SSO_COOKIE_USER);
			this.save_id = false;
		}
	},
	setUserID: function(value) {
		if (document.getElementById(this.USERINPUT_ID)) {
			document.getElementById(this.USERINPUT_ID).value = value;
		}
	},
	formSubmission: function() {
		if(this.save_id) {
			var id = document.getElementById(this.USERINPUT_ID).value;
			Cookies.set(this.SSO_COOKIE_USER, id, Cookies.getDate(1826)/*, "", window.location.hostname, false*/);
		} else {
			Cookies.del(this.SSO_COOKIE_USER);
		}
	},
	focusOnPassword: function() {
		if (document.getElementById(this.PASSWORD_ID)) {
			document.getElementById(this.PASSWORD_ID).focus();
		}
	},
	focusOnUsername: function() {
		if (document.getElementById(this.USERINPUT_ID)) {
			document.getElementById(this.USERINPUT_ID).focus();
		}
	},
	selectAd: function() {
		// thanks to jonathan wilson for the random image rotator script
		// http://forum.oscr.arizona.edu/showthread.php?t=810
		if (document.getElementById('ad')) {
			var length = 10;
			var ran_num = Math.round((length-1)*Math.random());
			ran_num=ran_num+1;
			var source = "https://www.asu.edu/weblogin/images/ads/"+ran_num+".jpg";
			
			var img = document.createElement('img');
			img.src = source;
			document.getElementById('ad').appendChild(img);
		}
	}
};