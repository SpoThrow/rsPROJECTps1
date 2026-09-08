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

class shoutboxLibrary
{
	/**
	 * Registry object
	 *
	 * @access	public
	 * @var		object
	 */
	public $registry;
	
	/**
	 * Registry object
	 *
	 * @access	private
	 * @var		boolean
	 */
	private $libraryLoaded = false;
	
	/**
	 * Post Handler class
	 *
	 * @access	public
	 * @var		object
	 */
	public $editor;
	
	/**
	* Parser Class
	*
	* @access	public
	* @var		object
	*/
	public $parser;
	
	/**
	 * Ajax Routines
	 *
	 * @access	public
	 * @var		object
	 */
	public $classAjax;
	
	/**
	 * Total Shouts
	 *
	 * @access	public
	 * @var		integer
	 */
	public $shout_total = 0;
	
	/**
	 * Shouts cache
	 *
	 * @access	private
	 * @var		array
	 */
	private $shouts_cache;
	
	/**
	 * max shout length allowed in bytes
	 * 
	 * @access	public
	 * @var		integer
	 */
	public $shout_max_length;
	
	/**
	 * Inactivity cutoff (in minutes)
	 *
	 * @access	public
	 * @var		integer
	 */
	public $inactivity_cutoff;
	
	/**
	 * Shouts order (asc|desc)
	 *
	 * @access	public
	 * @var		string
	 */
	public $shouts_order;
	
	/**
	 * Global shoutbox on?
	 *
	 * @access	public
	 * @var		boolean
	 */
	public $global_on = false;
	
	/**
	 * Preferences array
	 *
	 * @access	public
	 * @var		array
	 */
	public $prefs = array( 'shoutbox_height'        => 0,
						   'shoutbox_gheight'       => 0,
						   'global_display'         => 1,
						   'enter_key_shout'        => 1,
						   'enable_quick_commands'  => 1,
						   'display_refresh_button' => 1
						  );
	
	/**
	 * javascript preferences
	 *
	 * @access	public
	 * @var		array
	 */
	public $prefs_js = array();
	
	/**
	 * Moderator ID (if any)
	 *
	 * @access	public
	 * @var		integer
	 */
	public $moderator = 0;
	
	/**
	 * Moderator permsissions for JS
	 *
	 * @access	public
	 * @var		string
	 */
	public $mod_perms_js = '';
	
	/**
	 * Moderator permissions (for PHP)
	 *
	 * @access	public
	 * @var		array
	 */
	public $mod_perms = array();
	
	/**
	 * Contains the ignored users of the member
	 *
	 * @access	public
	 * @var		array
	 * @since	1.1.0 RC1
	 */
	public $ignoredUsers = array();
	
	private $cached_members = array();

	/**
    * Constructor
    *
    * @access	public
    * @param	object		ipsRegistry reference
    * @return	void
    */
	public function __construct( $registry )
	{
		/* Make registry objects */
		$this->registry   =  $registry;
		$this->DB         =  $this->registry->DB();
		$this->settings   =& $this->registry->fetchSettings();
		$this->request    =& $this->registry->fetchRequest();
		$this->lang       =  $this->registry->getClass('class_localization');
		$this->member     =  $this->registry->member();
		$this->memberData =& $this->registry->member()->fetchMemberData();
		$this->cache      =  $this->registry->cache();
		$this->caches     =& $this->registry->cache()->fetchCaches();
	}
	
	function show_error( $string='', $code='NO CODE YET', $return=false )
	{
		if ( $string == 'shoutbox_offline' )
		{
			/* Yeah, load parser and parse the offline msg */
			$this->parser->parse_smilies = 1;
			$this->parser->parse_html    = 1;
			$this->parser->parse_bbcode  = 1;
			
			/* Parse for display, this is from ACP so we need also preDbParse! */
			$string = $this->parser->preDbParse( $this->settings['shoutbox_offline_message'] );
			$string = $this->parser->preDisplayParse( $string );
			
			/* No message? Use the default */
			if ( $string == "" )
			{
				$string = $this->lang->words['error_shoutbox_offline'];
			}
		}
		
		if ( isset($this->lang->words[ 'error_'.$string ]) )
		{
			$string = $this->lang->words[ 'error_'.$string ];
		}

		if ( $return )
		{
			return $string;
		}
		
		$this->registry->output->showError( $string, $code );
	}
	
	public function _noModsMessage()
	{
		if ( $this->memberData['g_access_cp'] )
		{
			// Let's check less than 3 because member/groups arrays are always present
			if ( count($this->caches['shoutbox_mods']) < 3 )
			{
				return $this->registry->output->getTemplate('shoutbox')->no_mods();
			}
		}

		return "";
	}
	
