<?php

/**
 * Product Title:		IPB Shoutbox
 * Author:				Pete Treanor
 * Website URL:			http://www.ipbshoutbox.com
 * Copyright©:			IPB Works All rights Reserved 2011-2013
 */

if ( !defined( 'IN_IPB' ) )
{
	print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
	exit();
}

class task_item
{
	/**
	 * Parent task manager class
	 *
	 * @access	protected
	 * @var		object
	 */
	protected $class;

	/**
	 * This task data
	 *
	 * @access	protected
	 * @var		array
	 */
	protected $task			= array();

	/**
	 * Prevent logging
	 *
	 * @access	protected
	 * @var		boolean
	 */
	protected $restrict_log	= false;

	/**
	* Registry Object Shortcuts
	*/
	protected $registry;
	protected $DB;
	protected $settings;
	protected $lang;

	/**
	 * Constructor
	 *
	 * @access	public
	 * @param 	object		ipsRegistry reference
	 * @param 	object		Parent task class
	 * @param	array 		This task data
	 * @return	void
	 */
	public function __construct( ipsRegistry $registry, $class, $task )
	{
		/* Make registry objects */
		$this->registry	= $registry;
		$this->DB		= $this->registry->DB();
		$this->settings =& $this->registry->fetchSettings();
		$this->lang		= $this->registry->getClass('class_localization');

		$this->class	= $class;
		$this->task		= $task;
	}

	/**
	 * Run this task
	 *
	 * @access	public
	 * @return	void
	 */
	public function runTask()
	{
		$this->lang->loadLanguageFile( array( 'public_shoutbox' ), 'shoutbox' );

		if( ! $this->settings['shoutbox_twitter_enabled'] )
		{
            $this->class->appendTaskLog( $this->task, $this->lang->words['task_tweet_disabled'] );
            return;
		}

		if( empty( $this->settings['shoutbox_twitter_ckey'] ) OR empty( $this->settings['shoutbox_twitter_csecret'] ) OR empty( $this->settings['shoutbox_twitter_atoken'] ) OR empty( $this->settings['shoutbox_twitter_asecret'] ) )
		{
            $this->class->appendTaskLog( $this->task, $this->lang->words['task_tweet_noapi'] );
            return;
		}

		if( empty( $this->settings['shoutbox_twitter_names'] ) )
		{
            $this->class->appendTaskLog( $this->task, $this->lang->words['task_tweet_nouser'] );
            return;
		}

		//-------------------------------
		// Load Lib & Rebuild Cache
		//-------------------------------
		if( !class_exists('app_class_shoutbox') OR !$this->registry->isClassLoaded('shoutboxLibrary') )
		{
			require_once( IPSLib::getAppDir('shoutbox').'/app_class_shoutbox.php' );
			$app_class_shoutbox = new app_class_shoutbox( ipsRegistry::instance() );
		}

		@set_include_path( IPS_KERNEL_PATH . 'twitter/' . PATH_SEPARATOR . ini_get( 'include_path' ) );/*noLibHook*/
		/* Load API */
		require_once( IPS_KERNEL_PATH . 'twitter/twitteroauth.php' );/*noLibHook*/
		$this->registry->getClass('shoutboxLibrary')->global_on = true;
		$this->registry->getClass('shoutboxLibrary')->_startup();

		$twitFile = DOC_IPS_ROOT_PATH . '/cache/shoutbox_tweet.php';
		$twitList = array();
		$twitNames = explode( ',', str_replace( '@', '', $this->settings['shoutbox_twitter_names'] ) );
		$connection = new TwitterOAuth( $this->settings['shoutbox_twitter_ckey'], $this->settings['shoutbox_twitter_csecret'], $this->settings['shoutbox_twitter_atoken'], $this->settings['shoutbox_twitter_asecret'] );
		$connection->decode_json = false;
		if( @file_exists( $twitFile ) )
		{
			$twitList = $this->load( $twitFile );
		}
		$recache = false;
		$update = true;
		$count = 0;
		$member = IPSMember::load( $this->settings['shoutbox_twitter_user'], 'none', 'username' );
		$m_id = ( isset( $member['member_id'] ) ) ? $member['member_id'] : 1;
		foreach( $twitNames AS $v )
		{
			if( ! isset( $twitList[$v] ) )
			{
				$x = $connection->get( 'statuses/user_timeline', array( 'screen_name'	=> $v,
																		'count'			=> 1
																		) );
				$twitList[$v] = '0';
			}
			else
			{
				$x = $connection->get( 'statuses/user_timeline', array( 'screen_name'	=> $v,
																		'since_id'		=> $twitList[$v]
																		) );
			}
			$x = json_decode( $x, true );
			$x = array_reverse( $x );
			foreach( $x AS $y )
			{
				$twitList[$v] = $y['id_str'];
				$tweet = "Tweet(" . $this->linkify( "@{$v}" ) . "): " . $this->linkify( $y['text'] );
				$this->DB->insert( 'shoutbox_shouts',
									   array(	's_mid'		=> $m_id,
										  		's_message' => $tweet,
										  		's_date'    => strtotime( $y['created_at'] ),
										  		's_ip'      => 'System'
									 	)
								  );
				$recache = true;
				$count++;
			}
		}
		$this->save( $twitFile, $twitList );
		if( $recache )
		{
			$this->registry->getClass('shoutboxLibrary')->recacheShouts();
		}

		//-----------------------------------------
		// Log to logs table
		//-----------------------------------------
		if ( !$this->restrict_log )
		{
            $this->class->appendTaskLog( $this->task, sprintf( $this->lang->words['task_pruned_shouts'], $count ) );
		}
		//-----------------------------------------
		// Unlock Task: DO NOT MODIFY!
		//-----------------------------------------
		$this->class->unlockTask( $this->task );
	}
	protected function save( $file, $stuff )
	{
		$list  = "; <?php exit(); __halt_compiler();\n";
		$list .= json_encode( $stuff );
		file_put_contents( $file, $list );
	}

	protected function load( $file )
	{
		$x = file( $file );
		return( json_decode( $x[1], true ) );
	}

	protected function linkify( $status_text )
	{
	  // linkify URLs
	  $status_text = preg_replace( '/(https?:\/\/\S+)/', '<a href="\1" rel="nofollow" target="_blank">\1</a>', $status_text );

	  // linkify twitter users
	  $status_text = preg_replace( '/(^|\s)@(\w+)/', '\1@<a href="http://twitter.com/\2">\2</a>', $status_text );

	  // linkify tags
	  $status_text = preg_replace( '/(^|\s)#(\w+)/', '\1#<a href="http://search.twitter.com/search?q=%23\2">\2</a>', $status_text );

	  return $status_text;
	}
}
