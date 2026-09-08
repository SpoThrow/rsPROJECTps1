<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

$SQL[] = "ALTER TABLE members ADD shoutbox_shouts BIGINT( 8 ) NOT NULL DEFAULT '0';";
$SQL[] = "ALTER TABLE members CHANGE shoutbox_shouts shoutbox_shouts BIGINT( 8 ) NOT NULL DEFAULT '0';";
