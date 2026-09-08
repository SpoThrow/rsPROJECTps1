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

class admin_shoutbox_manage_moderators extends ipsCommand
{
	public $html;

	public function doExecute( ipsRegistry $registry )
	{
		/* Load Skin and Lang */
		$this->html               = $this->registry->output->loadTemplate( 'cp_skin_moderators' );
		$this->html->form_code    = 'module=manage&amp;section=moderators';
		$this->html->form_code_js = 'module=manage&section=moderators';

		/* Which do? =O */
		switch( $this->request['do'] )
		{
			case 'addGroup':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_add_moderator' );
				$this->moderatorForm('add', 'group');
			break;

			case 'addMember':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_add_moderator' );
				$this->moderatorForm('add', 'member');
			break;

			case 'doAddGroup':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_add_moderator' );
				$this->moderatorSave('add', 'group');
			break;

			case 'doAddMember':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_add_moderator' );
				$this->moderatorSave('add', 'member');
			break;

			case 'edit':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_add_moderator' );
				$this->moderatorForm('edit');
			break;

			case 'doEdit':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_add_moderator' );
				$this->moderatorSave('edit');
			break;

			case 'delete':
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_delete_moderator' );
				$this->moderatorDelete();
			break;

			default:
				$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_view_moderators' );
				$this->moderatorsList();
			break;
		}

		/* Output */
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();
	}

	private function moderatorsList()
	{
		/* Navigation */
		$this->registry->output->extra_nav[] = array( $this->settings['base_url'].$this->html->form_code, $this->lang->words['mod_list'] );

		/* Init some vars */
		$mods = array();

		/* Get mods from DB */
		$this->DB->build( array( 'select'   => "s.m_id, s.m_type",
								 'from'     => array( 'shoutbox_mods' => 's' ),
								 'order'    => 's.m_type ASC',
								 'add_join' => array( 0 => array( 'select' => 'm.members_display_name, m.member_group_id',
																  'from'   => array( 'members' => 'm' ),
																  'where'  => "s.m_mg_id=m.member_id AND s.m_type='member'",
																  'type'   => 'left' ),
													  1 => array( 'select' => 'g.g_id, g.g_title',
																  'from'   => array( 'groups' => 'g' ),
																  'where'  => "s.m_mg_id=g.g_id AND s.m_type='group'",
																  'type'   => 'left' ) )
						 )		);
		$this->DB->execute();

		/* Parse mods */
		if ( $this->DB->getTotalRows() )
		{
			while ( $row = $this->DB->fetch() )
			{
				$row['_type'] = $row['m_type'] == 'group' ? $this->lang->words['mod_group'] : $this->lang->words['mod_member'];
				$row['_name'] = $row['m_type'] == 'group' ? IPSMember::makeNameFormatted( $row['g_title'], $row['g_id'] ) : IPSMember::makeNameFormatted( $row['members_display_name'], $row['member_group_id'] );

				// Store it! :D
				$mods[] = $row;
			}
		}

		/* Add to Output */
		$this->registry->output->html .= $this->html->moderatorsListView( $mods, $pages );
	}

