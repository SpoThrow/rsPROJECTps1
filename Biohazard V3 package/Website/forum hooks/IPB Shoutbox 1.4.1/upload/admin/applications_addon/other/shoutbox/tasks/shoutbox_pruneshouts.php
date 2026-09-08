<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

if ( !defined( 'IN_IPB' ) )
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

		//-----------------------------------------
		// Prune Old Shouts
		//-----------------------------------------
		$days = intval( $this->settings['shoutbox_autoprune'] );
		$done = 0;
		
		if ( $days != 0 )
		{
			/* Get time */
			$time = ($days == -1) ? time() : time() - ( $days * 86400 );

			$this->DB->delete( "shoutbox_shouts", "s_date < $time" );
			
			$done = $this->DB->getAffectedRows();
			
			//-------------------------------
			// Load Lib & Rebuild Cache
			//-------------------------------
			require_once( IPSLib::getAppDir( 'shoutbox' ) . '/sources/classes/library.php' );
			$library = new shoutboxLibrary( $this->registry );
			$library->recacheShouts('recount');
			
			//-----------------------------------------
			// Log to logs table
			//-----------------------------------------
			if ( !$this->restrict_log )
			{
	            $this->class->appendTaskLog( $this->task, sprintf( $this->lang->words['task_pruned_shouts'], $done ) );
			}
		}

		//-----------------------------------------
		// Unlock Task: DO NOT MODIFY!
		//-----------------------------------------
		$this->class->unlockTask( $this->task );
	}
}