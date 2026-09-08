<?php

/*
+----------------------------------------------------
| User Reputation System v1.0.1
| ===================================================
| by Nguyen Tuan Dung (ntd1712)
| (c) 2005 - 2006 Vietnamese - Invision Resources
| http://invisionviet.net/
| ===================================================
| Date Started: Wed, 08 Feb 2006 17:41 (GMT+07:00)
| Release Data: Fri, 10 Mar 2006 02:32 (GMT+07:00)
| License Info: http://invisionviet.net/license.php
+----------------------------------------------------
*/

if( ! defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit;
}

class ad_reputation
{
	var $ipsclass;
	var $now_date = "";

	function auto_run()
	{
		$tmp_in = array_merge($_GET, $_POST, $_COOKIE);
		
		foreach( $tmp_in as $k => $v )
		{
			unset($$k);
		}

		$a = explode(",", gmdate("Y,n,j,G,i,s", time() + $this->ipsclass->get_time_offset()));
		$this->now_date = array( 'year' => $a[0], 'mon' => $a[1], 'mday' => $a[2],
								 'hours' => $a[3], 'minutes' => $a[4], 'seconds' => $a[5] );

		//-----------------------------------------
		// Load and config the library
		//-----------------------------------------

		switch( $this->ipsclass->input['code'] )
		{
			case 'modify':
				$this->show_level();
				break;
			case 'add':
				$this->show_form('new');
				break;
			case 'doadd':
				$this->do_update('new');
				break;
			case 'edit':
				$this->show_form('edit');
				break;
			case 'doedit':
				$this->do_update('edit');
				break;
			case 'doupdate':
				$this->do_update();
				break;
			case 'dodelete':
				$this->do_delete();
				break;
			case 'list':
				$this->view_list();
				break;
			case 'dolist':
				$this->do_list();
				break;
			case 'editrep':
				$this->show_form_rep('edit');
				break;
			case 'doeditrep':
				$this->do_edit_rep();
				break;
			case 'dodelrep':
				$this->do_delete_rep();
				break;

			default:
				$this->show_level();
				break;
		}
	}

	/*---------------------------------------------*/
	// REPUTATION MANAGER
	/*---------------------------------------------*/

