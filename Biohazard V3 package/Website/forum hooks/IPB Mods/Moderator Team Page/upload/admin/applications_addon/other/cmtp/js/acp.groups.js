( function($) {
		
	$.fn.changeDisplayNames = function(){
	
		$("#changedisplayname",this).click(function(){
			
			id 		= $(this).parent().data("ids");
			
			name 	= $(this).parent().data("name")

			$(this).parent().next().fadeIn();
				
			$(this).parent().next().next().fadeIn();
				
			$(this).hide();
				
			$(this).parent().append("<input type='input' size='30' class='input_text' value='"+name+"'>&nbsp;");

		});
	
	};
	
	$.fn.saveDisplayNameChange = function(){

		$("#saveDisplayNameChange",this).click(function(){
			
			name 	= $(this).prev().find("input").val();
			
			id 		= $(this).prev().data("ids");
			
			a = GenAjaxPost("app=cmtp&module=ajax&section=ajax","&newName="+name+"&ordering=Edit&do=addGroup&agroup="+id+"&count=0")
					
			if(a == "yippy"){

				$("#mdisplay").html(GenAjaxPost('app=cmtp&module=ajax&section=ajax&do=displaygroups',0));

			}
			else if(a == "error"){
	
				alert(SomeThingBad);
	
			}
			else if(a == "noperm"){
	
				alert(NoPerms);
	
			}						
	
		});
	
	};	
	$.fn.RessettDisplayNameChange = function(){
	
		$("#RessettDisplayNameChange",this).click(function(){
		
			$(this).prev().hide();
			
			$(this).hide();
			
			name = $(this).prev().prev().data("name");
			
			$(this).prev().prev().find("input").hide();
			
			$(this).prev().prev().find("span").show();
			
		});
	
	};
	
	$.fn.displayed = function(){
	
		$("#displayed",this).click(function(){
	
				id = $(this).data("ids");
	
				count = $(this).data("count") + 1;
	
				if($(this).hasClass("NotDisp")){
					
					a = GenAjaxPost("app=cmtp&module=ajax&section=ajax","&count="+count+"&do=addGroup&agroup="+id)
					
					if(a == "yippy"){
	
						$("#mdisplay").html(GenAjaxPost('app=cmtp&module=ajax&section=ajax&do=displaygroups',0));
	
					}
					else if(a == "error"){
	
						alert(SomeThingBad);
	
					}
					else if(a == "noperm"){
	
						alert(NoPerms);
	
					}	
	
				}
				else if($(this).hasClass("YesDisp")){
					
					if(!confirm("Are you sure you want to remove group from being displayed?")){
					
						return false;
				
					}						
					a = GenAjaxPost("app=cmtp&module=ajax&section=ajax","&do=deleteGroup&agroup="+id)
					
					if(a == "yippy"){
	
						$("#mdisplay").html(GenAjaxPost('app=cmtp&module=ajax&section=ajax&do=displaygroups',0));
	
					}					
					else if(a == "error"){
	
						alert(SomeThingBad);
	
					}
					else if(a == "noperm"){
	
						alert(NoPerms);
	
					}				
	
				}
			
		})
	
	}

	$.fn.openMembers = function (){
		
		$("#theworld").find("#openMembers").each(function(){
	
			id = $(this).data("ids");
			count = $(this).data("count");
	
			if($.cookie('group_'+id) == 1 && count >= 1)
			{
				
				a = GenAjaxPost("app=cmtp&module=ajax&section=ajax","&do=GetMembers&id="+id);
				
				if(a == "error"){
	
					alert(SomeThingBad);
	
				}

				else{
	
					$(this).data("open",1);
					
					$(this).html("x").removeClass("open").addClass("close");
									
					$("#groups_"+id).after("<tr id='new_row_"+id+"'><td colspan='6' style='padding-left:36px;' id='loadMembers"+id+"'></td></tr>");
					
					$("#loadMembers"+id).html(a);	
	
				}				
	
			}
	
		});
		
		$("#openMembers",this).click(function(){
		
			id = $(this).data("ids");
						
			var openMembersCookie = "group_"+id;
			
			if(!$(this).data("open")){
								
				a = GenAjaxPost("app=cmtp&module=ajax&section=ajax","&do=GetMembers&id="+id);
	
				if(a == "error"){
				
					alert(SomeThingBad);
			
				}
				else if(a == "noperm"){
				
					alert(NoPerms);
				
				}
				else{
	
					$.cookie(openMembersCookie,'1',{expires:365,path: '/'}); 
					
					$(this).data("open",1);
					
					$(this).html("x").removeClass("open").addClass("close");
									
					$("#groups_"+id).after("<tr id='new_row_"+id+"'><td colspan='6' style='padding-left:36px;' id='loadMembers"+id+"'></td></tr>");
					
					$("#loadMembers"+id).html(a);	
	
				}
			
			}
			else if(count >= 1){
				
				$.cookie(openMembersCookie,'0',{expires:365,path: '/'});
				
				$(this).html("+").removeClass("close").addClass("open");

				$(this).removeData("open");

				$("#new_row_"+id).remove();
			
			}
		
		});
	}
	
	$.fn.memberDisplay = function(){
		
		$("#memberDisplay",this).click(function(){
				
			id = $(this).data("ids");
			
			group = $(this).data("group");
			
			other = $(this).data("other");

			if($(this).hasClass("YesDisps")){
			
				if(!confirm("Are you sure you want to remove this member?")){
				
					return false;
				
				}	
				
				a = GenAjaxPost("app=cmtp&module=ajax&section=ajax","&do=insertMembers&id="+id+"&group="+group+"&other="+other);
	
				if(a == "success"){
	
					$(this).removeClass("YesDisps").addClass("NotDisps");
					$("#mdisplay").html(GenAjaxPost('app=cmtp&module=ajax&section=ajax&do=displaygroups',0));	
	
				}				
				else if(a == "error"){
	
					alert(SomeThingBad)
	
				}
				else if(a == "noperm"){
	
					alert(NoPerms);
	
				}
	
			}
			else if($(this).hasClass("NotDisps")){

				a = GenAjaxPost("app=cmtp&module=ajax&section=ajax","&do=delmembers&id="+id+"&group="+group);
	
				if(a == "success"){
	
					$(this).removeClass("NotDisps").addClass("YesDisps");
				}
				else if(a == "error"){
	
					alert(SomeThingBad)
	
				}
				else if(a == "noperm"){
	
					alert(NoPerms);
	
				}
	
			}		
			
		})
	}
	
	$.fn.PreviewSort = function PreviewSort(type, options) {
		var defaults = {
			'main': {
				handle: 'td .draghandle',
				items: 'tr.ipsControlRow',
				opacity: 0.6,
				revert: false,
				tolerance:'pointer',
				sendType: 'get',
				distance:10,
				forceHelperSize: true,
				helper: 'clone',	
				axis:'y',
				scrollSensitivity:20,
				scrollSpeed:2
			}
		};
			
		return this.each(function(){
			var x = ( $.undefined( defaults[type] ) ) ? defaults['custom'] : defaults[type];
			var o = $.extend( x, options );
			
			// If there's no function defined already, and a URL is specified, set up
			// an ajax call
			if( $.undefined( o['update'] ) && !$.undefined( o['url'] ) )
			{ 
				o['update'] = function( e, ui )
				{
					if ( ! Object.isUndefined( o['callBackUrlProcess'] ) )
					{
						o['url'] = o['callBackUrlProcess']( o['url'] );
					}
				
					var serial = $(this).sortable("serialize", ( o['serializeOptions'] || {} ) );
					
					$.ajax( {
						url: o['url'].replace( /&amp;/g, '&' ),
						type: o['sendType'],
						data: serial,
						processData: false,
						beforeSend: function(data){
							if($('#ajax_loading').length == 0)
							{
								$('#ipboard_body').prepend( ipb.templates['ajax_loading'] );
							}
							$('#ajax_loading').fadeIn(100);		
						},
						success: function(data){
							
							if( data['error'] )
							{
								alert( data['error'] );
								if( data["__session__expired__log__out__"] ){
									window.location.reload();
								}
							}
							else
							{
								Debug.write("Sortable update successfully posted");
								
								if($("body").find('#ipsGlobalNotification').length == 0) {

								$('#ipboard_body').prepend("<div id='ipsGlobalNotification' style='display:none;z-index:10000;'><div class='popupWrapper'><div class='popupInner'><div class='ipsPad'>" + jsReorder + "</div></div></div></div>");

							}

							$("#ipsGlobalNotification").fadeIn(500);
							}

							if ( ! Object.isUndefined( o['postUpdateProcess'] ) )
							{
								o['postUpdateProcess']( data );
								
							}
						},
						complete:function(){

							$('#ajax_loading').fadeOut(100);
							setTimeout(function() {	$('#ipsGlobalNotification').fadeOut(500) }, 1000);
							setTimeout(function() {	$('#ipsGlobalNotification').remove()}, 1700);
						},
						error: function(){
							alert( ipb.lang['session_timed_out'] );
							window.location.reload();
						}
					});
				}
			}
			
			     $(this ).sortable( o );

		});
		
	};
	
    $.cookie = function(key, value, options) {

        // key and at least value given, set cookie...
        if (arguments.length > 1 && (!/Object/.test(Object.prototype.toString.call(value)) || value === null || value === undefined)) {
            options = $.extend({}, options);

            if (value === null || value === undefined) {
                options.expires = -1;
            }

            if (typeof options.expires === 'number') {
                var days = options.expires, t = options.expires = new Date();
                t.setDate(t.getDate() + days);
            }

            value = String(value);

            return (document.cookie = [
                encodeURIComponent(key), '=', options.raw ? value : encodeURIComponent(value),
                options.expires ? '; expires=' + options.expires.toUTCString() : '', // use expires attribute, max-age is not supported by IE
                options.path    ? '; path=' + options.path : '',
                options.domain  ? '; domain=' + options.domain : '',
                options.secure  ? '; secure' : ''
            ].join(''));
        }
        // key and possibly options given, get cookie...
        options = value || {};
        var decode = options.raw ? function(s) { return s; } : decodeURIComponent;

        var pairs = document.cookie.split('; ');
        for (var i = 0, pair; pair = pairs[i] && pairs[i].split('='); i++) {
            if (decode(pair[0]) === key) return decode(pair[1] || ''); // IE saves cookies with empty string as "c; ", e.g. without "=" as opposed to EOMB, thus pair[1] may be undefined
        }
        return null;
    };		
    
}(jQuery));

function GenAjaxPost(loc,dat) {

	url = ipb.vars['base_url'].replace(/&amp;/g, '&')+loc+'&md5check='+ipb.vars['md5_hash'];

	var ReturnData = jQuery.ajax({

		type : "POST",

		url : url,

		data: dat,

		processData: false,

		async : false

	}).responseText;

	return ReturnData;

};
