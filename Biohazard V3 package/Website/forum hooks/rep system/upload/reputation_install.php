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

error_reporting(E_ERROR | E_WARNING | E_PARSE);
set_magic_quotes_runtime(0);
@set_time_limit(1200);

//-----------------------------------------------
// USER CONFIGURABLE ELEMENTS
//-----------------------------------------------

define('THIS_MODULE', 'REPUTATION');
define('ROOT_PATH', './');
define('KERNEL_PATH', ROOT_PATH.'ips_kernel/');
define('CACHE_PATH' , ROOT_PATH);

//-----------------------------------------------
// NO USER EDITABLE SECTIONS BELOW
//-----------------------------------------------

define('IN_ACP', 1);
define('IN_IPB', 1);
define('IN_DEV', 0);
define('IPBVERSION', '2.1.5');
define('IPB_LONG_VERSION', '21011');

define('USE_SHUTDOWN', 0);
define('SAFE_MODE_ON', 0);

$INFO = array();

//-----------------------------------------------
// INIT CLASSES
//-----------------------------------------------

require( ROOT_PATH.'sources/ipsclass.php' );

$ipsclass			= new ipsclass();
$ipsclass->vars		=& $INFO;
$ipsclass->template = new template();
$ipsclass->parse_incoming();
$ipsclass->initiate_ipsclass();

//-----------------------------------------------
// IMPORT $INFO!
//-----------------------------------------------

$require = ROOT_PATH.'conf_global.php';
if( ! file_exists($require) )
{
	install_error("Could not locate '$require'. You may need to enter a value for the root path in this installer script, to do this, simply open up this script in a text editor and enter a value in \$root - remember to add a trailing slash. NT users will need to use double backslashes");
}

require_once( $require );
$INFO['sql_driver'] = strtolower($INFO['sql_driver']);

require_once( KERNEL_PATH.'class_db_'.$INFO['sql_driver'].'.php');
$ipsclass->DB = new db_driver();

$ipsclass->DB->obj['sql_database']     = $INFO['sql_database'];
$ipsclass->DB->obj['sql_user']         = $INFO['sql_user'];
$ipsclass->DB->obj['sql_pass']         = $INFO['sql_pass'];
$ipsclass->DB->obj['sql_host']         = $INFO['sql_host'];
$ipsclass->DB->obj['sql_tbl_prefix']   = $INFO['sql_tbl_prefix'];
$ipsclass->DB->obj['query_cache_file'] = ROOT_PATH.'sources/sql/'.$INFO['sql_driver'].'_admin_queries.php';
$ipsclass->DB->obj['use_shutdown']     = 0;

//-----------------------------------------------
// REQUIRED VARS?
//-----------------------------------------------

if( is_array($ipsclass->DB->connect_vars) && count($ipsclass->DB->connect_vars) )
{
	foreach( $ipsclass->DB->connect_vars as $k => $v )
	{
		$ipsclass->DB->connect_vars[ $k ] = $INFO[ $k ];
	}
}

//-----------------------------------------------
// GET A DB CONNECTION
//-----------------------------------------------

if( ! $ipsclass->DB->connect() )
{
	install_error( "Connection error:<br /><br />".$ipsclass->DB->error );
}

//--------------------------------
// CONSTANT
//--------------------------------

define('SQL_PREFIX', $ipsclass->DB->obj['sql_tbl_prefix']);
define('SQL_DRIVER', $INFO['sql_driver']);

//-----------------------------------------------
// SET UP OUR VARS
//-----------------------------------------------

require( KERNEL_PATH.'class_converge.php' );
$ipsclass->converge = new class_converge( $ipsclass->DB );

$ipsclass->core	    = new core_functions();
$ipsclass->member   = $ipsclass->core->get_member();

//-----------------------------------------------
// LOGIN?
//-----------------------------------------------

