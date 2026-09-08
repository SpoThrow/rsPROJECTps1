jQ(document).ready(function() {
		var appUrl2 = ipb.vars['base_url'].replace(/&amp;/g, '&') + 'app=cmtp&module=ajax&section=bugReport&do=BugsReported&md5check=' + ipb.vars['md5_hash'];
		
		function getData(loc) {
		
			var dispData = jQ.ajax({

				url : loc,

				dataType : 'html',

				async : false

				}).responseText;

				return dispData;
			};


		jQ('#BugsReported').html(getData(appUrl2));	
		
		jQ("#startBugReport").click(function(){

			jQ("#GradeA").hide();

			

			if(jQ("body").find('#ipsGlobalNotification').length == 0) {

			jQ('#ipboard_body').prepend("<div id='ipsGlobalNotification' style='display:none;z-index:10000;'><div class='popupWrapper'><div class='popupInner'><div class='ipsPad'>" + startBugReport + "</div></div></div></div>");}

			jQ("#ipsGlobalNotification").fadeIn(500);

			setTimeout(function() {	jQ('#ipsGlobalNotification').fadeOut(500) }, 1000);

			setTimeout(function() {	jQ('#ipsGlobalNotification').remove() }, 1700);

			jQ("#GradeB").show();

		});

		jQ("#GrabBug").click(function(){
			
			msg = jQ("#bugReport").val();
			
			contact = jQ('#contact').val();
			
			function isValidEmailAddress(emailAddress) {
    			var pattern = new RegExp(/^(("[\w-+\s]+")|([\w-+]+(?:\.[\w-+]+)*)|("[\w-+\s]+")([\w-+]+(?:\.[\w-+]+)*))(@((?:[\w-+]+\.)*\w[\w-+]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$)|(@\[?((25[0-5]\.|2[0-4][\d]\.|1[\d]{2}\.|[\d]{1,2}\.))((25[0-5]|2[0-4][\d]|1[\d]{2}|[\d]{1,2})\.){2}(25[0-5]|2[0-4][\d]|1[\d]{2}|[\d]{1,2})\]?$)/i);
   				 return pattern.test(emailAddress);
			};
	
			if (!isValidEmailAddress( contact )){
			
				alert(NotAValidEmail);
			
				jQ('#contact').css("border-color","red");
			
				return;
			}		
			
			rmsg = msg.gsub('&nbsp;','').stripTags();
			
			if(rmsg.blank())
			{
			
				alert(BugReportBlank);

				return;
			}

			var appUrl = ipb.vars['base_url'].replace(/&amp;/g, '&') + 'app=cmtp&module=ajax&section=bugReport';
			
			jQ.ajax({
			
				type : "POST",
			
				url : appUrl,
			
				data : "do=startBugReport&msg="+msg+"&contact="+contact+"&md5check=" + ipb.vars['md5_hash'],
			
				beforeSend: function(data){
					if(jQ('#ajax_loading').length == 0)
					{
			
						jQ('#ipboard_body').prepend( ipb.templates['ajax_loading'] );
			
					}
			
						jQ('#ajax_loading').fadeIn(100);		
			
				},
			
				success : function(data) {
			
					
			
					if(jQ("body").find('#ipsGlobalNotification').length == 0) {
			
						jQ('#ipboard_body').prepend("<div id='ipsGlobalNotification' style='display:none;z-index:10000;'><div class='popupWrapper'><div class='popupInner'><div class='ipsPad'>" + BugReportSent + "</div></div></div></div>");
					}
			
					jQ("#ipsGlobalNotification").fadeIn(500);
			
					setTimeout(function() {	jQ('#ipsGlobalNotification').fadeOut(500) }, 1000);
			
					setTimeout(function() {	jQ('#ipsGlobalNotification').remove() }, 1700);
			
					jQ("#GradeB").hide();
			
					jQ("#GradeC").show();
		
					var appUrl2 = ipb.vars['base_url'].replace(/&amp;/g, '&') + 'app=cmtp&module=ajax&section=bugReport&do=BugsReported&md5check=' + ipb.vars['md5_hash'];
					
					function getData(loc) {
					
						var dispData = jQ.ajax({
			
							url : loc,
			
							dataType : 'html',
			
							async : false
			
							}).responseText;
			
							return dispData;
						};
			
			
					jQ('#BugsReported').html(getData(appUrl2));	
				
				},
				
				complete:function(){
				
					jQ('#ajax_loading').fadeOut(100);
				
					},
				
					error: function(){
				
					alert( ipb.lang['session_timed_out'] );
				
					window.location.reload();
					}
				});
			});
});
