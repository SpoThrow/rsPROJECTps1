<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

$_LOAD = array( 'shoutbox_shouts'   => 1,
				'shoutbox_mods'	    => 1,
				// We load also those caches to avoid
				// loading them using 1 query each cache
				'emoticons'         => 1,
				'ranks'             => 1,
				'reputation_levels' => 1
				);

$CACHE['shoutbox_shouts'] = array( 'array'				=> 1,
								   'allow_unload'		=> 0,
								   'default_load'		=> 1,
								   'recache_file'		=> IPSLib::getAppDir( 'shoutbox' ) . '/sources/classes/library.php',
								   'recache_class'		=> 'shoutboxLibrary',
								   'recache_function'	=> 'recacheShouts'
								   );

$CACHE['shoutbox_mods']		= array(
								'array'				=> 1,
								'allow_unload'		=> 0,
								'default_load'		=> 1,
								'recache_file'		=> IPSLib::getAppDir( 'shoutbox' ) . '/sources/classes/library.php',
								'recache_class'		=> 'shoutboxLibrary',
								'recache_function'	=> 'recacheModerators'
							);


/**
* Array for holding reset information
*
* Populate the $_RESET array and ipsRegistry will do the rest
*/
$_RESET = array();

###### Redirect requests... ######

//TODO: Those redirects are really needed?
if ( $_GET['autocom'] == 'shoutbox' )
{
	$_RESET['app'] = 'shoutbox';
}

if( ! isset( $_REQUEST['module'] ) )
{
	$_RESET['app']		= 'shoutbox';
	$_RESET['module']	= 'view';
	$_RESET['section']	= 'display';
}


# ALL
if ( $_REQUEST['CODE'] || $_REQUEST['code'] )
{
	$_RESET['do'] = $_REQUEST['CODE'] ? $_REQUEST['CODE'] : $_REQUEST['code'];
}