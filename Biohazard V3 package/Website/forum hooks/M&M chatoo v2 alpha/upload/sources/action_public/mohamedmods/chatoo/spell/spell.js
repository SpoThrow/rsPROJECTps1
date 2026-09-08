/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          M&M-ShoutBox v1 Alpha
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Created By:  mohamed
  Website:     http://www.invisioneyes.com
  Email:       traxman_08@hotmail.com
  file:        spell.js
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
All Codes have been written by mohamed for
      IPB 2.1.x M&M Chatoo v1 alpha
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
         Copyright (©) mohamed,2006
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

var cp = new cpaint();
cp.set_transfer_mode('post');
cp.set_response_type('text');



if (document.onclick) {
	var old_onclick = document.onclick;
	document.onclick = function(e) {
		checkClickLocation(e);
		old_onclick(e);
	}
} else {
	document.onclick = checkClickLocation;
}

var currObj; 
var spellingSuggestionsDiv = null;


//============================================================
// function setupSpellCheckers()
//============================================================

function setupSpellCheckers(){
	textareas = document.getElementsByTagName('INPUT');
	var numSpellCheckers = 0;
	tempSpellCheckers = Array();

	for(var i=0; i < textareas.length; i++){
		if(textareas[i].getAttribute("title") == "spellcheck"){
			tempSpellCheckers[numSpellCheckers] = textareas[i];
			numSpellCheckers++;
		}
	}
	
	for(i=0; i < tempSpellCheckers.length; i++){
		eval('spellCheckers' + i + '= new ajaxSpell("spellCheckers' + i + '", tempSpellCheckers[' + i + '].style.width, tempSpellCheckers[' + i + '].style.height, tempSpellCheckers[' + i + '].getAttribute("accesskey"), "spellCheckDiv' + i + '", tempSpellCheckers[' + i + '].getAttribute("name"), tempSpellCheckers[' + i + '].id, tempSpellCheckers[' + i + '].value);');
	}
	
};


//============================================================
// ajaxSpell(varName, width, height, spellUrl, divId, name, id)
//
//============================================================

