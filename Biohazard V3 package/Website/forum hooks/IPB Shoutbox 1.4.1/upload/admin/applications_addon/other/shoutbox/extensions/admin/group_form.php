<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

class admin_group_form__shoutbox implements admin_group_form
{	
	public $tab_name = "";
	
	public function getDisplayContent( $group=array(), $tabsUsed = 2 )
	{
		/* Load skin */
		$this->html = ipsRegistry::getClass('output')->loadTemplate('cp_skin_shoutbox_group_form', 'shoutbox');
		
		/* Load lang */
		ipsRegistry::getClass('class_localization')->loadLanguageFile( array( 'admin_shoutbox' ), 'shoutbox' );
		
		/* Show... */
		return array( 'tabs' => $this->html->acp_group_form_tabs( $group, ( $tabsUsed + 1 ) ), 'content' => $this->html->acp_group_form_main( $group, ( $tabsUsed + 1 ) ), 'tabsUsed' => 1 );
	}
	
	public function getForSave()
	{
		$return = array('g_shoutbox_view'				=> intval( ipsRegistry::$request['g_shoutbox_view'] ),
						'g_shoutbox_use'				=> intval( ipsRegistry::$request['g_shoutbox_use'] ),
						'g_shoutbox_posts_req'			=> intval( ipsRegistry::$request['g_shoutbox_posts_req'] ),
						'g_shoutbox_posts_req_display'	=> intval( ipsRegistry::$request['g_shoutbox_posts_req_display'] ),
						'g_shoutbox_bypass_flood'		=> intval( ipsRegistry::$request['g_shoutbox_bypass_flood'] ),
						'g_shoutbox_edit'				=> intval( ipsRegistry::$request['g_shoutbox_edit'] ),
						'g_shoutbox_delete_own'			=> intval( ipsRegistry::$request['g_shoutbox_delete_own'] ),
						'g_shoutbox_view_archive'		=> intval( ipsRegistry::$request['g_shoutbox_view_archive'] ),
						'g_shoutbox_old_req'            => intval( ipsRegistry::$request['g_shoutbox_old_req'] ),
                        'g_shoutbox_old_req_display'    => intval( ipsRegistry::$request['g_shoutbox_old_req_display'] ),
						);

		return $return;
	}
}