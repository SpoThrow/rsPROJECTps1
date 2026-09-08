<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

if ( !defined( 'IN_ACP' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly.";
	exit();
}


class admin_shoutbox_settings_settings extends ipsCommand
{
	/**
	 * Main class entry point
	 *
	 * @access	public
	 * @param	object		ipsRegistry reference
	 * @return	void		[Outputs to screen]
	 */
	public function doExecute( ipsRegistry $registry )
	{
		// Check permissions
		$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_settings_' . $this->request['do'] );

		// Grab, init and load settings
		$classToLoad = IPSLib::loadActionOverloader( IPSLib::getAppDir('core').'/modules_admin/settings/settings.php', 'admin_core_settings_settings' );
		$settings    = new $classToLoad();
		$settings->makeRegistryShortcuts( $this->registry );

		// OK What we have to do here is set the SB global hook point depending on the setting, only if we are
		// loaded to do that of course.

		/*
		if( $this->request['do'] == 'global_hook' )
		{
			switch( $this->settings['shoutbox_global_hook'] )
			{
				case 's': $newHookData = 'a:6:{s:15:"classToOverload";s:0:"";s:9:"skinGroup";s:11:"skin_boards";s:12:"skinFunction";s:18:"boardIndexTemplate";s:4:"type";s:7:"foreach";s:2:"id";s:11:"side_blocks";s:8:"position";s:9:"outer.pre";}'; break;
				case 'b': $newHookData = 'a:6:{s:15:"classToOverload";s:0:"";s:9:"skinGroup";s:11:"skin_global";s:12:"skinFunction";s:14:"globalTemplate";s:4:"type";s:2:"if";s:2:"id";s:15:"mainpageContent";s:8:"position";s:10:"post.endif";}'; break;
				case 't': $newHookData = 'a:6:{s:15:"classToOverload";s:0:"";s:9:"skinGroup";s:11:"skin_global";s:12:"skinFunction";s:14:"globalTemplate";s:4:"type";s:2:"if";s:2:"id";s:15:"mainpageContent";s:8:"position";s:11:"pre.startif";}'; break;
				case 'ct': $newHookData = 'a:6:{s:15:"classToOverload";s:0:"";s:9:"skinGroup";s:11:"skin_boards";s:12:"skinFunction";s:18:"boardIndexTemplate";s:4:"type";s:2:"if";s:2:"id";s:11:"cats_forums";s:8:"position";s:11:"pre.startif";}'; break;
				case 'cb': $newHookData = 'a:6:{s:15:"classToOverload";s:0:"";s:9:"skinGroup";s:11:"skin_boards";s:12:"skinFunction";s:18:"boardIndexTemplate";s:4:"type";s:2:"if";s:2:"id";s:11:"cats_forums";s:8:"position";s:10:"post.endif";}'; break;
			}
			$this->DB->update( 'core_hooks_files', array( 'hook_data' => $newHookData ), "hook_classname='shoutboxGlobalShoutbox'" );
			$this->cache->rebuildCache( 'hooks', 'global' );
			require_once( IPS_ROOT_PATH . 'sources/classes/skins/skinFunctions.php' );
			require_once( IPS_ROOT_PATH . 'sources/classes/skins/skinCaching.php' );
			$skinCaching = new skinCaching( $this->registry );
			$skinCaching->flagSetForRecache();
		}
		*/

		// Load skin and language
		$settings->html         = $this->registry->output->loadTemplate( 'cp_skin_settings', 'core' );
		$settings->form_code	= $settings->html->form_code    = 'module=settings&amp;section=settings';
		$settings->form_code_js	= $settings->html->form_code_js = 'module=settings&section=settings';

		$this->lang->loadLanguageFile( array( 'admin_tools' ), 'core' );

		// Show our settings
		$this->request['conf_title_keyword'] = 'shoutbox_'.$this->request['do'];
		$settings->return_after_save         = $this->settings['base_url'] . 'module=settings&amp;section=settings&amp;do=' . $this->request['do'];
		$settings->_viewSettings();

		// Output
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();
	}
}
