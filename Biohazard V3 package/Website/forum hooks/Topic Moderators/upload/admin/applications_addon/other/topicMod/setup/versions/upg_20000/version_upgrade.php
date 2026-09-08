<?php

class version_upgrade
{
	/**
	 * Custom HTML to show
	 *
	 * @var		string
	 */
	protected $_output = '';
	
	/**
	 * fetchs output
	 * 
	 * @return	string
	 */
	public function fetchOutput()
	{
		return $this->_output;
	}
	
	/**
	 * Execute selected method
	 *
	 * @param	object		Registry object
	 * @return	@e void
	 */
	public function doExecute( ipsRegistry $registry ) 
	{
		/* Make object */
		$this->registry =  $registry;
		$this->DB       =  $this->registry->DB();
		$this->settings =& $this->registry->fetchSettings();
		$this->request  =& $this->registry->fetchRequest();
		$this->cache    =  $this->registry->cache();
		$this->caches   =& $this->registry->cache()->fetchCaches();
		

		/* Add bitwise field */
		$this->DB->addField( 'topic_moderators', 'mod_bitoptions', 'int(10)', '0' );
		
		$this->DB->build( array( 'select' => '*', 'from' => 'topic_moderators' ) );
		$outer = $this->DB->execute();
		
		if ( $this->DB->getTotalRows() )
		{
			while( $r = $this->DB->fetch( $outer ) )
			{
				$bw['gbw_soft_delete'] = $r['delete_post'];
				
				$bitOptions = IPSBWOptions::freeze( $bw, 'moderators', 'topicMod' );
				
				$this->DB->update( 'topic_moderators', array( 'mod_bitoptions' => $bitOptions ), 'id=' . $r['id'] );
			}
		}
		
		$this->DB->dropField( 'topic_moderators', 'delete_post' );

		/* Message */
		$this->registry->output->addMessage( "All hard delete posts permissions changed to soft delete...." );
		
		/* Next Page */
		$this->request['workact'] = '';
		
		return true;
	}
}