<?php

if ( !defined('IN_ACP') )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded 'admin.php'.";
	exit();
}

class admin_badges_configuration_settings extends ipsCommand
{
	public function doExecute( ipsRegistry $registry )
	{
		/* Get settings library */
		$classToLoad = IPSLib::loadActionOverloader( IPSLib::getAppDir('core').'/modules_admin/settings/settings.php', 'admin_core_settings_settings' );
		$settings    = new $classToLoad();
		$settings->makeRegistryShortcuts( $this->registry );
		
		/* Load the language */
		$this->lang->loadLanguageFile( array( 'admin_tools' ), 'core' );
		
		/* HTML and form code */
		$settings->html         = $this->registry->output->loadTemplate( 'cp_skin_settings', 'core' );		
		$settings->form_code    = $settings->html->form_code    = 'module=settings&amp;section=settings';
		$settings->form_code_js = $settings->html->form_code_js = 'module=settings&section=settings';		
		
		/* Some final stuff */
		$settings->return_after_save = $this->settings[ 'base_url' ] . $this->form_code;
		$this->request[ 'conf_title_keyword' ] = 'badges';
		
		/* Display the settings */
		$settings->_viewSettings();
		
		/* Output */
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();
	}
}