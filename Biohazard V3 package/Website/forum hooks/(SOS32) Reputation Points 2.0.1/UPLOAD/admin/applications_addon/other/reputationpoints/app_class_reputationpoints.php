<?php

/**
 * Product Title:		(SOS32) Reputation Points
 * Product Version:		2.0.1
 * Author:				Adriano Faria
 * Website:				SOS Invision
 * Website URL:			http://forum.sosinvision.com.br/
 * Email:				administracao@sosinvision.com.br
 */

class app_class_reputationpoints
{
	public function __construct( ipsRegistry $registry )
	{
		if ( IN_ACP )
		{
			$registry->getClass('class_localization')->loadLanguageFile( array( 'admin_reputation' ),  'reputationpoints' );
		}
		else
		{
			$registry->getClass('class_localization')->loadLanguageFile( array( 'public_reputation' ), 'reputationpoints' );
		}
	}
}