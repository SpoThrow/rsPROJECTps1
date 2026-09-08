<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

/* Let's upgrade the hooks extra data to avoid uninstalling the templates since the new hooks doesn't have them '*/
$SQL[] = "UPDATE core_hooks SET hook_extra_data='a:4:{s:13:\"settingGroups\";a:1:{s:0:\"\";s:2:\"38\";}s:8:\"settings\";a:0:{}s:7:\"display\";a:2:{s:8:\"settings\";s:37:\"Setting groups: Hook: Global Shoutbox\";s:9:\"templates\";s:0:\"\";}s:9:\"templates\";a:0:{}}' WHERE hook_key='ipshoutbox_global';";
$SQL[] = "UPDATE core_hooks SET hook_extra_data='a:4:{s:13:\"settingGroups\";a:1:{s:0:\"\";s:2:\"39\";}s:8:\"settings\";a:0:{}s:7:\"display\";a:2:{s:8:\"settings\";s:34:\"Setting groups: Hook: Active Users\";s:9:\"templates\";s:0:\"\";}s:9:\"templates\";a:0:{}}' WHERE hook_key='ipshoutbox_active_users';";