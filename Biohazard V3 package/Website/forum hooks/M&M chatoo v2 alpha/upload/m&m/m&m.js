var bw = new get_browser();
var moh = new m2m_readme();

function get_browser()
{
	d          = document;
	var nav    = navigator;
	this.agt   = nav.userAgent.toLowerCase();
	this.major = parseInt(nav.appVersion);
	this.ns    = (d.layers);
	this.dom   = (d.getElementById) ? 1 : 0;
	this.ns4up = (this.ns && this.major >=4);
	this.ns6   = (this.agt.indexOf("Netscape6") != -1);
	this.op    = (window.opera ? 1 : 0);
	this.ie    = (d.all ? 1 : 0);
	this.ie4up = (this.ie && this.major >= 4);
	this.ie5   = (d.all && this.dom);
	this.gk    = (typeof(nav.product) != "undefine" && nav.product) ? 1 : 0;
	this.fb    = (this.agt.indexOf("firebird") != -1);
	this.fx    = (this.agt.indexOf("firefox") != -1);
	this.sf    = (this.agt.indexOf("safari")!=-1);
	this.win   = ((this.agt.indexOf("win") != -1) || (this.agt.indexOf("16bit") != -1));
	this.mac   = (this.agt.indexOf("mac") != -1);
}

function m2m_readme()
{
	this.id = 'moh';
	eval(this.id+"=this");
	this.sel   = m_select_all;
	this.cpy   = m_copy_it;
	this.gid   = m_get_id;
	this.go_to = m_goto;
}

function m_select_all(n)
{
	var id = this.gid('txt_'+n);
	if (!id)
	{
		return false;
	}

	id.focus();
	id.select();
	return false;
}

function m_copy_it(n)
{
	var id = this.gid('txt_'+n);
	if (!id)
	{
		return false;
	}

	if (!id.createTextRange)
	{
		alert('Your Browser Doesn\'t Support The Copy Command!');
		return false;
	}

	this.sel(n);
	cp = id.createTextRange();
	cp.execCommand('Copy');
}

function m_get_id(i)
{
	var id = null;

	if (document.getElementById)
	{
		id = document.getElementById(i);
	}
	else if (document.all)
	{
		id = document.all[i];
	}

	return id;
}

function m_goto(to)
{
	window.location = to;
}