	public function _noShoutsMessage()
	{
		if ( $this->shout_total <= 0 )
		{
			return $this->registry->output->getTemplate('shoutbox')->no_shouts();
		}

		return "";
	}
	
	# Added in Final release
	function saveAdminLog( $code, $action )
	{
		$this->DB->insert( 'admin_logs', array( 'member_id'    => $this->memberData['member_id'],
												'ctime'        => time(),
												'note'         => $action,
												'ip_address'   => $this->member->ip_address,
												'appcomponent' => 'shoutbox',
												'module'       => $this->request['module'],
												'section'      => $this->request['section'],
												'do'           => $code
  						  )						);
		
		return TRUE;
	}
	
	public function return_shouts( $return=false )
	{
		# Get request data
		$last_id = intval($this->request['lastid']);

		# Init vars
		$shouts  = array();
		$content = "";
		$new_ids = array();
		
		# Get our new shouts :D
		if ( $this->settings['shoutbox_skip_cache'] )
		{
			/* Load parser to parse shouts */
			$this->_loadParserAndEditor();
			
			$this->DB->build( array('select'   => 's.*',
									'from'     => array( 'shoutbox_shouts' => 's' ),
									'where'    => 's.s_id > '.$last_id,
									'order'    => 's.s_date DESC',
									'add_join' => array( 0 => array( 'select' => 'm.member_id, m.member_group_id, m.members_display_name, m.members_seo_name, mgroup_others',
																	 'from'   => array( 'members' => 'm' ),
																	 'where'  => 'm.member_id=s.s_mid',
																	 'type'   => 'left' ),
														 1 => array( 'select' => 'p.pp_main_photo, p.pp_main_width, p.pp_main_height, p.pp_thumb_photo, p.pp_thumb_width, p.pp_thumb_height',
																	 'from'   => array( 'profile_portal' => 'p' ),
																	 'where'  => 'p.pp_member_id=m.member_id',
																	 'type'   => 'left' ) ),
									'limit'    => array( 0, $this->settings['shoutbox_shouts_limit'] )
			)		);
			$noCache = $this->DB->execute();
			
			if ( $this->DB->getTotalRows($noCache) )
			{
				while ( $r = $this->DB->fetch($noCache) )
				{
					$this->parser->parsing_mgroup        = $r['member_group_id'];
					$this->parser->parsing_mgroup_others = $r['mgroup_others'];
					
					$r['s_message'] = $this->parser->preDisplayParse( $r['s_message'] );
					
					$shouts[ $r['s_id'] ] = $r;
				}
			}
		}
		else
		{
			$shouts = $this->shouts_cache;
		}
		
		// Sort shouts order
		if ( $this->shouts_order == 'asc' )
		{
			$shouts = array_reverse($shouts);
		}
		
		/* Let's load ignored users for parse_shout later */
		$this->_getIgnoredUsers();
		
		/* Parse shouts =D */
		if ( count( $shouts ) )
		{
			foreach ( $shouts as $row )
			{
				if ( $row['s_id'] <= $last_id )
				{
					// Adding a break here brokes everything,
					// leave continue in place!
					continue;
				}
				
				$content .= $this->parse_shout( $row );
				
				$new_ids[] = $row['s_id'];
			}
		}
		
		# First load?
		if ( $return )
		{
			return $content;
		}
		else
		{
			# Found new shouts?!?
			if ( $content != "" )
			{
				$new_ids =  count($new_ids) ? implode(",", $new_ids) : "";
				$a  = array('shouts' => $content,
							'ids'    => $new_ids,
							'sound'	 => '',
							#'force'  => 0, //TODO: this variable is needed in a future version to fix the bugs caused by editing a shout/clearing the cache
							);
				//----------------------------------
				// Shout Alerts
				//----------------------------------
				if( $new_ids != "" AND $this->settings['shoutbox_enable_sound'] AND ! $this->prefs[ 'disable_sound' ] )
				{
					$a['sound'] =  "soundManager.play('shoutbox-shouts', ipb.vars['board_url'] + '/public/sounds/shout.mp3' );\n";
				}


				$content = $this->registry->output->getTemplate('shoutbox')->shouts_ajax( $a );
			}
			$this->classAjax->returnHtml( $content );
		}
	}

