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

class public_shoutbox_view_display extends ipsCommand
{
	/**
	 * Shortcut for our library
	 * 
	 * @var		object
	 * @access	public
	 */
	public $library;
	protected $output;
	
	public function doExecute( ipsRegistry $registry )
	{
		/* Let's setup our shortcut =D */
		$this->library = $this->registry->getClass('shoutboxLibrary');
		
		/* Nav and page title stuff */
		$this->registry->output->addNavigation( $this->settings['shoutbox_title'], 'app=shoutbox', 'foo', 'app=shoutbox' );
		
		$this->library->_startup();
		
		if ( !$this->settings['shoutbox_online'] )
		{
			$this->library->show_error('shoutbox_offline', 'SB-OFF');
		}
		elseif ( !$this->memberData['g_shoutbox_view'] )
		{
			$this->library->show_error('no_view_perm', 'SB-VIEW');
		}
		elseif ( !$this->memberData['g_shoutbox_view'] && !$this->memberData['g_shoutbox_use'] )
		{
			$this->library->show_error('no_use_perm', 'SB-USE');
		}
		elseif ( $this->memberData['_cache']['shoutbox_banned'] )
		{
			if ($this->memberData['_cache']['shoutbox_banned_howlong'] == 24)
				$this->library->show_error('banned24', 'SB-BAN-24');
			else if ($this->memberData['_cache']['shoutbox_banned_howlong'] == 48)
				$this->library->show_error('banned48', 'SB-BAN-48');
			else
				$this->library->show_error('banned', 'SB-BAN');
		}
		
		/* If we don't have enough posts to use it reset some things */
		$this->memberData['g_shoutbox_posts_req'] = intval($this->memberData['g_shoutbox_posts_req']);
		
		if ( $this->memberData['g_shoutbox_use'] && $this->memberData['g_shoutbox_posts_req'] > 0 )
		{
			if ( $this->memberData['posts'] < $this->memberData['g_shoutbox_posts_req'] )
			{
				$this->memberData['g_shoutbox_use'] = 0;
				$this->library->moderator = 0;
			}
		}

		$this->memberData['g_shoutbox_old_req'] = intval($this->memberData['g_shoutbox_old_req']);
		if ( $this->memberData['g_shoutbox_use'] && $this->memberData['g_shoutbox_old_req'] > 0 )
		{
			$joined = time() - intval($this->memberData['joined']);
			$joined = floor($joined / 86400);
			if ( $joined < $this->memberData['g_shoutbox_old_req'] )
			{
				$this->memberData['g_shoutbox_use'] = 0;
				$this->library->moderator = 0;
			}
		}
		
		/* Setup our data array */
		$d = array( 'langs'           => $this->library->show_lang_for_js_use(),
					'shout_height'    => $this->library->prefs['shoutbox_height'],
					'nomods'          => $this->library->_noModsMessage(),
					'noshouts'        => $this->library->_noShoutsMessage(),
					'shouts'          => $this->library->return_shouts(1),
					'announcement'    => $this->library->_get_announcement( false ),
					'smilies'         => $this->library->editor->fetchEmoticons(),
					'bypassCKEditor'  => 0,
					);
		
		// New template with JS moved out of ajax_updates
		$d['js'] = $this->registry->output->getTemplate('shoutbox')->javascript( $d );
		
		$c = IPSCookie::get('_shoutbox_jscmd');
		
		if ( $c != "" )
		{
			$x = explode("|", $c );
			
			if ( is_array($x) && count($x) )
			{
				$d['langs'] .= $this->_parse_and_load_js_command( $x );
			}
		}
		
		/* Get top shouter */
		$topShouter = $this->_getTopShouter();
		$myCount    = $this->_getMyShoutCount();
		$statistics = $this->library->getMembersViewing( false, true );
		
		/* Add to Output */
		$this->output .= $this->registry->output->getTemplate('shoutbox')->shoutbox( $d, array_merge( $statistics, $topShouter, $myCount ) );
		
		/* What to do? */
		switch ( $this->request['do'] )
		{
			case 'popup':
				$this->renderPopup();
			break;
			
			default:
				$this->renderFull();
			break;
		}
	}
	
	private function renderPopup()
	{
		/* Is this enabled? */
		if ( !$this->settings['shoutbox_popup'] )
		{
			return;
		}
		
		/* Add in the hovercards because this doesn't get done in popups */
		$this->registry->output->addJSModule( 'hovercard', 0 );
		
		/* Send to screen */
		$this->registry->output->setTitle( $this->settings['shoutbox_title'] );
		$this->registry->output->popUpWindow( $this->registry->output->getTemplate('shoutbox')->popupWrapper( $this->output ) );
	}
	