if( isset($ipsclass->input['act']) )
{
	if( file_exists(ROOT_PATH.'reputation_install.lock') )
	{
		install_error( "This installer is locked!<br />Please (via FTP) remove the 'reputation_install.lock' file" );
	}

	if( $ipsclass->input['act'] != 'login' )
	{
		if( ! $ipsclass->member['id'] )
		{
			$ipsclass->core->login_screen( "You do not have access to this install script!" );
		}

		if( $ipsclass->return_md5_check() != $ipsclass->securekey )
		{
			$ipsclass->core->login_screen( "You do not have access to this install script!" );
		}

		if( ! $ipsclass->member['g_access_cp'] )
		{
			$ipsclass->core->login_screen( "You must be an admin to access this install script!" );
		}
	}
	else
	{
		if( empty($ipsclass->input['username']) )
		{
			$ipsclass->core->login_screen( "You must enter a username before proceeding." );
		}

		if( empty($ipsclass->input['password']) )
		{
			$ipsclass->core->login_screen( "You must enter a password before proceeding." );
		}

		$ipsclass->DB->query( "SELECT m.*, g.* FROM `".SQL_PREFIX."members` m, `".SQL_PREFIX."groups` g WHERE LOWER(name)='".strtolower($ipsclass->input['username'])."' AND m.mgroup=g.g_id" );
		$mem = $ipsclass->DB->fetch_row();

		if( empty($mem['id']) )
		{
			$ipsclass->core->login_screen( "Could not find a record matching that username, please check the spelling." );
		}

		$ipsclass->converge->converge_load_member($mem['email']);

		if( ! $ipsclass->converge->member['converge_id'] )
		{
			$ipsclass->core->login_screen( "Could not find a record matching that username, please check the spelling." );
		}

		$pass = md5($ipsclass->input['password']);

		if( $ipsclass->converge->converge_authenticate_member( $pass ) != TRUE )
		{
			$ipsclass->core->login_screen( "The password you entered is not correct." );
		}
		else
		{
			if( $mem['g_access_cp'] != 1 )
			{
				$ipsclass->core->login_screen( "You do not have access to the Admin CP." );
			}
			else
			{
				$ipsclass->member = $mem;
				$ipsclass->core->redirect( "reputation_install.php?act=dointro&amp;loginkey={$mem['member_login_key']}&amp;securekey=".$ipsclass->return_md5_check()."&amp;mid={$mem['id']}", "Thanks for logging in..." );
			}
		}
	}
}

//-----------------------------------------------
// MAIN LOGIC
//-----------------------------------------------

switch( $ipsclass->input['act'] )
{
	case 'dointro':
		do_intro();
		break;
	case 'doinstall':
		do_install();
		break;
	case 'doupgrade':
		do_upgrade();
		break;
	case 'doremove':
		do_remove();
		break;
	case 'dotemplate':
		do_template();
		break;
	case 'dotemplaterecache':
		do_template_recache();
		break;
	case 'dorecache':
		do_finish();
		break;

	default:
		$ipsclass->core->login_screen();
		break;
}

//-----------------------------------------------
// install_error
//-----------------------------------------------

function install_error($msg="")
{
	global $ipsclass;

	$ipsclass->template->contents .= "<br />
			<div class='borderwrap'>
				<p style='font-size:17pt;'><b>Install Error!</b></p><br /><br />
				<div class='warnbox'>
					<strong style='font-size:16px; color:#F00;'>Warning!</strong><br /><br />
					<b>The following errors must be rectified before continuing!</b><br />Please go back and try again!<br /><br />$msg
				</div><br />
			</div><br />";

	$ipsclass->template->output();
}

//-----------------------------------------------
// do_intro
//-----------------------------------------------

function do_intro()
{
	global $ipsclass;

	$ipsclass->template->contents .= "<br />
			<div class='borderwrap'>
				<p style='font-size:17pt;'><b>Welcome!</b></p><br /><br />
				<div class='tablepad'>
					You are about to perform an install.<br /><br />
					Before we go any further, please ensure that all the files and folders have been uploaded.<br /><br />
					Clicking the <strong>[ Fresh Install / Re-Install ]</strong> or <strong>[ Upgrade ]</strong> or <strong>[ Uninstall ]</strong> button<br />will begin the installation process on your database.<br /><br />
					<strong><font color='red' size='2pt'>WARNING:</font><br />BY CLICKING ON [ FRESH INSTALL / RE-INSTALL ] OR [ UNINSTALL ] BUTTON,<br />ALL EXISTING ".THIS_MODULE." DATA WILL BE LOST!!!</strong><br /><br />
					In order to prevent possible browser crashes during this script, we strongly recommend that you disable any additional toolbars you may be using on your browser, such as the Google toolbar etc.
				</div><br />
			</div><br />";

	$msg = "";
	$chk = array('reputation_mysql.php', 'reputation_settings.xml', 'reputation_templates.xml', 'sources/action_admin/reputation.php');
	foreach( $chk as $v )
	{
		if( ! file_exists(ROOT_PATH.$v) )
		{
			$msg .= "Cannot locate <b>./$v</b><br />";
		}
	}

	if( empty($msg) )
	{
		$ipsclass->template->contents .= "<div align='center'><input type='button' value='Fresh Install / Re-Install' onclick=\"if(confirm('Connection succeeded. The existing data will be lost! OK to proceed?')){this.disabled=true; self.location.href='reputation_install.php?act=doinstall&amp;loginkey={$ipsclass->input['loginkey']}&amp;securekey={$ipsclass->input['securekey']}&amp;mid={$ipsclass->input['mid']}';}else{alert('OK, action cancelled!');}\" /> <input type='button' value='Upgrade' onclick=\"this.disabled=true; self.location.href='reputation_install.php?act=doupgrade&amp;loginkey={$ipsclass->input['loginkey']}&amp;securekey={$ipsclass->input['securekey']}&amp;mid={$ipsclass->input['mid']};'\" /> <input type='button' value='Uninstall' onclick=\"if(confirm('Are you sure you want to uninstall this module?')){this.disabled=true; self.location.href='reputation_install.php?act=doremove&amp;loginkey={$ipsclass->input['loginkey']}&amp;securekey={$ipsclass->input['securekey']}&amp;mid={$ipsclass->input['mid']}';}else{alert('OK, action cancelled!');}\" /></div><br />";
	}
	else
	{
		$ipsclass->template->contents .= "
			<div class='warnbox'>
				<strong>Warning!</strong>
				<b>The following errors must be rectified before continuing!</b><br /><br />$msg
				Please make sure you uploaded all the folders and files.
			</div><br />";
	}

	$ipsclass->template->output();
}