	public function _get_announcement( $global=true )
	{
		/* Trim it! */
		$this->settings['shoutbox_announcement'] = trim($this->settings['shoutbox_announcement']);
		
		// Check :|
		if ( $this->settings['shoutbox_announcement'] != "" && $this->settings['shoutbox_announcement'] != "<br>" && $this->settings['shoutbox_announcement'] != "<br />" )
		{
			$hide = false;
			
			/* Override default settings */
			$this->parser->parse_smilies = 1;
			$this->parser->parse_html    = 1;
			$this->parser->parse_nl2br   = 0;
			$this->parser->parse_bbcode  = 1;
			
			/* set our mgroups */
			$this->parser->parsing_mgroup        = '';
			$this->parser->parsing_mgroup_others = '';
			
			/* Parse for display and reset values */
			$this->settings['shoutbox_announcement'] = $this->parser->preDisplayParse( $this->parser->preDbParse($this->settings['shoutbox_announcement']) );
			
			$this->parser->parse_html    = 0;
			$this->parser->parse_smilies = $this->settings['shoutbox_allow_smilies'];
			$this->parser->parse_bbcode  = $this->settings['shoutbox_allow_bbcode'];
		}
		else
		{
			$hide = true;
		}
		
		return $this->registry->output->getTemplate('shoutbox')->announcement( $global, $hide );
	}
	
	public function show_lang_for_js_use()
	{
		$langs = "\n\n";
		
		/* add only needed string - would love to have a JS file here.. */
		foreach( array( 'mod_loaded_confirm', 'my_prefs_loaded', 'sb_archive', 'sb_archive_loaded', 'filtered', 'page', 'of', 'filter_member_name_status', 'prefs_restored', 'prefs_saved', 'filtering', 'mod_shout_edited', 'processing', 'processed', 'saving_prefs', 'mod_opts_start_status', 'mod_opts_start_content', 'sure_delete_shout' ) as $key )
		{
			$word = str_replace( '"', '\\"', $this->lang->words[ $key ] );
			$langs .= "ipb.shoutbox.langs['{$key}'] = \"{$word}\";\n";
		}
		
		/* Let's parse also errors */
		foreach( array( 'blank_shout', 'shout_too_big', 'no_cmds_enabled', 'prefs_login', 'no_archive_perm', 'invalid_command', 'mod_no_perm', 'mod_no_perms', 'member_name_too_short', 'member_names_too_short', 'no_acp_access', 'flooding', 'prune_invalid_number', 'mod_invalid_name', 'mod_no_action', 'loading_members_viewing', 'already_submitting', 'already_filtering', 'no_found_shout', 'no_delete_shout' ) as $key )
		{
			$word = str_replace( '"', '\\"', $this->lang->words[ 'error_'.$key ] );
			$langs .= "ipb.shoutbox.errors['{$key}'] = \"{$word}\";\n";
		}
		
		return $langs;
	}

	function packEditHistory( $h=array() )
	{
		return addslashes(serialize($h));
	}

	function unpackEditHistory( $string )
	{
		return ( $string != '' && is_string($string) ) ? unserialize( stripslashes($string) ) : array();
	}

	private function _checkCachesStatus()
	{
		$load = array();
		
		/** Check if we have our caches **/
		if ( !$this->cache->exists('shoutbox_shouts') )
		{
			$load[] = 'shoutbox_shouts';
		}
		
		if ( !$this->cache->exists('shoutbox_mods') )
		{
			$load[] = 'shoutbox_mods';
		}
		
		if ( $this->global_on && !$this->cache->exists('emoticons') )
		{
			$load[] = 'emoticons';
		}
		
		// Got caches to load?
		if ( count($load) )
		{
			$this->cache->getCache( $load );
		}
	}
	
	public function _loadParserAndEditor()
	{
		if ( !is_object($this->parser) )
		{
			/* Load and config the post parser */
			require_once( IPS_ROOT_PATH . "sources/handlers/han_parse_bbcode.php" );
			$this->parser = new parseBbcode( ipsRegistry::instance() );
		}
		
		// Setup parsing options
		$this->parser->allow_update_caches	= 1;
		$this->parser->bypass_badwords		= intval($this->memberData['g_bypass_badwords']);
		$this->parser->parse_html			= 0;
		$this->parser->parse_nl2br			= 0;
		$this->parser->parse_smilies		= $this->settings['shoutbox_allow_smilies'];
		$this->parser->parse_bbcode			= $this->settings['shoutbox_allow_bbcode'];
		$this->parser->parsing_section		= 'shoutbox_shouts';
		
		if ( !is_object( $this->editor ) && IPS_IS_UPGRADER !== TRUE && IPS_IS_INSTALLER !== TRUE )
		{
			/* Load editor stuff */
			$classToLoad = IPSLib::loadLibrary( IPS_ROOT_PATH . 'sources/classes/editor/composite.php', 'classes_editor_composite' );
			$this->editor = new $classToLoad();
		}
	}
	
