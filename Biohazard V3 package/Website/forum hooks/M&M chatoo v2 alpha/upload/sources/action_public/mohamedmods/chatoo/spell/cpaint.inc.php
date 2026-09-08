<?
// CPAINT (Cross-Platform Asynchronous INterface Toolkit) - Version 1.3-SP
// Copyright (c) 2005 Boolean Systems, Inc. - http://cpaint.sourceforge.net

// $Id$
// $Log$


error_reporting (E_ALL ^ E_NOTICE); 
global $cpaint_xml_result;
header ("Expires: Fri, 14 Mar 1980 20:53:00 GMT");
header ("Last-Modified: " . gmdate("D, d M Y H:i:s") . " GMT");
header ("Cache-Control: no-cache, must-revalidate");
header ("Pragma: no-cache"); 
if ($_GET['cpaint_returnxml'] == "true") header("Content-Type:  text/xml");
if ($_POST['cpaint_returnxml'] == "true") header("Content-Type:  text/xml");
if ($_GET['cpaint_function'] != "") {
  if (checkBlacklist($_GET['cpaint_function']) == true) {
  	print(call_user_func_array($_GET['cpaint_function'], $_GET['cpaint_argument']));
  	exit();
  } else {
    print("A function name was passed that is not allowed to execute on this server.");
    exit();
  }
} elseif ($_POST['cpaint_function']) {
  if (checkBlacklist($_POST['cpaint_function']) == true) {
  	print(call_user_func_array($_POST['cpaint_function'], $_POST['cpaint_argument']));
  	exit();
  } else {
    print("A function name was passed that is not allowed to execute on this server.");
    exit();
  }
}
function cpaint_xml_return_data() {
	global $cpaint_xml_result;
	return "<?xml version=\"1.0\" standalone=\"yes\"?><AJAX-RESPONSE>" . $cpaint_xml_result . "</AJAX-RESPONSE>";
}
function cpaint_xml_add_data($dataname, $uniqueid, $datavalue) {
	global $cpaint_xml_result;
	$cpaint_xml_result = $cpaint_xml_result . "<" . strtoupper($dataname) . " ID=\"" . $uniqueid . "\">" . $datavalue . "</" . strtoupper($dataname) . ">";
}
function cpaint_xml_open_result($uniqueid) {
	global $cpaint_xml_result;
	$cpaint_xml_result = $cpaint_xml_result . "<AJAX-RESULT ID=\"" . $uniqueid . "\">";
}
function cpaint_xml_close_result() {
	global $cpaint_xml_result;
	$cpaint_xml_result = $cpaint_xml_result . "</AJAX-RESULT>";
}

function checkBlacklist($function_name) {
  $return_value = true;
  $function_name = strtolower($function_name);
  $phpint = get_defined_functions();
  // disallow every built-in PHP function
  $bl = $phpint['internal'];
  // language constructs seem to be missing... we add the most obvious
  $bl[] = 'eval';
  $bl[] = 'preg_replace';
  $bl[] = 'ereg';
  $bl[] = 'eregi';
  $bl[] = 'call_user_func_array';
  $bl[] = 'call_user_func';
  $bl[] = 'register_shutdown_function';
  $bl[] = 'register_tick_function';
  $bl[] = 'include';
  $bl[] = 'include_once';
  $bl[] = 'require';
  $bl[] = 'require_once';
  $bl[] = 'system';
  $bl[] = 'passthru';
  $bl[] = 'exec';
  $bl[] = 'file_get_contents';
  $bl[] = 'proc_open';
  $bl[] = 'shell_exec';
	
  if (in_array($function_name, $bl)) {
    $return_value = false;
  } // end: if
  
  return $return_value;
}
?>