//-----------------------------------------------
// do_install
//-----------------------------------------------

function do_install()
{
	global $ipsclass;

	$ipsclass->template->contents .= "<br />
			<div class='borderwrap'>
				<p style='font-size:17pt;'><b>Success!</b></p><br />&nbsp;&nbsp;<b>Processing ...</b><br /><br />
				<div class='tablepad' style='height:175px; overflow:-moz-scrollbars-vertical; overflow-y:auto;'>";

	//-----------------------------------
	// Empty the existing DB
	//-----------------------------------

	do_drop();

	//-----------------------------------
	// Populate the DB
	//-----------------------------------

	do_create();

	//-----------------------------------
	// Done?
	//-----------------------------------

	$ipsclass->template->contents .= "</div><br />
				<div class='tablepad'>
					<b>The installation process is almost complete.<br />The next step will install the templates into your database.</b><br /><br />
					<center><b><a href=\"reputation_install.php?act=dotemplate&amp;loginkey={$ipsclass->input['loginkey']}&amp;securekey={$ipsclass->input['securekey']}&amp;mid={$ipsclass->input['mid']}\">&raquo; CLICK HERE TO CONTINUE &laquo;</a></b></center>
				</div><br />
			</div><br />";

	$ipsclass->template->output();
}

//-----------------------------------------------
// do_upgrade
//-----------------------------------------------

function do_upgrade()
{
	global $ipsclass;

	$ipsclass->template->contents .= "<br />
			<div class='borderwrap'>
				<p style='font-size:17pt;'><b>Success!</b></p><br />&nbsp;&nbsp;<b>Processing ...</b><br /><br />
				<div class='tablepad' style='height:175px; overflow:-moz-scrollbars-vertical; overflow-y:auto;'>";

	//-----------------------------------
	// Populate the DB
	//-----------------------------------
/*
	$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."skin_templates` WHERE group_name='skin_reputation';" );
	$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."skin_templates_cache` WHERE template_group_name='skin_reputation';" );

	$conf = $ipsclass->DB->simple_exec_query( array ( 'select' => 'conf_title_id', 'from' => 'conf_settings_titles', 'where' => "conf_title_keyword='repoptions'" ) );
	if( $conf['conf_title_id'] )
	{
		$confgroup = $conf['conf_title_id'];

		$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."conf_settings_titles` WHERE conf_title_id='".$confgroup."';" );
		$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."conf_settings` WHERE conf_group='".$confgroup."';" );
		$ipsclass->template->contents .= "<font color='red'>DELETE FROM `".SQL_PREFIX."conf_settings_titles` WHERE conf_title_id='$confgroup';<br />DELETE FROM `".SQL_PREFIX."conf_settings` WHERE conf_group='$confgroup';</font><br />";
	}
*/
	do_create();

	//-----------------------------------
	// Changes on Mar 10, 2006
	//-----------------------------------

	//

	//-----------------------------------
	// Done?
	//-----------------------------------

	$ipsclass->template->contents .= "</div><br />
				<div class='tablepad'>
					<b>You have now successfully upgraded to newest version.<br />
					Click the link below to build the required templates.</b><br /><br />
					<center><b><a href=\"reputation_install.php?act=dotemplate&amp;loginkey={$ipsclass->input['loginkey']}&amp;securekey={$ipsclass->input['securekey']}&amp;mid={$ipsclass->input['mid']}\">&raquo; CLICK HERE TO CONTINUE &laquo;</a></b></center>
				</div><br />
			</div><br />";

	$ipsclass->template->output();
}

//-----------------------------------------------
// do_remove
//-----------------------------------------------

function do_remove()
{
	global $ipsclass;

	$ipsclass->template->contents .= "<br />
			<div class='borderwrap'>
				<p style='font-size:17pt;'><b>Success!</b></p><br />&nbsp;&nbsp;<b>Updating tables ...</b><br /><br />
				<div class='tablepad' style='height: 175px; overflow:-moz-scrollbars-vertical; overflow-y: auto;'>";

	//-----------------------------------
	// Empty the DB
	//-----------------------------------

	do_drop();

	//-----------------------------------
	// Done?
	//-----------------------------------

	$ipsclass->template->contents .= "</div><br />
				<div class='tablepad'>
					<b>Follow the instruction to complete uninstall.</b><br /><br />
					<center><b>&raquo; <a href=\"reputation_install.php?act=dointro&amp;loginkey={$ipsclass->input['loginkey']}&amp;securekey={$ipsclass->input['securekey']}&amp;mid={$ipsclass->input['mid']}\">RE-INSTALL MODULE</a></b> &nbsp;&nbsp;&curren;&curren;&curren;&nbsp;&nbsp; <b><a href=\"{$ipsclass->vars['board_url']}/index.php?act=idx\">RETURN BOARD INDEX</a> &laquo;</b></center>
				</div><br />
			</div><br />";

	$ipsclass->template->output();
}

