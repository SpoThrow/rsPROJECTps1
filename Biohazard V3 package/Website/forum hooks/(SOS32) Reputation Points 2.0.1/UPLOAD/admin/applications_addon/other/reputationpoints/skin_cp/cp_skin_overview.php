<?php

/**
 * Product Title:		(SOS32) Reputation Points
 * Product Version:		2.0.1
 * Author:				Adriano Faria
 * Website:				SOS Invision
 * Website URL:			http://forum.sosinvision.com.br/
 * Email:				administracao@sosinvision.com.br
 */
 
class cp_skin_overview extends output
{


public function __destruct()
{
}

//===========================================================================
// Upload Overview Index
//===========================================================================
function reputacaoOverviewIndex() {

$IPBHTML = "";
//--starthtml--//

$IPBHTML .= <<<HTML
<div class='section_title'>
	<h2>{$this->lang->words['visao_geral']}</h2>
</div>

<table width='100%'>
	<tr>
	    <td width='50%' valign='top'>
	        <div class="acp-box">
			    <h3>About {$this->caches['app_cache']['reputationpoints']['app_title']}</h3>
				<table class='alternate_rows double_pad' width='100%'>
					<tr>
						<td width='40%'><strong>{$this->lang->words['versao']}</strong></td>
						<td width='60%' align='center'><b> {$this->caches['app_cache']['reputationpoints']['app_version']}</b></td>
					</tr>
					<tr>
						<td><strong>{$this->lang->words['autor']}</strong></td>
						<td align='center'>Adriano Faria</td>
					</tr>
					<tr>
						<td><strong>{$this->lang->words['site']}</strong></td>
						<td align='center'><a href="http://forum.sosinvision.com.br" target="_blank">SOS Invision</a></td>
					</tr>
					<tr>
						<td colspan="2" align="center">
							<form target="_blank" action='https://www.paypal.com/cgi-bin/webscr' method='post'>
							         <input type='hidden' name='cmd' value='_xclick' />
							         <input type='hidden' name='business' value='adrfaria@yahoo.com.br' />
							         <input type='hidden' name='item_name' value='Donation for (SOS32) Reputation Points' />
							         <input type='hidden' name='no_note' value='1' />
							         <input type='hidden' name='currency_code' value='USD' />
							         <input type='hidden' name='on0' value='Name' />
							         <input type='hidden' name='on1' value='Forum' />
							         <input type='hidden' name='os0' value='{$this->memberData['members_display_name']}' />
							         <input type='hidden' name='os1' value='{$this->settings['board_name']}' />
							         <input type='hidden' name='cancel_return' value='http://www.sosinvision.com/index.php?act=idx' />
							         <input type='image' src='https://www.paypal.com/en_US/i/btn/btn_donateCC_LG.gif' name='submit' alt='Faça uma doação ao SOS Invision.' style='border:0px; background:transparent' />
							 </form>
						</td>
					</tr>
				</table>
			</div>
		</td>
	</tr>
</table>
HTML;

//--endhtml--//
return $IPBHTML;
}


}
?>