<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

class cp_skin_banned extends output
{
	public function __destruct()
	{
	}
	
	public function bannedListView( $rows, $pages )
	{
		$IPBHTML = "";
		//--starthtml--//
		
		$IPBHTML .= <<<HTML
<div class="section_title">
	<h2>{$this->lang->words['ban_list_title']}</h2>
</div>
<div class="acp-box">
	<h3>{$this->lang->words['ban_list_list']}</h3>
	<table class="ipsTable">
		<tr>
			<th style="width: 90%;">{$this->lang->words['mod_member_display_name']}</th>
			<th class="col_buttons" style="width: 10%; text-align: right;">{$this->lang->words['a_options']}</th>
		</tr>
HTML;
		
		if ( count( $rows ) && is_array( $rows ) )
		{
			foreach ( $rows as $row )
			{
				$IPBHTML .= <<<HTML
		<tr class="ipsControlRow">
			<td>{$row['members_display_name']}</td>
			<td>
				<ul class="ipsControlStrip">
					<li class="i_delete">
						<a href="#" onclick='acp.confirmDelete("{$this->settings['base_url']}&amp;{$this->form_code}&amp;do=unban&amp;mid={$row['member_id']}");'>{$this->lang->words['ban_remove_it']}</a>
					</li>
				</ul>
			</td>
		</tr>
HTML;
			}
		}
		else
		{
			$IPBHTML .= <<<HTML
		<tr>
			<td colspan="2" class="no_messages">
				{$this->lang->words['ban_no_banned_yet']}
			</td>
		</tr>
HTML;
		}
		
		$IPBHTML .= <<<HTML
	</table>
</div>
<br />
{$pages}
<br />
<script type="text/javascript" src="{$this->settings['js_app_url']}acp.banned.js"></script>
<div class="acp-box">
	<h3>{$this->lang->words['ban_member']}</h3>
	<form action="{$this->settings['base_url']}{$this->form_code}" method="post">
		<div class="acp-actionbar">
			<input type="hidden" name="do" value="ban" />
			<input type="hidden" name="_admin_auth_key" value="{$this->registry->adminFunctions->generated_acp_hash}" />
			{$this->lang->words['mod_member_display_name']}: <input type="text" name="member_name" id="banMemberName" value="" size="20" class="realbutton" /> <input type="submit" value="{$this->lang->words['ban_member']}" class="button primary" />
		</div>
	</form>
</div>
HTML;
		
		//--endhtml--//
		return $IPBHTML;
	}
}