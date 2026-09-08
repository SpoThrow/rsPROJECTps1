<?php

/**
 * Product Title:		Shoutbox
 * Author:				IPB Works
 * Website URL:			http://www.ipbworks.com/forums
 * Copyright©:			IPB Works All rights Reserved 2011-2012
 */

// Remove old templates
$SQL[] = "DELETE FROM skin_templates WHERE template_group='skin_shoutbox' AND template_name IN ('my_prefs_update','my_prefs','mod_options','members_viewing','members_viewing_row');";