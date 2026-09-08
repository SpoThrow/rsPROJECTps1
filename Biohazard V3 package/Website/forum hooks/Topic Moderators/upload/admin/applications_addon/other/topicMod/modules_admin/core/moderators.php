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


class admin_topicMod_core_moderators extends ipsCommand
{
	private $modPerms 	= array( 'close_topic', 'open_topic', 'edit_topic', 'pin_topic', 'unpin_topic', 'edit_post', 'post_q' );
	
	public function doExecute( ipsRegistry $registry )
	{
		/* Load Skin and Lang */
		$this->html               = $this->registry->output->loadTemplate( 'cp_skin_moderators' );
		$this->html->form_code    = 'module=core&amp;section=moderators';
		$this->html->form_code_js = '&module=core&section=moderators';
		
		
		$this->lang->loadLanguageFile( array( 'admin_forums' ), 'forums' );
		$this->lang->loadLanguageFile( array( 'admin_topicMod' ) );
		
		switch( $this->request['do'] )
		{
			default:
			case 'listModerators':
				$this->_listModerators();
				break;
				
			case 'newModerator':
				$this->registry->getClass('class_permissions')->checkPermissionAutoMsg( 'topicMod_add_moderator' );
				$this->_showForm( 'new' );
				break;
				
			case 'editModerator':
				$this->registry->getClass('class_permissions')->checkPermissionAutoMsg( 'topicMod_add_moderator' );
				$this->_showForm( 'edit' );
				break;
				
			case 'saveModerator':
				$this->registry->getClass('class_permissions')->checkPermissionAutoMsg( 'topicMod_add_moderator' );
				$this->_saveModerator();
				break;
				
			case 'deleteModerator':
				$this->registry->getClass('class_permissions')->checkPermissionAutoMsg( 'topicMod_delete_moderator' );
				$this->_deleteModerator();
				break;				
				
			case 'rebuildCache':
				$this->rebuildCache();
				break;			
		}
		
		/* Output */
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();
	}
	
	
	protected function _listModerators()
	{
		// A set of default joins, to get some useful info
		$addJoinMembers	= array( 	'select' 	=> 'm.members_display_name, m.member_group_id',
									'from'		=> array( 'members' => 'm' ),
									'where'		=> 'm.member_id=tm.member_id',
									'type'		=> 'left',
								);

		$addJoinTopics	= array(	'select'	=> 't.title as topicTitle, t.title_seo, t.tid',
									'from'		=> array( 'topics' => 't' ),
									'where'		=> 't.tid=tm.topic_id',
									'type'		=> 'left',
								);
		$addJoinGroups = array(		'select'	=> 'g.g_title, g.g_id',
									'from'		=> array( 'groups' => 'g' ),
									'where'		=> 'g.g_id=tm.group_id',
									'type'		=> 'left',
								);
		
		$this->DB->build( array( 	'select'	=> 'tm.*',
									'from'		=> array( 'topic_moderators' => 'tm' ),
									'add_join'	=> array( 	0 => $addJoinMembers,
															1 => $addJoinTopics, 
															2 => $addJoinGroups,
														),
						)		);
		$this->DB->execute();
		
		
		$moderators = array();
		
		// Got results?
		if( $this->DB->getTotalRows() > 0 )
		{
			while( $r = $this->DB->fetch() )
			{	
				$bwOptions = IPSBWOptions::thaw( $r['mod_bitoptions'], 'moderators', 'topicMod' );
				
				if ( is_array( $bwOptions ) )
				{
					$r = array_merge( $r, $bwOptions );
				}
				
				$moderators[ $r['id'] ] = $r;
				
				/* It's a group */
				if ( $r['group_id'] )
				{
					$moderators[ $r['id'] ]['memberName'] 	= IPSMember::makeNameFormatted( $r['g_title'], $r['g_id'] );
					$moderators[ $r['id'] ]['prefix']		= "{$this->settings['skin_acp_url']}/images/icons/group.png";
				}
				else
				{
					$moderators[ $r['id'] ]['memberName'] 	= IPSMember::makeNameFormatted( $r['members_display_name'], $r['member_group_id'] );
					$moderators[ $r['id'] ]['memberName'] 	= IPSMember::makeProfileLink( $moderators[ $r['id'] ]['memberName'], $r['member_id'] );
					$moderators[ $r['id'] ]['prefix']		= "{$this->settings['skin_acp_url']}/images/icons/user.png";
				}
				
				if ( $r['moderate_own'] )
				{
					$moderators[ $r['id'] ]['topicTitle']	= "Moderates own topics";
				}
				else
				{
					if ( $r['tid'] )
					{
						$moderators[ $r['id'] ]['topicTitle'] 	= "<a href='" . $this->registry->getClass('output')->formatUrl( ipsRegistry::getClass('output')->buildUrl( "showtopic=".$r['tid'], 'public' ), $r['title_seo'], 'showtopic' ) . "'>{$r['topicTitle']}</a>";
					}
					else
					{
						$moderators[ $r['id'] ]['topicTitle']	= "<span class='ipsBadge ipsBadge_red'>Topic does not exists</span>";
					}
				}
				
				foreach( $this->modPerms as $perm )
				{
					$moderators[ $r['id'] ][ $perm ] = $this->_buildPermImages( $r[ $perm ] );
				}
				
				$moderators[ $r['id'] ]['gbw_soft_delete'] = $this->_buildPermImages( $r['gbw_soft_delete'] );
			}
		}

				
		$this->registry->output->html .= $this->html->listModerators( $moderators );
		return;
		
		
	}
	
