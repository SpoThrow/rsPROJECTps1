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

class reputation
{
	var $ipsclass;
	var $ismod = 0;
	var $now   = 0;

	/*---------------------------------------------*/
	// auto_run
	/*---------------------------------------------*/

	function auto_run()
	{
		if( $this->ipsclass->is_bot == 1 )
		{
			$this->ipsclass->Error( array( 'LEVEL' => 1, 'MSG' => 'no_permission' ) );
		}

		if( ( $this->ipsclass->member['mgroup'] == $this->ipsclass->vars['admin_group'] ) || ( $this->ipsclass->member['g_access_cp'] == 1 ) )
		{
			$this->ismod = 1;
		}

		$show['closewindow'] = TRUE;

		//-----------------------------------------
		// Load and config the library
		//-----------------------------------------

		$this->ipsclass->load_language('lang_reputation');
		$this->ipsclass->load_template('skin_reputation');

		//-----------------------------------------
		// Check viewing permissions, etc...
		//-----------------------------------------

		if( ! $this->ismod && ! $this->ipsclass->vars['rep_is_online'] )
		{
			$message = $this->ipsclass->lang['rep_off'];
		}

		# Just added to Reputation?
		if( $this->ipsclass->input['done'] )
		{
			$output = "<tr><td class='row2'>{$this->ipsclass->lang['rep_added']}</td></tr>";

			$this->output = $this->ipsclass->compiled_templates['skin_reputation']->reputation( $show, $output );
			$this->ipsclass->print->pop_up_window( $this->ipsclass->vars['board_name'].' - '.$this->ipsclass->lang['reputation'], $this->output );
		}

		# Else...
		$this->now = time();
		$message   = "";
		$forum     = $this->ipsclass->forums->forum_by_id[ intval($this->ipsclass->input['fid']) ];

		if( ! $forum['id'] )
		{
			$message = $this->ipsclass->lang['rep_incorrect'];
		}

		$this->ipsclass->forums->forums_check_access( $forum['id'], 1, 'topic' );

		$post = $this->ipsclass->DB->build_and_exec_query( array ( 'select' => 'pid,author_id,topic_id', 'from' => 'posts', 'where' => "pid=".intval($this->ipsclass->input['pid']) ) );

		if( ! $post['topic_id'] || ! $post['pid'] || ! $post['author_id'] )
		{
			$message = $this->ipsclass->lang['rep_incorrect'];
		}

		if( ( ! $this->ipsclass->member['g_rep_use'] && $post['author_id'] != $this->ipsclass->member['id'] ) || ! $this->ipsclass->member['id'] )
		{
			$message = $this->ipsclass->lang['rep_noperm'];
		}

		$user = $this->ipsclass->DB->build_and_exec_query( array ( 'select' => 'id,mgroup,members_display_name,reputation', 'from' => 'members', 'where' => "id=".$post['author_id'] ) );

		if( ! $user['id'] )
		{
			$message = $this->ipsclass->lang['rep_incorrect'];
		}

		if( $user['mgroup'] == 5 )
		{
			$message = $this->ipsclass->lang['rep_banned'];
		}

		//-----------------------------------------
		// Already reputation this post?
		//-----------------------------------------

		$repeat = $this->ipsclass->DB->build_and_exec_query( array( 'select' => 'postid', 'from' => 'reputation', 'where' => "postid=".$post['pid']." AND whoadded=".$this->ipsclass->member['id'] ) );

		if( $repeat['postid'] )
		{
			$message = $this->ipsclass->lang['rep_samepost'];
		}

		if( ! $this->ismod )
		{
			if( $this->ipsclass->vars['rep_maxperday'] >= $this->ipsclass->vars['rep_repeat'] )
			{
				$klimit = intval($this->ipsclass->vars['rep_maxperday'] + 1);
			}
			else
			{
				$klimit = intval($this->ipsclass->vars['rep_repeat'] + 1);
			}

			$this->ipsclass->DB->simple_construct( array( 'select' => 'dateline,userid',
														  'from'   => 'reputation',
														  'where'  => "whoadded=".$this->ipsclass->member['id'],
														  'order'  => 'dateline DESC',
														  'limit'  => array(0, $klimit)
												)		);
			$this->ipsclass->DB->simple_exec();

			if( $this->ipsclass->DB->get_num_rows() )
			{
				$i = 0;
				while( $check = $this->ipsclass->DB->fetch_row() )
				{
					if( ( $i < $this->ipsclass->vars['rep_repeat'] ) && ( $check['userid'] == $post['author_id'] ) )
					{
						$message = $this->ipsclass->lang['rep_sameuser'];
					}
					if( ( ($i + 1) == $this->ipsclass->vars['rep_maxperday'] ) && ( ($check['dateline'] + 86400) > $this->now ) )
					{
						$message = $this->ipsclass->lang['rep_toomany'];
					}
					$i++;
				}
			}
		}

		//-----------------------------------------
		// What to do?
		//-----------------------------------------

		if( $this->ipsclass->input['do'] == 'addrep' )
		{
			if( $post['author_id'] == $this->ipsclass->member['id'] )
			{
				$message = $this->ipsclass->lang['rep_ownpost'];
			}

			$score = $this->fetch_reppower( $user, $this->ipsclass->input['reputation'] );
			$user['reputation'] += $score;

			$this->ipsclass->DB->simple_exec_query( array( 'update' => 'members', 'set' => "reputation=".$user['reputation'], 'where' => "id=".$post['author_id'] ) );

			$save = array( 'reputation' => $score,
						   'whoadded'   => $this->ipsclass->member['id'],
						   'reason'     => trim($this->ipsclass->input['reason']),
						   'dateline'   => $this->now,
						   'postid'     => $post['pid'],
						   'userid'     => $post['author_id']
						);

			$this->ipsclass->DB->do_insert( 'reputation', $save );

			$this->ipsclass->boink_it( $this->ipsclass->base_url."act=reputation&fid={$forum['id']}&amp;pid={$post['pid']}&amp;done=1" );
		}
		else
		{
			if( $post['author_id'] == $this->ipsclass->member['id'] )
			{
				# Is this your own post?
				$this->ipsclass->DB->simple_construct( array( 'select' => 'reputation, reason',
															  'from'   => 'reputation',
															  'where'  => "postid=".$post['pid'],
															  'order'  => 'dateline DESC'
													)		);
				$this->ipsclass->DB->simple_exec();

				if( $this->ipsclass->DB->get_num_rows() )
				{
					$total = 0;
					while( $postrep = $this->ipsclass->DB->fetch_row() )
					{
						$total += $postrep['reputation'];
						if( strlen($postrep['reason']) > 0 )
						{
							if( $postrep['reputation'] > 0 )
							{
								$posneg = 'pos';
							}
							elseif( $postrep['reputation'] < 0 )
							{
								$posneg = 'neg';
							}
							else
							{
								$posneg = 'balance';
							}

							$reasonbits .= $this->ipsclass->compiled_templates['skin_reputation']->reputation_reasonbits( $posneg, $postrep['reason'] );
						}
					}

					if( $total == 0 ){ $rep = 'rep_even'; }
					elseif( $total > 0 && $total <= 5 ){ $rep = 'rep_somewhat'; }
					elseif( $total > 5 && $total <= 15 ){ $rep = 'rep_positive'; }
					elseif( $total > 15 && $total <= 25 ){ $rep = 'rep_vpositive'; }
					elseif( $total > 25 ){ $rep = 'rep_enegative'; }
					elseif( $total < 0 && $total >= -5 ){ $rep = 'rep_somewhat'; }
					elseif( $total < -5 && $total >= -15 ){ $rep = 'rep_negative'; }
					elseif( $total < -15 && $total >= -25){ $rep = 'rep_vnegative'; }
					elseif( $total < -25 ){ $rep = 'rep_enegative'; }
				}
				else
				{
					$rep = 'rep_even';
				}

				$this->ipsclass->lang['rep_info']   = sprintf($this->ipsclass->lang['rep_info'], $post['topic_id'], $post['pid'], $this->ipsclass->lang[ $rep ] );
				$this->ipsclass->lang['rep_points'] = sprintf($this->ipsclass->lang['rep_points'], $user['reputation'] );

				$output = $this->ipsclass->compiled_templates['skin_reputation']->reputation_yourpost( $reasonbits );
			}
			else
			{
				# Not your post
				$this->ipsclass->lang['rep_text'] = sprintf($this->ipsclass->lang['rep_text'], $user['members_display_name']);
				$show['negativerep'] = ( $this->ismod || $this->ipsclass->member['g_rep_negative'] ) ? TRUE : FALSE;
				$show['closewindow'] = FALSE;

				$output = $this->ipsclass->compiled_templates['skin_reputation']->reputationbit( $show, $forum['id'], $post['pid'], $user['members_display_name'] );
			}
		}

		if( ! empty($message) )
		{
			$output = "<tr><td class='row2'>$message</td></tr>";
			$show['closewindow'] = TRUE;
		}

		$this->output = $this->ipsclass->compiled_templates['skin_reputation']->reputation( $show, $output );

		$this->ipsclass->print->pop_up_window( $this->ipsclass->vars['board_name'].' - '.$this->ipsclass->lang['reputation'], $this->output );
	}

