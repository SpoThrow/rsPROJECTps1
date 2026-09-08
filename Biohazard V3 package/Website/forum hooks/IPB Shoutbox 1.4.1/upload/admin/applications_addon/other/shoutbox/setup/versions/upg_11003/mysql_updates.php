<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

/* Normally the table should be there but let's prevent errors just in case... */
if ( ipsRegistry::DB()->checkForTable( 'shoutbox_upgrade_history' ) )
{
	// Remove the old upgrade_history table
	$SQL[] = "DROP TABLE shoutbox_upgrade_history;";
}

# Delete older templates since we use skin_shoutbox_hooks now =O
$SQL[] = "DELETE FROM skin_templates WHERE template_group='skin_shoutbox' AND template_name IN ('shoutbox_global','hookActiveUsers');";