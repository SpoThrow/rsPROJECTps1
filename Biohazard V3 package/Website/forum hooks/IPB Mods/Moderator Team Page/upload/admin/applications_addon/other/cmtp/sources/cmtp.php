<?php
/**
 * @ Application : 		Custom Moderator Team Page v2.0.0
 * @ Last Updated : 	June 13th, 2012 
 * @ Author :			Michael S. Edwards
 * @ Copyright :		(c) 2011 Coding Jungle
 * @ Link	 :			http://www.codingjungle.com/
 */
if (!defined('IN_ACP')) {
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

class cmtp {
		
	public $registry;
	
	public $DB;
	
	public $settings;
	
	public $request;
	
	public $lang;

	function __construct() {
		
		$acp = CP_DIRECTORY;
		
		$this -> registry = ipsRegistry::instance();
		
		$this -> DB = $this -> registry -> DB();
		
		$this -> settings = &$this -> registry -> fetchSettings();
		
		$this -> request = &$this -> registry -> fetchRequest();
		
		$this -> cache = $this -> registry -> cache();
		
		$this -> caches = &$this -> registry -> cache() -> fetchCaches();
		
		$this -> lang = $this -> registry -> getClass('class_localization');
		
		$this -> member = $this -> registry -> member();
		
		$this -> memberData = &$this -> registry -> member() -> fetchMemberData();
		
		$this -> lang -> loadLanguageFile(array('admin_lang'), 'cmtp');
		
	}

	public function MemberCount() {

		$cache = $this -> cache -> getCache('group_cache');

		$kudos = array();

		foreach ($cache as $i) {
				
			if($this->settings['cmtp_secondary_group'] == 1){
				$where = "member_group_id=".$i['g_id']." OR FIND_IN_SET( ".$i['g_id'].", mgroup_others)";
			}
			else{
				$where = "member_group_id=".$i['g_id'];
			}
			
			$count = $this -> DB -> buildAndFetch(array('select' => 'COUNT(*) as count',
			 											'from' => 'members', 
			 											'where' => $where
														));
			$count2 = $this -> DB -> buildAndFetch(array('select' => 'COUNT(*) as count',
			 											'from' => 'cmtp_members_added', 
			 											'where' => 'member_group_id='.$i['g_id']
														));											

			$kudos[$i['g_id']] = $count['count']+$count2['count'];

		}

		return $kudos;

	}
	
	public function OrderedGroups() {

		$cache = $this -> cache -> getCache('group_cache');

		$cacheG = $this -> cache -> getCache('cmtp_groups');

		if($cacheG){

		$caches = array();

		foreach ($cacheG as $i) {

			if ($cache[$i['group_id']]) {

				$cache[$i['group_id']]['g_title2']      = $i['replacement_name'];

				$cache[$i['group_id']]['display_order']      = $i['display_order'];

				$caches[$i['group_id']] = $cache[$i['group_id']];

			}

		}

		}

		return $caches;

	}
	
	public function NonGroups() {

		$cache = $this -> cache -> getCache('group_cache');

		$cacheG = $this -> cache -> getCache('cmtp_groups');

		if($cacheG){

		$d = array();
		foreach ($cacheG as $i) {
			$d[] = $i['group_id'];
		}
			
		$o = array();
		foreach($cache as $c){
			if(!in_array($c['g_id'],$d))
			{
				$o[] =$c;
			}
		}
		
			$caches = $o;

		}
		else{
			$caches = $cache;
		}

		return $caches;

	}
	
	public function cacheGroups() {

		$caches = $this -> cache -> getCache('group_cache');

		$this -> DB -> build(array('select' => 'm.*', 'from' => array('cmtp_groups' => 'm'), 'order' => 'm.display_order ASC'));

		$q = $this -> DB -> execute();

		$cache = array();

		while ($GroupItem = $this -> DB -> fetch($q)) {

			if(!$GroupItem['replacement_name'] || $GroupItem['replacement_name'] == NULL)
			{

				$cachesd = $caches[$GroupItem['group_id']];

				$GroupItem['replacement_name'] = $cachesd['g_title'];

			}

			$cache[$GroupItem['group_id']] = $GroupItem;

			$GroupItem['replacement_name'] = "";

		}

		$this -> cache -> setCache('cmtp_groups', $cache, array('array' => 1, 'donow' => 1));

	}
	
	public function GatherMembers($id){
			
			if(!is_numeric($id)){
				return "error, try again";
			}
			
			if($this->settings['cmtp_secondary_group'] == 1){
				$where = "member_group_id=".$id." OR FIND_IN_SET(".$id.", mgroup_others)";
			}
			else{
				$where = "member_group_id=".$id;
			}
						
			$this -> DB -> build(array('select' => '*',
				 						'from' => 'members', 
				 						'where' => $where,
				 						'order' => 'name asc'
								));
		
			$q = $this -> DB -> execute();
			
			$cache = array();
						
			while ($m = $this -> DB -> fetch($q)) 
			{
					
				$cache[$m['member_id']] = $m;
			
			}

			$this -> DB -> build(array('select' => '*',
				 						'from' => 'cmtp_members_added', 
				 						'where' => 'member_group_id='.$id,
				 						'order' => 'name asc'
								));
		
			$q = $this -> DB -> execute();

			while ($m = $this -> DB -> fetch($q)) 
			{
				$m['othergroup'] = 1;
				$m['members_display_name'] = $m['name'];
				$cache[$m['member_id']] = $m;
			
			}			
			
		return $cache;
	}
	
	public function cacheMembers(){
			
		$caches = $this -> cache -> getCache('group_cache');
		
		
		$cache = array();
		
		foreach($caches as $k => $c){
				
			$this -> DB -> build(array('select' => '*', 
										'from' => 'cmtp_members',
										'where' => 'group_id='.$k
										));

			$q = $this -> DB -> execute();

			$tos = array();

			while ($m = $this -> DB -> fetch($q)) {
					
				if(count($m)){
					$tos[$m['member_id']] = $m;
					}
			}
			
			if(count($tos)){
					
				$cache[$k] = $tos;
			
			}
		}
		
		$this -> cache -> setCache('cmtp_members', $cache, array('array' => 1, 'donow' => 1));		
	
	}
	
	public function BugReportDisplay()
	{

			$this->DB->build( array( 

			'select'	=> '*',

			'from'		=> 'cmtp_bugs',

			'order'	=> 'bugs_date desc',

			'limit' => array('0','5')

					)

				);
				
		$q = $this->DB->execute();

		$bugDisplay = array();

		while( $bug = $this->DB->fetch( $q ) )

		{

			$bugs['report'] = $this->parseIt($bug['bugs_report'], "display");

			$bugs['user']	= IPSMember::load($bug['bugs_submitter']);

			$bugs['contact'] = $bug['bugs_contact'];

			$bugs['date']	 = $bug['bugs_date'];

			$bugDisplay[$bug['bugs_id']] = $bugs;

		}

		return $bugDisplay;

	}

	public function CheckUpdate(){

		$ipbLong = $this -> cache -> getCache('vnums');

		$ipbLong = $ipbLong['long'];

		$app = "cmtp";

		$appLong = $this->caches['app_cache']['cmtp']['app_long_version'];

		$url = "http://codingjungle.com/checkupgrade.php?apps={$app}&ipb={$ipbLong}&appver={$appLong}";

		$classToLoad = IPSLib::loadLibrary( IPS_KERNEL_PATH . '/classFileManagement.php', 'classFileManagement' );

		$checker = new $classToLoad();	

		/* Timeout to prevent page from taking too long */

		$checker->timeout = 5;

		//$return = $checker->getFileContents( $url );

		return $checker->getFileContents( $url );

	}

	public function GrabNews(){

		$url = "http://codingjungle.com/index.php?app=ccs&module=pages&section=pages&folder=&id=5";

		$classToLoad = IPSLib::loadLibrary( IPS_KERNEL_PATH . '/classFileManagement.php', 'classFileManagement' );

		$checker = new $classToLoad();	

		/* Timeout to prevent page from taking too long */

		$checker->timeout = 5;

		$return = $checker->getFileContents( $url );

		return $return;

	}

	public function ParseNews(){

		$stream = $this->GrabNews();

		$parse = explode(",",$stream);

		foreach($parse as $key => $p)
		{

			$b = explode("*",$p);

			$b = implode("^",$b);

			$b = explode("^",$b);

			$newParse[] = $b;

		}

		$stream = $newParse;

		$count = count($stream);

		for($i = 0; $i <= $count; $i++)
		{

			if($stream[$i][0])

			{

			$stream2[$i]= array($stream[$i][0] => $stream[$i][1],$stream[$i][2] => $stream[$i][3]);

			}

		}

		return $stream2;

	}	
	
    public function parseIt($content,$type)
    {

		$classToLoad = IPSLib::loadLibrary( IPS_ROOT_PATH . 'sources/classes/editor/composite.php', 'classes_editor_composite' );

		$this->editor = new $classToLoad();

		$this->editor->setAllowHtml("1");

		if($type == "display")
		{	

			IPSText::getTextClass('bbcode')->parse_smilies			= 1;

			IPSText::getTextClass('bbcode')->parse_html				= 1;

			IPSText::getTextClass('bbcode')->parse_nl2br			= 1;

			IPSText::getTextClass('bbcode')->parse_bbcode			= 1;

			IPSText::getTextClass('bbcode')->parsing_section	    = 'cmtp';

			IPSText::getTextClass('bbcode')->parsing_mgroup			= $this->memberData['member_group_id'];

			IPSText::getTextClass('bbcode')->parsing_mgroup_others	= $this->memberData['mgroup_others'];

			$content = IPSText::getTextClass('bbcode')->preDisplayParse($content);

			$content = IPSText::getTextClass('bbcode')->preDisplayParse($content);

			return $content;	

		}
		elseif($type == "db")
		{

			IPSText::getTextClass('bbcode')->parse_smilies			= 1;

			IPSText::getTextClass('bbcode')->parse_html				= 1;

			IPSText::getTextClass('bbcode')->parse_nl2br			= 1;

			IPSText::getTextClass('bbcode')->parse_bbcode			= 1;

			IPSText::getTextClass('bbcode')->parsing_section	    = 'cmtp';

			$content = $this->editor->process(trim($content));

			$content = IPSText::getTextClass('bbcode')->preDbParse(trim($content));

			return $content;

		}
		elseif($type == "edit")
		{

			$content = IPSText::getTextClass('bbcode')->convertForRTE($content);

			return $content;

		}			

	}
	public function array_sort_by_column(&$arr, $col, $dir) {
		$sort_col = array();
		
		foreach ($arr as $key=> $row) {
			$sort_col[$key] = $row[$col];
		}
			
		return array_multisort($sort_col, $dir, $arr);
	}
	
	public function CustomModTeam() {
		//-----------------------------------------
		// Load online lang file
		//-----------------------------------------
		if($this->settings['cmtp_sort_by'] == 0){
			$dir = SORT_ASC;
		}
		else{
			$dir = SORT_DESC;
		}


		$outofgame = $this->cache->getCache("cmtp_members");
		
 		$this->lang->loadLanguageFile( array( 'public_online' ), 'members' );
		
		$this->lang->loadLanguageFile(array('public_lang'), 'cmtp');
		
		$groupCache = 	$this -> cache -> getCache('group_cache');
		
		$modCache = 	$this->cache->getCache('moderators');
		
		$modCache = is_array( $modCache ) && count( $modCache ) ? $modCache : array();
		
		$group_ids = array();
		
		foreach( $modCache as $i )
		{
		
			if ( $i['is_group'] && ! $this->caches['group_cache'][ $i['group_id'] ]['gbw_hide_leaders_page'] )
			{
		
				if ( isset( $group_ids[ $i['group_id'] ] ) )
				{
		
					if ( is_array( $group_ids[ $i['group_id'] ] ) )
					{
						$group_ids[ $i['group_id'] ][ $i['forum_id'] ] = ipsRegistry::getClass('class_forums')->forum_by_id[ $i['forum_id'] ]['name'];
					}
				}
				else
				{
		
					$group_ids[ $i['group_id'] ] = array( $i['forum_id'] => ipsRegistry::getClass('class_forums')->forum_by_id[ $i['forum_id'] ]['name'] );
		
				}
			}
			else if( $i['member_id'] )
			{
		
				$member_ids[ $i['member_id'] ] = $i['member_id'];
		
				$forumsMembers[ $i['member_id'] ][ $i['forum_id'] ] = ipsRegistry::getClass('class_forums')->forum_by_id[ $i['forum_id'] ]['name'];
		
			}
		}
	
		if(is_array($this->OrderedGroups()) && $this->settings['cmtp_layout_new'] == 0){
			
		foreach ($this->OrderedGroups() as $i) {
				
			$title = $i['g_title2'];
			
			$members = array();
			/* Custom Fields */
			
			$classToLoad = IPSLib::loadLibrary(IPS_ROOT_PATH . 'sources/classes/customfields/profileFields.php', 'customProfileFields');
			
			$custom_fields_class = new $classToLoad();
			
			if($this->settings['cmtp_secondary_group'] == 1){
			
				$where = "m.member_group_id=".$i['g_id']." OR FIND_IN_SET(".$i['g_id'].", m.mgroup_others)";
			
			}
			else{
			
				$where = "m.member_group_id=".$i['g_id'];
			
			}		
			
			$this -> DB -> build(array('select' => 'm.*', 
			'from' => array('members' => 'm'), 
			'where' => $where, 
			'add_join' => array( array('select' => 'pp.*', 
										'from' => array('profile_portal' => 'pp'), 
										'where' => 'pp.pp_member_id=m.member_id', 
										'type' => 'left', ), 
										array('select' => 's.*', 
										'from' => array('sessions' => 's'), 
										'where' => 's.member_id=m.member_id', 
										'type' => 'left', ), 
										array('select' => 'pf.*', 
										'from' => array('pfields_content' => 'pf'), 
										'where' => 'pf.member_id=m.member_id', 
										'type' => 'left', ), )
			
										));
			$outer = $this -> DB -> execute();

		while( $r = $this->DB->fetch($outer) )
		{
				
			if(!$outofgame[$i['g_id']][$r['member_id']] ){	

				$members[ $r['member_id'] ] = IPSMember::buildDisplayData( $r );
		
				if( ! $r['id'] or IPSMember::isLoggedInAnon( $r ) )
				{
					$location_info[ $r['member_id'] ] = '';
				}
				else
				{

					$location_info[ $r['member_id'] ]	= IPSMember::getLocation( $r );
				}
			}

		}
		
		$this -> DB -> build(array('select' => 'm.*',
			 						'from' => array('cmtp_members_added' => 'm'), 
			 						'where' => 'm.member_group_id='.$i['g_id']
							));
		
		$q = $this -> DB -> execute();

		while ($r = $this -> DB -> fetch($q)) 
		{
 		
				//$r['members_display_name'] = $r['name'];
					
				$cuser = IPSMember::load($r['member_id'], 'members,profile_portal,customFields', 'id');

				$members[ $r['member_id'] ] = IPSMember::buildDisplayData( $cuser );
										
				if( ! $r['id'] or IPSMember::isLoggedInAnon( $r ) )
				{
				
					$location_info[ $r['member_id'] ] = '';
					
				}
				else
				{
	
					$location_info[ $r['member_id'] ]	= IPSMember::getLocation( $r );
					
				}

		}			

		
		if($members){
			$this->array_sort_by_column($members, $this->settings['cmtp_order_by'],$dir);
		}
		foreach ($members as $k => $member) {
				if (!$this -> cache -> getCache('moderators')) {

							if($groupCache[$member['member_group_id']]['g_is_supmod'] ==1)
							{
								$forums = $this -> lang -> words['leader_all_forums'];
							}
							else{
								$forums = $this->lang->words['cmpt_public_php_nonefound'];
							}
							$members[$k]['forums'] = $forums;
							$members[$k]['online_extra'] = isset( $location_info[ $k ] ) ? $location_info[ $k ]['online_extra'] : '';
							$members[$k]['last_active']  = ( $members[ $k ]['member_id'] == $this->memberData['member_id'] ) ? IPS_UNIX_TIME_NOW : ( ( $members[ $k ]['online_extra'] ) ? $members[ $k ]['last_activity'] : $members[ $k ]['last_visit'] );

					
				} else {

							if($group_ids[$member['member_group_id']] || $member_ids[ $member['member_id'] ] )
							{

								$forums = isset( $member_ids[ $member['member_id'] ] ) ? $member_ids[ $member['member_id'] ] : array();
									
									if ( $forums == '*' )
									{
										$forums = $this->lang->words['leader_all_forums'];
									}
									else
									{
										$forums = array();
										
										foreach ( $group_ids as $gid => $fs )
										{
											if ( IPSMember::isInGroup( $member, $gid ) )
											{
												if ( $fs == '*' )
												{
													$forums = $this->lang->words['leader_all_forums'];
													break;
												}
												
												foreach ( $fs as $f_id => $f_name )
												{
													if (IPSMember::checkPermissions('read', $f_id) == TRUE) {
														$forums[ $f_id ] = $f_name;
													}
												}
											}
										}
										
										/* Now merge in member specific */
										if ( ! empty( $forumsMembers[ $member['member_id'] ] ) )
										{
											foreach( $forumsMembers[ $member['member_id'] ] as $f_id => $f_name )
											{
												if ( ! isset( $forums[ $f_id ] ) )
												{
													if (IPSMember::checkPermissions('read', $f_id) == TRUE) {
														$forums[ $f_id ] = $f_name;
													}
												}
											}
										}
									}
								
							}
							else
							{
								
								if($groupCache[$member['member_group_id']]['g_is_supmod'] ==1)
								{
									$forums = $this -> lang -> words['leader_all_forums'];
								}
								else{
									$forums = $this->lang->words['cmpt_public_php_nonefound'];
								}	
							}
 
							$members[$k]['forums'] = $forums;
						
							$members[$k]['online_extra'] = isset( $location_info[ $k ] ) ? $location_info[ $k ]['online_extra'] : '';
							$members[$k]['last_active']  = ( $members[ $k ]['member_id'] == $this->memberData['member_id'] ) ? IPS_UNIX_TIME_NOW : ( ( $members[ $k ]['online_extra'] ) ? $members[ $k ]['last_activity'] : $members[ $k ]['last_visit'] );

				}

			}

				if (count($members)) {

					$this->output .= $this->registry->getClass('output')->getTemplate('cmtp')->group_strip_fix($title, $members,"");
					
				}

		}
		
		}


		if($this -> output ){
			
			$this -> registry -> output -> setTitle($this -> lang -> words['forum_leaders'] . ' - ' . ipsRegistry::$settings['board_name']);
			
			$this -> registry -> output -> addNavigation($this -> lang -> words['forum_leaders'], '');
			
			$this -> registry -> getClass('output') -> addContent($this -> output);
			
			//generates the magic
			$this -> registry -> getClass('output') -> sendOutput();
		}
	}

	public function CustomModTeamNew(){
		if($this->settings['cmtp_sort_by'] == 0){
			$dir = SORT_ASC;
		}
		else{
			$dir = SORT_DESC;
		}		
		if(!count($this->OrderedGroups())){
			return;
		}
		//-----------------------------------------
		// Load online lang file
		//-----------------------------------------
		$st				= intval( $this->request['st'] );
		
		$perpage		= $this->settings['cmtp_layout_new_per_page'];
		
		$group_ids		= array();
		
		$member_ids		= array();
		
		$members		= array();
		
		$forumsMembers  = array();
	
		$pagination		= '';
	
		$mids			= array();
	
		$location_info	= array();
		
		$whereClause	= array();	
			
		$outofgame = $this->cache->getCache("cmtp_members");
		
 		$this->lang->loadLanguageFile( array( 'public_online' ), 'members' );
		
		$this->lang->loadLanguageFile(array('public_lang'), 'cmtp');
		
		$groupCache = 	$this -> cache -> getCache('group_cache');
		
		$modCache = 	$this->cache->getCache('moderators');
		
		$modCache = is_array( $modCache ) && count( $modCache ) ? $modCache : array();
		
		$group_ids = array();
		
		foreach( $modCache as $i )
		{
		
			if ( $i['is_group'] && ! $this->caches['group_cache'][ $i['group_id'] ]['gbw_hide_leaders_page'] )
			{
		
				if ( isset( $group_ids[ $i['group_id'] ] ) )
				{
		
					if ( is_array( $group_ids[ $i['group_id'] ] ) )
					{
						$group_ids[ $i['group_id'] ][ $i['forum_id'] ] = ipsRegistry::getClass('class_forums')->forum_by_id[ $i['forum_id'] ]['name'];
					}
				}
				else
				{
		
					$group_ids[ $i['group_id'] ] = array( $i['forum_id'] => ipsRegistry::getClass('class_forums')->forum_by_id[ $i['forum_id'] ]['name'] );
		
				}
			}
			else if( $i['member_id'] )
			{
		
				$member_ids[ $i['member_id'] ] = $i['member_id'];
		
				$forumsMembers[ $i['member_id'] ][ $i['forum_id'] ] = ipsRegistry::getClass('class_forums')->forum_by_id[ $i['forum_id'] ]['name'];
		
			}
		}
				
			$title = $this->settings['cmtp_layout_new_title'];
			
			$members = array();
			/* Custom Fields */
			
			$classToLoad = IPSLib::loadLibrary(IPS_ROOT_PATH . 'sources/classes/customfields/profileFields.php', 'customProfileFields');
			
			$custom_fields_class = new $classToLoad();
			
			if($this->settings['cmtp_secondary_group'] == 1){

				$is = count($this->OrderedGroups()) -1;
			
				foreach ($this->OrderedGroups() as $i) {
					
					$where .= "m.member_group_id=".$i['g_id']." OR FIND_IN_SET(".$i['g_id'].", m.mgroup_others)";
				
					if($is != 0){
				
						$where .= " OR ";
				
					}
					
					$is--;
					
				}
			}
			else{
			
				$is = count($this->OrderedGroups())-1;
				
				foreach ($this->OrderedGroups() as $i) {
					
					$where .= "m.member_group_id=".$i['g_id'];
					
					if($is != 0){
				
						$where .= " OR ";
				
					}
					
					$is--;
				}	
			}

				foreach($this->OrderedGroups() as $k =>$c)
				{
					$co = count($outofgame[$k]) + $co;  
				}

 			$count = $this->DB->buildAndFetch( array( 'select' => 'count(*) as dracula', 'from' => array( 'members' => 'm' ), 'where'=> $where));
						
			$count = $count['dracula'] - $co;

			/* Sort out pagination */
			$pagination = $this->registry->output->generatePagination( array( 'totalItems'			=> $count,
																			  'itemsPerPage'		=> $perpage,
																			  'currentStartValue'	=> $st,
																			  'baseUrl'				=> "app=forums&module=extras&section=stats&do=leaders" ) );
																			  			
			$this -> DB -> build(array('select' => 'm.*', 
			'from' => array('members' => 'm'), 
			'where' => $where, 
			'add_join' => array( array('select' => 'pp.*', 
										'from' => array('profile_portal' => 'pp'), 
										'where' => 'pp.pp_member_id=m.member_id', 
										'type' => 'left', ), 
										array('select' => 's.*', 
										'from' => array('sessions' => 's'), 
										'where' => 's.member_id=m.member_id', 
										'type' => 'left', ), 
										array('select' => 'pf.*', 
										'from' => array('pfields_content' => 'pf'), 
										'where' => 'pf.member_id=m.member_id', 
										'type' => 'left', ), ),
			'limit'		=> array( $st, $perpage )			
										));
			$outer = $this -> DB -> execute();

		while( $r = $this->DB->fetch($outer) )
		{
				
			if(!$outofgame[$i['g_id']][$r['member_id']] ){	

				$members[ $r['member_id'] ] = IPSMember::buildDisplayData( $r );
		
				if( ! $r['id'] or IPSMember::isLoggedInAnon( $r ) )
				{
					$location_info[ $r['member_id'] ] = '';
				}
				else
				{

					$location_info[ $r['member_id'] ]	= IPSMember::getLocation( $r );
				}
			}

		}

		$this -> DB -> build(array('select' => 'm.*',
			 						'from' => array('cmtp_members_added' => 'm'), 
			 						'where' => 'm.member_group_id='.$i['g_id']
							));
		
		$q = $this -> DB -> execute();

		while ($r = $this -> DB -> fetch($q)) 
		{
		
				//$r['members_display_name'] = $r['name'];
					
				$cuser = IPSMember::load($r['member_id'], 'members,profile_portal,customFields', 'id');

				$members[ $r['member_id'] ] = IPSMember::buildDisplayData( $cuser );
										
				if( ! $r['id'] or IPSMember::isLoggedInAnon( $r ) )
				{
				
					$location_info[ $r['member_id'] ] = '';
					
				}
				else
				{
	
					$location_info[ $r['member_id'] ]	= IPSMember::getLocation( $r );
					
				}

		}	
		if($members){
			$this->array_sort_by_column($members, $this->settings['cmtp_order_by'],$dir);
		}
		foreach ($members as $k => $member) {
				if (!$this -> cache -> getCache('moderators')) {

							if($groupCache[$member['member_group_id']]['g_is_supmod'] ==1)
							{
								$forums = $this -> lang -> words['leader_all_forums'];
							}
							else{
								$forums = $this->lang->words['cmpt_public_php_nonefound'];
							}
							$members[$k]['forums'] = $forums;
							$members[$k]['online_extra'] = isset( $location_info[ $k ] ) ? $location_info[ $k ]['online_extra'] : '';
							$members[$k]['last_active']  = ( $members[ $k ]['member_id'] == $this->memberData['member_id'] ) ? IPS_UNIX_TIME_NOW : ( ( $members[ $k ]['online_extra'] ) ? $members[ $k ]['last_activity'] : $members[ $k ]['last_visit'] );

					
				} else {

							if($group_ids[$member['member_group_id']] || $member_ids[ $member['member_id'] ] )
							{

								$forums = isset( $member_ids[ $member['member_id'] ] ) ? $member_ids[ $member['member_id'] ] : array();
									
									if ( $forums == '*' )
									{
										$forums = $this->lang->words['leader_all_forums'];
									}
									else
									{
										$forums = array();
										
										foreach ( $group_ids as $gid => $fs )
										{
											if ( IPSMember::isInGroup( $member, $gid ) )
											{
												if ( $fs == '*' )
												{
													$forums = $this->lang->words['leader_all_forums'];
													break;
												}
												
												foreach ( $fs as $f_id => $f_name )
												{
													if (IPSMember::checkPermissions('read', $f_id) == TRUE) {
														$forums[ $f_id ] = $f_name;
													}
												}
											}
										}
										
										/* Now merge in member specific */
										if ( ! empty( $forumsMembers[ $member['member_id'] ] ) )
										{
											foreach( $forumsMembers[ $member['member_id'] ] as $f_id => $f_name )
											{
												if ( ! isset( $forums[ $f_id ] ) )
												{
													if (IPSMember::checkPermissions('read', $f_id) == TRUE) {
														$forums[ $f_id ] = $f_name;
													}
												}
											}
										}
									}
								
							}
							else
							{
								
								if($groupCache[$member['member_group_id']]['g_is_supmod'] ==1)
								{
									$forums = $this -> lang -> words['leader_all_forums'];
								}
								else{
									$forums = $this->lang->words['cmpt_public_php_nonefound'];
								}	
							}
 
							$members[$k]['forums'] = $forums;
						
							$members[$k]['online_extra'] = isset( $location_info[ $k ] ) ? $location_info[ $k ]['online_extra'] : '';
							$members[$k]['last_active']  = ( $members[ $k ]['member_id'] == $this->memberData['member_id'] ) ? IPS_UNIX_TIME_NOW : ( ( $members[ $k ]['online_extra'] ) ? $members[ $k ]['last_activity'] : $members[ $k ]['last_visit'] );

				}

			}

				if (count($members)) {

					$this->output .= $this->registry->getClass('output')->getTemplate('cmtp')->group_strip_fix($title, $members,$pagination);
					
				}

		if($this -> output ){
			
			$this -> registry -> output -> setTitle($this -> lang -> words['forum_leaders'] . ' - ' . ipsRegistry::$settings['board_name']);
			
			$this -> registry -> output -> addNavigation($this -> lang -> words['forum_leaders'], '');
			
			$this -> registry -> getClass('output') -> addContent($this -> output);
			
			//generates the magic
			$this -> registry -> getClass('output') -> sendOutput();
		}		
	}
}