	public function _getIgnoredUsers()
	{
		/* Got ignored users? */
		if ( is_array($this->member->ignored_users) && count($this->member->ignored_users) )
		{
			foreach( $this->member->ignored_users as $_i )
			{
				if( $_i['ignore_topics'] )
				{
					$this->ignoredUsers[] = $_i['ignore_ignore_id'];
				}
			}
		}
		else
		{
			$this->ignoredUsers = array();
		}
	}
	
	public function _startup()
	{
		if ( $this->global_on )
		{
			$this->shouts_order = ($this->settings['shoutbox_global_shout_ordering'] == 'asc') ? 'asc' : 'desc';
		}
		else
		{
			$this->shouts_order = ($this->settings['shoutbox_shout_ordering'] == 'desc') ? 'desc' : 'asc';
		}
		
		if ( $this->libraryLoaded )
		{
			return TRUE;
		}
		
		/* Fix global shoutbox value for every area */
		if ( isset($this->request['global']) && $this->request['global'] == 1 )
		{
			$this->global_on = true;
		}
		
		/* Check if our caches are loaded */
		$this->_checkCachesStatus();
		
		/* If this is a bot stop him from using the shoutbox */
		if ( $this->member->is_not_human )
		{
			$this->memberData['g_shoutbox_use'] = 0;
			$this->moderator = 0;
		}
		
		/* Load IPB parser/editor */
		$this->_loadParserAndEditor();
		
		/* Are we using the ajax module? */
		if ( ipsRegistry::$current_module == 'ajax' )
		{
			require_once( IPS_KERNEL_PATH . 'classAjax.php');
			$this->classAjax = new classAjax();
		}
		
		# Link cache
		$this->shout_total  = intval($this->caches['shoutbox_shouts']['total']);
		$this->shouts_cache = $this->caches['shoutbox_shouts']['shouts'];

		$this->_load_shoutbox_prefs();

		$this->shoutbox_title    = (trim($this->settings['shoutbox_title']) != '') ? trim($this->settings['shoutbox_title']) : $this->lang->words['shoutbox_title'];
		$this->shout_max_length  = intval($this->settings['shoutbox_max_shout_length']*1024);
		$this->settings['shoutbox_shouts_limit'] = intval($this->settings['shoutbox_shouts_limit']) ? intval($this->settings['shoutbox_shouts_limit']) : 15 ;
		$this->settings['shoutbox_inactivity_cutoff'] = intval( $this->settings['shoutbox_inactivity_cutoff'] ) > 30 ? 30 : intval( $this->settings['shoutbox_inactivity_cutoff'] );
		$this->inactivity_cutoff = $this->settings['shoutbox_inactivity_cutoff'] ? $this->settings['shoutbox_inactivity_cutoff'] : 10;

		
		
		$this->_get_mod_perms();
		
		# Be sure that we have a proper int value to truncate names
		$this->settings['shoutbox_truncate_names'] = ( intval($this->settings['shoutbox_truncate_names']) > 0 ) ? intval($this->settings['shoutbox_truncate_names']) : 15;
		
		// Setup our library as loaded
		$this->libraryLoaded = true;
	}

	public function _get_mod_perms()
	{
		// Are we allowed to use this function?
		if ( $this->memberData['member_id'] && $this->memberData['g_shoutbox_use'] )
		{
			if ( isset($this->caches['shoutbox_mods']['members'][ $this->memberData['member_id'] ]) )
			{
				$this->moderator = $this->caches['shoutbox_mods']['members'][ $this->memberData['member_id'] ];
			}
			elseif ( isset($this->caches['shoutbox_mods']['groups'][ $this->memberData['member_group_id'] ]) )
			{
				$this->moderator = $this->caches['shoutbox_mods']['groups'][ $this->memberData['member_group_id'] ];
			}

			# Found it? Setup all so!
			if ( $this->moderator )
			{
                $this->mod_perms = $this->caches['shoutbox_mods'][ $this->moderator ];
                $p = array();
                
                # Fix edit shout perms
                if ( $this->checkModeratorPerm('edit_shouts', true) )
                {
                    $this->memberData['g_shoutbox_edit'] = 1;
                }
                
                foreach ( $this->mod_perms as $k => $v )
                {
                    if ( in_array($k, array('m_id', 'm_type', 'm_mg_id')) )
                    {
                        continue;
                    }
                    
                    $p[] = "'{$k}' : {$v}";
                }
                
                $this->mod_perms_js = implode( ", ", $p );
			}
		}
	}