//-----------------------------------------------
// do_template
//-----------------------------------------------
	
function do_template()
{
	global $ipsclass;

	require_once( KERNEL_PATH.'class_xml.php' );
	$xml = new class_xml(2);
	$xml->lite_parser = 1;

	$setting_content = implode("", file(ROOT_PATH.'reputation_templates.xml'));
	$xml->xml_parse_document( $setting_content );

	if( ! is_array($xml->xml_array['templateexport']['templategroup']['template']) )
	{
		install_error( "Error with dscript_templates.xml - could not process XML properly" );
	}

	foreach( $xml->xml_array['templateexport']['templategroup']['template'] as $id => $entry )
	{
		$row = $ipsclass->DB->simple_exec_query( array( 'select' => 'suid',
														'from'   => 'skin_templates',
														'where'  => "group_name='{$entry[ 'group_name' ]['VALUE']}' AND func_name='{$entry[ 'func_name' ]['VALUE']}' and set_id=1"
												)      );

		if( $row['suid'] )
		{
			$ipsclass->DB->do_update('skin_templates', array( 'func_data'		=> $entry[ 'func_data' ]['VALUE'],
									 						  'section_content' => $entry[ 'section_content' ]['VALUE'],
									 						  'updated'			=> time()
									 						), 'suid='.$row['suid'] );
		}
		else
		{
			$ipsclass->DB->do_insert('skin_templates', array('func_data'		=> $entry[ 'func_data' ]['VALUE'],
									 						 'func_name'		=> $entry[ 'func_name' ]['VALUE'],
									 						 'section_content'	=> $entry[ 'section_content' ]['VALUE'],
									 						 'group_name'		=> $entry[ 'group_name' ]['VALUE'],
									 						 'updated'			=> time(),
									 						 'set_id'			=> 1
									 				)		);
		}
	}

	//-----------------------------------
	// Done?
	//-----------------------------------

	$ipsclass->template->contents .= "<br />
			<div class='borderwrap'>
				<p style='font-size:17pt;'><b>Success!</b></p><br />&nbsp;&nbsp;<b>Processing ...</b><br /><br />
				<div class='tablepad' style='height:175px; overflow:-moz-scrollbars-vertical; overflow-y:auto;'>&nbsp;</div><br />
				<div class='tablepad'>
					<b>Template files installed!<br />The installation process is now complete.<br />
					Click the link below to build the required caches.</b><br /><br />
					<center><b><a href=\"reputation_install.php?act=dotemplaterecache&amp;loginkey={$ipsclass->input['loginkey']}&amp;securekey={$ipsclass->input['securekey']}&amp;mid={$ipsclass->input['mid']}\">&raquo; CLICK HERE TO FINISH &laquo;</a></b></center>
				</div><br />
			</div><br />";

	$ipsclass->template->output();
}

//-----------------------------------------------
// do_template_recache
//-----------------------------------------------

function do_template_recache()
{
	global $ipsclass;

	require_once( ROOT_PATH.'sources/lib/admin_cache_functions.php' );
	$acp = new admin_cache_functions();
	$acp->ipsclass =& $ipsclass;

	$row = $ipsclass->DB->simple_exec_query ( array ( 'select' => 'conf_value, conf_default', 'from' => 'conf_settings', 'where' => "conf_key='ipb_img_url'" ) );
	$ipsclass->vars['ipb_img_url'] = $row['conf_value'] ? $row['conf_value'] : $row['conf_default'];
	if( $ipsclass->vars['ipb_img_url'] == "{blank}" )
	{
		$ipsclass->vars['ipb_img_url'] = "";
	}

	$justdone = intval($ipsclass->input['justdone']);
	$justdone = $justdone ? $justdone : 1;

	//-----------------------------------
	// Get skins
	//-----------------------------------

	$ipsclass->DB->simple_construct( array( 'select' => '*',
											'from'   => 'skin_sets',
											'where'  => 'set_skin_set_id > '.$justdone,
											'order'  => 'set_skin_set_id',
											'limit'  => array(0, 1)
									)		);
	$ipsclass->DB->simple_exec();

	$r = $ipsclass->DB->fetch_row();

	if( $r['set_skin_set_id'] )
	{
		$acp->_rebuild_all_caches( array($r['set_skin_set_id']) );
		$extra = implode("<br />", $acp->messages);

		$ipsclass->core->redirect( "reputation_install.php?act=dotemplaterecache&amp;justdone={$r['set_skin_set_id']}&amp;loginkey={$ipsclass->input['loginkey']}&amp;securekey={$ipsclass->input['securekey']}&amp;mid={$ipsclass->input['mid']}", "Rebuild cache for skin set {$r['set_name']}<br />{$extra}<br />Proceeding to the next skin..." );
	}
	else
	{
		$ipsclass->core->redirect( "reputation_install.php?act=dorecache&amp;loginkey={$ipsclass->input['loginkey']}&amp;securekey={$ipsclass->input['securekey']}&amp;mid={$ipsclass->input['mid']}", "No more skins to rebuild..." );
	}
}

