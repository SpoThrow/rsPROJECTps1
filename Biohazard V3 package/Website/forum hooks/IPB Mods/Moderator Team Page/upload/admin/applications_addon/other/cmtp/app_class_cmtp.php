<?php
/**
 * @ Application : 		Custom Moderator Team Page v2.0.0
 * @ Last Updated : 	June 13th, 2012 
 * @ Author :			Michael S. Edwards
 * @ Copyright :		(c) 2011 Coding Jungle
 * @ Link	 :			http://www.codingjungle.com/
 */
class app_class_cmtp
{
	/**
	 * Constructor
	 *
	 * @access	public
	 * @param	object	ipsRegistry
	 * @return	@e void
	 */
	public function __construct( ipsRegistry $registry )
	{
		//-----------------------------------------
		// Could potentially be setup from sessions
		//-----------------------------------------
		
		if( !$registry->isClassLoaded( 'cmtp' ) )
		{
			$classToLoad	= IPSLib::loadLibrary( IPSLib::getAppDir('cmtp') . '/sources/cmtp.php', 'cmtp', 'cmtp' );
			$registry->setClass( 'cmtp', new $classToLoad( $registry ) );
		}
 

	}
}