function ajaxSpell(varName, width, height, spellUrl, divId, name, id, value){
	
	currObj = this;
	
	currObj.config               = new Array();         //the array of configuration options
	currObj.config['varName']    = varName;             //the name of the variable that this instance is stored in
	currObj.config['width']      = '360px';               //the width of the textarea
	currObj.config['height']     = '18px';              //the height of the textarea
	currObj.config['spellUrl']   = spellUrl;            //url to spell checker php code (spell_checker.php by default);
	currObj.config['divId']      = divId;               //the id of the div that the spell checker element is in
	currObj.config['name']       = name;                //what you want the form element's name to be
	currObj.config['id']         = id;                  //the unique id of the spell_checker textarea
	currObj.config['value']      = value;               //the value of the text box when the page was loaded
	currObj.config['value']      = currObj.config['value'].replace(/<br *\/?>/gi, "\n");
	spellContainer = document.createElement('DIV');
	spellContainer.id = currObj.config['divId'];
	spellContainer.style.width = currObj.config['width'];
	oldElement = document.getElementById(currObj.config['id']);
	oldElement.parentNode.replaceChild(spellContainer, oldElement);
	currObj.controlPanelDiv = document.createElement('DIV');
	currObj.controlPanelDiv.className = 'control_panel';
    currObj.controlPanelDiv.setAttribute("align", "center");
	document.getElementById(currObj.config['divId']).appendChild(currObj.controlPanelDiv);
	currObj.actionSpan = document.createElement('SPAN');
	currObj.actionSpan.className = "action";
	currObj.actionSpan.id = "action";
    currObj.actionSpan.style.display = "none";
	currObj.actionSpan.innerHTML = "<a class=\"check_spelling\" onclick=\"setCurrentObject(" + currObj.config['varName'] + "); " + currObj.config['varName'] + ".spellCheck();\">Preview & SpellCheck</a>";
	currObj.controlPanelDiv.appendChild(currObj.actionSpan);
	currObj.statusSpan = document.createElement('SPAN');
	currObj.statusSpan.className = "status";
	currObj.statusSpan.id = "status";
	currObj.controlPanelDiv.appendChild(currObj.statusSpan);
	currObj.textBox = document.createElement('INPUT');
    currObj.textBox.className = "text_box";
    currObj.textBox.setAttribute("style" , "float:left;");
	currObj.textBox.setAttribute("title", "spellcheck");
    currObj.textBox.style.display = "block";
	currObj.textBox.id = currObj.config['id'];
	currObj.textBox.style.width = currObj.config['width'];
	currObj.textBox.style.height = currObj.config['height'];
	currObj.textBox.setAttribute("rows", "");
	currObj.textBox.setAttribute("cols", "");
	if (checkBrowser() == "ie"){
    currObj.textBox.style.display = "block";
    currObj.textBox.setAttribute("onclick", "javascript:checkStatus('active');");
	}
	if (checkBrowser() != "ie"){
	currObj.textBox.setAttribute("onblur", "javascript:checkStatus('');");
	currObj.textBox.setAttribute("onfocus", "javascript:checkStatus('active');");
	}
	currObj.textBox.name = currObj.config['name'];
	currObj.textBox.value = currObj.config['value'];
	document.getElementById(currObj.config['divId']).appendChild(currObj.textBox);
	currObj.objToCheck              = document.getElementById(currObj.config['id']);      
	currObj.spellingResultsDiv      = null;                                               
	//prototypes for the ajaxSpell objects
	ajaxSpell.prototype.spellCheck           = spellCheck;
	ajaxSpell.prototype.spellCheck_cb        = spellCheck_cb;
	ajaxSpell.prototype.showSuggestions      = showSuggestions;
	ajaxSpell.prototype.showSuggestions_cb   = showSuggestions_cb;
	ajaxSpell.prototype.replaceWord          = replaceWord;
	ajaxSpell.prototype.switchText           = switchText;
	ajaxSpell.prototype.switchText_cb        = switchText_cb;
	ajaxSpell.prototype.resumeEditing        = resumeEditing;
	ajaxSpell.prototype.resetSpellChecker    = resetSpellChecker;
	ajaxSpell.prototype.resetAction          = resetAction;
};


//============================================================
// setCurrentObject(obj)
//============================================================

function setCurrentObject(obj){
	currObj = obj;
};


//============================================================
// spellCheck_cb(new_data)
//============================================================

function spellCheck_cb(new_data) {
	with(currObj);
	new_data = new_data.toString();
	var isThereAMisspelling = new_data.charAt(0);
	new_data = new_data.substring(1);
		
	if (currObj.spellingResultsDiv) {
		currObj.spellingResultsDiv.parentNode.removeChild(spellingResultsDiv);
	}
	currObj.spellingResultsDiv = document.createElement('DIV');
	currObj.spellingResultsDiv.className = 'edit_box';
    currObj.spellingResultsDiv.setAttribute("style" , "float:left;");
	currObj.spellingResultsDiv.style.width = currObj.objToCheck.style.width;
	currObj.spellingResultsDiv.style.height = currObj.objToCheck.style.height;

	currObj.spellingResultsDiv.innerHTML = new_data;
    currObj.objToCheck.style.display = "none";
	currObj.objToCheck.parentNode.insertBefore(currObj.spellingResultsDiv,currObj.objToCheck);
	
	currObj.statusSpan.innerHTML = "";
	currObj.actionSpan.innerHTML = "<a class=\"resume_editing\" onclick=\"setCurrentObject(" + currObj.config['varName'] + "); " + currObj.config['varName'] + ".resumeEditing();\">Resume</a>";
		
	if(isThereAMisspelling != "1"){
		currObj.statusSpan.innerHTML = "No Misspellings Found";
		currObj.objToCheck.disabled = false;
	}
};


