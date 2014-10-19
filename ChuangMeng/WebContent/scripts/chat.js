/*=============================================================================
#     FileName: chat.js
#         Desc: 消息与后端通讯的chat对象，已修改成jquery对象
#       Author: jiangsf
#   LastUpdate: 2013-10-18 11:48:39
=============================================================================*/
define(function(require, exports, module){
	var $ = require('jquery');
	require('socket.io');
	$.chat = {};
	$.chat = {
		iosocket:null,
		unreadEvent:null,
		uid:0,
		port : 80,
		events : {},	//绑定的事件

		//初始化socket
		init:function() {
			if (this.port == 80) {
				this.iosocket = io.connect('http://chat.mukewang.com');
			} else {
				this.iosocket = io.connect('http://chat.mukewang.com:'+this.port);
			}
			for (var event in this.events) {
				this.iosocket.on(event, this.events[event]);
			}
			this.checkUnreads();
		},

		//用户登录
		login:function(uinfo) {
			this.iosocket.emit('login', uinfo);
		},

		//绑定未读消息事件
		bandUnreads:function(uid, cb) {
			if (uid && cb) {
				this.uid = uid;
			}
			this.events['unreads'] = cb;
			if (this.iosocket) {
				this.iosocket.on('unreads', cb);
			}
			//this.checkUnreads();
		},

		//检查未读消息总数
		checkUnreads:function() {
			//发送获取未读消息总数指令
			this.iosocket.emit('unreads', this.uid);
		},

		//绑定服务端响应事件
		bindEvent:function(type, cb) {
			if (this.iosocket) {
				this.iosocket.on(type, cb);
			} else {
				this.events[type] = cb;
			}
		},

		//发送指令到服务端
		send:function(type, msg) {
			this.iosocket.emit(type, msg);
		}
	}
	//绑定未读消息事件
	if (OP_CONFIG.userInfo && OP_CONFIG.userInfo.uid) {
		$.chat.bandUnreads(OP_CONFIG.userInfo.uid, function(total){
			if (total > 0) {
				$('.msg_icon').show().html(total);
				$('#msg_new a').html('<span class="unread_num">'+total+'</span>');
			} else {
				$('.msg_icon').hide();
				$('#msg_new a').empty();
				
			}
		});
	}
});
