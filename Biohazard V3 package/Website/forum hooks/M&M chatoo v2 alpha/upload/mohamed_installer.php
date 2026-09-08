<?php


error_reporting  (E_ERROR | E_WARNING | E_PARSE);
set_magic_quotes_runtime(0);

$INFO = array();
define('ROOT_PATH', dirname(__FILE__).'/');
define('KERNEL_PATH', ROOT_PATH.'ips_kernel/');
define('IPB_THIS_SCRIPT', (($_GET['d'] == 'rebuild' || $_POST['d'] == 'rebuild') ? 'admin'  : ''));
require_once ROOT_PATH.'sources/ipsclass.php';
require_once ROOT_PATH.'sources/classes/class_display.php';
require_once ROOT_PATH.'sources/classes/class_session.php';
require_once ROOT_PATH.'conf_global.php';

define('USE_SHUTDOWN', 0);
define('SAFE_MODE_ON', 0);
define('IN_DEV', 0);
$ipsclass       = new ipsclass();
$ipsclass->vars = $INFO;
$ipsclass->init_db_connection();
$ipsclass->init_load_cache();
$ipsclass->print            =  new display();
$ipsclass->print->ipsclass  =& $ipsclass;
$ipsclass->sess             =  new session();
$ipsclass->sess->ipsclass   =& $ipsclass;
$ipsclass->parse_incoming();
$ipsclass->initiate_ipsclass();
$ipsclass->base_url = $ipsclass->vars['board_url'].'/index.'.$ipsclass->vars['php_ext'].'?';

new Install;
class Install
{
	var $output        = "";
	var $xml_file      = "mohamed_data.xml";
	var $mysql_version = "";
	var $sql_data      = array();
	var $uninstalling  = false;