	private function renderFull()
	{
		/* Append Copyright */
		if ( !$this->settings['ipb_copy_number'] OR !$this->settings['ips_cp_purchase'] )
		{
			$this->output .= "<div class='desc right' style='margin-top:10px;'>Powered by IPB Shoutbox {$this->caches['app_cache']['shoutbox']['app_version']} &copy; " . date('Y') . ", by <a href='http://www.ipbshoutbox.com' rel='nofollow external'>IPB Shoutbox</a></div>";
		}
		
		/* Send to screen */
		$this->registry->output->setTitle( $this->settings['shoutbox_title'] );
		$this->registry->output->addContent( $this->output );
		$this->registry->output->sendOutput();
	}
	
	private function _parse_and_load_js_command( $a=array() )
	{
		if ( !is_array($a) || !count($a) )
		{
			return "";
		}
		
		$returnData = array();
		
		// Reset cookie or we return here again
		IPSCookie::set('_shoutbox_jscmd', '', 0);
		
		switch ( $a[0] )
		{
			case 'mod':
				if ( $this->library->moderator )
				{
					if ( $a[1] == 'shout' )
					{
						$id = intval($a[2]);
						$s  = $this->library->getShout( $id );

						if ( is_array($s) && count($s) && $s['s_id'] )
						{
							$returnData = array( 'type' => 'mod-shout', 'id' => $id );
						}
					}
					elseif ( $a[1] == 'member' )
					{
						$a[2] = trim($a[2]);
						$mid  = 0;
						
						if ( $a[2] == 'number' )
						{
							$mid = intval($a[3]);
						}
						elseif ( $a[2] == 'string' )
						{
							$mn = trim($a[3]);
							if ( $mn != '' )
							{
								$md = $this->library->_get_member( (string)$mn );
								if ( intval($md['member_id']) )
								{
									$mid = intval($md['member_id']);
								}
							}
						}
						
						if ( $mid )
						{
							$returnData = array( 'type' => 'mod-member', 'mid' => $mid );
						}
					}
				}
				break;
			case 'archive':
				if ( $this->memberData['g_shoutbox_view_archive'] )
				{
					$returnData = array( 'type' => 'archive' );
				}
				break;
			case 'myprefs':
				if ( $this->memberData['member_id'] && $this->memberData['g_shoutbox_use'] )
				{
					$returnData = array( 'type' => 'myprefs' );
				}
				break;
			case 'edit':
				$id = intval($a[1]);
				$returnData = array( 'type' => 'edit-shout', 'id' => $id );
			default:
				break;
		}
		
		if ( count($returnData) )
		{
			return $this->registry->output->getTemplate('shoutbox')->ajax_jscmd( $returnData );
		}
		
		return "";
	}
	
	private function _getTopShouter()
	{
		/* Init vars */
		$data   = array();
		$return = array( 'top_shouter_id' => 0 );
		
		# Stats are enabled?
		$data = $this->DB->buildAndFetch( array( 'select'   => 'COUNT(s.s_mid) AS shouts, s.s_mid',
												 'from'     => array( 'shoutbox_shouts' => 's' ),
												 'where'    => 's.s_mid > 0',
												 'group'    => 's.s_mid',
												 'order'    => 'shouts DESC',
												 'limit'    => array( 0, 1 ),
												 'add_join' => array( array( 'select' => 'm.member_id, m.members_display_name, m.member_group_id, m.members_seo_name',
																			 'from'   => array( 'members' => 'm' ),
																			 'where'  => 'm.member_id=s.s_mid' )
																	 )
										 )		);
		
		$data['shouts'] = intval($data['shouts']);
		
		# Format name & link
		if ( $data['member_id'] && $data['shouts'] > 0 )
		{
			$data['members_display_name'] = $this->settings['shoutbox_format_names'] ? IPSMember::makeNameFormatted( $data['members_display_name'], $data['member_group_id'] ) : $data['members_display_name'];
			
			$return['top_shouter_name'] = IPSMember::makeProfileLink( $data['members_display_name'], $data['member_id'], $data['members_seo_name']);
			
			# Pass the other data
			$return['top_shouts_num']  = $data['shouts'];
			$return['top_shouter_id']  = $data['member_id'];
			$return['top_shouter_seo'] = $data['members_seo_name'];
		}
		
		return $return;
	}
	
	private function _getMyShoutCount()
	{
		/* Init */
		$return = array();
		
		/* Are you a member? */
		if ( $this->memberData['member_id'] )
		{
			$data = $this->DB->buildAndFetch( array( 'select' => 'COUNT(*) AS shouts', 'from' => 'shoutbox_shouts', 'where' => 's_mid=' . $this->memberData['member_id'] ) );
			$return['my_shouts_num'] = intval( $data['shouts'] );
		}
		
		return $return;
	}
}