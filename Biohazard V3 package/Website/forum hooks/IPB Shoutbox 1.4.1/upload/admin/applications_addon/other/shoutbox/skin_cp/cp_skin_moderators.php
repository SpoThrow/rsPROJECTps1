<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

class cp_skin_moderators extends output
{
	public function __destruct()
	{
	}
	
	public function moderatorsListView( $rows, $pages )
	{
		$IPBHTML = "";
		//--starthtml--//
		
		$IPBHTML .= <<<HTML
<div class="section_title">
	<h2>{$this->lang->words['mod_overview']}</h2>
	<div class="ipsActionBar clearfix">
		<ul>
			<li class="ipsActionButton">
				<a href="{$this->settings['base_url']}{$this->form_code}&amp;do=addGroup">
					<img src="{$this->settings['skin_acp_url']}/images/icons/add.png" alt="" />
					{$this->lang->words['mod_add_new_group']}
				</a>
			</li>
			<li class="ipsActionButton">
				<a href="{$this->settings['base_url']}{$this->form_code}&amp;do=addMember">
					<img src="{$this->settings['skin_acp_url']}/images/icons/add.png" alt="" />
					{$this->lang->words['mod_add_new_member']}
				</a>
			</li>
		</ul>
	</div>
</div>
<div class="acp-box">
	<h3>{$this->lang->words['mod_list']}</h3>
	<table class="ipsTable">
		<tr>
			<th style="width: 7%;">{$this->lang->words['mod_type']}</th>
			<th style="width: 83%;">{$this->lang->words['mod_moderator']}</th>
			<th class="col_buttons" style="width: 10%; text-align: right;">{$this->lang->words['a_options']}</th>
		</tr>
HTML;
		
		if ( count( $rows ) && is_array( $rows ) )
		{
			foreach ( $rows as $row )
			{
				$IPBHTML .= <<<HTML
		<tr class="ipsControlRow">
			<td><strong>{$row['_type']}</strong></td>
			<td>{$row['_name']}</td>
			<td>
				<ul class="ipsControlStrip">
					<li class="i_edit">
						<a href="{$this->settings['base_url']}{$this->form_code}&amp;do=edit&amp;id={$row['m_id']}">{$this->lang->words['mod_edit_mod']}</a>
					</li>
					<li class="i_delete">
						<a href="#" onclick='acp.confirmDelete("{$this->settings['base_url']}&amp;{$this->form_code}&amp;do=delete&amp;id={$row['m_id']}");'>{$this->lang->words['mod_delete_mod']}</a>
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
			<td colspan="3" class="no_messages">
				{$this->lang->words['mod_no_mods_added']}
			</td>
		</tr>
HTML;
		}
		
		$IPBHTML .= <<<HTML
	</table>
</div>
HTML;
		
		//--endhtml--//
		return $IPBHTML;
	}
	
	public function moderatorFormView( $title, $button, $action, $type, $form )
	{
		$IPBHTML = "";
		//--starthtml--//
		
		$IPBHTML .= <<<HTML
<div class="section_title">
	<h2>{$this->lang->words['mod_form_title']}</h2>
</div>
HTML;
		
		if ( strtolower( $type ) == 'member' )
		{
		$IPBHTML .= <<<HTML
<script type="text/javascript" src="{$this->settings['js_app_url']}acp.moderators.js"></script>
HTML;
		}
		
		$IPBHTML .= <<<HTML
<div class="acp-box">
	<h3>{$title}</h3>
	<form action="{$this->settings['base_url']}{$this->form_code}&amp;do={$action}" method="post">
		<table class="ipsTable">
			<tr>
				<td class="field_title">
					<strong class="title">{$type}</strong>
				</td>
				<td class="field_field">
					{$form['typeText']}
				</td>
			</tr>
			<tr>
				<th colspan="2">
					{$this->lang->words['mod_permissions']}
				</th>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['mod_can_edit']}</strong>
				</td>
				<td class="field_field">
					{$form['edit']}
				</td>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['mod_can_delete']}</strong>
				</td>
				<td class="field_field">
					{$form['delete']}
				</td>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['mod_can_delete_all']}</strong>
				</td>
				<td class="field_field">
					{$form['prune']}
				</td>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['mod_can_ban']}</strong>
				</td>
				<td class="field_field">
					{$form['ban']}
				</td>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['mod_can_unban']}</strong>
				</td>
				<td class="field_field">
					{$form['unban']}
				</td>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['mod_can_remove']}</strong>
				</td>
				<td class="field_field">
					{$form['remove']}
				</td>
			</tr>
		</table>
		<div class="acp-actionbar">
			<input type="hidden" name="_admin_auth_key" value="{$this->registry->adminFunctions->getSecurityKey()}" />
			<input type="submit" value="{$button}" class="button" />
		</div>
	</form>
</div>
HTML;
		
		//--endhtml--//
		return $IPBHTML;
	}
}