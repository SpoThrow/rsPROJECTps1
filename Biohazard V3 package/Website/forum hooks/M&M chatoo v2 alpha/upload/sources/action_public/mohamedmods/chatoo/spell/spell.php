<?

/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          M&M-ShoutBox v1 Alpha
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Created By:  mohamed
  Website:     http://www.invisioneyes.com
  Email:       traxman_08@hotmail.com
  file:        spell.php
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
All Codes have been written by mohamed for
      IPB 2.1.x M&M Chatoo v1 alpha
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
         Copyright (©) mohamed,2006
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

$allowed_html = '<p><a><br><b><i><img><strong><small><ul><li>';

if(!function_exists('pspell_suggest')){
	require_once ("pspell_comp.php");
}
$pspell_config = pspell_config_create("en");
pspell_config_mode($pspell_config, PSPELL_FAST);
$pspell_link = pspell_new_config($pspell_config);
require_once("cpaint.inc.php");

//=============================================================
// The showSuggestions function creates the list of up to 10
// suggestions to return for the given misspelled word.
//=============================================================

function showSuggestions($word, $id){ 

	global $pspell_link; 

	$retVal = "";
	
	$suggestions = pspell_suggest($pspell_link, $word);
	
	$numSuggestions = count($suggestions);
	
	$numSuggestionsToReturn = 10;	

	if($numSuggestions < $numSuggestionsToReturn){
		$tmpNum = $numSuggestions;
	}
	else{

	$tmpNum = $numSuggestionsToReturn;
	}
	
	if ($tmpNum > 0) {
		$retVal .= "<table>";

		for($i=0; $i<$tmpNum; $i++) {
			$retVal .= "<tr><td onmouseover=\"this.style.backgroundColor='004080'; this.style.color='FFFFFF';\" onmouseout=\"this.style.backgroundColor='E8F1FF'; this.style.color='000000';\"><span class=\"suggestion\" onClick=\"replaceWord('" . addslashes_custom($id) . "', '" . addslashes($suggestions[$i]) . "')\">$suggestions[$i]</span></td></tr>";
		}
	
		$retVal .= "</table>";
	}
	else {
		$retVal .= "No Suggestions";
	}
	
	return $retVal; 
	
} 


//===========================================================
// The spellCheck function takes the string of text entered
// in the text box and spell checks it.  It splits the text
// on anything inside of < > in order to prevent html from being
// spell checked.  Then any text is split on spaces so that only
// one word is spell checked at a time.  This creates a multidimensional
// array.  The array is flattened.  The array is looped through
// ignoring the html lines and spell checking the others.  If a word
// is misspelled, code is wrapped around it to highlight it and to
// make it clickable to show the user the suggestions for that
// misspelled word.
//===========================================================

