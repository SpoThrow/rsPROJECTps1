<?php

/**
 * Product Title:		(SOS32) Reputation Points
 * Product Version:		2.0.1
 * Author:				Adriano Faria
 * Website:				SOS Invision
 * Website URL:			http://forum.sosinvision.com.br/
 * Email:				administracao@sosinvision.com.br
 */
 
if ( !defined('IN_ACP') )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded 'admin.php'.";
	exit();
}

class admin_reputationpoints_overview_overview extends ipsCommand 
{
	public $html;
	
	public function doExecute( ipsRegistry $registry )
	{
		/* Load Skin and Lang */
		$this->html               = $this->registry->output->loadTemplate( 'cp_skin_overview' );
		$this->html->form_code    = '&amp;module=overview&amp;section=overview';
		$this->html->form_code_js = '&module=overview&section=overview';
		
		switch( $this->request['do'] )
		{
			case 'settings':
				$this->_reputacaoSettings();
				break;
			case 'overview':
			default:
				$this->reputacaoOverview();
				break;
		}
		
		/* Output */
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();
	}
	
	public function _reputacaoSettings()
	{
		$classToLoad = IPSLib::loadActionOverloader( IPS_ROOT_PATH . 'applications/core/modules_admin/settings/settings.php', 'admin_core_settings_settings' );
		$settings    = new $classToLoad();
		$settings->makeRegistryShortcuts( $this->registry );
		
		$this->lang->loadLanguageFile( array( 'admin_tools' ), 'core' );
		
		$settings->html			= $this->registry->output->loadTemplate( 'cp_skin_settings', 'core' );	
				
		$settings->form_code	= $settings->html->form_code    = 'module=tools&amp;section=settings';
		$settings->form_code_js	= $settings->html->form_code_js = 'module=tools&section=settings';

		$this->request['conf_title_keyword'] = 'reputationpoints';
		$settings->return_after_save         = $this->settings['base_url'].$this->form_code.'&do=settings';
		$settings->_viewSettings();	
	}
	
	public function reputacaoOverview()
	{
		$this->registry->output->html .= $this->html->reputacaoOverviewIndex();		
	}
}
?>