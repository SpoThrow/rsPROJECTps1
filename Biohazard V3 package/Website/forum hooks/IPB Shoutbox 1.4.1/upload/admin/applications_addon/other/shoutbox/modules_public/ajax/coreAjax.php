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

class public_shoutbox_ajax_coreAjax extends ipsAjaxCommand
{
	/**
	 * Shortcut for our library
	 * 
	 * @var		object
	 * @access	public
	 */
	public $library;

	public function doExecute( ipsRegistry $registry )
	{
		/* Let's setup our shortcut =D */
		$this->library = $this->registry->getClass('shoutboxLibrary');
		
		/* Startup */
		$this->library->_startup();
		
		if ( !$this->settings['shoutbox_online'] )
		{
			$this->returnError('shoutbox_offline');
		}

		if ( !$this->memberData['g_shoutbox_view'] )
		{
			if ( !$this->memberData['g_shoutbox_use'] )
			{
				$this->returnError('no_use_perm');
			}

			$this->returnError('no_view_perm');
		}
		
		# Banned?
		if ( $this->memberData['_cache']['shoutbox_banned'] )
		{
			if ($this->memberData['_cache']['shoutbox_banned_howlong'] == 24)
				$this->returnError('banned24');
			else if ($this->memberData['_cache']['shoutbox_banned_howlong'] == 48)
				$this->returnError('banned48');
			else
				$this->returnError('banned');
		}
		
		# Trim type
		$this->request['type'] = trim($this->request['type']);
		
		/* If we don't have enough posts to use it reset some things */
		$this->memberData['g_shoutbox_posts_req'] = intval($this->memberData['g_shoutbox_posts_req']);
		
		if ( $this->memberData['g_shoutbox_use'] && $this->memberData['g_shoutbox_posts_req'] > 0 )
		{
			if ( $this->memberData['posts'] < $this->memberData['g_shoutbox_posts_req'] )
			{
				if ( in_array($this->request['type'], array( 'getShouts', 'getMembers' )) )
				{
					$this->memberData['g_shoutbox_use'] = 0;
					$this->library->moderator = 0;
				}
				else
				{
					if ( $this->memberData['g_shoutbox_posts_req_display'] == 1 )
					{
						$this->returnError( sprintf( $this->lang->words['error_no_use_posts_display'], $this->lang->formatNumber($this->memberData['g_shoutbox_posts_req']) ) );
					}
					
					$this->returnError('no_use_posts');
				}
			}
		}
		
		$this->memberData['g_shoutbox_old_req'] = intval($this->memberData['g_shoutbox_old_req']);
		if ( $this->memberData['g_shoutbox_use'] && $this->memberData['g_shoutbox_old_req'] > 0 )
		{
			$joined = time() - intval($this->memberData['joined']);
			$joined = floor($joined / 86400);
			if ( $joined < $this->memberData['g_shoutbox_old_req'] )
			{
				if ( in_array($this->request['type'], array( 'getShouts', 'getMembers' )) )
				{
					$this->memberData['g_shoutbox_use'] = 0;
					$this->library->moderator = 0;
				}
				else
				{
					if ( $this->memberData['g_shoutbox_old_req_display'] == 1 )
					{
						$this->returnError( sprintf( $this->lang->words['error_no_use_old_display'], $this->lang->formatNumber($this->memberData['g_shoutbox_old_req']) ) );
					}	
					$this->returnError('no_use_old');
				}
			}
		}

		/* Check auth key */
		// Don't check authKey for refresh, causes troubles with sessions
		
		# Commented out since from IPB 3.0.2 and above each ajax call automatically checks the secure_key
		/*if ( !in_array($this->request['type'], array( 'getShouts', 'getMembers' )) && trim($this->request['secure_key']) != $this->member->form_hash )
		{
			$this->returnError('auth_key_expired');
		}*/
		
		switch ($this->request['type'])
		{
			case 'getShouts':
				$this->library->return_shouts();
				break;
			case 'submit':
				$this->submit_shout();
				break;
			case 'getMembers':
				$this->library->getMembersViewing();
				break;
			case 'mod':
				$this->moderator();
				break;
			case 'announce':
				$this->ajax_announce();
				break;
			# Added in RC1
			case 'prune':
				$this->ajax_prune();
				break;
			case 'ban':
				$this->ajax_ban();
				break;
			case 'ban24':
				$this->ajax_ban(24);
				break;
			case 'ban48':
				$this->ajax_ban(48);
				break;
			case 'unban':
				$this->ajax_unban();
				break;
			# End Added in RC1
			case 'prefs':
				$this->preferences();
				break;
			case 'archive':
				$this->ajax_archive();
				break;
			case 'smilies':
				$this->fetch_smilies();
				break;
			case 'delete':
				$this->delete_my_shout();
				break;
			default:
				$this->ajax_nothing();
				break;
		}
	}
	
	private function delete_my_shout()
	{
		/* Clean input */
		$id = intval( $this->request['shout'] );
		
		/* Make sure we got a shout */
		if ( !$id )
		{
			$this->returnJsonError( 'noshout' );
		}
		
		/* Pull the info for this shout */
		$shout = $this->DB->buildAndFetch( array( 'select' => '*', 'from' => 'shoutbox_shouts', 'where' => 's_id=' . $id ) );
		
		/* More error checking */
		if ( !$shout['s_id'] )
		{
			$this->returnJsonError( 'noshout' );
		}
		if ( $shout['s_mid'] != $this->memberData['member_id'] )
		{
			$this->returnJsonError( 'nopermission' );
		}
		if ( !$this->memberData['g_shoutbox_delete_own'] )
		{
			$this->returnJsonError( 'nopermission' );
		}
		
		/* Delete the shout */
		$this->DB->delete( 'shoutbox_shouts', 's_id=' . $id );
		
		/* Rebuild shouts cache */
		$this->library->recacheShouts( 'recount', false );
		
		/* Success! */
		$this->returnJsonArray( array( 'return' => 'ok', 'id' => $id ) );
	}
	
	private function fetch_smilies()
	{
		/* INIT */
		$emoArray = array();
		$start    = intval( $this->request['pg'] );
		
		/* Get all emoticons for this skin */
		if ( $this->memberData['g_shoutbox_use'] && $this->settings['shoutbox_allow_smilies'] )
		{
			foreach( $this->cache->getCache('emoticons') as $emo )
			{
				if ( $emo['emo_set'] == $this->registry->output->skin['set_emo_dir'] )
				{
					$emoArray[] = array( 'text' => addslashes( $emo['typed'] ), 'image' => $emo['image'] );
				}
			}
		}
		
		/* Elements for constructing pages */
		$total    = count( $emoArray );
		$emoArray = array_slice( $emoArray, $this->settings['shoutbox_emos_perpage']*$start, $this->settings['shoutbox_emos_perpage'] );
		$html     = count( $emoArray ) ? $this->registry->output->getTemplate('shoutbox_hooks')->hookGlobalShoutboxEmoticons( $emoArray, $total, $start-1, $start+1 ) : '';
		
		/* Return */
		$this->returnHtml( $html );
	}
	