function spellCheck($string) {
   
   global $pspell_link; 

   $retVal = "";
   $isThereAMisspelling = false;
   
   $string = stripslashes_custom($string); 
   
   $string = preg_replace("/\r?\n/", "\n", $string);
   
   $words = preg_split("/(<[^>]*>)/", $string, -1, PREG_SPLIT_DELIM_CAPTURE);
   
   $numResults = count($words); //the number of elements in the array.
   
  for($x=0; $x<$numResults; $x++){

   	if(!preg_match("/<[^>]*>/", $words[$x])){ 

	 	$words[$x] = preg_split("/(\s+)/", $words[$x], -1, PREG_SPLIT_DELIM_CAPTURE); 

	}
	else
	{ 

		$words[$x] = preg_replace("/</", "<!--<", $words[$x]);
   		$words[$x] = preg_replace("/>/", ">-->", $words[$x]);
	}
  }
  
  $words = flattenArray($words);
  
  $numResults = count($words);
  
  for ($i=0; $i<$numResults; $i++) {	
	if(!preg_match("/<[^>]*>/", $words[$i])){
	
		  preg_match("/[A-Z']{1,16}/i", $words[$i], $tmp);
		  $tmpWord = $tmp[0];

		  if (!pspell_check($pspell_link, $tmpWord)) {			 
			 $isThereAMisspelling = true;		 
			 $onClick = "onClick=\"showSuggestions('" . addslashes($tmpWord) . "', '" . $i . "_" . addslashes($tmpWord) . "');\"";
			 $words[$i] = str_replace($tmpWord, "<span " . $onClick . " id=\"" . $i . "_" . $tmpWord . "\" class=\"highlight\">" . stripslashes($tmpWord) . "</span>", $words[$i]); 
		  }
		  
		  $words[$i] = preg_replace("/\n/", "<br />", $words[$i]); 
	  }
   }

   $string = ""; 
   

   if(!$isThereAMisspelling)
   {
     $string = "0";
   }
   else
   { 
   	$string = "1";
   }
	
   for($i=0; $i<$numResults; $i++){
	$string .= $words[$i];
   }
	
	//but we want the html to be rendered in the div for preview purposes.
	$string = preg_replace("/<!--<br( [^>]*)?>-->/i", "<br />", $string);
	$string = preg_replace("/<!--<p( [^>]*)?>-->/i", "<p>", $string);
	$string = preg_replace("/<!--<\/p>-->/i", "</p>", $string);
	$string = preg_replace("/<!--<b( [^>]*)?>-->/i", "<b>", $string);
	$string = preg_replace("/<!--<\/b>-->/i", "</b>", $string);
	$string = preg_replace("/<!--<strong( [^>]*)?>-->/i", "<strong>", $string);
	$string = preg_replace("/<!--<\/strong>-->/i", "</strong>", $string);
	$string = preg_replace("/<!--<i( [^>]*)?>-->/i", "<i>", $string);
	$string = preg_replace("/<!--<\/i>-->/i", "</i>", $string);
	$string = preg_replace("/<!--<small( [^>]*)?>-->/i", "<small>", $string);
	$string = preg_replace("/<!--<\/small>-->/i", "</small>", $string);
	$string = preg_replace("/<!--<ul( [^>]*)?>-->/i", "<ul>", $string);
	$string = preg_replace("/<!--<\/ul>-->/i", "</ul>", $string);
	$string = preg_replace("/<!--<li( [^>]*)?>-->/i", "<li>", $string);
	$string = preg_replace("/<!--<\/li>-->/i", "</li>", $string);
	$string = preg_replace("/<!--<img (?:[^>]+ )?src=\"?([^\"]*)\"?[^>]*>-->/i", "<img src=\"\\1\" />", $string);
	
	return $string;

} 



//===========================================================
// The flattenArray function is a recursive function that takes a
// multidimensional array and flattens it to be a one-dimensional
// array.  The one-dimensional flattened array is returned.
//===========================================================

function flattenArray($array)
{
   $flatArray = array();
   foreach($array as $subElement){
       if(is_array($subElement))
           $flatArray = array_merge($flatArray, flattenArray($subElement));
       else
           $flatArray[] = $subElement;
   }
   return $flatArray;
   
} 


//===========================================================
// This is a custom stripslashes function that only strips
// the slashes if magic quotes are on.  This is written for
// compatibility with other servers in the event someone doesn't
// have magic quotes on.
//===========================================================

function stripslashes_custom($string){

	if(get_magic_quotes_gpc()){
		return stripslashes($string);
	}
	else {
		return $string;
	}
}

//===========================================================
// This is a custom addslashes function that only adds
// the slashes if magic quotes are off.  This is written for
// compatibility with other servers in the event someone doesn't
// have magic quotes on.
//===========================================================

function addslashes_custom($string){

	if(!get_magic_quotes_gpc()){
		return addslashes($string);
	}
	else {
		return $string;
	}
}


//===========================================================
// This function prepares the text to be sent back to the text
// box from the div.  The comments are removed and breaks are
// converted back into \n's.  All the html tags that the user
// might have entered that aren't on the approved list:
// <p><br><a><b><strong><i><small><ul><li> are stripped out.
// The user-entered returns have already been replaced with
// $u2026 so that they can be preserved.  I replace all the 
// \n's that might have been added by the browser (Firefox does
// this in trying to pretty up the HTML) with " " so that 
// everything will look the way it did when the user typed it
// in the box the first time.
//===========================================================
function switchText($string){
	global $allowed_html;
	$string = preg_replace("/<!--/", "", $string);
	$string = preg_replace("/-->/", "", $string);
	$string = preg_replace("/\r?\n/", " ", $string);
	$string = strip_tags($string, $allowed_html);
	$string = stripslashes_custom($string); //we only need to strip slashes if magic quotes are on
	$string = html_entity_decode($string);
	return $string;
	
}

?>