	protected function _showform( $type )
	{
		$formData = array();
		
		if ( $type == 'edit' )
		{
			if ( ! intval( $this->request['id'] ) )
			{
				$this->registry->output->global_message = "Invalid moderator ID";
				$this->_listModerators();
				return;
			}
			
			/* Load various data here */
			$this->DB->build( array( 	'select' 	=> 'tm.*',
										'from'		=> array( 'topic_moderators' => 'tm' ),
										'where'		=> 'tm.id=' . $this->request['id'],
										'add_join'	=> array(
															array( 	'select'	=> 'm.members_display_name',
																	'from'		=> array( 'members' => 'm' ),
																	'where'		=> 'tm.member_id = m.member_id',
																),
															array( 	'select'	=> 't.*',
																	'from'		=> array( 'topics' => 't' ),
																	'where'		=> 't.tid=tm.topic_id',
																	'type'		=> 'left',
																),
															),
							)	);
							
			$this->DB->execute();
			
			if ( $this->DB->getTotalRows() == 1 )
			{
				$formData = $this->DB->fetch();
				$formData['topicLink'] = "Topic Title: <a href='" . ipsRegistry::getClass('output')->formatUrl( ipsRegistry::getClass('output')->buildUrl( "showtopic=".$formData['tid'], 'public' ), $formData['title_seo'], 'showtopic' ) . "'>{$formData['title']}</a>";
			}

			/* BW Options */
			$_tmp = IPSBWOptions::thaw( $formData['mod_bitoptions'], 'moderators', 'topicMod' );

			if ( count( $_tmp ) )
			{
				foreach( $_tmp as $k => $v )
				{ 
					$formData[ $k ] = $v;
				}
			}
		}
		
		//-----------------------------------------
		// Member groups
		//-----------------------------------------
			
		$this->DB->build( array( 'select' => 'g_id, g_title', 'from' => 'groups', 'order' => "g_title" ) );
		$this->DB->execute();
		
		$formData['member_groups'] = array();
		$formData['member_groups'][0] = array( 0, 'Select a group' );
		
		while ( $r = $this->DB->fetch() )
		{
		 	$formData['member_groups'][ $r['g_id'] ] = array( $r['g_id'], $r['g_title'] );
		}
		
		$this->registry->output->extra_nav[]	= array( '', $this->lang->words['mod_' . $type ] );
		$this->registry->output->html .= $this->html->showForm( $formData, $type );
	}
	
