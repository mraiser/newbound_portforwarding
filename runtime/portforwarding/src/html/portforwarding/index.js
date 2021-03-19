function sendPortforwarding(id,stream,sessionid,cb) {
	var params = '';
	if (id) params += 'id='+encodeURIComponent(id);
	if (stream) params += (params != '' ? '&' : '') + 'stream='+encodeURIComponent(stream);
	if (sessionid) params += (params != '' ? '&' : '') + 'sessionid='+encodeURIComponent(sessionid);
	json('portforwarding', params, cb);
}

function sendList(cb) {
	var params = '';
	json('list', params, cb);
}

function sendAddlocal(ipaddr,port,name,cb) {
	var params = '';
	if (ipaddr) params += 'ipaddr='+encodeURIComponent(ipaddr);
	if (port) params += (params != '' ? '&' : '') + 'port='+encodeURIComponent(port);
	if (name) params += (params != '' ? '&' : '') + 'name='+encodeURIComponent(name);
	json('addlocal', params, cb);
}

function sendDeletelocal(id,cb) {
	var params = '';
	if (id) params += 'id='+encodeURIComponent(id);
	json('deletelocal', params, cb);
}

function sendAddremote(peer,id,name,port,ipaddr,peername,cb) {
	var params = '';
	if (peer) params += 'peer='+encodeURIComponent(peer);
	if (id) params += (params != '' ? '&' : '') + 'id='+encodeURIComponent(id);
	if (name) params += (params != '' ? '&' : '') + 'name='+encodeURIComponent(name);
	if (port) params += (params != '' ? '&' : '') + 'port='+encodeURIComponent(port);
	if (ipaddr) params += (params != '' ? '&' : '') + 'ipaddr='+encodeURIComponent(ipaddr);
	if (peername) params += (params != '' ? '&' : '') + 'peername='+encodeURIComponent(peername);
	json('addremote', params, cb);
}

function sendDeleteremote(id,cb) {
	var params = '';
	if (id) params += 'id='+encodeURIComponent(id);
	json('deleteremote', params, cb);
}



function swapControls(n) {
  $('.topnavitem').removeClass('ui-btn-active');
  $('#topnav'+n).addClass('ui-btn-active');
  $('.controls').css('display', 'none');
  $('#controls'+n).css('display', 'block');
}

function getQueryParameter ( parameterName ) {
	  var queryString = window.top.location.search.substring(1);
	  var parameterName = parameterName + "=";
	  if ( queryString.length > 0 ) {
	    begin = queryString.indexOf ( parameterName );
	    if ( begin != -1 ) {
	      begin += parameterName.length;
	      end = queryString.indexOf ( "&" , begin );
	        if ( end == -1 ) {
	        end = queryString.lengthblock
	      }
	      return unescape ( queryString.substring ( begin, end ) );
	    }
	  }
	  return "null";
}

function addLocal() {
  var ipaddr = $('#localaddr').val();
  var port = $('#localport').val();
  var name = $('#localname').val();
  sendAddlocal(ipaddr,port,name,function(result){
    if (result.status == 'ok') {
      document.getElementById('locallist').data[result.key] = result.data;
      addLocalToList(result.data, result.key);
    }
    else alert(result.msg);
  });
}
               
function addLocalToList(local, key){
  var data = document.getElementById('locallist').data;
  if ($('#locals').html().indexOf('<i>') == 0) {
    var newhtml = '<ul id="ul_locallist" data-role="listview" data-inset="true" data-theme="c" data-divider-theme="a">'
        +'<li data-role="list-divider">Shared Endpoints</li></ul>';
    $('#locals').html(newhtml);
	$('#locals').trigger('create');
  }
  var newhtml = '<li id="lp_'+key+'">'
      +'<input type="button" value="delete" style="float: right;" onclick="deleteLocal(\''+key+'\');">'
//      +'<a id="a_'+key+'" class="li_locallist" onclick="alert(9);">'
      +'<h3 class="ui-li-heading">'+local.name+'</h3><p class="ui-li-desc">'+local.ipaddr+":"+local.port
      +'</p>'
//      +'</a>'
      +'</li>';
    $('#ul_locallist').append(newhtml);
    $('#ul_locallist').listview('refresh');
}    

