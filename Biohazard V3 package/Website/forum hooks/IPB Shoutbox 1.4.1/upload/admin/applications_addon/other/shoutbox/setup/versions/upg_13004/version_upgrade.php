<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

if (!defined('IN_IPB'))
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

class version_upgrade
{
	public function doExecute(ipsRegistry $registry)
	{
		$this->registry =  $registry;
		$this->DB       =  $this->registry->DB();

		$this->DB->build( array(	'select'	=> 'COUNT(*) AS count, s_mid',
									'from'		=> 'shoutbox_shouts',
									'group'		=> 's_mid',
									'order'		=> 's_mid',
									'where'		=> "s_mid > 0"
								) );
		$result = $this->DB->execute();
		while ( $row = $this->DB->fetch( $result ) )
		{
			$this->DB->update( 'members', array( 'shoutbox_shouts' => $row['count'] ), "member_id = {$row['s_mid']}" );
		}
		$this->DB->build( array(	'select'	=> "s_date",
									'from'		=> 'shoutbox_shouts',
									'order'		=> 's_date',
								) );
		$result = $this->DB->execute();
		$sd = array();
		while ( $row = $this->DB->fetch( $result ) )
		{
			$d = date( 'Y-m-d', $row['s_date'] );
			if( isset( $sd[ $d ] ) )
			{
				$sd[ $d ]++;
				continue;
			}
			$sd[ $d ] = 1;
		}
		foreach( $sd AS $d => $c )
		{
			$this->DB->insert(	'shoutbox_stats',
						   		array(	's_date'	=> $d,
								  		's_count'	=> $c,
								 )
						  );
		}
		return true;
	}

	public function fetchOutput()
	{
		return "";
	}
}
