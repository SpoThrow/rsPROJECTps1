<?php
/**
 * @ Application : 		Custom Moderator Team Page v2.0.0
 * @ Last Updated : 	June 13th, 2012 
 * @ Author :			Michael S. Edwards
 * @ Copyright :		(c) 2011 Coding Jungle
 * @ Link	 :			http://www.codingjungle.com/
 */

if ( !defined('IN_ACP') )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}
 
class admin_cmtp_settings_settings extends ipsCommand
{

	public function doExecute( ipsRegistry $registry )
	{
		$this->registry->getClass('class_permissions')->checkForAppAccess( 'cmtp');

		$this->registry->getClass('class_permissions')->checkForModuleAccess( 'cmtp', 'settings' );	
		/* Load Languages */
		$this->registry->class_localization->loadLanguageFile( array( 'admin_tools' ), 'core' );
	
		$this->registry->class_localization->loadLanguageFile( array( 'admin_lang' ), 'promenu' );

		/* Grab, init and load settings */
		$classToLoad = IPSLib::loadActionOverloader( IPSLib::getAppDir('core').'/modules_admin/settings/settings.php', 'admin_core_settings_settings' );
	
		$settings    = new $classToLoad();
	
		$settings->makeRegistryShortcuts( $this->registry );

		$settings->html         = $this->registry->output->loadTemplate( 'cp_skin_settings', 'core' );
	
		$this->form_code    = 'module=settings&amp;section=settings';
	
		$this->form_code_js = 'module=settings&section=settings';	

		/* ok..the breadcrumb needs to be removed and rebuild .. so let's remove it now !!! */
		$this->registry->output->ignoreCoreNav = TRUE;
		
		switch( $this->request['do'] )
		{

			default:
				// Lets build the new breadcrumb
				$this->registry->output->extra_nav[] = array( '', IPSLib::getAppTitle( 'cmtp' ) );
				$this->registry->output->extra_nav[] = array( $this->settings['base_url'] . $this->form_code, "Settings" );				
				$this->registry->output->extra_nav[] = array( "", "Settings" );	

				/* Show our settings */
				$this->request['conf_title_keyword'] = 'cmtp';
				$settings->return_after_save         = $this->settings['base_url'] . $this->form_code;
				$settings->_viewSettings();
				break;			
		}
		// Send It Out!!!
		$this->registry->getClass('output')->html_main .= $this->registry->getClass('output')->global_template->global_frame_wrapper();
		 
		$this->registry->getClass('output')->sendOutput();	
	}
}