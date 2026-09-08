<?
/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          M&M-ShoutBox v1 Alpha
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Created By:  mohamed
  Website:     http://www.invisioneyes.com
  Email:       traxman_08@hotmail.com
  file:        chatoo.php
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
All Codes have been written by mohamed for
      IPB 2.1.x M&M Chatoo v1 alpha
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
         Copyright (©) mohamed,2006
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

if (!defined('IN_IPB'))
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. Please be sure to return to the homepage, and visit the \"system\" link.";
	exit();
}

if( file_exists(ROOT_PATH."sources/action_public/mohamedmods/conf_chatoo.php") )
{
	require( ROOT_PATH."sources/action_public/mohamedmods/conf_chatoo.php" );
	if( count($CHATOO) > 0 )
	{
		foreach( $CHATOO as $item => $value )
		{
			$ipsclass->vars[ $item ] = $value;
		}
	}
}
else
{
	echo "You must upload conf_chatoo.php to your sources/action_public/mohamedmods/ folder before using this mod.<br />If the folder does not exist, create it first and then upload this file.";
	exit;
}
if( file_exists(ROOT_PATH."sources/action_public/mohamedmods/chatoo/chat.php") )
{
	require( ROOT_PATH."sources/action_public/mohamedmods/chatoo/chat.php" );
	if( count($SHOUT) > 0 )
	{
		foreach( $SHOUT as $item => $value )
		{
			$ipsclass->vars[ $item ] = $value;
		}
	}
}
else
{
	echo "You must upload chat.php to your sources/action_public/mohamedmods/chatoo folder  and chmode it to 777 before using this mod.<br />If the folder does not exist, create it first and then upload this file.";
	exit;
}
if( ! function_exists('str_ireplace') )
{
	//Start of Script Taken from dean.no-spam.bayley<at>virgin[dot]net on php.net
	function str_ireplace($needle="",$rep="",$haystack="",$pos=0)
	{
		if( ($needle == "") || ($rep == "") || ($haystack == "") ) return false;
		$b = explode(strtolower($needle), strtolower($haystack));
		foreach( $b as $bK => $bV )
		{
			$b[$bK] = substr($haystack,$pos,strlen($bV));
			$pos += strlen($bV) + strlen($needle);
		}
		return implode($rep,$b);
	}
	//END of Script Taken from dean.no-spam.bayley<at>virgin[dot]net on php.net
}
class chatoo
{
	# Classes
	var $ipsclass;
	var $base_url	= "";
	var $parser;
	var $output		= "";