	function Install()
	{
		global $ipsclass;

		$this->php_version();
		$this->mysql_version();
		$this->checksystem();
		$this->load_xml();

$this->output .= base64_decode("PCFET0NUWVBFIGh0bWwgUFVCTElDICItLy9XM0MvL0RURCBYSFRNTCAxLjAgVHJhbnNpdGlvbmFsLy9FTiIgImh0dHA6Ly93d3cudzMub3JnL1RSL3hodG1sMS9EVEQveGh0bWwxLXRyYW5zaXRpb25hbC5kdGQiPg0KPGh0bWwgeG1sOmxhbmc9ImVuIiBsYW5nPSJlbiIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGh0bWwiPg0KCTxoZWFkPg0KCQk8dGl0bGU+TSZNIENoYXRvbyBBbHBoYSAyPC90aXRsZT4NCgkJPG1ldGEgaHR0cC1lcXVpdj0iUGFnZS1FbnRlciIgY29udGVudD0iYmxlbmRUcmFucyhEdXJhdGlvbj0wLjMpIiAvPg0KCQk8bWV0YSBodHRwLWVxdWl2PSJjb250ZW50LXR5cGUiIGNvbnRlbnQ9InRleHQvaHRtbDsgY2hhcnNldD1pc28tODg1OS0xIiAvPg0KCQk8c3R5bGUgdHlwZT0idGV4dC9jc3MiPg0KCQkJPCEtLQ0KCQkJYm9keQ0KCQkJew0KCQkJCWJhY2tncm91bmQtY29sb3I6ICNEMURDRUI7DQoJCQkJY29sb3I6ICMwMDA7DQoJCQkJZm9udC1mYW1pbHk6IFZlcmRhbmEsIEFyaWFsLCBIZWx2ZXRpY2EsIHNhbnMtc2VyaWY7DQoJCQkJZm9udC1zaXplOiAxMnB4Ow0KCQkJfQ0KCQkJYQ0KCQkJew0KCQkJCWNvbG9yOiAjMDAwMDU1Ow0KCQkJfQ0KCQkJYTpob3Zlcg0KCQkJew0KCQkJCWNvbG9yOiMzMzMzNzc7DQogICAgICAgICAgICAgICAgICAgICAgICAgdGV4dC1kZWNvcmF0aW9uOnVuZGVybGluZTsNCgkJCX0NCgkJCS50YWJsZQ0KCQkJew0KCQkJCWJvcmRlcjogMXB4IHNvbGlkICMzNDU0ODc7DQoJCQkJbWFyZ2luOiAycHg7DQoJCQkJcGFkZGluZzogMXB4Ow0KCQkJCWJhY2tncm91bmQtY29sb3I6ICNEMURDRUI7DQoJCQkJdGV4dC1hbGlnbjogY2VudGVyOw0KCQkJfQ0KCQkJLnRyb3dzDQoJCQl7DQoJCQkJYm9yZGVyOiAxcHggc29saWQgI0ZGRkZGRjsNCgkJCX0NCg0KCQkJLnRleHRhcmVhDQoJCQl7DQoJCQkJZm9udC1zaXplOiAxMXB4Ow0KCQkJCWNvbG9yOiAjRkZGRkZGOw0KCQkJCWJhY2tncm91bmQtY29sb3I6ICMwNTUzNkQ7DQoJCQkJYm9yZGVyOiAxcHggc29saWQgI0ZGRkZGRjsNCgkJCQlvdmVyZmxvdzogaGlkZGVuOw0KCQkJfQ0KCQkJLS0+DQoJCTwvc3R5bGU+DQoJPC9oZWFkPg0KCSA8Ym9keT4NCjxjZW50ZXI+PGRpdj48aW1nIHNyYz0nbSZtL2ltZy9zZXJ2aWNlcy5naWYnLz48aW1nIHNyYz0nbSZtL2ltZy9sb2dvNC5qcGcnLz48aW1nIHNyYz0nbSZtL2ltZy90b29sLmdpZicvPjwvZGl2PjwvY2VudGVyPjxicj4NCg0KCQk8ZGl2IGFsaWduPSJjZW50ZXIiPg0KCQk8aDM+TSZNIENoYXRvbyBBbHBoYSAyPC9oMz4NCgkJPHRhYmxlIHdpZHRoPSI4MCUiIGNlbGxwYWRkaW5nPSIzIiBjZWxsc3BhY2luZz0iMyIgY2xhc3M9InRhYmxlIj4NCgkJCTx0cj4NCgkJCQk8dGQ+");
		switch ($ipsclass->input['d'])
		{
			case 'start':
				$this->start_install();
				break;
			case 'templates':
				$this->run_templates();
				break;
			case 'rebuild':
				if ($ipsclass->input['-uninstall-'])
				{
					$this->uninstalling = true;
				}

				$this->rebuild_templates();
				break;
			case 'finish':
				$this->finish_install();
				break;
			case 'uninstall':
				$this->uninstalling = true;
				$this->uninstall();
				break;
			case 'datauninstall':
				$this->datauninstall();
				break;
			case 'uninstall-temps':
				$this->uninstalling = true;
				$this->uninstall_temps();
				break;
			case 'uninstall-finish':
				$this->uninstalling = true;
				$this->uninstall_finish();
				break;
			default:
				$this->intro();
				break;
		}
       $this->output .= base64_decode("CTwvdGQ+PC90cj48L3RhYmxlPg0KPHRhYmxlIHdpZHRoPSI4MCUiIGNlbGxwYWRkaW5nPSIzIiBjZWxsc3BhY2luZz0iMyIgY2xhc3M9InRhYmxlIj4NCgkJPHRyPg0KCQkJPHRkIHZhbGlnbj0ibWlkZGxlIj5NJk0gQ2hhdG9vIKkgMjAwNiA8YSBocmVmPSJodHRwOi8vd3d3LmludmlzaW9uZXllcy5jb20iIHRhcmdldD0iX2JsYW5rIj5tb2hhbWVkPC9hPjwvdGQ+DQoJCTwvdHI+DQoJCTwvdGFibGU+DQoJCTwvZGl2Pg0KCTwvYm9keT4NCjwvaHRtbD4=");
		$this->print_it($this->output);
	}

	function intro()
	{
		$this->output .= "To begin, please click the button below to start the installation.<p><form method='post' action='?d=start'><input type='submit' name='startinstall' value='Start Installation' class='hand' /></form><p>Or<p>Click The Button Below To Uninstall This Mod.<p><form method='post' action='?d=uninstall'><input type='submit' value='Uninstall' class='hand' /></form>Or<p>Click The Button Below To Upgrade from v1 to v2.<p><form method='post' action='?d=datauninstall'><input type='submit' value='Upgrade' class='hand' /></form>";
	}

	function start_install()
	{
		$this->run_mysql();
		$this->output .= "<meta http-equiv='refresh' content='0;url=?d=templates'>Installation of mysql data is complete.<p><form method='post' action='?d=templates'><input type='submit' value='Click Here If You Are Not Forwarded...' class='hand' /></form>";
	}

