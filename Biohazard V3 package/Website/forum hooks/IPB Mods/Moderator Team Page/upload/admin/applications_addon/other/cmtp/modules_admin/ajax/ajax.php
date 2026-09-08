<?php
/**
 * @ Application : 		Custom Moderator Team Page v2.0.0
 * @ Last Updated : 	June 13th, 2012 
 * @ Author :			Michael S. Edwards
 * @ Copyright :		(c) 2011 Coding Jungle
 * @ Link	 :			http://www.codingjungle.com/
 */
class admin_cmtp_ajax_ajax extends ipsAjaxCommand
{
	/**
	 * HTML library
	 *
	 * @access	public
	 * @var		object
	 */
	public $html;

	public function doExecute( ipsRegistry $registry )
	{

		//-----------------------------------------
		// What to do?
		//-----------------------------------------
	
		switch( $this->request['do'] )
		{
			case 'displaygroups':
				$this->displaygroups();
				break;
			case 'addGroup':
				$this->addGroup();
				break;
			case 'deleteGroup':
				$this->deleteGroup();
				break;
			case 'doReOrder':
				$this->doReOrder();
				break;
			case 'GetMembers':
				$this->GetMembers();
				break;
			case 'insertMembers':
				$this->insertMembers();
				break;
			case 'delmembers':
				$this->delmembers();
				break;
			case 'AddMemberToGroup':
				$this->AddMemberToGroup();
				break;
		}
	}
	
	public function displaygroups(){
		$this->returnHtml($this->registry->output->loadTemplate( 'cp_skin_groups' )->displaygroups());		
	}
	
	public function addGroup(){
		/*get the group ID to add*/	
		$group = intval($this->request['agroup']);
		
		$ordering= $this->request['ordering'];
		
		$count = $this->request['count'];
		
		if(!is_numeric($group) || !is_numeric($count))
		{
			$this->returnHtml("error");
		}
		
		if($this->registry->getClass('class_permissions')->checkPermission( "cmtp_add_groups_display", "cmtp", "groups" ) == false)
		{
			$this->returnHtml("noperm");
		}		
		 /* get the cache of groups to check again*/
		$cache = $this->cache->getCache('group_cache');

		/* get the group cache*/
		$group_cache = $this->cache->getCache('cmtp_groups');

			/* check to see if the group exist, if not, return error*/
			if(!$cache[$group])
			{

				$this->returnHtml('notThere');

			}
			else if($group_cache[$group] && $ordering != "Edit")
			{

				$this->returnHtml('exist');

			}
			else if($ordering == "Edit"){

				if($this->request['newName']){
					
					$newname = $this->request['newName'];

				}
				else {

					$newname = $cache[$group]['g_title'];

				}
				
				$this->DB->update( 'cmtp_groups', 

				array( 'replacement_name' => $newname ), 

				'group_id=' . $group );

				$this -> registry -> getClass('cmtp') -> cacheGroups();

				$this -> returnHtml('yippy');				

			}
			else{

				$addGroup['group_id'] = $group;

				if($this->request['newName']){

					$newname = $this->request['newName'];

				}
				else {
					$newname = $cache[$group]['g_title'];
				}

				$addGroup['replacement_name'] = $newname;

				$addGroup['display_order']	  = 0;
				
				$addGroup['display_order'] = $count;

				$this->DB->insert(

				"cmtp_groups",

				$addGroup

				);

				$this -> registry -> getClass('cmtp') -> cacheGroups();

				$this -> returnHtml('yippy');

			}
	}

	
	public function deleteGroup(){
		
		/*get the group ID to add*/	
		$group = intval($this->request['agroup']);
		
		if(!is_numeric($group))
		{
			$this->returnHtml("error");
		}
		if($this->registry->getClass('class_permissions')->checkPermission( "cmtp_add_groups_display", "cmtp", "groups" ) == false)
		{
			$this->returnHtml("noperm");
		}		
		/* get the cache of groups to check again*/
		$cache = $this->cache->getCache('group_cache');

		/* get the group cache*/
		$group_cache = $this->cache->getCache('cmtp_groups');

			/* check to see if the group exist, if not, return error*/
			if(!$cache[$group])
			{

				$this->returnHtml('notThere');

			}
			else if(!$group_cache[$group])
			{

				$this->returnHtml('hmm');

			}
			else{

				$deleteGroup['group_id'] = $group;

				$this->DB->delete(

				"cmtp_groups",

				"group_id=".$group

				);
				
				$this->DB->delete(
					
					"cmtp_members",
					
					"group_id=".$group
				);
				
				$this->DB->delete(
					
					"cmtp_members_added",
					
					"member_group_id=".$group
				);
				
				$this -> DB -> build(array('select' => 'm.*', 'from' => array('cmtp_groups' => 'm'), 'order' => 'm.display_order ASC'));
		
				$q = $this -> DB -> execute();
		
				$cache = array();
				
				$i = 1;
				
				while ($GroupItem = $this -> DB -> fetch($q)) {
				
					$GroupItem['display_order'] = $i;
				
					$cache[$GroupItem['group_id']] = $GroupItem;
				
					$i++;
				
				}
				
				foreach($cache as $k => $c){
					$this->DB->update( 'cmtp_groups', 

					array( 'display_order' => $c['display_order'] ), 

					'group_id=' . $k );		
				}
				
				$this->registry->getClass('cmtp')->cacheGroups();
				
				$this->registry->getClass('cmtp')->cacheMembers();

				$this -> returnHtml('yippy');

			}

	}

