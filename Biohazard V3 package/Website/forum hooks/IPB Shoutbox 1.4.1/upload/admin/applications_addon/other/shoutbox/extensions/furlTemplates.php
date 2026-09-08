<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

if ( !defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

$_SEOTEMPLATES = array( 'app=shoutbox'  => array( 
						'app'           => 'shoutbox',
						'allowRedirect' => 1,
						'out'           => array( '/app=shoutbox$/i', 'shoutbox/' ),
						'in'            => array( 'regex'   => "#^/shoutbox#i",
												  'matches' => array( array( 'app', 'shoutbox' ) ) ) ),
					  );