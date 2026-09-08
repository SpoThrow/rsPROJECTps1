/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

var Resize =
{
	obj     : null,
	objloop : null,
	int     : null,

	init: function(o, oRoot, ho, wo, minX, maxX, minY, maxY)
	{
		o.onmousedown = Resize.start;
		o.hmode       = true;
		o.vmode       = true;
		o.root        = (oRoot && oRoot != null) ? oRoot : o;

		if (o.hmode  && isNaN(parseInt(o.root.style.left  ))) o.root.style.left   = "0px";
		if (o.vmode  && isNaN(parseInt(o.root.style.top   ))) o.root.style.top    = "0px";
		if (!o.hmode && isNaN(parseInt(o.root.style.right ))) o.root.style.right  = "0px";
		if (!o.vmode && isNaN(parseInt(o.root.style.bottom))) o.root.style.bottom = "0px";

		o.minX = (typeof minX != 'undefined') ? minX : null;
		o.minY = (typeof minY != 'undefined') ? minY : null;
		o.maxX = (typeof maxX != 'undefined') ? maxX : null;
		o.maxY = (typeof maxY != 'undefined') ? maxY : null;

		o.h_only = false;
		o.w_only = false;

		o.root.Resizing = new Function();

		ho = (ho == true) ? true : false;
		wo = (wo == true) ? true : false;

		if (ho == true)
		{
			o.h_only = true;
		}
		else if (wo == true)
		{
			o.w_only = true;
		}
	},

	start: function(e)
	{
		var o = Resize.obj = Resize.objloop = this;
		e     = Resize.fixE(e);
		var y = parseInt((o.vmode) ? o.root.style.top  : o.root.style.bottom);
		var x = parseInt((o.hmode) ? o.root.style.left : o.root.style.right );

		o.lastMouseX = o.startMouseX = e.clientX;
		o.lastMouseY = o.startMouseY = e.clientY;

		var obj = Resize.data();
		var rec = ipb.shoutbox.rect(obj.x, obj.y, obj.w, obj.h);
		o.oh    = rec.h;
		o.ow    = rec.w;

		if (o.hmode)
		{
			if (o.minX != null) o.minMouseX	= e.clientX - x + o.minX;
			if (o.maxX != null) o.maxMouseX	= o.minMouseX + o.maxX - o.minX;
		}
		else
		{
			if (o.minX != null) o.maxMouseX = -o.minX + e.clientX + x;
			if (o.maxX != null) o.minMouseX = -o.maxX + e.clientX + x;
		}

		if (o.vmode)
		{
			if (o.minY != null) o.minMouseY	= e.clientY - y + o.minY;
			if (o.maxY != null) o.maxMouseY	= o.minMouseY + o.maxY - o.minY;
		}
		else
		{
			if (o.minY != null) o.maxMouseY = -o.minY + e.clientY + y;
			if (o.maxY != null) o.minMouseY = -o.maxY + e.clientY + y;
		}

		document.onmousemove = Resize.resize;
		document.onmouseup   = Resize.end;

		return false;
	},

	resize : function(e)
	{
		e     = Resize.fixE(e);
		var o = Resize.obj;

		var ey	= e.clientY;
		var ex	= e.clientX;
		var y   = parseInt(o.vmode ? o.root.style.top  : o.root.style.bottom);
		var x   = parseInt(o.hmode ? o.root.style.left : o.root.style.right );
		var h   = parseInt(o.root.offsetHeight);
		var t   = (document.all) ? ipb.shoutbox.truebody().scrollTop : window.pageYOffset;

		if (o.minX != null) ex = o.hmode ? Math.max(ex, o.minMouseX) : Math.min(ex, o.maxMouseX);
		if (o.maxX != null) ex = o.hmode ? Math.min(ex, o.maxMouseX) : Math.max(ex, o.minMouseX);
		if (o.minY != null) ey = o.vmode ? Math.max(ey, o.minMouseY) : Math.min(ey, o.maxMouseY);
		if (o.maxY != null) ey = o.vmode ? Math.min(ey, o.maxMouseY) : Math.max(ey, o.minMouseY);

		var rec = Resize.data();
		if (Resize.obj.h_only == true)
		{
			ajh     = ey-o.startMouseY;
			rec.h   = o.oh+ajh;

			if (!isNaN(o.root.min_height) && o.root.min_height > 0)
			{
				if (rec.h < o.root.min_height)
				{
					rec.h = o.root.min_height;
				}
			}

			rec.ho = true;
			if (ey >= 0 && ey <= 3)
			{
				Resize.int = setInterval(Resize.resizeloop, 1);
                window.scrollBy(0, -3);
			}
			else
			{
				if (Resize.int)
				{
					clearInterval(Resize.int);
				}
			}

			Resize.obj.root.style['height'] = rec.h+'px';
		}
		else if (Resize.obj.w_only == true)
		{
			ajw     = ex-o.startMouseX;
			rec.w   = o.ow+ajw;

			if (!isNaN(o.root.min_width) && o.root.min_width > 0)
			{
				if (rec.w < o.root.min_width)
				{
					rec.w = o.root.min_width;
				}
			}

			rec.wo = true;
			if (!isNaN(o.root.max_width) && o.root.max_width > 0)
			{
				if (rec.w > o.root.max_width)
				{
					rec.w = o.root.max_width;
				}
			}

			Resize.obj.root.style['width'] = rec.w+'px';
		}

		Resize.obj.lastMouseX	        = ex;
		Resize.obj.lastMouseY	        = ey;
		Resize.obj.root.Resizing(rec);

		return false;
	},

	resizeloop : function(e)
	{
		Resize.obj = Resize.obj_loop;
		Resize.resize(e);
	},

	end : function(e)
	{
		document.onmousemove = null;
		document.onmouseup   = null;

		Resize.obj.root.Resize_end(Resize.data());

		Resize.obj     = null;
		Resize.objloop = null;

		if (Resize.int)
		{
			clearInterval(Resize.int);
		}
	},

	data : function(e)
	{
		var oo = Resize.obj.root;
		var xx = Resize.style(oo, 'left');
		var yy = Resize.style(oo, 'top');
		var ww = Resize.style(oo, 'width');
		var hh = Resize.style(oo, 'height');

		if (hh <= 0)
		{
			hh = oo.offsetHeight;
		}

		if (ww <= 0)
		{
			ww = oo.offsetWidth;
		}

		rect = ipb.shoutbox.rect( xx, yy, ww, hh );
		return rect;
	},

	style : function(o, n)
	{
		if (!o)
		{
			return 0;
		}

		if (!o.style)
		{
			return 0;
		}

		var t;
		var s = o.style;

		try
		{
			eval("t = parseInt(s."+n+", 10);");
		}

		catch(e)
		{
			return 0;
		}

		if (isNaN(t))
		{
			t=0;
		}

		return t;
	},

	fixE : function(e)
	{
		if (typeof e == 'undefined') e = window.event;
		if (typeof e.layerX == 'undefined') e.layerX = e.offsetX;
		if (typeof e.layerY == 'undefined') e.layerY = e.offsetY;
		return e;
	}
};

/**
 * Core javascript class
 *
 * Contains global functions
 */
