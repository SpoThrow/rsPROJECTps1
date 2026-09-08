<?php
/**
 * @ Application : 		Custom Moderator Team Page v2.0.0
 * @ Last Updated : 	June 13th, 2012 
 * @ Author :			Michael S. Edwards
 * @ Copyright :		(c) 2011 Coding Jungle
 * @ Link	 :			http://www.codingjungle.com/
 */
class cp_skin_groups extends output {
	/* We must declare a destructor */
	public function __destruct() {
	}

	public function MainDisplay(){

		$acp = CP_DIRECTORY;
		
		$groups = $this->registry->getClass('cmtp')->OrderedGroups();

		$html .= "";

		$html .= <<<EOF

			<script src="{$this->settings['board_url']}/{$acp}/applications_addon/other/cmtp/js/acp.groups.js"></script>		

			<script type='text/javascript'>
			
				NoExist = '{$this ->lang->words['cmtp_js_doesNotExist']}';

				NotThere = '{$this->lang->words['cmtp_js_NotThere']}';

				SomeThingBad = '{$this->lang->words['cmtp_js_error']}';

				jsHullo = '{$this->lang->words['cmtp_js_added']}';

				jsToodleLoo = '{$this->lang->words['cmtp_js_deleted']}';

				jsEdit = '{$this->lang->words['cmtp_js_edited']}';

				jsReorder = '{$this->lang->words['cmtp_js_reorder']}';
				
				NoPerms = "{$this->lang->words['cmtp_js_no_perms']}";
				
								
				jQuery(document).ready(function() {

					jQuery("#mdisplay").html(GenAjaxPost('app=cmtp&module=ajax&section=ajax&do=displaygroups',0));

				});

			</script>
			<div class="section_title">
				<h2>{$this->lang->words['cmtp_php_SelectGroups']}</h2>
				<ul class="context_menu">
					<li>
						<a href='{$this->settings['base_url']}module=groups&amp;section=groups&do=recache'>
							Refresh Cache
						</a>
					</li>
				</ul>
			</div>
			<div class="acp-actionbar " style="text-align:left;margin-bottom:10px;">
				<form id="addMgroup">
					<div style="font-size:15px;font-weight:bold;">Add Member to Group</div>
					Name <input class="input_text" type="text" name="name" id="modUserName" size="20" value="" autocomplete="off"> 
EOF;
			$html .= '<select name="Groups" id="Groups" class="dropdown">';
			$html .= '<option value="0">Select A Group</option>';
			if(count($groups)){
				foreach($groups as $k => $c)
				{
					$html .= '<option value="'.$k.'">'.$c['g_title'].'</options>';
				}
			}
			$html .= '</select>';
			$html .=<<<EOF

				</form>
				<br>
				<span class="button" id="addItGroup">
					Add To Group
				</span>	
			</div>
			<script>
				jQuery("#addItGroup").click(function(){
					
					member = jQuery("#modUserName").val();
					
					group = jQuery("#Groups").val();
					
					if(member.length == 0)
					{
						alert("No Member Selected");
						return;
					}	
					
					if( group == 0){
						alert("No Group Selected");
						return;
					}
					
					a = GenAjaxPost('app=cmtp&module=ajax&section=ajax&do=AddMemberToGroup','&member='+member+'&group='+group);

					if(a == "success"){
						jQuery("#modUserName").val("");
						jQuery("#Groups").val("0");
						jQuery("#mdisplay").html(GenAjaxPost('app=cmtp&module=ajax&section=ajax&do=displaygroups',0));
					}
					else if(a == "notexist"){
						alert("Member Doesn't Exist!'");
						return;
					}
					else if(a == "error"){
						alert(SomeThingBad);
						return;
					}
				});
			</script>
			
			<script>
				document.observe("dom:loaded", function(){
					if( $('modUserName') )
					{
						new ipb.Autocomplete( $('modUserName'), { multibox: false, url: acp.autocompleteUrl, templates: { wrap: acp.autocompleteWrap, item: acp.autocompleteItem } } );
					}
				});
			</script>
			
			<div  id="mdisplay"></div>
		
EOF;

		return $html;		
	}