//============================================================
// spellCheck()
//============================================================

function spellCheck() {
	with(currObj);
	var query;
	
	if (currObj.spellingResultsDiv) {
		currObj.spellingResultsDiv.parentNode.removeChild(currObj.spellingResultsDiv);
		currObj.spellingResultsDiv = null;
	}
 	
	currObj.actionSpan.innerHTML = "<a class=\"check_spelling\">Preview & SpellCheck</a>";
	currObj.statusSpan.innerHTML = "Checking...";
	query = currObj.objToCheck.value;

	cp.call(currObj.config['spellUrl'], 'spellCheck', spellCheck_cb, query, currObj.config['varName']);
};



//============================================================
// The replaceWord function takes the id of the misspelled word
// that the user clicked on and replaces the innerHTML of that
// span with the new word that the user selects from the suggestion
// div.  It hides the suggestions div and changes the color of
// the previously misspelled word to green to let the user know
// it has been changed.  It then calls the switchText php function
// with the innerHTML of the div to update the text of the text box.
//============================================================

function addWord(id){
	var wordToAdd = document.getElementById(id).innerHTML;
	
	with(currObj);
	
	if (spellingSuggestionsDiv) {
		spellingSuggestionsDiv.parentNode.removeChild(spellingSuggestionsDiv);
		spellingSuggestionsDiv = null;
	}
	
	currObj.statusSpan.innerHTML = "Adding Word...";
	
	cp.call(currObj.config['spellUrl'], 'addWord', addWord_cb, wordToAdd);

};

//============================================================
// The addWord_cb function is a callback function that
// php's addWord function returns to.  It recieves the
// return status of the add to word to personal dictionary call.
// It hides the status item.
//============================================================

function addWord_cb(returnedData){
	alert(returnedData);
	with(currObj);
	currObj.statusSpan.innerHTML = "";
	resumeEditing();
	spellCheck();
}; //end addWord_cb function



//============================================================
// This function is called by the event listener when the user
// clicks on anything.  It is used to close the suggestion div
// if the user clicks anywhere that's not inside the suggestion
// div.  It just checks to see if the name of what the user clicks
// on is not "suggestions" then hides the div if it's not.
//============================================================

function checkClickLocation(e){
	if (spellingSuggestionsDiv) {
		

		if (spellingSuggestionsDiv.ignoreNextClick) {
			spellingSuggestionsDiv.ignoreNextClick = false;
		}
		else {
			var theTarget = getTarget(e);
			
			if (theTarget != spellingSuggestionsDiv){
				spellingSuggestionsDiv.parentNode.removeChild(spellingSuggestionsDiv);
				spellingSuggestionsDiv = null;
			}
		}
	}
	
	return true; 
};


//============================================================
// The get target function gets the correct target of the event.
// This function is required because IE handles the events in
// a different (wrong) manner than the rest of the browsers.
//============================================================

function getTarget(e){
	var value;
	if (checkBrowser() == "ie"){
		value = window.event.srcElement;
	}
	else{
		value = e.target;
	}
	return value;
}; //end getTarget function


//============================================================
// The checkBrowser function simply checks to see what browser
// the user is using and returns a string containing the browser
// type.
//
//============================================================

function checkBrowser(){
	var theAgent = navigator.userAgent.toLowerCase();
	if(theAgent.indexOf("msie") != -1){
		if(theAgent.indexOf("opera") != -1){
			return "opera";
		}
		else{
			return "ie";
		}
	}
	else if(theAgent.indexOf("netscape") != -1){
		return "netscape";
	}
	else if(theAgent.indexOf("firefox") != -1){
		return "firefox";
	}
	else if(theAgent.indexOf("mozilla/5.0") != -1){
		return "mozilla";
	}
	else if(theAgent.indexOf("\/") != -1){
		if (theAgent.substr(0,theAgent.indexOf('\/')) != 'mozilla'){
			return navigator.userAgent.substr(0,theAgent.indexOf('\/'));
		}
		else{
			return "netscape";
		} 
	}
	else if(theAgent.indexOf(' ') != -1){
		return navigator.userAgent.substr(0,theAgent.indexOf(' '));
	}
	else{ 
		return navigator.userAgent;
	}
};


