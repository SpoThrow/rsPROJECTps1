//--------------------------------------------
// Set up our simple tag open values
//--------------------------------------------

var B_open = 0;
var I_open = 0;
var U_open = 0;
var QUOTE_open = 0;
var CODE_open = 0;
var SQL_open = 0;
var HTML_open = 0;

var bbtags   = new Array();

var fombjj    = document.chatForm;

//==========================================
// prep and set up
//==========================================




//==========================================
// Set the help bar status
//==========================================

function hstat(msg)
{
	document.chatForm.helpbox.value = eval( "help_" + msg );
}

//==========================================
// Set the number of tags open box
//==========================================

function cstat()
{
	var c = stacksize(bbtags);
	
	if ( (c < 1) || (c == null) ) {
		c = 0;
	}
	
	if ( ! bbtags[0] ) {
		c = 0;
	}
	
	document.chatForm.tagcount.value = c;
}


//==========================================
// Close all tags
//==========================================

function closealll()
{
	if (bbtags[0])
	{
		while (bbtags[0])
		{
			tagRemove = popstack(bbtags)
			document.chatForm.postcontentt.value += "[/" + tagRemove + "]";
			
			//--------------------------------------------
			// Change the button status
			// Ensure we're not looking for FONT, SIZE or COLOR as these
			// buttons don't exist, they are select lists instead.
			//--------------------------------------------
			
			if ( (tagRemove != 'FONT') && (tagRemove != 'SIZE') && (tagRemove != 'COLOR') )
			{
				eval("document.chatForm." + tagRemove + ".value = ' " + tagRemove + " '");
				eval(tagRemove + "_open = 0");
			}
		}
	}
	
	//--------------------------------------------
	// Ensure we got them all
	//--------------------------------------------
	
	document.chatForm.tagcount.value = 0;
	bbtags = new Array();
}

//==========================================
// EMOTICONS
//==========================================

function emoticon(theSmilie)
{
	doInsertt(" " + theSmilie + " ", "", false);
}

//==========================================
// ADD CODE
//==========================================

function add_codee(NewCode)
{
    document.chatForm.postcontentt.value += NewCode;
    document.chatForm.postcontentt.focus();
}

//==========================================
// ALTER FONT
//==========================================

function alterfontt(theval, thetag)
{
    if (theval == 0)
    	return;
	
	if(doInsertt("[" + thetag + "=" + theval + "]", "[/" + thetag + "]", true))
		pushstack(bbtags, thetag);
	
    document.chatForm.ffont.selectedIndex  = 0;
    document.chatForm.fsize.selectedIndex  = 0;
    document.chatForm.fcolor.selectedIndex = 0;
    
    cstat();
	
}


//==========================================
// SIMPLE TAGS (such as B, I U, etc)
//==========================================

function simpletagg(thetag)
{
	var tagOpen = eval(thetag + "_open");
	
		if (tagOpen == 0)
		{
			if(doInsertt("[" + thetag + "]", "[/" + thetag + "]", true))
			{
				eval(thetag + "_open = 1");
				
				//--------------------------------------------
				// Change the button status
				//--------------------------------------------
				
				eval("document.chatForm." + thetag + ".value += '*'");
		
				pushstack(bbtags, thetag);
				cstat();
				hstat('click_close');
			}
		}
		else
		{
			//--------------------------------------------
			// Find the last occurance of the opened tag
			//--------------------------------------------
			lastindex = 0;
			
			for (i = 0 ; i < bbtags.length; i++ )
			{
				if ( bbtags[i] == thetag )
				{
					lastindex = i;
				}
			}
			
			//--------------------------------------------
			// Close all tags opened up to that tag was opened
			//--------------------------------------------
			
			while (bbtags[lastindex])
			{
				tagRemove = popstack(bbtags);
				doInsertt("[/" + tagRemove + "]", "", false)
				
				//--------------------------------------------
				// Change the button status
				//--------------------------------------------
				
				if ( (tagRemove != 'FONT') && (tagRemove != 'SIZE') && (tagRemove != 'COLOR') )
				{
					eval("document.chatForm." + tagRemove + ".value = ' " + tagRemove + " '");
					eval(tagRemove + "_open = 0");
				}
			}
			
			cstat();
		}
}

//==========================================
// List tag
//==========================================

