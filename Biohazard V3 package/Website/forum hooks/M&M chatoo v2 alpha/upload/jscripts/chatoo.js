// M&M Shoutbox ajax functions
// author: mohamed
// version: 1.0
// http://www.invisioneyes.com
// please let the author know if you put any of this to use
// M&M ShoutBox (including this script) is published under a creative commons license for ipb
// license: http://creativecommons.org/licenses/by-nc-sa/2.0/
// Feel free to use this ajax function in any script but notify me of it and give credits 


//===========================Advantage of Ajax===================================================
//        Ajax isn't the ability to refresh content in a page as javascript did that long ago
// but ajax have ability to use server to run functions in afile and server response from this file 
// is represented to the page ........javascript can't use server to do that it 
// instead use direct call for files which consume lot of time
// so ajax is an enhancement for time consumed in content changing 
// in other word better reacte between user and page
//================================================================================================


//============================Disadvantage of Ajax==================================================
//       we must not go far in using ajax to an extend of updating whole page and loosing lot of 
// page identities in one page  why??!!!!!
// simply cause keep in mind two problems first :some people will want to bookmark certain page 
// how can they do that if all pages are in same page loaded without identity and won't be there next 
// timeor needs steps to get to it
// second is what if you wanted to go back to previous page how can you do that 
// you can't undo content updated by aJAX.. 
// so ajax is good aslong as we didn't reache this part of messing with this two problems 
// and causing whole page lose of identity.
//=====================================================================================================


var httpReceiveChat = getHTTPObject();
var httpSendChat = getHTTPObject();
var httpDeleteChat = getHTTPObject();
var DeleteChaturl = "index.php?act=chatoo&do=mod";
var GetChaturl = "index.php?act=chatoo&do=get";
var SendChaturl = "index.php?act=chatoo&do=add";
var lastID = -1;
var inS;

window.onload = initJavaScript;

function initJavaScript() {
	document.forms['chatForm'].elements['postcontentt'].setAttribute('autocomplete','off');
	checkStatus('');
	receiveChat();
    setupSpellCheckers();
	}

function receiveChat() {

	if (httpReceiveChat.readyState == 4 || httpReceiveChat.readyState == 0) {
  	httpReceiveChat.open("GET",GetChaturl + '&lastID=' + lastID , true);
    httpReceiveChat.onreadystatechange = handleHttpReceiveChat; 
  	httpReceiveChat.send(null);
	}
}

function handleHttpReceiveChat() {
  if (httpReceiveChat.readyState == 4) {
    results = httpReceiveChat.responseText.split('---'); 
    if (results.length > 4) {
	    for(i=0;i < (results.length-3);i=i+5) { 
	    	insertNewContent(results[i+4],results[i],results[i+2],results[i+1],results[i+3]);
			
	    }
	    lastID = results[results.length-6];
    }
	
    setTimeout('receiveChat();',4000); 
 
  }
}

function shoutdel(id)
{

	del_shout = "Are you sure you want to delete this Shout?";
		if ( id && confirm(del_shout) && (httpDeleteChat.readyState == 4 || httpDeleteChat.readyState == 0)  )
		{
		param = '&id='+ id;	
		httpDeleteChat.open("POST", DeleteChaturl , true);
		httpDeleteChat.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
  	    httpDeleteChat.onreadystatechange = handleHttpDeleteChat;
  	    httpDeleteChat.send(param);
	       
		}
         else if(id)
		{
setTimeout('shoutdel();',1000);
	    }	
}
function handleHttpDeleteChat() {
  if (httpDeleteChat.readyState == 4) {
  	receiveChat();
  }
}

function insertNewContent(mid,liID,liName,liTime,liText) {
if((able == 1 || acp == 1 || (his == 1 && memid == mid)) && checkBrowsers() == "ie" )
	{ 
	inS = '<a  name="delete"  onclick="shoutdel('+ liID +');"  ><img height="17" src="uploads/chatoo/104.gif"  /></a>';
	}
	else if(able == 1 || acp == 1 || (his == 1 && memid == mid))
	{
	inS = '<a  name="delete"  onclick="shoutdel('+ liID +');"  ><img src="uploads/chatoo/104.png"  /></a>';
	}
	else
	{
	inS = "";
	}

	insertO = document.getElementById('outputList');
	oLi = document.createElement('li');
	oSpan = document.createElement('span');
	oSpan.setAttribute('className','name');
	oSpan.setAttribute('class','name');
	oSpan.innerHTML += liName+'['+ liTime +']: ';
	oLi.appendChild(oSpan);
	oLi.innerHTML += liText +' '+ inS;
	insertO.insertBefore(oLi, insertO.firstChild);
 
}

function sendshout() {
	currentChatText = document.forms['chatForm'].elements['postcontentt'].value;
	if (currentChatText != '' && (httpSendChat.readyState == 4 || httpSendChat.readyState == 0)) {
		param = '&c='+ currentChatText;	
		httpSendChat.open("POST", SendChaturl, true);
		httpSendChat.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
  	httpSendChat.onreadystatechange = handleHttpSendChat;
  	httpSendChat.send(param);

  	document.forms['chatForm'].elements['postcontentt'].value = '';
	}
else if (currentChatText != '') {
		setTimeout('sendshout();',1000);
	}
}

function handleHttpSendChat() {
  if (httpSendChat.readyState == 4) {
  	receiveChat();
  }
}


function checkStatus(focusState) {
	if (checkBrowsers() != "ie"){
	currentChatText = document.forms['chatForm'].elements['postcontentt'];
	oSubmit = document.forms['chatForm'].elements['submit'];
	if (currentChatText.value != '' || focusState == 'active') {
		oSubmit.disabled = false;
	} else {
		oSubmit.disabled = true;
	}
	}
}
function checkBrowsers(){
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
} 

function getHTTPObject() {
  var xmlhttp = false;
if (window.XMLHttpRequest){
xmlhttp = new XMLHttpRequest();
}
else if (window.ActiveXObject){
try{
xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
} 
catch (e){
try{
xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
}
catch (e){}
}
}

  return xmlhttp;
}