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
 
class admin_cmtp_overview_overview extends ipsCommand
{

	public function doExecute( ipsRegistry $registry )
	{
		$this -> lang -> loadLanguageFile(array('admin_lang'), 'cmtp');

		$this->registry->getClass('class_permissions')->checkForAppAccess( 'cmtp');

		$this->registry->getClass('class_permissions')->checkForModuleAccess( 'cmtp', 'overview' );	
			
		$this->html = $this->registry->output->loadTemplate( 'cp_skin_overview' );
				//BEGONE breadcrumb
		$this->registry->output->ignoreCoreNav = TRUE;
		
		// Lets build the new breadcrumb
		$this->registry->output->extra_nav[] = array( '', IPSLib::getAppTitle( 'cmtp' ) );
		
		$this->registry->output->extra_nav[] = array( $this->settings['base_url']."module=overview&amp;section=overview", "Overview" );	


		/* Which do */
		switch( $this->request['do'] )
		{
			case "view":
				$this->_View();
				break;

			default:
				$this->_View();
				break;
		}
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();	
	}

	private function _View()
	{

		$this->registry->output->html .= $this->html->JavascriptGroups();
		$this->registry->output->html .= $this->html->overview();		
	}

}