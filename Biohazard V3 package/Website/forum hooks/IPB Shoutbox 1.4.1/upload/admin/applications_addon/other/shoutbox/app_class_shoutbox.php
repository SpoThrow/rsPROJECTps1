<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

class app_class_shoutbox
{
	/**
	 * Constructor
	 *
	 * @access	public
	 * @param	object	ipsRegistry
	 * @return	void
	 */
	public function __construct( ipsRegistry $registry )
	{
		require_once( IPSLib::getAppDir( 'shoutbox' ) . "/sources/classes/library.php" );
		$registry->setClass( 'shoutboxLibrary', new shoutboxLibrary( $registry ) );
		
		if ( IN_ACP )
		{
			$registry->getClass('class_localization')->loadLanguageFile( array( 'admin_shoutbox' ), 'shoutbox' );
		}
		else
		{
			$registry->getClass('class_localization')->loadLanguageFile( array( 'public_shoutbox' ), 'shoutbox' );
		}
	}
}