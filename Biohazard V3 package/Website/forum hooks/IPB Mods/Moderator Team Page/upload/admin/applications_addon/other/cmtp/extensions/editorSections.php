<?php
/**
 * @ Application : 		Custom Moderator Team Page v2.0.0
 * @ Last Updated : 	June 13th, 2012 
 * @ Author :			Michael S. Edwards
 * @ Copyright :		(c) 2011 Coding Jungle
 * @ Link	 :			http://www.codingjungle.com/
 */

if ( ! defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

/*
 * An array of key => value pairs
 * When going to parse, the key should be passed to the editor
 *  to determine which bbcodes should be parsed in the section
 *
 */
$BBCODE	= array(
				'cmtp'			=> "Custom Moderator Team Page",
				);
