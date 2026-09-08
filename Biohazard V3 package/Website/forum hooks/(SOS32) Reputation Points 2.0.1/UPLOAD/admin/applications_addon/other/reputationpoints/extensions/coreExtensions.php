<?php

/**
 * Product Title:		(SOS32) Reputation Points
 * Product Version:		2.0.1
 * Author:				Adriano Faria
 * Website:				SOS Invision
 * Website URL:			http://forum.sosinvision.com.br/
 * Email:				administracao@sosinvision.com.br
 */
 
/**
* Main loader class
*/
class publicSessions__reputationpoints
{
	public function getSessionVariables()
	{
		//-----------------------------------------
		// INIT
		//-----------------------------------------
		$array = array( 'location_1_type'   => '',
						'location_1_id'     => 0,
						'location_2_type'   => '',
						'location_2_id'     => 0
						);

		return $array;
	}

	/**
	 * Parse/format the online list data for the records
	 *
	 * @access	public
	 * @author	Terabyte
	 * @param	array		Online list rows to check against
	 * @return   array		Online list rows parsed
	 */
	public function parseOnlineEntries( $rows )
	{
		if( !is_array($rows) || !count($rows) )
		{
			return $rows;
		}
		
		/* Load language file */
		ipsRegistry::getClass('class_localization')->loadLanguageFile( array( 'public_reputation' ), 'reputationpoints' );
		
		$final = array();
		
		foreach( $rows as $row )
		{
			if( $row['current_appcomponent'] == 'reputationpoints' )
			{
				$row['where_line'] = ipsRegistry::getClass('class_localization')->words['WHERE_reputacao'];
				$row['where_link'] = 'app=reputationpoints';
			}

			$final[ $row['id'] ] = $row;
		}
		
		return $final;
	}
}