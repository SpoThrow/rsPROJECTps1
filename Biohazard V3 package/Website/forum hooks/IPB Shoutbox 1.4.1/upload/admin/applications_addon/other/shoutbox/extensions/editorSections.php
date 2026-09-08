<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

if ( !defined('IN_ACP') )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded 'admin.php'.";
	exit();
}

/*
 * An array of key => value pairs
 * When going to parse, the key should be passed to the editor
 *  to determine which bbcodes should be parsed in the section
 *
 */
ipsRegistry::getClass('class_localization')->loadLanguageFile( array( 'admin_shoutbox' ), 'shoutbox' );
$BBCODE	= array( 'shoutbox_shouts'	=> ipsRegistry::getClass('class_localization')->words['ctype__shouts'],
				);