function deleteLocal(id){
  sendDeletelocal(id,function(result){
    if (result.status == 'ok') populateLists();
    else alert(result.msg);
  });
}

function addRemoteToList(remote, key, onlist){
  if (document.getElementById('lp_'+key) == null) {
	if ($('#remotes').html().indexOf('<i>') == 0) {
	  var newhtml = '<ul id="ul_remotelist" data-role="listview" data-inset="true" data-theme="c" data-divider-theme="a">'
		  +'<li data-role="list-divider">Shared Endpoints</li></ul>';
	  $('#remotes').html(newhtml);
	  $('#remotes').trigger('create');
	}
	var button;
	if (remote.connected) button = '<input type="button" value="disconnect" style="float: right;" onclick="deleteRemote(\''+key+'\');">';
	else if (onlist) button = '<input type="button" value="remove" style="float: right;" onclick="deleteRemote(\''+key+'\');">';
	else button = '<input type="button" value="connect" style="float: right;" onclick="addRemote(\''+key+'\');">'
	var newhtml = '<li id="lp_'+key+'">'
		+ button
		+'<h3 class="ui-li-heading">'+remote.name+'</h3>'
		+'<p class="ui-li-desc">'+remote.peername+' ('+remote.peer+')<br>Local Port: <input type="text" value="'+remote.port+'" id="connect_'+key+'"'
		+ (remote.connected ? ' disabled><font color="green">LISTENING</font>' : '>')
		+'</p>'
		+'</li>';
    $('#ul_remotelist').append(newhtml);
    $('#ul_remotelist').listview('refresh');
    
    document.getElementById('lp_'+key).data = remote;
  }
}    

function populateLists(){
  sendList(function(result){
    if (result.status == 'ok') {
      document.getElementById('locallist').data = result.local;
      $('#locallist').html('<div id="locals"><i>none found</i></div>');
      var l=0;
      for (var item in result.local) {
        var rli = result.local[item];
        addLocalToList(rli, item);
        l++;
      }

      document.getElementById('remotelist').data = result.remote;
      $('#remotelist').html('<div id="remotes"><i>none found</i></div>');
      var l = 0;
      for (var item in result.remote) {
        var rri = result.remote[item];
        addRemoteToList(rri, item, true);
        l++;
      }
      
      $.getJSON('../peerbot/connections?callback=?', function(result){
        var data = result.data;
        console.log(data);
        for (var item in data){
          var rdi = result.data[item];
          if (rdi.connected) getRemotePortInfo(rdi);
        } 
      });
    }
    else alert(result.msg);
  });
}

function getRemotePortInfo(rdi) {
  $.getJSON('../peerbot/remote/'+rdi.id+'/portforwarding/list', function(result){
    for (var endpoint in result.local){
      var fwd = result.local[endpoint];
      fwd.peer = rdi.id;
      fwd.peername = rdi.name;
      fwd.ipaddr = rdi.addr;
      console.log(fwd);
      addRemoteToList(fwd, endpoint);
    }
  });
}

function addRemote(key) {
  var fwd = document.getElementById('lp_'+key).data;
  sendAddremote(fwd.peer,key,fwd.name,$('#connect_'+key).val(),fwd.ipaddr,fwd.peername,function(result) {
    if (result.status != 'ok') alert(result.msg);
    else populateLists();
  });
}

function deleteRemote(id){
  sendDeleteremote(id, function(result){
    populateLists();
  });
}

$(document).on( 'pagecreate', function() {
  if (getQueryParameter('header') == 'false') $('#headertitle').css('display', 'none');
  swapControls(0);
  populateLists();
  buildConnectionBar('connections', populateLists);
});
