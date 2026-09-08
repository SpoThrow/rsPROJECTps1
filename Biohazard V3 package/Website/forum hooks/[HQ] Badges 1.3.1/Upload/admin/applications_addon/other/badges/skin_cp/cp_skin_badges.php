<?php
class cp_skin_badges extends output
{
	public function list_badges( $badges )
	{
		/* Post key */
		$post_key = $this->request['post_key'] ? $this->request['post_key'] : md5( microtime() );
		
		$IPBHTML = "";
		//--starthtml--//
		
		$IPBHTML .= <<<HTML
<script type="text/javascript" src="{$this->settings['js_app_url']}acp.savedQueries.js"></script>
<script type="text/javascript">
	ipb.lang['js_error_occurred'] = "{$this->lang->words['js_error_occurred']}";
</script>
<div class="section_title">
	<h2>{$this->lang->words['mod_title']}</h2>
	<div class="ipsActionBar clearfix">
		<ul>
			<li class="ipsActionButton">
				<a href="{$this->settings['base_url']}{$this->form_code}&amp;do=newBadges&amp;post_key={$post_key}">
					<img src="{$this->settings['skin_acp_url']}/images/icons/add.png" alt="{$this->lang->words['new_Badges']}" /> {$this->lang->words['new_Badges']}
				</a>
			</li>
		</ul>
	</div>
</div>
<div class="acp-box">
	<h3>{$this->lang->words['badges']}</h3>

HTML;
		if ( count( $badges ) )
		{
			$IPBHTML .= <<<HTML
	<table class="ipsTable">
		<tr>
			<th style="width: 5%;">{$this->lang->words['Badges_id']}</th>
			<th style="width: 15%;">{$this->lang->words['Badges_code']}</th>
			<th style="width: 75%;">{$this->lang->words['Badges_groups']}</th>
			<th style="width: 5%;">{$this->lang->words['Badges_enable']}</th>
			<th style="width: 5%;">Sec.Group?</th>
			<th class="col_buttons">&nbsp;</th>
		</tr>

HTML;
			foreach ( $badges as $query )
			{
				if ($query['ba_enabled'])
				{
					$active = "<img src=\"{$this->settings['skin_acp_url']}/images/icons/accept.png\">";
				} else {
					$active = "<img src=\"{$this->settings['skin_acp_url']}/images/icons/cross.png\">";
				}
				if ($query['ba_t'])
				{
					$sec = "<img src=\"{$this->settings['skin_acp_url']}/images/icons/accept.png\">";
				} else {
					$sec = "<img src=\"{$this->settings['skin_acp_url']}/images/icons/cross.png\">";
				}
				$IPBHTML .= <<<HTML
		<tr class="ipsControlRow">
			<td style="background-color: aliceBlue;text-align: center;"><strong><a href="#">{$query['ba_id']}</a></strong></td>
			<td><img src="{$this->settings['board_url']}/uploads/badges/{$query['ba_type']}.png"</td>
			<td>{$query['ba_gid_s']}</td>
			<td align='center'>{$active}</td>
			<td align='center'>{$sec}</td>
			<td>
				<ul class="ipsControlStrip">
					<li class="i_export">

HTML;
				
				$IPBHTML .= <<<HTML
					</li>
					<li class="i_edit">
						<a href="{$this->settings['base_url']}{$this->form_code}&amp;do=editBadges&amp;id={$query['ba_id']}">{$this->lang->words['edit_Badges']}</a>
					</li>
					<li class="i_delete">
						<a href="#" onclick="acp.confirmDelete('{$this->settings['base_url']}{$this->form_code}&amp;do=deleteBadges&amp;id={$query['ba_id']}');">{$this->lang->words['delete_Badges']}</a>
					</li>
				</ul>
			</td>
		</tr>

HTML;
			}
			
			$IPBHTML .= <<<HTML
			
	</table>
</div>
HTML;
		}
		else
		{
			$IPBHTML .= <<<HTML
	<div class="no_messages">
		{$this->lang->words['no_badges']} <a href="{$this->settings['base_url']}{$this->form_code}&amp;do=newBadges&amp;post_key={$post_key}" class="mini_button">{$this->lang->words['new_Badges']}</a>
	</div>

HTML;
		}
		
		//--endhtml--//
		return $IPBHTML;
	}
	