	protected function _saveModerator()
	{
		if ( $id = intval( $this->request['id'] ) )
		{
			$this->DB->buildAndFetch( array( 'select' => '*', 'from' => 'topic_moderators', 'where' => 'id=' . $id ) );
			
			if ( $this->DB->getTotalRows() )
			{
				$dbData = array();
				
				foreach( $this->modPerms as $perm )
				{
					$dbData[ $perm ] = ( isset( $this->request[ $perm ] ) ) ? $this->request[ $perm ] : 0;
				}
				
				if ( $this->request['moderate_own'] == 1 )
				{
					$dbData['topic_id'] 	= 0;
					$dbData['moderate_own'] = 1;
					$dbData['forums']		= $this->_getSelectedForums();
				}
				else if ( intval( $this->request['topic_id'] ) )
				{
					$dbData['topic_id'] = $this->request['topic_id'];
					$dbData['moderate_own'] = 0;
				}
				else
				{
					$this->registry->output->global_error = "Invalid topic ID";
					$in_error = 1;
				}
				
				
				if ( ! empty( $this->request['memberName'] ) )
				{
					$member = $this->DB->buildAndFetch( array(	'select' 	=> 'member_id',
																'from'		=> 'members',
																'where'		=> 'members_l_display_name="' . strtolower( $this->request['memberName'] ) . '"',
														)		);
					if ( ! is_array( $member ) OR count( $member ) == 0 )
					{
						$this->registry->output->global_error = "No such member";
						$in_error = 1;
					}
					
					$dbData['member_id'] = $member['member_id'];
					$dbData['group_id'] = 0;
				}
				else if ( isset( $this->request['group_id'] ) AND intval( $this->request['group_id'] ) != 0 )
				{
					$dbData['group_id'] = $this->request['group_id'];
					$dbData['member_id'] = 0;
				}
				else
				{
					$this->registry->output->global_error = "Invalid or empty member name";
					$in_error = 1;
				}
				
				/* Any errors? */
				if ( $in_error )
				{
					$dbData['members_display_name'] = $this->request['memberName'];
					$dbData['id']					= $id;
					
					$this->registry->output->html .= $this->html->showForm( $dbData, 'edit' );
					return;
				}
				
				$dbData['mod_bitoptions'] = IPSBWOptions::freeze( $this->request, 'moderators', 'topicMod' );
				
				$this->DB->update( 'topic_moderators', $dbData, 'id=' . $id );
				
				$this->rebuildCache();
				
				$this->registry->output->global_message = "Moderator edited";
				$this->_listModerators();
				return;
			}
		}
		else
		{
			$dbData = array();
			
			foreach( $this->modPerms as $perm )
			{
				$dbData[ $perm ] = ( isset( $this->request[ $perm ] ) ) ? $this->request[ $perm ] : 0;
			}
			
			if ( $this->request['moderate_own'] == 1 )
			{
				$dbData['topic_id'] 	= 0;
				$dbData['moderate_own'] = 1;
				$dbData['forums']		= $this->_getSelectedForums();
			}
			else if ( intval( $this->request['topic_id'] ) )
			{
				$dbData['topic_id'] = $this->request['topic_id'];
				$dbData['moderate_own'] = 0;
			}
			else
			{
				$this->registry->output->global_error = "Invalid topic ID";
				$in_error = 1;
			}

			if ( ! empty( $this->request['memberName'] ) )
			{
				$member = $this->DB->buildAndFetch( array(	'select' 	=> 'member_id',
															'from'		=> 'members',
															'where'		=> 'members_l_display_name="' . strtolower( $this->request['memberName'] ) . '"',
													)		);
													
				if ( ! is_array( $member ) OR count( $member ) == 0 )
				{
					$this->registry->output->global_error = "No such member";
					$in_error = 1;
				}
				
				$dbData['member_id'] = $member['member_id'];
				$dbData['group_id'] = 0;
			}
			else if ( isset( $this->request['group_id'] ) AND intval( $this->request['group_id'] ) != 0 )
			{
				$dbData['group_id'] = $this->request['group_id'];
				$dbData['member_id'] = 0;
			}
			else
			{
				$this->registry->output->global_error = "Invalid or empty member name";
				$in_error = 1;
			}
			
			if ( $in_error )
			{
				$dbData['members_display_name'] = $this->request['memberName'];
				
				$this->registry->output->html .= $this->html->showForm( $dbData, 'new' );
				return;
			}
			
			
			$dbData['mod_bitoptions'] = IPSBWOptions::freeze( $this->request, 'moderators', 'topicMod' );
			
			$this->DB->insert( 'topic_moderators', $dbData );
			
			$this->rebuildCache();
			
			$this->registry->output->global_message = "Moderator added";
			$this->_listModerators();
			return;
		}
	}
	
	protected function _deleteModerator()
	{
		$id = intval( $this->request['id'] );
		
		if ( $id )
		{
			$this->DB->delete( 'topic_moderators', 'id=' . $id );
			
			$this->registry->output->global_message = "Moderator deleted";
			$this->rebuildCache();
			
			$this->_listModerators();
			return;
		}
		else
		{
			$this->registry->output->global_error = "Invalid ID";
			
			$this->_listModerators();
			return;
		}
	}
	
