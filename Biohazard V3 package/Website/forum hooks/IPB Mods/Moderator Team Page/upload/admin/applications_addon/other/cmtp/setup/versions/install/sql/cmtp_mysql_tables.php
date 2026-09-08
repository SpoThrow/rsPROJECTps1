<?php
/**
 * @ Application : 		ProMenu
 * @ File : 			promenu_mysql_tables.php
 * @ Last Updated : 	Jan 3, 2012 3:26:49 AM
 * @ Author :			Robert Simons
 * @ Copyright :		(c) 2011 Provisionists, LLC
 * @ Link	 :			http://www.provisionists.com/
 */


$TABLE[] = "CREATE TABLE cmtp_groups (
	group_id				int(25) NOT NULL,
	display_order			int(25) NOT NULL,
	replacement_name 		varchar(255),
	KEY (group_id),
	KEY (display_order)
);";

$TABLE[] = "CREATE TABLE cmtp_bugs (
	bugs_id					int(10) NOT NULL auto_increment,
	bugs_submitter			varchar(225) NOT NULL,
	bugs_contact			varchar(50) NOT NULL,
	bugs_report				text NOT NULL,
	bugs_date				varchar(255) NOT NULL,
	bugs_sent				int(10) NOT NULL,
	PRIMARY KEY (bugs_id)
);";

$TABLE[] = "CREATE TABLE cmtp_members (
	group_id				int(255) NOT NULL,
	member_id				int(255) NOT NULL,
	etc						varchar(255),
	KEY (group_id),
	KEY (member_id)
);";

$TABLE[] = "CREATE TABLE cmtp_members_added (
	member_group_id				int(255) NOT NULL,
	member_id					int(255) NOT NULL,
	name						varchar(255),
	KEY (member_group_id),
	KEY (member_id)
);";

