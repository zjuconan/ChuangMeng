define(function(require, exports, module){
	//window.$ = require('jquery');
	//登录加载socket.io库
	//if(OP_CONFIG.userInfo){
	//	require.async('socket.io');
	//	require('chat');
	//	$.chat.init();
	//}
	var store=require('store');
	//计算字符串长度
	String.prototype.strLen = function() {
	    var len = 0;
	    for (var i = 0; i < this.length; i++) {
	        if (this.charCodeAt(i) > 255 || this.charCodeAt(i) < 0) len += 2; else len ++;
	    }
	    return len;
	}


	//将字符串拆成字符，并存到数组中
	String.prototype.strToChars = function(){
	    var chars = new Array();
	    for (var i = 0; i < this.length; i++){
	        chars[i] = [this.substr(i, 1), this.isCHS(i)];
	    }
	    String.prototype.charsArray = chars;
	    return chars;
	}

	//判断某个字符是否是汉字
	String.prototype.isCHS = function(i){
	    if (this.charCodeAt(i) > 255 || this.charCodeAt(i) < 0)
	        return true;
	    else
	        return false;
	}

	//截取字符串（从start字节到end字节）
	String.prototype.subCHString = function(start, end){
	    var len = 0;
	    var str = "";
	    this.strToChars();
	    for (var i = 0; i < this.length; i++) {
	        if(this.charsArray[i][1])
	            len += 2;
	        else
	            len++;
	        if (end < len)
	            return str;
	        else if (start < len)
	            str += this.charsArray[i][0];
	    }
	    return str;
	}

	//截取字符串（从start字节截取length个字节）

	String.prototype.subCHStr = function(start, length){
	    return this.subCHString(start, start + length);
	}
	
	if(OP_CONFIG.userInfo){
		require.async('socket.io');
		require.async('chat', function(){
            $.chat.init();
        });
	}
	//非学习页加载头部和回到顶部脚本
	function popLoginSns(){
		require.async('../../logic/login/login-regist', function(login){
			login.init();
		});
	}
	
	(OP_CONFIG.page=='code') && $('#J_GotoTop').hide()
	
	
	function backTop(){
		h = $(window).height();
		t = $(document).scrollTop();
		if(t >=768){
			$('#backTop').show();
		}else{
			$('#backTop').hide();
		}
	}
	//顶部用户导航
	/*		if($('#nav_list').is(":visible")){
			$(this).removeClass("hover")
			$('#nav_list').hide();			
		}else{
			$(this).addClass("hover")
			$('#nav_list').show();			
		}

		return false;*/
	$('[action-type="my_menu"],#nav_list').on('mouseenter',function(){
		$('[action-type="my_menu"]').addClass("hover")
		$('#nav_list').show()
	})
    $('[action-type="my_menu"],#nav_list').on('mouseleave',function(){
		$('[action-type="my_menu"]').removeClass("hover")
	$('#nav_list').hide()
	});
	$('#set_btn').click(function() { location.href='/space/course' });

	$('#J_Login').on('click',popLoginSns);

	//回到顶部
	$(document).ready(function(e) {
		backTop();
		$('#backTop').click(function(){
			$("html,body").animate({scrollTop:0},200);	
		})

	});

	//点击课程链接 清空原来存储选项
	$("#nav-item a:eq(0)").click(function(event) {
		store.clear()
	});

	$(window).scroll(function(e){
		backTop();		
	});

	!function(){
		var cookie,
			ua,
			match;
		ua=window.navigator.userAgent;
		match=/;\s*MSIE (\d+).*?;/.exec(ua);
		if(match&&+match[1]<9){
			cookie=document.cookie.match(/(?:^|;)\s*ic=(\d)/);
			if(cookie&&cookie[1]){
				return ;
			}
			$("body").prepend([
				"<div id='js-compatible' class='compatible-contianer'>",
					"<p class='cpt-ct'><i></i>您的浏览器版本过低。为保证最佳学习体验，<a href='/static/html/broswer.html'>请点此更新高版本浏览器</a></p>",
					"<div class='cpt-handle'><a href='javascript:;' class='cpt-agin'>以后再说</a><a href='javascript:;' class='cpt-close'><i></i></a>",
				"</div>"
			].join(""));
			$("#js-compatible .cpt-agin").click(function(){
				var d=new Date();
				d.setTime(d.getTime()+30*24*3600*1000);
				//d.setTime(d.getTime()+60*1000);
				document.cookie="ic=1; expires="+d.toGMTString()+"; path=/";
				$("#js-compatible").remove();
			});
			$("#js-compatible .cpt-close").click(function(){
				$("#js-compatible").remove();
			});
		}
	}();
});
