jQuery(document).ready(function() {
		jQuery("#DeleteBug").click(function(){
			
			pid = jQuery("#DeleteBug").attr("d");
			var appUrl = ipb.vars['base_url'].replace(/&amp;/g, '&') + 'app=cmtp&module=ajax&section=bugReport';
			
			jQuery.ajax({
			
				type : "POST",
			
				url : appUrl,
			
				data : "do=DeleteBug&id="+pid+"&md5check=" + ipb.vars['md5_hash'],
			
				beforeSend: function(data){
					if(jQuery('#ajax_loading').length == 0)
					{
			
						jQuery('#ipboard_body').prepend( ipb.templates['ajax_loading'] );
			
					}
			
						jQuery('#ajax_loading').fadeIn(100);		
			
				},
			
				success : function(data) {
			
					
			
					if(jQuery("body").find('#ipsGlobalNotification').length == 0) {
			
						jQuery('#ipboard_body').prepend("<div id='ipsGlobalNotification' style='display:none;z-index:10000;'><div class='popupWrapper'><div class='popupInner'><div class='ipsPad'>" + BugDeleted + "</div></div></div></div>");
					}
			
					jQuery("#ipsGlobalNotification").fadeIn(500);
			
					setTimeout(function() {	jQuery('#ipsGlobalNotification').fadeOut(500) }, 1000);
			
					setTimeout(function() {	jQuery('#ipsGlobalNotification').remove() }, 1700);
			
					var appUrl2 = ipb.vars['base_url'].replace(/&amp;/g, '&') + 'app=cmtp&module=ajax&section=bugReport&do=BugsReported&md5check=' + ipb.vars['md5_hash'];
		
					function getData(loc) {
		
					var dispData = jQuery.ajax({
		
						url : loc,
		
						dataType : 'html',
		
						async : false
		
						}).responseText;
		
						return dispData;
					};
		
					jQuery('#BugsReported').html(getData(appUrl2));	
				
				},
				
				complete:function(){
				
					jQuery('#ajax_loading').fadeOut(100);
				
					},
				
					error: function(){
				
					alert( ipb.lang['session_timed_out'] );
				
					window.location.reload();
					}
				});			
			
		});
}); 