//============================================================
// The showSuggestions_cb function is a callback function that
// php's showSuggestions function returns to.  It sets the 
// suggestions table to contain the new data and then displays
// the suggestions div.  It also clears the status message.
//============================================================

function showSuggestions_cb(new_data){
	with(currObj);
	spellingSuggestionsDiv.innerHTML = new_data;
	spellingSuggestionsDiv.style.display = 'block';
	currObj.statusSpan.innerHTML = "";
}; //end showSuggestions_cb function


//============================================================
// The showSuggestions function calls the showSuggestions php
// function to get suggestions for the misspelled word that the
// user has clicked on.  It sets the status to "Searching...",
// hides the suggestions div, finds the x and y position of the
// span containing the misspelled word that user clicked on so 
// the div can be displayed in the correct location, and then
// calls the showSuggestions php function with the misspelled word
// and the id of the span containing it.
//============================================================

function showSuggestions(word, id) {
	with(currObj);
	currObj.statusSpan.innerHTML = "Searching....";
	var x = findPosX(id);
	var y = findPosY(id);
	
	var scrollPos = 0;
	if (checkBrowser() != "ie") {
		scrollPos = currObj.spellingResultsDiv.scrollTop;
	}

	if (spellingSuggestionsDiv) {
		spellingSuggestionsDiv.parentNode.removeChild(spellingSuggestionsDiv);
	}
	spellingSuggestionsDiv = document.createElement('DIV');
	spellingSuggestionsDiv.style.display = "none";
	spellingSuggestionsDiv.className = 'suggestion_box';
	spellingSuggestionsDiv.style.position = 'absolute';
	spellingSuggestionsDiv.style.left = x + 'px';
	spellingSuggestionsDiv.style.top = (y+16-scrollPos) + 'px';
	spellingSuggestionsDiv.ignoreNextClick = true;
	document.body.appendChild(spellingSuggestionsDiv);
	cp.call(currObj.config['spellUrl'], 'showSuggestions', showSuggestions_cb, word, id);
};


//============================================================
// The replaceWord function takes the id of the misspelled word
// that the user clicked on and replaces the innerHTML of that
// span with the new word that the user selects from the suggestion
// div.  It hides the suggestions div and changes the color of
// the previously misspelled word to green to let the user know
// it has been changed.  It then calls the switchText php function
// with the innerHTML of the div to update the text of the text box.
//============================================================

function replaceWord(id, newWord){
	document.getElementById(id).innerHTML = trim(newWord);
	if (spellingSuggestionsDiv) {
		spellingSuggestionsDiv.parentNode.removeChild(spellingSuggestionsDiv);
		spellingSuggestionsDiv = null;
	}
	document.getElementById(id).style.color = "#005500";
}; //end replaceWord function


//============================================================
// The switchText function is a funtion is called when the user
// clicks on resume editing (or submits the form).  It calls the
// php function to switchText and uncomments the html and replaces
// breaks and everything.  Here all the breaks that the user has
// typed are replaced with %u2026.  Firefox does this goofy thing
// where it cleans up the display of your html, which adds in \n's
// where you don't want them.  So I replace the user-entered returns
// with something unique so that I can rip out all the breaks that
// the browser might add and we don't want.
//============================================================

function switchText() {
	with(currObj);
	var text = currObj.spellingResultsDiv.innerHTML;
	text = text.replace(/<br *\/?>/gi, "~~~");
	cp.call(currObj.config['spellUrl'], 'switchText', switchText_cb, text);
}; 


