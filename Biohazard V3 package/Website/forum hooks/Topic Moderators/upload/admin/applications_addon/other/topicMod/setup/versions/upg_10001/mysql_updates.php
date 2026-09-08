<?php

$SQL[] = "ALTER TABLE topic_moderators ADD group_id mediumint(8) NOT NULL DEFAULT '0'";
$SQL[] = "ALTER TABLE topic_moderators ADD moderate_own tinyint(1) DEFAULT NULL";