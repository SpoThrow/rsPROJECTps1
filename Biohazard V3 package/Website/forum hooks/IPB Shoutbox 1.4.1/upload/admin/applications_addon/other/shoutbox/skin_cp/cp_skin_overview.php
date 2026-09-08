<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

class cp_skin_overview extends output
{
	public function shoutboxOverviewIndex( $stats, $upgrades, $update )
	{
		/* Update available? */
		$_update = "";
		
		if ( $update[0] )
		{
			$this->registry->class_localization->loadLanguageFile( array( 'admin_applications' ), 'core' );
			$_update = "&nbsp;<span class='ipsBadge badge_purple'>{$this->lang->words['hook_update_available']}</span>";
			
			if ( !empty( $update[1] ) )
			{
				$_update = "<a href='{$update[1]}' target='_blank'>{$_update}</a>";
			}
			else if( !empty( $update[2] ) )
			{
				$_update = "<a href='{$update[2]}' target='_blank'>{$_update}</a>";
			}
		}
		
		$IPBHTML = "";
		//--starthtml--//

		$IPBHTML .= <<<HTML
<div class="section_title">
	<h2>{$this->lang->words['ov_title']}</h2>
</div>
<table class="form_table">
	<tr>
		<td style="width: 50%;" valign="top">
			<div class="acp-box">
				<h3>{$this->lang->words['ov_stats']}</h3>
				<table class="ipsTable">
					<tr>
						<td style="width: 40%;"><strong>{$this->lang->words['total_shouts']}</strong></td>
						<td style="width: 60%;">{$stats['total']}</td>
					</tr>
					<tr>
						<td><strong>{$this->lang->words['top_shouter']}</strong></td>
						<td>{$stats['topMember']} ({$stats['topShouts']})</td>
					</tr>
					<tr>
						<td><strong>{$this->lang->words['latest_shout_by']}</strong></td>
						<td>{$stats['lastName']}</td>
					</tr>
					<tr>
						<td><strong>{$this->lang->words['latest_shout_date']}</strong></td>
						<td>{$stats['lastDate']}</td>
					</tr>
					<tr>
						<td><strong>{$this->lang->words['moderators']}</strong></td>
						<td>{$stats['moderators']}</td>
					</tr>
					<tr>
						<td><strong>{$this->lang->words['banned_members']}</strong></td>
						<td>{$stats['banned']}</td>
					</tr>
				</table>
			</div>
			<br class="clear" />
			<div class="acp-box">
				<h3>{$this->lang->words['ov_upgrade_history']}</h3>
				<table class="ipsTable">
					<tr>
						<th style="width: 60%">{$this->lang->words['ov_version']}</th>
						<th style="width: 40%">{$this->lang->words['upgrade_date']}</th>
					</tr>

HTML;
						
						if ( count( $upgrades ) )
						{
							foreach ( $upgrades as $upgrade )
							{
								$IPBHTML .= <<<HTML
					<tr>
						<td>{$upgrade['upgrade_version_human']} ({$upgrade['upgrade_version_id']})</td>
						<td>{$upgrade['_date']}</td>
					</tr>

HTML;
							}
						}
						
						$IPBHTML .= <<<HTML
				</table>
			</div>
		</td>
		<td style="width: 50%;" valign="top">
			<div class="acp-box">
				<h3>{$this->lang->words['ov_general_info']}</h3>
				<table class="ipsTable">
					<tr>
						<td style="width: 40%;"><strong>{$this->lang->words['shoutbox_online']}</strong></td>
						<td style="width: 60%; text-align: center;"><img src='{$this->settings['skin_acp_url']}/images/icons/{$stats['online']}.png' alt="" /></td>
					</tr>
					<tr>
						<td><strong>{$this->lang->words['shoutbox_version']}</strong></td>
						<td align='center'>{$this->caches['app_cache']['shoutbox']['app_version']}</td>
					</tr>

HTML;
						
						if ( $_update )
						{
							$IPBHTML .= <<<HTML
					<tr>
						<td colspan="2" style="text-align: center;">
							{$_update}
						</td>
					</tr>

HTML;
						}
						
						$IPBHTML .= <<<HTML
				</table>
			</div>
			<br class="clear" />
			<div class="acp-box">
				<h3>{$this->lang->words['group_permissions']}</h3>
				<table class="ipsTable">

HTML;
				
				foreach ( $this->caches['group_cache'] as $group )
				{
					$name = IPSMember::makeNameFormatted( $group['g_title'], $group['g_id'] );
					
					$IPBHTML .= <<<HTML
					<tr>
						<td style="width: 80%;">{$name}</td>
						<td style="width: 80%; text-align: center;">
							<a href="{$this->settings['_base_url']}app=members&amp;module=groups&amp;section=groups&amp;do=edit&amp;id={$group['g_id']}&amp;_initTab=shoutbox" title="{$this->lang->words['edit_group']}">{$this->lang->words['edit_group']}</a>
						</td>
					</tr>

HTML;
				}
				
				$IPBHTML .= <<<HTML
				</table>
			</div>
		</td>
	</tr>
</table>
<center><b>Full Support at IPBShoutbox.com</b></center>
HTML;

		//--endhtml--//
		return $IPBHTML;
	}
}