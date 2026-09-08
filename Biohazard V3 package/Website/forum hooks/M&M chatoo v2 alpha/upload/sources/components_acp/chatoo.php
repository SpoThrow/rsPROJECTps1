<?php

/*
+---------------------------------------------------------------------------------------------
|
| 	M&M shoutbox System v1.0 by  mohamed 
|	(C)Copyright mohamed 2006
|
+---------------------------------------------------------------------------------------------
+	All credits to mohamed at invisioneyes.com
+ 	must remain in tact or use of this
+	modification is not permitted!
+
+------------------------------------------*/

if( ! defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit;
}

if( file_exists(ROOT_PATH."sources/action_public/mohamedmods/conf_chatoo.php") )
{
	require( ROOT_PATH.'sources/action_public/mohamedmods/conf_chatoo.php' );
	if( count($CHATOO) > 0 )
	{
		foreach( $CHATOO as $item => $value )
		{
			$this->ipsclass->vars[ $item ] = $value;
		}
	}
}
else
{
	echo "You must upload conf_chatoo.php to your sources/action_public/mohamedmods/  folder before using this mod.<br />If the folder does not exist, create it first and then upload this file.";
	exit;
}

class ad_chatoo
{
	var $ipsclass;
	var $parser = "";
	var $html;

	function auto_run()
	{
		$tmp_in = array_merge($_GET, $_POST, $_COOKIE);
		
		foreach( $tmp_in as $k => $v )
		{
			unset($$k);
		}

		$this->ipsclass->admin->nav[] = array( $this->ipsclass->form_code, 'M&M Chatoo System Administration' );

    	require_once( ROOT_PATH."sources/handlers/han_parse_bbcode.php" );
        $this->parser = new parse_bbcode();
        $this->parser->ipsclass =& $this->ipsclass;
        $this->parser->allow_update_caches = 0;

		$this->parser->parse_html    = 1;
		$this->parser->parse_smilies = 1;
		$this->parser->parse_bbcode  = 1;

		$this->ipsclass->DB->simple_construct(array('select' => 'g_id,g_title', 'from' => 'groups'));
		$this->ipsclass->DB->simple_exec();
		while ($g = $this->ipsclass->DB->fetch_row())
		{
			$this->groups[] = array($g['g_id'], $g['g_title']);
		}

		switch( $this->ipsclass->input['code'] )
		{
			case 'settings':
				$this->settings1();
				break;
			case 'do_settings':
				$this->settings2();
				break;
			case 'updatecache':
				$this->do_cache_update();
				break;
			case 'close':
				$this->show_close_form();
				break;
			case 'doclose':
				$this->do_close();
				break;
			case 'lays':
				$this->layouts();
				break;
			case 'addlay':
				$this->layout_form('add');
				break;
			case 'editlay':
				$this->layout_form('edit');
				break;
			case 'dolay':
				$this->layout_process();
				break;
			case 'dellay':
				$this->delete_layout();
				break;
			case 'dodellay':
				$this->do_delete_layout();
				break;
			case 'gcperms':
				$this->glayout_perms();
				break;
			case 'editgcperms':
				$this->edit_glayout_perms();
				break;
			case 'doeditgcperms':
				$this->do_edit_glayout_perms();
				break;
			case 'gperms':
				$this->edit_gperms();
				break;
			case 'dogperms':
				$this->do_edit_gperms();
				break;
			default:
				$this->settings1();
				break;
		}
	}
	function show_splash()
	{
		$this->ipsclass->admin->page_title = "M&M Chatoo System Main Page";
		$this->ipsclass->admin->page_detail = "You can manage your Chatoo System from this section.";
		if (!$this->ipsclass->cache['Chatoo_cache'] || !is_array($this->ipsclass->cache['Chatoo_cache']))
		{
			$this->ipsclass->cache['Chatoo_cache'] = $this->mo->update_shout_cache();
		}
$hidden=array();
$toprates=array();
		$query1 = $this->ipsclass->DB->query( "SELECT COUNT(id) as ids FROM ".SQL_PREFIX."chatoo WHERE open='1' ORDER BY id ASC" );

		$out['shouts'] = $this->ipsclass->cache['chatoo_cache']['tot_shouts'];
		$out['lays'] = $query1['ids'];





		//------------------------------------------------

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form( array(1 => array( 'code'  , 'updatecache' ),
																			2 => array( 'act'   , 'chatoo'   ),
																			3 => array( 'section', $this->ipsclass->section_code ),
																	)      );

		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "30%");
		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "70%");

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table( "Chatoo System Quick Info" );
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "<b>Total Shouts</b>", $out['shouts'] ) );
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "<b>Total Layouts</b>", $out['lays'] ) );
		$this->ipsclass->html .= $this->ipsclass->adskin->end_form( "Update Cache" );
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();

		//------------------------------------------------

		$this->ipsclass->adskin->td_header[] = array( "ID"        , "20%" );
		$this->ipsclass->adskin->td_header[] = array( "Layout"    , "60%" );
		$this->ipsclass->adskin->td_header[] = array( "Used By" , "20%" );

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table( "Hidden/Pending Cocktails" );

		$query2 = $this->ipsclass->DB->query( "SELECT * FROM ".SQL_PREFIX."chatoo  WHERE open='1'  ORDER BY id ASC" );
		while( $row = $this->ipsclass->DB->fetch_row( $query2 ) )
		{
		$so = $this->ipsclass->DB->query( "SELECT COUNT(id) as ids FROM ".SQL_PREFIX."members WHERE lay=".$row['id']);

				$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array($row['id'] ,
												                 $row['title'],$so['ids'])      );
			
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();

		$this->ipsclass->admin->output();
	}

	function settings1()
	{


	    $max = @ini_get('upload_max_filesize');


		$this->ipsclass->admin->page_title = "Chatoo System Settings";
		$this->ipsclass->admin->page_detail = "You can configure your Chatoo System from this page.";

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form( array( 	1 => array( 'code'  , 'do_settings' ),
												2 => array( 'act'   , 'chatoo' ),
									 			3 => array( 'open_close' , $this->ipsclass->vars['open_close'] ),
												4 => array( 'section', $this->ipsclass->section_code ),
																	)		);

		$this->ipsclass->adskin->td_header[] = array( "&nbsp;" , "60%" );
		$this->ipsclass->adskin->td_header[] = array( "&nbsp;" , "40%" );
		$this->ipsclass->html .= $this->ipsclass->adskin->start_table( "Chatoo System Configuration" );

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "<b> Enable Adding Shouts to shoutbox?</b>", $this->ipsclass->adskin->form_yes_no("m_shout_add", $this->ipsclass->vars['m_shout_add'] )
																	)		);

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "<b>Maximum Allowed Shout length</b><div class='greytext' > in characters</div>", $this->ipsclass->adskin->form_input("max_let", $this->ipsclass->vars['max_let'] )
																	)		);

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "<b>What is the shouts limit in database cutoff?</b><div class='greytext'>every new shout it will check if shouts in database exceeded this amount and then will delete old shouts till that number is remained, to turn off just leave blank or enter 0 (100 is recommended)</div>", $this->ipsclass->adskin->form_input("sanity", $this->ipsclass->vars['sanity'] )
																	)		);
		$this->ipsclass->html .= $this->ipsclass->adskin->end_form("Update Configuration");
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();

		$this->ipsclass->admin->output();
	}

	function settings2()
	{
		$content = "<"."?php\n";

		$content .= "\$CHATOO['m_shout_add']	= \"{$this->ipsclass->input['m_shout_add']}\";\n";
		$content .= "\$CHATOO['max_let']	= \"{$this->ipsclass->input['max_let']}\";\n";
		$content .= "\$CHATOO['sanity']	= \"{$this->ipsclass->input['sanity']}\";\n";

		$content .= "\n?".">\n";

		if( is_writable(ROOT_PATH."sources/action_public/mohamedmods/conf_chatoo.php") )
		{
	 		if( $fh = @fopen(ROOT_PATH."sources/action_public/mohamedmods/conf_chatoo.php", "w") )
			{
		 		@fputs($fh, $content, strlen($content) );
		 		@fclose($fh);
	 		}
		}
		else
		{
			$this->ipsclass->admin->error( "Fatal Error: Could not open conf_chatoo.php for writing - no changes applied. Try changing the CHMOD to 0777" );
			exit;
		}
		
		$this->ipsclass->admin->save_log( "Chatoo System Configuration Updated" );
		$this->ipsclass->admin->done_screen( "Chatoo System Configuration Updated", "Chatoo System Administration", $this->ipsclass->form_code );
		exit;
	}


	function gcat_perms()
	{
		global $ibforums, $DB, $std;

		$grps = array();
		$this->ipsclass->DB->simple_construct(array('select' => 'count(id) as total, mgroup', 'from' => 'members', 'where' => 'id > 0 GROUP BY mgroup'));
		$this->ipsclass->DB->simple_exec();
		while ($r = $this->ipsclass->DB->fetch_row())
		{
			$grps[$r['mgroup']] = $r['total'];
		}

		$this->ipsclass->admin->page_title = "cocktails System: Group Cat Perms";
		$this->ipsclass->admin->page_detail = "This Page Allows You To Edit Group Category Permissions.";
		$this->ipsclass->admin->nav[] = array('section={$this->ipsclass->section_code}&act=cocktails&code=gcperms', 'Group Cat Perms</a>');

		$this->ipsclass->adskin->td_header[] = array("Group Name", "60%");
		$this->ipsclass->adskin->td_header[] = array("Members", "20%");
		$this->ipsclass->adskin->td_header[] = array("Options", "20%");

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table("cocktails System: Groups");
		$this->ipsclass->DB->simple_construct(array('select' => 'g_id, g_title', 'from' => 'groups', 'order' => 'g_title ASC, g_id ASC'));
		$this->ipsclass->DB->simple_exec();
		while ($r = $this->ipsclass->DB->fetch_row())
		{
			$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array("<b>{$r['g_title']}</b>", "<center>".$this->ipsclass->do_number_format($grps[$r['g_id']])."</center>", "<center><span class='fauxbutton'><a href='{$this->ipsclass->base_url}&{$this->ipsclass->form_code}&code=editgcperms&gid={$r['g_id']}' title='Edit Group Category Permissions'>Edit Perms</a></span></center>"));
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();
		$this->ipsclass->admin->output();
	}

	function edit_gcat_perms()
	{

		$gid = intval($this->ipsclass->input['gid']);
		if (!$gid || $gid <= 0)
		{
			$this->ipsclass->admin->error("Please Enter A Valid Group ID To Edit The Category Permissions For.");
		}

		$g = $this->ipsclass->DB->simple_exec_query(array('select' => 'g_id, g_title', 'from' => 'groups', 'where' => 'g_id='.$gid));
		if (!$this->ipsclass->DB->get_num_rows())
		{
			$this->ipsclass->admin->error("That Group Doesn't Exist!");
		}

		$this->ipsclass->html .= "
			<script type='text/javascript'>
			var names = new Array('DISP', 'VIEW', 'SUBM', 'COMM');

			function multi_check(t, n, c)
			{
				var obj = document.forms['theAdminForm'];
				if (t == 0)
				{
					var nm;
					var ttl = tbox = 0;

					for (var i=0; i<names.length; i++)
					{
						nm = names[i]+'_'+n;
						eval('obj.elements[nm].checked = c');
					}
				}
				else if (t == 1)
				{
					for (var i=0; i<obj.elements.length; i++)
					{
						var e = obj.elements[i];
						if (e.type == 'checkbox' && !e.disabled)
						{
							var en = e.name.substring(0, 4);
							if (en == names[n])
							{
								e.checked = c;
							}
						}
					}
				}
			}
			</script>";

		$this->ipsclass->admin->page_title = "cocktails System: Editing Group Cat Perms";
		$this->ipsclass->admin->page_detail = "This Page Allows You To Edit Group Category Permissions.";
		$this->ipsclass->admin->nav[] = array('section='.$this->ipsclass->section_code.'&act=cocktails&code=gcperms', 'Group Cat Perms</a>');

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form( array( 	1 => array( 'code'  , 'doeditgcperms' ),
												2 => array( 'act'   , 'cocktails' ),
									 			3 => array( 'gid' , $gid ),
												4 => array( 'section', $this->ipsclass->section_code ),
																	)		);

		$this->ipsclass->adskin->td_header[] = array("Category Name"     , "40%");
		$this->ipsclass->adskin->td_header[] = array("Display Cat."   , "10%");
		$this->ipsclass->adskin->td_header[] = array("View cocktails"     , "20%");
		$this->ipsclass->adskin->td_header[] = array("Submit cocktails"   , "20%");
		$this->ipsclass->adskin->td_header[] = array("Add Comments"   , "10%");

		$cat_data = $this->grab_cat_data();
		$this->ipsclass->html .= $this->ipsclass->adskin->start_table("Category Permissions For Group: ".$g['g_title']);
		foreach ($cat_data as $id => $r)
		{
			$s1 = $s2 = $s3 = $s4 = $global = "";

			$global = "<center id='mgyellow'><i>Global</i></center>";
			if ($r['display_perms'] == '*')
			{
				$s1 = $global;
			}
			else if (preg_match("/(^|,)".$gid."(,|$)/", $r['display_perms']))
			{
				$s1 = "<center id='mgyellow'><input type='checkbox' name='DISP_".$r['cid']."' value='1' checked></center>";
			}
			else
			{
				$s1 = "<center id='mgyellow'><input type='checkbox' name='DISP_".$r['cid']."' value='1'></center>";
			}

			$global = "<center id='mgblue'><i>Global</i></center>";
			if ($r['view_perms'] == '*')
			{
				$s2 = $global;
			}
			else if (preg_match("/(^|,)".$gid."(,|$)/", $r['view_perms']))
			{
				$s2 = "<center id='mgblue'><input type='checkbox' name='VIEW_".$r['cid']."' value='1' checked></center>";
			}
			else
			{
				$s2 = "<center id='mgblue'><input type='checkbox' name='VIEW_".$r['cid']."' value='1'></center>";
			}

			$global = "<center id='mggreen'><i>Global</i></center>";
			if ($r['submit_perms'] == '*')
			{
				$s3 = $global;
			}
			else if (preg_match("/(^|,)".$gid."(,|$)/", $r['submit_perms']))
			{
				$s3 = "<center id='mggreen'><input type='checkbox' name='SUBM_".$r['cid']."' value='1' checked></center>";
			}
			else
			{
				$s3 = "<center id='mggreen'><input type='checkbox' name='SUBM_".$r['cid']."' value='1'></center>";
			}

			$global = "<center id='memgroup'><i>Global</i></center>";
			if ($r['comment_perms'] == '*')
			{
				$s4 = $global;
			}
			else if (preg_match("/(^|,)".$gid."(,|$)/", $r['comment_perms']))
			{
				$s4 = "<center id='memgroup'><input type='checkbox' name='COMM_".$r['cid']."' value='1' checked></center>";
			}
			else
			{
				$s4 = "<center id='memgroup'><input type='checkbox' name='COMM_".$r['cid']."' value='1'></center>";
			}

			if ($r['sub'] == 0)
			{
				$css = 'pformstrip';
			}
			else
			{
				$css = '';
			}

			$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array("<div style='float:left'><b>{$r['cname']}</b></div><div style='float:right'><input type='button' id='button' value='+' onclick='multi_check(0, {$r['cid']}, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(0, {$r['cid']}, false)' /></div>", $s1, $s2, $s3, $s4), $css);
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array(
									"&nbsp;",
									"<center><input type='button' id='button' value='+' onclick='multi_check(1, 0, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(1, 0, false)' /></center>",
									"<center><input type='button' id='button' value='+' onclick='multi_check(1, 1, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(1, 1, false)' /></center>",
									"<center><input type='button' id='button' value='+' onclick='multi_check(1, 2, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(1, 2, false)' /></center>",
									"<center><input type='button' id='button' value='+' onclick='multi_check(1, 3, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(1, 3, false)' /></center>",
								), 'tdrow2');

		$this->ipsclass->html .= $this->ipsclass->adskin->end_form("Update Category Permissions");
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();
		$this->ipsclass->admin->output();
	}

	function do_edit_gcat_perms()
	{

		$gid = intval($this->ipsclass->input['gid']);
		if (!$gid || $gid <= 0)
		{
			$this->ipsclass->admin->error("Please Enter A Valid Group ID To Edit The Category Permissions For.");
		}

		$g = $this->ipsclass->DB->simple_exec_query(array('select' => 'g_id, g_title', 'from' => 'groups', 'where' => 'g_id='.$gid));
		if (!$this->ipsclass->DB->get_num_rows())
		{
			$this->ipsclass->admin->error("That Group Doesn't Exist!");
		}

		$this->ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'cocktails_cats', 'order' => 'position ASC'));
		$q = $this->ipsclass->DB->simple_exec();
		while ($r = $this->ipsclass->DB->fetch_row($q))
		{
			$perms = unserialize(stripslashes($r['group_perm']));
			$s1 = $s2 = $s3 = $s4 = "";

			if ($perms['display_perms'] == '*')
			{
				$s1 = '*';
			}
			else
			{
				$s1_ids = explode(",", $perms['display_perms']);
				if (is_array($s1_ids))
				{
					foreach ($s1_ids as $i)
					{
						if ($gid == $i)
						{
							continue;
						}
						else
						{
							$s1 .= $i.",";
						}
					}
				}

				if ($this->ipsclass->input['DISP_'.$r['cid']] == 1)
				{
					$s1 .= $gid.",";
				}

				$s1 = preg_replace("/,$/", "", $s1);
				$s1 = preg_replace("/^,/", "", $s1);
			}

			if ($perms['view_perms'] == '*')
			{
				$s2 = '*';
			}
			else
			{
				$s2_ids = explode(",", $perms['view_perms']);
				if (is_array($s2_ids))
				{
					foreach ($s2_ids as $i)
					{
						if ($gid == $i)
						{
							continue;
						}
						else
						{
							$s2 .= $i.",";
						}
					}
				
				}

				if ($this->ipsclass->input['VIEW_'.$r['cid']] == 1)
				{
					$s2 .= $gid.",";
				}
				
				$s2 = preg_replace("/,$/", "", $s2);
				$s2 = preg_replace("/^,/", "", $s2);
			}


			if ($perms['submit_perms'] == '*')
			{
				$s3 = '*';
			}
			else
			{
				$s3_ids = explode(",", $perms['submit_perms']);
				if (is_array($s3_ids))
				{
					foreach ($s3_ids as $i)
					{
						if ($gid == $i)
						{
							continue;
						}
						else
						{
							$s3 .= $i.",";
						}
					}
				
				}

				if ($this->ipsclass->input['SUBM_'.$r['cid']] == 1)
				{
					$s3 .= $gid.",";
				}
				
				$s3 = preg_replace("/,$/", "", $s3);
				$s3 = preg_replace("/^,/", "", $s3);
			}

			if ($perms['comment_perms'] == '*')
			{
				$s4 = '*';
			}
			else
			{
				$s4_ids = explode(",", $perms['comment_perms']);
				if (is_array($s4_ids))
				{
					foreach ($s4_ids as $i)
					{
						if ($gid == $i)
						{
							continue;
						}
						else
						{
							$s4 .= $i.",";
						}
					}
				
				}

				if ($this->ipsclass->input['COMM_'.$r['cid']] == 1)
				{
					$s4 .= $gid.",";
				}
				
				$s4 = preg_replace("/,$/", "", $s4);
				$s4 = preg_replace("/^,/", "", $s4);
			}

			$this->ipsclass->DB->do_update('cocktails_cats', array('group_perm' => addslashes(serialize(array(
													'display_perms'  => $s1,
													'view_perms'     => $s2,
													'submit_perms'   => $s3,
													'comment_perms'  => $s4,
												     )))), 'cid='.$r['cid']);
		}

		define('IN_SCRIPT', 1);
		$this->do_cache_update();
	$this->ipsclass->admin->redirect('section='.$this->ipsclass->section_code.'&act=cocktails&code=gcperms', "Successfully Updated Group: ".$g['g_title'], 0, 1);
			
	}

	function grab_cat_data()
	{
		global $ibforums, $DB;

		$cats = array();
		$this->ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'cocktails_cats', 'order' => 'position ASC'));
		$this->ipsclass->DB->simple_exec();
		while ($data = $this->ipsclass->DB->fetch_row())
		{
			$perms = unserialize(stripslashes($data['group_perm']));
			$data['view_perms']     = $perms['view_perms'];
			$data['submit_perms']   = $perms['submit_perms'];
			$data['comment_perms']  = $perms['comment_perms'];
			$data['display_perms']  = $perms['display_perms'];

			$first[$data['sub']][$c['cid']] = $c['cname'];
			$cats[$data['cid']] = $data;
		}

		$newcats = array();
		foreach ($cats as $cid => $cd)
		{
			$level = $this->calc_level($first, $cd['sub']);
			$out = "";
			for ($i=0; $i<$level; $i++)
			{
				$out .= "--";
			}

			$cd['cname'] = $out.$cd['cname'];
			$newcats[$cid] = $cd;
		}

		return $newcats;
	}

	function calc_level($tbl, $par, $lvl=0)
	{
		global $ibforums;

		if ($par <= 0)
		{
			return $lvl;
		}

		$d = $tbl[$par];
		if ($d != '')
		{
			$lvl = $this->calc_level($tbl, $d['sub'], $lvl+1);
		}

		return $lvl;
	}

	function categories()
	{
		global $ibforums, $DB, $std;

		$this->ipsclass->admin->page_title = "cocktails System: Categories";
		$this->ipsclass->admin->page_detail = "This Page Lists All Your Available Categories.";
		$this->ipsclass->admin->nav[] = array('section={$this->ipsclass->section_code}&act=cocktails&code=cats', 'Categories</a>');

		$this->ipsclass->adskin->td_header[] = array("Category", "70%");
		$this->ipsclass->adskin->td_header[] = array("Options", "30%");

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table("cocktails System: Categories");
		$this->ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'cocktails_cats'));
		$this->ipsclass->DB->simple_exec();
		if ($this->ipsclass->DB->get_num_rows())
		{
			while ($c = $this->ipsclass->DB->fetch_row())
			{
				if ($c['cid'] <= 0)
				{
					continue;
				}

				$first[$c['sub']][$c['cid']] = $c['cname'];
			}

			$this->list_cats(0, $first);
		}
		else
		{
			$this->ipsclass->html .= $this->ipsclass->adskin->add_td_basic("<div align='center'><b>There Are No Categories Available.</b></div>", 'center', 'tdrow1');
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_basic("<div align='center'><input type='button' value='Add Category' id='button' onclick=\"window.location='{$this->ipsclass->base_url}&{$this->ipsclass->form_code}&code=addcat'\"></div>", 'center', 'pformstrip');
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();
		$this->ipsclass->admin->output();
	}

	function list_cats($pcat, $tbl, $lvl=0, $mlvl=0)
	{
		global $ibforums;

		$list = $tbl[$pcat];
		if ($list == '' || !count($list))
		{
			return;
		}

		if (count($list))
		{
			foreach ($list as $k => $v)
			{
				$output = '';
				for ($i=0; $i<$lvl; $i++)
				{
					$output .= "--";
				}

				$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array($output." ".$v, "<div align='center'><span class='fauxbutton'><a href='{$this->ipsclass->base_url}&{$this->ipsclass->form_code}&code=editcat&id={$k}'>Edit</a></span><span class='fauxbutton'><a href='{$this->ipsclass->base_url}&{$this->ipsclass->form_code}&code=delcat&id={$k}'>Delete</a></span></div>"));
				$this->list_cats($k, $tbl, $lvl+1, $mlvl);
			}
		}
	}

	function category_form($type="add")
	{

		$forum_array = $this->forumfunc->ad_forums_forum_list(1);
		array_unshift($forum_array, array(0 , 'None (No Topics)'));

		$catss = array();
		$this->ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'cocktails_cats'));
		$this->ipsclass->DB->simple_exec();
		if ($this->ipsclass->DB->get_num_rows())
		{
			while ($c = $this->ipsclass->DB->fetch_row())
			{
				$catss[$c['sub']][$c['cid']] = $c['cname'];
			}
		}

		$catss = $this->sort_cats($catss);
		array_unshift($catss, array(0 , 'None (Root Cat)'));

		$button = "Add Category";
		$this->ipsclass->admin->page_title = "cocktails System: Add Category";
		$this->ipsclass->admin->page_detail = "This Page Allows You To Add A New Category To The cocktails System.";

		if ($type == 'edit')
		{
			if ($this->ipsclass->input['id'] == '')
			{
				$this->ipsclass->admin->error("Please Provide A Valid Category ID To Edit.");
			}

			$button = "Edit Category";
			$this->ipsclass->admin->page_title = "cocktails System: Edit Category";
			$this->ipsclass->admin->page_detail = "This Page Allows You To Edit A Category Already Available In The cocktails System.";

			$data = $this->ipsclass->DB->simple_exec_query(array('select' => '*', 'from' => 'cocktails_cats', 'where' => 'cid='.intval($this->ipsclass->input['id'])));
			if (!$this->ipsclass->DB->get_num_rows())
			{
				$this->ipsclass->admin->error("The Category You Are Wanting To Edit Doesn't Exist.");
			}

			$perms = unserialize(stripslashes($data['group_perm']));
			$data['view_perms']     = $perms['view_perms'];
			$data['submit_perms']   = $perms['submit_perms'];
			$data['comment_perms']  = $perms['comment_perms'];
			$data['display_perms']  = $perms['display_perms'];
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form(array(0 => array('section', $this->ipsclass->section_code), 1 => array('act', 'cocktails'), 2 => array('code', 'docat'), 3 => array('type', $type), 4 => array('id', $this->ipsclass->input['id'])));

		//-------------------------------------------------------------
		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "40%");
		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "60%");
		//-------------------------------------------------------------

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table("Category Basics");
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array("<b>Category Name:</b>", $this->ipsclass->adskin->form_input('cname', $data['cname'])));
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array("<b>Category Description:</b>", $this->ipsclass->adskin->form_textarea('desc',$this->ipsclass->txt_raw2form( $this->parser->pre_edit_parse( $row['desc'] )))));
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array("<b>Is This Category Open?</b>", $this->ipsclass->adskin->form_yes_no('open', ($type != 'edit') ? 1 : $data['open'])));
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array("<b>Parent Category:</b>", $this->ipsclass->adskin->form_dropdown('sub', $catss, $data['sub'])));
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();


		//-------------------------------------------------------------
		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "40%");
		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "60%");
		//-------------------------------------------------------------

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table("Category Miscellaneous Options");
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array("<b>cocktails Need Admin Approval Before Being Available?</b><br><i>If The Global Setting Is Yes Or No, This Will Be Ignored.</i>", $this->ipsclass->adskin->form_yes_no('authorize', $data['authorize'])));
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array("<b>Select Which Forum To Create Topics For Submitted cocktails In:</b><br><i>If The Global Setting Is Not 'Per-Category', This Will Be Ignored.</i>", $this->ipsclass->adskin->form_dropdown('fordaforum', $forum_array, $data['fordaforum'])));
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();


		//-------------------------------------------------------------

		$this->ipsclass->adskin->td_header[] = array("Group Name"     , "40%");
		$this->ipsclass->adskin->td_header[] = array("Display Cat."   , "10%");
		$this->ipsclass->adskin->td_header[] = array("View cocktails"     , "20%");
		$this->ipsclass->adskin->td_header[] = array("Submit cocktails"   , "20%");
		$this->ipsclass->adskin->td_header[] = array("Add Comments"   , "10%");

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table("Category Group Permissions");
		$this->ipsclass->html .= $this->group_perms($data['display_perms'], $data['view_perms'], $data['submit_perms'], $data['comment_perms']);

		$this->ipsclass->html .= $this->ipsclass->adskin->end_form($button);
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();
		$this->ipsclass->admin->output();
	}

	function sort_cats($cats, $pcat=0, $lvl=0, $ncats=array())
	{
		$list = $cats[$pcat];
		if ($list == '')
		{
			return $ncats;
		}

		while (list($k, $v) = @each($list))
		{
			list ($v1, $v2) = $v;

			$output = '';
			for ($i=0; $i<$lvl; $i++)
			{
				$output .= "--";
			}

			$ncats[] = array($k, $output.$v);
			$ncats = $this->sort_cats($cats, $k, $lvl+1, $ncats);
		}

		return $ncats;
	}

	function category_process()
	{
		$type = ($this->ipsclass->input['type'] == 'edit') ? 'edit' : 'add';
		if ($type == 'edit')
		{
			$this->ipsclass->input['id'] = intval($this->ipsclass->input['id']);
			if (!$this->ipsclass->input['id'] || $this->ipsclass->input['id'] <= 0)
			{
				$this->ipsclass->admin->error("Please Enter A Valid Category ID To Edit.");
			}

			$this->ipsclass->DB->simple_exec_query(array('select' => 'cid', 'from' => 'cocktails_cats', 'where' => 'cid='.$this->ipsclass->input['id']));
			if (!$this->ipsclass->DB->get_num_rows())
			{
				$this->ipsclass->admin->error("The Category You Are Trying To Edit Doesn't Exist.");
			}
		}

		$this->ipsclass->input['cname'] = trim($this->ipsclass->input['cname']);
		if ($this->ipsclass->input['cname'] == '')
		{
			$this->ipsclass->admin->error("Please Enter A Valid Category Name.");
		}

		if ($this->ipsclass->input['sub'] == 0 || $this->ipsclass->input['sub'] == '')
		{
			$this->ipsclass->DB->simple_exec_query(array('select' => 'cname', 'from' => 'cocktails_cats', 'where' => "cname='".$this->ipsclass->input['cname']."' AND sub=0"));
		}
		else
		{
			$this->ipsclass->DB->simple_exec_query(array('select' => 'cname', 'from' => 'cocktails_cats', 'where' => "cname='".$this->ipsclass->input['cname']."' AND sub='".$this->ipsclass->input['sub']."'"));
		}

		if ($type != 'edit' && $this->ipsclass->DB->get_num_rows() > 0)
		{
			$this->ipsclass->admin->error("Sorry, Can't Create Duplicate Category Names In The Same Root Or Parent Category.");
		}

		$perms = $this->compile_perms();
		$gperm = addslashes(serialize(array(
							'submit_perms'   => $perms['SUBM'],
							'view_perms'     => $perms['VIEW'],
							'comment_perms'  => $perms['COMM'],
							'display_perms'  => $perms['DISP']
				   )         )     );
		$desc = $this->ipsclass->remove_tags( $this->ipsclass->input['desc'] );

		$desc = $this->parser->pre_db_parse( $desc );
		$cnotes = $this->ipsclass->remove_tags( $this->ipsclass->input['cnotes'] );
		$cnotes = $this->parser->pre_db_parse( $cnotes );
		$query = array(
				'sub'           => $this->ipsclass->input['sub'],
				'cname'         => trim($this->ipsclass->input['cname']),
				'`desc`'          => trim($desc),
				'open'          => $this->ipsclass->input['open'],
				'authorize'     => $this->ipsclass->input['authorize'],
				'fordaforum'    => $this->ipsclass->input['fordaforum'],
				'group_perm'    => $gperm,

			      );

		if ($type == 'edit')
		{
			$this->ipsclass->DB->do_update('cocktails_cats', $query, 'cid='.$this->ipsclass->input['id']);
		define('IN_SCRIPT',1);
			$this->do_cache_update();


			$this->ipsclass->admin->save_log("cocktails System: Updated Category ({$this->ipsclass->input['cname']})");
			$this->ipsclass->admin->redirect('section='.$this->ipsclass->section_code.'&act=cocktails&code=cats', "Successfully Updated Category: ".$this->ipsclass->input['cname'], 0, 1);
		}
		else
		{
			$this->ipsclass->DB->do_insert('cocktails_cats', $query);
		define('IN_SCRIPT',1);
			$this->do_cache_update();

			$this->ipsclass->admin->save_log("cocktails System: Added Category ({$this->ipsclass->input['cname']})");
			$this->ipsclass->admin->redirect('section='.$this->ipsclass->section_code.'&act=cocktails&code=cats', "Successfully Added Category: ".$this->ipsclass->input['cname'], 0, 1);
		}
	}

	function reorder_cats()
	{
		global $ibforums, $DB, $std;

		$this->ipsclass->admin->page_title = "cocktails System: Reorder Categories";
		$this->ipsclass->admin->page_detail  = "The Page Allows You To Reorder Your Categories In A Custom Manner To Suit Your Needs.";

		$this->ipsclass->adskin->td_header[] = array("Category Name", "100%");
		$this->ipsclass->html .= $this->ipsclass->adskin->start_form(array(0 => array('section', $this->ipsclass->section_code), 1 => array('act', 'cocktails'), 2 => array('code', 'doreorder')));
		$this->ipsclass->html .= $this->ipsclass->adskin->start_table("cocktails System Categories");

		$this->ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'cocktails_cats', 'order' => 'position, cid ASC'));
		$this->ipsclass->DB->simple_exec();
		if ($this->ipsclass->DB->get_num_rows())
		{
			while ($c = $this->ipsclass->DB->fetch_row())
			{
				if ($c['cid'] <= 0)
				{
					continue;
				}

				$first[$c['sub']][$c['cid']] = array($c['cname'], $c['position']);
			}

			if (count($first))
			{
				$this->list_rocats(0, $first);
			}
		}
		else
		{
			$this->ipsclass->html .= $this->ipsclass->adskin->add_td_basic("<div align='center'><b>There Are No Categories Available To Reorder.</b></div>", 'center', 'tdrow1');
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->end_form('Reorder Categories');
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();
		$this->ipsclass->admin->output();
	}

	function list_rocats($pcat, $tbl, $lvl=0)
	{
		global $ibforums;

		$list = $tbl[$pcat];
		if ($list == '' || !is_array($list) || !count($list))
		{
			return;
		}

		$oings = array();
		if (count($list))
		{
			for ($i=1; $i<=count($list); $i++)
			{
				$oings[] = array($i, $i);
			}
		}

		if (count($list))
		{
			foreach ($list as $k => $v)
			{
				$output = '';
				if ($lvl > 0)
				{
					for ($i=0; $i<$lvl; $i++)
					{
						$output .= "&nbsp; &nbsp; &nbsp;";
					}
				}

				$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row(array($output.$this->ipsclass->adskin->form_dropdown('ro_'.$k, $oings, $v[1])." ".$v[0]));
				$this->list_rocats($k, $tbl, $lvl+1);
			}
		}

		return true;
	}

	function do_reorder_cats()
	{
		global $DB, $ibforums, $std;

		$this->ipsclass->DB->simple_construct(array('select' => 'cid', 'from' => 'cocktails_cats'));
		$q = $this->ipsclass->DB->simple_exec();
		while ($r = $this->ipsclass->DB->fetch_row($q))
		{
			$this->ipsclass->DB->do_update('cocktails_cats', array('position' => $this->ipsclass->input['ro_'.$r['cid']]), 'cid='.$r['cid']);
		}

		define('IN_SCRIPT',1);
		$this->do_cache_update();

		$this->ipsclass->admin->save_log("Artists System:  Reorderd Categories");
		$this->ipsclass->admin->redirect('section='.$this->ipsclass->section_code.'&act=cocktails&code=reorder', "Successfully Reordered The Categories", 0, 1);
	}

	function delete_cat()
	{

		$catss = array();
		$this->ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'cocktails_cats'));
		$this->ipsclass->DB->simple_exec();
		if ($this->ipsclass->DB->get_num_rows())
		{
			while ($c = $this->ipsclass->DB->fetch_row())
			{
				$catss[$c['sub']][$c['cid']] = $c['cname'];
			}
		}

		$catsf = $this->sort_cats($catss);
		$catsp = $this->sort_cats($catss);
		array_unshift($catsf, array(0, 'None (Delete Cocktails)'));
		array_unshift($catsp, array(0, 'None (Root Cat)'));

		$this->ipsclass->admin->page_title = "cocktails System: Delete Category";
		$this->ipsclass->admin->page_detail = "This Page Allows You To Delete A Category From The cocktails System.";

		if ($this->ipsclass->input['id'] == '')
		{
			$this->ipsclass->admin->error("Please Provide A Valid Category ID To Delete.");
		}

		$data = $this->ipsclass->DB->simple_exec_query(array('select' => '*', 'from' => 'cocktails_cats', 'where' => 'cid='.$this->ipsclass->input['id']));
		if (!$this->ipsclass->DB->get_num_rows())
		{
			$this->ipsclass->admin->error("The Category You Are Wanting To Delete Doesn't Exist.");
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form(array(0 => array('section', $this->ipsclass->section_code), 1 => array('act', 'cocktails'), 2 => array('code', 'dodelcat'), 3 => array('id', $this->ipsclass->input['id'])));
		$this->ipsclass->html .= $this->ipsclass->adskin->start_table("cocktails System:  Delete Category");
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_basic("<div align='center'><b>Are You Sure You Want To Delete The Category:&nbsp; {$data['cname']}</b></div>", 'center', '{class}');

		$nc = 0;
		$this->ipsclass->DB->simple_exec_query(array('select' => 'cid', 'from' => 'cocktails_cats', 'where' => 'sub='.$data['cid']));
		if ($this->ipsclass->DB->get_num_rows())
		{
			$nc = 1;
			$this->ipsclass->html .= $this->ipsclass->adskin->add_td_basic("<div align='center'><b>Select A New Parent Category For The Sub Cats In This Category:&nbsp;</b> ".$this->ipsclass->adskin->form_dropdown('parcat', $catsp)."</div>", 'center', 'tdrow1');
		}

		$this->ipsclass->DB->simple_exec_query(array('select' => 'cat', 'from' => 'cocktails', 'where' => 'cat='.$data['cid']));
		if ($this->ipsclass->DB->get_num_rows())
		{
			$nc = 1;
			$this->ipsclass->html .= $this->ipsclass->adskin->add_td_basic("<div align='center'><b>Which Category To Move The Existing artists In This Category Too:&nbsp;</b> ".$this->ipsclass->adskin->form_dropdown('newcat', $catsf)."</div>", 'center', 'tdrow1');
		}

		$this->ipsclass->html = str_replace('{class}', ($nc == 1) ? 'pformstrip' : 'tdrow1', $this->ipsclass->html);
		$this->ipsclass->html .= $this->ipsclass->adskin->end_form('Delete Category');
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();
		$this->ipsclass->admin->output();
	}

	function do_delete_cat()
	{

		$this->ipsclass->input['id'] = intval($this->ipsclass->input['id']);
		$this->ipsclass->input['parcat'] = intval($this->ipsclass->input['parcat']);
		$this->ipsclass->input['newcat'] = intval($this->ipsclass->input['newcat']);

		if (!$this->ipsclass->input['id'] || $this->ipsclass->input['id'] <= 0)
		{
			$this->ipsclass->admin->error("Please Enter A Valid Category ID To Delete.");
		}

		$r = $this->ipsclass->DB->simple_exec_query(array('select' => '*', 'from' => 'cocktails_cats', 'where' => 'cid='.$this->ipsclass->input['id']));
		if (!$this->ipsclass->DB->get_num_rows())
		{
			$this->ipsclass->admin->error("The Category You Are Wanting To Delete Doesn't Exist.");
		}

		$subids = $this->get_sub_catids($r['cid']);
		$subids[] = $r['cid'];
		if ($this->ipsclass->input['parcat'] > 0 && in_array($this->ipsclass->input['parcat'], $subids))
		{
			$this->ipsclass->admin->error("You Can't Select A New Parent Category That Is A Sub This Categrory Or Any Category Under This Category.");
		}

		if ($this->ipsclass->input['newcat'] > 0 && in_array($this->ipsclass->input['newcat'], $subids))
		{
			$this->ipsclass->admin->error("You Can't Select A Category To Move The artists That Is A Sub This Categrory Or Any Category Under This Category.");
		}

		$this->ipsclass->DB->simple_exec_query(array('select' => 'cid', 'from' => 'cocktails_cats', 'where' => 'sub='.$r['cid']));
		if ($this->ipsclass->DB->get_num_rows())
		{
			$this->ipsclass->DB->do_update('cocktails_cats', array('sub' => ($this->ipsclass->input['parcat'] == 0) ? 0 : $this->ipsclass->input['parcat']), 'sub='.$r['cid']);
		}

		if ($this->ipsclass->input['newcat'] > 0)
		{
			$this->ipsclass->DB->simple_exec_query(array('select' => 'id', 'from' => 'cocktails', 'where' => 'cat='.$r['cid']));
			if ($this->ipsclass->DB->get_num_rows())
			{
				$this->ipsclass->DB->do_update('cocktails', array('cat' => $this->ipsclass->input['newcat']), 'cat='.$r['cid']);
			}
		}
		else
		{
			$this->ipsclass->DB->simple_construct(array('select' => 'title,topicid,mem_id ,mem_name', 'from' => 'cocktails', 'where' => 'cat='.$r['cid']));
			$fq = $this->ipsclass->DB->simple_exec();
			if ($this->ipsclass->DB->get_num_rows())
			{
				while ($f = $this->ipsclass->DB->fetch_row($fq))
				{

					$this->ipsclass->DB->simple_exec_query(array('delete' => 'cocktails', 'where' => 'id='.intval($f['id'])));
					$this->ipsclass->DB->simple_exec_query(array('delete' => 'cocktails_favorites', 'where' => 'fid='.intval($f['id'])));
					$this->ipsclass->DB->simple_exec_query(array('delete' => 'cocktails_comments', 'where' => 'cocktail_id='.intval($f['id'])));
					$this->ipsclass->DB->simple_exec_query(array('delete' => 'cocktails_ratings', 'where' => 'did='.intval($f['id'])));
					$this->ipsclass->DB->simple_exec_query(array('update' => 'members', 'set' => 'cocktails=cocktails-1', 'where' => 'id='.$f['mid']));

					if ($this->ipsclass->vars['m_auto_kill_topic'] == 1)
					{
						$this->rex->my_remove_topic($f['topic']);
					}
				}
			}
		}

		define('IN_SCRIPT', 1);
		$this->ipsclass->DB->simple_exec_query(array('delete' => 'cocktails_cats', 'where' => 'cid='.$r['cid']));
		$this->do_cache_update();

		$this->ipsclass->admin->save_log("cocktails System:  Deleted Category ({$r['cname']})");
		$this->ipsclass->admin->redirect('section='.$this->ipsclass->section_code.'&act=cocktails&code=cats', "Successfully Deleted Category: ".$r['cname'], 0, 1);
	}

	function get_sub_catids($cid=0, $scids=array())
	{
		global $DB;

		if (!$cid || $cid <= 0)
		{
			return $scids;
		}

		$this->ipsclass->DB->simple_construct(array('select' => 'cid', 'from' => 'cocktails_cats', 'where' => 'sub='.$cid));
		$this->ipsclass->DB->simple_exec();
		if ($this->ipsclass->DB->get_num_rows())
		{
			while ($r = $this->ipsclass->DB->fetch_row())
			{
				$scids[] = $r['cid'];
				$scids = $this->get_sub_catids($r['cid'], $scids);
			}
		}

		return $scids;
	}




	function show_close_form()
	{
		if( $this->ipsclass->vars['open_close'] )
		{
			$text = "Open";
			$text1 = "<span style='color:green; font-weight:bold;'>Open</span>";
			$text2 = "Open";
			$text3 = "Closed";
		}
		else
		{
			$text = "Close";
			$text1 = "<span style='color:red; font-weight:bold;'>Closed</span>";
			$text2 = "Closed";
			$text3 = "Open";
		}

		$this->ipsclass->admin->page_title = "{$text} Cocktails System";
		$this->ipsclass->admin->page_detail = "You can open or close your Cocktails System from this page.";

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form( array( 1 => array( 'code'  , 'doclose' ),
									 2 => array( 'act'   , 'cocktails'   ),
									 3 => array( 'section', $this->ipsclass->section_code ),
																	)		);

		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "60%");
		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "40%");

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table( "{$text} Cocktails System" );

		$this->ipsclass->html .= "<tr><td colspan='2' class='tdrow1' width='100%'><b>Do you want to {$text} the Cocktails System?</b>&nbsp;&nbsp;--&nbsp;&nbsp;Currently, the Cocktails System is {$text1}</td></tr>";
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "<b>Change Status from {$text2} to {$text3}</b>", $this->ipsclass->adskin->form_yes_no( "open_close", "0" )
																	)		);

		$this->ipsclass->html .= $this->ipsclass->adskin->end_form( "Change Status" );
		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();

		$this->ipsclass->admin->output();
	}

	function do_close()
	{
		$current = intval($this->ipsclass->vars['open_close']);
		$yes_no = intval($this->ipsclass->input['open_close']);

		if( $current == 1 )
		{
			$tobe = ( $yes_no == 1 ) ? 0 : 1;
		}
		elseif( $current == 0 )
		{
			$tobe = ( $yes_no == 1 ) ? 1 : 0;
		}

		if( $tobe == 1 )
		{
			$textform = "<span style='color:green; font-weight:bold;'>Open</span>";
		}
		else
		{
			$textform = "<span style='color:red; font-weight:bold;'>Closed</span>";
		}

		$content = "<"."?php\n";
		$content .= "\$COCKTAILS['open_close']		= \"{$tobe}\";\n";

		$content .= "\$COCKTAILS['catpageorder'] 		= \"{$this->ipsclass->vars['catpageorder']}\";\n";
		$content .= "\$COCKTAILS['catpageascdesc'] 	= \"{$this->ipsclass->vars['catpageascdesc']}\";\n";
$content .= "\$COCKTAILS['m_cocktails_perpage'] 	= \"{$this->ipsclass->input['m_cocktails_perpage']}\";\n";
		$content .= "\$COCKTAILS['coc_perpage'] 	= \"{$this->ipsclass->input['coc_perpage']}\";\n";
				$content .= "\$COCKTAILS['max_tease']	= \"{$this->ipsclass->input['max_tease']}\";\n";

		$content .= "\$COCKTAILS['c_do_resize'] 	 	= \"{$this->ipsclass->vars['c_do_resize']}\";\n";
		$content .= "\$COCKTAILS['c_thumb_w'] 	 	= \"{$this->ipsclass->vars['c_thumb_w']}\";\n";
		$content .= "\$COCKTAILS['c_thumb_h'] 	 	= \"{$this->ipsclass->vars['c_thumb_h']}\";\n";
		$content .= "\$COCKTAILS['rc_thumb_w'] 	 	= \"{$this->ipsclass->vars['rc_thumb_w']}\";\n";
		$content .= "\$COCKTAILS['rc_thumb_h'] 	 	= \"{$this->ipsclass->vars['rc_thumb_h']}\";\n";
		$content .= "\$COCKTAILS['c_forum'] 	 		= \"{$this->ipsclass->vars['c_forum']}\";\n";
		$content .= "\$COCKTAILS['c_postt'] 	 		= \"{$this->ipsclass->vars['c_postt']}\";\n";
		$content .= "\$COCKTAILS['c_topicimage'] 	 	= \"{$this->ipsclass->vars['c_topicimage']}\";\n";
		$content .= "\$COCKTAILS['c_comments'] 	 	= \"{$this->ipsclass->vars['c_comments']}\";\n";
		$content .= "\$COCKTAILS['comm_perpage'] 	 	= \"{$this->ipsclass->vars['comm_perpage']}\";\n";
		$content .= "\$COCKTAILS['c_show_rancoc'] 	= \"{$this->ipsclass->vars['c_show_rancoc']}\";\n";
		$content .= "\$COCKTAILS['c_show_global_notes'] 	= \"{$this->ipsclass->vars['c_show_global_notes']}\";\n";
		$content .= "\$COCKTAILS['c_show_top_up']		= \"{$this->ipsclass->vars['c_show_top_up']}\";\n";
		$content .= "\$COCKTAILS['c_global_notes']	= '{$this->ipsclass->vars['c_global_notes']}';\n";
		$content .= "\$COCKTAILS['c_how_many_top'] = \"{$this->ipsclass->vars['c_how_many_top']}\";\n";
		$content .= "\$COCKTAILS['c_manually_approve']	= \"{$this->ipsclass->vars['c_manually_approve']}\";\n";
		$content .= "\$COCKTAILS['m_cat_add']	= \"{$this->ipsclass->input['m_cat_add']}\";\n";
		$content .= "\$COCKTAILS['m_topic_coc']	= \"{$this->ipsclass->input['m_topic_coc']}\";\n";
		$content .= "\$COCKTAILS['m_topic_options']	= \"{$this->ipsclass->input['m_topic_options']}\";\n";
		$content .= "\$COCKTAILS['m_screen_max_size']	= \"{$this->ipsclass->input['m_screen_max_size']}\";\n";
		$content .= "\$COCKTAILS['m_screenshot_ext']	= \"{$this->ipsclass->input['m_screenshot_ext']}\";\n";
		$content .= "\$COCKTAILS['m_screen_dir']	= \"{$this->ipsclass->input['m_screen_dir']}\";\n";
		$content .= "\n?".">\n";

		if( is_writable(ROOT_PATH."sources/action_public/mohamedmods/conf_cocktails.php") )
		{
	 		if( $fh = @fopen(ROOT_PATH."sources/action_public/mohamedmods/conf_cocktails.php", "w") )
			{
		 		@fputs($fh, $content, strlen($content) );
		 		@fclose($fh);
	 		}
		}
		else
		{
			$this->ipsclass->admin->error( "Fatal Error: Could not open conf_cocktails.php for writing - no changes applied. Try changing the CHMOD to 0777" );
			exit;
		}

		$this->ipsclass->admin->save_log( "Cocktails System Status has been changed to: {$textform}" );
		$this->ipsclass->admin->done_screen( "Cocktails System Configuration Updated", "Cocktails System Administration", $this->ipsclass->form_code );		
		exit;
	}

	function group_perms($disp='*', $view='*', $subm='*', $comm='*')
	{
		global $ibforums, $DB, $std;

		$html = "
			<script type='text/javascript'>
			var names = new Array('DISP', 'VIEW', 'SUBM', 'COMM');

			function multi_check(type, n, c)
			{
				var obj = document.forms['theAdminForm'];

				if (type == 0 || type == 1)
				{
					if (type == 0)
					{
						c = obj.elements[names[n]+'_ALL'].checked;
					}

					for (var i=0; i<obj.elements.length; i++)
					{
						var e = obj.elements[i];
						if (e.type == 'checkbox' && !e.disabled)
						{
							var en = e.name.substring(0, 4);
							if (en == names[n])
							{
								e.checked = c;
							}
						}
					}
				}
				else if (type == 2)
				{
					var nm;
					var ttl = tbox = 0;

					for (var i=0; i<names.length; i++)
					{
						nm = names[i]+'_'+n;
						eval('obj.elements[nm].checked = c');
						check_count(i);
					}
				}
			}

			function check_count(n)
			{
				var obj = document.forms['theAdminForm'];
				var tbox = 0;
				var ttl = 0;

				for (var i=0; i<obj.elements.length; i++)
				{
					var e = obj.elements[i];
					if (e.type == 'checkbox' && !e.disabled)
					{
						var en = e.name.substring(0, 4);
						if (en == names[n] && e.name != names[n]+'_ALL')
						{
							tbox++;
							if (e.checked)
							{
								ttl++;
							}
						}
					}
				}

				var nm = names[n]+'_ALL';
				eval('obj.elements[nm].checked = (ttl == tbox) ? true : false');
			}
			</script>";

		$html .= $this->ipsclass->adskin->add_td_basic("GLOBAL: All Current Groups", "left", "pformstrip");

		$s1 = $s2 = $s3 = $s4  = '';
		if ($disp == '*')
		{
			$s1 = ' checked';
		}

		if ($view == '*')
		{
			$s2 = ' checked';
		}

		if ($subm == '*')
		{
			$s3 = ' checked';
		}

		if ($comm == '*')
		{
			$s4 = ' checked';
		}

		$html_disp = "<input type='checkbox' onclick='multi_check(0, 0, 0)' name='DISP_ALL' value='1'{$sc}>\n";
		$html_view = "<input type='checkbox' onclick='multi_check(0, 1, 0)' name='VIEW_ALL' value='1'{$rec}>\n";
		$html_subm = "<input type='checkbox' onclick='multi_check(0, 2, 0)' name='SUBM_ALL' value='1'{$ac}>\n";
		$html_comm = "<input type='checkbox' onclick='multi_check(0, 3, 0)' name='COMM_ALL' value='1'{$ac}>\n";

		$html .= $this->ipsclass->adskin->add_td_row(array(
								"<b>All Current Groups</b>",
								"<center id='mgyellow'>{$html_disp}</center>",
								"<center id='mgblue'>{$html_view}</center>",
								"<center id='mgred'>{$html_subm}</center>",
								"<center id='memgroup'>{$html_comm}</center>",
						      )     );

		$html .= $this->ipsclass->adskin->add_td_basic("OR: Adjust Group Permissions Below", "left", "pformstrip");
		$this->ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'groups', 'order' => 'g_title ASC'));
		$this->ipsclass->DB->simple_exec();
		while ($data = $this->ipsclass->DB->fetch_row())
		{
			$s1 = $s2 = $s3 = $s4 = '';
			if ($disp == '*' || preg_match("/(^|,)".$data['g_id']."(,|$)/", $disp))
			{
				$s1 = ' checked';
			}

			if ($view == '*' || preg_match("/(?:^|,)".$data['g_id']."(?:,|$)/", $view))
			{
				$s2 = ' checked';
			}

			if ($subm == '*' || preg_match("/(?:^|,)".$data['g_id']."(?:,|$)/", $subm))
			{
				$s3 = ' checked';
			}

			if ($comm == '*' || preg_match("/(?:^|,)".$data['g_id']."(?:,|$)/", $comm))
			{
				$s4 = ' checked';
			}

			$html_disp = "<input type='checkbox' name='DISP_{$data['g_id']}' value='1' onclick='check_count(0)'{$s1}>";
			$html_view = "<input type='checkbox' name='VIEW_{$data['g_id']}' value='1' onclick='check_count(1)'{$s2}>";
			$html_subm = "<input type='checkbox' name='SUBM_{$data['g_id']}' value='1' onclick='check_count(2)'{$s3}>";
			$html_comm = "<input type='checkbox' name='COMM_{$data['g_id']}' value='1' onclick='check_count(3)'{$s4}>";

			$html .= $this->ipsclass->adskin->add_td_row(array(
									"<div align='right' style='font-weight:bold'>{$data['g_title']} &nbsp; <input type='button' id='button' value='+' onclick='multi_check(2, {$data['g_id']}, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(2, {$data['g_id']}, false)' /></div>",
									"<center id='mgyellow'>{$html_disp}</center>",
									"<center id='mgblue'>{$html_view}</center>",
									"<center id='mgred'>{$html_subm}</center>",
									"<center id='memgroup'>{$html_comm}</center>",
							      )     );
		}

		$html .= $this->ipsclass->adskin->add_td_row(array(
								"&nbsp;",
								"<center><input type='button' id='button' value='+' onclick='multi_check(1, 0, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(1, 0, false)' /></center>",
								"<center><input type='button' id='button' value='+' onclick='multi_check(1, 1, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(1, 1, false)' /></center>",
								"<center><input type='button' id='button' value='+' onclick='multi_check(1, 2, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(1, 2, false)' /></center>",
								"<center><input type='button' id='button' value='+' onclick='multi_check(1, 3, true)' />&nbsp;<input type='button' id='button' value='-' onclick='multi_check(1, 3, false)' /></center>",
						      )     );
	
		return $html;
	}

	function compile_perms()
	{
		global $ibforums, $DB, $std;

		$names = array('DISP', 'VIEW', 'SUBM', 'COMM');
		$r_array = array('DISP' => '', 'VIEW' => '', 'SUBM' => '', 'COMM');
		foreach ($r_array as $k => $v)
		{
			if ($this->ipsclass->input[$k.'_ALL'] == 1)
			{
				$r_array[$k] = '*';
			}
		}

		$this->ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'groups', 'order' => 'g_title, g_id'));
		$this->ipsclass->DB->simple_exec();
		while ($data = $this->ipsclass->DB->fetch_row())
		{
			foreach ($names as $name)
			{
				if ($r_array[$name] != '*')
				{
					if ($this->ipsclass->input[$name.'_'.$data['g_id']] == 1)
					{
						$r_array[$name] .= $data['g_id'].",";
					}
				}
			}
		}

		foreach ($names as $name)
		{
			$r_array[$name] = preg_replace("/,$/", "", $r_array[$name]);
		}

		return $r_array;
	}
	function compiles_perms()
	{
		global $ibforums, $DB, $std;

		$names = array('DISP', 'VIEW', 'SUBM', 'COMM');
		$r_array = array('DISP' => '', 'VIEW' => '', 'SUBM' => '', 'COMM');
		foreach ($r_array as $k => $v)
		{
			if ($this->ipsclass->input[$k.'_ALL'] == 1)
			{
				$r_array[$k] = '*';
			}
		}

		$this->ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'cocktails_cats', 'order' => 'position ASC'));
		$this->ipsclass->DB->simple_exec();
		while ($data = $this->ipsclass->DB->fetch_row())
		{
			foreach ($names as $name)
			{
				if ($r_array[$name] != '*')
				{
					if ($this->ipsclass->input[$name.'_'.$data['cid']] == 1)
					{
						$r_array[$name] .= $data['cid'].",";
					}
				}
			}
		}

		foreach ($names as $name)
		{
			$r_array[$name] = preg_replace("/,$/", "", $r_array[$name]);
		}

		return $r_array;
	}

	function do_cache_update()
	{

		$this->rex->update_cocktails_cache();
		if (IN_SCRIPT == 1)
		{
			return;
		}
		else
		{
			$this->ipsclass->admin->save_log("Artists System:  Manually Updated Cache");
			$this->ipsclass->admin->redirect('section='.$this->ipsclass->section_code.'&act=cocktails', "Successfully Updated Cache", 0, 1);		
		}
	}

}

?>