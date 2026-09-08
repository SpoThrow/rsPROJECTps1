<?php
/**
 * @ Application : 		Custom Moderator Team Page v2.0.0
 * @ Last Updated : 	June 13th, 2012 
 * @ Author :			Michael S. Edwards
 * @ Copyright :		(c) 2011 Coding Jungle
 * @ Link	 :			http://www.codingjungle.com/
 */

$_LOAD = array( 'cmtp_groups'   => 1
				);
$_LOAD = array( 'cmtp_members'   => 1
				);
$CACHE['cmtp_groups'] = array( 'array'				=> 1,
								   'allow_unload'		=> 0,
								   'default_load'		=> 1,
								   'recache_file'		=> IPSLib::getAppDir( 'cmtp' ) . '/sources/cmtp.php',
								   'recache_class'		=> 'cmtp',
								   'recache_function'	=> 'cacheGroups'
								   );


$CACHE['cmtp_members'] = array( 'array'				=> 1,
								   'allow_unload'		=> 0,
								   'default_load'		=> 1,
								   'recache_file'		=> IPSLib::getAppDir( 'cmtp' ) . '/sources/cmtp.php',
								   'recache_class'		=> 'cmtp',
								   'recache_function'	=> 'cacheMembers'
								   );

/**
* Array for holding reset information
*
* Populate the $_RESET array and ipsRegistry will do the rest
*/
$_RESET = array();