	private function moderatorForm( $formType='add', $modType='group' )
	{
		/* Init some vars */
		$formData = array();

		/* Adding a moderator? huh */
		if ( $formType == 'add' )
		{
			if ( $modType == 'group' )
			{
				/* Sort out string =D */
				$title  = $this->lang->words['mod_adding_group'];
				$button = $this->lang->words['mod_add_group'];
				$doForm = 'doAddGroup';
				$groups = array();

				//-----------------------------------------
				// Let's exclude those groups from being
				// moderators (Guest, Validating
				//-----------------------------------------
				$exclude = array( intval($this->settings['guest_group']),
								  intval($this->settings['auth_group'])
								 );

				/* Get groups from DB */
				$this->DB->build( array( 'select'   => "g.g_id, g.g_title",
										 'from'     => array( 'groups' => 'g' ),
										 'where'    => "g.g_view_board=1 AND g.g_id NOT IN (".implode(",", $exclude).")",
										 'add_join' => array( 0 => array( 'select' => 's.m_id',
																		  'from'   => array( 'shoutbox_mods' => 's' ),
																		  'where'  => "s.m_mg_id=g.g_id AND s.m_type='group'",
																		  'type'   => 'left' ) )
								 )		);
				$this->DB->execute();

				while ( $row = $this->DB->fetch() )
				{
					/* Exclude groups already assigned */
					if ( $row['m_id'] )
					{
						continue;
					}

					$groups[] = array( $row['g_id'] , $row['g_title'] );
				}

				/* Setup our typeText :D */
				$formData['typeText'] = $this->registry->output->formDropdown( 'm_mg_id', $groups, '', '', '', 'realbutton' );
			}
			else
			{
				/* Sort out string =D */
				$title  = $this->lang->words['mod_adding_member'];
				$button = $this->lang->words['mod_add_member'];
				$doForm  = 'doAddMember';
				$filters =  array( 0 => array( 'name', $this->lang->words['mod_member_display_name'] ),
								   1 => array( 'id'  , $this->lang->words['mod_member_id'] )
								  );

				/* Setup our typeText :D */
				$formData['typeText'] = $this->registry->output->formInput( 'm_mg_id', '', '', 25, 'text', '', 'realbutton' )."&nbsp;".$this->registry->output->formDropdown( 'mtype', $filters, '', '', '', 'realbutton' );
			}
		}
		else
		{
			# Get moderator from DB
			$data = $this->DB->buildAndFetch( array( 'select'   => "s.*",
													 'from'     => array( 'shoutbox_mods' => 's' ),
													 'where'    => 's.m_id='.intval($this->request['id']),
													 'add_join' => array( 0 => array( 'select' => 'm.member_group_id, m.members_display_name',
																					  'from'   => array( 'members' => 'm' ),
																					  'where'  => "s.m_mg_id=m.member_id and s.m_type='member'",
																					  'type'   => 'left'),
																		  1 => array( 'select' => 'g.g_title',
																					  'from'   => array( 'groups' => 'g' ),
																					  'where'  => "s.m_mg_id=g.g_id and s.m_type='group'",
																					  'type'   => 'left')	)
											 )		);

			# Found the ID?
			if ( $data['m_id'] )
			{
				# What we are editing? :O
				$modType = ( $data['m_type'] == 'group' ) ? 'group' : 'member';
			}
			else
			{
				$this->registry->output->showError( 'error_mod_id_not_found' );
			}

			# Set code id for save
			$doForm = 'doEdit&amp;id='.$data['m_id'];

			if ( $modType == 'group' )
			{
				/* Sort out string =D */
				$title  = $this->lang->words['mod_editing_group'];
				$button = $this->lang->words['mod_edit_group'];

				$formData['typeText'] = IPSMember::makeNameFormatted( $data['g_title'], $data['m_mg_id'] );
			}
			else
			{
				/* Sort out string =D */
				$title  = $this->lang->words['mod_editing_member'];
				$button = $this->lang->words['mod_edit_member'];

				$formData['typeText'] = IPSMember::makeNameFormatted( $data['members_display_name'], $data['member_group_id'] );
			}
		}

		/* Setup Navigation */
		$this->registry->output->extra_nav[] = array( '', $title );

		//-----------------------------------------
		// Sort form values
		//-----------------------------------------
		$formData['edit']   = $this->registry->output->formYesNo( 'm_edit_shouts', $data['m_edit_shouts'] );
		$formData['delete'] = $this->registry->output->formYesNo( 'm_delete_shouts', $data['m_delete_shouts'] );
		$formData['prune']  = $this->registry->output->formYesNo( 'm_delete_shouts_user', $data['m_delete_shouts_user'] );
		$formData['ban']    = $this->registry->output->formYesNo( 'm_ban_members', $data['m_ban_members'] );
		$formData['unban']  = $this->registry->output->formYesNo( 'm_unban_members', $data['m_unban_members'] );
		$formData['remove'] = $this->registry->output->formYesNo( 'm_remove_mods', $data['m_remove_mods'] );

		/* Add to Output */
		$this->registry->output->html .= $this->html->moderatorFormView( $title, $button, $doForm, ($modType == 'group' ? $this->lang->words['mod_group'] : $this->lang->words['mod_member']), $formData );
	}

