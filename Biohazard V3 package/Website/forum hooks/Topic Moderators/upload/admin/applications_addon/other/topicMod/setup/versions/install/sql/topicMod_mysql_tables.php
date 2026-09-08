<?php

$TABLE[] = "CREATE TABLE topic_moderators (
  id mediumint(8) NOT NULL AUTO_INCREMENT,
  member_id mediumint(8) NOT NULL DEFAULT '0',
  topic_id mediumint(8) NOT NULL DEFAULT '0',
  forums varchar(255) DEFAULT NULL,
  close_topic tinyint(1) DEFAULT NULL,
  open_topic tinyint(1) DEFAULT NULL,
  edit_topic tinyint(1) DEFAULT NULL,
  pin_topic tinyint(1) DEFAULT NULL,
  unpin_topic tinyint(1) DEFAULT NULL,
  edit_post tinyint(1) DEFAULT NULL,
  post_q tinyint(1) DEFAULT NULL,
  group_id mediumint(8) NOT NULL DEFAULT '0',
  moderate_own tinyint(1) DEFAULT NULL,
  mod_bitoptions int(10) NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
  );";

?>