	public function badges_form( $query=array(), $type, $title, $blurb )
	{
		/* Load forums functions */
		
		ipsRegistry::getAppClass( 'forums' );
		
		$form = array();
		
		$form['enabled']  = $this->registry->output->formYesNo( 'enabled', isset( $query['ba_enabled'] ) ? $query['ba_enabled'] : 0 );
		$form['second']   = $this->registry->output->formYesNo( 'second', isset( $query['ba_t'] ) ? $query['ba_t'] : 0 );
		$form['link']     = $this->registry->output->formInput( 'link', isset( $query['ba_links'] ) ? $query['ba_links'] : '' );
		
		$forumslist = $this->registry->getClass('class_forums')->adForumsForumList(1);
		
		$form['forums']	  = $this->registry->output->formMultiDropdown( "forums[]", $forumslist, explode( ',', $query['ba_forums'] ) );
		
		$icon  = array(array('1', 'Badge 1'),array('2', 'Badge 2'),array('3', 'Badge 3'),array('4', 'Badge 4'),array('5', 'Badge 5'));
		
		$form['icon'] 	  = $this->registry->output->formDropdown( 'icon', $icon, $query['ba_type'] ? $query['ba_type'] : '');

		foreach( $this->registry->cache()->getCache('group_cache') as $g_id => $group )
		{
			$mem_group[]  = array( $g_id , $group['g_title'] );
		}
		if ( $type == 'new' )
		{
			$ba_saved = array();
			
			$this->DB->build( array( 'select' => 'ba_gid', 'from' => 'HQ_badges', 'where' => 'ba_enabled=1' ) );
			$this->DB->execute();
			
			while($val = $this->DB->fetch())
				{
					$ba_saved[] = $val[ba_gid];
				}
			$tot   = count($mem_group);	
			$tot2   = count($ba_saved);
			for ($bcount=0; $bcount < $tot; $bcount++) 
				{						
					for ($i=0; $i < $tot2; $i++) {
						if ($mem_group[$bcount][0] == $ba_saved[$i]) {
							unset($mem_group[$bcount]);
						}
					}
				}
		}
		$form['groups']   = $this->registry->output->formMultiDropdown( "groups[]", $mem_group, explode( ',', $query['ba_gid'] ), 5 );
		
		
		// $form['badge'] 	  = $this->registry->output->formUpload( 'badge' );
		
		$IPBHTML = "";
		//--starthtml--//
		
		$IPBHTML .= <<<HTML
<div class="section_title">
	<h2>{$title}</h2>
</div>
<form action="{$this->settings['base_url']}{$this->form_code}&amp;do=doBadges" method="post">
	<div class="acp-box">
		<h3>{$title}</h3>
		<table class="ipsTable">
			<tr>
				<th colspan='3'>{$blurb}</th>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['Badges_enable']}</strong>
				</td>
				<td class="field_field">{$form['enabled']}</td>
				<td></td>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['Badges_code']}</strong>
				</td>
				<td class="field_field">{$form['icon']}</td>
				<td>
				B. 1 <img src="&#46;&#46;/uploads/badges/1.png" width="20px" height="20px"> 
				B. 2 <img src="&#46;&#46;/uploads/badges/2.png" width="20px" height="20px"> 
				B. 3 <img src="&#46;&#46;/uploads/badges/3.png" width="20px" height="20px"> 
				B. 4 <img src="&#46;&#46;/uploads/badges/4.png" width="20px" height="20px"> 
				B. 5 <img src="&#46;&#46;/uploads/badges/5.png" width="20px" height="20px"></td>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['Badges_groups']}</strong>
				</td>
				<td class="field_field">{$form['groups']}</td>
				<td></td>
			</tr>
			<tr>
				<td class="field_title">
					<strong class="title">{$this->lang->words['Badges_link']}</strong>
				</td>
				<td class="field_field">{$form['link']} <br>es: http://mydom.com/mytopic</td>
				<td></td>
			</tr>
			<?php } ?>
			<tr>
			    <td class="field_title">
					<strong class="title">{$this->lang->words['Badges_secondary']}</strong>
				</td>
				<td class="field_field">{$form['second']}</td>
				<td></td>
			</tr>
			<tr>
			    <td class="field_title">
					<strong class="title">{$this->lang->words['Badges_forums']}</strong>
				</td>
				<td class="field_field">{$form['forums']}</td>
				<td></td>
			</tr>
		</table>
		<div class="acp-actionbar">
			<input type="hidden" name="type" value="{$type}" />
			<input type="hidden" name="id" id="query_id" value="{$query['ba_id']}" />
			<input type="hidden" name="post_key" id="post_key" value="{$query['post_key']}" />
			<input type="hidden" name="_admin_auth_key" value="{$this->registry->getClass('adminFunctions')->_admin_auth_key}" />
			<input class="button" type="submit" value="{$title}" />
		</div>
	</div>
	<br />
</form>
HTML;
		
		//--endhtml--//
		return $IPBHTML;
	}
	
}