	private function moderatorSave( $formType='add', $modType='group' )
	{
		/* Check for proper values */
		$formType = $formType == 'add' ? 'add' : 'edit';
		$modType = $modType == 'group' ? 'group' : 'member';

		/* Adding a moderator? huh */
		if ( $formType == 'add' )
		{
			if ( $modType == 'group' )
			{
				//-----------------------------------------
				// Let's exclude those groups from being
				// moderators (Guest, Validating, Banned)
				//-----------------------------------------
				$exclude = array( $this->settings['guest_group'],
								  $this->settings['auth_group']
								 );

				/* Check group ID */
				$m_mg_id = intval($this->request['m_mg_id']);

				if ( !$m_mg_id || in_array( $m_mg_id, $exclude ) || !$this->caches['group_cache'][ $m_mg_id ]['g_view_board'] )
				{
					$this->registry->output->showError( 'error_group_id_invalid' );
				}

				/* Check if group exists */
				$check = $this->DB->buildAndFetch( array( 'select' => 'g_id, g_title',
														  'from'   => 'groups',
														  'where'  => 'g_id='.$m_mg_id
												  )		 );

				if ( !$check['g_id'] )
				{
					$this->registry->output->showError( 'error_group_not_found' );
				}
			}
			else
			{
				/* Which type of member search are we using? =O */
				if ( trim($this->request['mtype']) == 'name' )
				{
					$this->request['m_mg_id'] = strtolower( trim($this->request['m_mg_id']) );

					if ( strlen($this->request['m_mg_id']) > 2 )
					{
						$query = "members_l_display_name='".$this->DB->addSlashes($this->request['m_mg_id'])."'";
					}
					else
					{
						$this->registry->output->showError( 'error_member_too_short' );
					}
				}
				else
				{
					$this->request['m_mg_id'] = intval($this->request['m_mg_id']);

					if ( $this->request['m_mg_id'] > 0 )
					{
						$query = "member_id=".$this->request['m_mg_id'];
					}
					else
					{
						$this->registry->output->showError( 'error_member_id_invalid' );
					}
				}

				/* Check if member exists */
				$check = $this->DB->buildAndFetch( array( 'select' => 'member_id, members_display_name',
														  'from'   => 'members',
														  'where'  => $query
												  )		 );

				if ( !$check['member_id'] )
				{
					$this->registry->output->showError( 'error_member_not_found' );
				}

				# Save Member ID
				$m_mg_id = $check['member_id'];
			}

			/* Moderator already in use? */
			$tmp = $this->DB->buildAndFetch( array( 'select' => 'm_id',
													'from'   => 'shoutbox_mods',
													'where'  => "m_mg_id={$m_mg_id} AND m_type='{$modType}'"
											)		);

			if ( $tmp['m_id'] )
			{
				$this->registry->output->showError( sprintf( $this->lang->words['error_already_moderator'], ($modType == 'group' ? $this->lang->words['mod_group'] : $this->lang->words['mod_member']) ) );
			}
		}
		else
		{
			//--------------------------------------------
			// This moderator exists?
			//--------------------------------------------
			$edit = $this->DB->buildAndFetch( array( 'select' => 'm_id, m_type, m_mg_id',
													 'from'   => 'shoutbox_mods',
													 'where'  => 'm_id='.intval($this->request['id'])
											 )		);

			if ( !$edit['m_id'] )
			{
				$this->registry->output->showError( 'error_moderator_not_found' );
			}

			$modType = $edit['m_type'];
			$m_mg_id = $edit['m_mg_id'];
		}

		//--------------------------------------------
		// Save/Insert?
		//--------------------------------------------
		$permissions = array( 'm_edit_shouts'        => intval($this->request['m_edit_shouts']),
							  'm_delete_shouts'      => intval($this->request['m_delete_shouts']),
							  'm_delete_shouts_user' => intval($this->request['m_delete_shouts_user']),
							  'm_ban_members'        => intval($this->request['m_ban_members']),
							  'm_unban_members'      => intval($this->request['m_unban_members']),
							  'm_remove_mods'        => intval($this->request['m_remove_mods'])
							 );

		/* Finally update DB! ;D */
		if ( $formType == 'add' )
		{
			# Insert ID/type values in array
			$permissions['m_mg_id'] = $m_mg_id;
			$permissions['m_type'] = $modType;

			$this->DB->insert( 'shoutbox_mods', $permissions );
			$action = $this->lang->words['mod_added'];
		}
		else
		{
			$this->DB->update( 'shoutbox_mods', $permissions, 'm_id='.$edit['m_id'] );
			$action = $this->lang->words['mod_edited'];
		}

		/* Rebuild mods cache */
		$this->registry->getClass('shoutboxLibrary')->recacheModerators();

		/* Save a log & redirect */
		$logName = $modType == 'group' ? $check['g_title'] : $check['members_display_name'];
		$this->registry->getClass('adminFunctions')->saveAdminLog( sprintf( $this->lang->words['mod_save_admin_log'], $action, ($modType == 'group' ? $this->lang->words['mod_group'] : $this->lang->words['mod_member']), $logName, $m_mg_id ) );

		$this->registry->output->global_message = sprintf( $this->lang->words['mod_save_message'], $action );
		$this->moderatorsList();
	}

	private function moderatorDelete()
	{
		/* Get moderator */
		$data = $this->DB->buildAndFetch( array( 'select'   => "s.m_id, s.m_type, s.m_mg_id",
												 'from'     => array( 'shoutbox_mods' => 's' ),
												 'where'    => 's.m_id='.intval($this->request['id']),
												 'add_join' => array( 0 => array( 'select' => 'm.members_display_name',
																				  'from'   => array( 'members' => 'm' ),
																				  'where'  => "s.m_mg_id=m.member_id AND s.m_type='member'",
																				  'type'   => 'left' ),
																	  1 => array( 'select' => 'g.g_title',
																				  'from'   => array( 'groups' => 'g' ),
																				  'where'  => "s.m_mg_id=g.g_id AND s.m_type='group'",
																				  'type'   => 'left' ) )
										 )		);

		if ( !$data['m_id'] )
		{
			$this->registry->output->showError( 'error_moderator_not_found' );
		}

		/* Delete it :( */
		$this->DB->delete( 'shoutbox_mods', 'm_id='.$data['m_id'] );

		/* Rebuild mods cache */
		$this->registry->getClass('shoutboxLibrary')->recacheModerators();

		/* Save a log & redirect */
		$logName = $data['m_type'] == 'group' ? $data['g_title'] : $data['members_display_name'];
		$this->registry->getClass('adminFunctions')->saveAdminLog( sprintf( $this->lang->words['mod_delete_admin_log'], ($data['m_type'] == 'group' ? $this->lang->words['mod_group'] : $this->lang->words['mod_member']), $logName, $data['m_mg_id'] ) );

		$this->registry->output->global_message = $this->lang->words['mod_delete_message'];
		$this->moderatorsList();
	}
}
