<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

if ( ! defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

class task_item
{
	/**
	 * Parent task manager class
	 *
	 * @access	protected
	 * @var		object
	 */
	protected $class;

	/**
	 * This task data
	 *
	 * @access	protected
	 * @var		array
	 */
	protected $task			= array();

	/**
	 * Prevent logging
	 *
	 * @access	protected
	 * @var		boolean
	 */
	protected $restrict_log	= false;

	/**
	* Registry Object Shortcuts
	*/
	protected $registry;
	protected $DB;
	protected $settings;
	protected $lang;

	/**
	 * Constructor
	 *
	 * @access	public
	 * @param 	object		ipsRegistry reference
	 * @param 	object		Parent task class
	 * @param	array 		This task data
	 * @return	void
	 */
	public function __construct( ipsRegistry $registry, $class, $task )
	{
		/* Make registry objects */
		$this->registry	= $registry;
		$this->DB		= $this->registry->DB();
		$this->settings =& $this->registry->fetchSettings();
		$this->lang		= $this->registry->getClass('class_localization');

		$this->class	= $class;
		$this->task		= $task;
	}

	/**
	 * Run this task
	 *
	 * @access	public
	 * @return	void
	 */
	public function runTask()
	{
		$this->lang->loadLanguageFile( array( 'public_shoutbox' ), 'shoutbox' );

		$done = 0;

		$this->DB->build( array( 'select' => 'member_id, members_cache',
								 'from'   => 'members',
								 'where'  => "members_cache LIKE '%shoutbox_banned\";i:1%'"
						 )		);
		$this->DB->execute();

		$res = array();
		while ( $member = $this->DB->fetch() )
		{
			if ( !$member['member_id'] )
				return false;
		
			$member['_cache'] = IPSMember::unpackMemberCache( $member['members_cache'] );
			if ($member['_cache']['shoutbox_banned'] && $member['_cache']['shoutbox_banned_howlong'] && $member['_cache']['shoutbox_banned_time'])
			{
				$diff = time() - $member['_cache']['shoutbox_banned_time'];
				$diff = floor($diff / 3600);
				if ($diff > $member['_cache']['shoutbox_banned_howlong'])
				{
					unset($member['_cache']['shoutbox_banned']);
					unset($member['_cache']['shoutbox_banned_howlong']);
					unset($member['_cache']['shoutbox_banned_time']);
					$res[$member['member_id']] = serialize($member['_cache']);
					$done = $done + 1;
				}
			}
		}

		foreach($res as $key => $value)
			$this->DB->update( 'members', array( 'members_cache' => $value ), 'member_id='.$key );
		//-----------------------------------------
		// Log to logs table
		//-----------------------------------------
		if ( !$this->restrict_log )
		{
            $this->class->appendTaskLog( $this->task, sprintf( $this->lang->words['task_removedbanned_shouts'], $done ) );
		}
		//-----------------------------------------
		// Unlock Task: DO NOT MODIFY!
		//-----------------------------------------
		$this->class->unlockTask( $this->task );
	}
}
?>