	/*---------------------------------------------*/
	// fetch_reppower
	/*---------------------------------------------*/

	function fetch_reppower($user=array(),$rep='pos')
	{
		# User does not have permission to leave negative reputation
		if( ! $this->ipsclass->member['g_rep_negative'] )
		{
			$rep = 'pos';
		}

		if( ! $this->ipsclass->member['g_rep_use'] )
		{
			$rep = 0;
		}
		elseif( $this->ismod && $this->ipsclass->vars['rep_adminpower'] )
		{
			$reppower = ( $rep != 'pos' ) ? intval($this->ipsclass->vars['rep_adminpower'] * -1) : intval($this->ipsclass->vars['rep_adminpower']);
		}
		elseif( ( $this->ipsclass->member['posts'] < $this->ipsclass->vars['rep_minpost'] ) || ( $user['reputation'] < $this->ipsclass->vars['rep_minrep'] ) )
		{
			$reppower = 0;
		}
		else
		{
			$reppower = 1;

			if( $this->ipsclass->vars['rep_pcpower'] )
			{
				$reppower += intval($this->ipsclass->member['posts'] / $this->ipsclass->vars['rep_pcpower']);
			}

			if( $this->ipsclass->vars['rep_kppower'] )
			{
				$reppower += intval($user['reputation'] / $this->ipsclass->vars['rep_kppower']);
			}

			if( $this->ipsclass->vars['rep_rdpower'] )
			{
				$reppower += intval(($this->now - $this->ipsclass->member['joined']) / 86400 / $this->ipsclass->vars['rep_rdpower']);
			}

			if( $rep != 'pos' )
			{
				# Make negative reputation worth half of positive, but at least 1
				$reppower = intval($reppower / 2);
				$reppower = ( $reppower < 1 ) ? 1 : $reppower;
				$reppower *= -1;
			}
		}

		return $reppower;
	}
}

?>