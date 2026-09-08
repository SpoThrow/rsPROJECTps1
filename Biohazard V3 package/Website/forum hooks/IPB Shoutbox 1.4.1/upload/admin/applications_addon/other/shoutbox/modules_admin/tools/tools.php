<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

if ( !defined('IN_ACP') )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

class admin_shoutbox_tools_tools extends ipsCommand 
{
	public $html;
	
	public function doExecute( ipsRegistry $registry )
	{
		/* Load Skin and Lang */
		$this->html               = $this->registry->output->loadTemplate( 'cp_skin_tools' );
		$this->html->form_code    = 'module=tools&amp;section=tools';
		$this->html->form_code_js = 'module=tools&section=tools';
		
		switch( $this->request['do'] )
		{
			case 'rebuildCache':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_tools_rebuild' );
				$this->rebuildCaches();
			break;
			
			case 'resetPrefs':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_tools_prefs' );
				$this->resetPreferences();
			break;
			
			case 'emptyTable':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_tools_empty' );
				$this->emptyTable();
			break;
			
			default:
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_tools_view' );
				$this->toolsList();
			break;
		}
		
		/* Output */
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();
	}
	
	private function toolsList()
	{
		/* Navigation */
		$this->registry->output->extra_nav[] = array( $this->settings['base_url'].$this->html->form_code, $this->lang->words['tls_list'] );
		
		/* Init some vars */
		$formData   = array();
		$cachesList = array( 0 => array( 'all'   , $this->lang->words['tls_cache_all']    ),
							 1 => array( 'shouts', $this->lang->words['tls_cache_shouts'] ),
							 2 => array( 'mods'  , $this->lang->words['tls_cache_mods']   )
							);
		
		/* Setup forms fields */
		$formData['caches'] = $this->registry->output->formDropdown( 'cache', $cachesList, 'all' );
		$formData['prefs']  = $this->registry->output->formSimpleInput( 'perRun', 100, 4 );
		
		//TODO: Add a check for security key on each tool
		
		/* Add to Output */
		$this->registry->output->html .= $this->html->toolsList( $formData );
	}
	
	private function rebuildCaches()
	{
		switch( $this->request['cache'] )
		{
			case 'shouts':
				$this->registry->getClass('shoutboxLibrary')->recacheShouts( 'recount', false );
				
				$this->registry->getClass('adminFunctions')->saveAdminLog( $this->lang->words['tls_shouts_recached'] );
				$this->registry->output->global_message = $this->lang->words['tls_shouts_message'];
				break;
			case 'mods':
				$this->registry->getClass('shoutboxLibrary')->recacheModerators();
				
				$this->registry->getClass('adminFunctions')->saveAdminLog( $this->lang->words['tls_mods_recached'] );
				$this->registry->output->global_message = $this->lang->words['tls_mods_message'];
				break;
			case 'all':
				$this->registry->getClass('shoutboxLibrary')->recacheShouts( 'recount', false );
				$this->registry->getClass('shoutboxLibrary')->recacheModerators();
				
				$this->registry->getClass('adminFunctions')->saveAdminLog( $this->lang->words['tls_shouts_recached']."<br />".$this->lang->words['tls_mods_recached'] );
				$this->registry->output->global_message = $this->lang->words['tls_shouts_message']."<br/>".$this->lang->words['tls_mods_message'];
				break;
			default:
				$this->registry->output->global_message = "<strong>{$this->lang->words['tls_invalid_cache']}</strong>";
		}
		
		$this->toolsList();
	}
	
	private function resetPreferences()
	{
		/* Init some vars */
		$done  = 0;
		$start = ( intval($this->request['st']) >= 0 ) ? intval($this->request['st']) : 0;
		$end   = intval($this->request['perRun']) ? intval($this->request['perRun']) : 100;
		$dis   = $end + $start;
		
		/* Got more members? */
		$tmp = $this->DB->buildAndFetch( array( 'select' => 'member_id', 'from' => 'members', 'limit' => array( $dis, 1 ) ) );
		$max = intval($tmp['member_id']);
		
		//-----------------------------------------
		// Load needed data & Process
		//-----------------------------------------
		$this->DB->build( array( 'select' => 'member_id, members_cache',
								 'from'   => 'members',
								 'limit'  => array( $start, $end )
						 )		);
		$outer = $this->DB->execute();
		
		while( $row = $this->DB->fetch($outer) )
		{
			$row['_cache'] = IPSMember::unpackMemberCache( $row['members_cache'] );
			
			if ( isset($row['_cache']['shoutbox_prefs']) )
			{
				/* Remove the ban from the member */
				unset($row['_cache']['shoutbox_prefs']);
				
				$this->DB->update( 'members', array( 'members_cache' => serialize($row['_cache']) ), 'member_id='.$row['member_id'] );
			}
			
			$done++;
		}
		
		/* Are we done with the reset? :O */
		if ( !$done and !$max )
		{
		 	/* Redirect */
			$this->registry->output->redirect( $this->settings['base_url'] . $this->html->form_code, $this->lang->words['tls_reset_prefs_done'], 2 );
		}
		else
		{
			/* More members */
			$this->registry->output->html .= $this->registry->output->global_template->temporaryRedirect( $this->settings['base_url'] . $this->html->form_code . "&do=resetPrefs&perRun={$end}&st={$dis}", sprintf( $this->lang->words['tls_reset_prefs_going'], $dis ) );
		}
	}
	
	private function emptyTable()
	{
		/* Navigation */
		$this->registry->output->extra_nav[] = array( '', $this->lang->words['tls_empty_confirm'] );
		
		/* Confirmed? */
		if ( isset($this->request['confirm']) && $this->request['confirm'] == 1 )
		{
			$this->DB->delete( 'shoutbox_shouts' );
			
			/* Rebuild also the cache! */
			$this->registry->getClass('shoutboxLibrary')->recacheShouts( 'recount', false );
			
			/* Save log and redirect */
			$this->registry->getClass('adminFunctions')->saveAdminLog( $this->lang->words['tls_table_emptied'] );
			$this->registry->output->redirect( $this->settings['base_url'].$this->html->form_code, $this->lang->words['tls_empty_admin_mesg'] );
		}
		else
		{
			/* Display confirmation screen =O! */
			$this->registry->output->html .= $this->html->emptyTableConfirmation();
		}
	}
}