	function auto_run()
	{
//Headers are sent to prevent browsers from caching.. IE is still resistent sometimes
	// Bust cache in the head
header( "Expires: Mon, 26 Jul 1997 05:00:00 GMT" ); 
header( "Last-Modified: " . gmdate( "D, d M Y H:i:s" ) . "GMT" ); 
header( "Cache-Control: no-cache, must-revalidate" ); 
header( "Pragma: no-cache" );
header("Content-Type: text/html; charset=utf-8");
        $this->member = $this->ipsclass->member;
		$this->base_url = $this->ipsclass->base_url;
		$this->ipsclass->load_language('lang_chatoo');

if($this->member['g_chatoo_on'] == 0)
		{
	        end;
		}
	switch ($this->ipsclass->input['do'])
		{
		    case "add":
			    $this->add();
			    break;
		    case "get":
			   $this->get();
			    break;	
		    case "mod":
			   $this->mod();
			    break;
			default:
				$this->get();
				break;
	   }
	}

function add()
	{
$add = $this->ipsclass->vars['m_shout_add'];
if(($add == 0 && $this->member['g_add_shout_close'] == 1 ) || ($add == 1 && $this->member['g_s_add_shout'] == 1 ))
	{
$namenotfound = $this->member['members_display_name'];
$prefix = $this->member['prefix'];
$suffix = $this->member['suffix']; 
$text = $this->ipsclass->input['c'];
$text = str_replace("---"," - - ",$text);
$namenotfound = str_replace("---"," - - ",$namenotfound);
//the message is cut of after certain letters
$max = $this->ipsclass->vars['max_let'];

if (strlen($text) > $max && $max > 0 && $max != "") 
	{
	$text = substr($text,0,$max); 
    }


	
if ($namenotfound != '' && $text != '') { 
	   $id = $this->ipsclass->get_date(time(), "LONG");
       $mid = intval($this->member['id']);
	   $name= $prefix.$namenotfound.$suffix;
       $split = array();
	   $split = explode("|", $this->ipsclass->vars['shouts']);
       $c = count ($split);
	   $shout = explode("---", $split[$c-1]);
	   if($shout[0] != "")
	{
	   $i = $shout[0]+1;
	}
	else
	{
		$i = 1;
	}


	   $content = "<"."?php\n";
       $content .= "\$SHOUT['shouts'] = \"{$this->ipsclass->vars['shouts']}|{$i} ---{$id} ---{$name} ---{$text} ---{$mid} ---\";\n";
	   $content .= "\n?".">\n";

		if( is_writable(ROOT_PATH."sources/action_public/mohamedmods/chatoo/chat.php") )
		{
	 		if( $fh = @fopen(ROOT_PATH."sources/action_public/mohamedmods/chatoo/chat.php", "w") )
			{
		 		@fputs($fh, $content, strlen($content) );
		 		@fclose($fh);
	 		}

		}
		else
		{
			exit;
		}
        $sanity = intval($this->ipsclass->vars['sanity']);
       if ($sanity > 0 && $sanity != "")
	   {
	$this->sanity($sanity); //maintenance
	   }
     }
   }
 else
   {
	 end;
   }
}
function mod() 
  {
	$id = intval($this->ipsclass->input['id']);
if($this->member['g_delete_shouts'] == 1 || $this->member['g_delete_his'] == 1)
	{
	if ($id) {
       $splited = array();
	   $splited = explode("|", $this->ipsclass->vars['shouts']);
       $sc = count($splited);
	   $content = "<"."?php\n";
       for($i=0; $i<=$sc; $i++)
       {
         $shout = explode("---", $splited[$i]);
		
		if($shout[0] == $id || $shout[0] == "")
		   {
			continue;
	       }
		   else
		   {
       $x .= "|{$shout[0]} ---{$shout[1]} ---{$shout[2]} ---{$shout[3]} ---{$shout[4]} ---";
		   }
	   }
$content .= "\$SHOUT['shouts'] = \"{$x}\"";
	   $content .= "\n?".">\n";

		if( is_writable(ROOT_PATH."sources/action_public/mohamedmods/chatoo/chat.php") )
		{
	 		if( $fh = @fopen(ROOT_PATH."sources/action_public/mohamedmods/chatoo/chat.php", "w") )
			{
		 		@fputs($fh, $content, strlen($content) );
		 		@fclose($fh);
	 		}
		}
		else
		{
			exit;
		}
	}
	else
	{
		end;
	}
  }
  }
function sanity($position) 
  {
       if ($position > 0 && $position != "")
	   {
       $splited = array();
	   $splited = explode("|", $this->ipsclass->vars['shouts']);
       $ss = count($splited);
	   if($ss > $position)
		 {
       $sc = $ss-$position;
		   
	   $content = "<"."?php\n";
       for($i=0; $i>=$sc; $i++)
       {
         $shout = explode("---", $splited[$i]);
		
		if($shout[0] == $id || $shout[0] == "")
		   {
			continue;
	       }
		   else
		   {
       $x .= "|{$shout[0]} ---{$shout[1]} ---{$shout[2]} ---{$shout[3]} ---{$shout[4]} ---";
		   }
	   }
$content .= "\$SHOUT['shouts'] = \"{$x}\"";
	   $content .= "\n?".">\n";

		if( is_writable(ROOT_PATH."sources/action_public/mohamedmods/chatoo/chat.php") )
		{
	 		if( $fh = @fopen(ROOT_PATH."sources/action_public/mohamedmods/chatoo/chat.php", "w") )
			{
		 		@fputs($fh, $content, strlen($content) );
		 		@fclose($fh);
	 		}
		}
		else
		{
			exit;
		}
		 }
		 else
		   {
			 end;
		   }

	}
else
	{
		   end;
	}
}
	function get()
	{
$lastID = $_GET[lastID];
		if( ! $this->member['g_s_view_shouts'] )
		{
			if($lastID == -1)
			{
		$i = 1;
		$time = $this->ipsclass->get_date(time(), "LONG");
		$name = "Chatoo";
		$text = $this->ipsclass->lang['shout_section_permission'];

	echo $i." ---".$time." ---".$name." ---".$text." ---";
	    }
		else
			{end;}
		}
		else
		{

       $lastID = intval($lastID);
       $splits = array();
	   $splits = explode("|", $this->ipsclass->vars['shouts']);
       $limit = count($splits);
			
       for($i=0; $i<=$limit; $i++)
       {
         $shout = explode("---", $splits[$i]);
		
		 if($shout[0] > $lastID && $shout[0] != "")
		   {
$this->ipsclass->vars['EMOTICONS_URL'] = "style_emoticons/default";

        require_once( ROOT_PATH."sources/handlers/han_parse_bbcode.php" );
        $this->parser                      =  new parse_bbcode();
        $this->parser->ipsclass            =& $this->ipsclass;
        $this->parser->allow_update_caches = 1;
		$this->parser->parse_smilies = 1;
        $this->parser->parse_bbcode  = $this->member['g_shout_bbcode'];
		$this->parser->parse_html    = $this->member['g_shout_html'];
        $this->parser->parse_nl2br   = 1;
        $text = $this->parser->pre_db_parse($shout[3]);
        $name = $this->parser->pre_db_parse($shout[2]);
        $text = $this->parser->pre_display_parse($text);
        $name = $this->parser->pre_display_parse($name);
		echo $shout[0]." ---".$shout[1]." ---".$name." ---".$text." ---".$shout[4]." ---"; // --- is being used to separete the fields in the output
		   }
	   }
	     
	   }
	}
}
?>