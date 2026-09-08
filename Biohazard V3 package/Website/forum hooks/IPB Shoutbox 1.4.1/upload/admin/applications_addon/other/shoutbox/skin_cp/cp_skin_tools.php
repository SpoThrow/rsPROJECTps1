<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

class cp_skin_tools extends output
{
	public function __destruct()
	{
	}

	public function toolsList( $form )
	{
		$IPBHTML = "";
		//--starthtml--//

		$IPBHTML .= <<<HTML
<div class="section_title">
	<h2>{$this->lang->words['tls_list']}</h2>
</div>
<form action="{$this->settings['base_url']}{$this->form_code}" method="post">
	<div class="acp-box">
		<h3>{$this->lang->words['tls_rebuild_caches']}</h3>
		<table class="ipsTable">
			<tr>
				<td class="field_field">
					<strong class="title">{$this->lang->words['tls_rebuild_caches']}</strong><br />
					<span class="desctext">{$this->lang->words['tls_rebuild_caches_desc']}</span>
				</td>
			</tr>
		</table>
		<div class="acp-actionbar">
			<input type="hidden" name="do" value="rebuildCache" />
			<input type="hidden" name="_admin_auth_key" value="{$this->registry->adminFunctions->generated_acp_hash}" />
			{$form['caches']} <input type="submit" value="{$this->lang->words['tls_rebuild_caches']}" class="button primary" />
		</div>
	</div>
</form>
<br />
<form action="{$this->settings['base_url']}{$this->form_code}" method="post">
	<div class="acp-box">
		<h3>{$this->lang->words['tls_reset_prefs']}</h3>
		<table class="ipsTable">
			<tr>
				<td class="field_field">
					<strong class="title">{$this->lang->words['tls_reset_prefs']}</strong><br />
					<span class="desctext">{$this->lang->words['tls_reset_prefs_desc']}</span>
				</td>
			</tr>
		</table>
		<div class="acp-actionbar">
			<input type="hidden" name="do" value="resetPrefs" />
			<input type="hidden" name="_admin_auth_key" value="{$this->registry->adminFunctions->generated_acp_hash}" />
			{$form['prefs']} {$this->lang->words['tls_per_cycle']} <input type="submit" value="{$this->lang->words['tls_reset_prefs']}" class="button primary" />
		</div>
	</div>
</form>
<br />
<form action="{$this->settings['base_url']}{$this->form_code}" method="post">
	<div class="acp-box">
		<h3>{$this->lang->words['tls_empty_table']}</h3>
		<table class="ipsTable">
			<tr>
				<td class="field_field">
					<strong class="title">{$this->lang->words['tls_empty_table']}</strong><br />
					<span class="desctext">{$this->lang->words['tls_empty_table_desc']}</span>
				</td>
			</tr>
		</table>
		<div class="acp-actionbar">
			<input type="hidden" name="do" value="emptyTable" />
			<input type="hidden" name="_admin_auth_key" value="{$this->registry->adminFunctions->generated_acp_hash}" />
			<input type="submit" value="{$this->lang->words['tls_empty_table']}" class="button primary" />
		</div>
	</div>
</form>
HTML;

		//--endhtml--//
		return $IPBHTML;
	}

	public function emptyTableConfirmation()
	{
		$IPBHTML = "";
		//--starthtml--//

		$IPBHTML .= <<<HTML
<div class="section_title">
	<h2>{$this->lang->words['tls_empty_confirm']}</h2>
</div>
<form action="{$this->settings['base_url']}{$this->form_code}" method="post">
	<div class="acp-box">
		<h3>{$this->lang->words['tls_empty_table']}</h3>
		<table class="ipsTable">
			<tr>
				<td class="field_field">
					<strong class="title">{$this->lang->words['tls_empty_table']}</strong><br />
					<span class="desctext">
						{$this->lang->words['tls_empty_table_desc']}<br /><br />
						<strong style="color:red">{$this->lang->words['tls_empty_confirm_desc']}</strong>
					</span>
				</td>
			</tr>
		</table>
		<div class="acp-actionbar">
			<input type="hidden" name="do" value="emptyTable" />
			<input type="hidden" name="confirm" value="1" />
			<input type="hidden" name="_admin_auth_key" value="{$this->registry->adminFunctions->generated_acp_hash}" />
			<input type="submit" value="{$this->lang->words['tls_proceed_action']}" class="button primary" />
		</div>
	</div>
</form>
HTML;

		//--endhtml--//
		return $IPBHTML;
	}
}