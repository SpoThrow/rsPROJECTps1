<?php

/**
 *	Topic Moderators
 *
 * @author 		Martin Aronsen
 * @copyright	2008 - 2012 Invision Modding
 * @web: 		http://invisionmodding.com
 * @IPB ver.:	IP.Board 3.3
 * @version:	2.1.1  (21001)
 *
 */
 
if ( ! defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

class admin_topicMod_settings_settings extends ipsCommand
{
	public $html;
	public $registry;
	
	private $form_code;
	private $form_code_js;
	
	public function doExecute( ipsRegistry $registry )
	{
		$this->html         = $this->registry->output->loadTemplate( 'cp_skin_tools', 'core' );	
		
		$this->form_code 	= $this->html->form_code    = 'module=settings';
		$this->form_code_js = $this->html->form_code_js = 'module=settings';
		
		$this->registry->getClass('class_localization')->loadLanguageFile( array( 'admin_tools' ), 'core' );
		$this->registry->getClass('class_localization')->loadLanguageFile( array( 'admin_main' ) );
		
		
		if( $this->request['saved'] == 1 )
		{
			$this->registry->output->global_message = $this->lang->words['s_updated'];
		}
		
		$this->showSettings();

		
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();
	}
	

	
	private function showSettings()
	{
		$classToLoad = IPSLib::loadActionOverloader( IPSLib::getAppDir( 'core' ).'/modules_admin/settings/settings.php', 'admin_core_settings_settings' );
		$settings =  new $classToLoad;
		$settings->makeRegistryShortcuts( $this->registry );
				
		$settings->html			= $this->registry->output->loadTemplate( 'cp_skin_settings', 'core' );
		$settings->form_code	= $settings->html->form_code    = $this->form_code;
		$settings->form_code_js	= $settings->html->form_code_js = $this->form_code_js;

		$this->request['conf_title_keyword'] = 'im_topicMod';
		$settings->return_after_save     = $this->settings['base_url'] . $this->form_code . '&saved=1';
		$settings->_viewSettings();
	}
}