	private function returnError( $string='', $check=true )
	{
		// We passed a string or a whole text?
		if ( $check && isset($this->lang->words[ 'error_'.$string ]) )
		{
			$string = $this->lang->words[ 'error_'.$string ];
		}
		
		$this->returnString( 'error-'.$string );
	}
	
	private function submit_shout()
	{
		if ( !$this->memberData['g_shoutbox_use'] )
		{
			$this->returnError('no_use_perm');
		}
		
		/**
		 * 1.1.2
		 * Our posts are restricted?
		 */
        if ( $this->memberData['restrict_post'] )
        {
        	$this->lang->loadLanguageFile( array('public_error'), 'core' );
        	
        	if ( $this->memberData['restrict_post'] == 1 )
        	{
        		$this->returnError( $this->lang->words['posting_restricted'], false );
        	}
        	
        	$post_arr = IPSMember::processBanEntry( $this->memberData['restrict_post'] );
        	
        	/* Restriction is already ended? */
        	if ( time() >= $post_arr['date_end'] )
        	{
        		IPSMember::save( $this->memberData['member_id'], array( 'core' => array( 'restrict_post' => 0 ) ) );
        	}
        	/* Still restricted, oh well... */
        	else
        	{
				$this->returnError( sprintf( $this->lang->words['posting_off_susp'], $this->registry->getClass( 'class_localization')->getDate( $post_arr['date_end'], 'LONG', 1 ) ), false );
        	}
        }
		
		// Check flooding
		if ( $this->settings['shoutbox_flood_limit'] > 0 && $this->memberData['g_shoutbox_bypass_flood'] != 1 )
		{
			// Load our latest shout from DB
			$shout = $this->DB->buildAndFetch( array( 'select'   => 's_date',
													  'from'     => 'shoutbox_shouts',
													  'where'    => 's_mid='.$this->memberData['member_id'],
													  'order'    => 's_date DESC',
													  'limit'    => array(0, 1)
											  )		 );
			
			if ( intval($shout['s_date']) > 0 )
			{
				$time_check = time() - intval($shout['s_date']);
	
				if ( $time_check < $this->settings['shoutbox_flood_limit'] )
				{
					$this->returnError( str_replace('{#EXTRA#}', ($this->settings['shoutbox_flood_limit']-$time_check), $this->lang->words['error_flooding']) );
				}
			}
		}
		
		/* Parse the shout!! */
		$shout = $_POST['shout'];
		
		if( $this->settings['shoutbox_stop_shouting'] )
		{
			if( function_exists('mb_convert_case') )
			{
				if( in_array( strtolower( $this->settings['gb_char_set'] ), array_map( 'strtolower', mb_list_encodings() ) ) )
				{
					$shout = mb_convert_case( $shout, MB_CASE_TITLE, $this->settings['gb_char_set'] );
				}
				else
				{
					$shout = ucwords( $shout );
				}
			}
			else
			{
				$shout = ucwords( $shout );
			}
		}
		
		/* If in the global shoutbox, we don't need to have the editor process this */
		if ( !$this->request['global'] )
		{
			$shout = $this->library->editor->process( $shout );
		}
		else
		{
			$shout = nl2br( IPSText::htmlspecialchars( $shout ) );
		}
		
		$shout = $this->library->parser->preDbParse( $shout );
		
		// Check for errors
		if ( $this->library->parser->error != "" )
		{
			$this->returnError( $this->library->parser->error );
		}
		
		/* Check for other errors :O */
		if ( strlen( trim( IPSText::removeControlCharacters( IPSText::br2nl( $shout ) ) ) ) < 1 )
		{
			$this->returnError('blank_shout', false);
		}
		
		if ( $this->library->shout_max_length && IPSText::mbstrlen( $shout ) > $this->library->shout_max_length )
		{
			$this->returnError('shout_too_big', false);
		}
		
		// Finally save shout
		$this->DB->force_data_type = array( 's_mid' => 'int', 's_message' => 'string' );
		
		$this->DB->insert( 'shoutbox_shouts',
						   array( 's_mid'     => $this->memberData['member_id'],
								  's_message' => $shout,
								  's_date'    => time(),
								  's_ip'      => $this->member->ip_address
								 )
						  );
		
		/**
		 * Update our session when submitting shouts!
		 * By default ajax module doesn't update session
		 */
		$this->member->updateMySession( array( 'current_appcomponent' => 'shoutbox', 'current_module' => 'ajax', 'current_section' => 'submit' ) );
		
		/* Recache & return */
		$this->library->recacheShouts('add');
		$this->library->return_shouts();
	}

	private function ajax_nothing()
	{
		$this->returnString("You shouldn't see this message at all!");
	}