	function run_templates()
	{
		$this->skins();
		$this->output .= "<meta http-equiv='refresh' content='0;url=?d=rebuild'>Installation of templates is complete.<p><form method='post' action='?d=rebuild'><input type='submit' value='Click Here If You Are Not Forwarded...' class='hand' /></form>";
	}

	function uninstall()
	{
		$this->uninstall_mysql();
		$this->output .= "<meta http-equiv='refresh' content='0;url=?d=uninstall-temps'>Uninstallation of mysql data is complete.<p><form method='post' action='?d=uninstall-temps'><input type='submit' value='Click Here If You Are Not Forwarded...' class='hand' /></form>";
	}

	function datauninstall()
	{
		$this->uninstall_data();
		$this->output .= "<center>Done...</center>";
	}
	function uninstall_temps()
	{
		$this->remove_templates();
		$this->output .= "<meta http-equiv='refresh' content='0;url=?d=rebuild&-uninstall-=true'>Uninstallation of templates is complete.<p><form method='post' action='?d=rebuild&-uninstall-=true'><input type='submit' value='Click Here If You Are Not Forwarded...' class='hand' /></form>";
	}

	function uninstall_finish()
	{
		

		$this->output .= "The uninstallation is complete!<p>";
	}

	function finish_install()
	{
		

		$this->output .= "<p><script language='Javascript' type='text/javascript' src='m&m/m&m.js'></script>The installation is complete!<p><br><input type='button' value='Continue to edits ' onclick=\"moh.go_to('m&m/index_php.html')\" class='hand' />";
	}
	function uninstall_data()
	{
		global $ipsclass;

		$this->_delete_tables();
	}
	function uninstall_mysql()
	{
		global $ipsclass;

		$skeys = $this->_get_setting_keys();

		$this->_delete_settings($skeys);
		$this->_delete_setting_group();
		$this->_delete_tables();
		$this->_delete_alters();
		$this->_delete_components();
		$this->_rebuild_caches();
	}

	function run_mysql()
	{
		global $ipsclass;

		set_time_limit(900);
		$sets     = $this->_get_settings_array();
		$skeys    = $this->_get_setting_keys();
		$tables   = $this->_get_tables();
		$old_sets = $this->_get_setting_data($skeys);
		$set_grp  = $this->_run_setting_group();
		$this->_run_settings($set_grp);
		$this->_restore_settings($sets, $old_sets);
		$this->_recount_set_group($set_grp);
		$this->_run_tables();
		$this->_run_alters();
		$this->_run_inserts();
		$this->_add_components();
		$this->_rebuild_caches();
	}

	function rebuild_templates()
	{
		global $ipsclass;

		require_once(ROOT_PATH.'sources/lib/admin_cache_functions.php');
		$cache = new admin_cache_functions();
		$cache->ipsclass =& $ipsclass;

		$c = ($ipsclass->input['completed']) ? $ipsclass->input['completed'] : 1;
		$s = $ipsclass->DB->simple_exec_query(array('select' => '*', 'from' => 'skin_sets', 'where' => 'set_skin_set_id>'.$c, 'order' => 'set_skin_set_id', 'limit' => array(0, 1)));

		if ($s['set_skin_set_id'])
		{
			$cache->_rebuild_all_caches(array($s['set_skin_set_id']));
			$this->output .= "<meta http-equiv='refresh' content='0;url=?d=rebuild&".(($this->uninstalling) ? '-uninstall-=true&' : '')."completed={$s['set_skin_set_id']}'>Rebuilding skin: {$s['set_name']} is complete.<br>Proceed to next skin.<p><form method='post' action='?d=rebuild&".(($this->uninstalling) ? '-uninstall-=true&' : '')."completed={$s['set_skin_set_id']}'><input type='submit' value='Click Here If You Are Not Forwarded...' class='hand'></form>";
		}
		else
		{
			$this->output .= "<meta http-equiv='refresh' content='0;url=?d=".($this->uninstalling ? 'uninstall-finish' : 'finish')."'>Rebuilding of skins and templates is complete.<p><form method='post' action='?d=".($this->uninstalling ? 'uninstall-finish' : 'finish')."'><input type='submit' value='Click Here If You Are Not Forwarded...' class='hand'></form>";
		}
	}