//-----------------------------------------------
// do_finish
//-----------------------------------------------

function do_finish()
{
	global $ipsclass;

	//-----------------------------------
	// Group cache
	//-----------------------------------

	require_once( ROOT_PATH.'sources/action_admin/groups.php' );
	$lib = new ad_groups();
	$lib->ipsclass =& $ipsclass;
	$lib->rebuild_group_cache();
	unset($lib);

	//-----------------------------------
	// Settings
	//-----------------------------------

	require_once( ROOT_PATH.'sources/action_admin/settings.php' );
	$lib = new ad_settings();
	$lib->ipsclass =& $ipsclass;
	$lib->setting_rebuildcache();
	unset($lib);

	//-----------------------------------
	// Module cache
	//-----------------------------------

	require_once( ROOT_PATH.'sources/action_admin/reputation.php' );
	$lib = new ad_reputation();
	$lib->ipsclass =& $ipsclass;
	$lib->update_rep_cache();
	unset($lib);

	//-----------------------------------
	// Attempt to lock the install..
	//-----------------------------------

	do_lock();

	$ipsclass->template->output();
}

//-----------------------------------------------
// do_create
//-----------------------------------------------

function do_create()
{
	global $ipsclass;

	$root = ROOT_PATH;
	if( $root == './' ) $root = str_replace("\\", "/", getcwd()).'/';

	require_once( ROOT_PATH.'reputation_mysql.php' );

	//-----------------------------------
	// Populate the DB
	//-----------------------------------

	$tbl_exist = array();
	$tbl_exist = $ipsclass->DB->get_table_names();

	$tbl_create = array('reputation','reputationlevel');
	for( $i = 0; $i < count($tbl_create); $i++ )
	{
		if( ! in_array(SQL_PREFIX.$tbl_create[$i], $tbl_exist) )
		{
			$ipsclass->DB->query( $TABLE[$i] );
			$sqltext = str_replace("\n", "<br />\n", $TABLE[$i]);
			$ipsclass->template->contents .= "<font color='red'>$sqltext</font><br /><br />";
		}
	}

	//-----------------------------------
	// Populate tables...
	//-----------------------------------

	$g_alt = array('g_rep_use','g_rep_negative','g_rep_seeown','g_rep_hide');
	for( $i = 0; $i < count($g_alt); $i++ )
	{
		if( ! $ipsclass->DB->field_exists( $g_alt[$i], "groups" ) )
		{
			$ipsclass->DB->query( $GALT[$i] );
			$ipsclass->template->contents .= "<font color='red'>$GALT[$i]</font><br />";
		}
	}

	if( ! $ipsclass->DB->field_exists( 'reputation', "members" ) )
	{
		$ipsclass->DB->query( "ALTER TABLE `".SQL_PREFIX."members` ADD `reputation` int(10) NOT NULL default '10';" );
		$ipsclass->template->contents .= "<font color='red'>ALTER TABLE `".SQL_PREFIX."members` ADD `reputation` int(10) NOT NULL default '10';</font><br />";
	}

	$level = $ipsclass->DB->simple_exec_query( array('select' => 'COUNT(*) as cnt', 'from' => 'reputationlevel' ) );
	if( $level['cnt'] < 1 )
	{
		for( $i = 0; $i < count($INSC); $i++ )
		{
			$ipsclass->DB->query( $INSC[$i] );
			$ipsclass->template->contents .= "<font color='red'>$INSC[$i]</font><br />";
		}
	}

	if( $ipsclass->input['act'] == 'doupgrade' )
	{
		$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."cache_store` WHERE cs_key='replevel';");
	}
	$ipsclass->DB->query( "INSERT INTO `".SQL_PREFIX."cache_store` (cs_key, cs_array) VALUES ('replevel', 1);" );

	//-----------------------------------
	// Unpack: reputation_settings
	//-----------------------------------

	$updated  = 0;
	$inserted = 0;
	$need_update    = array();
	$cur_settings   = array();
	$setting_groups = array();
	$setting_groups_by_key = array();

	$ipsclass->DB->simple_construct( array( 'select' => 'conf_id, conf_key, conf_value', 'from' => 'conf_settings', 'order' => 'conf_id' ) );
	$ipsclass->DB->simple_exec();

	while( $r = $ipsclass->DB->fetch_row() )
	{
		$cur_settings[ $r['conf_key'] ] = $r['conf_id'];
	}

	if( IN_DEV )
	{
		$ipsclass->DB->simple_construct( array( 'select' => '*', 'from' => 'conf_settings_titles', 'order' => 'conf_title_title' ) );
		$ipsclass->DB->simple_exec();
	}
	else
	{
		$ipsclass->DB->simple_construct( array( 'select' => '*', 'from' => 'conf_settings_titles', 'where' => 'conf_title_noshow=0', 'order' => 'conf_title_title' ) );
		$ipsclass->DB->simple_exec();
	}

	while( $r = $ipsclass->DB->fetch_row() )
	{
		$setting_groups[ $r['conf_title_id'] ] = $r;
		$setting_groups_by_key[ $r['conf_title_keyword'] ] = $r;
	}

	require_once( KERNEL_PATH.'class_xml.php' );
	$xml = new class_xml(1);
	$xml->lite_parser = 1;

	$setting_content = implode("", file(ROOT_PATH.'reputation_settings.xml'));
	$xml->xml_parse_document( $setting_content );

	if( ! is_array($xml->xml_array['settingexport']['settinggroup']['setting']) )
	{
		install_error( "Error with reputation_settings.xml - could not process XML properly" );
	}

	$fields = array('conf_title', 'conf_description', 'conf_group', 'conf_type', 'conf_key', 'conf_default','conf_extra', 'conf_evalphp', 'conf_protected', 'conf_position', 'conf_start_group', 'conf_end_group','conf_help_key', 'conf_add_cache', 'conf_title_keyword');
	$setting_fields = array('conf_title_keyword', 'conf_title_title', 'conf_title_desc', 'conf_title_noshow');

	if( ! is_array($xml->xml_array['settingexport']['settinggroup']['setting'][0]) )
	{
		$tmp = $xml->xml_array['settingexport']['settinggroup']['setting'];
		unset($xml->xml_array['settingexport']['settinggroup']['setting']);
		$xml->xml_array['settingexport']['settinggroup']['setting'][0] = $tmp;
	}

	foreach( $xml->xml_array['settingexport']['settinggroup']['setting'] as $id => $entry )
	{
		$newrow = array();

		if( ! $entry['conf_is_title']['VALUE'] )
		{
			foreach( $fields as $f )
			{
				$newrow[$f] = $entry[ $f ]['VALUE'];
			}

			$new_settings[] = $newrow;
		}
		else
		{
			foreach( $setting_fields as $f )
			{
				$newrow[$f] = $entry[ $f ]['VALUE'];
			}

			$new_titles[] = $newrow;
		}
	}

	if( is_array($new_titles) && count($new_titles) )
	{
		foreach( $new_titles as $idx => $data )
		{
			if( $data['conf_title_title'] && $data['conf_title_keyword'] )
			{
				$conf_id = $setting_groups_by_key[ $data['conf_title_keyword'] ]['conf_title_id'];

				$save = array('conf_title_title'   => $data['conf_title_title'],
							  'conf_title_desc'    => $data['conf_title_desc'],
							  'conf_title_keyword' => $data['conf_title_keyword'],
							  'conf_title_noshow'  => $data['conf_title_noshow']  );

				if( ! $conf_id )
				{
					$ipsclass->DB->do_insert( 'conf_settings_titles', $save );
					$conf_id = $ipsclass->DB->get_insert_id();
				}
				else
				{
					$ipsclass->DB->do_update( 'conf_settings_titles', $save, 'conf_title_id='.$conf_id );
				}

				$save['conf_title_id'] = $conf_id;

				$setting_groups_by_key[ $save['conf_title_keyword'] ] = $save;
				$setting_groups[ $save['conf_title_id'] ]             = $save;

				$need_update[] = $conf_id;
			}
		}
	}

	if( is_array($new_settings) && count($new_settings) )
	{
		foreach( $new_settings as $idx => $data )
		{
			//$data['conf_evalphp'] = str_replace( '\\', '\\\\', $data['conf_evalphp'] );
			$data['conf_group'] = $setting_groups_by_key[ $data['conf_title_keyword'] ]['conf_title_id'];
			unset( $data['conf_title_keyword'] );

			if( $cur_settings[ $data['conf_key'] ] )
			{
				$ipsclass->DB->do_update( 'conf_settings', $data, 'conf_id='.$cur_settings[ $data['conf_key'] ] );
				$updated++;
			}
			else
			{
				$ipsclass->DB->do_insert( 'conf_settings', $data );
				$inserted++;
			}
		}
	}

	if( count($need_update) )
	{
		foreach( $need_update as $i => $idx )
		{
			$conf = $ipsclass->DB->simple_exec_query( array( 'select' => 'COUNT(*) as count', 'from' => 'conf_settings', 'where' => 'conf_group='.$idx ) );
			$count = intval($conf['count']);

			$ipsclass->DB->do_update( 'conf_settings_titles', array( 'conf_title_count' => $count ), 'conf_title_id='.$idx );
		}
	}
}