//============================================================
// The switchText_cb function is a call back funtion that the
// switchText php function returns to.  I replace all the %u2026's
// with returns.  It then replaces the text in the text box with 
// the corrected text fromt he div.
//============================================================

function switchText_cb(new_string) {
	with(currObj);
	new_string = new_string.replace(/~~~/gi, "\n");
	currObj.objToCheck.style.display = "none";
	currObj.objToCheck.value = new_string;
	currObj.objToCheck.disabled = false;
	if (currObj.spellingResultsDiv) {
		currObj.spellingResultsDiv.parentNode.removeChild(currObj.spellingResultsDiv);
		currObj.spellingResultsDiv = null;
	}
	currObj.objToCheck.style.display = "block";
	currObj.resetAction();
};


//============================================================
// The resumeEditing function is called when the user is in the
// correction mode and wants to return to the editing mode.  It
// hides the results div and the suggestions div, then enables
// the text box and unhides the text box.  It also calls
// resetAction() to reset the status message.
//============================================================

function resumeEditing() {
	with(currObj);
	currObj.actionSpan.innerHTML = "<a class=\"resume_editing\">Resume</a>";
	currObj.statusSpan.innerHTML = "Loading....";
	
	if (spellingSuggestionsDiv) {
		spellingSuggestionsDiv.parentNode.removeChild(spellingSuggestionsDiv);
		spellingSuggestionsDiv = null;
	}
	
	currObj.switchText();
}; 


//============================================================
// The resetAction function just resets the status message to
// the default action of "Check Spelling".
//============================================================

function resetAction() {
	with(currObj);
	
	currObj.actionSpan.innerHTML = "<a class=\"check_spelling\" onclick=\"setCurrentObject(" + currObj.config['varName'] + "); " + currObj.config['varName'] + ".spellCheck();\">Preview & SpellCheck</a>";
	currObj.statusSpan.innerHTML = "";
};


//============================================================
// The resetSpellChecker function resets the entire spell checker
// to the defaults.
//============================================================

function resetSpellChecker() {
	with(currObj);
	currObj.resetAction();
	
	currObj.objToCheck.value = "";
	currObj.objToCheck.style.display = "block";
	currObj.objToCheck.disabled = false;
	
	if (currObj.spellingResultsDiv) {
		currObj.spellingResultsDiv.parentNode.removeChild(currObj.spellingResultsDiv);
		currObj.spellingResultsDiv = null;
	}
	if (spellingSuggestionsDiv) {
		spellingSuggestionsDiv.parentNode.removeChild(spellingSuggestionsDiv);
		spellingSuggestionsDiv = null;
	}
	currObj.statusSpan.style.display = "none";
	
};


//============================================================
// The findPosX function just finds the X offset of the top left
// corner of the object it's given.
//============================================================

function findPosX(object){
	var curleft = 0;
	var obj = document.getElementById(object);
	if (obj.offsetParent){
		while (obj.offsetParent){
			curleft += obj.offsetLeft - obj.scrollLeft;
			obj = obj.offsetParent;
		}
	}
	else if (obj.x){
		curleft += obj.x;
	}
	return curleft;
}; 


//============================================================
// The findPosY function just finds the Y offset of the top left
// corner of the object it's given.
//============================================================

function findPosY(object){
	var curtop = 0;var curtop = 0;
	var obj = document.getElementById(object);
	if (obj.offsetParent){
		while (obj.offsetParent){
			curtop += obj.offsetTop - obj.scrollTop;
			obj = obj.offsetParent;
		}
	}
	else if (obj.y){
		curtop += obj.y;
	}
	return curtop;
}; //end findPosY function


//============================================================
// Trims white space from a string.
//============================================================

function trim(s) {
  while (s.substring(0,1) == ' ') {
    s = s.substring(1,s.length);
  }
  while (s.substring(s.length-1,s.length) == ' ') {
    s = s.substring(0,s.length-1);
  }
  return s;
};