	private function ajax_archive()
	{
		if ( !$this->memberData['g_shoutbox_view_archive'] )
		{
			$this->returnError('no_archive_perm', false);
		}

		if ( trim($this->request['action']) == 'filter')
		{
			$s = explode(',', $this->convertAndMakeSafe($this->request['start']));
			$e = explode(',', $this->convertAndMakeSafe($this->request['end']));
			$m = trim($this->convertAndMakeSafe($this->request['member']));
			
			if ( strpos($m, ",") !== false )
			{
				$m = explode(',', $m);
			}

			if ( is_array($m) )
			{
				$x = array();
				
				foreach ( $m as $n )
				{
					$n = trim($n);
					if ( $n == '' || strlen($n) < 3 )
					{
						continue;
					}

					$x[] = $n;
				}

				if ( !count($x) )
				{
					$this->returnError('member_names_too_short', false);
				}
			}
			elseif ( $m != '' && strlen($m) < 3 )
			{
				$this->returnError('member_name_too_short', false);
			}
			
			$y = array( 'month'    => $s[0],
						'day'      => $s[1],
						'year'     => $s[2],
						'hour'     => $s[3],
						'minute'   => $s[4],
						'meridiem' => $s[5]
						);

			$z = array( 'month'    => $e[0],
						'day'      => $e[1],
						'year'     => $e[2],
						'hour'     => $e[3],
						'minute'   => $e[4],
						'meridiem' => $e[5]
						);
			
			if ( $this->settings['shoutbox_archive_clock'] == 'ampm' )
			{
				if ( $y['meridiem'] == 'am' )
				{
					if ( $y['hour'] == 12 )
					{
						$y['hour'] = 0;
					}
				}
				elseif ( $y['meridiem'] == 'pm' )
				{
					if ( $y['hour'] >= 1 && $y['hour'] <= 11 )
					{
						$y['hour'] += 12;
					}
				}

				if ( $z['meridiem'] == 'am' )
				{
					if ( $z['hour'] == 12 )
					{
						$z['hour'] = 0;
					}
				}
				elseif ( $z['meridiem'] == 'pm' )
				{
					if ( $z['hour'] >= 1 && $z['hour'] <= 11 )
					{
						$z['hour'] += 12;
					}
				}
			}

			$a = gmmktime( $y['hour'], $y['minute'], 0, $y['month'], $y['day'], $y['year'] ) - $this->lang->getTimeOffset();
			$b = gmmktime( $z['hour'], $z['minute'], 0, $z['month'], $z['day'], $z['year'] ) - $this->lang->getTimeOffset();
			
			/** Get shouts from DB **/
			$this->DB->build( array( 'select'   => 's.*',
									 'from'     => array('shoutbox_shouts' => 's'),
									 'where'    => 's.s_date >= '.$a.' AND s.s_date <= '.$b,
									 'order'    => 's.s_date ASC',
									 'add_join' => array( 0 => array( 'select' => 'm.*',
									 								  'from'   => array('members' => 'm'),
																	   'where'  => 'm.member_id=s.s_mid',
																	   'type'   => 'left' ) )
							 )		);
			$this->DB->execute();
			
			$s = array();
			
			while ( $r = $this->DB->fetch() )
			{
				$s[ $r['s_id'] ] = $r;
			}
			
			$n = array();
			foreach ( $s as $k => $v )
			{
				$v['_date'] = $v['s_date'];
				
				if ( $v['_date'] >= $a && $v['_date'] <= $b )
				{
					if ( $m != '' && !preg_match("#".$m."#is", $v['members_display_name']) )
					{
						continue;
					}

					$n[ $k ] = $v;
				}
			}

			if ( count($n) < 1 )
			{
				$rh = $this->registry->output->getTemplate('shoutbox')->archive_message($this->lang->words['filter_no_results']);
				$rh .= $this->registry->output->getTemplate('shoutbox')->archive_ajax( array( 'pages'   => 0, 'curpage' => 0 ) );
				
				$this->returnHtml( $rh );
			}

			$pg = (intval($this->request['page']) > 1) ? intval($this->request['page']) : 1;
			$st = 0;
			$cn = '';
			$pp = (intval($this->settings['shoutbox_archive_perpage']) >= 0) ? intval($this->settings['shoutbox_archive_perpage']) : 10;
			$pn = 0;
			$nm = 0;
			$np = false;
			$dt = array();
			$op = '';
			$st = intval(($pg-1)*$pp);
			$tn = 0;
			$tp = ($pp > 0) ? ceil(count($n)/$pp) : 1;

			if ( $pg > $tp )
			{
				$pg = 1;
			}
			
			/* Let's load ignored users for parse_shout later */
			$this->library->_getIgnoredUsers();
			
			foreach ( $n as $k => $v )
			{
				$tn++;
				
				if ( $tn <= $st )
				{
					continue;
				}

				$cn = ($cn == 'row2' || $cn == '') ? 'row1' : 'row2';

				$v['class']      = $cn;
				$v['_date_full'] = 1;
				$v['_archive']   = 'archive-';

				$op .= $this->library->parse_shout($v);
				if ($pp > 0 && $tn >= ($st+$pp))
				{
					break;
				}
			}

			$d = array( 'pages'   => $tp, 'curpage' => $pg );

			$op .= $this->registry->output->getTemplate('shoutbox')->archive_ajax($d);
			$this->returnHtml( $op );
		}
		else
		{
			/* Load oldest shout from DB */
			$s = $this->DB->buildAndFetch( array( 'select' => '*',
												  'from'   => 'shoutbox_shouts',
												  'order'  => 's_date ASC',
												  'limit'  => array(0, 1)
										  )		 );
			
			$c = explode( ",", $this->lang->getTime( time(), '%Y,%m,%d,%H,%M,%p' ) );
			$o = explode( ",", $this->lang->getTime( $s['s_date'], '%Y,%m,%d,%H,%M,%p' ) );
			
			$c = array( 'year'     => $c[0],
						'month'    => $c[1],
						'day'      => $c[2],
						'hour'     => $c[3],
						'minute'   => $c[4],
						'meridiem' => strtolower($c[5])
						);

			$o = array( 'year'     => $o[0],
						'month'    => $o[1],
						'day'      => $o[2],
						'hour'     => $o[3],
						'minute'   => $o[4],
						'meridiem' => strtolower($o[5])
						);

			$d = array( 'height'      => 275,
						's_months'    => $this->_arrayToOptions('months', $o['month']),
						's_days'      => $this->_arrayToOptions('days', $o['day']),
						's_years'     => $this->_arrayToOptions($this->_getYears($o['year']), $o['year']),
						's_hours'     => $this->_arrayToOptions('hours', $o['hour']),
						's_minutes'   => $this->_arrayToOptions('minutes', $o['minute']),
						's_meridiems' => $this->_arrayToOptions('meridiems', $o['meridiem']),
						
						'e_months'    => $this->_arrayToOptions('months', $c['month']),
						'e_days'      => $this->_arrayToOptions('days', $c['day']),
						'e_years'     => $this->_arrayToOptions($this->_getYears($o['year']), $c['year']),
						'e_hours'     => $this->_arrayToOptions('hours', $c['hour']),
						'e_minutes'   => $this->_arrayToOptions('minutes', $c['minute']),
						'e_meridiems' => $this->_arrayToOptions('meridiems', $c['meridiem']),
						'oldestShout' => $this->lang->getTime( $s['s_date'], '%B %d, %Y %H:%M:%S' )
						);

			$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->archive($d) );
		}
	}

	private function preferences()
	{
		if ( !$this->memberData['member_id'] || !$this->memberData['g_shoutbox_use'] )
		{
			$this->returnError('prefs_login', false);
		}

		switch ( trim($this->request['action']) )
		{
			case 'load':
				$this->preferencesLoadPopup();
				break;
			case 'save':
				$this->preferencesSave();
				break;
			case 'restore':
				$this->preferencesRestore();
				break;
			case 'appHeight':
				$this->preferencesShoutsAppHeight();
				break;
			case 'globalHeight':
				$this->preferencesShoutsGlobalHeight();
				break;
			default:
				$this->ajax_nothing();
				break;
		}
	}

	private function preferencesLoadPopup( $return=false )
	{
		$p = array( 'gsb_y' => ($this->library->prefs['global_display'] == 1) ? 'checked ': '',
					'gsb_n' => ($this->library->prefs['global_display'] == 1) ? '' : 'checked ',
					'ets_y' => ($this->library->prefs['enter_key_shout'] == 1) ? 'checked ' : '',
					'ets_n' => ($this->library->prefs['enter_key_shout'] == 1) ? '' : 'checked ',
					'eqc_y' => ($this->library->prefs['enable_quick_commands'] == 1) ? 'checked ' : '',
					'eqc_n' => ($this->library->prefs['enable_quick_commands'] == 1) ? '' : 'checked ',
					'drb_y' => ($this->library->prefs['display_refresh_button'] == 1) ? 'checked ' : '',
					'drb_n' => ($this->library->prefs['display_refresh_button'] == 1) ? '' : 'checked ',
					'snd_y' => ($this->library->prefs['disable_sound'] == 1) ? 'checked ' : '',
					'snd_n' => ($this->library->prefs['disable_sound'] == 1) ? '' : 'checked ',
					'sdo'   => $this->library->prefs['shoutbox_display_order'],
					);
		
		# Now letz check if the global shoutbox hook is disabled
		$p['global_shoutbox'] = false;
		
		if( is_array($this->caches['hooks']['templateHooks']) && count($this->caches['hooks']['templateHooks']) )
		{
			foreach( $this->caches['hooks']['templateHooks'] as $hdata )
			{
				foreach( $hdata as $_hid => $_hdata )
				{
					if ( $_hdata['className'] == 'shoutboxGlobalShoutbox' )
					{
						$p['global_shoutbox'] = true;
						break;
					}
				}
			}
		}
		
		$data = $this->registry->output->getTemplate('shoutbox')->popupMyPrefs( $p );

		if ( $return )
		{
			return $data;
		}

		$this->returnHtml( $data );
	}

	private function preferencesSave()
	{
		$this->library->prefs['global_display']         = intval($this->request['prefs_gsb']);
		$this->library->prefs['enter_key_shout']        = intval($this->request['prefs_ets']);
		$this->library->prefs['enable_quick_commands']  = intval($this->request['prefs_eqc']);
		$this->library->prefs['display_refresh_button'] = intval($this->request['prefs_drb']);
		$this->library->prefs['shoutbox_display_order'] = $this->request['prefs_sdo'];
		$this->library->prefs['disable_sound']			= $this->request['prefs_snd'];

		$this->library->_save_shoutbox_prefs();

		$data = array( 'prefs' => $this->library->prefs_js,
					   'msg'   => $this->lang->words['prefs_saved']
					  );

		$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->popupMyPrefsUpdate( $data ) );
	}
	
	private function preferencesRestore()
	{
		$this->library->prefs = array( 'shoutbox_height'        => $this->settings['shoutbox_default_height'],
									   'shoutbox_gheight'       => $this->settings['shoutbox_default_gheight'],
									   'global_display'         => 1,
									   'enter_key_shout'        => 1,
									   'enable_quick_commands'  => 1,
									   'display_refresh_button' => 1,
									   'disable_sound'			=> 0,
									   'shoutbox_display_order' => 0
									  );

		$this->library->_save_shoutbox_prefs( true );
		
		$data = array( 'prefs' => $this->library->prefs_js,
					   'msg'   => $this->lang->words['prefs_restored']
					  );
		
		$return  = $this->preferencesLoadPopup(true);
		$return .= $this->registry->output->getTemplate('shoutbox')->popupMyPrefsUpdate( $data );

		$this->returnHtml( $return );
	}

	private function preferencesShoutsAppHeight()
	{
		$this->library->prefs['shoutbox_height'] = ( intval($this->request['height']) < 100 ) ? 100 : intval($this->request['height']);

		$this->library->_save_shoutbox_prefs();

		$this->returnString('OK');
	}

	private function preferencesShoutsGlobalHeight()
	{
		$this->library->prefs['shoutbox_gheight'] = ( intval($this->request['height']) < 100 ) ? 100 : intval($this->request['height']);

		$this->library->_save_shoutbox_prefs();

		$this->returnString('OK');
	}
	
	private function ajax_announce()
	{
		# This member have ACP access?
		if ( !$this->memberData['g_access_cp'] )
		{
			$this->returnError('no_acp_access', false);
		}
		
		// Update announcement in DB
		IPSLib::updateSettings( array( 'shoutbox_announcement' => str_replace( "&#39;", "'", IPSText::stripslashes($_POST['announce']) ) ) );
		
		/* Override default settings */
		$this->library->parser->parse_smilies = 1;
		$this->library->parser->parse_html    = 1;
		$this->library->parser->parse_bbcode  = 1;
		
		/* Clean $_POST data */
		$_POST['announce'] = $_POST['announce'];
		$_POST['announce'] = IPSText::parseCleanValue( $_POST['announce'] );
		
		/* Parse for display */
		$announce = $this->library->editor->process( $_POST['announce'] );
		$announce = $this->library->parser->preDbParse( $announce );
		$announce = $this->library->parser->preDisplayParse( $announce );
		
		/* Got errors? */
		if ( $this->library->parser->error != "" )
		{
			$this->returnString( $this->library->parser->error );
		}
		
		/* Reset to our default values */
		$this->library->parser->parse_html    = 0;
		$this->library->parser->parse_smilies = $this->settings['shoutbox_allow_smilies'];
		$this->library->parser->parse_bbcode  = $this->settings['shoutbox_allow_bbcode'];

		// Return the formatted announce
		$this->returnHtml( $announce );
	}
	
	# Added in RC1
	private function ajax_prune()
	{
		# This member have ACP access?
		if ( !$this->memberData['g_access_cp'] )
		{
			$this->returnError('no_acp_access', false);
		}
		
		# We have a valid number?
		if ( !is_numeric($this->request['days']) )
		{
			$this->returnError('prune_invalid_number', false);
		}
		
		# Finally prune old shouts
		$days = intval($this->request['days']);
		
		if ( $days > 0 )
		{
			$old = time()-($days*24*60*60);
			
			$this->DB->delete( "shoutbox_shouts", "s_date < ".$old );
			
			$deleted = $this->DB->getAffectedRows();
			
			$action = sprintf( $this->lang->words['pruned_all_shouts'], $deleted, $days );
		}
		else
		{
			$this->DB->delete( "shoutbox_shouts" );
			
			$action = $this->lang->words['prune_executed_all'];
		}
		
		// Rebuild shouts cache
		$this->library->recacheShouts( 'recount', false );

		// Save log & Return succesfull message
		$this->library->saveAdminLog( 'emptyshouts', $action );
		
		
		if ( $days > 0 )
		{
			$this->returnString( sprintf( $this->lang->words['prune_executed_older_days'], $deleted ) );
		}
		else
		{
			$this->returnString( $action );
		}
	}
	
	# Added in RC1
	private function ajax_ban($time = 0)
	{
		# This member can ban?
		if ( !$this->library->mod_perms['m_ban_members'] )
		{
			$this->returnError('mod_no_perm', false);
		}
		
		# We have a valid name?
		$name = strtolower( $this->convertAndMakeSafe( $this->request['name'], 0 ) );
    	$name = str_replace("&#43;", "+", $name );
    	$name = trim( $name );
    	
		if ( strlen( $name ) < 3 )
		{
			$this->returnError('mod_invalid_name');
		}
		
    	$member = $this->library->_get_member( $name );
		
		# Check if we can ban this member!
		if ( !$member['member_id'] )
		{
			$this->returnError('mod_member_not_exist');
		}
		
		if ( $member['member_id'] == $this->memberData['member_id'] )
		{
			$this->returnError('mod_no_ban_self');
		}
		
		/* ACP access? */
		if ( $this->caches['group_cache'][ $member['member_group_id'] ]['g_access_cp'] == 1 )
		{
			$this->returnError('mod_no_ban_admins');
		}
		
		# Is this member already banned?
		if ( $member['_cache']['shoutbox_banned'] )
		{
			$this->returnError('mod_member_already_banned');
		}
		
		# Still here? ban that member so! :O
		IPSMember::packMemberCache( $member['member_id'], array( 'shoutbox_banned' => 1, 'shoutbox_banned_howlong' => $bannedTime, 'shoutbox_banned_time' => time() ), $member['_cache'] );
		
		// Save log & Return succesfull message
		$this->library->saveAdminLog( 'doban', sprintf( $this->lang->words['sblog_banned'], $member['members_display_name'], $member['member_id'] ) );
		
		$this->returnString( $this->lang->words['mod_member_banned'] );
	}
	
	# Added in RC1
	private function ajax_unban()
	{
		# This member can unban?
		$this->library->checkModeratorPerm('unban_members');
		
		# We have a valid name?
		$name = strtolower( $this->convertAndMakeSafe( $this->request['name'], 0 ) );
    	$name = str_replace("&#43;", "+", $name );
    	$name = trim( $name );
    	
		if ( strlen( $name ) < 3 )
		{
			$this->returnError('mod_invalid_name');
		}
		
		$member = $this->library->_get_member( $name );
		
		# Check if we can ban this member!
		if ( !$member['member_id'] )
		{
			$this->returnError('mod_member_not_exist');
		}
		
		/* Removing the ban from yourself?!? */
		if ( $member['member_id'] == $this->memberData['member_id'] )
		{
			$this->returnError('mod_no_unban_self');
		}
		
		# Is this member already banned?
		if ( !$member['_cache']['shoutbox_banned'] )
		{
			$this->returnError('mod_member_already_unbanned');
		}
		
		# Still here? unban that member so! :O
		IPSMember::packMemberCache( $member['member_id'], array( 'shoutbox_banned' => 0, 'shoutbox_banned_howlong' => 0, 'shoutbox_banned_time' => 0 ), $member['_cache'] );
		
		// Save log & Return succesfull message
		$this->library->saveAdminLog( 'dounban', sprintf( $this->lang->words['sblog_unbanned'], $member['members_display_name'], $member['member_id'] ) );
		
		$this->returnString( $this->lang->words['mod_member_unbanned'] );
	}

	private function moderator()
	{
		// Check permissions :O
		if ( !$this->library->moderator && !$this->memberData['g_shoutbox_edit'] )
		{
			$this->returnError('mod_no_perms', false);
		}

		switch ( trim($this->request['action']) )
		{
			case 'loadShout':
				$this->moderatorLoadShout();
				break;
			case 'loadMember':
				$this->moderatorLoadMember();
				break;
			case 'loadQuickCmd':
				$this->moderatorLoadQuickCommand();
				break;
			case 'loadCommand':
				$this->moderatorLoadCommand(false);
				break;
			case 'performCommand':
				$this->moderatorLoadCommand(true);
				break;
			default:
				$this->ajax_nothing();
				break;
		}
	}

	private function moderatorLoadShout()
	{
		/* Init some vars */
		$shout    = $this->library->getShout( $this->request['id'] );
		$ourShout = false;
		
		if ( !$shout['s_id'] )
		{
			$this->returnError('mod_shout_not_exist');
		}

		/* This is out shout? :O */
		if ( $shout['member_id'] == $this->memberData['member_id'] )
		{
			$ourShout = 1;
		}

		/* Sort out formatting/links */
		if ( $this->settings['shoutbox_format_names'] )
		{
			$shout['members_display_name'] = IPSMember::makeNameFormatted( $shout['members_display_name'], $shout['member_group_id'] );
		}
		$shout['shouted_by'] = IPSMember::makeProfileLink( $shout['members_display_name'], $shout['member_id'], $shout['members_seo_name'] );
		$shout['_is_a_mod']  = $this->library->checkIsModerator( $shout['member_id'], $shout['member_group_id'] );
		
		/* Update permissions if we can edit shouts :O */
		if ( $ourShout && $this->memberData['g_shoutbox_edit'] )
		{
			$this->library->mod_perms['m_edit_shouts'] = 1;
		}
		
		$shout['s_message'] = $this->library->parser->preDisplayParse( $shout['s_message'] );
		
		$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->popupModerator( $shout, $this->library->mod_perms, 'shout' , $ourShout ) );
	}

	private function moderatorLoadMember()
	{
		$mid = intval($this->request['mid']);
		if ( !$mid )
		{
			$this->returnError('mod_invalid_mid');
		}

		$m = $this->library->_get_member( $mid );
		if ( !$this->DB->getTotalRows() )
		{
			$this->returnError('mod_member_not_exist');
		}
		
		/* Sort out member name */
		$m['_members_display_name'] = IPSText::truncate( $m['members_display_name'], 30 );
		$m['_members_display_name'] = $this->settings['shoutbox_format_names'] ? IPSMember::makeNameFormatted( $m['_members_display_name'], $m['member_group_id'] ) : $m['_members_display_name'];
		
		$m['_is_a_mod'] = $this->library->checkIsModerator( $mid, $m['member_group_id'] );

		$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->popupModerator($m, $this->library->mod_perms, 'member') );
	}

	private function moderatorLoadQuickCommand()
	{
		$mt  = trim($this->request['modtype']);
		$in  = '';

		if ( $mt == 'shout' )
		{
			$in = intval($this->request['shout']);
			if ( !$in )
			{
				$this->returnError('mod_invalid_id');
			}

			$this->request['id'] = $in;
			$this->moderatorLoadShout();
		}
		elseif ( $mt == 'member' )
		{
			$in = $this->convertAndMakeSafe($this->request['member']);
			if ( is_numeric($in) )
			{
				if ( !$in )
				{
					$this->returnError('mod_invalid_mid');
				}

				$this->request['mid'] = $in;
				$this->moderatorLoadMember();
			}
			elseif ( is_string($in) )
			{
				if ( strlen(trim($in)) < 3 )
				{
					$this->returnError('mod_invalid_name');
				}

				$m = $this->library->_get_member( $in );
				if ( !$m || !is_array($m) || !$m['member_id'] )
				{
					$this->returnError('mod_member_not_exist');
				}

				$this->request['mid'] = $m['member_id'];
				$this->moderatorLoadMember();
			}
		}

		$this->returnError('unknown');
	}

	private function moderatorLoadCommand( $doCommand=false )
	{
		switch ( trim($this->request['command']) )
		{
			case 'edit':
				$this->library->checkModeratorPerm('edit_shouts');
				$this->moderatorEditShout( $doCommand );
				break;
			case 'delete':
				$this->moderatorDeleteShout( $doCommand );
				break;
			case 'deleteAll':
				$this->moderatorDeleteMembersShouts( $doCommand );
				break;
			case 'ban':
				$this->library->checkModeratorPerm('ban_members');
				$this->moderatorBanMember( $doCommand );
				break;
			case 'ban24':
				$this->library->checkModeratorPerm('ban_members');
				$this->moderatorBanMember( $doCommand, 24 );
				break;
			case 'ban48':
				$this->library->checkModeratorPerm('ban_members');
				$this->moderatorBanMember( $doCommand, 48 );
				break;
			case 'unban':
				$this->library->checkModeratorPerm('unban_members');
				$this->moderatorUnbanMember( $doCommand );
				break;
			case 'removeMod':
				$this->library->checkModeratorPerm('remove_mods');
				$this->moderatorRemoveModerator( $doCommand );
				break;
			case 'editHistory':
				$this->library->checkModeratorPerm('edit_shouts');
				$this->moderatorEditHistoryShout();
				break;
			default:
				$this->ajax_nothing();
				break;
		}
	}

	private function moderatorEditShout( $doAction=false )
	{
		/* Init some vars */
		$shout    = $this->library->getShout( $this->request['id'] );
		$ourShout = false;
		
		/* Search for errors */
		if ( !$shout['s_id'] )
		{
			$this->returnError('mod_shout_not_exist');
		}

		if ( !$this->library->moderator && $this->memberData['member_id'] != $shout['member_id'] )
		{
			$this->returnError('mod_shout_only_own');
		}

		if ( $doAction )
		{
			// Setup update array
			$shout['s_edit_history'][] = array( 'mid'  => $this->memberData['member_id'],
												'date' => time(),
												'ip'   => $this->member->ip_address
												);
			$update = array( 's_edit_history' => $this->library->packEditHistory( $shout['s_edit_history'] ) );
			
			/* Parse the shout!! */
			$newShout = $_POST['shout'];
			
			if( $this->settings['shoutbox_stop_shouting'] )
			{
				if( function_exists('mb_convert_case') )
				{
					if( in_array( strtolower( $this->settings['gb_char_set'] ), array_map( 'strtolower', mb_list_encodings() ) ) )
					{
						$newShout = mb_convert_case( $newShout, MB_CASE_LOWER, $this->settings['gb_char_set'] );
					}
					else
					{
						$newShout = strtolower( $newShout );
					}
				}
				else
				{
					$newShout = strtolower( $newShout );
				}
			}
			
			$newShout = $this->library->editor->process( $newShout );
			$newShout = $this->library->parser->preDbParse( $newShout );
			
			// Check for errors
			if ( $this->library->parser->error != "" )
			{
				$this->returnError( $this->library->parser->error );
			}
			
			/* Check for other errors :O */
			if ( strlen( trim( IPSText::removeControlCharacters( IPSText::br2nl( $newShout ) ) ) ) < 1 )
			{
				$this->returnError('blank_shout', false);
			}
			
			if ( $this->library->shout_max_length && IPSText::mbstrlen( $newShout ) > $this->library->shout_max_length )
			{
				$this->returnError('shout_too_big', false);
			}
			
			$update['s_message'] = $newShout;
			
			// Save changes in DB
			$this->DB->force_data_type = array( 's_message' => 'string' );
			$this->DB->update( 'shoutbox_shouts', $update, 's_id='.$shout['s_id'] );
			$this->library->recacheShouts();
			
			// Beta 3
			// Return the edited shout
			// instead of the message
			$shout['s_message'] = $this->library->parser->preDisplayParse( $update['s_message'] );
			$shout['ajax_return'] = true;
			
			/* Let's load ignored users for parse_shout */
			$this->library->_getIgnoredUsers();
			
			$this->returnHtml( $this->library->parse_shout($shout) );
		}
		else
		{
			$this->library->parser->parsing_mgroup        = $shout['member_group_id'];
			$this->library->parser->parsing_mgroup_others = $shout['mgroup_others'];
			
			$shout['s_message'] = $this->library->parser->preEditParse( $shout['s_message'] );
			
			$e['smilies'] = $this->library->editor->fetchEmoticons();
			$e['text']    = $shout['s_message'];
			
			$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->mod_opts_content( 'edit', $e ) );
		}
	}
	
	private function moderatorDeleteShout( $doAction=false )
	{
		/* Init some vars */
		$shout = $this->library->getShout( $this->request['id'] );
		
		if ( !$shout['s_id'] )
		{
			$this->returnError('mod_shout_not_exist');
		}
		
		// Check for permission
		if ( ($s['member_id'] == $this->memberData['member_id'] && !$this->library->checkModeratorPerm('delete_shouts', true)) || ($s['member_id'] != $this->memberData['member_id'] && !$this->library->checkModeratorPerm('delete_shouts_user', true)) )
		{
            $this->returnError('mod_no_perm', false);
		}

		if ( $doAction )
		{
			$this->DB->delete( 'shoutbox_shouts', 's_id='.$shout['s_id'] );
			$this->library->recacheShouts('remove');
			
			$this->returnString('OK');
		}
		else
		{
			$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->mod_opts_content('delete') );
		}
	}

	private function moderatorDeleteMembersShouts( $doAction=false )
	{
		/* Let's do some checks */
		if ( trim($this->request['modtype']) == 'member' )
		{
			$data = $this->library->_get_member( $this->request['id'] );
			
			if ( !$data['member_id'] )
			{
				$this->returnError('mod_member_not_exist');
			}
		}
		else
		{
			$data = $this->library->getShout( $this->request['id'] );
			
			if ( !$data['member_id'] )
			{
				$this->returnError('mod_shout_not_exist');
			}
		}
		
		// Check for permission
		if ( ($data['member_id'] == $this->memberData['member_id'] && !$this->library->checkModeratorPerm('delete_shouts', true)) || ($data['member_id'] != $this->memberData['member_id'] && !$this->library->checkModeratorPerm('delete_shouts_user', true)) )
		{
            $this->returnError('mod_no_perm', false);
		}
		
		if ( $doAction )
		{
			// Get ids for our "hide routine"
			$found = array();
			
			$this->DB->build( array( 'select' => 's_id',
									 'from'   => 'shoutbox_shouts',
									 'where'  => 's_mid='.$data['member_id']
							 )		);
			$this->DB->execute();
			
			while ( $rows = $this->DB->fetch() )
			{
				$found[] = $rows['s_id'];
			}
			
			$ids = count($found) ? implode(",", $found) : "";
			
			// DRR (Delete-Recount-Return) yup!
			$this->DB->delete( 'shoutbox_shouts', 's_mid='.$data['member_id'] );
			
			$this->library->recacheShouts('recount');
			$this->returnString( $ids );
		}
		else
		{
			$found = $this->DB->buildAndFetch( array( 'select' => 'count(s_id) as total',
 													  'from'   => 'shoutbox_shouts',
													  'where'  => 's_mid='.$data['member_id']
											  )		 );
			
			$e = array( 'total' => $this->lang->formatNumber( $found['total'] ) );
			
			$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->mod_opts_content('delete-all', $e) );
		}
	}

	private function moderatorBanMember( $doAction=false, $bannedTime = 0 )
	{
		$id = intval($this->request['id']);
		$mt = trim($this->request['modtype']);

		if ( !$id )
		{
			if ( $mt == 'shout' )
			{
				$this->returnError('mod_invalid_id');
			}
			elseif ( $mt == 'member' )
			{
				$this->returnError('mod_invalid_mid');
			}
		}

		if ( $mt == 'shout' )
		{
			$s = $this->library->getShout( $id );

			$id = $s['s_mid'];
		}
		elseif ( $mt == 'member' )
		{
			$s = $this->library->_get_member( $id );

			$id = $s['member_id'];
		}

		if ( !$this->DB->getTotalRows() )
		{
			if ($mt == 'shout')
			{
				$this->returnError('mod_shout_not_exist');
			}
			elseif ($mt == 'member')
			{
				$this->returnError('mod_member_not_exist');
			}
		}

		/* Baninng ourself? */
		if ( $id == $this->memberData['member_id'] )
		{
			$this->returnError('mod_no_ban_self');
		}
		
		/* ACP access? */
		if ( $this->caches['group_cache'][ $member['member_group_id'] ]['g_access_cp'] == 1 )
		{
			$this->returnError('mod_no_ban_admins');
		}
		
		if ( $s['_cache']['shoutbox_banned'] )
		{
			$this->returnError('mod_member_already_banned');
		}

		if ( $doAction )
		{
			/* Ban the member */
			IPSMember::packMemberCache( $id, array( 'shoutbox_banned' => 1, 'shoutbox_banned_howlong' => $bannedTime, 'shoutbox_banned_time' => time() ), $s['_cache'] );
			
			// Save log & Return succesfull message
			$this->library->saveAdminLog( 'doban', sprintf( $this->lang->words['sblog_banned'], $s['members_display_name'], $id ) );

			$this->returnString( $this->lang->words['mod_member_banned'] );
		}
		else
		{
			$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->mod_opts_content('ban') );
		}
	}

	private function moderatorUnbanMember( $p=false )
	{
		$id = intval($this->request['id']);
		$mt = trim($this->request['modtype']);

		if ( !$id )
		{
			if ( $mt == 'shout' )
			{
				$this->returnError('mod_invalid_id');
			}
			elseif ( $mt == 'member' )
			{
				$this->returnError('mod_invalid_mid');
			}
		}

		if ($mt == 'shout')
		{
			$s = $this->library->getShout( $id );

			$id = $s['s_mid'];
		}
		elseif ($mt == 'member')
		{
			$s = $this->library->_get_member( $id );

			$id = $s['member_id'];
		}

		if ( !$this->DB->getTotalRows() )
		{
			if ($mt == 'shout')
			{
				$this->returnError('mod_shout_not_exist');
			}
			elseif ($mt == 'member')
			{
				$this->returnError('mod_member_not_exist');
			}
		}

		/* Removing the ban from yourself?!? */
		if ( $id == $this->memberData['member_id'] )
		{
			$this->returnError('mod_no_unban_self');
		}

		if ( !$s['_cache']['shoutbox_banned'] )
		{
			$this->returnError('mod_member_already_unbanned');
		}

		if ( $p )
		{
			/* Remove the ban from the member */
			unset($s['_cache']['shoutbox_banned']);
			unset($s['_cache']['shoutbox_banned_howlong']);
			unset($s['_cache']['shoutbox_banned_time']);
			
			$this->DB->update( 'members', array( 'members_cache' => serialize($s['_cache']) ), 'member_id='.$id );
			// Save log & Return succesfull message
			$this->library->saveAdminLog( 'dounban', sprintf( $this->lang->words['sblog_unbanned'], $s['members_display_name'], $id ) );
			
			$this->returnString( $this->lang->words['mod_member_unbanned'] );
		}
		else
		{
			$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->mod_opts_content('unban') );
		}
	}

	private function moderatorRemoveModerator( $doAction=false )
	{
		$id = intval($this->request['id']);
		$mt = trim($this->request['modtype']);

		if ( !$id )
		{
			if ($mt == 'shout')
			{
				$this->returnError('mod_invalid_id');
			}
			elseif ($mt == 'member')
			{
				$this->returnError('mod_invalid_mid');
			}
		}

		if ($mt == 'shout')
		{
			$s = $this->library->getShout( $id );
		}
		elseif ($mt == 'member')
		{
			$s = $this->library->_get_member( $id );
		}

		if ( !$this->DB->getTotalRows() )
		{
			if ($mt == 'shout')
			{
				$this->returnError('mod_shout_not_exist');
			}
			elseif ($mt == 'member')
			{
				$this->returnError('mod_member_not_exist');
			}
		}

		if ( !$this->library->checkIsModerator( $s['member_id'] ) && $this->library->checkIsModerator( 0, $s['member_group_id'] ) )
		{
			$this->returnError('mod_group_no_remove');
		}
		elseif ( !$this->library->checkIsModerator( $s['member_id'] ) )
		{
			$this->returnError('mod_member_not_mod');
		}

		if ( $doAction )
		{
			$this->DB->delete( 'shoutbox_mods', "m_type='member' AND m_mg_id=".$s['member_id'] );

			# Rebuild mods cache
			$this->library->recacheModerators();

			$this->returnString( $this->lang->words['mod_member_unmodded'] );
		}
		else
		{
			$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->mod_opts_content('delmod') );
		}
	}

	private function moderatorEditHistoryShout()
	{
		/* Init vars */
		$rows = array();
		$mt   = trim($this->request['modtype']);

		$s = $this->library->getShout( intval($this->request['id']) );
		if ( !$this->DB->getTotalRows() )
		{
			$this->returnError('mod_shout_not_exist');
		}

		$this->_sort     = 'asc';
		$this->_sort_key = 'date';
		uasort($s['s_edit_history'], array($this, '_sort_array'));

		$ids = array();
		foreach ( $s['s_edit_history'] as $r )
		{
			if ( is_array($ids) && !in_array($r['mid'], $ids) )
			{
				$ids[] = $r['mid'];
			}
		}

		$cn = 'row1';
		$md = $this->library->_get_members( $ids );

		foreach ( $s['s_edit_history'] as $edit )
		{
			/* Sort member */
			if ( isset($md[ $edit['mid'] ]) && is_array($md[ $edit['mid'] ]) )
			{
				$edit = array_merge($edit, $md[ $edit['mid'] ] );
			}
			
			$edit['class'] = $cn = ($cn == 'row1') ? 'row2' : 'row1';
			
			/* Sort formatting */
			if ( $this->settings['shoutbox_format_names'] )
			{
				$edit['members_display_name'] = IPSMember::makeNameFormatted( $edit['members_display_name'], $edit['member_group_id'] );
			}
			
			$edit['_name'] = IPSMember::makeProfileLink( $edit['members_display_name'], $edit['member_id'], $edit['members_seo_name'] );
			$edit['_date'] = $this->lang->getDate( $edit['date'], 'LONG' );
			
			// Add edit
			$rows[] = $edit;
		}
		
		$this->returnHtml( $this->registry->output->getTemplate('shoutbox')->mod_opts_content( 'editHistory', $rows ) );
	}

	public function _arrayToOptions( $get, $selected=null )
	{
		/* Init vars */
		$data = array();
		$html = "";
		
		/* We have a string? */
		if ( is_array($get) && count($get) )
		{
			$data = $get;
		}
		elseif ( is_string($get) )
		{
			switch ($get)
			{
				case 'months':
					$data = $this->_getMonths();
					break;
				case 'days':
					$data = $this->_getDays();
					break;
				case 'hours':
					$data = $this->_getHours();
					break;
				case 'minutes':
					$data = $this->_getMinutes();
					break;
				case 'meridiems':
					$data = $this->_getMeridiems();
					break;
			}
		}
		
		/* Loop data! =D */
		if ( is_array($data) && count($data) )
		{
			foreach ( $data as $k => $v )
			{
				$s = "";
				if ( $k == $selected )
				{
					$s = " selected";
				}

				$html .= "<option value='{$k}'{$s}>{$v}</option>";
			}
		}

		return $html;
	}

	public function _getMonths()
	{
		$a = array();

		for ( $i=1; $i<=12; $i++ )
		{
			$a[ $i ] = $this->lang->words['M_'.$i];
		}

		return $a;
	}

	public function _getDays()
	{
		$a = array();

		for ( $i=1; $i<=31; $i++ )
		{
			$a[ $i ] = $i;
		}

		return $a;
	}

	public function _getYears( $year=null )
	{
		/* Init some vars */
		$return  = array();
		$current = $this->lang->getTime( time(), '%Y' );
		
		if ( $year == '' || $year == null )
		{
			$year = $current;
		}
		
		for ( $i=$year; $i<=$current; $i++ )
		{
			$return[ $i ] = $i;
		}
		
		return $return;
	}

	public function _getHours()
	{
		$a = array();
		$max = $this->settings['shoutbox_archive_clock'] == 'ampm' ? 12 : 24;

		for ( $i=1; $i<=$max; $i++ )
		{
			$a[ $i ] = $i;
		}

		return $a;
	}

	public function _getMinutes()
	{
		$a = array();

		for ( $i=0; $i<=59; $i++ )
		{
			if ( $i <= 9 )
			{
				$i = '0'.$i;
			}

			$a[ $i ] = $i;
		}

		return $a;
	}

	public function _getMeridiems()
	{
		return array( 'am' => $this->lang->words['am'], 'pm' => $this->lang->words['pm'] );
	}

	public function _sort_array( $a, $b )
	{
		$this->_sort = ( $this->_sort != '' ) ? $this->_sort : 'asc';
		if ( $this->_sort == 'asc' )
		{
			if ( $this->_sort_key != '' )
			{
				if ( is_numeric($a[$this->_sort_key]) )
				{
					if ( $a[$this->_sort_key] < $b[$this->_sort_key] )
					{
						return -1;
					}
					elseif ( $a[$this->_sort_key] > $b[$this->_sort_key] )
					{
						return 1;
					}
					else
					{
						return 0;
					}
				}
				else
				{
					return strcasecmp( $a[$this->_sort_key], $b[$this->_sort_key] );
				}
			}
			else
			{
				if ( is_numeric($a) )
				{
					if ( $a < $b )
					{
						return -1;
					}
					elseif ( $a > $b )
					{
						return 1;
					}
					else
					{
						return 0;
					}
				}
				else
				{
					return strcasecmp( $a, $b );
				}
			}
		}
		elseif ( $this->_sort == 'desc' )
		{
			if ( $this->_sort_key != '' )
			{
				if ( is_numeric($a[$this->_sort_key]) )
				{
					if ( $a[$this->_sort_key] > $b[$this->_sort_key] )
					{
						return -1;
					}
					elseif ( $a[$this->_sort_key] < $b[$this->_sort_key] )
					{
						return 1;
					}
					else
					{
						return 0;
					}
				}
				else
				{
					return strcasecmp( $b[$this->_sort_key], $a[$this->_sort_key] );
				}
			}
			else
			{
				if ( is_numeric($a) )
				{
					if ( $a > $b )
					{
						return -1;
					}
					elseif ( $a < $b )
					{
						return 1;
					}
					else
					{
						return 0;
					}
				}
				else
				{
					return strcasecmp( $b, $a );
				}
			}
		}
		else
		{
			return 0;
		}
	}
}