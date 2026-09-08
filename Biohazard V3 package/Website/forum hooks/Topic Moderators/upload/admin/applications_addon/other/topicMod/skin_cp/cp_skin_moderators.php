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
 
class cp_skin_moderators extends output
{
	/**
	* Prevent our main destructor being called by this class
	*
	* @param	void
	* @return	void
	* @access	public
	*/
	
	public function __destruct() { }

	
	public function listModerators( $mods=array() )	
	{

$IPBHTML = "";

$IPBHTML .= <<<HTML

<div class='section_title'>
	<h2>Topic Moderators</h2>
	
	<div class='ipsActionBar clearfix'>
		<ul>
			<li class='ipsActionButton'>
				<a href="{$this->settings['base_url']}{$this->form_code}&amp;do=newModerator">
					<img src="{$this->settings['skin_acp_url']}/images/icons/user_add.png" /> Add new moderator
				</a>
			</li>
		</ul>
	</div>
</div>

<br />

<div class='acp-box'>
	<h3>Moderators</h3>
	<table class='ipsTable double_tad'>
		<tr>
			<th style='width: 1%'>&nbsp;</th>
			<th style='width: 15%'>Member/Group Name</th>
			<th style='width: 15%'>Topic Title</th>
			<th style='width: 15%; text-align: center;'>Can Close / Open?</th>
			<th style='width: 15%; text-align: center;'>Can Pin / Unpin?</th>
			<th style='width: 15%; text-align: center;'>Can Edit Post / Topic Title?</th>
			<th style='width: 15%; text-align: center;'>Can Delete Post?</th>
			<th style='width: 15%; text-align: center;'>Can Queue Posts?</th>
			<th width='5%'>&nbsp;</th>
		</tr>
HTML;

if ( count( $mods ) > 0 )
{
	
	foreach( $mods as $mod )
	{
		$IPBHTML .= <<<HTML
			<tr class='ipsControlRow'>
				<td><img src="{$mod['prefix']}" alt='' border='' /></td>
				<td class="altrow">{$mod['memberName']}</td>	
				<td>{$mod['topicTitle']}</td>
				<td style='text-align: center;'>{$mod['close_topic']} / {$mod['open_topic']}</td>
				<td style='text-align: center;'>{$mod['pin_topic']} / {$mod['unpin_topic']}</td>
				<td style='text-align: center;'>{$mod['edit_post']} / {$mod['edit_topic']}</td>
				<td style='text-align: center;'>{$mod['gbw_soft_delete']}</td>
				<td style='text-align: center;'>{$mod['post_q']}</td>
				<td class='col_buttons right'>
					<ul class='ipsControlStrip'>
						<li class='i_edit'>
							<a href='{$this->settings['base_url']}&amp;{$this->form_code}&amp;do=editModerator&amp;id={$mod['id']}'>Edit Moderator</a>
						</li>
						<li class='i_delete'>
							<a href='#' onclick='return acp.confirmDelete("{$this->settings['base_url']}{$this->form_code}&amp;do=deleteModerator&amp;id={$mod['id']}&amp;_admin_auth_key={$this->registry->getClass('adminFunctions')->_admin_auth_key}");'>
								Delete Moderator
							</a>
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
		<td class='no_messages' colspan='11'>
			You've not added any topic moderators yet. <a class='mini_button' href="{$this->settings['base_url']}{$this->form_code}&amp;do=newModerator">Add moderator</a>
		</td>
	</tr>
HTML;
}
	$IPBHTML .= <<<HTML
	</table>
</div>

HTML;
		return $IPBHTML;
	}
	
	public function showForm( array $formData, $type )	
	{


//-----------------------------------------
// Set some of the form variables
//-----------------------------------------

$button				= $type == 'edit' ? $this->lang->words['mod_edithis'] : $this->lang->words['mod_addthis'];
$title 				= $type == 'edit' ? $this->lang->words['mod_edit'] : $this->lang->words['mod_add'];
$secure_key			= ipsRegistry::getClass('adminFunctions')->getSecurityKey();

//-----------------------------------------
// Start off the form fields
//-----------------------------------------

$form					= array();

$form['memberName']		= $this->registry->output->formInput( "memberName", 	$formData['members_display_name'] );
$form['topicId']		= $this->registry->output->formInput( "topic_id", 		intval( $formData['topic_id'] ) );
$form['close_topic']	= $this->registry->output->formYesNo( "close_topic", 	$formData['close_topic'] );
$form['open_topic']		= $this->registry->output->formYesNo( "open_topic", 	$formData['open_topic'] );
$form['edit_topic']		= $this->registry->output->formYesNo( "edit_topic", 	$formData['edit_topic'] );
$form['pin_topic']		= $this->registry->output->formYesNo( "pin_topic", 		$formData['pin_topic'] );
$form['unpin_topic']	= $this->registry->output->formYesNo( "unpin_topic", 	$formData['unpin_topic'] );
$form['edit_post']		= $this->registry->output->formYesNo( "edit_post", 		$formData['edit_post'] );
$form['post_q']			= $this->registry->output->formYesNo( "post_q", 		$formData['post_q'] );
$form['moderate_own']	= $this->registry->output->formCheckbox( "moderate_own", $formData['moderate_own'] );
$form['member_groups']	= $this->registry->output->formDropdown( 'group_id', $formData['member_groups'], $formData['group_id'] );

$form['gbw_mod_soft_delete']			= $this->registry->output->formYesNo( "gbw_soft_delete", $formData['gbw_soft_delete'] );
$form['gbw_mod_un_soft_delete']			= $this->registry->output->formYesNo( "gbw_un_soft_delete", $formData['gbw_un_soft_delete'] );
$form['gbw_mod_soft_delete_see']		= $this->registry->output->formYesNo( "gbw_soft_delete_see", $formData['gbw_soft_delete_see'] );
$form['gbw_mod_soft_delete_reason']		= $this->registry->output->formYesNo( "gbw_soft_delete_reason", $formData['gbw_soft_delete_reason'] );
$form['gbw_mod_soft_delete_see_post']	= $this->registry->output->formYesNo( "gbw_soft_delete_see_post", $formData['gbw_soft_delete_see_post'] );

/* Forums dropdown */
require_once( IPSLib::getAppDir( 'forums' ) .'/sources/classes/forums/class_forums.php' );/*noLibHook*/
$classToLoad = IPSLib::loadLibrary( IPSLib::getAppDir( 'forums' ) .'/sources/classes/forums/admin_forum_functions.php', 'admin_forum_functions', 'forums' );

$aff = new $classToLoad( $this->registry );
$aff->forumsInit();
$forum_jump = $aff->adForumsForumData();
		
/* Build forum multiselect */
$form['forums'] = "<select name='forums[]' class='textinput' size='15' multiple='multiple'>\n";

$form['forums'] .= "<option value='all' " . ( $formData['forums'] == '*' ? "selected='selected'" : '' ) . ">{$this->lang->words['frm_m_allForums']}</option>\n";
	
foreach( $forum_jump as $i )
{
	if( strstr( "," . $formData['forums'] . ",", "," . $i['id'] . "," ) AND $formData['forums'] != '*' )
	{
		$selected = ' selected="selected"';
	}
	else
	{
		$selected = "";
	}
	
	if( !empty( $i['redirect_on'] ) )
	{
		continue;
	}
	
	$fporum_jump[] = array( $i['id'], $i['depthed_name'] );
	
	$form['forums']  .= "<option value=\"{$i['id']}\" $selected>{$i['depthed_name']}</option>\n";
}

$form['forums'] .= "</select>";

$IPBHTML = "";

$IPBHTML .= <<<HTML
<script type='text/javascript' src='{$this->settings['js_app_url']}acp.topicMod.js'></script>
<div class='section_title'>
	<h2>{$title}</h2>
</div>

<div class='warning' id="formWarningBox" style="display:none;">
</div>
<br />

<div class='acp-box'>
	<h3>{$this->lang->words['frm_m_genset']}</h3>
	<form action='{$this->settings['base_url']}{$this->form_code}&amp;secure_key={$secure_key}' method='post' name='adform'  id='adform'>
		<input type='hidden' name='do' value='saveModerator' />
		<input type='hidden' name='id' value='{$formData['id']}' />
		
		<table width='100%' cellpadding='0' cellspacing='0' class='ipsTable double_pad'>
			<tr>
				<th colspan='2'>Details</th>
			</tr>
			<tr>
				<td class='field_title'>
					<strong class='title'>{$this->lang->words['frm_m_memberName']}</strong>
				</td>
				<td class='field_field'>
					{$form['memberName']}
				</td>
			</tr>
			<tr>
				<td class='field_title'>
					<strong class='title'>{$this->lang->words['frm_m_group']}</strong>
				</td>
				<td class='field_field'>
					{$form['member_groups']}
				</td>
			</tr>
			<tr id="topicData">
		 		<td class='field_title'>
					<strong class='title'>{$this->lang->words['frm_m_topicId']}</strong>
				</td>
		 		<td class='field_field'>
					{$form['topicId']}
					<span id="topicTitle" class='desctext'>{$formData['topicLink']}</span>
		 		</td>
		 	</tr>
		 	<tr>
		 		<td class='field_title'>
					<strong class='title'>{$this->lang->words['frm_m_moderateOwn']}</strong>
				</td>
		 		<td class='field_field'>
					{$form['moderate_own']}<br />
					<span class="desctext">{$this->lang->words['frm_m_moderateOwn_desc']}</span>
		 		</td>
		 	</tr>
		 	<tr id="forumsDropdown" style="display: none;">
				<div>
					<td class='field_title'>
						<strong class='title'>{$this->lang->words['frm_m_forums']}</strong>
					</td>
					<td class='field_field'>
						{$form['forums']}<br />
						<span class='desctext'>{$this->lang->words['frm_m_forumDesc']}</span>
					</td>
				</div>
			</tr>
		 	<tr>
				<th colspan='2'>{$this->lang->words['frm_m_modOptions']}</th>
			</tr>
		 	<tr>
		 		<td class='field_title'>
					<strong class='title'>{$this->lang->words['frm_m_close']}</strong>
				</td>
		 		<td class='field_field'>
					{$form['close_topic']}
		 		</td>
		 	</tr>
		 	
		 	<tr>
		 		<td class='field_title'>
					<strong class='title'>{$this->lang->words['frm_m_open']}</strong>
				</td>
		 		<td class='field_field'>
					{$form['open_topic']}
		 		</td>
		 	</tr>
			
		 	<tr>
		 		<td class='field_title'>
					<strong class='title'>{$this->lang->words['frm_m_pin']}</strong>
				</td>
		 		<td class='field_field'>
					{$form['pin_topic']}
		 		</td>
		 	</tr>
		 	
		 	<tr>
		 		<td class='field_title'>
		 			<strong class='title'>{$this->lang->words['frm_m_unpin']}</strong>
		 		</td>
		 		<td class='field_field'>
		 			{$form['unpin_topic']}
		 		</td>
		 	</tr>
		 	
		 	<tr>
		 		<td class='field_title'>
		 			<strong class='title'>{$this->lang->words['frm_m_edit']}</strong>
		 		</td>
		 		<td class='field_field'>
		 			{$form['edit_post']}
		 		</td>
		 	</tr>
			<tr>
		 		<td class='field_title'>
					<strong class='title'>{$this->lang->words['frm_m_topic']}</strong><br />
				</td>
		 		<td class='field_field'>
					{$form['edit_topic']}
		 		</td>
		 	</tr>

			<tr>
				<td class='field_title'>
					<strong class='title'>{$this->lang->words['frm_m_visiblepost']}</strong>
				</td>
				<td class='field_field'>
					{$form['post_q']}
				</td>
			</tr>
			
		 	<tr>
				<td class='field_title'>
					<strong class='title'>{$this->lang->words['gf_bw_soft_delete']}</strong>
				</td>
				<td class='field_field'>
					{$form['gbw_mod_soft_delete']}<br />
					<span class='desctext'>{$this->lang->words['soft_delete_info']}</span>
				</td>
			</tr>
			<tr>
				<td class='field_title'>
					<strong class='title'>{$this->lang->words['gf_bw_soft_delete_see']}</strong>
				</td>
				<td class='field_field'>
					{$form['gbw_mod_soft_delete_see']}
				</td>
			</tr>
			<tr>
				<td class='field_title'>
					<strong class='title'>{$this->lang->words['gf_bw_un_soft_delete']}</strong>
				</td>
				<td class='field_field'>
					{$form['gbw_mod_un_soft_delete']}<br />
					<span class='desctext'>"Can see 'Soft Deleted' posts" has to be enabled for this to function</span>
				</td>
			</tr>
			<tr>
				<td class='field_title'>
					<strong class='title'>{$this->lang->words['gf_bw_soft_delete_reason']}</strong>
				</td>
				<td class='field_field'>
					{$form['gbw_mod_soft_delete_reason']}
				</td>
			</tr>
			<tr>
				<td class='field_title'>
					<strong class='title'>{$this->lang->words['gf_bw_soft_delete_see_post']}</strong>
				</td>
				<td class='field_field'>
					{$form['gbw_mod_soft_delete_see_post']}
				</td>
			</tr>
		 	
		</table>
		<div class='acp-actionbar'>
			<input type='submit' value='{$button}' class='button' accesskey='s'>
		</div>
	</form>
</div>
HTML;

		return $IPBHTML;
	}
}