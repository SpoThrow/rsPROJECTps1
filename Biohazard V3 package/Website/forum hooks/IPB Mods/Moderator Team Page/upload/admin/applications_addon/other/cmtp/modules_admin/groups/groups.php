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
 
class admin_cmtp_groups_groups extends ipsCommand
{

	public function doExecute( ipsRegistry $registry )
	{

		$this->registry->getClass('class_permissions')->checkForAppAccess( 'cmtp');

		$this->registry->getClass('class_permissions')->checkForModuleAccess( 'cmtp', 'groups' );
				
		$this->html = $this->registry->output->loadTemplate( 'cp_skin_groups' );
		
		//BEGONE breadcrumb
		$this->registry->output->ignoreCoreNav = TRUE;
		
		// Lets build the new breadcrumb
		$this->registry->output->extra_nav[] = array( '', IPSLib::getAppTitle( 'cmtp' ) );
	
		$this->registry->output->extra_nav[] = array( $this->settings['base_url']."module=groups&amp;section=groups", "{$this->lang->words['cmtp_php_mainLink']}" );	


		/* Which do */
		switch( $this->request['do'] )
		{
			case "recache":
				$this->recache();
				/* Add to Output */
				$this->View();
				/* Display Message */
				$this->registry->output->global_message = "Recache Complete";
				break;
			default:
				$this->View();
				break;
		}
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		
		$this->registry->output->sendOutput();	

	}
	private function recache(){
		$this->registry->getclass("cmtp")->cacheGroups();
		$this->registry->getclass("cmtp")->cacheMembers();

	}
	private function View()
	{

		$this->registry->output->html .= $this->html->MainDisplay();

	}

}