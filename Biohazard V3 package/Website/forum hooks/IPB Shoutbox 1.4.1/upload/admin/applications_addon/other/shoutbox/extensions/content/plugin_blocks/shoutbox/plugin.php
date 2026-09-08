<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */
 
class plugin_shoutbox implements pluginBlockInterface
{
/**#@+
	 * Registry Object Shortcuts
	 *
	 * @access	protected
	 * @var		object
	 */
	protected $DB;
	protected $settings;
	protected $lang;
	protected $member;
	protected $memberData;
	protected $cache;
	protected $caches;
	protected $registry;
	protected $request;
	/**#@-*/

	/**
	 * Constructor
	 *
	 * @access	public
	 * @param	object		Registry reference
	 * @return	@e void
	 */
	public function __construct( ipsRegistry $registry )
	{
	//-----------------------------------------
		// Make shortcuts
		//-----------------------------------------
		$this->registry		= $registry;
		$this->DB			= $this->registry->DB();
		$this->settings		=& $this->registry->fetchSettings();
		$this->member		= $this->registry->member();
		$this->memberData	=& $this->member->fetchMemberData();
		$this->cache		= $this->registry->cache();
		$this->caches		=& $this->cache->fetchCaches();
		$this->request		=& $this->registry->fetchRequest();
		$this->lang 		= $this->registry->getClass('class_localization');
if(IPSLib::appIsInstalled('shoutbox'))
{
		$this->registry->getAppClass('shoutbox');
}
	}
/**
	 * Return the tag help for this block type
	 *
	 * @access	public
	 * @return	array
	 */
	public function getTags()
	{
		return array(
$this->lang->words['block_plugin__generic']	=> array(
array( '&#36;title', $this->lang->words['block_custom__title'] ) ,																		),
IPSLib::getAppTitle('shoutbox')	=> array(
					array( "&#36;content", IPSLib::getAppTitle('shoutbox')),
					)
);
	}
/**
	 * Return the plugin meta data
	 *
	 * @access	public
	 * @return	array 			Plugin data (name, description, hasConfig)
	 */
	public function returnPluginInfo()
	{
if(!IPSLib::appIsInstalled('shoutbox'))
{
return array();
}
		return array(
					'key'			=> 'shoutbox',
'app'			=> 'shoutbox',
					'name'			=> IPSLib::getAppTitle('shoutbox'),
					'description'	=> '',
					'hasConfig'		=> true,
					'templateBit'	=> 'block__custom',
					);
	}
public function returnPluginConfig( $session )
	{
$templates = array('Global', 'Sidebar');
$session['config_data']['custom_config']['hook_type'] = in_array($session['config_data']['custom_config']['hook_type'], $templates)?$session['config_data']['custom_config']['hook_type']:'global';
		$databases	= array();
$opts = array();
foreach( $templates as $id)
{
$opts[] = array($id, $id.' '.IPSLib::getAppTitle('shoutbox'));
}


$filters[] = array( 'label'     => $this->lang->words['generic__select_contenttype'],
                'description' => $this->lang->words['generic__desc_contenttype'],
                'field'    => $this->registry->output->formDropdown( 'hook_type', $opts, $session['config_data']['custom_config']['hook_type'] ),
         );

return $filters;
}
/**
	 * Check the plugin config data
	 *
	 * @access	public
	 * @param	array 			Submitted plugin data to check (usually $this->request)
	 * @return	array 			Array( (bool) Ok or not, (array) Plugin data to use )
	 */
	public function validatePluginConfig( $data )
	{
$templates = array('Global', 'Sidebar');
$id = trim($data['hook_type']);
if(!in_array($id, $templates))
{
return array( false, array('hook_type' => $templates[0]));
}
return array( true, array( 'hook_type' => $id ) );
}
/**
	 * Execute the plugin and return the HTML to show on the page.
	 * Can be called from ACP or front end, so the plugin needs to setup any appropriate lang files, skin files, etc.
	 *
	 * @access	public
	 * @param	array 				Block data
	 * @return	string				Block HTML to display or cache
	 */
	public function executePlugin( $block )
	{
if(!IPSLib::appIsInstalled('shoutbox'))
{
return;
}
$config	= unserialize($block['block_config']);
$_hook = trim($config['custom']['hook_type']);
/* Check temporary ban status */
		if( $this->memberData['temp_ban'] )
		{
			# Let's just return, it is useless to process it since it is already done in ipsRegistry
			return;
		}

		/* Check the new way to ban */
		if ( $this->memberData['member_banned'] == 1 )
		{
			return;
		}

		/* No permission to view the board */
		if ( $this->memberData['g_view_board'] != 1 )
		{
			return;
		}

		/* Offline or can't view? */
		if ( !$this->settings['shoutbox_online'] || !$this->memberData['g_shoutbox_view'] || (!$this->memberData['g_shoutbox_view'] && !$this->memberData['g_shoutbox_use']) || ( $this->settings['board_offline'] && !$this->memberData['g_access_offline'] ) )
		{
			return;
		}

		/** Shoutbox app? - Are we shoutbox banned? **/
		if ( ipsRegistry::$current_application == 'shoutbox' || ipsRegistry::$current_application == 'ipchat' || $this->memberData['_cache']['shoutbox_banned'] )
		{
			return;
		}

		/* Login enforced? */
		if ( !$this->memberData['member_id'] && $this->settings['force_login'] == 1 )
		{
			return;
		}




		$this->registry->getAppClass('shoutbox');
		$this->registry->getClass('shoutboxLibrary')->global_on = true;
		$this->registry->getClass('shoutboxLibrary')->_startup();



		// We have the right amount of posts
		// to use it if we can use it?
		if ( $this->memberData['g_shoutbox_use'] && $this->memberData['g_shoutbox_posts_req'] )
		{
			$this->memberData['g_shoutbox_posts_req'] = intval($this->memberData['g_shoutbox_posts_req']);
			if ($this->memberData['g_shoutbox_posts_req'] && $this->memberData['posts'] < $this->memberData['g_shoutbox_posts_req'])
			{
				$this->memberData['g_shoutbox_use'] = 0;
				$this->registry->getClass('shoutboxLibrary')->moderator = 0;
			}
		}



		$d = array( 'shout_height' => $this->registry->getClass('shoutboxLibrary')->prefs['shoutbox_gheight'],
					'announcement' => $this->registry->getClass('shoutboxLibrary')->_get_announcement(),
					'noshouts'     => $this->registry->getClass('shoutboxLibrary')->_noShoutsMessage(),
					'shouts'       => $this->registry->getClass('shoutboxLibrary')->return_shouts(true),
					);

		/* Setup JS things */
		$d['js']  = $this->registry->getClass('output')->getTemplate('shoutbox')->javascript( $d );
		$d['js'] .= $this->registry->getClass('shoutboxLibrary')->show_lang_for_js_use();
/* Which hook? */
		if ( $_hook == 'Sidebar' )
		{
			$hook = 'hookGlobalShoutboxSidebar';
		}
		else
		{
			# Not sidebar? Use the normal one then
			$hook = 'hookGlobalShoutbox';
		}
		$html = '<!--- ShoutBoxJsLoader --->'.$this->registry->getClass('output')->getTemplate('shoutbox_hooks')->$hook( $d );
	$pluginConfig	= $this->returnPluginInfo();
		$templateBit	= $pluginConfig['templateBit'] . '_' . $block['block_id'];

		ob_start();
 		$_return	=  $this->registry->getClass('output')->getTemplate('ccs')->$templateBit( $block['block_name'], $html );
 		ob_end_clean();
 		return $_return;
}
}
?>
