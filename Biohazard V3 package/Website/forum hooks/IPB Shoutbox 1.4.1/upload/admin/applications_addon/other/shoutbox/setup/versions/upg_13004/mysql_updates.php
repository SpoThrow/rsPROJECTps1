<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

$SQL[] = "ALTER TABLE members ADD shoutbox_shouts BIGINT( 8 ) NOT NULL DEFAULT '0';";
$SQL[] = "UPDATE core_applications SET app_title='Shoutbox', app_description='Shoutbox adds a feature rich shoutbox to your existing IP.Board&#33;', app_author='Larry Lewis' WHERE app_directory='shoutbox';";
$SQL[] = "DROP TABLE IF EXISTS shoutbox_stats;";
$SQL[] = "CREATE TABLE shoutbox_stats (
  s_date date NOT NULL,
  s_count bigint(8) NOT NULL DEFAULT '0',
  PRIMARY KEY (`s_date`)
);";
