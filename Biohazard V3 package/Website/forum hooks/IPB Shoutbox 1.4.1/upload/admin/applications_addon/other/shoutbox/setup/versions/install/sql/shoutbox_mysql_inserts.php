<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

// First Shout
$INSERT[] = "INSERT INTO shoutbox_shouts (s_id, s_mid, s_date, s_message, s_ip, s_edit_history) VALUES ( NULL, '0', UNIX_TIMESTAMP(), 'Congratulations, you have successfully installed Shoutbox!<br />Now you need to setup the shoutbox permissions in ACP -> Members TAB -> Member Groups -> Manage Member Groups -> Edit a Group -> Shoutbox TAB', '127.0.0.1', NULL );";