	public function rebuildCache()
	{
		$toCache = array();
		
		$this->DB->build( array( 	'select'	=> 'tm.*, tm.id as mid',
									'from'		=> array( 'topic_moderators' => 'tm' ),
									'add_join'	=> array(
															array( 	'select' 	=> 'm.members_display_name, m.members_seo_name as seoname, m.member_group_id',
																	'from' 		=> array( 'members' => 'm' ),
																	'where' 	=> 'tm.member_id=m.member_id',
																	'type'		=> 'left',
															),
															array(	'select'	=> 'g.g_title, g.g_id',
																	'from'		=> array( 'groups' => 'g' ),
																	'where'		=> 'g.g_id=tm.group_id',
																	'type'		=> 'left',
															)
														)
						)		);
		$this->DB->execute();
		
		if( $this->DB->getTotalRows() > 0 )
		{
			while( $r = $this->DB->fetch() )
			{
				/* Unpack bitwise fields */
				$_tmp = IPSBWOptions::thaw( $r['mod_bitoptions'], 'moderators', 'topicMod' );
	
				if ( count( $_tmp ) )
				{
					foreach( $_tmp as $k => $v )
					{ 
						$r[ $k ] = $v;
					}
				}
				if ( $r['moderate_own'] )
				{
					$r['forums'] = ( empty( $r['forums'] ) OR is_null( $r['forums'] ) ) ? '*' : $r['forums'];
					
					if ( $r['group_id'] )
					{
						unset( $r['members_display_name'], $r['seoname'], $r['member_group_id'] );
						$toCache['moderate_own']['group'][ $r['group_id'] ][] 	= $r;
					}
					else
					{
						unset( $r['g_title'], $r['g_id'] );
						$toCache['moderate_own']['member'][ $r['member_id'] ][] 	= $r;
					}
				}
				else if ( $r['group_id'] )
				{
					unset( $r['members_display_name'], $r['seoname'], $r['member_group_id'] );
					$toCache['group'][ $r['topic_id'] ][ $r['group_id'] ] 	= $r;
				}
				else
				{
					unset( $r['g_title'], $r['g_id'] );
					$toCache['member'][ $r['topic_id'] ][ $r['member_id'] ] 	= $r;
				}
			}
			
		}
		
		$this->cache->setCache( 'topicmod', $toCache, array( 'array' => 1, 'donow' => 1 ) );
	}
	
	protected function _buildPermImages( $value )
	{
		$value = intval( $value );
		
		$image = ( $value > 0 ) ? 'accept.png' : 'cross.png';
		
		return "<img src='{$this->settings['skin_acp_url']}/images/icons/{$image}' alt='{$image}' />";

	}
	
	/**
	 * Get selected forums
	 *
	 * @return	string	Comma separated list of forum ids
	 */ 
    protected function _getSelectedForums()
    {
    	/* INI */
		$forumids = array();
    	
		/* Check for the forums array */
    	if( is_array( $_POST['forums'] )  )
    	{
    		/* Add All Forums */
    		if( in_array( 'all', $_POST['forums'] ) )
    		{
    			return '*';
    		}
    		/* Add selected Forums */
    		else
    		{
				/* Loop through the selected forums */
				foreach( $_POST['forums'] as $l )
				{
					if( $this->registry->class_forums->forum_by_id[ $l ] )
					{
						$forumids[] = intval( $l );
					}
				}
				
				if( ! count( $forumids  ) )
				{
					return;
				}
    		}
		}
		/* Not an array */
		else
		{
			/* All Forums */
			if ( $this->request['forums'] == 'all' )
			{
				return '*';
			}
			else
			{
				/* Anything selected? */
				if( $this->request['forums'] != "" )
				{
					$l = intval( $this->request['forums'] );
					
					/* Single Forum */
					if( $this->registry->class_forums->forum_by_id[ $l ] )
					{
						$forumids[] = intval( $l );
					}
					
					/* Check subs */
					if ( $this->request['searchsubs'] == 1 )
					{
						$children = $this->registry->class_forums->forumsGetChildren( $l );
						
						if( is_array ($children ) and count( $children ) )
						{
							$forumids = array_merge( $forumids, $children );
						}
					}
				}
			}
		}
		
		return implode( ",", $forumids );
    }
}