	function skins()
	{
		global $ipsclass;

		$cache = 0;
		foreach ($this->xml_array['templates']['template'] as $k => $t)
		{
			if ($cache == 0)
			{
				$ipsclass->DB->simple_exec_query(array('select' => '*', 'from' => 'skin_templates_cache', 'where' => "template_group_name='".$t['group_name']['VALUE']."'"));
				if ($ipsclass->DB->get_num_rows())
				{
					$ipsclass->DB->simple_exec_query(array('delete' => 'skin_templates_cache', 'where' => "template_group_name='".$t['group_name']['VALUE']."'"));
				}
				$cache = 1;
			}

			$ipsclass->DB->simple_exec_query(array('select' => '*', 'from' => 'skin_templates', 'where' => "group_name='".$t['group_name']['VALUE']."' AND func_name='".$t['func_name']['VALUE']."'"));
			if ($ipsclass->DB->get_num_rows())
			{
				$ipsclass->DB->simple_exec_query(array('delete' => 'skin_templates', 'where' => "group_name='".$t['group_name']['VALUE']."' AND func_name='".$t['func_name']['VALUE']."'"));
			}

			$temp = array();
			$temp['set_id']          = 1;
			$temp['group_name']      = $t['group_name']['VALUE'];
			$temp['section_content'] = $t['section_content']['VALUE'];
			$temp['func_name']       = $t['func_name']['VALUE'];
			$temp['func_data']       = $t['func_data']['VALUE'];
			$temp['updated']         = time();

			$ipsclass->DB->do_insert('skin_templates', $temp);
		}
	}

	function remove_templates()
	{
		global $ipsclass;

		$temps = $this->xml_array['templates']['template'];
		if (count($temps) <= 0)
		{
			return;
		}

		if (count($temps) > 1)
		{
			foreach ($temps as $t)
			{
				$this->_delete_old_template($t);
			}
		}
		else if (count($temps) == 1)
		{
			$t = $d;
			$this->_delete_old_template($t);
		}
	}

	function _delete_old_template($t=array())
	{
		global $ipsclass;

		$ipsclass->DB->simple_exec_query(array('select' => '*', 'from' => 'skin_templates', 'where' => "group_name='".$t['group_name']['VALUE']."' AND func_name='".$t['func_name']['VALUE']."'"));
		if ($ipsclass->DB->get_num_rows())
		{
			$ipsclass->DB->simple_exec_query(array('delete' => 'skin_templates', 'where' => "group_name='".$t['group_name']['VALUE']."' AND func_name='".$t['func_name']['VALUE']."'"));
		}
	}

	function _get_tables()
	{
		global $ipsclass;

		$t = array(0 => null);
		$d = $this->sql_data['table'][0];

		if (count($d) <= 0)
		{
			return $t;
		}
		$t[] = $d['name']['VALUE'];
		
		return $t;
	}

	function _get_setting_keys()
	{
		global $ipsclass;

		$s = array();
		$d = $this->xml_array['settings']['setting'];

		if (!count($d))
		{
			return $s;
		}

		if (count($d) > 1)
		{
			foreach ($d as $k => $v)
			{
				$s[] = $v['conf_key']['VALUE'];
			}
		}
		else if (count($d) == 1)
		{
			$s[] = $d['conf_key']['VALUE'];
		}

		return $s;
	}

	function _get_settings_array()
	{
		global $ipsclass;

		$s = array();
		$d = $this->xml_array['settings']['setting'];

		if (!count($d))
		{
			return $s;
		}

		if (count($d) > 1)
		{
			foreach ($d as $k => $v)
			{
				$s[] = $v['conf_key']['VALUE'];
			}
		}

		return $s;
	}

