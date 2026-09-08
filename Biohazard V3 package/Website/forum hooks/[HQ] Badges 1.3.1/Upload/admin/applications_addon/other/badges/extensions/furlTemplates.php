<?php

if ( !defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

$_SEOTEMPLATES = array( 'app=badges' => array( 'app'			=> 'badges',
												'allowRedirect' => 1,
												'out'           => array( '/app=badges/i', 'badges/' ),
												'in'            => array( 'regex'   => "#^/badges(/|$|\?)#i", 'matches' => array( array( 'app', 'badges' ) ) ) )
					  );