	function show_level()
	{
		$this->ipsclass->admin->nav[] = array( '', 'User Reputation Manager' );
		$this->ipsclass->admin->page_title  = "Overview";
		$this->ipsclass->admin->page_detail = "You can manage your reputation level from here.";

		$this->ipsclass->DB->simple_construct( array( 'select' => '*', 'from' => 'reputationlevel', 'order' => 'minimumreputation ASC' ) );
		$this->ipsclass->DB->simple_exec();

		if( ! $this->ipsclass->DB->get_num_rows() )
		{
			$this->show_add();
			return;
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form( array( 1 => array( 'code'   , 'doupdate' ),
																			 2 => array( 'act'    , 'reputation' ),
																			 3 => array( 'section', $this->ipsclass->section_code ),
																	)		);

		$this->ipsclass->adskin->td_header[] = array( "ID"                      , "5%"  );
		$this->ipsclass->adskin->td_header[] = array( "Reputation Level"        , "60%" );
		$this->ipsclass->adskin->td_header[] = array( "Minimum Reputation Level", "20%" );
		$this->ipsclass->adskin->td_header[] = array( "Controls"                , "15%" );

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table( "User Reputation Manager" );

		while( $r = $this->ipsclass->DB->fetch_row() )
		{
			$this->ipsclass->html .= "<tr>\n".
									 "	<td class='tdrow2'>#".$r['reputationlevelid']."</td>\n".
									 "	<td class='tdrow2'><b>User</b> ".$r['level']."</td>\n".
									 "	<td class='tdrow2' align='center'><input type='text' name='reputation[".$r['reputationlevelid']."]' value='".$r['minimumreputation']."' size='12' class='textinput' /></td>\n".
									 "	<td class='tdrow2' align='center'><a href='{$this->ipsclass->base_url}&amp;{$this->ipsclass->form_code}&amp;code=edit&amp;reputationlevelid=".$r['reputationlevelid']."'>Edit</a> - <a href='{$this->ipsclass->base_url}&amp;{$this->ipsclass->form_code}&amp;code=dodelete&amp;reputationlevelid=".$r['reputationlevelid']."'>Delete</a></td>\n".
									 "</tr>\n";
		}

		$this->ipsclass->html .= "<tr><td class='pformstrip' colspan='4' align='center'><input type='submit' value='Update' accesskey='s' id='button' /> <input type='reset' class='button' tabindex='1' value='Reset' accesskey='r' id='button' /></td></tr>";
		$this->ipsclass->html .= "</form>";

		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();

		$this->ipsclass->admin->output();
	}

	/*---------------------------------------------*/
	// EDIT FORM
	/*---------------------------------------------*/

	function show_form($type='edit')
	{
		$this->ipsclass->admin->nav[] = array( '', 'User Reputation Manager' );
		$this->ipsclass->admin->page_title  = "Overview";
		$this->ipsclass->admin->page_detail = "You can manage your reputation level from here.";

		if( $type == 'edit' )
		{
			$this->ipsclass->DB->simple_construct( array( 'select' => '*', 'from' => 'reputationlevel', 'where' => 'reputationlevelid='.intval($this->ipsclass->input['reputationlevelid']) ) );
			$this->ipsclass->DB->simple_exec();

			if( ! $r = $this->ipsclass->DB->fetch_row() )
			{
				$this->ipsclass->admin->error( "Either specify the value ID." );
			}

			$title  = "Reputation Level: <span style='font-weight:normal;'>{$r['level']} (ID:#{$r['reputationlevelid']})</span>";
			$button = "Update";
			$code   = 'doedit';
		}
		else
		{
			$title  = "Add New Reputation Level";
			$button = "Save";
			$code   = 'doadd';
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form( array( 1 => array( 'code'   , $code ),
																			 2 => array( 'act'    , 'reputation' ),
																			 3 => array( 'section', $this->ipsclass->section_code ),
																			 4 => array( 'reputationlevelid', $r['reputationlevelid'] ),
																	)		);

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table( $title );

		$this->ipsclass->adskin->td_header[] = array("", "40%");
		$this->ipsclass->adskin->td_header[] = array("", "60%");

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Description", "<input type='text' class='textinput' name='level' value=\"{$r['level']}\" size='35' maxlength='250' />" ) );
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Minimum Reputation Level", "<input type='text' class='textinput' name='minimumreputation' value=\"{$r['minimumreputation']}\" size='35' maxlength='10' />" ) );

		$this->ipsclass->html .= "<tr><td class='pformstrip' colspan='2' align='center'><input type='submit' value='$button' accesskey='s' id='button' /> <input type='reset' class='button' tabindex='1' value='Reset' accesskey='r' id='button' /></td></tr>";
		$this->ipsclass->html .= "</form>";

		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();

		$this->ipsclass->admin->output();
	}

	/*---------------------------------------------*/
	// UPDATE REPUTATION LEVEL
	/*---------------------------------------------*/

	function do_update($type="")
	{
		if( $type != "" )
		{
			$level = str_replace("<br />", "", $this->ipsclass->input['level']);
			$level = trim($level);

			$temp = $this->ipsclass->txt_stripslashes( $_POST['level'] );
			if( (strlen(trim($temp)) < 2) || ($level == "") )
			{
				$this->ipsclass->admin->error( "The text you entered was too short." );
			}

			if( $this->ipsclass->txt_mb_strlen( $_POST['level'] ) > 250 )
			{
				$this->ipsclass->admin->error( "The text entry is too long." );
			}

			$redirect = "Saved Reputation Level <i>{$this->ipsclass->input['level']}</i> Successfully.";
		}

		//-----------------------------------------
		// Switch
		//-----------------------------------------

		if( $type == 'new' )
		{
			$this->ipsclass->DB->do_insert( 'reputationlevel', array( 'minimumreputation' => intval($this->ipsclass->input['minimumreputation']), 'level' => $level ) );
		}
		elseif( $type == 'edit' )
		{
			$levelid = intval($this->ipsclass->input['reputationlevelid']);

			//-----------------------------------------
			// Check to make sure its a valid ID
			//-----------------------------------------

			$this->ipsclass->DB->simple_construct( array( 'select' => 'reputationlevelid', 'from' => 'reputationlevel', 'where' => 'reputationlevelid='.$levelid ) );
			$this->ipsclass->DB->simple_exec();

			if( ! $this->ipsclass->DB->get_num_rows() )
			{
				$this->ipsclass->admin->error( "Either specify the value ID." );
			}

			$this->ipsclass->DB->do_update( 'reputationlevel', array( 'minimumreputation' => intval($this->ipsclass->input['minimumreputation']), 'level' => $level ), 'reputationlevelid='.$levelid );
		}
		else
		{
			$ids = $this->ipsclass->input['reputation'];
			if( is_array($ids) && count($ids) )
			{
				foreach( $ids as $k => $v )
				{
					$this->ipsclass->DB->do_update( 'reputationlevel', array( 'minimumreputation' => intval($v) ), 'reputationlevelid='.intval($k) );
				}
			}
			else
			{
				$this->ipsclass->admin->error( "Either specify the value ID." );
			}

			$redirect = "Saved Reputation Level Successfully.";
		}

		$this->update_rep_cache();

		$this->ipsclass->admin->done_screen( $redirect, "User Reputations - User Reputation Manager", $this->ipsclass->form_code, 'redirect' );
	}

	/*---------------------------------------------*/
	// REMOVE REPUTATION LEVEL
	/*---------------------------------------------*/

	function do_delete()
	{
		$levelid = intval($this->ipsclass->input['reputationlevelid']);

		//-----------------------------------------
		// Check to make sure its a valid ID
		//-----------------------------------------

		$this->ipsclass->DB->simple_construct( array( 'select' => 'reputationlevelid', 'from' => 'reputationlevel', 'where' => 'reputationlevelid='.$levelid ) );
		$this->ipsclass->DB->simple_exec();

		if( ! $this->ipsclass->DB->get_num_rows() )
		{
			$this->ipsclass->admin->error( "Either specify the value ID." );
		}

		//-----------------------------------------
		// Delete the level
		//-----------------------------------------

		$this->ipsclass->DB->simple_exec_query( array( 'delete' => 'reputationlevel', 'where' => 'reputationlevelid='.$levelid ) );
		$this->update_rep_cache();

		$this->ipsclass->admin->done_screen( "Deleted Reputation Level Successfully", "User Reputations - User Reputation Manager", $this->ipsclass->form_code, 'redirect' );
	}

	/*---------------------------------------------*/
	// VIEW COMMENTS
	/*---------------------------------------------*/

	function view_list()
	{
		$this->ipsclass->admin->nav[] = array( '', 'User Reputation Manager' );
		$this->ipsclass->admin->page_title  = "Overview";
		$this->ipsclass->admin->page_detail = "You can manage your reputation comments from here.";

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form( array( 1 => array( 'code'   , 'list' ),
																			 2 => array( 'act'    , 'reputation' ),
																			 3 => array( 'section', $this->ipsclass->section_code ),
																			 4 => array( 'dolist', 1 ),
																	)		);

		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "20%");
		$this->ipsclass->adskin->td_header[] = array("&nbsp;", "80%");

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table( "View Reputation Comments" );

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Left For", "<input type='text' class='textinput' name='leftfor' value='' size='35' maxlength='250' tabindex='1' />" ) );
		$this->ipsclass->html .= "<tr><td class='tdrow2' colspan='2'><div class='desctext'>To limit the comments left for a specific user, enter the username here. Leave this field empty to receive comments left for every user.</div></td></tr>";

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Left By", "<input type='text' class='textinput' name='leftby' value='' size='35' maxlength='250' tabindex='2' />" ) );
		$this->ipsclass->html .= "<tr><td class='tdrow2' colspan='2'><div class='desctext'>To limit the comments left by a specific user, enter the username here. Leave this field empty to receive comments left by every user.</div></td></tr>";

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Start Date",
		"<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>
			<td style='padding-right:5px;'>Month<br /><select name='start[month]' class='textinput' tabindex='3'>".$this->get_month_dropdown(1)."</select></td>
			<td style='padding-right:5px;'>Day<br /><input type='text' class='textinput' name='start[day]' value='".($this->now_date['mday']+1)."' size='4' maxlength='2' tabindex='3' /></td>
			<td>Year<br /><input type='text' class='textinput' name='start[year]' value='".$this->now_date['year']."' size='4' maxlength='4' tabindex='3' /></td>
			<td width='97%'>&nbsp;</td>
		</tr></table>"
																	)		);
		$this->ipsclass->html .= "<tr><td class='tdrow2' colspan='2'><div class='desctext'>Select a start date for this report. Select a month, day, and year. The selected statistic must be no older than this date for it to be included in the report.</div></td></tr>";

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "End Date",
		"<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>
			<td style='padding-right:5px;'>Month<br /><select name='end[month]' class='textinput' tabindex='4'>".$this->get_month_dropdown()."</select></td>
			<td style='padding-right:5px;'>Day<br /><input type='text' class='textinput' name='end[day]' value='".$this->now_date['mday']."' size='4' maxlength='2' tabindex='4' /></td>
			<td>Year<br /><input type='text' class='textinput' name='end[year]' value='".$this->now_date['year']."' size='4' maxlength='4' tabindex='4' /></td>
			<td width='97%'>&nbsp;</td>
		</tr></table>"
																	)		);
		$this->ipsclass->html .= "<tr><td class='tdrow2' colspan='2'><div class='desctext'>Select an end date for this report. Select a month, day, and year. The selected statistic must not be newer than this date for it to be included in the report. You can use this setting in conjunction with the 'Start Date' setting to create a window of time for this report.</div></td></tr>";

		$this->ipsclass->html .= "<tr><td class='pformstrip' colspan='2' align='center'><input type='submit' value='Search' accesskey='s' id='button' tabindex='5' /> <input type='reset' class='button' value='Reset' accesskey='r' id='button' tabindex='6' /></td></tr>";
		$this->ipsclass->html .= "</form>";

		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();

		//-----------------------------------------
		// Any results?
		//-----------------------------------------

		if( $this->ipsclass->input['dolist'] )
		{
			$links = "";
			$first = intval($this->ipsclass->input['st']);
			$start = mktime(0, 0, 0, $this->ipsclass->input['start']['month'], $this->ipsclass->input['start']['day'], $this->ipsclass->input['start']['year']) + $this->ipsclass->get_time_offset();
			$start = intval($this->ipsclass->input['startstamp']) ? intval($this->ipsclass->input['startstamp']) : $start;
			$end   = mktime(0, 0, 0, $this->ipsclass->input['end']['month'], $this->ipsclass->input['end']['day'] + 1, $this->ipsclass->input['end']['year']) + $this->ipsclass->get_time_offset();
			$end   = intval($this->ipsclass->input['endstamp']) ? intval($this->ipsclass->input['endstamp']) : $end;

			if( ! $start )
			{
				$start = time() - (3600 * 24 * 30);
			}

			if( ! $end )
			{
				$end = time();
			}

			if( $start >= $end )
			{
				$this->ipsclass->admin->error( "Start date is after the end date." );
			}

			if( $this->ipsclass->input['leftby'] )
			{
				$leftby = $this->ipsclass->DB->build_and_exec_query( array( 'select' => 'id', 'from' => 'members', 'where' => "MD5(name)='".md5($this->ipsclass->input['leftby'])."'" ) );

				if( ! $leftby['id'] )
				{
					$this->ipsclass->admin->error( "Could not find user '".$this->ipsclass->input['leftby']."'" );
				}

				$who  = $leftby['id'];
				$cond = "WHERE r.whoadded=".$who;
			}

			if( $this->ipsclass->input['leftfor'] )
			{
				$leftfor = $this->ipsclass->DB->build_and_exec_query( array( 'select' => 'id', 'from' => 'members', 'where' => "MD5(name)='".md5($this->ipsclass->input['leftfor'])."'" ) );

				if( ! $leftfor['id'] )
				{
					$this->ipsclass->admin->error( "Could not find user '".$this->ipsclass->input['leftfor']."'" );
				}

				$user  = $leftfor['id'];
				$cond .= ($cond ? " AND" : "")." r.userid=".$user;
			}

			if( $start )
			{
				$cond .= ($cond ? " AND" : "")." r.dateline >= $start";
			}

			if( $end )
			{
				$cond .= ($cond ? " AND" : "")." r.dateline <= $end";
			}

			switch( $this->ipsclass->input['orderby'] )
			{
				case 'leftbyuser':
					$order = 'leftby.name';
					break;
				case 'leftforuser':
					$order = 'leftfor.name';
					break;
				default:
					$order   = 'r.dateline';
					$orderby = 'dateline';
			}

			$this->ipsclass->adskin->td_header[] = array( "ID"                , "5%"  );
			$this->ipsclass->adskin->td_header[] = array( "<a href='{$this->ipsclass->base_url}&{$this->ipsclass->form_code}&code=list&dolist=1&who=".intval($who)."&user=".intval($user)."&orderby=leftbyuser&startstamp=$start&endstamp=$end&st=$first'>Left By</a>"  , "20%" );
			$this->ipsclass->adskin->td_header[] = array( "<a href='{$this->ipsclass->base_url}&{$this->ipsclass->form_code}&code=list&dolist=1&who=".intval($who)."&user=".intval($user)."&orderby=leftforuser&startstamp=$start&endstamp=$end&st=$first'>Left For</a>", "20%" );
			$this->ipsclass->adskin->td_header[] = array( "<a href='{$this->ipsclass->base_url}&{$this->ipsclass->form_code}&code=list&dolist=1&who=".intval($who)."&user=".intval($user)."&orderby=date&startstamp=$start&endstamp=$end&st=$first'>Date</a>"           , "17%" );
			$this->ipsclass->adskin->td_header[] = array( "Point"             , "5%"  );
			$this->ipsclass->adskin->td_header[] = array( "Reason"            , "23%" );
			$this->ipsclass->adskin->td_header[] = array( "Controls"          , "10%" );

			$this->ipsclass->html .= $this->ipsclass->adskin->start_table( "Reputation Comments" );

			//-----------------------------------------
			// Get count
			//-----------------------------------------

			$this->ipsclass->DB->simple_construct( array( 'select' => 'COUNT(*) AS cnt', 'from' => 'reputation r', 'where' => $cond ) );
			$this->ipsclass->DB->simple_exec();

			$total = $this->ipsclass->DB->fetch_row();

			if( ! $total['cnt'] )
			{
				$this->ipsclass->html .= "<tr><td class='tdrow1' colspan='6' align='center'>No Matches Found!</td></tr>";
			}

			//-----------------------------------------
			// Pages...
			//-----------------------------------------

			$links = $this->ipsclass->build_pagelinks( array( 'TOTAL_POSS'  => $total['cnt'],
															  'PER_PAGE'    => 25,
															  'CUR_ST_VAL'  => $first,
															  'L_SINGLE'    => "",
															  'L_MULTI'     => "",
															  'BASE_URL'    => $this->ipsclass->base_url."&{$this->ipsclass->form_code}&code=list&dolist=1&who=".intval($who)."&user=".intval($user)."&orderby=$orderby&startstamp=$start&endstamp=$end",
													 )      );

			//-----------------------------------------
			// Print...
			//-----------------------------------------

			$this->ipsclass->DB->build_query( array( 'select'   => 'r.*',
													 'from'     => array( 'reputation' => 'r' ),
													 'where'    => $cond,
													 'add_join' => array(
													 # POST TABLE JOIN
												 					  0 => array( 'select' => 'p.topic_id',
												 								  'from'   => array( 'posts' => 'p' ),
												 								  'where'  => 'p.pid=r.postid',
												 								  'type'   => 'left' ),
													 # MEMBER TABLE JOIN
												 					  1 => array( 'select' => 'leftfor.id as leftfor_id, leftfor.members_display_name as leftfor_name',
												 								  'from'   => array( 'members' => 'leftfor' ),
												 								  'where'  => 'leftfor.id=r.userid',
												 								  'type'   => 'left' ),
													 # MEMBER TABLE JOIN
											 						  2 => array( 'select' => 'leftby.id as leftby_id, leftby.members_display_name as leftby_name',
											 						  			  'from'   => array( 'members' => 'leftby' ),
											 						  			  'where'  => 'leftby.id=r.whoadded',
											 						  			  'type'   => 'left' )
											 							),
													 'order'	=> $order,
													 'limit'    => array( $first, 25 ) ) );
			$this->ipsclass->DB->exec_query();

			while( $r = $this->ipsclass->DB->fetch_row() )
			{
				$r['dateline'] = $this->ipsclass->get_date( $r['dateline'], 'LONG' );

				$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "#{$r['reputationid']}",
																					 "<a href='index.php?showuser={$r['leftby_id']}' target='_blank'>{$r['leftby_name']}</a>",
																					 "<a href='index.php?showuser={$r['leftfor_id']}' target='_blank'>{$r['leftfor_name']}</a>",
																					 $r['dateline'],
																					 $r['reputation'],
																					 "<a href='index.php?showtopic={$r['topic_id']}&view=findpost&p={$r['postid']}' target='_blank'>{$r['reason']}</a>",
																					 " <a href='{$this->ipsclass->base_url}&{$this->ipsclass->form_code}&code=editrep&reputationid={$r['reputationid']}'>[Edit]</a> <a href='{$this->ipsclass->base_url}&{$this->ipsclass->form_code}&code=dodelrep&reputationid={$r['reputationid']}'>[Delete]</a> ",
																			)      );
			}

			$this->ipsclass->html .= $this->ipsclass->adskin->end_table();
			$this->ipsclass->html .= "<br /><div>$links</div>";
		}

		$this->ipsclass->admin->output();
	}

	/*---------------------------------------------*/
	// EDIT REPUTATION
	/*---------------------------------------------*/

	function show_form_rep()
	{
		$this->ipsclass->admin->nav[] = array( '', 'User Reputation Manager' );
		$this->ipsclass->admin->page_title  = "Overview";
		$this->ipsclass->admin->page_detail = "You can manage your reputation from here.";

		$this->ipsclass->DB->build_query( array( 'select'   => 'r.*',
												 'from'     => array( 'reputation' => 'r' ),
												 'where'    => "reputationid=".intval($this->ipsclass->input['reputationid']),
												 'add_join' => array(
												 # POST TABLE JOIN
											 					  1 => array( 'select' => 'p.topic_id',
											 								  'from'   => array( 'posts' => 'p' ),
											 								  'where'  => 'p.pid=r.postid',
											 								  'type'   => 'left' ),
												 # TOPIC TABLE JOIN 					  			  
																  0 => array( 'select' => 't.title',
												 				  'from'   => array( 'topics' => 't' ),
								 								  'where'  => 'p.topic_id=t.tid',
								 								  'type'   => 'left' ),
												 # MEMBER TABLE JOIN
																  2 => array( 'select' => 'leftfor.members_display_name as leftfor_name',
																			  'from'   => array( 'members' => 'leftfor' ),
																			  'where'  => 'leftfor.id=r.userid',
																			  'type'   => 'left' ),
												 # MEMBER TABLE JOIN
											 					  3 => array( 'select' => 'leftby.members_display_name as leftby_name',
											 					  			  'from'   => array( 'members' => 'leftby' ),
											 					  			  'where'  => 'leftby.id=r.whoadded',
											 					  			  'type'   => 'left' )
											 						),
												 'limit'    => array( 0, 1 ) ) );
		$this->ipsclass->DB->exec_query();

		if( ! $r = $this->ipsclass->DB->fetch_row() )
		{
			$this->ipsclass->admin->error( "Either specify the value ID." );
		}

		$this->ipsclass->html .= $this->ipsclass->adskin->start_form( array( 1 => array( 'code'   , 'doeditrep' ),
																			 2 => array( 'act'    , 'reputation' ),
																			 3 => array( 'section', $this->ipsclass->section_code ),
																			 4 => array( 'reputationid', $r['reputationid'] ),
																			 5 => array( 'oldreputation', $r['reputation'] )
																	)		);

		$this->ipsclass->html .= $this->ipsclass->adskin->start_table( "Edit Reputation" );

		$this->ipsclass->adskin->td_header[] = array("", "37%");
		$this->ipsclass->adskin->td_header[] = array("", "63%");

		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Topic", "<a href='index.php?showtopic={$r['topic_id']}&view=findpost&p={$r['postid']}' target='_blank'>{$r['title']}</a>" ) );
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Left By", $r['leftby_name'] ) );
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Left For", $r['leftfor_name'] ) );
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Comment", "<input type='text' name='reason' value='".$r['reason']."' size='35' maxlength='250' class='textinput' />" ) );
		$this->ipsclass->html .= $this->ipsclass->adskin->add_td_row( array( "Reputation", "<input type='text' name='reputation' value='".$r['reputation']."' size='35' maxlength='10' class='textinput' />" ) );

		$this->ipsclass->html .= "<tr><td class='pformstrip' colspan='2' align='center'><input type='submit' value='Save' accesskey='s' id='button' /> <input type='reset' class='button' tabindex='1' value='Reset' accesskey='r' id='button' /></td></tr>";
		$this->ipsclass->html .= "</form>";

		$this->ipsclass->html .= $this->ipsclass->adskin->end_table();

		$this->ipsclass->admin->output();
	}

	/*---------------------------------------------*/
	// UPDATE REPUTATION
	/*---------------------------------------------*/

	function do_edit_rep()
	{
		if( $this->ipsclass->input['reason'] )
		{
			$reason = str_replace("<br />", "", $this->ipsclass->input['reason']);
			$reason = trim($reason);

			$temp = $this->ipsclass->txt_stripslashes( $_POST['reason'] );
			if( (strlen(trim($temp)) < 2) || ($reason == "") )
			{
				$this->ipsclass->admin->error( "The text you entered was too short." );
			}

			if( $this->ipsclass->txt_mb_strlen( $_POST['reason'] ) > 250 )
			{
				$this->ipsclass->admin->error( "The text entry is too long." );
			}
		}

		$oldrep = intval($this->ipsclass->input['oldreputation']);
		$newrep = intval($this->ipsclass->input['reputation']);

		//-----------------------------------------
		// Check to make sure its a valid ID
		//-----------------------------------------

		$this->ipsclass->DB->simple_construct( array( 'select' => 'reputationid,userid', 'from' => 'reputation', 'where' => "reputationid=".intval($this->ipsclass->input['reputationid']) ) );
		$this->ipsclass->DB->simple_exec();

		if( ! $r = $this->ipsclass->DB->fetch_row() )
		{
			$this->ipsclass->admin->error( "Either specify the value ID." );
		}

		$this->ipsclass->DB->do_update( 'reputation', array( 'reputation' => $newrep, 'reason' => $reason ), "reputationid=".$r['reputationid'] );

		if( $oldrep != $newrep )
		{
			$diff = $oldrep - $newrep;
			$this->ipsclass->DB->do_update( 'members', array( 'reputation' => "reputation-".$diff ), "id=".$r['userid'] );
		}

		$this->ipsclass->admin->done_screen( "Saved Reputation #ID{$this->ipsclass->input['reputationid']} Successfully.", "User Reputations - User Reputation Manager", "{$this->ipsclass->form_code}&code=list", 'redirect' );
	}

	/*---------------------------------------------*/
	// DELETE REPUTATION
	/*---------------------------------------------*/

	function do_delete_rep()
	{
		//-----------------------------------------
		// Check to make sure its a valid ID
		//-----------------------------------------

		$this->ipsclass->DB->simple_construct( array( 'select' => 'reputationid,reputation,userid', 'from' => 'reputation', 'where' => "reputationid=".intval($this->ipsclass->input['reputationid']) ) );
		$this->ipsclass->DB->simple_exec();

		if( ! $r = $this->ipsclass->DB->fetch_row() )
		{
			$this->ipsclass->admin->error( "Either specify the value ID." );
		}

		//-----------------------------------------
		// Delete the level
		//-----------------------------------------

		$this->ipsclass->DB->simple_exec_query( array( 'delete' => 'reputation', 'where' => "reputationid=".$r['reputationid'] ) );
		$this->ipsclass->DB->do_update( 'members', array( 'reputation' => "reputation-".$r['reputation'] ), "id=".$r['userid'] );

		$this->ipsclass->admin->done_screen( "Deleted Reputation Successfully", "User Reputations - User Reputation Manager", "{$this->ipsclass->form_code}&code=list", 'redirect' );
	}

	/*---------------------------------------------*/
	// update_rep_cache
	/*---------------------------------------------*/

	function update_rep_cache()
	{
		$this->ipsclass->cache['replevel'] = array();

		$this->ipsclass->DB->simple_construct( array( 'select' => '*', 'from' => 'reputationlevel', 'order' => 'minimumreputation ASC' ) );
		$this->ipsclass->DB->simple_exec();

		while( $r = $this->ipsclass->DB->fetch_row() )
		{
			$this->ipsclass->cache['replevel'][ $r['minimumreputation'] ] = $r['level'];
		}

		$this->ipsclass->update_cache( array( 'name' => 'replevel', 'array' => 1, 'deletefirst' => 1, 'donow' => 1 ) );
	}

	/*---------------------------------------------*/
	// get_month_dropdown
	/*---------------------------------------------*/

	function get_month_dropdown($i=0)
	{
        $month = array('----','January','February','March','April','May','June','July','August','September','October','November','December');
		foreach( $month as $k => $m )
		{
			$return .= "\t<option value='".$k."'";
			$return .= ( ($k+$i) == $this->now_date['mon'] ) ? " selected='selected'" : "";
			$return .= ">".$m."</option>\n";
		}

		return $return;
	}
}

?>