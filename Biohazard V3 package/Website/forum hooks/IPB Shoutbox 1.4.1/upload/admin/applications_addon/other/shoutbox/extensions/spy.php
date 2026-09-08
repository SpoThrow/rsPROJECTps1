<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

class spyPluginShoutbox implements spyPlugin
{
	public $preParsedData = array();


	public function __construct($registry)
	{
		/* Make registry objects */
		$this->registry = $registry;
		$this->DB       = $this->registry->DB();
		$this->memberData =& $this->registry->member()->fetchMemberData();
		$this->lang		= $this->registry->getClass('class_localization');
		$this->settings =& $this->registry->fetchSettings();

		ipsRegistry::getAppClass( 'shoutbox' );
	}

	public function checkPermissions( $data )
	{
		/* Check temporary ban status */
		if( $this->memberData['temp_ban'] )
		{
			# Let's just return, it is useless to process it since it is already done in ipsRegistry
			return FALSE;
		}

		/* Check the new way to ban */
		if ( $this->memberData['member_banned'] == 1 )
		{
			return FALSE;
		}

		/* No permission to view the board */
		if ( $this->memberData['g_view_board'] != 1 )
		{
			return FALSE;
		}

		/* Offline or can't view? */
		if ( !$this->settings['shoutbox_online'] || !$this->memberData['g_shoutbox_view'] || (!$this->memberData['g_shoutbox_view'] && !$this->memberData['g_shoutbox_use']) || ( $this->settings['board_offline'] && !$this->memberData['g_access_offline'] ) )
		{
			return FALSE;
		}

		/** Are we shoutbox banned? **/
		if( $this->memberData['_cache']['shoutbox_banned'] )
		{
			return FALSE;
		}

		/* Login enforced? */
		if( !$this->memberData['member_id'] && $this->settings['force_login'] == 1 )
		{
			return FALSE;
		}


		/* Return */
		return TRUE;
	}

	public function preParseData( $data )
	{
		if ( !$this->registry->isClassLoaded( 'shoutboxLibrary' ) )
		{
			require_once( IPSLib::getAppDir( 'shoutbox' ) . "/sources/classes/library.php" );
			$this->registry->setClass( 'shoutboxLibrary', new shoutboxLibrary( $this->registry ) );
		}
		$this->registry->shoutboxLibrary->_loadParserAndEditor();
		$this->registry->shoutboxLibrary->parser->parsing_mgroup        = $this->memberData['member_group_id'];
		$this->registry->shoutboxLibrary->parser->parsing_mgroup_others = $this->memberData['mgroup_others'];
		/* Add to our array */

		switch( $data['data_type'] )
		{
			case 'added_shout':
				$s = $this->registry->shoutboxLibrary->getShout( $data['type_id'] );
				if( ! $s['s_id'] ) break;		// Shout not there don't show
				$s['s_message'] = $this->registry->shoutboxLibrary->parser->preDisplayParse( $s['s_message'] );
				$data['what'] = IPSText::truncate( strip_tags( $s['s_message'] ), 100 );
				$data['what_link'] = $this->registry->output->buildSEOUrl( 'app=shoutbox', 'public', '', 'shoutbox' );;
				/* Set the WHERE part */
				$data['where'] = IPSLib::getAppTitle('shoutbox');
				$data['where_link'] = $this->registry->output->buildSEOUrl( 'app=shoutbox', 'public', '', 'shoutbox' );
				/* Data type clean */
				$data['data_type_clean'] = $this->lang->words[ 'spy_shoutbox_' . $data['data_type'] ];
				$data['replies'] = '&nbsp;';
				$this->preParsedData[] = $data;
				break;
		}
	}

	public function parseData( $data )
	{
		/* Return */
		return $data;
	}

}


