<?php

$TABLE[] = "CREATE TABLE IF NOT EXISTS `".SQL_PREFIX."reputation` (
  `reputationid` int(11) unsigned NOT NULL auto_increment,
  `reputation` int(10) NOT NULL default '0',
  `whoadded` int(10) NOT NULL default '0',
  `reason` varchar(250) default NULL,
  `dateline` int(10) NOT NULL default '0',
  `postid` int(10) NOT NULL default '0',
  `userid` mediumint(8) NOT NULL default '0',
  PRIMARY KEY (`reputationid`),
  KEY `userid` (`userid`),
  KEY `whoadded` (`whoadded`),
  KEY `multi` (`postid`,`userid`),
  KEY `dateline` (`dateline`)
);";

$TABLE[] = "CREATE TABLE IF NOT EXISTS `".SQL_PREFIX."reputationlevel` (
  `reputationlevelid` int(11) NOT NULL auto_increment,
  `minimumreputation` int(10) NOT NULL default '0',
  `level` varchar(250) default NULL,
  PRIMARY KEY (`reputationlevelid`),
  KEY `reputationlevel` (`minimumreputation`)
);";

$GALT[] = "ALTER TABLE `".SQL_PREFIX."groups` ADD `g_rep_use` tinyint(1) NOT NULL default '1';";
$GALT[] = "ALTER TABLE `".SQL_PREFIX."groups` ADD `g_rep_negative` tinyint(1) NOT NULL default '1';";
$GALT[] = "ALTER TABLE `".SQL_PREFIX."groups` ADD `g_rep_seeown` tinyint(1) NOT NULL default '0';";
$GALT[] = "ALTER TABLE `".SQL_PREFIX."groups` ADD `g_rep_hide` tinyint(1) NOT NULL default '0';";

$MALT[] = "ALTER TABLE `".SQL_PREFIX."members` ADD `reputation` int(10) NOT NULL default '10';";

$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('-999999','is infamous around these parts');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('-50','can only hope to improve');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('-10','has a little shameless behaviour in the past');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('0','is an unknown quantity at this point');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('10','is on a distinguished road');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('50','will become famous soon enough');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('150','has a spectacular aura about');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('250','is a jewel in the rough');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('350','is just really nice');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('450','is a glorious beacon of light');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('550','is a name known to all');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('650','is a splendid one to behold');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('1000','has much to be proud of');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('1500','has a brilliant future');";
$INSC[] = "INSERT INTO `".SQL_PREFIX."reputationlevel` (minimumreputation, level) VALUES ('2000','has a reputation beyond repute');";

?>