	public function _load_shoutbox_prefs()
	{
		// Alowed to use the shoutbox?
		if ( $this->memberData['g_shoutbox_use'] )
		{
			//How to convert this ipsclass->member?
			$p = unserialize(stripslashes($this->memberData['_cache']['shoutbox_prefs']));

			if ( is_array($p) && count($p) )
			{
				foreach ( $p as $k => $v )
				{
					$this->prefs[ $k ] = $v;
				}
			}
		}

		if ( intval($this->prefs['shoutbox_height']) < 100 )
		{
			$this->prefs['shoutbox_height'] = $this->settings['shoutbox_default_height'];
		}

		if ( intval($this->prefs['shoutbox_gheight']) < 100 )
		{
			$this->prefs['shoutbox_gheight'] = $this->settings['shoutbox_default_gheight'];
		}

		$this->_load_shoutbox_prefs_js();
	}

	private function _load_shoutbox_prefs_js()
	{
		$data = array();

		foreach ( $this->prefs as $k => $v )
		{
			$data[ $k ] .= "'{$k}' : \"{$v}\"";
		}

		$this->prefs_js = implode(', ', $data);
	}

	public function _save_shoutbox_prefs( $reset=false )
	{
		if ( !$this->memberData['member_id'] || !$this->memberData['g_shoutbox_use'] )
		{
			return false;
		}

		if ( $reset )
		{
			unset($this->memberData['_cache']['shoutbox_prefs']);
		}
		else
		{
			$this->memberData['_cache']['shoutbox_prefs'] = serialize($this->prefs);
		}

		IPSMember::packMemberCache( $this->memberData['member_id'], $this->memberData['_cache'], $this->memberData['_cache'] );

		$this->_load_shoutbox_prefs_js();
	}

	public function parse_shout( $r=array() )
	{
		if ( !is_array($r) || !count($r) )
		{
			return "";
		}

		if ( !$r['member_id'] )
		{
			$r['members_display_name'] = $this->lang->words['global_guestname'];
			$r['member_group_id'] = $this->settings['guest_group'];
		}

		$r['_archive'] = ($r['_archive'] != '') ? $r['_archive'] : '';
		
		// Get a proper date from UNIX
		$r['s_date']   = $this->lang->getDate( $r['s_date'], 'manual{'.$this->settings['shoutbox_time_long'].'}' );
		#$r['s_date']   = ( $r['s_date'] > $this->_start_stamp ) ? $this->lang->getDate( $r['s_date'], 'manual{'.$this->settings['shoutbox_time_long'].'}' ) : $this->lang->getDate( $r['s_date'], 'manual{'.$this->settings['shoutbox_time_today'].'}' );
		
		/* Format names */
		if ( isset( $this->cached_members[ $r['member_id'] ] ) )
		{
			$r['_members_display_name'] = $this->cached_members[ $r['member_id'] ];
		}
		else
		{
			$r['_members_display_name'] = IPSText::truncate( $r['members_display_name'], $this->settings['shoutbox_truncate_names'] );
			$r['_members_display_name'] = $this->settings['shoutbox_format_names'] ? IPSMember::makeNameFormatted( $r['_members_display_name'], $r['member_group_id'] ) : $r['_members_display_name'];
			$this->cached_members[ $r['member_id'] ] = $r['_members_display_name'];
		}
		
		//-----------------------------------------
		// Are we ignoring this shouter? (Beta 2)
		//-----------------------------------------
		$r['_ignored'] = 0;
		
		if( count($this->ignoredUsers) )
		{
			if( in_array( $r['member_id'], $this->ignoredUsers ) )
			{
				if ( ! strstr( $this->settings['cannot_ignore_groups'], ','.$poster['member_group_id'].',' ) )
				{
					$r['_ignored'] = 1;
				}
			}
		}
		
		/* Show the edit shout button? */
		$can_edit = false;
		
		if ( $this->memberData['member_id'] && $this->memberData['member_id'] == $r['member_id'] && $this->memberData['g_shoutbox_edit'] )
		{
			$can_edit = true;
		}
		
		/* Profile photo */
		$r['photo'] = IPSMember::buildProfilePhoto( $r, 'icon' );
		
		/* Sidebar hook enabled and global on? */
		if ( $this->global_on && $this->settings['shoutbox_global_hook'] == 's' )
		{
			return $this->registry->output->getTemplate('shoutbox')->shout_row_sidebar( $r, $can_edit, $r['ajax_return'] );
		}
		else
		{
			return $this->registry->output->getTemplate('shoutbox')->shout_row( $r, $can_edit, $r['ajax_return'] );
		}
	}
	
