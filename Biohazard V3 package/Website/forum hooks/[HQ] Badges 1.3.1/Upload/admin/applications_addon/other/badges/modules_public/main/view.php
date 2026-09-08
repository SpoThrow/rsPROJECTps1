<?php

if ( !defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

class public_aboutus_main_view extends ipsCommand
{
	public function doExecute( ipsRegistry $registry )
	{
		/* Language */
		$this->lang->loadLanguageFile( array( 'public_main' ), 'aboutus' );
		
		/* Set our parser flags */
		IPSText::getTextClass('bbcode')->parse_html    = 0;
		IPSText::getTextClass('bbcode')->parse_nl2br   = 0;
		IPSText::getTextClass('bbcode')->parse_bbcode  = 1;
		IPSText::getTextClass('bbcode')->parse_smilies = 1;
		
		/* Clean up the message */
		$this->settings['aboutUsText'] = IPSText::getTextClass('bbcode')->preDisplayParse( IPSText::getTextClass('bbcode')->preDbParse( $this->settings['aboutUsText'] ) );
		
		/* Set the title and navigation */
		$this->registry->output->setTitle( IPSLib::getAppTitle('aboutus') );
		$this->registry->output->addNavigation( IPSLib::getAppTitle('aboutus'), 'app=aboutus', "false", 'app=aboutus' );
		
		/* Send to screen */
		$this->registry->output->addContent( $this->registry->output->getTemplate('aboutus')->viewAboutUs() );
		$this->registry->output->sendOutput();
	}
}