<?php

$SQL[] = "ALTER TABLE cmtp_groups ADD INDEX(display_order)";

$SQL[] = "CREATE TABLE cmtp_members (
	group_id				int(255) NOT NULL,
	member_id				int(255) NOT NULL,
	etc						varchar(255),
	KEY (group_id),
	KEY (member_id)
);";

$SQL[] = "CREATE TABLE cmtp_members_added (
	member_group_id				int(255) NOT NULL,
	member_id					int(255) NOT NULL,
	name						varchar(255),
	KEY (member_group_id),
	KEY (member_id)
);";