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

class version_upgrade
{
	/**
	 * Custom HTML to show
	 *
	 * @access	private
	 * @var		string
	 */
	private $_output = '';
	
	/**
	* fetchs output
	* 
	* @access	public
	* @return	string
	*/
	public function fetchOutput()
	{
		return $this->_output;
	}
	
	/**
	 * Execute selected method
	 *
	 * @access	public
	 * @param	object		Registry object
	 * @return	void
	 */
	public function doExecute( ipsRegistry $registry ) 
	{
		/* Make object */
		$this->registry = $registry;
		
		/* Let's remove the old hooks files to avoid confusion */
		$hooksPath = IPSLib::getAppDir('shoutbox') . '/xml/hooks/';
		
		# Global shoutbox
		@unlink( $hooksPath . 'global_shoutbox_top.xml' );
		@unlink( $hooksPath . 'global_shoutbox_bottom.xml' );
		# Active users
		@unlink( $hooksPath . 'active_users_stats.xml' );
		@unlink( $hooksPath . 'active_users_sidebar.xml' );
		
		
		$this->registry->output->addMessage("Custom upgrade script run");
		
		return true;
	}
}