<?php

/* Badges */

$TABLE[] = "CREATE TABLE HQ_badges (
  ba_id 		int(10) 		NOT NULL AUTO_INCREMENT,
  ba_gid 		varchar(255) 	NOT NULL DEFAULT '0',
  ba_type   	varchar(255) 	NOT NULL,
  ba_t		   	int(10) 		NULL,
  ba_r		   	int(10)		 	NULL,
  ba_background	varchar(255)	NULL, 
  ba_links		varchar(255) 	NULL,
  ba_enabled	tinyint(1)		NOT NULL DEFAULT '0', 
  ba_forums		varchar(2000)	NULL,

  PRIMARY KEY (ba_id)
)"; 