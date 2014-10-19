define(function(require, exports, module){
	require('common');
	require('/static/lib/layer/1.6.0/layer.min.js');
	require('/static/lib/layer/1.6.0/skin/layer.css');
	require('../../../lib/jquery/plugin/jquery.scrollbar');
	require('../../../css/jquery.scrollbar.css');
	require('/static/page/course/common/course_collection.js');	
	require('/static/component/base/placeholder/placeholder.js');
	require('jwplayer');


	if(!OP_CONFIG.userInfo){//判断当前用户是否登
		$(document).on("shown",function(e){
    		var $target;
    		if(($target=$(e.target)).hasClass("rl-modal")){
    			$target.find("[data-dismiss]").remove().end().find("a:contains('忘记密码')").remove();
    			$(".modal-backdrop").off("click");
    		}
    	});

		if(typeof thePlayer=="object") thePlayer.play(false);					
	    require.async('login_sns', function(login){
	        login.init();
	    });
    }

		//显示WIKI
		// (OP_CONFIG.userInfo.usertype>1) &&$('#create_wiki').show()
		 
		$('.detaillist').perfectScrollbar({
			wheelSpeed: 20,
			wheelPropagation: false
		});	
		$('.chaptername').on('click',function(){
			var $height = $(document).height();
		  	$('.chaptername').css('background','#1f2426');
		  	$('.chaptername').find('span').removeClass('borderstyle'); 
		  	if( $("#sectionlist").hasClass('hide') ) { 
		      	$("#sectionlist").removeClass('hide'); 
		      	//判断当前正在学习的元素距离父容器的高度，如果超过则scrollTop到一定的位置
		      	var listHeight = $("#sectionlist b").parent('a').position().top;
				if(listHeight >= 450){
					$('.detaillist').scrollTop(listHeight);
					$('.detaillist').perfectScrollbar('update');//同步滚动条插件的scrollTop值
				}
			 	$('#video_mark').css('height',$height);
		      	$('#video_mark').fadeIn();
				if(typeof thePlayer!="undefined"){
		     		if(thePlayer.getState() == 'PLAYING'){
			   	 		thePlayer.pause();
		      		}	
				}
	    	}    
		});	
	$('#video_mark').click(function(){ 
		$('#sectionlist').addClass('hide');
		$('#video_mark').fadeOut();
		$('.chaptername').css('background','#2a2c2e');
		$('.chaptername').find('span').addClass('borderstyle');
		if(thePlayer&&thePlayer.getState()=="PAUSED"){
			thePlayer.play();
		}
	});
	
	//ListData
	
	var GetListData={
			mate:require("/static/page/course/common/ajax-otherscode-list.js")({
				container:$('#mateLoadListData'),
				params:{ 
					mid:pageInfo.mid 
				}
			}),
			qa:require("/static/page/course/common/ajax-discuss-list.js")({
				container:$('#qaLoadListData'),
				params:{ 
					order:"last",
					mid:pageInfo.mid 
				}
			}),
			note:require("/static/page/course/common/ajax-note-list.js")({
				container:$('#noteLoadListData'),
				params:{
					mid:pageInfo.mid
				},
				def:{ //template compalie pre define
					"media_id":pageInfo.mid,
					mediaType:'video' //required in diffrent page; 'video','code','ceping'
				}
			}),
			wiki:require("/static/page/course/common/ajax-wiki-list.js")({
				container:$('#wikiLoadListData'),
				params:{
					mid:pageInfo.mid
				}
			})	
		}
	exports.tabList=GetListData;		
	//window.getListData=GetListData;			
	GetListData.qa.load();			
	//tab切换
	$('.course-menu a').one("click",function(e){
		//data loader
		var $this=$(this),id;
	 	//(typeof shot !='undefined')&&shot.reset()
		id=$(this).attr('id');
		id=id.substring(0,id.length-4);//sub the 'Menu' 4 char;
		GetListData[id]&&GetListData[id].load(); 
    }).on("click",function(e){
    	
    	var $this=$(this),
    		id;
    	e.preventDefault();
    	//(typeof shot !='undefined')&&shot.reset();
    	if($this.attr('id')=='wikiMenu'&&OP_CONFIG.userInfo.usertype>1) {//toggle create wiki button
		 	$('#create_wiki').show()
		}
		else{
			$('#create_wiki').hide()
	 	}
	 	if($this.hasClass("active")) return ;

	 	$this.parent().siblings().find(".active").removeClass("active");
	 	$this.addClass("active");
	 	id=$this.attr("id");
	 	id=id.substring(0,id.length-4);//sub the 'Menu' 4 char; 
	 	$("#"+id+"-content").siblings(".tab-con").hide().end().show();
    });
	

	
	/*$(document).on('click',function(e){
		var $target = $(e.target);
		switch( $target.attr('class')){
			case 'btn-close':
				shot.reset();
			break;
		}
	})*/

	//placeholder rewrite
	if(!("placeholder" in document.createElement("input"))){
		$(document).on("focus",".js-placeholder",function(){
			var $this=$(this);
			if($this.val()==$this.attr("placeholder")){
				$this.val("").removeClass("placeholder");
			}
		})
		.on("blur",".js-placeholder",function(){
			var $this=$(this);
			if(!$this.val().length&&$this.attr("placeholder")){
				$this.addClass("placeholder").val($this.attr("placeholder"));
			}
		});
	}
	//qa rich text editor
	UE.getEditor("discuss-editor",{initialFrameHeight:80,autoFloatEnabled:false,autoClearinitialContent:true,initialStyle:'p{line-height:1.5em;font-size:13px;color:#444}'});
	
	/*//焦点框
	var textAreaDis = $('.discussInput textarea'),
	   defaultTxtDis = textAreaDis.val();
	$('.discussInput').delegate('textarea','focus',
	function(){
		$(this).css('color','#363d40');
		if(textAreaDis.val() == defaultTxtDis){
			textAreaDis.val('');	
		}
	});
	$('.discussInput').delegate('textarea','blur',
	function(){
		if(textAreaDis.val() == ''){
			textAreaDis.val(defaultTxtDis);
			textAreaDis.css('color','#d0d6d9');
		}
   });
   var textAreaNote = $('.noteInput textarea'),
	   defaultTxtNote = textAreaNote.val();
	$('.noteInput').delegate('textarea','focus',
	function(){
		$(this).css('color','#363d40');
		if(textAreaNote.val() == defaultTxtNote){
			textAreaNote.val('');	
		}
	});
	$('.noteInput').delegate('textarea','blur',
	function(){ 								
		if(textAreaNote.val() == ''){
			textAreaNote.val(defaultTxtNote);
			textAreaNote.css('color','#d0d6d9');
		}
  	});*/

	//发讨论	
	
	var postData=window.postData={
		mid:pageInfo.mid,  
		picture_url:'',
		picture_time:0
	}
	
	function Store(name){
		this.name=name;
		this.data=null;
	}	
	Store.prototype={
		reset:function(){
			this.data=null;
		},
		set:function(key,data){
			if(data===undefined){
				this.data=key;
			}
			else{
				this.data=this.data||{};
				this.data[key]=data;
			}
		},
		prev:function(data){
			$.extend(data,this.data);
		},
		extendMethod:function(name,fun){
			if(!this.name||typeof this[name]!="function") return ;
			this["_"+name]=this[name];
			this[name]=function(){
				this["_"+name].call(this);
				fun.call(this);
			}
		},
		success:$.noop
	}

	var remote={
		qa:new Store("qa"),
		note:new Store("note")
	}
	exports.remote=remote;
   	$('#js-discuss-submit').on('click',function(){
	   	var $v,
	   		c,
	   		content,
	   		txt,
	   		txtLength,
	   		data={},
	   		$this=$(this);

		if($this.hasClass("submit-loading")) return;
		content=UE.getEditor("discuss-editor").getContent();
		content=$.trim(content);
		txt=$.trim(UE.getEditor("discuss-editor").getContentTxt());
		txtLength=txt.length;
	  	if(txtLength==0||txt=="请输入讨论内容..."){
		  	layer.msg('请输入讨论内容', 2, -1);
		  	return;
		}
	  	if(txtLength < 3){
		   	layer.msg('输入不能小于3个字符', 2, -1);
			return;
		}
	  	if(content.length >15000){
	   		layer.msg('输入不能超过15000个字', 2, -1);
	   		return;
	  	}
	  	if(($v=$("#js-discuss-btm .verify-code")).length){
	  		c=$v.find("input").val();
	  		if(c.length==0){
	  			layer.msg("请输入验证码",2,-1);
	  			return ;
	  		}
	  		if(c.length!=4){
	  			layer.msg("请输入正确的验证码",2,-1);
	  			return ;
	  		}
	  		data.verify_code=c;
	  	}
	  	$.extend(data,postData);
	  	data.content=content;
		remote.qa.prev(data);
		$this.addClass("submit-loading").val("正在发布...");
		$.ajax({
			url:"/course/ajaxsaveques2",
			data:data,
			type:"post",
			dataType:"json",
			success:function(data){
				if(data.result==0){
				  	layer.msg('发布成功!', 2, -1);
				  	GetListData.qa.load();
			    	UE.getEditor("discuss-editor").setContent("");
			    	remote.qa.reset();
			    	$("#js-discuss-btm .verify-code").remove();
				}
				else if(data.result==-103001){
					//verify code;
					if($("#js-discuss-btm .verify-code").length) return ;
					$("#js-discuss-btm").append([
		                '<div class="verify-code l">',
		                    '<input type="text" maxlength="4" class="verify-code-ipt">',
		                    '<img src="/wenda/getverifycode?',Math.random(),'" >',
		                    '<span class="verify-code-around">看不清换一换</span>',
		                '</div>'
		            ].join(""));

				}
				else{
					layer.msg(data.msg, 2, -1);
				}
			},
			complete:function(){
				$this.removeClass("submit-loading").val("发布");
			}
		})

	});
	   	   
   //发笔记
   
    $('#js-note-submit').on('click',function(){
    	var $this=$(this),
    		data={};
    	if($this.hasClass("submit-loading")) return ;
    	data.content=$.trim($('#js-note-textarea').val());
    	if(!data.content.length||data.content== $("#js-note-textarea").attr("placeholder")){
	 		layer.msg('请输入内容', 2, -1);
	  		return;
	  	}
	  	if(data.content.length>0 && data.content.length < 3){
			layer.msg('输入不能小于3个字符', 2, -1);
			return;
		}
		if(data.content.length >300){
			layer.msg('输入不能超过300个字', 2, -1);
			return;
		}
    	$.extend(data,postData);
    	remote.note.prev(data);
		data.is_shared=$('#js-isshare').is(':checked')?1:0; //是否分享
		$this.addClass("submit-loading");
		$.ajax({
			url:"/course/addnote",
			type:"post",
			dataType:"json",
			data:data,
			success:function(data){
				$this.removeClass("submit-loading");
				if(data.result==0){
			   		layer.msg('发布成功', 2, -1);
			 		GetListData.note.load();
			  		/*if(typeof(shot)!="undefined"){
				  		shot.reset();
				  	}
				  	if(typeof(window.codeData)!="undefined"){
				    	window.codeData.reSet("#NotePublist .J_ShotBtn");
				   	}*/
			 		$('#js-note-text-counter').find('em').text(0);
			 		$('#js-note-textarea').val("").blur(); //blur to trigger fake placeholder
	        	}	
			 	else{
				  	layer.msg(data.msg, 2, -1);
				}
				remote.note.success(data);
				remote.note.reset();
			}
		});
	});
	
	$("#js-note-textarea").on("keyup change",function(){
		$('#js-note-text-counter').find('em').text($.trim($(this).val()).length);//how to handle space?
	});
   	
	//截图动作	
   	$('.shot-btn').on('click',function(){
	   shot.screenShot()
	})	
	
	
	
	/*换一换同学*/
  
	  var getUser=function(){
		  $.post('/course/classmates', {cid:course_id, total:6}, function(data){
			$('.users').children().remove();
			$(data).each(function(i, user) {
				$('<li><a href="/space/u/uid/'+user.uid+'" target="_blank"><img src="'+user.portrait+'" /></a><h3><a href="/space/u/uid/'+user.uid+'" target="_blank">'+user.nickname+'</a></h3><em>'+user.job_title+'</em></li>').appendTo($('.users'));
			});
		   });
		  }
		
		getUser()
	$('.changeUser').click(function(){
		getUser()
		
	});
	

	
	/*//表单验证 字符检测
	
	   var numLimit=function(textarea,number){
		   
			 this.textarea=textarea;
			 this.number =number;
			 //this.minLen = this.textarea.attr('minlength');
			 this.maxLen =this.textarea.attr('maxlength');
			 this.init();
			 this.zero();
		   }
		   numLimit.prototype={
			   init:function(){
				   var _this=this;
				   _this.number.text(_this.textarea.val().length);
				   this.textarea.on('keyup change',function(){  
					   if(_this.textarea.val().length>_this.maxLen){
						  _this.textarea.val(_this.textarea.val().substring(0,_this.maxLen)); 
						  
						   } 
						else{
							_this.number.text(_this.textarea.val().length)
							
							}
				   })
				   
				   },
				zero:function(){
					var _this=this;
				    _this.number.text(0);
					}
				
			   }
	
	  $('textarea').each(function(index, element) {
		  new numLimit($(element),$(element).parent().find('em'));
      });*/
			
	$("#js-note-textarea").on("keyup change",function(){
		var $this=$(this);
		$("#js-note-text-counter em").text($.trim($this.val()).length);
	});
	//下载统计
	$('.downlist a').click(function(){
		var id=$(this).attr("data-id");
		$.ajax({
			url:"/course/ajaxdownloadlog",
			type:"post",
			data:{
				id:id
			}
		})
	})

	//源代码下载
	$('.coursedownload').mouseover(function(){
		$('.downlist').show();
		$('.coursedownload h2').find('i').addClass('activeicon');
	})
	$('.coursedownload').mouseout(function(){
		$('.downlist').hide();
		$('.coursedownload h2').find('i').removeClass('activeicon');
	})
		
	
	//verify code
    $(document).on("click",".verify-code-around",function(){
        var $img=$(this).prev("img");
        $img.attr("src",$img.attr("src").replace(/\?\S*/,"?"+Math.random()));
    });
	//吐槽
	if(!OP_CONFIG.userInfo) {return;}//如果没登录
	$("#judgeCont").placeholder();
	var pauseTime,
		placeValue=$("#judgeCont").attr("placeholder");
	$("#judgeBtn").click(function() {
		$("#judgeSubject").css({'display':'block'}).siblings("#successNote").css({'display':'none'});
		if(typeof(thePlayer)!='undefined') {
			if(thePlayer.getState()=="PLAYING") {//播放中 
				thePlayer.pause();
				pauseTime = parseInt(thePlayer.getPosition());
			} else{
				pauseTime = "0";
			}
		} else{
			pauseTime = "0";
		}
		$("#judgeTip").html("");
		$("#judgeWrap").show();
	})
	$("#cancelJudge,#closeJudge").click(function() {
		if(typeof(thePlayer)!='undefined') {
			if(thePlayer.getState()=="PAUSED") {//停止 
				thePlayer.play();
			}
		}
		$("#judgeCont").val("");
		$("#judgeWrap").hide();
	})

	$('#judgeCont').focus(function() {
		$("#judgeTip").html("");
	})
	$("#sendJudge").click(function() {
		var judgeCont = $.trim($("#judgeCont").val()),
			len = judgeCont.length,
			parameters = {};
		if(len<=0) {
			$("#judgeTip").html("评价不能为空!");
			$("#judgeCont").val('');
		}
		parameters.content = judgeCont;//反馈内容
		parameters.mid = pageInfo.mid;//提交的章节
		parameters.stop_time = pauseTime;//停止时间
		$.ajax({
			url      : '/course/ajaxmediacontent',
			data     : parameters,
			type     : "POST",
			dataType : 'json',
			success  : function (data){
				//console.dir(data)
				if(data.result== '0'){
					$("#successNote").css({'display':'block'}).siblings("#judgeSubject").css({'display':'none'});
					$("#judgeCont").attr("placeholder", placeValue).val("").focus().blur();
					$('#judgeWrap').fadeOut(1600);
					if(typeof(thePlayer)!='undefined') {
						if(thePlayer.getState()=="PAUSED") {//停止 
							thePlayer.play();
						}
					}
				}
			},
			error: function(data){
				$("#judgeTip").html("请求失败!");
				//alert("请求失败")
			}
		})
	})

//分享
var chaptername=$("#sectionlist h3").text(),   //章名称
	sectionname=$(".videohead h1 span b").text();
var html="我正在慕课网学习一门很不错的课程【"+chaptername+""+"】我已学习至"+sectionname+",分享给想学习实用IT技能的同学!"    //节名称
var imgPic = $('#coursePic img')[0].src;
window._bd_share_config={"common":{"bdSnsKey":{},"bdText":html,"bdMini":"2","bdMiniList":false,"bdPic":imgPic,"bdStyle":"0","bdSize":"16"},"share":{}};with(document)0[(getElementsByTagName('head')[0]||body).appendChild(createElement('script')).src='http://bdimg.share.baidu.com/static/api/js/share.js?v=89860593.js?cdnversion='+~(-new Date()/36e5)];	


});