	function _get_setting_data($s=array())
	{
		global $ipsclass;

		$d = array();
		if (!count($s))
		{
			return;
		}

		$ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'conf_settings', 'where' => "conf_key IN ('".implode("','", $s)."')"));
		$ipsclass->DB->simple_exec();
		if ($ipsclass->DB->get_num_rows())
		{
			while ($r = $ipsclass->DB->fetch_row())
			{
				$val = ($r['conf_value'] != '' && $r['conf_value'] != $r['conf_default']) ? $r['conf_value'] : '';
				$d[$r['conf_key']] = $val;
			}
		}

		return $d;
	}

	function _delete_settings($s=array())
	{
		global $ipsclass;

		if (count($s) <= 0)
		{
			return;
		}

		if (count($s))
		{
			$ipsclass->DB->simple_exec_query(array('delete' => 'conf_settings', 'where' => "conf_key IN ('".implode("','", $s)."')"));
		}
	}

	function _delete_setting_group()
	{
		global $ipsclass;

		$d = $this->xml_array['settings']['group'];
		if (!count($d))
		{
			return 0;
		}

		if ($d['keyword']['VALUE'] != '')
		{
			$ipsclass->DB->simple_exec_query(array('delete' => 'conf_settings_titles', 'where' => "conf_title_keyword='".$d['keyword']['VALUE']."'"));
		}
		else if ($d['title']['VALUE'] != '')
		{
			$ipsclass->DB->simple_exec_query(array('delete' => 'conf_settings_titles', 'where' => "conf_title_title='".$d['title']['VALUE']."'"));
		}
	}

	function _restore_settings($d=array())
	{
		global $ipsclass;

		if (count($d))
		{
			foreach ($d as $k => $v)
			{
				$ipsclass->DB->do_update('conf_settings', array('conf_value' => $v), "conf_key='".$k."'");
			}
		}
	}

	function _recount_set_group($g=0)
	{
		global $ipsclass;

		$c = $ipsclass->DB->simple_exec_query(array('select' => 'count(*) as count', 'from' => 'conf_settings', 'where' => 'conf_group='.$g));
		$ct = intval($c['count']);
		$ipsclass->DB->do_update('conf_settings_titles', array('conf_title_count' => $ct), 'conf_title_id='.$g);
	}

	function _run_settings($sg=0)
	{
		global $ipsclass;

		$d = $this->xml_array['settings']['setting'];
		if (!count($d))
		{
			return;
		}

		if (count($d) > 1)
		{
			foreach ($d as $k => $s)
			{
				$set = array();
				$set['conf_title']       = $s['conf_title']['VALUE'];
				$set['conf_description'] = $s['conf_description']['VALUE'];
				$set['conf_group']       = $sg;
				$set['conf_type']        = $s['conf_type']['VALUE'];
				$set['conf_key']         = $s['conf_key']['VALUE'];
				$set['conf_value']       = '';
				$set['conf_default']     = $s['conf_default']['VALUE'];
				$set['conf_extra']       = $s['conf_extra']['VALUE'];
				$set['conf_evalphp']     = $s['conf_evalphp']['VALUE'];
				$set['conf_protected']   = 1;
				$set['conf_position']    = $s['conf_position']['VALUE'];
				$set['conf_start_group'] = $s['conf_start_group']['VALUE'];
				$set['conf_end_group']   = $s['conf_end_group']['VALUE'];
				$set['conf_help_key']    = '';
				$set['conf_add_cache']   = 1;

				$ipsclass->DB->do_insert('conf_settings', $set);
			}
		}
		else if (count($d) == 1)
		{
			$set = array();
			$set['conf_title']       = $d['conf_title']['VALUE'];
			$set['conf_description'] = $d['conf_description']['VALUE'];
			$set['conf_group']       = $sg;
			$set['conf_type']        = $d['conf_type']['VALUE'];
			$set['conf_key']         = $d['conf_key']['VALUE'];
			$set['conf_value']       = '';
			$set['conf_default']     = $d['conf_default']['VALUE'];
			$set['conf_extra']       = $d['conf_extra']['VALUE'];
			$set['conf_evalphp']     = $d['conf_evalphp']['VALUE'];
			$set['conf_protected']   = 1;
			$set['conf_position']    = $d['conf_position']['VALUE'];
			$set['conf_start_group'] = $d['conf_start_group']['VALUE'];
			$set['conf_end_group']   = $d['conf_end_group']['VALUE'];
			$set['conf_help_key']    = '';
			$set['conf_add_cache']   = 1;

			$ipsclass->DB->do_insert('conf_settings', $set);
		}
	}

	function _run_setting_group()
	{
		global $ipsclass;

		$d = $this->xml_array['settings']['group'];
		if (!count($d))
		{
			return 0;
		}

		$ipsclass->DB->simple_exec_query(array('delete' => 'conf_settings_titles', 'where' => "conf_title_title='".$d['title']['VALUE']."'"));
		$ipsclass->DB->do_insert('conf_settings_titles', array('conf_title_title' => $d['title']['VALUE'], 'conf_title_desc' => $d['desc']['VALUE'], 'conf_title_count' => 0, 'conf_title_noshow' => 0, 'conf_title_keyword' => $d['keyword']['VALUE']));
		return $ipsclass->DB->get_insert_id();
	}


	function _run_tables()
	{
		global $ipsclass;

		$d = $this->sql_data['table'][0];
		if (count($d) <= 0)
		{
			return;
		}


			$ipsclass->DB->query("DROP TABLE IF EXISTS ".SQL_PREFIX.$d['name']['VALUE']);
			$ipsclass->DB->query("CREATE TABLE IF NOT EXISTS ".SQL_PREFIX.$d['name']['VALUE']." (".$d['data']['VALUE'].") TYPE=".$d['type']['VALUE']);
		
	}

	function _delete_tables()
	{
		global $ipsclass;

		$d = $this->sql_data['table'];
		if (count($d) <= 0)
		{
			return;
		}


			$ipsclass->DB->query("DROP TABLE IF EXISTS ".SQL_PREFIX.$d['name']['VALUE']);
		
	}

	function _run_alters()
	{
		global $ipsclass;

		$d = $this->sql_data['alter'][0];
		if (count($d) <= 0)
		{
			return;
		}

		if (count($d) > 1)
		{
			foreach ($d as $k => $v)
			{
				if (!$ipsclass->DB->field_exists($v['field']['VALUE'], $v['table']['VALUE']))
				{
					$ipsclass->DB->query('ALTER TABLE '.SQL_PREFIX.$v['table']['VALUE'].' '.strtoupper($v['do']['VALUE']).' '.$v['field']['VALUE'].' '.$v['type']['VALUE'].' '.((strtolower($v['default']['VALUE']) != 'null' || $v['default']['VALUE'] != '') ? "DEFAULT '".$v['default']['VALUE']."'" : 'DEFAULT NULL'));
				}
			}
		}
		else if (count($d) == 1)
		{
			$d = $d[0];
			if (!$ipsclass->DB->field_exists($d['field']['VALUE'], $d['table']['VALUE']))
			{
				$ipsclass->DB->query('ALTER TABLE '.SQL_PREFIX.$d['table']['VALUE'].' '.strtoupper($d['do']['VALUE']).' '.$d['field']['VALUE'].' '.$d['type']['VALUE'].' '.((strtolower($d['default']['VALUE']) != 'null' || $d['default']['VALUE'] != '') ? "DEFAULT '".$d['default']['VALUE']."'" : 'DEFAULT NULL'));
			}
		}
	}

	function _delete_alters()
	{
		global $ipsclass;

		$d = $this->sql_data['alter'][0];
		if (count($d) <= 0)
		{
			return;
		}

		if (count($d) > 1)
		{
			foreach ($d as $k => $v)
			{
				if ($ipsclass->DB->field_exists($v['field']['VALUE'], $v['table']['VALUE']))
				{
					$ipsclass->DB->query('ALTER TABLE '.SQL_PREFIX.$v['table']['VALUE'].' DROP '.$v['field']['VALUE']);
				}
			}
		}
		else if (count($d) == 1)
		{
			$d = $d[0];
			if (!$ipsclass->DB->field_exists($d['field']['VALUE'], $d['table']['VALUE']))
			{
				$ipsclass->DB->query('ALTER TABLE '.SQL_PREFIX.$d['table']['VALUE'].' DROP '.$d['field']['VALUE']);
			}
		}
	}

	function _run_inserts()
	{
		global $ipsclass;

		$d = $this->sql_data['insert'];
		if (count($d) <= 0)
		{
			return;
		}

		if (count($d) > 1)
		{
			foreach ($d as $k => $v)
			{
				if (!count($v['fields']))
				{
					continue;
				}

				$f = $q = array();
				foreach ($v['fields'] as $s => $fd)
				{
					if (strtolower($s) == 'value')
					{
						continue;
					}

					$f[$s] = $fd['VALUE'];
					$q[] = $s."='".$fd['VALUE']."'";
				}

				$ipsclass->DB->simple_exec_query(array('select' => '*', 'from' => $v['table']['VALUE'], 'where' => implode(' AND ', $q)));
				if (!$ipsclass->DB->get_num_rows())
				{
					$ipsclass->DB->do_insert($v['table']['VALUE'], $f);
				}
			}
		}
		else if (count($d) == 1)
		{
			$d = $d[0];
			if (!count($d['fields']))
			{
				return;
			}

			$f = $q = array();
			foreach ($d['fields'] as $s => $fd)
			{
				if (strtolower($s) == 'value')
				{
					continue;
				}

				$f[$s] = $fd['VALUE'];
				$q[] = $s."='".$fd['VALUE']."'";
			}

			$ipsclass->DB->simple_exec_query(array('select' => '*', 'from' => $d['table']['VALUE'], 'where' => implode(' AND ', $q)));
			if (!$ipsclass->DB->get_num_rows())
			{
				$ipsclass->DB->do_insert($d['table']['VALUE'], $f);
			}
		}
	}

	function _add_components()
	{
		global $ipsclass;

		if (!is_array($this->xml_array['components']))
		{
			return;
		}

		$fields = array('com_title', 'com_description', 'com_author', 'com_url', 'com_version', 'com_menu_data', 'com_enabled', 'com_safemode', 'com_section', 'com_filename', 'com_url_title', 'com_url_uri');
		if (!is_array($this->xml_array['components']['component'][0]))
		{
			$tmp = $this->xml_array['components']['component'];
			$this->xml_array['components'] = array();
			$this->xml_array['components']['component'] = array();
			$this->xml_array['components']['component'][0] = $tmp;
		}

		foreach ($this->xml_array['components']['component'] as $id => $e)
		{
			$nr = array();
			foreach ($fields as $f)
			{

				$nr[$f] = $e[$f]['VALUE'];
			}

			if ($nr['com_section'] != '')
			{
				$ipsclass->DB->build_and_exec_query(array('delete' => 'components', 'where' => "com_section='{$nr['com_section']}'"));
			}

			$ipsclass->DB->force_data_type = array('com_version' => 'string');
			$nr['com_date_added'] = time();
			$ipsclass->DB->do_insert('components', $nr);
		}
	}

	function _delete_components()
	{
		global $ipsclass;

		if (!is_array($this->xml_array['components']['component'][0]))
		{
			$tmp = $this->xml_array['components']['component'];
			unset($xml->xml_array['components']['component']);
			$this->xml_array['components']['component'][0] = $tmp;
		}

		foreach ($this->xml_array['components']['component'] as $id => $e)
		{
			if ($e['com_section']['VALUE'] != '')
			{
				$ipsclass->DB->build_and_exec_query(array('delete' => 'components', 'where' => "com_section='{$e['com_section']['VALUE']}'"));
			}
		}
	}

	function _rebuild_caches()
	{
		global $ipsclass;

		$c = array();
		$d = $this->xml_array['caches']['cache'];
		if (!count($d))
		{
			return;
		}

		if (count($d) > 1)
		{
			foreach ($d as $k => $v)
			{
				if (strtolower($k) == 'value')
				{
					continue;
				}

				$c[] = $v['VALUE'];
			}
		}

		if (!is_array($c) || !count($c))
		{
			return;
		}

		foreach ($c as $cc)
		{
			switch ($cc)
			{
				case 'groups':
					$grps = array();
					$ipsclass->DB->simple_construct(array('select' => '*', 'from' => 'groups'));
					$ipsclass->DB->simple_exec();

					while ($i = $ipsclass->DB->fetch_row())
					{
						$grps[$i['g_id']] = $i;
					}

					$gv = $ipsclass->DB->add_slashes(serialize($grps));
					$ipsclass->DB->manual_addslashes = 1;
					$ipsclass->DB->simple_construct(array('delete' => 'cache_store', 'where' => "cs_key='group_cache'"));
					$ipsclass->DB->simple_shutdown_exec();
					$ipsclass->DB->do_shutdown_insert('cache_store', array('cs_array' => 1, 'cs_key' => 'group_cache', 'cs_value' => $gv));
					break;
				case 'components':
					$components = array();
					$ipsclass->DB->simple_construct(array('select' => 'com_id,com_enabled,com_section,com_filename,com_url_uri,com_url_title,com_position', 'from' => 'components', 'where'  => 'com_enabled=1', 'order' => 'com_position ASC'));
					$ipsclass->DB->simple_exec();

					while ($r = $ipsclass->DB->fetch_row())
					{
						$components[] = $r;
					}

					$cvalue = $ipsclass->DB->add_slashes(serialize($components));
					$ipsclass->DB->manual_addslashes = 1;
					$ipsclass->DB->simple_construct(array('delete' => 'cache_store', 'where' => "cs_key='components'"));
					$ipsclass->DB->simple_shutdown_exec();
					$ipsclass->DB->do_shutdown_insert('cache_store', array('cs_array' => 1, 'cs_key' => 'components', 'cs_value' => $cvalue));
					break;
				default:
					break;
			}
		}
	}

	function load_xml()
	{
		require_once(KERNEL_PATH.'class_xml.php');
		$xml = new class_xml();
		$xmlfile = ROOT_PATH.$this->xml_file;
		$xmldata = implode('', file($xmlfile));
		$xml->xml_parse_document($xmldata);

		if (!is_array($xml->xml_array['mohamedmod_data']))
		{
			$this->error("Install Error:<p>Unknown Error In ".$this->xml_file.". &nbsp; Installer Couldn't Process The XML Properly.");
		}

		$this->xml_array = $xml->xml_array['mohamedmod_data'];
		$this->_parse_sql_xml();
	}

	function _parse_sql_xml()
	{
		if (is_array($this->xml_array['sql_data']) && count($this->xml_array['sql_data']))
		{
			foreach ($this->xml_array['sql_data'] as $k => $v)
			{
				if (strtolower($k) == 'value')
				{
					continue;
				}

				$this->sql_data[$k][] = $v;
			}
		}
	}

	function table_exists($table)
	{
		global $ipsclass;

		if ($this->mysql_version >= 32303)
		{
			$hastable = 0;
			$ipsclass->DB->query("SHOW TABLE STATUS FROM `{$ipsclass->DB->obj['sql_database']}`");
			while ($r = $ipsclass->DB->fetch_row())
			{
				if ($r['Name'] == SQL_PREFIX.$table)
				{
					$hastable = 1;
				}
			}

			return $hastable;
		}
		else
		{
			$hastable = 0;
			$result = mysql_list_tables($ipsclass->DB->obj['sql_database']);
			$num_tables = @mysql_numrows($result);
			$tables = array();

			for ($i=0; $i<$num_tables; $i++)
			{
				$tables[] = mysql_tablename($result, $i);
			}

			mysql_free_result($result);
			foreach($tables as $tbl)
			{
				if ($tbl == SQL_PREFIX.$table)
				{
					$hastable = 1;
				}
			}

			return $hastable;
		}
	}

	function mysql_version()
	{
		global $ipsclass;

		$ipsclass->DB->query("SELECT VERSION() AS version");
		if (!$row = $ipsclass->DB->fetch_row())
		{
			$ipsclass->DB->query("SHOW VARIABLES LIKE 'version'");
			$row = $ipsclass->DB->fetch_row();
		}

		$a = explode('.', preg_replace("/^(.+?)[-_]?/", "\\1", $row['version']));
		$b = (!isset($a) || !isset($a[0])) ? 3 : $a[0];
		$c = (!isset($a[1])) ? 21 : $a[1];
		$d = (!isset($a[2])) ? 0 : $a[2];
   		$this->mysql_version = (int)sprintf('%d%02d%02d', $b, $c, intval($d));
	}

	function php_version()
	{
		$this->phpv    = phpversion();
		$this->phpva   = split('[/.-]', $this->phpv);
		$this->is_php5 = false;

		if ($this->phpva[0] >= 5)
		{
			$this->is_php5 = true;
		}
	}

	function checksystem()
	{
		if (!file_exists(ROOT_PATH.$this->xml_file))
		{
			$this->error("Install Error:<p><b>".$this->xml_file."</b> does not exist! &nbsp; Please upload it to your forums root directory.");
		}
	}

	function error($msg)
	{
		global $dheader, $dfooter;

		$this->headers();
		print $dheader.$msg.$dfooter;
		exit;
	}

	function headers()
	{
		@header("HTTP/1.0 200 OK");
		@header("HTTP/1.1 200 OK");
		@header("Content-type: text/html");
		@header("Cache-Control: no-cache, must-revalidate, max-age=0");
		@header("Expires: Mon, 26 Jul 1997 05:00:00 GMT");
		@header("Pragma: no-cache");
	}

	function print_it($output)
	{
		global $dheader, $dfooter;

		$this->headers();
		print $dheader.$output.$dfooter;
	}
}
?>