//-----------------------------------------------
// do_drop
//-----------------------------------------------

function do_drop()
{
	global $ipsclass;

	if( $ipsclass->input['act'] == 'doinstall' || $ipsclass->input['act'] == 'doremove' )
	{
		$tbl_drop = array('reputation','reputationlevel');
		for( $i = 0; $i < count($tbl_drop); $i++ )
		{
			$ipsclass->DB->query( "DROP TABLE IF EXISTS `".SQL_PREFIX.$tbl_drop[$i]."`;" );
			$ipsclass->template->contents .= "<font color='red'>DROP TABLE IF EXISTS `".SQL_PREFIX.$tbl_drop[$i]."`;</font><br />";
		}

		$g_drop = array('g_rep_use','g_rep_negative','g_rep_seeown','g_rep_hide');
		for( $i = 0; $i < count($g_drop); $i++ )
		{
			if( $ipsclass->DB->field_exists( $g_drop[$i], "groups" ) )
			{
				$ipsclass->DB->query( "ALTER TABLE `".SQL_PREFIX."groups` DROP `".$g_drop[$i]."`;" );
				$ipsclass->template->contents .= "<font color='red'>ALTER TABLE `".SQL_PREFIX."groups` DROP `".$g_drop[$i]."`;</font><br />";
			}
		}

		if( $ipsclass->DB->field_exists( 'reputation', "members" ) )
		{
			$ipsclass->DB->query( "ALTER TABLE `".SQL_PREFIX."members` DROP `reputation`;" );
			$ipsclass->template->contents .= "<font color='red'>ALTER TABLE `".SQL_PREFIX."members` DROP `reputation`;</font><br />";
		}

		$conf = $ipsclass->DB->simple_exec_query( array ( 'select' => 'conf_title_id', 'from' => 'conf_settings_titles', 'where' => "conf_title_keyword='repoptions'" ) );
		if( $conf['conf_title_id'] )
		{
			$confgroup = $conf['conf_title_id'];

			$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."conf_settings_titles` WHERE conf_title_id='".$confgroup."';" );
			$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."conf_settings` WHERE conf_group='".$confgroup."';" );
			$ipsclass->template->contents .= "<font color='red'>DELETE FROM `".SQL_PREFIX."conf_settings_titles` WHERE conf_title_id='$confgroup';<br />DELETE FROM `".SQL_PREFIX."conf_settings` WHERE conf_group='$confgroup';</font><br />";
		}

		$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."skin_templates` WHERE group_name='skin_reputation';" );
		$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."skin_templates_cache` WHERE template_group_name='skin_reputation';" );
		$ipsclass->DB->query( "DELETE FROM `".SQL_PREFIX."cache_store` WHERE cs_key='replevel';" );
		$ipsclass->template->contents .= "<font color='red'>DELETE FROM `".SQL_PREFIX."skin_templates` WHERE group_name='skin_reputation';<br />DELETE FROM `".SQL_PREFIX."skin_templates_cache` WHERE template_group_name='skin_reputation';<br />DELETE FROM `".SQL_PREFIX."cache_store` WHERE cs_key='replevel';</font><br /><br />";
	}
}

