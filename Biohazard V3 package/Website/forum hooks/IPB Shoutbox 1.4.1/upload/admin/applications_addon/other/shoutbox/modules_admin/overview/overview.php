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

class admin_shoutbox_overview_overview extends ipsCommand 
{
	public $html;
	
	public function doExecute( ipsRegistry $registry )
	{
		/* Load Skin and Lang */
		$this->html               = $this->registry->output->loadTemplate( 'cp_skin_overview' );
		$this->html->form_code    = '&amp;module=overview&amp;section=overview';
		$this->html->form_code_js = '&module=overview&section=overview';
		
		/* Check permissions */
		$this->registry->class_permissions->checkPermissionAutoMsg( 'shoutbox_view_overview' );
		
		/* Setup some variables */
		$temp  = array();
		$stats = array( 'total'      => $this->lang->formatNumber( $this->caches['shoutbox_shouts']['total'] ),
						'topMember'  => '--',
						'topShouts'  => 0,
						'lastName'   => '--',
						'lastDate'   => 0,
						'moderators' => count($this->caches['shoutbox_mods']['groups']) + count($this->caches['shoutbox_mods']['members']),
						'banned'     => 0
						);
		
		/* Get banned members count */
		$temp = $this->DB->buildAndFetch( array( 'select' => 'COUNT(member_id) as count', 'from' => 'members', 'where' => "members_cache LIKE '%shoutbox_banned\";i:1%'" ) );
		
		$stats['banned'] = $temp['count'];
		
		/* We have some shouts? */
		if ( $this->caches['shoutbox_shouts']['total'] > 0 )
		{
			$temp = $this->DB->buildAndFetch( array( 'select' => 'COUNT(s_mid) AS shouts, s_mid',
													 'from'   => 'shoutbox_shouts',
													 'where'  => 's_mid > 0',
													 'group'  => 's_mid',
													 'order'  => 'shouts DESC',
													 'limit'  => array( 0, 1 ),
											 )		);
			
			/* Save data in our main array */
			$stats['topMember'] = intval($temp['s_mid']);
			$stats['topShouts'] = intval($temp['shouts']);
			
			/* Get latest shout */
			$temp = array();
			$temp = array_shift($this->caches['shoutbox_shouts']['shouts']);
			
			$stats['lastDate'] = $this->lang->getDate( $temp['s_date'], 'LONG');
			
			// Get top shouter and last shout member info
			$this->DB->build( array( 'select' => 'member_id, members_display_name, member_group_id',
									 'from'   => 'members',
									 'where'  => 'member_id IN ('.$stats['topMember'].','.intval($temp['s_mid']).')'
							 )		);
			$this->DB->execute();
			
			while( $mem = $this->DB->fetch() )
			{
				if ( $mem['member_id'] == $stats['topMember'] )
				{
					$stats['topMember'] = "<a target='_blank' href='{$this->registry->output->buildUrl( $this->settings['board_url'] . '/index.php?showuser=' . $mem['member_id'], 'none' )}'>" . IPSMember::makeNameFormatted( $mem['members_display_name'], $mem['member_group_id'] ) . "</a>";
				}
				
				if ( $mem['member_id'] == $temp['s_mid'] )
				{
					$stats['lastName'] = "<a target='_blank' href='{$this->registry->output->buildUrl( $this->settings['board_url'] . '/index.php?showuser=' . $mem['member_id'], 'none' )}'>" . IPSMember::makeNameFormatted( $mem['members_display_name'], $mem['member_group_id'] ) . "</a>";
				}
			}
		}
		
		/* Setup general informations */
		$stats['online'] = $this->settings['shoutbox_online'] ? 'tick' : 'cross';
		
		/* Get upgrade history */
		$upgradeRows = array();
		
		$this->DB->build( array( 'select' => 'upgrade_version_id, upgrade_version_human, upgrade_date',
								 'from'   => 'upgrade_history',
								 'where'  => "upgrade_app='shoutbox'",
								 'order'  => 'upgrade_version_id DESC',
								 'limit'  => array( 0, 5 )
						 )		);
		$this->DB->execute();
		
		while( $row = $this->DB->fetch() )
		{
			$row['_date'] = $this->lang->getDate( $row['upgrade_date'], 'SHORT' );
			
			$upgradeRows[] = $row;
		}
		
		/* Get hooks file for update check */
		$classToLoad = IPSLib::loadActionOverloader( IPSLib::getAppDir( 'core' ) . '/modules_admin/applications/hooks.php', 'admin_core_applications_hooks' );
		$hooksClass = new $classToLoad();
		$hooksClass->makeRegistryShortcuts( $this->registry );
		
		/* Check for update */
		$update = $hooksClass->_updateAvailable( $this->caches['app_cache'][ IPS_APP_COMPONENT ]['app_update_check'], $this->caches['app_cache'][ IPS_APP_COMPONENT ]['app_long_version'] );
		$update[2] = $this->caches['app_cache'][ IPS_APP_COMPONENT ]['app_website'];
		
		/* Add to Output */
		$this->registry->output->html .= $this->html->shoutboxOverviewIndex( $stats, $upgradeRows, $update );
		
		/* Output */
		$this->registry->output->html_main .= $this->registry->output->global_template->global_frame_wrapper();
		$this->registry->output->sendOutput();
	}
}