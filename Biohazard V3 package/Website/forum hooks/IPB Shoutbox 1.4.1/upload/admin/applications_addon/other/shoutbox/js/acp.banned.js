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
		Debug.write("Initializing acp.banned.js");
		
		document.observe("dom:loaded", function(){
			if( $('banMemberName') )
			{
				ACPShoutbox.autoComplete = new ipb.Autocomplete( $('banMemberName'), { multibox: false, url: acp.autocompleteUrl, templates: { wrap: acp.autocompleteWrap, item: acp.autocompleteItem } } );
			}
		});
	}
};

ACPShoutbox.init();