window.IPBoard.prototype.shoutbox=
{
	/* Paths to fix cross-site AJAX issues */
	realBaseUrl: 	location.protocol + '//' + location.host,
	realBaseUrlWww:	location.protocol + '//www.' + location.host + location.pathname + '?',

	/* Normal Base URL which PHP uses */
	baseUrl:		ipb.vars['base_url'] + '&app=shoutbox&module=ajax&section=coreAjax&secure_key=' + ipb.vars['secure_hash'] + '&',

	/**
	 * Member specific variables
	 */
	can_use:			0,
	can_edit: 			0,
	members_refresh:	15,
	shouts_refresh:		30,
	hide_refresh:		1,
	flood_limit:		0,
	bypass_flood:		0,
	my_last_shout:		0,
	total_shouts:		0,
	last_shout_id:		0,
	inactive_timeout:	5,

	/**
	 * OTHER: Array variables
	 */
	errors:			[],
	langs:			[],
	month_days:		[31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31],
	langs:			[],

	/**
	 * OTHER: Boolean variables
	 */
	global_on:			false,
	enable_cmds:		true,
	mod_in_action:		false,
	moderator:			false,
	_inactive:			false,
	view_archive:		false,
	events_loaded:		false,
	events_rte_loaded:	false,
	submittingShout:	false,
	inactiveWhenPopup:	false,
	animatingGlobal:	false,
	archive_filtering:	false, //Prevent too much filter requests to be done

	/**
	 * OTHER: Integer variables
	 */
	mod_shout_id:			0,
	time_minute:			60 * 1000,
	events_load_max_tries:	10,
	events_load_tries:		0,

	/**
	 * OTHER: Other variables
	 */
	timeoutShouts:	null,
	timeoutMembers:	null,
	mod_command:	'',
	tempShout:		null, //1.1.0 RC1

	/**
	 * Javascript functions
	 *
	 * Code below this comment are functions
	 * used within the javascript
	 */
	initialize:	function()
	{
		Debug.write( 'IP.Shoutbox javascript is loading' );

		document.observe( 'dom:loaded', function()
		{
			// Is global shoutbox?
			if ( ipb.shoutbox.shoutboxGLOBAL )
			{
				ipb.shoutbox.global_on = true;
			}

			/**
			 * Sort out AJAX URLS
			 *
			 * This fixes a bug as AJAX treats http://
			 * and http://www. as two seperate URLs
			 */

			/* Without www */
			if ( ipb.shoutbox.realBaseUrl.match( /^http:\/\/www/ ) && ! ipb.vars['base_url'].match( /^http:\/\/www/ ) )
			{
				ipb.vars['base_url'] = ipb.vars['base_url'].replace( /^http:\/\//, 'http://www.' );
			}

			/* With www */
			if ( ipb.vars['base_url'].match( /^http:\/\/www/ ) && ! ipb.shoutbox.realBaseUrl.match( /^http:\/\/www/ ) )
			{
				location.href = location.href.replace( /^http:\/\//, 'http://www.' );
			}

			ipb.shoutbox.setupShoutbox();
		}.bind(this));
	},

	populateSmilies: function(element, e)
	{
		Event.stop(e);

		if ( Object.isUndefined( ipb.global.popups['sb_smilies'] ) )
		{
			ipb.global.popups['sb_smilies'] = true;
			ipb.menus.closeAll(e);
			$(element).identify();

			/* Create pop-up wrapper */
			$('ipboard_body').insert( { bottom: new Element('div', { 'id': 'shoutbox-smilies-button_menucontent'}) } );
			$('shoutbox-smilies-button_menucontent').setStyle('width: 400px').update( "<div class='ipsPad ipsForm_center'><img src='" + ipb.vars['loading_img'] + "' /></div>" );

			var _newMenu = new ipb.Menu( $(element), $( "shoutbox-smilies-button_menucontent" ) );
			_newMenu.doOpen();

			new Ajax.Request
			(
				ipb.shoutbox.baseUrl + 'type=smilies',
				{
					method: 'post',
					evalJSON: 'force',
					hideLoader: true,
					parameters: {
						secure_key: ipb.vars['secure_hash']
					},
					onSuccess: function(t)
					{
						$('shoutbox-smilies-button_menucontent').update( t.responseText );
					}
				}
			);
		}

		return false;
	},

	emoticonPager: function(e, elem)
	{
		Event.stop(e);
		var _pg = elem.id.replace( 'page_', '' );

		new Ajax.Request
		(
			ipb.shoutbox.baseUrl + 'type=smilies',
			{
				method: 'post',
				evalJSON: 'force',
				hideLoader: true,
				parameters: {
					secure_key: ipb.vars['secure_hash'],
					pg: _pg
				},
				onSuccess: function(t)
				{
					$('shoutbox-smilies-button_menucontent').update( t.responseText );
				}
			}
		);
	},

	insertMember: function(e, elem)
	{
		Event.stop(e);

		var to_insert = "@" + elem.readAttribute("data-store") + " ";

		if ( ipb.shoutbox.global_on )
		{
			ipb.shoutbox.insertAtCursor( to_insert );
		}
		else
		{
			d = ipb.textEditor.getEditor().CKEditor.getData();
			ipb.textEditor.getEditor().CKEditor.setData( d + to_insert.replace( ' ', '&nbsp;' ) );
			ipb.textEditor.getEditor().CKEditor.focus();
		}
	},

	deleteShout: function(e, elem)
	{
		/* Check to make sure */
		if ( confirm( ipb.shoutbox.langs['sure_delete_shout'] ) )
		{
			/* INIT */
			Event.stop(e);
			var id = elem.id.replace( 'delete_', '' );

			/* Send the ajax request */
			new Ajax.Request( ipb.shoutbox.baseUrl + 'type=delete',
			{
				method: 'post',
				encoding: ipb.vars['charset'],
				parameters: {
					shout: id
				},
				onSuccess: function(s)
				{
					if ( s.responseJSON['error'] )
					{
						/* Display the correct error message */
						switch ( s.responseJSON['error'] )
						{
							case 'noshout':
								alert( ipb.shoutbox.langs['no_found_shout'] );
							break;
							case 'nopermission':
								alert( ipb.shoutbox.langs['no_delete_shout'] );
							break;
						}
					}
					else
					{
						/* Remove that row */
						$('shout-row-' + s.responseJSON['id']).remove();
					}
				}
			});
		}
	},

	setupShoutbox: function()
	{
		var doReload = true;

		/* Allowed to use it? =O */
		if ( ipb.shoutbox.can_use )
		{
			/* At-member */
			ipb.delegate.register( '.at_member', ipb.shoutbox.insertMember );

			/* Delete my shout */
			ipb.delegate.register( '.deleteMyShout', ipb.shoutbox.deleteShout );

			/** Init buttons **/
			var bts = [
				[ 'shoutbox-refresh-button', ipb.shoutbox.refreshShouts ],
				[ 'shoutbox-submit-button' , ipb.shoutbox.submitShout ],
				[ 'shoutbox-clear-button'  , ipb.shoutbox.clearShout ],
				[ 'shoutbox-myprefs-button', ipb.shoutbox.myPrefsLoad ],
				[ 'shoutbox-popup-button'  , ipb.shoutbox.showShoutboxPopup ]
			];

			/** Global SB?  **/
			if ( ipb.shoutbox.global_on )
			{
				/* Setup global resizer */
				ipb.shoutbox.resizeGlobalShouts();

				/* Add also BBCODE button */
				bts.push( [ 'shoutbox-bbcode-button' , ipb.shoutbox.bbcodePopup ] );

				// Setup our global SB toggle
				ipb.shoutbox.setupToggle();

				/* Emoticon pager */
				ipb.delegate.register( '.emoticonPager', ipb.shoutbox.emoticonPager );
			}
			else
			{
				ipb.shoutbox.resizeShouts();

				/* Archive time =O */
				if ( ipb.shoutbox.view_archive && $('load-shoutbox-archive') )
				{
					bts.push( [ 'load-shoutbox-archive' , ipb.shoutbox.displayArchive ] );
				}
			}

			/**
			 * 1.1.0 Beta 2
			 * Setup onlick for all buttons
			 */
			for ( var x=0; x<bts.length; x++ )
			{
				if ( $( bts[x][0] ) )
				{
					$( bts[x][0] ).observe('click', bts[x][1]);
				}
			}
		}
		else
		{
			/* Reset some vars to be sure */
			ipb.shoutbox.myMemberID   = 0;
			ipb.shoutbox.moderator    = 0;
			ipb.shoutbox.hide_refresh = 0;
			ipb.shoutbox.view_archive = false;
		}

		/** Sort other things **/
		ipb.shoutbox.initEvents();

		/**
		 * 1.0.0 Final
		 * Update shouts view (class, scroll, etc)
		 */
		ipb.shoutbox.updateLastActivity();
		ipb.shoutbox.updateJSPreferences();
		ipb.shoutbox.rewriteShoutClasses();
		ipb.shoutbox.shoutsGetLastID();
		ipb.shoutbox.shoutsScrollThem();

		if( ipb.shoutbox.global_on )
		{
			/**
			 * 1.1.0 RC 1
			 * Let's update live active users on board index! =D
			 */
			if ( $('shoutbox-active-total') && $('shoutbox-active-total').hasClassName('ajax_update') )
			{
				ipb.shoutbox.reloadMembers(false);
			}

			/* Block refresh if collapsed */
			if ( $('category_shoutbox') && $('category_shoutbox').hasClassName('collapsed') )
			{
				doReload = false;
			}

			if ( $('shoutbox_sidebar') && !$('shoutbox_sidebar').down('ul').visible() )
			{
				doReload = false;
			}
		}
		else
		{
			/**
			 * 1.1.0 Beta 2
			 * Set timer, we load members on display
			 */
			ipb.shoutbox.reloadMembers(false);
		}

		/**
		 * 1.0.0 Final
		 * Set timer, we load shouts on display
		 */
		if ( doReload )
		{
			ipb.shoutbox.reloadShouts(false);
		}
	},

	checkForCommands: function()
	{
		var s = ipb.shoutbox.getShout( true ),
			a = s.split(' '),
			m = new Array();

		if ( !ipb.shoutbox.validCommandSyntax( a[0], true  ))
		{
			return 'doshout';
		}

		if ( !ipb.shoutbox.enable_cmds )
		{
			ipb.shoutbox.produceError('no_cmds_enabled');
			return null;
		}
		else
		{
			switch (a[0])
			{
				case '/announce':
					//Let's clear the shout there
					ipb.shoutbox.clearShout();

					if ( ipb.shoutbox.can_access_acp )
					{
						new Ajax.Request( ipb.shoutbox.baseUrl + 'type=announce',
							{
								method: 'post',
								encoding: ipb.vars['charset'],
								parameters: {
									announce: s.substring(9).encodeParam()
								},
								onSuccess: function(s)
								{
									if ( ipb.shoutbox.checkForErrors(s.responseText) )
									{
										return false;
									}

									if ( s.responseText == '<!--nothing-->' || s.responseText == '' )
									{
										$('shoutbox-announcement-row').hide();
									}
									else
									{
										$('shoutbox-announcement-row').show();
										$('shoutbox-announcement-text').update( s.responseText );
									}

									return true;
								}
							}
						);
					}
					else
					{
						ipb.shoutbox.produceError('no_acp_access');
					}
					break;
				/**
				 * RC1
				 * Prune old shouts
				 */
				case '/prune':
					if ( ipb.shoutbox.can_access_acp )
					{
						var days = s.substring(6);

						if ( !isNaN(days) && days != '' )
						{
							ipb.shoutbox.clearShout();

							new Ajax.Request( ipb.shoutbox.baseUrl + 'type=prune',
								{
									method: 'post',
									encoding: ipb.vars['charset'],
									parameters: {
										days:		days
									},
									onSuccess: function(s)
									{
										if ( ipb.shoutbox.checkForErrors(s.responseText) )
										{
											return false;
										}

										/**
										 * 1.1.0 Final
										 *
										 * Reset this value just in case
										 * the page is not reloaded
										 */
										ipb.shoutbox.last_shout_id = 0;

										ipb.shoutbox.actionTaken(s.responseText);

										/**
										 * 1.1.0 Beta 1
										 * Reload page if shouts are pruned
										 */
										window.location=window.location;
									}
								}
							);
						}
						else
						{
							ipb.shoutbox.produceError('prune_invalid_number');
						}
					}
					else
					{
						ipb.shoutbox.clearShout();
						ipb.shoutbox.produceError('no_acp_access');
					}
					break;
				/**
				 * RC1
				 * Ban members
				 */
				case '/ban':
					if (ipb.shoutbox.mod_perms['m_ban_members'])
					{
						var banName = s.substring(4);

						if ( banName != null && banName != '' )
						{
							ipb.shoutbox.clearShout();

							new Ajax.Request( ipb.shoutbox.baseUrl + 'type=ban',
								{
									method: 'post',
									encoding: ipb.vars['charset'],
									parameters: {
										name:		banName
									},
									onSuccess: function(s)
									{
										if ( ipb.shoutbox.checkForErrors(s.responseText) )
										{
											return false;
										}

										ipb.shoutbox.actionTaken(s.responseText);
									}
								}
							);
						}
						else
						{
							ipb.shoutbox.produceError('mod_invalid_name');
						}
					}
					else
					{
						ipb.shoutbox.clearShout();
						ipb.shoutbox.produceError('mod_no_perm');
					}
					break;
				case '/ban24':
					if (ipb.shoutbox.mod_perms['m_ban_members'])
					{
						var banName = s.substring(4);

						if ( banName != null && banName != '' )
						{
							ipb.shoutbox.clearShout();

							new Ajax.Request( ipb.shoutbox.baseUrl + 'type=ban24',
								{
									method: 'post',
									encoding: ipb.vars['charset'],
									parameters: {
										name:		banName
									},
									onSuccess: function(s)
									{
										if ( ipb.shoutbox.checkForErrors(s.responseText) )
										{
											return false;
										}

										ipb.shoutbox.actionTaken(s.responseText);
									}
								}
							);
						}
						else
						{
							ipb.shoutbox.produceError('mod_invalid_name');
						}
					}
					else
					{
						ipb.shoutbox.clearShout();
						ipb.shoutbox.produceError('mod_no_perm');
					}
					break;
				case '/ban48':
					if (ipb.shoutbox.mod_perms['m_ban_members'])
					{
						var banName = s.substring(4);

						if ( banName != null && banName != '' )
						{
							ipb.shoutbox.clearShout();

							new Ajax.Request( ipb.shoutbox.baseUrl + 'type=ban48',
								{
									method: 'post',
									encoding: ipb.vars['charset'],
									parameters: {
										name:		banName
									},
									onSuccess: function(s)
									{
										if ( ipb.shoutbox.checkForErrors(s.responseText) )
										{
											return false;
										}

										ipb.shoutbox.actionTaken(s.responseText);
									}
								}
							);
						}
						else
						{
							ipb.shoutbox.produceError('mod_invalid_name');
						}
					}
					else
					{
						ipb.shoutbox.clearShout();
						ipb.shoutbox.produceError('mod_no_perm');
					}
					break;
				/**
				 * RC1
				 * Unban members
				 */
				case '/unban':
					if (ipb.shoutbox.mod_perms['m_unban_members'])
					{
						var unbanName = s.substring(6);

						if ( unbanName != null && unbanName != '' )
						{
							ipb.shoutbox.clearShout();

							new Ajax.Request( ipb.shoutbox.baseUrl + 'type=unban',
								{
									method: 'post',
									encoding: ipb.vars['charset'],
									parameters: {
										name:		unbanName
									},
									onSuccess: function(s)
									{
										if ( ipb.shoutbox.checkForErrors(s.responseText) )
										{
											return false;
										}

										ipb.shoutbox.actionTaken(s.responseText);
									}
								}
							);
						}
						else
						{
							ipb.shoutbox.produceError('mod_invalid_name');
						}
					}
					else
					{
						ipb.shoutbox.clearShout();
						ipb.shoutbox.produceError('mod_no_perm');
					}
					break;
				case '/refresh':
					ipb.shoutbox.clearShout();
				 	ipb.shoutbox.reloadShouts(true);
					break;
				case '/prefs':
					ipb.shoutbox.clearShout();
					if (ipb.shoutbox.myMemberID > 0)
					{
						if ( ipb.shoutbox.global_on )
						{
							ipb.shoutbox.setActionAndReload('myprefs');
						}
						else
						{
							ipb.shoutbox.myPrefsLoad();
						}
					}
					else
					{
						ipb.shoutbox.produceError('prefs_login');
					}
					break;
				case '/archive':
					ipb.shoutbox.clearShout();

					if (ipb.shoutbox.view_archive)
					{
						if (ipb.shoutbox.global_on)
						{
							ipb.shoutbox.setActionAndReload('archive');
						}
						else
						{
							ipb.shoutbox.displayArchive();
						}
					}
					else
					{
						ipb.shoutbox.produceError('no_archive_perm');
					}

					break;
				case '/moderator':
					if (ipb.shoutbox.moderator)
					{
						if ( ipb.shoutbox.validCommandSyntax(a[1]) && ipb.shoutbox.validCommandSyntax(a[2]))
						{
							t = a[1];
							d = a[2];

							var modType  = null,
								shoutID  = 0,
								memType  = '',
								memberID = 0;

							switch (t)
							{
								case 'shout':
									if (parseInt(d) > 0)
									{
										modType = 'shout';
										shoutID = parseInt(d);
									}
									break;
								case 'member':
									modType = 'member';

									if (parseInt(d) > 0)
									{
										memType  = 'number';
										memberID = parseInt(d);
									}
									else
									{
										memType  = 'string';
										memberID = d.toString();
									}
									break;
								default:
									break;
							}

							if ( modType != null && modType != '' )
							{
								ipb.shoutbox.clearShout();

								if ( ipb.shoutbox.global_on )
								{
									ipb.shoutbox.setActionAndReload('mod|'+modType+'|'+((modType == 'member') ? memType+'|'+memberID : shoutID));
								}
								else
								{
									new Ajax.Request( ipb.shoutbox.baseUrl + 'type=mod&action=loadQuickCmd',
										{
											method: 'post',
											encoding: ipb.vars['charset'],
											parameters: {
												modtype:	modType,
												shout:		shoutID,
												memtype:	memType,
												member:		memberID
											},
											onSuccess: function(s)
											{
												if ( ipb.shoutbox.checkForErrors(s.responseText) )
												{
													return false;
												}

												/* Popup already exist, show it! */
												if ( $('modOpts_popup') )
												{
													$('modOpts_inner').update( s.responseText );
												}
												else
												{
													ipb.shoutbox.modOpts	= new ipb.Popup( 'modOpts',
																	{
																		type: 'pane',
																		modal: true,
																		w: '550px',
																		h: 'auto',
																		initial: s.responseText,
																		hideAtStart: true,
																		close: '.cancel'
																	}
																);

													/* Setup close button */
													$('modOpts_close').stopObserving();
													$('modOpts_close').observe( 'click',
														function()
														{
															ipb.shoutbox.closePopup('moderator');
														}
													);
												}

												ipb.shoutbox.setupPopup('moderator');
											}
										}
									);
								}
							}
							else
							{
								ipb.shoutbox.produceError('invalid_command');
							}
						}
						else
						{
							ipb.shoutbox.produceError('invalid_command');
						}
					}
					else
					{
						ipb.shoutbox.clearShout();
						ipb.shoutbox.produceError('mod_no_perms');
					}

					break;
				default:
					return 'doshout';
			}
		}
	},

	validCommandSyntax: function(c, m)
	{
		if (c != '' && typeof(c) != 'undefined' && c != null && c)
		{
			if (m == true)
			{
				c = c.toString();
				if (c.match(new RegExp("^/([a-zA-Z]+?)$", 'i')))
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			else
			{
				return true;
			}
		}

		return false;
	},

	/**
	 * Rewrites the classes for the shouts
	 *
	 * @var		boolean		check	Tells the function to skip or perfom the check for in_archive (needed when we reload shouts while the archive popup is open!)
	 */
	rewriteShoutClasses: function(check)
	{
		var table = null,
			check = check == false ? false : true;
			skip  = 0;

		if ( check && ipb.shoutbox.in_archive )
		{
			table = $('shoutbox-archive-shouts');
		}
		else
		{
			table = $('shoutbox-shouts-table');
		}

		/* Let's update the rows! =D */
		if ( table.down('tbody') )
		{
			$A( table.down('tbody').childElements() ).each(
				function(tr)
				{
					skip = ( skip == 0 ) ? 1 : 0;

					$A( tr.childElements() ).each(
						function(td)
						{
							/* Remove inline styles added by Highlight */
							td.setStyle({
								backgroundColor: '',
								backgroundImage: ''
							});

							if ( skip )
							{
								if ( td.hasClassName('altrow') )
								{
									td.removeClassName('altrow');
								}
							}
							else
							{
								td.addClassName('altrow');
							}
						}
					);
				}
			)
		};
	},

	initEvents: function()
	{
		if ( !ipb.shoutbox.events_loaded )
		{
			ipb.shoutbox.events_loaded = true;
			document.observe('keypress', ipb.shoutbox.keypress_handler );
		}
	},

	displayArchive: function(e)
	{
		if ( !ipb.shoutbox.view_archive || ipb.shoutbox.global_on )
		{
			return false;
		}

		if ( ipb.shoutbox.in_prefs || ipb.shoutbox.in_archive || ipb.shoutbox.in_mod )
		{
			Debug.write("displayArchive: in_prefs, in_archive or in_mod are set to true so this check fails and this should never happen!");
			return false;
		}

		if ( typeof( e ) != 'undefined' )
		{
			Event.stop(e);
		}

		if ( $('archiveArea_popup') )
		{
			ipb.shoutbox.setupPopup('archive');
			return true;
		}

		new Ajax.Request( ipb.shoutbox.baseUrl + '&type=archive&action=load',
			{
				method: 'get',
				encoding: ipb.vars['charset'],
				onSuccess: function(s)
				{
					ipb.shoutbox.archiveArea = new ipb.Popup(
						'archiveArea',
						{
							type: 'pane',
							modal: true,
							w: '740px',
							h: '450px',
							initial: s.responseText,
							hideAtStart: true,
							close: '.cancel'
						}
					);

					/* Setup close button */
					$('archiveArea_close').stopObserving();
					$('archiveArea_close').observe( 'click',
						function()
						{
							ipb.shoutbox.closePopup('archive');
						}
					);

					/* Setup the filter button */
					$('shoutbox-archive-filter-button').observe( 'click', ipb.shoutbox.archiveDoFilter );

					/* Setup menu and onclick for archive filters */
					new ipb.Menu( $( 'shoutbox_archive_filters' ), $( 'shoutbox_archive_filters_menucontent' ) );

					$('filter_today').down('a').observe('click', ipb.shoutbox.archiveQuickFilters.bindAsEventListener(this) );
					$('filter_yesterday').down('a').observe('click', ipb.shoutbox.archiveQuickFilters.bindAsEventListener(this) );
					$('filter_month').down('a').observe('click', ipb.shoutbox.archiveQuickFilters.bindAsEventListener(this) );
					$('filter_all').down('a').observe('click', ipb.shoutbox.archiveQuickFilters.bindAsEventListener(this) );
					$('filter_mine').down('a').observe('click', ipb.shoutbox.archiveQuickFilters.bindAsEventListener(this) );

					/* Let's "fix" the ugly scrollbar removing overflow:auto */
					$('archiveArea_inner').setStyle( { overflow: 'hidden' } );

					ipb.shoutbox.setupPopup('archive');
				}
			}
		);
	},

	produceError: function( error )
	{
		var errorDiv  = 'app';

		/* Global shoutbox? */
		if ( ipb.shoutbox.global_on )
		{
			if ( ipb.shoutbox.can_edit && ipb.shoutbox.mod_in_action )
			{
				errorDiv = 'editShout';
			}
			else
			{
				errorDiv = 'glb';
			}
		}
		else if ( ipb.shoutbox.in_prefs )
		{
			errorDiv = 'myprefs';
		}
		else if ( ipb.shoutbox.in_mod )
		{
			errorDiv = 'moderator';
		}
		else if ( ipb.shoutbox.in_archive )
		{
			errorDiv = 'archive';
		}

		/* Got a key to find the string? */
		if ( ipb.shoutbox.errors[ error ] )
		{
			error = ipb.shoutbox.errors[ error ];
		}

		/* Div exists? Display it! =D */
		if ( $('shoutbox-inline-error-'+errorDiv) )
		{
			$('shoutbox-inline-error-'+errorDiv).update( error ).show();

			// Set timer to hide it
			setTimeout("$('shoutbox-inline-error-" + errorDiv + "').hide()", 2000);
		}
		else
		{
			alert( error );
		}
	},

	getShout: function( strip_html )
	{
		var d = '';

		if ( ipb.shoutbox.global_on )
		{
			d = $('shoutbox-global-shout').value;
		}
		else if (ipb.textEditor.getEditor().CKEditor)
		{
			d = ipb.textEditor.getEditor().CKEditor.getData();
		}
		else
		{
			$$('textarea.ipsEditor_textarea').each
			(
				function( te )
				{
					if (te.name == "shout_msg")
					{
						d = te.value;
					}
				}
			);
		}

		d = d.strip();

		while (d.match(new RegExp("^(.+?)<br>$", 'i')))
		{
			d = d.replace(new RegExp("^(.+?)<br>$", 'i'), '$1');
		}

		d = d.strip();

		if (d.toLowerCase().substring(d.length-4, d.length) == '<br>')
		{
			d = d.substring(0, d.length-4);
		}

		d = d.strip();

		if ( strip_html )
		{
			d = d.stripTags();
		}

		return d;
	},

	clearShout: function()
	{
		ipb.shoutbox.updateLastActivity();

		if ( ipb.shoutbox.global_on )
		{
			// Save our shout if we get errors
			ipb.shoutbox.tempShout = $('shoutbox-global-shout').getValue();

			$('shoutbox-global-shout').setValue("");
			$('shoutbox-global-shout').focus();
		}
		else if ( ipb.textEditor.getEditor().CKEditor )
		{
			// Save our shout if we get errors
			ipb.shoutbox.tempShout = ipb.textEditor.getEditor().CKEditor.getData();
			ipb.textEditor.getEditor().CKEditor.setData('');
		}
		else
		{
			$$('textarea.ipsEditor_textarea').each
			(
				function( te )
				{
					if (te.name == "shout_msg")
					{
						// Save our shout if we get errors
						ipb.shoutbox.tempShout = te.getValue();

						te.setValue("");
						te.focus();
					}
				}
			);
		}
	},

	restoreShout: function()
	{
		if ( ipb.shoutbox.tempShout != null )
		{
			if ( ipb.shoutbox.global_on )
			{
				$('shoutbox-global-shout').setValue( ipb.shoutbox.tempShout );
				$('shoutbox-global-shout').focus();
			}
			else
			{
				ipb.textEditor.getEditor().CKEditor.setData( ipb.shoutbox.tempShout );
			}
		}
	},

	resizeShouts: function()
	{
		var ss = $('shoutbox-shouts'),
			sr = $('shouts-resizer');

		if ( ! ipb.shoutbox.myMemberID)
		{
			sr.style.cursor = 'default';
			return false;
		}

		Resize.init( sr, ss, true, false );

		ss.min_height = 100;
		ss.Resizing   = function()
		{
			ipb.shoutbox.shoutsScrollThem();
		}

		ss.Resize_end = function(data)
		{
			ipb.shoutbox.myPrefsHeightSave( parseInt(data.h) );
		}
	},

	getTimestamp: function()
	{
		var d = new Date(),
			t = d.getTime();

		return Math.floor( t / 1000 );
	},

	updateJSPreferences: function()
	{
		if ( !ipb.shoutbox.myMemberID || !ipb.shoutbox.can_use )
		{
			return false;
		}

		if ( ipb.shoutbox.my_prefs['display_refresh_button'] == 1 )
		{
			$('shoutbox-refresh-button').show();
		}
		else
		{
			$('shoutbox-refresh-button').hide();
		}

		ipb.shoutbox.enable_cmds = ( ipb.shoutbox.my_prefs['enable_quick_commands'] == 1 ) ? true : false;
	},

	shoutsGetLastID: function()
	{
		var tempLastID = 0;

		$A( $('shoutbox-shouts-table').down('tbody').childElements() ).each(
			function(tr)
			{
				tempLastID = parseInt(tr.id.substring(10));

				if ( tempLastID > ipb.shoutbox.last_shout_id )
				{
					ipb.shoutbox.last_shout_id = tempLastID;
				}
			}
		);

		return ipb.shoutbox.last_shout_id;
	},

	shoutsScrollThem: function()
	{
		var area = $( 'shoutbox-shouts' );

		if ( ipb.shoutbox.shout_order == 'asc' )
		{
			area.scrollTop = area.scrollHeight - parseInt( area.getHeight() ) + 500;
		}
		else
		{
			area.scrollTop = 0;
		}
	},

	isInactive: function()
	{
		if ( ipb.shoutbox._inactive )
		{
			return true;
		}

		var diff     = parseInt( ipb.shoutbox.getTimestamp() - ipb.shoutbox.last_active ),
			myMin    = ( diff / 60 ) * ipb.shoutbox.time_minute,
			checkMin = ipb.shoutbox.inactive_timeout * ipb.shoutbox.time_minute;

		if ( myMin >= checkMin )
		{
			ipb.shoutbox.displayInactivePrompt();
			return true;
		}
		else
		{
			return false;
		}
	},

	displayInactivePrompt: function()
	{
		/** Do some common things =O **/
		ipb.shoutbox._inactive = true;
		clearTimeout( ipb.shoutbox.timeoutShouts );
		clearTimeout( ipb.shoutbox.timeoutMembers );

		// Which shoutbox? :D
		if ( ipb.shoutbox.global_on )
		{
			$('shoutbox-shouts-table').hide();

			$('shoutbox-inactive-prompt').setStyle( { height: $('shoutbox-shouts').getStyle('height') } );
			$('shoutbox-inactive-prompt').show();
		}
		else
		{
			// Do we have another popup open already?
			if ( ipb.shoutbox.in_prefs || ipb.shoutbox.in_mod || ipb.shoutbox.in_archive )
			{
				ipb.shoutbox.inactiveWhenPopup = true;
			}
			else
			{
				if ( $('inactivePrompt_popup') )
				{
					ipb.shoutbox.inactivePrompt.show();
				}
				else
				{
					ipb.shoutbox.inactivePrompt = new ipb.Popup( 'inactivePrompt',
						{
							type: 'pane',
							modal: true,
							w: '450px',
							initial: "<table>" + $('shoutbox-inactive-prompt').innerHTML + "</table>",
							hideAtStart: false,
							close: '.close'
						}
					);

					$('inactivePrompt_close').hide();
				}
			}
		}
	},

	processInactivePrompt: function()
	{
		if ( !ipb.shoutbox._inactive )
		{
			return false;
		}

		/** Do some common things =O **/
		ipb.shoutbox._inactive = false;
		ipb.shoutbox.updateLastActivity();

		// Which shoutbox? :D
		if ( ipb.shoutbox.global_on )
		{
			$('shoutbox-inactive-prompt').hide();
			$('shoutbox-shouts-table').show();

			// Refresh shout only if we are not submitting
			if ( !ipb.shoutbox.submittingShout )
			{
				ipb.shoutbox.reloadShouts(true);
			}
		}
		else
		{
			ipb.shoutbox.inactivePrompt.hide();

			// App page, get also members :D
			ipb.shoutbox.reloadMembers(true);
			ipb.shoutbox.reloadShouts(true);
		}
	},

	actionTaken: function(text)
	{
		if ( !text)
		{
			return false;
		}

		alert( text );
	},

	setActionAndReload: function(action)
	{
		if ( action != '' && typeof(action) != 'undefined' && action != null )
		{
			var url = ipb.vars['base_url'] + '&app=shoutbox';

			ipb.Cookie.set( '_shoutbox_jscmd', action );

			try
			{
				window.location = url.replace(/&$/ig, '');
			}

			catch(me)
			{
				window.location.href = url.replace(/&$/ig, '');
			}
		}

		return false;
	},

	emoticonOnclick: function(emo_code)
	{
		//Check focus
		$('shoutbox-global-shout').focus();

		// Parse properly emo_code
		emo_code = emo_code.replace( /&quot;/g, '"' );
		emo_code = emo_code.replace( /&lt;/g  , '<' );
		emo_code = emo_code.replace( /&gt;/g  , '>' );
		emo_code = emo_code.replace( /&amp;/g , '&' );

		// Update textarea and close menu
		ipb.shoutbox.insertAtCursor( " " + emo_code + " " );

		return false;
	},

	bbcodePopup: function(e)
	{
		window.open( ipb.vars['base_url'] + "&app=forums&module=extras&section=legends&do=bbcode", "bbcode", "status=0,toolbar=0,width=1024,height=800,scrollbars=1");
		Event.stop(e);
		return false;
	},

	showShoutboxPopup: function(e)
	{
		window.open( ipb.vars['board_url'] + "/index.php?app=shoutbox&do=popup", "popup", "status=0,toolbar=0,location=0,menubar=0,width=1024,height=768,scrollbars=1");
		Event.stop(e);
		return false;
	},

	popupUpdateStatus: function(lang, text)
	{
		if ( !$('shoutbox-popup-status') )
		{
			Debug.warn("ID shoutbox-popup-status NOT FOUND! | LANG: " + lang + " | TEXT: " + text );
			return false;
		}

		if ( !ipb.shoutbox.myMemberID || ipb.shoutbox.global_on )
		{
			return false;
		}

		text = ( text == true ) ? true : false;

		if ( !text && ( !lang || !ipb.shoutbox.langs[lang] ) )
		{
			return false;
		}

		if ( text )
		{
			$('shoutbox-popup-status').update( lang );
		}
		else
		{
			$('shoutbox-popup-status').update( ipb.shoutbox.langs[lang] );
		}
	},

	popupUpdateContent: function(lang, text)
	{
		if ( !ipb.shoutbox.myMemberID || ipb.shoutbox.global_on )
		{
			return false;
		}

		text = ( text == true ) ? true : false;

		if ( !text && ( !lang || !ipb.shoutbox.langs[lang] ) )
		{
			return false;
		}

		if ( text )
		{
			$('shoutbox-popup-content').update( lang );
		}
		else
		{
			$('shoutbox-popup-content').update( ipb.shoutbox.langs[lang] );
		}
	},

	mod_opts_get_edit_shout: function()
	{
		if ( !ipb.shoutbox.moderator && !ipb.shoutbox.can_edit && !ipb.shoutbox.mod_in_action )
		{
			return false;
		}

		var d = ipb.textEditor.getEditor().CKEditor.getData();

		d = d.strip();

		while (d.match(new RegExp("^(.+?)<br>$", 'i')))
		{
			d = d.replace(new RegExp("^(.+?)<br>$", 'i'), '$1');
		}

		d = d.strip();

		if (d.toLowerCase().substring(d.length-4, d.length) == '<br>')
		{
			d = d.substring(0, d.length-4);
		}

		d = d.strip();

		return d;
	},

	mod_opts_do_edit_shout: function()
	{
		if ( (!ipb.shoutbox.moderator && !ipb.shoutbox.can_edit) || !ipb.shoutbox.mod_in_action )
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();

		var globalOn = ( ipb.shoutbox.global_on ) ? '&global=1' : '',
			shout    = ipb.shoutbox.mod_opts_get_edit_shout();

		if ( shout == '' )
		{
			ipb.shoutbox.produceError('blank_shout');
			return false;
		}

		if ( shout.length * 1024 > ipb.shoutbox.max_length )
		{
			ipb.shoutbox.produceError('shout_too_big');
			return false;
		}

		/**
		 * 1.1.2
		 * Show "processing" only if we are in the app page!
		 */
		if ( ipb.shoutbox.global_on )
		{
			ipb.shoutbox.popupUpdateStatus('processing');
		}

		/* Letz ajax! */
		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=mod&action=performCommand&command=' + ipb.shoutbox.mod_command + '&modtype=shout' + globalOn,
			{
				method: 'post',
				encoding: ipb.vars['charset'],
				parameters: {
					id:			ipb.shoutbox.mod_shout_id,
					shout:		shout.encodeParam()
				},
				onSuccess: function(s)
				{
					$('shout-row-' + ipb.shoutbox.mod_shout_id ).update( s.responseText );

					/* We have the editShout_popup available and visible? */
					if ( $('editShout_popup') && $('editShout_popup').visible() )
					{
						ipb.shoutbox.closePopup('editShout');
					}
					else
					{
						if ( $('editHistory_shout') )
						{
							$('editHistory_shout').show();
						}

						ipb.shoutbox.modOptsEditReset();
					}

					window.location.reload();
				}
			}
		);
	},

	refreshShouts: function()
	{
		/* Block it if we are inactive */
		if ( ipb.shoutbox._inactive )
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();
		ipb.shoutbox.reloadShouts(true);
	},

	archive_get_dropdowns: function(t, v)
	{
		if (t != 'start' && t != 'end')
		{
			return new Array();
		}

		var a = new Array
		(
			$('filter_'+t+'_month'),
			$('filter_'+t+'_day'),
			$('filter_'+t+'_year'),
			$('filter_'+t+'_hour'),
			$('filter_'+t+'_minute'),
			$('filter_'+t+'_meridiem')
		);

		if (v == true)
		{
			for (var i=0; i<a.length; i++)
			{
				if ( a[i] )
				{
					a[i] = a[i].getValue();
				}
			}
		}

		return a;
	},

	archive_set_dropdown_option: function(o, v)
	{
		if (o.options.length > 0)
		{
			for (var i=0; i<o.options.length; i++)
			{
				if (o.options[i].value == v)
				{
					o.selectedIndex = i;
					break;
				}
			}
		}
	},

	archiveDoFilter: function()
	{
		if ( !ipb.shoutbox.view_archive || ipb.shoutbox.global_on || !ipb.shoutbox.in_archive )
		{
			return false;
		}

		/* Already filtering? prevent odd things then */
		if ( ipb.shoutbox.archive_filtering )
		{
			ipb.shoutbox.produceError('already_filtering');
			return false;
		}

		ipb.shoutbox.archive_filtering = true;


		if ( ipb.shoutbox.blur )
		{
			ipb.shoutbox.blur();
		}

		ipb.shoutbox._inactive   = true;
		ipb.shoutbox.updateLastActivity();

		var p = {
			'start':	ipb.shoutbox.archive_get_dropdowns( 'start', true ),
			'end':		ipb.shoutbox.archive_get_dropdowns( 'end', true ),
			'member':	$('filter_member_name').getValue().strip()
		};

		if ( p['member'].indexOf(',') > 0 )
		{
			var x = new Array();
			var m = p['member'].split(',');

			for (var i=0; i<m.length; i++)
			{
				m[i] = m[i].strip();
				if (m[i] == '' || m[i].length < 3)
				{
					continue;
				}

				x[x.length] = m[i];
			}

			if (x.length <= 0)
			{
				ipb.shoutbox.produceError('member_names_too_short');
				return false;
			}
		}
		else if (p['member'].length > 0 && p['member'].length < 3)
		{
			ipb.shoutbox.produceError('member_name_too_short');
			return false;
		}

		ipb.shoutbox.archive_cur_filter = p;

		ipb.shoutbox.popupUpdateStatus('filtering');

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=archive&action=filter',
			{
				method: 	'post',
				encoding: ipb.vars['charset'],
				parameters:	{
					'start':	ipb.shoutbox.archive_get_dropdowns( 'start', true ).join(','),
					'end':		ipb.shoutbox.archive_get_dropdowns( 'end', true ).join(','),
					'member':	p['member']
				},
				onSuccess: function(s)
				{
					ipb.shoutbox.archive_filter_process(s);
				}
			}
		);

		return false;
	},

	archive_filter_process: function(s)
	{
		ipb.shoutbox.popupUpdateStatus('processed');

		if ( ipb.shoutbox.checkForErrors(s.responseText) )
		{
			ipb.shoutbox.archive_filtering = false;
			return false;
		}

		$('shoutbox-archive-shouts').update( s.responseText );

		new Effect.Parallel(
			[
				new Effect.BlindUp( $('beforeButtonClick') ),
				new Effect.BlindDown( $('afterButtonClick') )
			],
			{
				duration: 1.0
			}
		);

		$('backToFilters').stopObserving();
		$('backToFilters').observe( 'click', function()
			{
				new Effect.Parallel(
					[
						new Effect.BlindUp( $('afterButtonClick') ),
						new Effect.BlindDown( $('beforeButtonClick') )
					],
					{
						duration: 1.0
					}
				);

				/* Reset filtering action using that button! */
				ipb.shoutbox.archive_filtering = false;
			}
		);

		ipb.shoutbox.rewriteShoutClasses();
		ipb.shoutbox.archive_update_floaters();
	},

	rect: function(x, y, w, h)
	{
		var recta = new Object;
		recta.x = x;
		recta.y = y;
		recta.w = w;
		recta.h = h;

		return recta;
	},

	archiveQuickFilters: function(event)
	{
		if ( !ipb.shoutbox.view_archive || ipb.shoutbox.global_on || !ipb.shoutbox.in_archive )
		{
			return false;
		}

		if ( ipb.shoutbox.blur )
		{
			ipb.shoutbox.blur();
		}

		/* Get the proper filter */
		var element = Event.element(event);
		var filter  = element.up('li').id.sub( 'filter_', '' );

		/* Ypdate last active */
		ipb.shoutbox.updateLastActivity();

		var d = new Date();
		var s = ipb.shoutbox.archive_get_dropdowns('start', false);
		var e = ipb.shoutbox.archive_get_dropdowns('end', false);

		switch ( filter )
		{
			case 'today':
				ipb.shoutbox.archive_set_dropdown_option(s[0], d.getMonth()+1);
				ipb.shoutbox.archive_set_dropdown_option(s[1], d.getDate());
				ipb.shoutbox.archive_set_dropdown_option(s[2], d.getFullYear());
				ipb.shoutbox.archive_set_dropdown_option(s[3], 12);
				ipb.shoutbox.archive_set_dropdown_option(s[4], 0);
				ipb.shoutbox.archive_set_dropdown_option(s[5], 'am');

				ipb.shoutbox.archive_set_dropdown_option(e[0], d.getMonth()+1);
				ipb.shoutbox.archive_set_dropdown_option(e[1], d.getDate());
				ipb.shoutbox.archive_set_dropdown_option(e[2], d.getFullYear());
				ipb.shoutbox.archive_set_dropdown_option(e[3], 11);
				ipb.shoutbox.archive_set_dropdown_option(e[4], 59);
				ipb.shoutbox.archive_set_dropdown_option(e[5], 'pm');

				$('filter_member_name').value = '';
				break;
			case 'yesterday':
				var m = d.getMonth()+1;
				var a = d.getDate();
				var y = d.getFullYear();

				if (a == 1)
				{
					if (m == 1)
					{
						m  = 12;
						y -= 1;
					}
					else
					{
						m -= 1;
					}

					a = ipb.shoutbox.month_days(m-1);
				}
				else
				{
					a -= 1;
				}

				ipb.shoutbox.archive_set_dropdown_option(s[0], m);
				ipb.shoutbox.archive_set_dropdown_option(s[1], a);
				ipb.shoutbox.archive_set_dropdown_option(s[2], y);
				ipb.shoutbox.archive_set_dropdown_option(s[3], 12);
				ipb.shoutbox.archive_set_dropdown_option(s[4], 0);
				ipb.shoutbox.archive_set_dropdown_option(s[5], 'am');

				ipb.shoutbox.archive_set_dropdown_option(e[0], m);
				ipb.shoutbox.archive_set_dropdown_option(e[1], a);
				ipb.shoutbox.archive_set_dropdown_option(e[2], y);
				ipb.shoutbox.archive_set_dropdown_option(e[3], 11);
				ipb.shoutbox.archive_set_dropdown_option(e[4], 59);
				ipb.shoutbox.archive_set_dropdown_option(e[5], 'pm');

				$('filter_member_name').value = '';
				break;
			case 'month':
				ipb.shoutbox.archive_set_dropdown_option(s[0], d.getMonth()+1);
				ipb.shoutbox.archive_set_dropdown_option(s[1], 1);
				ipb.shoutbox.archive_set_dropdown_option(s[2], d.getFullYear());
				ipb.shoutbox.archive_set_dropdown_option(s[3], 12);
				ipb.shoutbox.archive_set_dropdown_option(s[4], 0);
				ipb.shoutbox.archive_set_dropdown_option(s[5], 'am');

				ipb.shoutbox.archive_set_dropdown_option(e[0], d.getMonth()+1);
				ipb.shoutbox.archive_set_dropdown_option(e[1], ipb.shoutbox.month_days[d.getMonth()]);
				ipb.shoutbox.archive_set_dropdown_option(e[2], d.getFullYear());
				ipb.shoutbox.archive_set_dropdown_option(e[3], 11);
				ipb.shoutbox.archive_set_dropdown_option(e[4], 59);
				ipb.shoutbox.archive_set_dropdown_option(e[5], 'pm');

				$('filter_member_name').value = '';
				break;
			case 'all':
			case 'mine':
				dd = new Date(ipb.shoutbox.oldest_shout);
				hr = dd.getHours();
				md = '';

				if (hr < 12)
				{
					md = 'am';
					if (hr == 0)
					{
						hr = 12;
					}
				}
				else if (hr > 12)
				{
					md  = 'pm';
					hr -= 12;
				}

				ipb.shoutbox.archive_set_dropdown_option(s[0], dd.getMonth()+1);
				ipb.shoutbox.archive_set_dropdown_option(s[1], dd.getDate());
				ipb.shoutbox.archive_set_dropdown_option(s[2], dd.getFullYear());
				ipb.shoutbox.archive_set_dropdown_option(s[3], hr);
				ipb.shoutbox.archive_set_dropdown_option(s[4], dd.getMinutes());
				ipb.shoutbox.archive_set_dropdown_option(s[5], md);

				ipb.shoutbox.archive_set_dropdown_option(e[0], d.getMonth()+1);
				ipb.shoutbox.archive_set_dropdown_option(e[1], d.getDate());
				ipb.shoutbox.archive_set_dropdown_option(e[2], d.getFullYear());
				ipb.shoutbox.archive_set_dropdown_option(e[3], 11);
				ipb.shoutbox.archive_set_dropdown_option(e[4], 59);
				ipb.shoutbox.archive_set_dropdown_option(e[5], 'pm');

				if ( filter == 'mine' )
				{
					$('filter_member_name').value = ipb.shoutbox.my_dname;
				}
				else
				{
					$('filter_member_name').value = '';
				}
				break;
			default:
				ipb.shoutbox.archive_filtering = false;
				return false; //Leave a return here!
		}

		ipb.shoutbox.archiveDoFilter();
	},

	archive_update_floaters: function()
	{
		if ( !ipb.shoutbox.view_archive || ipb.shoutbox.global_on || !ipb.shoutbox.in_archive )
		{
			return false;
		}

		ipb.shoutbox.archive_filtering = false;

		var o = $('shoutbox-archive-shouts-div');
		var r = $('shoutbox-archive-pages-floater');

		/* Scroll shouts to top again after changing page */
		o.scrollTop = 0;

		if ( ipb.shoutbox.shout_pages == 0 )
		{
			r.hide();
			return false;
		}
	},

	archive_update_pager: function(p)
	{
		var html = '';

		ipb.shoutbox.cur_page = p;

		if ( p > 1 )
		{
			html += "<span onclick='ipb.shoutbox.archive_goto_prev_page()' style='cursor:pointer'>&laquo;</span>&nbsp;";
		}

		html += ipb.shoutbox.langs['page']+' '+p+' '+ipb.shoutbox.langs['of']+' '+ipb.shoutbox.shout_pages;

		if ( p < ipb.shoutbox.shout_pages )
		{
			html += "&nbsp;<span onclick='ipb.shoutbox.archive_goto_next_page()' style='cursor:pointer'>&raquo;</span>";
		}

		$('shoutbox-archive-pages-data').innerHTML = html;
	},

	archive_goto_prev_page: function()
	{
		if ( !ipb.shoutbox.view_archive || ipb.shoutbox.global_on || !ipb.shoutbox.in_archive )
		{
			return false;
		}

		var p = ipb.shoutbox.cur_page;
		var t = ipb.shoutbox.shout_pages;
		var n = Math.floor(p-1);

		if (n < 1)
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();
		ipb.shoutbox.archive_cur_filter['page'] = n;

		ipb.shoutbox.popupUpdateStatus('filtering');

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=archive&action=filter',
			{
				method: 	'post',
				encoding: ipb.vars['charset'],
				parameters:	{
					'start':	ipb.shoutbox.archive_get_dropdowns( 'start', true ).join(','),
					'end':		ipb.shoutbox.archive_get_dropdowns( 'end', true ).join(','),
					'member':	$('filter_member_name').getValue().strip(),
					'page':		n
				},
				onSuccess: function(s)
				{
					ipb.shoutbox.archive_filter_process(s);
				}
			}
		);
	},

	archive_goto_next_page: function()
	{
		if (!ipb.shoutbox.view_archive || ipb.shoutbox.global_on || !ipb.shoutbox.in_archive)
		{
			return false;
		}

		var p = ipb.shoutbox.cur_page;
		var t = ipb.shoutbox.shout_pages;
		var n = Math.floor(p+1);

		if (n > t)
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();
		ipb.shoutbox.archive_cur_filter['page'] = n;

		ipb.shoutbox.popupUpdateStatus('filtering');

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=archive&action=filter',
			{
				method: 	'post',
				encoding: ipb.vars['charset'],
				parameters:	{
					'start':	ipb.shoutbox.archive_get_dropdowns( 'start', true ).join(','),
					'end':		ipb.shoutbox.archive_get_dropdowns( 'end', true ).join(','),
					'member':	$('filter_member_name').getValue().strip(),
					'page':		n
				},
				onSuccess: function(s)
				{
					ipb.shoutbox.archive_filter_process(s);
				}
			}
		);
	},

	shouts_fade: function(ids)
	{
		if ( ids != null && ids.length > 0 )
		{
			/** Update live total shouts count **/
			ipb.shoutbox.updateTotalShouts( ipb.shoutbox.total_shouts + ids.length );

			/* Fade disabled, stop here u_u */
			if ( !ipb.shoutbox.enable_fade )
			{
				return false;
			}

			// Fade our new shouts
			ids.each(
				function( ID )
				{
					$A( $( 'shout-row-' + ID ).childElements() ).each(
						function( td )
						{
							new Effect.Highlight( td, { startcolor: '#ffff99' } );
						}
					);
				}
			);
		}
	},

	resizeGlobalShouts: function()
	{
		var sso = $('shoutbox-shouts');
		var srh = $('shouts-global-resizer');

		if ( !ipb.shoutbox.myMemberID )
		{
			srh.style.cursor = 'default';
			return false;
		}

		Resize.init(srh, sso, true, false);

		sso.min_height = 100;
		sso.Resizing   = function()
		{
			ipb.shoutbox.shoutsScrollThem();
		}

		sso.Resize_end = function(data)
		{
			ipb.shoutbox.myPrefsGlobalHeightSave( parseInt(data.h) );
		}
	},

	keypress_handler: function(e)
	{
		if (document.all)
		{
			if (e && window.event && e._skip != true)
			{
				e = window.event;
			}
		}

		if (!e || e == null || typeof(e) == 'undefined')
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();

		var ret = true;
		if (document.layers)
		{
			var alt   = (e.modifiers&Event.ALT_MASK) ? true : false;
			var ctrl  = (e.modifiers&Event.CONTROL_MASK) ? true : false;
			var shift = (e.modifiers&Event.SHIFT_MASK) ? true : false;
			var key   = (e.data.keyCode) ? e.data.keyCode : e.which;
		}
		else
		{
			var alt   = e.altKey;
			var ctrl  = e.ctrlKey;
			var shift = e.shiftKey;

			if (document.all)
			{
				var key = (e.data.keyCode) ? e.data.keyCode : e.keyCode;
			}
			else
			{
				if (e.keyCode > 0)
				{
					var key = e.keyCode;
				}
				else if (e.which > 0)
				{
					var key = e.which;
				}
				else
				{
					var key = e.data.keyCode;
				}
			}
		}

		var obj = (e.srcElement) ? e.srcElement : (e.editor) ? 'ckeditor' : e.target;

		if ( obj != 'ckeditor' )
		{
			if ( Prototype.Browser.WebKit || Prototype.Browser.Gecko )
			{
				if (obj.nodeType == 3 && e.target.parentNode.nodeType == 1)
				{
					obj = e.target.parentNode;
				}
			}

			if (typeof(obj.id) == 'undefined')
			{
				obj.id = '';
			}
		}

		if ( ipb.shoutbox.my_prefs['enter_key_shout'] == 1 && (obj.id == 'shoutbox-global-shout' || obj.name == "shout_msg" || obj == 'ckeditor') && !shift && !alt && !ctrl && key == 13)
		{
			ipb.shoutbox.submitShout();

			Event.stop(e);
			return false;
		}
		else if ( (alt && key == 13) || (ctrl && key == 13) )
		{
			ipb.shoutbox.submitShout();

			Event.stop(e);
			return false;
		}

		return true;
	},

	insertAtCursor: function(text)
	{
		var editor = $('shoutbox-global-shout');

		editor.focus();

		if ( !Object.isUndefined( editor.selectionStart ) )
		{
			var open = editor.selectionStart + 0;
			var st   = editor.scrollTop;
			var end  = open + text.length;

			/* Opera doesn't count the linebreaks properly for some reason */
			if( Prototype.Browser.Opera )
			{
				var opera_len = text.match( /\n/g );

				try
				{
					end += parseInt(opera_len.length);
				}
				catch(e)
				{
					Debug.write( "Error with Opera => " + e );
				}
			}

			editor.value = editor.value.substr(0, editor.selectionStart) + text + editor.value.substr(editor.selectionEnd);

			editor.setSelectionRange( end, end );
		}
		else if ( document.selection && document.selection.createRange )
		{
			var sel  = document.selection.createRange();
			sel.text = text.replace(/\r?\n/g, '\r\n');
			sel.select();
		}
		else
		{
			editor.value += text;
		}
	},

	truebody: function()
	{
		return (document.compatMode && document.compatMode != 'BackCompat') ? document.documentElement : document.body;
	},

	get_offset_left: function(o, p)
	{
		var l = 0;
		if (o.offsetParent)
		{
			while (o.offsetParent)
			{
				if (p != null && o == p)
				{
					break;
				}

				l += o.offsetLeft;
				o = o.offsetParent;
			}
		}
		else if (o.x)
		{
			l += o.x;
		}

		return l;
	},

	get_offset_top: function(o, p)
	{
		var t = 0;
		if (o.offsetParent)
		{
			while (o.offsetParent)
			{
				if (p != null && o == p)
				{
					break;
				}

				t += o.offsetTop;
				o = o.offsetParent;
			}
		}
		else if (o.y)
		{
			t += o.y;
		}

		return t;
	},

	get_page_size: function()
	{
		var x;
		var y;
		var ww;
		var wh;
		var pw;
		var ph;

		if (window.innerHeight && window.scrollMaxY)
		{
			x = document.body.scrollWidth;
			y = window.innerHeight+window.scrollMaxY;
		}
		else if (document.body.scrollHeight > document.body.offsetHeight)
		{
			x = document.body.scrollWidth;
			y = document.body.scrollHeight;
		}
		else
		{
			x = document.body.offsetWidth;
			y = document.body.offsetHeight;
		}

		if (self.innerHeight)
		{
			ww = self.innerWidth;
			wh = self.innerHeight;
		}
		else if (document.documentElement && document.documentElement.clientHeight)
		{
			ww = document.documentElement.clientWidth;
			wh = document.documentElement.clientHeight;
		}
		else if (document.body)
		{
			ww = document.body.clientWidth;
			wh = document.body.clientHeight;
		}

		pw = x;
		ph = y;

		if (y < wh)
		{
			ph = wh;
		}

		if (x < ww)
		{
			pw = ww;
		}

		return new Array(pw, ph, ww, wh);
	},

	checkForErrors: function( text, status )
	{
		if ( text.substring(0, 6) == 'error-' )
		{
			if ( status != null && status != '' )
			{
				ipb.shoutbox.popupUpdateStatus( status );
			}

			ipb.shoutbox.produceError( text.substring(6) );

			return true;
		}
		else
		{
			return false;
		}
	},

	setupPopup: function(popup)
	{
		switch(popup)
		{
			case 'preferences':
				ipb.shoutbox.in_prefs = true;

				ipb.shoutbox.popupUpdateStatus('my_prefs_loaded');

				/* Setup onclick for buttons */
				$('myprefs_save').observe('click', ipb.shoutbox.myPrefsSave );
				$('myprefs_restore').observe('click', ipb.shoutbox.myPrefsRestore );

				/* Show it */
				ipb.shoutbox.preferences.show();
				break;
			case 'moderator':
				ipb.shoutbox.in_mod = true;

				/* Setup onclick for mod menu */
				if ( $('edit_shout') )
				{
					$('edit_shout').observe('click', ipb.shoutbox.modOptsDo.bindAsEventListener(this) );
				}
				if ( $('editHistory_shout') )
				{
					$('editHistory_shout').observe('click', ipb.shoutbox.modOptsDo.bindAsEventListener(this) );
				}
				if ( $('delete_shout') )
				{
					$('delete_shout').observe('click', ipb.shoutbox.modOptsDo.bindAsEventListener(this) );
				}
				if ( $('deleteAll_shout') )
				{
					$('deleteAll_shout').observe('click', ipb.shoutbox.modOptsDo.bindAsEventListener(this) );
				}
				if ( $('ban_shout') )
				{
					$('ban_shout').observe('click', ipb.shoutbox.modOptsDo.bindAsEventListener(this) );
				}
				if ( $('ban24_shout') )
				{
					$('ban24_shout').observe('click', ipb.shoutbox.modOptsDo.bindAsEventListener(this) );
				}
				if ( $('ban48_shout') )
				{
					$('ban48_shout').observe('click', ipb.shoutbox.modOptsDo.bindAsEventListener(this) );
				}
				if ( $('unban_shout') )
				{
					$('unban_shout').observe('click', ipb.shoutbox.modOptsDo.bindAsEventListener(this) );
				}
				if ( $('removeMod_shout') )
				{
					$('removeMod_shout').observe('click', ipb.shoutbox.modOptsDo.bindAsEventListener(this) );
				}

				/* Show it */
				ipb.shoutbox.modOpts.show();
				break;
			case 'archive':
				$('load-shoutbox-archive').stopObserving();
				$('load-shoutbox-archive').observe( 'click', ipb.shoutbox.displayArchive );

				ipb.shoutbox.in_archive = true;

				ipb.shoutbox.popupUpdateStatus('sb_archive_loaded');

				/* Show it */
				ipb.shoutbox.archiveArea.show();
				break;
			// Added in 1.1.1
			case 'editShout':
				ipb.shoutbox.in_mod = true;

				/* Observe buttons >_< */
				$('mod_edit_shout_confirm').observe('click', ipb.shoutbox.mod_opts_do_edit_shout );
				$('mod_edit_shout_clear').observe('click', ipb.shoutbox.modOptsEditClear );
				$('mod_edit_shout_cancel').observe('click',
					function()
					{
						ipb.shoutbox.closePopup('editShout');
					}
				);
				/* Show it */
				ipb.shoutbox.editShoutPopup.show();
				break;
			default:
				Debug.write("setupPopup called without a proper popup defined: "+popup);
				break;
		}
	},

	closePopup: function(popup)
	{
		switch(popup)
		{
			case 'preferences':
				ipb.shoutbox.preferences.hide();

				ipb.shoutbox.in_prefs = false;
				break;
			case 'moderator':
				ipb.shoutbox.modOptsEditReset();
				ipb.shoutbox.modOpts.hide();

				ipb.shoutbox.in_mod = false;
				ipb.shoutbox.mod_shout_id = 0;
				ipb.shoutbox.mod_member_id = 0;
				ipb.shoutbox.mod_in_action = false;
				break;
			case 'archive':
				ipb.shoutbox.archiveArea.hide();

				ipb.shoutbox.in_archive = false;
				ipb.shoutbox.archive_filtering = false;
				break;
			// Added in 1.1.1
			case 'editShout':
				ipb.shoutbox.editShoutPopup.hide();

				ipb.shoutbox.in_mod = false;
				ipb.shoutbox.mod_shout_id = 0;
				ipb.shoutbox.mod_member_id = 0;
				ipb.shoutbox.mod_in_action = false;

				/* Empty the popup to avoid duplicate IDs in the main shoutbox  - 1.1.1 */
				$('editShout_inner').update('');
				break;
			default:
				Debug.write("closePopup called without a proper popup defined: "+popup);
				break;
		}

		/* We got inactive while a popup was open! =O */
		if ( ipb.shoutbox.inactiveWhenPopup )
		{
			ipb.shoutbox.inactiveWhenPopup = false;

			setTimeout("ipb.shoutbox.displayInactivePrompt()", 600);
		}
	},

	popupModeratorReset: function()
	{
		/* Update status/content */
		ipb.shoutbox.popupUpdateStatus('mod_opts_start_status');
		ipb.shoutbox.popupUpdateContent('mod_opts_start_content');

		/* Reset our mod vars */
		ipb.shoutbox.mod_in_action = false;
		ipb.shoutbox.mod_editor_id  = '';
		ipb.shoutbox.mod_editor_rte = 0;
	},

	updateLastActivity: function()
	{
		ipb.shoutbox.last_active = ipb.shoutbox.getTimestamp();
	},

	updateTotalShouts: function( newCount )
	{
		ipb.shoutbox.total_shouts = parseInt(newCount);

		if ( !ipb.shoutbox.global_on )
		{
			$('shoutbox-total-shouts').update( ipb.shoutbox.total_shouts );

			if ( ipb.shoutbox.enable_fade )
			{
				new Effect.Highlight( $('shoutbox-total-shouts'), { startcolor: '#ffff99' } );
			}
		}
	},

	/**
	 * Setup the toggle hide/show for the shoutbox
	 */
	setupToggle: function()
	{
		if ( !ipb.shoutbox.global_on || !$('category_shoutbox') )
		{
			return false;
		}

		$('category_shoutbox').select('.toggle')[0].stopObserving();
		$('category_shoutbox').select('.toggle')[0].observe( 'click', ipb.shoutbox.toggleShoutbox );

		/* ipb.board not loaded? */
		if ( Object.isUndefined(ipb.board) )
		{
			cookie = ipb.Cookie.get('toggleCats');

			if( cookie )
			{
				var cookies = cookie.split( ',' );

				//-------------------------
				// Little fun for you...
				//-------------------------
				for( var abcdefg=0; abcdefg < cookies.length; abcdefg++ )
				{
					if( cookies[ abcdefg ] == 'shoutbox' )
					{
						var wrapper	= $('category_shoutbox').down('.table_wrap');

						wrapper.hide();
						$('category_shoutbox').addClassName('collapsed');

						// Block there, we found our shoutbox :D
						break;
					}
				}
			}
		}
	},

	/**
	 * Show/hide the shoutbox
	 *
	 * @var		{event}		e	The event
	 */
	toggleShoutbox: function(e)
	{
		if ( !ipb.shoutbox.global_on )
		{
			return false;
		}

		/* Code taked from the function ipb.board.toggleCat(e); because the board file is not loaded always */
		if( ipb.shoutbox.animatingGlobal || (!Object.isUndefined(ipb.board) && ipb.board.animating) ){ return false; }

		/* Init some vars */
		var click   = Event.element(e),
			remove  = $A(),
			wrapper = $( click ).up('.category_block').down('.table_wrap');

		$( wrapper ).identify(); // IE8 fix

		ipb.shoutbox.animatingGlobal = true;

		// Get cookie
		cookie = ipb.Cookie.get('toggleCats');
		if( cookie == null ){
			cookie = $A();
		} else {
			cookie = cookie.split(',');
		}

		Effect.toggle( wrapper, 'blind', {duration: 0.4, afterFinish: function(){ ipb.shoutbox.animatingGlobal = false; } } );

		if( $('category_shoutbox').hasClassName('collapsed') )
		{
			$('category_shoutbox').removeClassName('collapsed');
			remove.push('shoutbox');

			// Not inactive? load new shouts!
			if ( !ipb.shoutbox._inactive )
			{
				ipb.shoutbox.reloadShouts(true);
			}
		}
		else
		{
			// Remove Shouts timer
			clearTimeout(ipb.shoutbox.timeoutShouts);

			new Effect.Morph( $('category_shoutbox'), {style: 'collapsed', duration: 0.4, afterFinish: function(){
				$('category_shoutbox').addClassName('collapsed');

				ipb.shoutbox.animatingGlobal = false;
			} });
			cookie.push('shoutbox');
		}

		cookie = "," + cookie.uniq().without( remove ).join(',') + ",";
		ipb.Cookie.set('toggleCats', cookie, 1);

		Event.stop( e );
	},

	/**
	 * Show hidden shouts from ignored users
	 *
	 * @var		int		id		Shout ID
	 */
	showHiddenShout: function(id)
	{
		if( $('hidden_shout_' + id ) )
		{
			elem = $('hidden_shout_' + id );
			new Effect.Parallel( [
				new Effect.BlindDown( elem ),
				new Effect.Appear( elem )
			], { duration: 0.5 } );
		}

		if( $('unhide_shout_' + id ) )
		{
			$('unhide_shout_' + id ).hide();
		}
	},

	submitShout: function()
	{
		if ( ipb.shoutbox.submittingShout )
		{
			ipb.shoutbox.produceError('already_submitting');
			return false;
		}

		/**
		 * Beta 3
		 * Can only view shoutbox?
		 */
		if ( !ipb.shoutbox.can_use )
		{
			return false;
		}

		var globalOn 	= ( ipb.shoutbox.global_on ) ? '&global=1' : '',
			postedShout	= ipb.shoutbox.getShout( false );

		if ( postedShout == '' )
		{
			ipb.shoutbox.produceError('blank_shout');
			return false;
		}

		if ( postedShout.length * 1024 > ipb.shoutbox.max_length )
		{
			ipb.shoutbox.produceError('shout_too_big');
			return false;
		}

		/**
		 * Beta 3
		 * Re-added flood check also JS side
		 */
		if ( ipb.shoutbox.flood_limit && ipb.shoutbox.bypass_flood != 1 && ipb.shoutbox.my_last_shout )
		{
			var flood_check = ipb.shoutbox.getTimestamp() - ipb.shoutbox.my_last_shout;

			if (flood_check < ipb.shoutbox.flood_limit)
			{
				ipb.shoutbox.produceError( ipb.shoutbox.errors['flooding'].replace( '{#EXTRA#}', ( ipb.shoutbox.flood_limit - flood_check ) ) );
				return false;
			}
		}

		var c = ipb.shoutbox.checkForCommands();

		if ( c != null && c == 'doshout' )
		{
			/**
			 * 1.1.0 Alpha
			 *
			 * Clear timeout to avoid loading twice the same shouts
			 * And also setup a boolean value to stop new attempts
			 */
			clearTimeout(ipb.shoutbox.timeoutShouts);
			ipb.shoutbox.submittingShout = true;

			/* /me code */
			if ( ipb.shoutbox.can_me_tag )
			{
				postedShout = postedShout.replace( '/me ', ipb.shoutbox.member_name + ' ' );
			}

			// Take care of the other things
			ipb.shoutbox.clearShout();
			ipb.shoutbox.updateLastActivity();
			ipb.shoutbox.my_last_shout = ipb.shoutbox.last_active;

			/** Finally ajax it! =D **/
			new Ajax.Request( ipb.shoutbox.baseUrl + 'type=submit&lastid=' + ipb.shoutbox.last_shout_id + globalOn,
				{
					method: 'post',
					encoding: ipb.vars['charset'],
					parameters: {
						shout:    postedShout.encodeParam()
					},
					onSuccess: function(s)
					{
						// Beta 3: process inactive prompt if we are submitting a new shout
						if ( ipb.shoutbox.global_on && ipb.shoutbox._inactive )
						{
							ipb.shoutbox.processInactivePrompt();
						}

						if ( ipb.shoutbox.checkForErrors(s.responseText) )
						{
							ipb.shoutbox.restoreShout();
							ipb.shoutbox.submittingShout = false;
							return false;
						}

						/**
						 * 1.1.0 RC 1
						 * Everything is okay, reset our tempShout
						 */
						ipb.shoutbox.tempShout = null;

						/**
						 * 1.0.0 Final
						 * Fix no shouts message
						 */
						if ( ipb.shoutbox.total_shouts <= 0 )
						{
							$('shoutbox-no-shouts-message').hide();
						}

						var shoutsDiv = $('shoutbox-shouts-table').down('tbody');

						if ( ipb.shoutbox.shout_order == 'asc' )
						{
							shoutsDiv.update( shoutsDiv.innerHTML + s.responseText );
						}
						else
						{
							if ( ipb.shoutbox.total_shouts > 0 )
							{
								shoutsDiv.down('tr').insert( { before: s.responseText } );
							}
							else
							{
								shoutsDiv.update( s.responseText );
							}
						}

						// Fix shout classes
						ipb.shoutbox.rewriteShoutClasses();

						// Remove the block
						ipb.shoutbox.submittingShout = false;

						// Setup latest ID
						ipb.shoutbox.shoutsGetLastID();

						if ( ipb.shoutbox.can_use && ipb.shoutbox.my_prefs['display_refresh_button'] == 1 )
						{
							$('shoutbox-refresh-button').show();
						}

						/**
						 * Beta 2
						 * Restart timer
						 */
						ipb.shoutbox.timeoutShouts = setTimeout("ipb.shoutbox.reloadShouts(true)", ipb.shoutbox.shouts_refresh);

						ipb.shoutbox.shoutsScrollThem();

						if ( !globalOn && ipb.textEditor.getEditor().CKEditor )
						{
							ipb.textEditor.getEditor().CKEditor.focus();
						}
					}
				}
			);
		}
	},

	reloadShouts: function(doLoad)
	{
		/**
		 * Beta 2
		 * Fix timeout with clearTimeout
		 */
		clearTimeout( ipb.shoutbox.timeoutShouts );

		// If for any odd chance we get there while submitting block it!
		if ( ipb.shoutbox.submittingShout )
		{
			return false;
		}

		doLoad = (doLoad == true) ? true : false;

		if ( doLoad )
		{
			var globalOn = ( ipb.shoutbox.global_on ) ? '&global=1' : '';

			// Setup latest ID
			ipb.shoutbox.shoutsGetLastID();

			// Hide refresh button?
			if ( ipb.shoutbox.hide_refresh )
			{
				$('shoutbox-refresh-button').hide();
			}

			new Ajax.Request( ipb.shoutbox.baseUrl + 'type=getShouts&lastid=' + ipb.shoutbox.last_shout_id + globalOn,
				{
					method: 'get',
					encoding: ipb.vars['charset'],
					hideLoader: ipb.shoutbox.img_disable ? true : false,
					onSuccess: function(s)
					{
						ipb.shoutbox.updateShouts(s.responseText);
						return true;
					}
				}
			);
		}
		else if ( ipb.shoutbox.isInactive() )
		{
			Debug.write("reloadShouts: shoutbox inactive, timer is activated again clicking I'm back now");
			return false;
		}

		/**
		 * Beta 2
		 * Set again timeout if we are not inactive :O
		 */
		ipb.shoutbox.timeoutShouts = setTimeout("ipb.shoutbox.reloadShouts(true)", ipb.shoutbox.shouts_refresh);
	},

	updateShouts: function(response)
	{
		/* Show again the button! */
		if ( ipb.shoutbox.can_use && ipb.shoutbox.my_prefs['display_refresh_button'] == 1 )
		{
			$('shoutbox-refresh-button').show();
		}

		/** And reset timer **/
		ipb.shoutbox.reloadShouts();

		// Finally update shouts
		// Leave the code above there or it causes a bug | Terabyte
		if ( response != '' && response != '<!--nothing-->' && response != '0' )
		{
			if ( ipb.shoutbox.checkForErrors(response) )
			{
				return false;
			}

			/**
			 * 1.0.0 Final
			 * Fix no shouts message
			 */
			if ( ipb.shoutbox.total_shouts <= 0 )
			{
				$('shoutbox-no-shouts-message').hide();
			}

			var shoutsDiv = $('shoutbox-shouts-table').down('tbody');

			if ( ipb.shoutbox.shout_order == 'asc' )
			{
				shoutsDiv.update( shoutsDiv.innerHTML + response );
			}
			else
			{
				if ( ipb.shoutbox.total_shouts > 0 )
				{
					shoutsDiv.down('tr').insert( { before: response } );
				}
				else
				{
					shoutsDiv.update( response );
				}
			}

			// Fix shout classes
			ipb.shoutbox.rewriteShoutClasses(false);

			// Setup latest ID
			ipb.shoutbox.shoutsGetLastID();

			ipb.shoutbox.shoutsScrollThem();
		}
	},

	reloadMembers: function(doLoad)
	{
		/**
		 * 1.1.0 Alpha
		 * Fix timeout with clearTimeout
		 */
		clearTimeout( ipb.shoutbox.timeoutMembers );

		doLoad = (doLoad == true) ? true : false;

		if ( doLoad )
		{
			new Ajax.Request( ipb.shoutbox.baseUrl + 'type=getMembers',
				{
					method: 'get',
					evalJSON: 'force',
					encoding: ipb.vars['charset'],
					hideLoader: ipb.shoutbox.img_disable ? true : false,
					onSuccess: function(d)
					{
						if( Object.isUndefined( d.responseJSON ) )
						{
							ipb.shoutbox.produceError('loading_members_viewing');
						}
						else
						{
							/* Update stats! =D */
							$('shoutbox-active-total').update( d.responseJSON['TOTAL'] );
							$('shoutbox-active-member').update( d.responseJSON['MEMBERS'] );
							$('shoutbox-active-guests').update( d.responseJSON['GUESTS'] );
							$('shoutbox-active-anon').update( d.responseJSON['ANON'] );

							/* Sort out names */
							if ( d.responseJSON['NAMES'] == '' )
							{
								$('shoutbox-active-names').hide();
							}
							else
							{
								$('shoutbox-active-names').update( d.responseJSON['NAMES'].join(', ') ).show();
							}

							/* Highligh the names! */
							if ( ipb.shoutbox.enable_fade )
							{
								new Effect.Highlight( $('shoutbox-active-names'),
									{
										startcolor: '#ffff99'
									}
								);
							}
						}

						ipb.shoutbox.reloadMembers();
						return true;
					}
				}
			);
		}
		else if ( ipb.shoutbox.isInactive() )
		{
			Debug.write("reloadMembers: shoutbox inactive, timer is activated again clicking I'm back now");
			return false;
		}

		/**
		 * 1.1.0 Alpha
		 * Set again timeout if we are not inactive :O
		 */
		ipb.shoutbox.timeoutMembers = setTimeout("ipb.shoutbox.reloadMembers(true)", ipb.shoutbox.members_refresh);
	},

	myPrefsLoad: function()
	{
		if ( ipb.shoutbox.myMemberID <= 0 )
		{
			return false;
		}

		if ( ipb.shoutbox.blur )
		{
			ipb.shoutbox.blur();
		}

		if ( ipb.shoutbox.global_on )
		{
			ipb.shoutbox.setActionAndReload('myprefs');
			return false;
		}

		ipb.shoutbox.updateLastActivity();

		/* Popup already exist? */
		if ( $('myPrefs_popup') )
		{
			ipb.shoutbox.setupPopup('preferences');
			return true;
		}

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=prefs&action=load',
			{
				method: 'get',
				encoding: ipb.vars['charset'],
				onSuccess: function(s)
				{
					if ( ipb.shoutbox.checkForErrors(s.responseText) )
					{
						return false;
					}

					ipb.shoutbox.preferences = new ipb.Popup( 'myPrefs',
									{
										type: 'pane',
										modal: true,
										w: '550px',
										h: 'auto',
										initial: s.responseText,
										hideAtStart: true,
										close: '.cancel'
									}
								);

					/* Setup close button */
					$('myPrefs_close').stopObserving();
					$('myPrefs_close').observe( 'click',
						function()
						{
							ipb.shoutbox.closePopup('preferences');
						}
					);

					ipb.shoutbox.setupPopup('preferences');
				}
			}

		);

		return false;
	},

	myPrefsSave: function()
	{
		if ( !ipb.shoutbox.myMemberID || ipb.shoutbox.global_on )
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();

		// Update our prefs on-fly
		ipb.shoutbox.my_prefs['global_display']         = ($('my_prefs_gsb_y').checked) ? 1 : 0;
		ipb.shoutbox.my_prefs['enter_key_shout']        = ($('my_prefs_ets_y').checked) ? 1 : 0;
		ipb.shoutbox.my_prefs['enable_quick_commands']  = ($('my_prefs_eqc_y').checked) ? 1 : 0;
		ipb.shoutbox.my_prefs['display_refresh_button'] = ($('my_prefs_drb_y').checked) ? 1 : 0;
		ipb.shoutbox.my_prefs['disable_sound']		= ($('my_prefs_snd_y').checked) ? 1 : 0;
		ipb.shoutbox.my_prefs['shoutbox_display_order'] = ($('my_prefs_sdo').value);

		// Setup status
		ipb.shoutbox.popupUpdateStatus('saving_prefs');

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=prefs&action=save',
			{
				method: 'post',
				encoding: ipb.vars['charset'],
				parameters: {
					prefs_gsb : ipb.shoutbox.my_prefs['global_display'],
					prefs_ets : ipb.shoutbox.my_prefs['enter_key_shout'],
					prefs_eqc : ipb.shoutbox.my_prefs['enable_quick_commands'],
					prefs_drb : ipb.shoutbox.my_prefs['display_refresh_button'],
					prefs_sdo : ipb.shoutbox.my_prefs['shoutbox_display_order'],
					prefs_snd : ipb.shoutbox.my_prefs['disable_sound']
				},
				onSuccess: function(s)
				{
					if ( ipb.shoutbox.checkForErrors(s.responseText, 'my_prefs_loaded') )
					{
						return false;
					}

					// Run code and update prefs
					s.responseText.evalScripts();

					// Stop observing so we don't have double onclick events later
					$('myprefs_save').stopObserving();
					$('myprefs_restore').stopObserving();

					ipb.shoutbox.preferences.hide();
					ipb.shoutbox.in_prefs = false;
				}
			}

		);

		return false;
	},

	myPrefsRestore: function()
	{
		if ( !ipb.shoutbox.myMemberID || ipb.shoutbox.global_on )
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();

		// Setup status
		ipb.shoutbox.popupUpdateStatus('processing');

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=prefs&action=restore',
			{
				method: 'get',
				encoding: ipb.vars['charset'],
				onSuccess: function(s)
				{
					if ( ipb.shoutbox.checkForErrors(s.responseText, 'my_prefs_loaded') )
					{
						return false;
					}

					// Update popup and update prefs
					$('myPrefs_inner').update( s.responseText );

					ipb.shoutbox.preferences.hide();
					ipb.shoutbox.in_prefs = false;
				}
			}
		);

		return false;
	},

	myPrefsHeightSave: function( newHeight )
	{
		if ( !ipb.shoutbox.myMemberID || ipb.shoutbox.global_on )
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=prefs&action=appHeight',
			{
				method: 'post',
				parameters: {
					height:		newHeight
				},
				onSuccess: function(s)
				{
					if ( ipb.shoutbox.checkForErrors(s.responseText) )
					{
						return false;
					}
				}
			}
		);

		return false;
	},

	myPrefsGlobalHeightSave: function( newHeight )
	{
		if ( !ipb.shoutbox.myMemberID || !ipb.shoutbox.global_on )
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=prefs&action=globalHeight',
			{
				method: 'post',
				parameters: {
					height:		newHeight
				},
				onSuccess: function(s)
				{
					if ( ipb.shoutbox.checkForErrors(s.responseText) )
					{
						return false;
					}
				}
			}
		);

		return false;
	},

	/**
	 * Load moderator popup for a shout
	 */
	modOptsLoadShout: function(id)
	{
		if ( (!ipb.shoutbox.moderator && !ipb.shoutbox.can_edit) || ipb.shoutbox.mod_in_action )
		{
			return false;
		}

		if ( ipb.shoutbox.global_on )
		{
			ipb.shoutbox.setActionAndReload('mod|shout|'+id);
			return false;
		}

		/* We are in the archive? Let's close it then */
		if ( ipb.shoutbox.in_archive )
		{
			ipb.shoutbox.closePopup('archive');
		}

		ipb.shoutbox.updateLastActivity();

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=mod&action=loadShout&id=' + id,
			{
				method: 'get',
				onSuccess: function(s)
				{
					ipb.shoutbox.modOptsShow(s.responseText);
				}
			}
		);

		return false;
	},

	/**
	 * Load moderator popup for a member
	 */
	modOptsLoadMember: function(id)
	{
		if ( !ipb.shoutbox.moderator || ipb.shoutbox.mod_in_action )
		{
			return false;
		}

		if ( ipb.shoutbox.global_on )
		{
			ipb.shoutbox.setActionAndReload('mod|member|number|'+id);
			return false;
		}

		/* We are in the archive? Let's close it then */
		if ( ipb.shoutbox.in_archive )
		{
			ipb.shoutbox.closePopup('archive');
		}

		ipb.shoutbox.updateLastActivity();

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=mod&action=loadMember&mid=' + id,
			{
				method: 'get',
				encoding: ipb.vars['charset'],
				onSuccess: function(s)
				{
					ipb.shoutbox.modOptsShow(s.responseText);
				}
			}
		);

		return false;
	},

	/**
	 * Show the moderation popup
	 */
	modOptsShow: function(response)
	{
		if ( (!ipb.shoutbox.moderator && !ipb.shoutbox.can_edit) || ipb.shoutbox.mod_in_action )
		{
			return false;
		}

		if ( ipb.shoutbox.checkForErrors(response) )
		{
			return false;
		}

		/* Popup already exist, show it! */
		if ( $('modOpts_popup') )
		{
			$('modOpts_inner').update( response );
		}
		else
		{
			ipb.shoutbox.modOpts	= new ipb.Popup( 'modOpts',
							{
								type: 'pane',
								modal: true,
								w: '620px',
								h: 'auto',
								initial: response,
								hideAtStart: true,
								close: '.cancel'
							}
						);

			/* Setup close button */
			$('modOpts_close').stopObserving();
			$('modOpts_close').observe( 'click',
				function()
				{
					ipb.shoutbox.closePopup('moderator');
				}
			);
		}

		ipb.shoutbox.setupPopup('moderator');
	},

	/**
	 * Do a moderator action
	 */
	modOptsDo: function(event)
	{
		if ( (!ipb.shoutbox.moderator && !ipb.shoutbox.can_edit) || ipb.shoutbox.mod_in_action )
		{
			return false;
		}

		if ( ipb.shoutbox.blur )
		{
			ipb.shoutbox.blur();
		}

		var element = Event.element(event);

		/**
		 * 1.1.0 Alpha
		 *
		 * Leave this if in place or clicking on images in the menu won't trigger the proper id
		 */
		if ( element.tagName == 'IMG' )
		{
			element = element.up('li');
		}

		var modID = (ipb.shoutbox.mod_shout_id) ? ipb.shoutbox.mod_shout_id : ipb.shoutbox.mod_member_id;
		var type  = (ipb.shoutbox.mod_shout_id) ? 'shout' : 'member';

		// Se to true by default and set false if needed
		ipb.shoutbox.mod_in_action = true;

		/* Save our command and compare it with our choices */
		ipb.shoutbox.mod_command = element.id.sub( '_shout', '' );

		switch ( ipb.shoutbox.mod_command )
		{
			case 'edit':
			case 'delete':
			case 'deleteAll':
			case 'ban':
			case 'ban24':
			case 'ban48':
			case 'unban':
			case 'removeMod':
			case 'editHistory':
				break;
			default:
				ipb.shoutbox.mod_in_action = false;
				ipb.shoutbox.produceError('mod_no_action');
				return false; //Leave this return in place and don't change it with a break!
		}

		ipb.shoutbox.updateLastActivity();

		// Setup status
		ipb.shoutbox.popupUpdateStatus('processing');

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=mod&action=loadCommand&command=' + ipb.shoutbox.mod_command + '&modtype=' + type + '&id=' + modID,
			{
				method: 'get',
				encoding: ipb.vars['charset'],
				onSuccess: function(s)
				{
					if ( ipb.shoutbox.checkForErrors(s.responseText) )
					{
						ipb.shoutbox.mod_in_action = false;
						return false;
					}

					/* Update status & content */
					ipb.shoutbox.popupUpdateStatus('mod_loaded_confirm');
					$('shoutbox-popup-content').update( s.responseText );

					/* Setup onclick events */
					if ( ipb.shoutbox.mod_command == 'edit' )
					{
						// Change width & reposition
						$('modOpts_inner').setStyle('width:750px;');
						ipb.positionCenter( $('modOpts_popup') );

						$('mod_edit_shout_confirm').observe('click', ipb.shoutbox.mod_opts_do_edit_shout );
						$('mod_edit_shout_clear').observe('click', ipb.shoutbox.modOptsEditClear );
						$('mod_edit_shout_cancel').observe('click', ipb.shoutbox.modOptsEditReset );
					}
					else if ( ipb.shoutbox.mod_command == 'editHistory' )
					{
						ipb.shoutbox.mod_in_action = false;
						ipb.shoutbox.popupUpdateStatus('processed');
					}
					else
					{
						if ( $('confirm_option_yes') )
						{
							$('confirm_option_yes').observe('click', ipb.shoutbox.modOptsDoConfirm.bindAsEventListener(this) );
						}
						if ( $('confirm_option_no') )
						{
							$('confirm_option_no').observe('click', ipb.shoutbox.modOptsDoConfirm.bindAsEventListener(this) );
						}
					}
				}
			}
		);
	},

	modOptsEditClear: function()
	{
		if ( !ipb.shoutbox.moderator && !ipb.shoutbox.can_edit )
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();

		// Update editor ^.^
		ipb.textEditor.getEditor().CKEditor.setData('');
	},

	modOptsEditReset: function()
	{
		ipb.shoutbox.popupModeratorReset();

		$('modOpts_inner').setStyle('width:550px;');
		ipb.positionCenter( $('modOpts_popup') );
	},

	modOptsDoConfirm: function(event)
	{
		if ( !ipb.shoutbox.moderator || !ipb.shoutbox.mod_in_action )
		{
			return false;
		}

		ipb.shoutbox.updateLastActivity();

		/* Get element from event */
		var element = Event.element(event);

		// Action confirmed?
		if ( element.id == 'confirm_option_yes' )
		{
			var modID = (ipb.shoutbox.mod_shout_id) ? ipb.shoutbox.mod_shout_id : ipb.shoutbox.mod_member_id;
			var type  = (ipb.shoutbox.mod_shout_id) ? 'shout' : 'member';

			// Setup status
			ipb.shoutbox.popupUpdateStatus('processing');

			new Ajax.Request( ipb.shoutbox.baseUrl + 'type=mod&action=performCommand&command=' + ipb.shoutbox.mod_command + '&modtype=' + type + '&id=' + modID,
				{
					method: 'get',
					onSuccess: function(s)
					{
						if ( ipb.shoutbox.checkForErrors(s.responseText) )
						{
							ipb.shoutbox.mod_in_action = false;
							return false;
						}

						ipb.shoutbox.popupUpdateStatus('processed');

						// We have any action to perform?
						switch ( ipb.shoutbox.mod_command )
						{
							case 'delete':
								$('shout-row-'+ipb.shoutbox.mod_shout_id).remove();

								ipb.shoutbox.rewriteShoutClasses();

								// Close popup, no need for it since the shout has been deleted
								ipb.shoutbox.closePopup('moderator');

								/** Update live total shouts count **/
								ipb.shoutbox.updateTotalShouts( ipb.shoutbox.total_shouts - 1 );
								break;
							case 'deleteAll':
								var ids = s.responseText.split(",");

								ids.each( function(id) {
									if ( $('shout-row-'+id) )
									{
										$('shout-row-'+id).remove();
									}
								});

								ipb.shoutbox.closePopup('moderator');

								// Reload the page if we deleted all shouts >.<!
								if ( $('shoutbox-shouts-table').down('tbody').childElements().length < 1 )
								{
									window.location=window.location;
								}

								ipb.shoutbox.rewriteShoutClasses();

								/** Update live total shouts count **/
								ipb.shoutbox.updateTotalShouts( ipb.shoutbox.total_shouts - ids.length );
								break;
							case 'ban':
								$('ban_shout').hide();

								if ( $('unban_shout') )
								{
									$('unban_shout').show();
								}
								break;
							case 'ban':
								$('ban_shout').hide();
								$('ban24_shout').hide();
								$('ban48_shout').hide();
													
								if ( $('unban_shout') )
								{
								$('unban_shout').show();
								}
							break;
							case 'ban24':
							$('ban_shout').hide();
								$('ban24_shout').hide();
								$('ban48_shout').hide();
															
								if ( $('unban_shout') )
								{
								$('unban_shout').show();
							}
							break;
							case 'ban48':
								$('ban_shout').hide();
								$('ban24_shout').hide();
								$('ban48_shout').hide();
								if ( $('unban_shout') )
								{
									$('unban_shout').show();
								}
							break;
							case 'unban':
								$('unban_shout').hide();

								if ( $('ban_shout') )
								{
									$('ban_shout').show();
								}
								break;
							default:
								break;
						}

						ipb.shoutbox.popupModeratorReset();

						// Update properly our status after the reset
						ipb.shoutbox.popupUpdateStatus( s.responseText, true );
					}
				}
			);
		}
		else
		{
			// Reset our popup so =O
			ipb.shoutbox.popupModeratorReset();
		}
	},

	/**
	 * Edit shouts from the global shoutbox! :D
	 * Added in 1.1.1
	 */
	editShout: function(id)
	{
		if ( !id || !ipb.shoutbox.can_edit || ipb.shoutbox.mod_in_action )
		{
			return false;
		}

		if ( ipb.shoutbox.global_on )
		{
			ipb.shoutbox.setActionAndReload('edit|'+id);
			return false;
		}

		// Se to true by default and set false if needed
		ipb.shoutbox.mod_in_action = true;

		ipb.shoutbox.updateLastActivity();

		new Ajax.Request( ipb.shoutbox.baseUrl + 'type=mod&action=loadCommand&command=edit&modtype=shout&id=' + id + '&global=1',
			{
				method: 'get',
				encoding: ipb.vars['charset'],
				onSuccess: function(s)
				{
					if ( ipb.shoutbox.checkForErrors(s.responseText) )
					{
						ipb.shoutbox.mod_in_action = false;
						return false;
					}

					/* We have to setup here our vars or the edit will fail... */
					ipb.shoutbox.mod_shout_id = id;
					ipb.shoutbox.mod_command  = 'edit';

					/* Popup already exist, show it! */
					if ( $('editShout_popup') )
					{
						$('editShout_inner').update( s.responseText );
					}
					else
					{
						ipb.shoutbox.editShoutPopup = new ipb.Popup( 'editShout',
										{
											type: 'pane',
											modal: true,
											w: '750px',
											h: 'auto',
											initial: s.responseText,
											hideAtStart: true,
											close: '.cancel'
										}
									);

						/* Hide close button */
						$('editShout_close').stopObserving();
						$('editShout_close').observe( 'click',
							function()
							{
								ipb.shoutbox.closePopup('editShout');
							}
						);
					}

					/* Run setup */
					ipb.shoutbox.setupPopup('editShout');
				}
			}
		);

		return false;
	}
}

ipb.shoutbox.initialize();
