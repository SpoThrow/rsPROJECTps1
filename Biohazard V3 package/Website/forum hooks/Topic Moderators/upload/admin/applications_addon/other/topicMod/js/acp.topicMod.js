/************************************************/
/* Topic Moderators								*/
/* -------------------------------------------- */
/* acp.topicMod.js						 		*/
/* (c) Invision Modding 2011					*/
/* -------------------------------------------- */
/* Author: Martin Aronsen						*/
/************************************************/

var _topicMod = window.IPBACP;
_topicMod.prototype.topicMod = 
{
	/*------------------------------*/
	/* Constructor 					*/
	init: function()
	{
		Debug.write("Initializing acp.topicMod.js");
		
		document.observe("dom:loaded", function()
		{
			$( 'topic_id' )[ $F( 'moderate_own' ) == 1 ? 'disable' : 'enable' ]();
			$( 'forumsDropdown' )[ $F( 'moderate_own' ) == 1 ? 'show' : 'hide' ]();
			
			if ( $( 'gbw_un_soft_delete_yes' ).checked && $( 'gbw_soft_delete_see_no' ).checked)
			{
				$( 'gbw_soft_delete_see_yes' ).checked = 'checked';
			}
			
			if ( $( 'gbw_soft_delete_see_no' ).checked && $( 'gbw_un_soft_delete_yes' ).checked )
			{
				$( 'gbw_un_soft_delete_no' ).checked = 'checked';
			}
			
			acp.topicMod.initEvents();
			
		});
	},
	
	initEvents: function()
	{
		var autoComplete = new ipb.Autocomplete( $( 'memberName' ), { multibox: false, url: acp.autocompleteUrl, templates: { wrap: acp.autocompleteWrap, item: acp.autocompleteItem } } );
	
		var eventMethods = 'blur,focus,change,keyup';
		
		if ( Prototype.Browser.WebKit || Prototype.Browser.Gecko )
		{
			eventMethods += ',input';
		}

		eventMethods.split(',').each( function(event)
		{
			$( 'topic_id' ).observe( event, acp.topicMod.fetchTopicTitle );
		});
		
		$( 'moderate_own' ).observe( 'click', function( e )
		{
			$( 'topic_id' )[ $F( 'moderate_own' ) == 1 ? 'disable' : 'enable' ]();

			$( 'forumsDropdown' )[ $F( 'moderate_own' ) == 1 ? 'appear' : 'fade' ]({duration:0.3});
		});
		
		$( 'gbw_un_soft_delete_yes' ).observe( 'click', function(e)
		{
			$( 'gbw_soft_delete_see_yes' ).checked = 'checked';
		});
		
		$( 'gbw_soft_delete_see_no' ).observe( 'click', function(e)
		{
			$( 'gbw_un_soft_delete_no' ).checked = 'checked';
		});
		
		$( 'adform' ).observe( 'submit', acp.topicMod.validateForm );
		
		
	},
	
	validateForm: function( e )
	{
		Event.stop(e);
		
		msg = "";
		
		if ( $F( 'moderate_own' ) == 0 )
		{
			if ( $F( 'topic_id' ) == "" || $F( 'topic_id' ) == "0" )
			{
				msg += "<li>No topic ID added</li>";
			}
		}
		
		if ( msg != "" )
		{
			$( 'formWarningBox' ).update( "The following errors were found:<br /><ul>" + msg + "</ul>" ).show();
		}
		else
		{
			$( 'adform' ).submit();
		}
	},
	
	fetchTopicTitle: function( e )
	{
		Event.stop( e );
		
		var url = ipb.vars['base_url'] + 'app=topicMod&module=ajax&section=ajax&do=getTopicTitle',
		url = url.replace( /&amp;/g, '&' );
		
		new Ajax.Request( url,
		{
			method: 'post',
			evalJSON: 'force',
			parameters: {
				'md5check': ipb.vars['md5_hash'],
				'topic_id': $F( 'topic_id' )
			},
			onSuccess: function( t )
			{
				if( Object.isUndefined( t.responseJSON ) )
				{
					alert( "Bad Request" );
				}
				else if ( t.responseJSON['error'] )
				{
					$( 'topicTitle' ).update( t.responseJSON['error'] );
					new Effect.Highlight( $( 'topicData' ), { startcolor: '#ade57a' } );
				}
				else if ( t.responseJSON['tid'] )
				{
					$( 'topicTitle' ).update( "Topic Title: " + t.responseJSON['topicLink'] );

					new Effect.Highlight( $( 'topicData' ), { startcolor: '#ade57a' } );

				}
			}
		});
		
		return false;
		
	}
};

acp.topicMod.init();