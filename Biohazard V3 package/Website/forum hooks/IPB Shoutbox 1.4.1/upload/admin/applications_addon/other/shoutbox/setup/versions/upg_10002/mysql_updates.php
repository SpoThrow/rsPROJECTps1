<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

// Add new group permission to view shoutbox but not use it
$SQL[] = "ALTER TABLE groups ADD g_shoutbox_view TINYINT(1) NOT NULL DEFAULT '1';";

// Add new group permission to let users edit their shouts
$SQL[] = "ALTER TABLE groups ADD g_shoutbox_edit TINYINT(1) NOT NULL DEFAULT '0';";

// Remove old setting not used anymore
$SQL[] = "DELETE FROM conf_settings WHERE conf_key='shoutbox_global_exclude_pages';";