	public function getShout( $id=0 )
	{
		/* Init some vars */
		$shout = array();
		$id    = intval($id);
		
		/* We have a proper ID? O.ò */
		if ( $id <= 0 )
		{
			return $shout;
		}

		$shout = $this->DB->buildAndFetch( array( 'select'   => 's.*, m.member_id, m.member_group_id, m.members_display_name, m.members_seo_name, m.members_cache, mgroup_others',
												  'from'     => array( 'shoutbox_shouts' => 's' ),
												  'where'    => 's.s_id='.$id,
												  'add_join' => array( 0 => array( 'from'   => array( 'members' => 'm' ),
																				   'where'  => 'm.member_id=s.s_mid',
																				   'type'   => 'left' ),
																	   1 => array( 'select' => 'p.pp_main_photo, p.pp_main_width, p.pp_main_height, p.pp_thumb_photo, p.pp_thumb_width, p.pp_thumb_height',
																				   'from'   => array( 'profile_portal' => 'p' ),
																				   'where'  => 'p.pp_member_id=m.member_id',
																				   'type'   => 'left' ) ),
										  )		 );
		
		/* Deleted member? Setup as guest so :D */
		if ( !$shout['member_id'] )
		{
			$shout['member_id'] = 0;
			$shout['member_group_id'] = $this->settings['guest_group'];
			$shout['members_display_name'] = $this->lang->words['global_guestname'];
		}
		else
		{
			// Actual member, let's unpack the cache
			$shout['_cache'] = IPSMember::unpackMemberCache( $shout['members_cache'] );
		}
		
		/* Let's unpack also the edit history cache */
		$shout['s_edit_history'] = $this->unpackEditHistory( $shout['s_edit_history'] );
		
		/* And get a photo */
		$shout['photo'] = IPSMember::buildProfilePhoto( $shout, 'icon' );
		
		return $shout;
	}

	public function _get_member( $d )
	{
		# Vars
		$where  = "";
		$member = array();

		if ( is_numeric($d) && $d )
		{
			$where = "member_id=".intval($d);
		}
		elseif ( is_string($d) && $d != "" )
		{
			$where = "members_l_display_name='".$this->DB->addSlashes( strtolower( trim($d) ) )."'";
		}

		if ( $where != "" )
		{
			$member = $this->DB->buildAndFetch( array( 'select' => 'member_id, member_group_id, members_display_name, members_cache, members_seo_name, mgroup_others',
													   'from'   => 'members',
													   'where'  => $where
											   )	  );
			
			/* Unpack member cache here */
		$member['_cache'] = IPSMember::unpackMemberCache( $member['members_cache'] );
		}
		
		return $member;
	}

	public function _get_members( $ids=array() )
	{
		/* Init vars */
		$data = array();
		
		if ( is_array($ids) )
		{
			if ( count($ids) > 1 )
			{
				$this->DB->build( array('select' => 'member_id, member_group_id, members_display_name, members_cache, members_seo_name, mgroup_others',
										'from'   => 'members',
										'where'  => 'member_id IN ('.implode(',', $ids).')'
								 )		);
				$this->DB->execute();

				while ( $r = $this->DB->fetch() )
				{
					/* Unpack member cache here */
					$r['_cache'] = IPSMember::unpackMemberCache( $r['members_cache'] );
					
					$data[ $r['member_id'] ] = $r;
				}
			}
			elseif ( count($ids) == 1 )
			{
				$data = array( $ids[0] => $this->_get_member($ids[0]) );
			}
			else
			{
				return array();
			}
		}
		elseif ( is_numeric($ids) )
		{
			$data = array( $ids => $this->_get_member($ids) );
		}

		return $data;
	}
	
	public function checkIsModerator( $memberID=0, $groupID=0 )
	{
		if ( !$memberID && !$groupID )
		{
			return FALSE;
		}

		if ( isset($this->caches['shoutbox_mods']['members'][ $memberID ]) || isset($this->caches['shoutbox_mods']['groups'][ $groupID ]) )
		{
			return TRUE;
		}

		return FALSE;
	}
    
