<?php

/*
+---------------------------------------------------------------------------------------------
|
| 	M&M Chatoo System v1.0 Alpha by  mohamed 
|	(C)Copyright mohamed 2006
|   all codes written by mohamed
+---------------------------------------------------------------------------------------------
+	All credits to mohamed at invisioneyes.com
+ 	must remain intact or use of this
+	modification is not permitted!
+
+------------------------------------------*/

if( ! defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit;
}

class chatoo_extra
{
	var $class = "";

	/*---------------------------------------------*/
	// register_class
	/*---------------------------------------------*/

	function register_class($class)
	{
		$this->class =& $class;
	}

	/*---------------------------------------------*/
	// pm/emails tools
	/*---------------------------------------------*/
  function moh_pmemail($data)
	{
			require_once(ROOT_PATH.'sources/lib/func_msg.php');
			$this->msglib = new func_msg();
            $this->msglib->ipsclass =& $this->ipsclass;
			$this->msglib->init();
		
			
				$this->msglib->register_class(&$this);
				$this->msglib->force_pm = 1;
				$this->post_key = md5(microtime());


				$this->msglib->to_by_id    = $data['smempm'];
 				$this->msglib->from_member['id'] = $data['id'];
                $this->msglib->from_member['members_display_name'] = $data['members_display'];
 				$this->msglib->msg_title   = $data['title']."_Info";
	 			$this->msglib->msg_post    = $data['custom'];
 				$this->msglib->send_pm(array(
							'save_only' => '',
							'orig_id'   => '',
							'preview'   => 0,
							'track'     => 0,
							'add_sent'  => 0,
							'hide_cc'   => 0
						)     );

 				if ($this->msglib->error != '')
 				{
 					$this->msglib->send_form(0, $this->msglib->error);
 					$this->output .= $this->msglib->output;
 					return;
 				}

   

		require_once( ROOT_PATH."sources/classes/class_email.php" );
		
		$this->email           = new emailer();
		$this->email->ipsclass =& $this->ipsclass;
		$this->email->email_init();
				$this->email->get_template("email_member");
				$this->email->build_message( array(	'MESSAGE'     => str_replace( "<br>", "\n", str_replace( "\r", "", $data['custom'] ) ),
					                             'MEMBER_NAME' 	    => $data['smem'],
							                       'FROM_NAME'      => $data['name'],
					)		);
				$this->email->subject = $data['title']."_Info";
				$this->email->to      = $data['email'];
	            $this->email->from    = $data['memail'];
				$this->email->send_mail();
	}

   function moh_pm($data)
	{
			require_once(ROOT_PATH.'sources/lib/func_msg.php');
			$this->msglib = new func_msg();
            $this->msglib->ipsclass =& $this->ipsclass;
			$this->msglib->init();
		
			
				$this->msglib->register_class(&$this);
				$this->msglib->force_pm = 1;
				$this->post_key = md5(microtime());


				$this->msglib->to_by_id    = $data['smempm'];
 				$this->msglib->from_member['id'] = $data['id'];
                $this->msglib->from_member['members_display_name'] = $data['members_display'];
 				$this->msglib->msg_title   = $data['title']."_Info";
	 			$this->msglib->msg_post    = $data['custom'];
 				$this->msglib->send_pm(array(
							'save_only' => '',
							'orig_id'   => '',
							'preview'   => 0,
							'track'     => 0,
							'add_sent'  => 0,
							'hide_cc'   => 0
						)     );

 				if ($this->msglib->error != '')
 				{
 					$this->msglib->send_form(0, $this->msglib->error);
 					$this->output .= $this->msglib->output;
 					return;
 				}
	}
  function moh_email($data)
	{
			require_once( ROOT_PATH."sources/classes/class_email.php" );
		
		$this->email           = new emailer();
		$this->email->ipsclass =& $this->ipsclass;
		$this->email->email_init();
				$this->email->get_template("email_member");
				$this->email->build_message( array(	'MESSAGE'     => str_replace( "<br>", "\n", str_replace( "\r", "", $data['custom'] ) ),
					                             'MEMBER_NAME' 	    => $data['smem'],
							                       'FROM_NAME'      => $data['name'],
					)		);
				$this->email->subject = $data['title']."-Info";
				$this->email->to      = $data['email'];
	            $this->email->from    = $data['memail'];
				$this->email->send_mail();
          return true;
	}
   /*-------------------------------------------------------------------------*/
	// HTML: add smilie box.
	// ---------------------------------| Taken from IPB Files
	// Inserts the clickable smilies box
	/*-------------------------------------------------------------------------*/
	
	function html_add_smilie_box($in_html="")
	{
		$show_table = 0;
		$count      = 0;
		$smilies    = "<tr align='center'>\n";
		$smilie_id  = 0;
		$total = 0;
		
		//-----------------------------------------
		// Get the smilies from the DB
		//-----------------------------------------
		
		if ( ! is_array( $this->ipsclass->cache['emoticons'] ) )
		{
			$this->ipsclass->cache['emoticons'] = array();
			
			$this->ipsclass->DB->simple_construct( array( 'select' => 'typed,image,clickable,emo_set', 'from' => 'emoticons' ) );
			$this->ipsclass->DB->simple_exec();
		
			while ( $r = $this->ipsclass->DB->fetch_row() )
			{
				$this->ipsclass->cache['emoticons'][] = $r;
			}
		}
		
	
		foreach( $this->ipsclass->cache['emoticons'] as $get => $clickable )
		{
			if( $clickable['clickable'] )
			{
				$total++;
			}
		}
		
		foreach( $this->ipsclass->cache['emoticons'] as $a_id => $elmo )
		{
			if ( $elmo['emo_set'] != $this->ipsclass->skin['_emodir'] )
			{
				continue;
			}
			
			if ( ! $elmo['clickable'] )
			{
				continue;
			}
					
			$show_table++;
			$count++;
			$smilie_id++;
			
			
			//-----------------------------------------
			// Make single quotes as URL's with html entites in them
			// are parsed by the browser, so ' causes JS error :o
			//-----------------------------------------
			
			if (strstr( $elmo['typed'], "&#39;" ) )
			{
				$in_delim  = '"';
				$out_delim = "'";
			}
			else
			{
				$in_delim  = "'";
				$out_delim = '"';
			}
			
			$smilies .= "<td><a href={$out_delim}javascript:commentEmoticon($in_delim".$elmo['typed']."$in_delim, 'smid_$smilie_id'){$out_delim}><img id='smid_$smilie_id' src=\"".$this->ipsclass->vars['EMOTICONS_URL']."/".$elmo['image']."\" alt='smilie' border='0' /></a></td>\n";
			
		}
		
		//-----------------------------------------
		// Write 'em
		//-----------------------------------------
		
		if ( $count != $this->ipsclass->vars['emo_per_row'] )
		{
			for ($i = $count ; $i < $this->ipsclass->vars['emo_per_row'] ; ++$i)
			{
				$smilies .= "<td>&nbsp;</td>\n";
			}
			$smilies .= "</tr>";
		}
		        $this->ipsclass->load_template('skin_post');

		$table = $this->ipsclass->compiled_templates['skin_post']->smilie_tablee();
		
		if ($show_table != 0)
		{
			$table   = preg_replace( "/<!--THE SMILIES-->/", $smilies, $table );
			$in_html = preg_replace( "/<!--SMILIE TABLE-->/", $table, $in_html );
		}
		
		return $in_html;
	}
	function smilie_alpha_sort($a, $b)
	{
		return strcmp( $a['typed'], $b['typed'] );
	}

}

?>