<?php

/**
 *	Topic Moderators
 *
 * @author 		Martin Aronsen
 * @copyright	2008 - 2012 Invision Modding
 * @web: 		http://invisionmodding.com
 * @IPB ver.:	IP.Board 3.3
 * @version:	2.1.1  (21001)
 *
 */
 
if ( ! defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

$_RESET = array();

$_LOAD = array(
				'topicmod' => 1
				);

$CACHE['topicmod'] = array( 
								'array'            => 1,
								'allow_unload'     => 0,
							    'default_load'     => 1,
							    'recache_file'     => IPSLib::getAppDir( 'topicMod' ) . '/modules_admin/core/moderators.php',
								'recache_class'    => 'admin_topicMod_core_moderators',
							    'recache_function' => 'rebuildCache' 
							);			

//-----------------------------------------
// Bitwise Options
//-----------------------------------------

$_BITWISE = array( 'moderators' => array( 'gbw_soft_delete',
										  'gbw_un_soft_delete',
										  'gbw_soft_delete_see',
										  'gbw_soft_delete_reason',
										  'gbw_soft_delete_see_post' ) );