	public function checkModeratorPerm( $perm='', $return=false )
	{
		// Beta 3: Check if we can edit own shouts =O
		if ( $perm == 'edit_shouts' && $this->memberData['g_shoutbox_edit'] )
		{
			return TRUE;
		}
		elseif ( !$this->mod_perms[ 'm_'.$perm ] )
		{
			if ( $return )
			{
				return FALSE;
			}
			
			$this->classAjax->returnString('error-mod_no_perm');
			exit();
		}

		return TRUE;
	}
	
	public function getMembersViewing( $ajax=true, $addMe=false )
	{
		$active = array( 'TOTAL'   => 0 ,
						 'NAMES'   => array(),
						 'GUESTS'  => 0 ,
						 'MEMBERS' => 0 ,
						 'ANON'    => 0 ,
					   );
		
		if( !$this->settings['au_cutoff'] )
		{
			$this->settings['au_cutoff'] = 15;
		}
		
		//-----------------------------------------
		// Get the users from the DB
		//-----------------------------------------
		$cut_off = $this->settings['au_cutoff'] * 60;
		$time    = time() - $cut_off;
		$rows    = array();
		$ar_time = time();
		
		/* Add our ID only if the call is not ajax */ 
		if ( $addMe && $this->memberData['member_id'] )
		{
			$rows = array( $ar_time.'.'.md5( microtime() ) => array( 
																	'id'           => 0,
																	'login_type'   => substr( $this->memberData['login_anonymous'], 0, 1),
																	'running_time' => $ar_time,
																	'seo_name'     => $this->memberData['members_seo_name'],
																	'member_id'    => $this->memberData['member_id'],
																	'member_name'  => $this->memberData['members_display_name'],
																	'member_group' => $this->memberData['member_group_id'] 
																	) 
						);
		}
		
		$this->DB->build( array( 'select' => 'id, member_id, member_name, seo_name, login_type, running_time, member_group, uagent_type',
								 'from'   => 'sessions',
								 'where'  => "current_appcomponent='shoutbox' AND running_time > $time",
						 )		);
		$this->DB->execute();
		
		//-----------------------------------------
		// FETCH...
		//-----------------------------------------
		while ( $r = $this->DB->fetch() )
		{
			$rows[ $r['running_time'].'.'.$r['id'] ] = $r;
		}
		
		krsort( $rows );
		
		//-----------------------------------------
		// cache all printed members so we
		// don't double print them
		//-----------------------------------------
		$cached = array();
		
		foreach ( $rows as $result )
		{
			$last_date = $this->lang->getTime( $result['running_time'] );
			
			//-----------------------------------------
			// Bot?
			//-----------------------------------------
			if ( isset($result['uagent_type']) && $result['uagent_type'] == 'search' )
			{
				//-----------------------------------------
				// Seen bot of this type yet?
				//-----------------------------------------
				if ( !$cached[ $result['member_name'] ] )
				{
					if ( $this->settings['spider_anon'] )
					{
							if ( $this->memberData['g_access_cp'] )
						{
							$active['NAMES'][] = $result['member_name'];
						}
					}
					else
					{
						$active['NAMES'][] = $result['member_name'];
					}
					
					$cached[ $result['member_name'] ] = 1;
				}
				else
				{
					//-----------------------------------------
					// Yup, count others as guest
					//-----------------------------------------
					$active['GUESTS']++;
				}
			}
			
			//-----------------------------------------
			// Guest?
			//-----------------------------------------
			elseif ( !$result['member_id'] || !$result['member_name'] )
			{
				$active['GUESTS']++;
			}
			
			//-----------------------------------------
			// Member?
			//-----------------------------------------
			else
			{
				if ( empty($cached[ $result['member_id'] ]) )
				{
					$cached[ $result['member_id'] ] = 1;
					
					$result['member_name'] = IPSMember::makeNameFormatted( $result['member_name'], $result['member_group'] );
					
					if ( !$this->settings['disable_anonymous'] AND $result['login_type'] )
					{
						if ( $this->memberData['g_access_cp'] and ($this->settings['disable_admin_anon'] != 1) )
						{
							$active['NAMES'][] = $this->registry->output->getTemplate('shoutbox')->memberViewingName( $result['member_id'], "<a href='".$this->registry->output->buildSEOUrl( "showuser={$result['member_id']}", 'public', $result['seo_name'], 'showuser' )."' title='$last_date'>{$result['member_name']}</a>*" );
							$active['ANON']++;
						}
						else
						{
							$active['ANON']++;
						}
					}
					else
					{
						$active['MEMBERS']++;
						$active['NAMES'][] = $this->registry->output->getTemplate('shoutbox')->memberViewingName( $result['member_id'], "<a href='".$this->registry->output->buildSEOUrl( "showuser={$result['member_id']}", 'public', $result['seo_name'], 'showuser' )."' title='$last_date'>{$result['member_name']}</a>" );
					}
				}
			}
		}
		
		/* If guests can't view the Shoutbox, let's remove them */
		if ( !$this->caches['group_cache'][ $this->settings['guest_group'] ]['g_shoutbox_view'] )
		{
			$active['GUESTS'] = 0;
		}
		
		$active['TOTAL'] = $active['MEMBERS'] + $active['GUESTS'] + $active['ANON'];
		
		/* Ajaxing? :O */
		if ( $ajax )
		{
			//Fix up images directory
			$active['NAMES'] = str_replace( "{style_image_url}", $this->settings['img_url'], $active['NAMES'] );
			
			$this->classAjax->returnJsonArray( $active );
		}
		else
		{
			$this->lang->loadLanguageFile( array( 'public_boards' ), 'forums' );
			
			$this->lang->words['active_users'] = sprintf( $this->lang->words['active_users'], $this->settings['au_cutoff'] );
			
			return $active;
		}
	}
	
