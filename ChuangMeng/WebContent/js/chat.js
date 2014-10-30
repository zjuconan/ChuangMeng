var wsuri = 'ws://' + window.location.host + '/ChuangMeng/chat';
var ws = null;
function startWebSocket() {
	if ('WebSocket' in window)
		ws = new WebSocket(wsuri);
	else if ('MozWebSocket' in window)
		ws = new MozWebSocket(wsuri);
	else
		alert("not support");

	ws.onmessage = function(evt) {
		var message='<li><a class="img_l" target="_blank" href="http://v.youmi.cn/profile/6884476"><img width="50" height="50" alt="" src="http://i2.umivi.net/avatar/76/6884476m.png"></a>'+
			'<div class="flo_l"><p class="ptitle"><a target="_blank" href="http://v.youmi.cn/profile/6884476">小吴888</a><i class="v1"></i>'+
			'<span class="launch">发起讨论</span></p><p>'+evt.data+'</p><p class="d_operat"><a class="reply" href="javascript:ReplyUtls.button(745031);" title="回复">回复(0)</a><span>2014-10-25 10:02:10</span></p></div></li>';
		$("#newlist_infos").prepend(message);
		var scrollbar5 = $("#scrollbar3").data("plugin_tinyscrollbar")
		scrollbar5.update();
	};

	ws.onclose = function(evt) {
		alert("close");
	};

	ws.onopen = function(evt) {
		alert("open");
	};
}

function sendMsg() {
	ws.send($("#des_txt").val());
}