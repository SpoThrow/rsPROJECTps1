<?php
/**
 * @ Application : 		Custom Moderator Team Page v2.0.0
 * @ Last Updated : 	June 13th, 2012 
 * @ Author :			Michael S. Edwards
 * @ Copyright :		(c) 2011 Coding Jungle
 * @ Link	 :			http://www.codingjungle.com/
 */
class admin_cmtp_ajax_bugReport extends ipsAjaxCommand
{
	/**
	 * HTML library
	 *
	 * @access	public
	 * @var		object
	 */
	public $html;
	
	public function doExecute( ipsRegistry $registry )
	{

		//-----------------------------------------
		// What to do?
		//-----------------------------------------
	
		switch( $this->request['do'] )
		{
			case 'startBugReport':
				$this->_startBugReport();
				break;
			case 'BugsReported':
				$this->_BugsReported();
				break;
			case 'DeleteBug':
				$this->_deleteBugReport();
				break;
		}
	}
	
	public function _startBugReport(){
		$msg = $_POST['msg'];
		$contact = $this->request['contact'];
		
		$content = $this->registry->getClass('cmtp')->parseIt($msg,"display");

			
				$message .= "User: ".$this->memberData['name']."<br>";
				$message .= "Contact's Email: ".$contact."<br>";
				$message .= "Site: ".$this->settings['board_url']."<br>";
				$message .= "App Version: ".$this->caches['app_cache']['cmtp']['app_version'];
				$message .= "IPB Version: ".IPB_VERSION."<br>";
				$message .= "Bug Report: <br><br>".$msg;
				$body 	  = $this->registry->getClass('cmtp')->parseIt($message,"display");
				IPSText::getTextClass('email')->setHtmlEmail(TRUE);
				IPSText::getTextClass('email')->message = $body;										
				IPSText::getTextClass('email')->subject = "Bug Report From: ".$this->settings['board_url'];
				IPSText::getTextClass('email')->to      = "bugreports@codingjungle.com	";
				IPSText::getTextClass('email')->sendMail();
				
				/* Init some vars */
				$addbug = array();

				$content = $this->registry->getClass('cmtp')->parseIt($msg,"db");
				
				/* Lets build an array to feed to the database */
				$addbug['bugs_submitter']  	= addslashes($this->memberData['member_id']);
				$addbug['bugs_contact']		= $contact;
				$addbug['bugs_report']		= $content;
				$addbug['bugs_date']		= time();
				$addbug['bugs_sent']		= "0";

				/* Lets insert the array and build a bug report */
				$this->DB->insert(
				"cmtp_bugs",
				$addbug
				);		
	}

	public function _BugsReported(){
		$bugs = $this->registry->getClass('cmtp')->BugReportDisplay();
		$this->returnHtml($this->registry->output->loadTemplate( 'cp_skin_overview' )->BugReportDisplay($bugs));
	}
	
	public function _deleteBugReport()
	{
		if(!$this->request['id'])
		{
			exit;
		}
		$this->DB->delete( "cmtp_bugs", "bugs_id=" . intval( $this->request['id'] ) );
		$this->_View("1");
	}
}	