//-----------------------------------------------
// do_lock
//-----------------------------------------------

function do_lock()
{
	global $ipsclass;

	if( $ipsclass->input['act'] != 'doremove' )
	{
		$ipsclass->template->contents .= "<br />
			<div class='borderwrap'>
				<p style='font-size:17pt;'><b>Congratulation!</b></p><br /><br />
				<div class='tablepad'>
					<b>Setup is complete.</b><br /><br />Follow the instruction to modify files needed.<br /><br />";
	}

	if( $fh = @fopen(ROOT_PATH.'reputation_install.lock', 'w') )
	{
		@fwrite($fh, 'blah', 4);
		@fclose($fh);

		if( ! @chmod(ROOT_PATH.'reputation_install.lock', 0666) ) continue;

		if( $ipsclass->input['act'] != 'doremove' )
		{
			$ipsclass->template->contents .= "<font color='red'>Although the installer is now locked (to re-install, remove the file 'reputation_install.lock'),<br />for added security, please remove the reputation_install.php program before continuing.</font><br /><br />";
		}
	}
	else
	{
		if( $ipsclass->input['act'] != 'doremove' )
		{
			$ipsclass->template->contents .= "<font color='red'>PLEASE REMOVE THE INSTALLER ('reputation_install.php') BEFORE CONTINUING ...</font><br /><br />";
		}
	}

	if( $ipsclass->input['act'] != 'doremove' )
	{
		$ipsclass->template->contents .= "
					<center><b><a href=\"{$ipsclass->vars['board_url']}/index.php?act=idx\">&raquo; RETURN BOARD INDEX &laquo;</a></b></center>
				</div><br />
			</div><br />";
	}
}

/*---------------------------------------*/
// TEMPLATE
/*---------------------------------------*/

class template
{
	var $contents = "";