	public function recacheShouts( $rebuild='', $updateCaches=true )
	{
		/* Setup vars */
		$cache['shouts'] = array();
		
		/* Load parser to parse shouts on recache */
		$this->_loadParserAndEditor();
		
		$this->DB->build( array('select'   => 's.*',
								'from'     => array( 'shoutbox_shouts' => 's' ),
								'order'    => 's.s_date DESC',
								'add_join' => array( 0 => array( 'select' => 'm.member_id, m.member_group_id, m.members_display_name, m.members_seo_name, mgroup_others',
																 'from'   => array( 'members' => 'm' ),
																 'where'  => 'm.member_id=s.s_mid',
																 'type'   => 'left' ),
													 1 => array( 'select' => 'p.pp_main_photo, p.pp_main_width, p.pp_main_height, p.pp_thumb_photo, p.pp_thumb_width, p.pp_thumb_height',
																 'from'   => array( 'profile_portal' => 'p' ),
																 'where'  => 'p.pp_member_id=m.member_id',
																 'type'   => 'left' ) ),
								'limit'    => array( 0, $this->settings['shoutbox_shouts_limit'] )
		)		);
		$cacheShouts = $this->DB->execute();
		
		while ( $r = $this->DB->fetch($cacheShouts) )
		{
			$this->parser->parsing_mgroup        = $r['member_group_id'];
			$this->parser->parsing_mgroup_others = $r['mgroup_others'];
			
			$r['s_message'] = $this->parser->preDisplayParse( $r['s_message'] );
			
			/* Unset mgroup_others to reduce cache size */
			unset($r['mgroup_others']);
			
			$cache['shouts'][ $r['s_id'] ] = $r;
		}
		
		/* Which type of rebuild? */
		switch ( $rebuild )
		{
			case 'add':
				$cache['total'] = $this->caches['shoutbox_shouts']['total'] + 1;
				break;
			case 'remove':
				$cache['total'] = $this->caches['shoutbox_shouts']['total'] - 1;
				break;
			case 'recount':
			default:
				$tmp = $this->DB->buildAndFetch( array( 'select' => 'count(s_id) as total', 'from' => 'shoutbox_shouts' ) );
				$cache['total'] = intval($tmp['total']);
				break;
		}
		
		/* Update local caches? */
		if ( $updateCaches )
		{
			$this->shout_total  = $cache['total'];
			$this->shouts_cache = $cache['shouts'];
		}
		
		/* Finally update cache */
		$this->cache->setCache( 'shoutbox_shouts', $cache, array( 'array' => 1, 'deletefirst' => 1 ) );
	}
	
	public function recacheModerators()
	{
		$cache = array( 'groups' => array(), 'members' => array() );
		
		$this->DB->build( array( 'select' => "*", 'from' => 'shoutbox_mods' ) );
		$this->DB->execute();
		
		while ( $r = $this->DB->fetch() )
		{
			if ( $r['m_type'] == 'group' )
			{
				$cache['groups'][ $r['m_mg_id'] ] = $r['m_id'];
			}
			else
			{
				$cache['members'][ $r['m_mg_id'] ] = $r['m_id'];
			}
			
			$cache[ $r['m_id'] ] = $r;
		}
		
		$this->cache->setCache( 'shoutbox_mods', $cache, array( 'array' => 1, 'deletefirst' => 1 ) );
	}
}