function tag_listt()
{
	var listvalue = "init";
	var thelist = "";
	
	while ( (listvalue != "") && (listvalue != null) )
	{
		listvalue = prompt(list_prompt, "");
		if ( (listvalue != "") && (listvalue != null) )
		{
			thelist = thelist+"[*]"+listvalue+"\n";
		}
	}
	
	if ( thelist != "" )
	{
		doInsertt( "[LIST]\n" + thelist + "[/LIST]\n", "", false);
	}
}

//==========================================
// URL tag
//==========================================

function tag_urll()
{
    var FoundErrors = '';
    var enterURL   = prompt(text_enter_url, "http://");
    var enterTITLE = prompt(text_enter_url_name, jsfile_myweb_lang);

    if (!enterURL) {
        FoundErrors += " " + error_no_url;
    }
    if (!enterTITLE) {
        FoundErrors += " " + error_no_title;
    }

    if (FoundErrors) {
        alert(jsfile_error_lang + FoundErrors);
        return;
    }

	doInsertt("[URL="+enterURL+"]"+enterTITLE+"[/URL]", "", false);
}

//==========================================
// Insert attachment tag
//==========================================

function insert_attach_to_textarea(aid)
{
	doInsertt( "[attachmentid="+aid+"]" );
}

//==========================================
// Image tag
//==========================================

function tag_imagee()
{
    var FoundErrors = '';
    var enterURL   = prompt(text_enter_image, "http://");

    if (!enterURL) {
        FoundErrors += " " + error_no_url;
    }

    if (FoundErrors) {
        alert(jsfile_error_lang + FoundErrors);
        return;
    }

	doInsertt("[IMG]"+enterURL+"[/IMG]", "", false);
}

function tag_emaill()
{
    var emailAddress = prompt(text_enter_email, "");

    if (!emailAddress) { 
		alert(error_no_email); 
		return; 
	}

	doInsertt("[EMAIL]"+emailAddress+"[/EMAIL]", "", false);
}

//--------------------------------------------
// GENERAL INSERT FUNCTION
//--------------------------------------------
// ibTag: opening tag
// ibClsTag: closing tag, used if we have selected text
// isSingle: true if we do not close the tag right now
// return value: true if the tag needs to be closed later

//

function doInsertt(ibTag, ibClsTag, isSingle)
{
	var isClose = false;
	var obj_tal = document.chatForm.postcontentt;
	
	//----------------------------------------
	// It's IE!
	//----------------------------------------
	if ( (ua_vers >= 4) && is_ie && is_win)
	{

	document.chatForm.postcontentt.focus();
			var sel = document.selection;
			var rng = sel.createRange();
			rng.colapse;
			if((sel.type == "Text" || sel.type == "None") && rng != null)
			{
				if(ibClsTag != "" && rng.text.length > 0)
					ibTag += rng.text + ibClsTag;
				else if(isSingle)
					isClose = true;
	
				rng.text = ibTag;
			}
		
	}
	//----------------------------------------
	// It's MOZZY!
	//----------------------------------------
	
	else if ( document.chatForm.postcontentt.selectionEnd )
	{ 
		var ss = document.chatForm.postcontentt.selectionStart;
		var st = document.chatForm.postcontentt.scrollTop;
		var es = document.chatForm.postcontentt.selectionEnd;
		
		if (es <= 2)
		{
			es = document.chatForm.postcontentt.textLength;
		}
		
		var start  = (document.chatForm.postcontentt.value).substring(0, ss);
		var middle = (document.chatForm.postcontentt.value).substring(ss, es);
		var end    = (document.chatForm.postcontentt.value).substring(es, document.chatForm.postcontentt.textLength);
		
		//-----------------------------------
		// text range?
		//-----------------------------------
		
		if (document.chatForm.postcontentt.selectionEnd - document.chatForm.postcontentt.selectionStart > 0)
		{
			middle = ibTag + middle + ibClsTag;
		}
		else
		{
			middle = ibTag + middle;
			
			if (isSingle)
			{
				isClose = true;
			}
		}
		
		document.chatForm.postcontentt.value = start + middle + end;
		
		var cpos = ss + (middle.length);
		
		document.chatForm.postcontentt.selectionStart = cpos;
		document.chatForm.postcontentt.selectionEnd   = cpos;
		document.chatForm.postcontentt.scrollTop      = st;


	}
	//----------------------------------------
	// It's CRAPPY!
	//----------------------------------------
	else
	{
		if (isSingle)
		{
			isClose = true;
		}
		
		document.chatForm.postcontentt.value += ibTag;
	}
	


	return isClose;
}	
