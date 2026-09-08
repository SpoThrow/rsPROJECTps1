<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

class cp_skin_shoutbox_group_form extends output
{
	public function __destruct()
	{
	}
	
	public function acp_group_form_main( $group, $tabId )
	{
		/* Set up our array of form elements */
		$form                                 = array();
		$form['g_shoutbox_view']              = $this->registry->output->formYesNo( "g_shoutbox_view", $group['g_shoutbox_view'] );
		$form['g_shoutbox_use']               = $this->registry->output->formYesNo( "g_shoutbox_use", $group['g_shoutbox_use'] );
		$form['g_shoutbox_posts_req']         = $this->registry->output->formInput( "g_shoutbox_posts_req", $group['g_shoutbox_posts_req'] );
		$form['g_shoutbox_posts_req_display'] = $this->registry->output->formYesNo( "g_shoutbox_posts_req_display", $group['g_shoutbox_posts_req_display'] );
		$form['g_shoutbox_old_req']			  = $this->registry->output->formInput( "g_shoutbox_old_req", $group['g_shoutbox_old_req'] );
		$form['g_shoutbox_old_req_display']	  = $this->registry->output->formYesNo( "g_shoutbox_old_req_display", $group['g_shoutbox_old_req_display'] );
		$form['g_shoutbox_bypass_flood']      = $this->registry->output->formYesNo( "g_shoutbox_bypass_flood", $group['g_shoutbox_bypass_flood'] );
		$form['g_shoutbox_edit']              = $this->registry->output->formYesNo( "g_shoutbox_edit", $group['g_shoutbox_edit'] );
		$form['g_shoutbox_delete_own']        = $this->registry->output->formYesNo( "g_shoutbox_delete_own", $group['g_shoutbox_delete_own'] );
		$form['g_shoutbox_view_archive']      = $this->registry->output->formYesNo( "g_shoutbox_view_archive", $group['g_shoutbox_view_archive'] );
		
		$IPBHTML = "";
		
		/* Display the tab */
		$IPBHTML .= <<<EOF
<div id="tab_GROUPS_{$tabId}_content">
	<table class="ipsTable">
		<tr>
			<td class="field_title">
				<strong class="title">{$this->lang->words['g_shoutbox_view']}</strong>
			</td>
			<td class="field_field">
				{$form['g_shoutbox_view']}<br />
				<span class="desctext">{$this->lang->words['g_shoutbox_view_desc']}</span>
			</td>
		</tr>
		<tr>
			<td class="field_title">
				<strong class="title">{$this->lang->words['g_shoutbox_use']}</strong>
			</td>
			<td class="field_field">
				{$form['g_shoutbox_use']}<br />
				<span class="desctext">{$this->lang->words['g_shoutbox_use_desc']}</span>
			</td>
		</tr>
		<tr>
			<td class="field_title">
				<strong class="title">{$this->lang->words['g_shoutbox_posts_req']}</strong>
			</td>
			<td class="field_field">
				{$form['g_shoutbox_posts_req']}<br />
				<span class="desctext">{$this->lang->words['g_shoutbox_posts_req_desc']}</span>
			</td>
		</tr>
		<tr>
			<td class="field_title">
				<strong class="title">{$this->lang->words['g_shoutbox_posts_req_display']}</strong>
			</td>
			<td class="field_field">
				{$form['g_shoutbox_posts_req_display']}<br />
				<span class="desctext">{$this->lang->words['g_shoutbox_posts_req_display_desc']}</span>
			</td>
		</tr>
		<tr>
					<td class="field_title">
						<strong class="title">{$this->lang->words['g_shoutbox_old_req']}</strong>
					</td>
					<td class="field_field">
						{$form['g_shoutbox_old_req']}<br />
						<span class="desctext">{$this->lang->words['g_shoutbox_old_req_desc']}</span>
					</td>
				</tr>
				<tr>
					<td class="field_title">
						<strong class="title">{$this->lang->words['g_shoutbox_old_req_display']}</strong>
					</td>
					<td class="field_field">
						{$form['g_shoutbox_old_req_display']}<br />
						<span class="desctext">{$this->lang->words['g_shoutbox_old_req_display_desc']}</span>
					</td>
				</tr>
		<tr>
			<td class="field_title">
				<strong class="title">{$this->lang->words['g_shoutbox_bypass_flood']}</strong>
			</td>
			<td class="field_field">
				{$form['g_shoutbox_bypass_flood']}<br />
				<span class="desctext">{$this->lang->words['g_shoutbox_bypass_flood_desc']}</span>
			</td>
		</tr>
		<tr>
			<td class="field_title">
				<strong class="title">{$this->lang->words['g_shoutbox_edit']}</strong>
			</td>
			<td class="field_field">
				{$form['g_shoutbox_edit']}<br />
				<span class="desctext">{$this->lang->words['g_shoutbox_edit_desc']}</span>
			</td>
		</tr>
		<tr>
			<td class="field_title">
				<strong class="title">{$this->lang->words['g_shoutbox_delete_own']}</strong>
			</td>
			<td class="field_field">
				{$form['g_shoutbox_delete_own']}<br />
				<span class="desctext">{$this->lang->words['g_shoutbox_delete_own_desc']}</span>
			</td>
		</tr>
		<tr>
			<td class="field_title">
				<strong class="title">{$this->lang->words['g_shoutbox_view_archive']}</strong>
			</td>
			<td class="field_field">
				{$form['g_shoutbox_view_archive']}<br />
				<span class="desctext">{$this->lang->words['g_shoutbox_view_archive_desc']}</span>
			</td>
		</tr>
	</table>
</div>
EOF;
		
		/* Return */
		return $IPBHTML;
	}

	public function acp_group_form_tabs( $group, $tabId )
	{
		$IPBHTML = "";
		
		/* The app title */
		$title = IPSLib::getAppTitle('shoutbox');
		
		/* Display the tab */
		$IPBHTML .= <<<EOF
	<li id='tab_GROUPS_{$tabId}'>{$title}</li>
EOF;
		
		/* Return */
		return $IPBHTML;
	}
}