	function displaygroups(){
	
		$acp = CP_DIRECTORY;

		$get_group = $this->registry->getClass('cmtp')->OrderedGroups();
		
		$count = count($get_group);
		
		$counting = $this->registry->getClass('cmtp')->MemberCount();
		
		$uRl = str_replace("app=cmtp&","",$this->settings['base_url']);

		$uRl = str_replace("&amp;","&",$uRl);		
		
		$html .= <<<EOF
		<style type="text/css">
			.NotDisp{
				width:16px;
				height:16px;
				background: url({$this->settings['skin_acp_url']}/images/icons/cross.png);
				cursor:pointer;
			}
			.YesDisp{
				width:16px;
				height:16px;
				background: url({$this->settings['skin_acp_url']}/images/icons/accept.png);
				cursor:pointer;
			}
			.open{
				cursor:pointer;
				color:green;
				font-weight:bold;
			}
			.close{
				cursor:pointer;
				color:red;
				font-weight:bold;
			}
		</style>
		<div class="acp-box" id="theworld">

			<h3>{$this->lang->words['cmtp_php_SelectGroups']}</h3>
				<table class="ipsTable double_pad" id="GroupsSelect">
					<tbody>
						<tr>
							<th width="1%">&nbsp</th>
							<th width="20%">{$this->lang->words['cmtp_php_groupTitle']}</th>
							<th width="38%" align="center" style="text-align:center">{$this->lang->words['cmtp_php_DisplayName']}</th>
							<th width="20%" align="center" style="text-align:center">{$this->lang->words['cmtp_php_displayed']}</th>
							<th width="20%" align="center" style="text-align:center">{$this->lang->words['cmtp_php_members']}</th>
						</tr>				
EOF;
	if(count($get_group)){
		foreach ($get_group as $groups) {
			if($groups['g_title2'])
			{
				$displayName = $groups['g_title2'];
			}
			else {
				$displayName = $groups['g_title'];
			}
			$html .= <<<EOF
							<tr class='ipsControlRow' id='groups_{$groups['g_id']}'>
								<td align='center'>

								<div class="draghandle">&nbsp;</div></td>

								<td>
									<a href='{$uRl}app=members&amp;module=groups&amp;section=groups&amp;do=edit&amp;id={$groups['g_id']}' style='font-weight:bold;color:#3287C9;'>
										{$groups['prefix']}
											{$groups['g_title']}
										{$groups['suffix']}
									</a>
									<small>
										(ID: {$groups['g_id']})
									</small>
								</td>
								<td align='center'>

									<span data-ids="{$groups['g_id']}" data-name="{$displayName}">

									<span style="cursor:pointer;" id="changedisplayname">{$displayName}</span>

									</span>
									<span style="display:none;" id="saveDisplayNameChange">
										[<span style="color:green;font-weight:bold;cursor:pointer;">+</span>]
									</span>
									<span style="display:none;" id="RessettDisplayNameChange">
										[<span style="color:red;font-weight:bold;cursor:pointer;">x</span>]
									</span>

								</td>
								<td align='center'>

						<div id='displayed' data-ids="{$groups['g_id']}" data-count="{$count}" class="YesDisp" ></div>

								</td>
								<td align='center'>
									<span>
										{$counting[$groups['g_id']]}
									</span>
EOF;

						if($counting[$groups['g_id']] >= 1){
								$html .=<<<EOF
									[<span  id="openMembers" data-ids="{$groups['g_id']}"  data-count="{$counting[$groups['g_id']]}" class="open">+</span>]
EOF;
						}
						$html .= <<<EOF
								</td>
							</tr> 							
EOF;
		}
	}

	$get_groups = $this->registry->getClass('cmtp')->NonGroups();

	if(count($get_groups)){
		foreach ($get_groups as $groups) {

			$html .= <<<EOF
							<tr class='ipsControlRow'>
								<td align='center'>

									<div>&nbsp;</div>
									
								</td>

								<td>
									<a href='{$uRl}app=members&amp;module=groups&amp;section=groups&amp;do=edit&amp;id={$groups['g_id']}' style='font-weight:bold;color:#3287C9;'>
										{$groups['prefix']}
											{$groups['g_title']}
										{$groups['suffix']}
									</a>
									<small>
										(ID: {$groups['g_id']})
									</small>
								</td>
								<td align='center'>

								</td>
								<td align='center'>
EOF;

				$html .= <<<EOF
				<div id='displayed' data-ids="{$groups['g_id']}" data-count="{$count}" class="NotDisp" ></div>

								</td>
								<td align='center'>{$counting[$groups['g_id']]}</td>
							</tr>	
EOF;

		}
	}		
		$html .= <<<EOF
				</tbody>
			</table>
		</div>
		<br>
 <script>
jQuery(document).ready(function() {
	jQuery("#theworld").changeDisplayNames();
	jQuery("#theworld").displayed();
	jQuery("#theworld").saveDisplayNameChange();
	jQuery("#theworld").RessettDisplayNameChange();
	jQuery("#theworld").openMembers();
EOF;
if($this->registry->getClass('class_permissions')->checkPermission( "cmtp_can_reorder", "cmtp", "groups" ) == true){
		$html .=<<<EOF
	jQuery('#GroupsSelect').PreviewSort('main', { 
	url: ipb.vars['base_url'].replace(/&amp;/g, '&') + 'app=cmtp&module=ajax&section=ajax&do=doReOrder&md5check=' + ipb.vars['md5_hash'],
	serializeOptions: { key: 'groups[]' }
	} );
EOF;
}
	$html .=<<<EOF
}); 
</script>
EOF;
		return $html;
	}
	
	public function GetMembers($members,$id){
			
		$outofgame = $this->cache->getCache("cmtp_members");
		
		$member = $members;

		$html .= <<<EOF
					<style type="text/css">
						.NotDisps{
							width:16px;
							height:16px;
							background: url({$this->settings['skin_acp_url']}/images/icons/cross.png) no-repeat;
							cursor:pointer;
							float:left;
							margin-left:5px;
 							padding-right:5px;
						}
						.YesDisps{
							width:16px;
							height:16px;
							background: url({$this->settings['skin_acp_url']}/images/icons/accept.png) no-repeat;
							cursor:pointer;
							float:left;
							margin-left:5px;
 							padding-right:5px;
						}

					</style>
EOF;
			
		if(count($members)){
			
			foreach($member as $k => $members){
											
				$html .=<<<EOF
						<div style="width:100px;float:left;">
							<div style="float:left;margin-left:10px;">
								{$members['members_display_name']}
							</div>
							<div id="memberDisplay" data-ids="{$members['member_id']}" data-group="{$id}" data-other="{$members['othergroup']}"
EOF;
				if($outofgame[$id][$members['member_id']] ){
					$html .=<<<EOF
						class="NotDisps"
EOF;
				}
				else{
					$html .=<<<EOF
						class="YesDisps"
EOF;
				}
				$html .=<<<EOF
							></div>
						</div>
EOF;
			}
		}
		
		$html .= <<<EOF
<script>
jQuery(document).ready(function() {
	jQuery("#loadMembers{$id}").memberDisplay();
}); 
</script>
EOF;
		return $html;
	}
}
