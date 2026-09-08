<?php

$SQL[] = "ALTER TABLE HQ_badges ADD ba_links varchar(255) NULL";
$SQL[] = "ALTER TABLE HQ_badges CHANGE ba_forums ba_forums VARCHAR( 2000 ) NULL DEFAULT NULL";