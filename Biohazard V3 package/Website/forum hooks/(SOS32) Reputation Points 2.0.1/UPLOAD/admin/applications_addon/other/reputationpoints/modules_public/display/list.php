<?php

/**
 * Product Title:		(SOS32) Reputation Points
 * Product Version:		2.0.1
 * Author:				Adriano Faria
 * Website:				SOS Invision
 * Website URL:			http://forum.sosinvision.com.br/
 * Email:				administracao@sosinvision.com.br
 */

if ( ! defined( 'IN_IPB' ) )
{
    print "<h1>Incorrect access</h1>You cannot access this file directly. If you have recently upgraded, make sure you upgraded all the relevant files.";
    exit();
}

class public_reputationpoints_display_list extends ipsCommand
{
    protected $output               = '';
    protected $pageTitle            = '';   

	public function doExecute( ipsRegistry $registry )
	{
        switch( $this->request['do'] )
        {
        	default:
        		$this->_showList();
        	break;
        }

		$this->registry->output->addContent( $this->output );
		$this->registry->getClass('output')->sendOutput();
	}

    public function _showList()
    {
	 	if ( !$this->settings['reputacao_systemon'] )
	 	{
			$this->registry->output->showError( $this->lang->words['systemoff'] );
		}
		
		if ( !in_array( $this->memberData['member_group_id'], explode( ',', $this->settings['reputacao_grupos'] ) ) )
		{
			$this->registry->output->showError( $this->lang->words['erro_permissao'] );
		}
		
		/* Add some CSS */
		$this->registry->output->addToDocumentHead( 'importcss' , "{$this->settings['css_base_url']}style_css/{$this->registry->output->skin['_csscacheid']}/ipb_mlist.css" );
		/* Add some JS */
		$this->registry->output->addToDocumentHead( 'javascript', "{$this->settings['js_base_url']}js/ips.memberlist.js" );

		$this->registry->output->addNavigation( $this->lang->words['title'], '' );
		
		$positive = array();
		$negative = array();
		 
		$where = "";
		$nr	   = intval( $this->settings['reputacao_nr'] ) ? $this->settings['reputacao_nr'] : 30;
		
		/* Usuários banidos ? */
		if ( !$this->settings['reputacao_banned'] )
		{
			$where = " and member_banned = 0 AND member_group_id != {$this->settings['banned_group']}";
		}
		
		/* Top Users + */
        $this->DB->build(array( 'select'   => 'm.member_id, m.members_display_name, m.members_seo_name, m.member_group_id, m.has_blog, m.has_gallery, m.posts, m.joined',
                                'from'     => array('members' => 'm'),
                                'add_join' => array(
                                0 => array( 'select' => 'pp.*',
						  	  				'from'   => array( 'profile_portal' => 'pp' ),
										  	'where'  => 'pp.pp_member_id=m.member_id',
							  				'type'   => 'left' ),
                                ),
                                'where'	 => "pp_reputation_points > 0".$where,
                                'order'  => "pp.pp_reputation_points DESC, m.member_id",
   								'limit'  => array( 0, $nr ),
        ) );
		
		$outer = $this->DB->execute();
		
		$rank = 0;

		while( $r = $this->DB->fetch( $outer ) )
		{
			$r = IPSMember::buildDisplayData( $r );
			$r['rank'] = ++$rank;
			$r['pp_reputation_points'] = $this->registry->getClass('class_localization')->formatNumber($r['pp_reputation_points']);
			$positive[] = $r;
		}

		/* Top Users - */
        $this->DB->build(array( 'select'   => 'm.member_id, m.members_display_name, m.members_seo_name, m.member_group_id, m.has_blog, m.has_gallery, m.posts, m.joined',
                                'from'     => array('members' => 'm'),
                                'add_join' => array(
                                0 => array( 'select' => 'pp.*',
						  	  				'from'   => array( 'profile_portal' => 'pp' ),
										  	'where'  => 'pp.pp_member_id=m.member_id',
							  				'type'   => 'left' ),
                                ),
                                'where'	 => "pp_reputation_points < 0".$where,
                                'order'  => "pp.pp_reputation_points ASC, m.member_id",
   								'limit'  => array( 0, $nr ),
        ) );
		
		$outer2 = $this->DB->execute();
		
		$rank = 0;

		while( $r = $this->DB->fetch( $outer2 ) )
		{
			$r = IPSMember::buildDisplayData( $r );
			$r['rank'] = ++$rank;
			$r['pp_reputation_points'] = $this->registry->getClass('class_localization')->formatNumber($r['pp_reputation_points']);
			$negative[] = $r;
		}
		
		$this->output .= $this->registry->output->getTemplate( 'reputationpoints' )->listReps( $positive, $negative );

        $this->pageTitle = $this->settings['board_name']." - ".$this->lang->words['title'];
		$this->registry->output->setTitle( $this->pageTitle );
		$this->registry->output->addContent( $this->output );
		$this->registry->output->sendOutput();
    }
}
?>