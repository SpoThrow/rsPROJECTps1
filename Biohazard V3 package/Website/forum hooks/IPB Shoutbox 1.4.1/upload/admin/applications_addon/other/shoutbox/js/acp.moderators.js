/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

ACPShoutbox = {
	// Constructor
	init: function()
	{
		Debug.write("Initializing acp.moderators.js");
		
		document.observe("dom:loaded", function(){
			if( $('m_mg_id') )
			{
				ACPShoutbox.autoComplete = new ipb.Autocomplete( $('m_mg_id'), { multibox: false, url: acp.autocompleteUrl, templates: { wrap: acp.autocompleteWrap, item: acp.autocompleteItem } } );
			}
		});
	}
};

ACPShoutbox.init();