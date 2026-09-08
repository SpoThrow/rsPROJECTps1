<?PHP
/**
 * @ Application : 		Custom Moderator Team Page v2.0.0
 * @ Last Updated : 	June 13th, 2012 
 * @ Author :			Michael S. Edwards
 * @ Copyright :		(c) 2011 Coding Jungle
 * @ Link	 :			http://www.codingjungle.com/
 */
class cp_skin_overview extends output {

	/* We must declare a destructor */

	public function __destruct() {

	}

	public function JavascriptGroups() {

		$acp = CP_DIRECTORY;

		$html .= <<<HTML

			<script type='text/javascript'>

				var BugDeleted = "{$this->lang->words['cmtp_js_bug_delete']}";

				var startBugReport = "{$this->lang->words['cmtp_js_bug_startBug']}";

				var NotAValidEmail = "{$this->lang->words['cmtp_js_bug_NotValidEmail']}";

				var BugReportBlank = "{$this->lang->words['cmtp_js_bug_BugReportBlank']}";

				var BugReportSent = "{$this->lang->words['cmtp_js_bug_BugReportSent']}";

			</script>

			<script src="{$this->settings['board_url']}/{$acp}/applications_addon/other/cmtp/js/acp.bugreport.js"></script>

HTML;

		return $html;

	}

	public function overview() {

		$stream = $this->registry->getClass('cmtp')->ParseNews();

		$html = "";

		$html .= <<<HTML

			<div class='section_title'>

				<h2>{$this->lang->words['cmtp_php_OverView']}</h2>

			</div>

			<table width='90%' valign='top' align='left'>

				<tr>

					<td width='60%' valign='top'>

						<div class='acp-box'>

							<h3>{$this->lang->words['cmtp_php_News']}</h3>

								<table class="ipsTable double_pad" style="width:100%;">

									<tbody>

HTML;

								if(is_array($stream))

								{

									foreach($stream as $news)

									{

										$html .= <<<HTML

											<tr style="width:100%;">

												<td style="width:100%;">

													<a href="{$news['url']}" target='_blank'>{$news['name']}</a><br>

												</td>

											</tr>

HTML;

									}

								}

								else

								{

									$html .= <<<HTML

										<tr style="width:100%;">

											<td style="width:100%;">

												{$this->lang->words['cmtp_php_NewsBad']}

											</td>

										</tr>

HTML;

								}

		$html .= <<<HTML

								</tbody>

							</table>

						</div>

						<br class="clear" />

HTML;

		$html .= $this->bugReport();
		$update = $this->registry->getClass("cmtp")->CheckUpdate();
		$html .= <<<HTML

					</td>

					<td width='2%'></td>

					<td width='38%' valign='top'>

						<div class="acp-box">

							<h3>{$this->lang->words['cmtp_php_GeneralInfo']}</h3>

								<table class="ipsTable">

									<tr>

										<td style="width: 40%;"><strong>{$this->lang->words['cmtp_php_Installed']}</strong></td>

										<td style="width: 60%; text-align: center;">{$this->caches['app_cache']['cmtp']['app_version']}

										</td>

									</tr>
									<tr>
										<td style="width: 40%;"><strong>{$this->lang->words['cmtp_php_CurrentVersion']}</strong></td>
										<td style="width: 60%; text-align: center;">{$update}

										</td>
									</tr>
								</table>

						</div>

						<br>

						<div id='BugsReported'></div>

					</td>

				</tr>

			</table>

HTML;

		//--endhtml--//

		return $html;

	}

	public function bugReport(){

		$html .="";

		$html .=<<<HTML

			<div id="AllBuggerUp" class='acp-box' style="width:100%;">

				<h3>{$this->lang->words['cmtp_php_BugReport']}</h3>

					<table class="ipsTable double_pad" style="width:100%;">

					<tbody>

						<tr style="width:100%;">

							<td style="width:100%;">

								<div id="GradeA" class='left'  style='display:block;width:100%;'>

								{$this->lang->words['cmtp_php_BugReportBody']}<span id="startBugReport" class="mini_button" style='cursor:pointer;'>{$this->lang->words['cmtp_php_BugReportButton']}</span>

								</div>

								<div id="GradeB" class="left" style='display:none;width:100%;'>

									<div style='text-align:left;'>

										{$this->lang->words['cmtp_php_BugReportContact']}<br>

										<input type="" name="contact" id="contact" class="input_text">

										<br><br>

									</div>

									<div id="Reports" style='text-align:left;width:100%;'>

										{$this->lang->words['cmtp_php_BugReportDescription']}<br>

										<textarea id="bugReport" cols="40" rows="5" wrap="soft" class="multitext"></textarea>

									</div>

									<div style='text-align:center'>

									<br>

										<input id="GrabBug" type="submit" class="realbutton" value="{$this->lang->words['cmtp_php_BugReportSend']}">

									</div>

								</div>

								<div id="GradeC" class='left' style="display:none;width:100%;">

									{$this->lang->words['cmtp_php_BugReportReturn']}

								</div>

							</td>

						</tr>

					</tbody>

					</table>

			</div>

HTML;

		return $html;

	}

	public function BugReportDisplay($bugs){

		$this -> lang -> loadLanguageFile(array('admin_lang'), 'cmtp');

		$acp = CP_DIRECTORY;		

		$html .= <<<HTML

			<script src="{$this->settings['board_url']}/{$acp}/applications_addon/other/cmtp/js/acp.bugDelete.js"></script>

HTML;

		$html .="";

		$html .= <<<HTML

			<div class="acp-box">

				<h3>{$this->lang->words['cmtp_php_BugReportDisplay']}</h3>

					<table class="ipsTable">

						<tr>

							<td style="width:100%;">	

HTML;

		if(is_array($bugs) && count($bugs))

		{

			foreach($bugs as $key => $bug)

			{

				$html .= <<<HTML

					<div >

						<strong>{$this->lang->words['cmtp_php_BugReportDate']}: </strong>{$this->lang->formatTime($bug['date'],"long",0)}

						<div class='col_buttons right'><div class='icon delete'>

							<span style='cursor:pointer;color:#3287C9;' id='DeleteBug' d="{$key}">{$this->lang->words['cmtp_php_BugReportDelete']}</span>

						</div>

					</div>

					<br>

					<strong>{$this->lang->words['cmtp_php_BugReportUser']}: </strong>{$bug['user']['members_display_name']}<br>

					<strong>{$this->lang->words['cmtp_php_BugReportDisplayLatest']}: </strong>

					{$bug['report']}

					<div style="height:1.5em;border-bottom: 1px solid #000;"></div>

					<br>

					<br>

					</div>

HTML;

			}

		}

		else

		{
			$html .= <<<HTML

			<div><strong>{$this->lang->words['cmtp_php_BugReportDisplayNone']}</strong></div>

HTML;

		}

		$html .= <<<HTML

							</td>



						</tr>

					</table>

			</div>

HTML;

		return $html;

	}

}
