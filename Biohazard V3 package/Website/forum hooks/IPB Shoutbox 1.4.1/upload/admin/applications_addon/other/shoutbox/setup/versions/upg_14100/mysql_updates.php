<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

$TABLE[] = "ALTER TABLE groups ADD g_shoutbox_old_req INT(11) NOT NULL DEFAULT '0'";
$TABLE[] = "ALTER TABLE groups ADD g_shoutbox_old_req_display TINYINT(1) NOT NULL DEFAULT '1'";