	function print_top()
	{
		return "
		<html>
		<head><title>Module Installer Script</title>
			<style type='text/css'>
				html{ overflow-x:auto; }
				body{ background:#FFF; color:#222; font-family:Arial, Verdana, Tahoma, Times New Roman, Courier; font-size:11px; line-height:135%; margin:0; padding:0; text-align:center; }
				table, tr, td{ background:transparent; color:#222; font-size: 11px; line-height: 135%; }
				a:link, a:visited, a:active{ background:transparent; color:#0066CC; text-decoration:none; }
				a:hover{ background:transparent; color:#000000; text-decoration:underline; }
				#wrapper{ margin:5px auto 20px auto; text-align:left; width:80%; }
				.tablepad{ border:1px solid #345487; background:#ECF5FF; padding:6px; margin-right:1%; margin-left:1%; text-align:left; }
				input{ background:#FFF; border:1px solid #CDCDCD; color:#000; font-family:Arial, Verdana, Helvetica, sans-serif; font-size:11px; margin:5px; padding:2px; vertical-align:middle; }
				input.button{ margin:0; width:auto; }
				.borderwrap{ background:#FFF; border:1px solid #EEE; padding:3px; margin:0; }
				.borderwrap p{ background:#F9F9F9; border:1px solid #CCC; margin:5px; padding:10px; text-align:left; }
				.warnbox{ border:1px solid #F00; background:#FFE0E0; padding:6px; margin-right:1%; margin-left:1%; text-align:left; }
				.maintitle{ border:1px solid #FFF; border-bottom:1px solid #5176B5; color:#FFF; font-size:12px; font-weight:bold; margin:0; padding:8px; background:transparent url(style_images/1/tile_cat.gif); }
			</style>
		</head>
		<body>
		<div id='wrapper'>";
	}

	function output()
	{
		$year = date('Y');

		echo $this->print_top();
		echo $this->contents;
		echo "
			<div align='center' style='border:1px solid #EEE; padding:5px 0px 5px 5px; color:#808080; font-face:Tahoma;'><a href='http://invisionviet.net'>Module Installer v1.0.0</a> &copy; $year <a href='mailto:ntd1712@hotmail.com'>ntd1712</a></div>
		</div>
		</body>
		</html>";
		exit;
	}
}

/*---------------------------------------*/
// CORE FUNCTIONS
/*---------------------------------------*/

class core_functions
{
	function login_screen($msg="")
	{
		global $ipsclass;

		$user = isset($_POST['username']) ? $ipsclass->input['username'] : "";
		$pass = isset($_POST['password']) ? $ipsclass->input['password'] : "";

		$ipsclass->template->contents .= "<br />
			<div class='borderwrap'>
				<p style='font-size:17pt'><b>Welcome!</b></p><br /><br />
				<div class='tablepad'>
					<div class='maintitle'>Verification Required - Please Log in</div>
					<form action='reputation_install.php?act=login' method='post' name='theAdminForm'>
						<div>Your Forums Username: <input name='username' type='text' size='50' value='$user' /></div>
						<div>Your Forums Password: <input name='password' type='password' size='50' value='$pass' /></div>
						<input name='submit' type='submit' value='Log in' id='button' accesskey='s' style='margin:0px;'>
					</form>
				</div><br />
			</div><br />";

		if( $msg != "" )
		{
			$ipsclass->template->contents .= "<div class='warnbox'>
				<strong>Warning!</strong>
				<b>The following errors must be rectified before continuing!</b><br /><br />
				$msg
			</div><br />";
		}

		$ipsclass->template->output();
	}

	function redirect($url, $text, $time=2)
	{
		global $ipsclass;

		$url = str_replace("&amp;", "&", $url);
		$ipsclass->template->contents .= "<meta http-equiv='refresh' content=\"{$time}; url={$url}\"><br />
			<div class='borderwrap'>
				<p style='font-size:17pt;'><b>Welcome!</b></p><br /><br />
				<div class='tablepad'>
					<div class='maintitle'>Redirecting</div><br />
					<div>$text<br /><br /><center><a href='{$url}'>Click here if not redirected...</a></center></div>
				</div><br />
			</div><br />";

		$ipsclass->template->output();
	}

	function get_member()
	{
		global $ipsclass;

		$member = array('id' => 0);

		$ipsclass->member_id = intval($ipsclass->input['mid']);
		$ipsclass->loginkey  = $ipsclass->txt_alphanumerical_clean( $ipsclass->input['loginkey']  );
		$ipsclass->securekey = $ipsclass->txt_alphanumerical_clean( $ipsclass->input['securekey'] );

		if( (strlen($ipsclass->loginkey) != 32) || (strlen($ipsclass->securekey) != 32) )
		{
			return $member;
		}

		$ipsclass->DB->query( "SELECT m.*, g.* FROM `".SQL_PREFIX."members` m LEFT JOIN `".SQL_PREFIX."groups` g ON (m.mgroup=g.g_id) WHERE member_login_key='{$ipsclass->loginkey}' and id='{$ipsclass->member_id}'" );
		$member = $ipsclass->DB->fetch_row();

		return $member;
	}
}

?>