	public function doReOrder()
	{

		/* Define the position */

		$position	= 1;

		/* Check if the array is present and count the menus */
		if( is_array($this->request['groups']) AND count($this->request['groups']) )
		{

			/* Time to update the database with the new position */
			foreach( $this->request['groups'] as $this_id )
			{

				$this->DB->update( 'cmtp_groups', array( 'display_order' => $position ), 'group_id=' . $this_id );

				$position++;

			}

		}

		$this -> registry -> getClass('cmtp') -> cacheGroups();

		$this->returnHtml('succes');		

	}
	
	public function GetMembers(){
		if( $this->registry->getClass('class_permissions')->checkPermission( "cmtp_manage_members", "cmtp", "groups" ) == false)
		{
			$this->returnHtml("noperm");
		}
		
		$members = $this->registry->getClass('cmtp')->GatherMembers($this->request['id']);
		
		$this->returnHtml($this->registry->output->loadTemplate( 'cp_skin_groups' )->GetMembers($members,$this->request['id']));			
	}
	
	public function insertMembers(){

		$m['member_id'] = $this->request['id'];

		$m['group_id']	= $this->request['group'];
		
		if(!is_numeric($m['group_id']) || !is_numeric($m['member_id'])){
			$this->returnHtml("error");
		}

		if( $this->registry->getClass('class_permissions')->checkPermission( "cmtp_manage_members", "cmtp", "groups" ) == false)
		{
			$this->returnHtml("noperm");
		}
		
		if($this->request['other'] != 1)
		{
			$this->DB->insert(
				"cmtp_members",
				$m
			);
		}
		else{
			$this->DB->delete(
					"cmtp_members_added",
					"member_id=".$m['member_id']." AND member_group_id=".$m['group_id']
				);	
		}
		$this->registry->getClass('cmtp')->cacheMembers();

		$this->returnHtml('success');		
		
	}
	
	public function delmembers(){
			
		$id = $this->request['id'];
		
		$group = $this->request['group'];
		
		if(!is_numeric($id) || !is_numeric($group) ){
			$this->returnHtml("error");
		}
		
		if($this->registry->getClass('class_permissions')->checkPermission( "cmtp_manage_members", "cmtp", "groups" ) == false)
		{
			$this->returnHtml("noperm");
		}
				
		$this->DB->delete(
					"cmtp_members",
					"member_id=".$id." AND group_id=".$group
				);
	
		$this->registry->getClass('cmtp')->cacheMembers();
				
		$this->returnHtml("success");
	}
	
	public function AddMemberToGroup(){
		$member = $this->request['member'];
		
		$group = $this->request['group'];
		
		if(!intval($group)){
			$this->returnHtml("error");
		}

		$this -> DB -> build(array('select' => 'member_id',
			 						'from' => 'members', 
			 						'where' => 'name="'.$member.'"',
			 						'order' => 'name asc'
								));
								
		$q = $this -> DB -> execute();
				
		$m = $this -> DB -> fetch($q); 
		
		if(!$m['member_id'])
		{
				
			$this->returnHtml("notexist");
		
		}
		else{
			
			$ma['member_id'] = $m['member_id'];
		
			$ma['name']  = $member;
		
			$ma['member_group_id'] = $group;
			 
			$this->DB->insert(
				"cmtp_members_added",
				$ma
			);
				
			$this->returnHtml("success");
		}

	}
}