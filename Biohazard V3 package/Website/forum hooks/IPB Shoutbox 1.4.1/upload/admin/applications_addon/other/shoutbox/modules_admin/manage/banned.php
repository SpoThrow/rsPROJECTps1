<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

if ( !defined( 'IN_ACP' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

class admin_shoutbox_manage_banned extends ipsCommand 
{
	public $html;
	
	public function doExecute( ipsRegistry $registry )
	{
		/* Load Skin and Lang */
		$this->html               = $this->registry->output->loadTemplate( 'cp_skin_banned' );
		$this->html->form_code    = 'module=manage&amp;section=banned';
		$this->html->form_code_js = 'module=manage&section=banned';
		
		switch( $this->request['do'] )
		{
			case 'ban':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_add_ban' );
				$this->doBan();
			break;
			
			case 'unban':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_remove_ban' );
				$this->doUnban();
			break;
			
			default:
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_view_banned' );
				$this->listBanned();
			break;
		}
		
		/* Output */
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();
	}
	
	private function listBanned()
	{
		/* Navigation */
		$this->registry->output->extra_nav[] = array( $this->settings['base_url'].$this->html->form_code, $this->lang->words['ban_list_list'] );
		
		/* Init some vars */
		$start   = intval($this->request['st']);
		$perPage = 20;
		$members = array();
		
		/* Get n° of banned members */
		$count = $this->DB->buildAndFetch( array( 'select' => 'count(member_id) as total',
												  'from'   => 'members',
												  'where'  => "members_cache LIKE '%shoutbox_banned\";i:1%'"
										  )		 );
		
		/* Sort pagination */
		$pages = $this->registry->output->generatePagination( array( 'totalItems'        => $count['total'],
																	 'itemsPerPage'      => $perPage,
																	 'currentStartValue' => $start,
																	 'baseUrl'           => $this->settings['base_url'].$this->html->form_code
															 )		);
		
		/* sort out the data! :D */
		if ( $count['total'] )
		{
			$this->DB->build( array( 'select' => 'member_id, members_display_name, member_group_id',
									 'from'   => 'members',
									 'where'  => "members_cache LIKE '%shoutbox_banned\";i:1%'",
									 'order'  => 'members_display_name ASC',
									 'limit'  => array( $start, $perPage )
							 )		);
			$this->DB->execute();

			while ( $row = $this->DB->fetch() )
			{
				$row['members_display_name'] = IPSMember::makeNameFormatted( $row['members_display_name'], $row['member_group_id'] );
				$row['members_display_name'] = IPSMember::makeProfileLink( $row['members_display_name'], $row['member_id'] );
				
				$members[] = $row;
			}
		}
		
		/* Add to Output */
		$this->registry->output->html .= $this->html->bannedListView( $members, $pages );
	}
	
	private function doBan()
	{
		/* Check if we have the member */
		$member = $this->DB->buildAndFetch( array( 'select' => 'member_id, members_display_name, members_cache',
												   'from'   => 'members',
												   'where'  => "members_l_display_name='".$this->DB->addSlashes( strtolower( trim($this->request['member_name']) ) )."'"
										   )	  );
		
		/* Check for errors */
		if ( !$member['member_id'] )
		{
			$this->registry->output->showError( 'error_member_not_found' );
		}
		
		if ( $member['member_id'] == $this->memberData['member_id'] )
		{
			$this->registry->output->showError( 'error_ban_yourself' );
		}
		
		$member['_cache'] = IPSMember::unpackMemberCache( $member['members_cache'] );
		
		if ( $member['_cache']['shoutbox_banned'] )
		{
			$this->registry->output->showError( 'error_already_banned' );
		}
		
		/* Ban the member */
		IPSMember::packMemberCache( $member['member_id'], array( 'shoutbox_banned' => 1 ), $member['_cache'] );

		/* Save a log and redirect */
		$this->registry->getClass('adminFunctions')->saveAdminLog( sprintf( $this->lang->words['ban_admin_log'], $member['members_display_name'], $member['member_id'] ) );
		
		$this->registry->output->global_message = sprintf( $this->lang->words['ban_done_message'], $member['members_display_name'] );
			
		$this->listBanned();
	}
	
	private function doUnban()
	{
		/* Check if we have the member */
		$member = $this->DB->buildAndFetch( array( 'select' => 'member_id, members_display_name, members_cache',
												   'from'   => 'members',
												   'where'  => 'member_id='.intval($this->request['mid'])
										   )	  );

		/* Check for errors */
		if ( !$member['member_id'] )
		{
			$this->registry->output->showError( 'error_member_not_found' );
		}
		
		if ( $member['member_id'] == $this->memberData['member_id'] )
		{
			$this->registry->output->showError( 'error_ban_undo_yourself' );
		}
		
		$member['_cache'] = IPSMember::unpackMemberCache( $member['members_cache'] );

		if ( !$member['_cache']['shoutbox_banned'] )
		{
			$this->registry->output->showError( 'error_ban_not_banned' );
		}
		
		/* Remove the ban from the member */
		unset($member['_cache']['shoutbox_banned']);
		
		$this->DB->update( 'members', array( 'members_cache' => serialize($member['_cache']) ), 'member_id='.$member['member_id'] );

		/* Save a log and redirect */
		$this->registry->getClass('adminFunctions')->saveAdminLog( sprintf( $this->lang->words['ban_undo_admin_log'], $member['members_display_name'], $member['member_id'] ) );
		
		$this->registry->output->global_message = sprintf( $this->lang->words['ban_undo_done_message'], $member['members_display_name'] );
		
		$this->listBanned();
	}
}