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

class admin_topicMod_ajax_ajax extends ipsAjaxCommand 
{	
	public function doExecute( ipsRegistry $registry )
	{
		
		switch( $this->request['do'] )
		{
			case 'getTopicTitle':
				$this->_getTopicTitle();
				break;
		}
	}
	
	private function _getTopicTitle()
	{
		$id = intval( $this->request['topic_id'] );
		
		if ( $id )
		{
			$topicData = $this->DB->buildAndFetch( array( 'select' => '*', 'from' => 'topics', 'where' => 'tid=' . $id ) );
			
			if( $this->DB->getTotalRows() )
			{
				$topicData['topicLink'] = "<a href='" . ipsRegistry::getClass('output')->formatUrl( ipsRegistry::getClass('output')->buildUrl( "showtopic=".$topicData['tid'], 'public' ), $topicData['title_seo'], 'showtopic' ) . "'>{$topicData['title']}</a>";
				
				$this->returnJsonArray( $topicData );
			}
			else
			{
				$this->returnJsonError( 'No such topic' );
			}
		}
		else
		{
			$this->returnJsonError( 'Invalid ID' );
		}
	}
}