define(function(require, exports, module){
	require('jwplayer');
    require('./common/animate-achievement');
    var store=require('store');
	var commonInterface=require("/static/page/course/common/course_detail_common.js");

	var mediaData;
	$.getJSON("/course/ajaxmediainfo/?mid="+pageInfo.mid+"&mode=flash",function(data){
		mediaData=data.data.result;
		initPlayer();
	});

	if(typeof continueTime != 'number'){
		continueTime=0;
        var sv=store.get("_vt");
        if(sv&&sv[pageInfo.mid]){
            continueTime=sv[pageInfo.mid].st||0;
        }
	}

    $(window).on("beforeunload",function(){
        var vt=store.get("_vt")||{},
            it=vt[pageInfo.mid],
            state=thePlayer.getState();
        if(state=="IDLE"){
            delete vt[pageInfo.mid];
            store.set("_vt",vt);
            return ;
        }
        if(it){
            it.t=new Date().getTime();
            it.st=thePlayer.getPosition();
            store.set("_vt",vt);
        }
        else{
            it={
                t:new Date().getTime(),
                st:thePlayer.getPosition()
            }
            ck();
            vt[pageInfo.mid]=it;
            store.set("_vt",vt);
        }
        function ck(){ //check length<10 ,delete overflowed;
            var k,tk,i=0,tt=new Date().getTime();
            for(k in vt){
                i++;
                if(vt[k].t<tt){
                    tt=vt[k].t;
                    tk=k;
                }
            }
            if(i>=10){
                delete vt[tk];
                ck();
            }
        }
    });
	var sentLearnTime=(function(){
		if(!OP_CONFIG.userInfo){
			return ;
		}
	 	var _params={},
	 		lastTime=0,
	 		startTime=new Date().getTime();
		var fn;
	    _params.mid=pageInfo.mid;

	    window.setInterval(fn=function(){
			var overTime,
				stayTime;
			if(typeof(thePlayer)!='object') return //no video no time;
			overTime=new Date().getTime();
			stayTime=parseInt(overTime-startTime)/1000;

			_params.time=stayTime-lastTime;
			_params.learn_time =thePlayer.getPosition();
	
			$.ajax({
				url:'/course/ajaxmediauser/',
				data:_params,
				type:"POST",
				dataType:'json',
				success:function(data){
					if(data.result== '0'){
						lastTime=stayTime;
                        var chapterMp = data.data.media;
                        var courseMp = data.data.course;
                        chapterMp && setAchievement(chapterMp.mp.point, chapterMp.mp.desc, function(){
                            courseMp && setAchievement(courseMp.mp.point, courseMp.mp.desc);
                        });
					} 
				}
			});
		},60000);

		window.onbeforeunload=function(){
			var overTime,
				stayTime;
			if(typeof(thePlayer)!='object') return //no video no time;
			overTime=new Date().getTime();
			stayTime=parseInt(overTime-startTime)/1000;

			_params.time=stayTime-lastTime;
			_params.learn_time =thePlayer.getPosition();
	
			$.ajax({
				url:'/course/ajaxmediauser/',
				data:_params,
				type:"POST",
				async:false,
				dataType:'json',
				success:function(data){
					if(data.result=='0'){
						lastTime=stayTime;
					} 
				}
			});
		}
		return fn;
	})();

    function setAchievement(mp, des, callback){
        var $animateMp = $('<div class="animate-mp">' +
            '<p class="mp">经验<span class="num">+'+mp+'</span></p>' +
            '<p class="desc">'+des+'</p>' +
            '</div>');
        $("#video-box_wrapper").append($animateMp);
        setTimeout(function(){
            $animateMp.fadeIn();
            setTimeout(function(){
                $animateMp.addMP('.my_mp',function(){
                    this.fadeOut(function(){
                        var $mp = $(".my_mp .mp_num");
                        $mp.text(+$mp.text() + mp);
                        $animateMp.remove();
                    });
                    callback && callback();
                });
            },2000)
        },100);
    }
	//总是以flash的方式开始调用 
    function initPlayer(){
        thePlayer = jwplayer('video-box').setup({
            width:1000,
            height:530,
            playlist: [{
                //image: "JW.jpg",
                sources: [{
                   file: mediaData.mpath[2],
				   // file: "http://10.96.141.77/e1df8c31-3ab8-4efb-9d48-e5134a59dcca/Y_dest_1.mp4",
                    label: "普清",
                    "default": true
                },{
                    file: mediaData.mpath[1],
                    label: "高清"
                },{
                    file: mediaData.mpath[0],
                    label: "超清"
                }]
            }],
            primary: "flash",
            autostart:false,
            startparam: "start",
            autochange:true,
            events: {
                onReady: function() {//
                    if(OP_CONFIG.userInfo){
                        thePlayer.seek(continueTime);
                    }
                },
                onComplete: function(){
                    $('#J_NextBox').removeClass('hide');
                    sentLearnTime();
                },
    		    onBuffer:function(callback){//缓冲状态，缓冲图标显示
				
					playserWaitTime=new Date().getTime();
				   if(bufferType==1){
						playerOldHd=thePlayer.getCurrentQuality();
				   };
					key=radKey(10)+playserWaitTime;
					sendVideoTestData(2,0,"",playserWaitTime,0);
		
				},
				onPlay:function(callback){//开始播放－缓冲结束
					
					if(callback.oldstate=="PAUSED" ){
						return;
					} 
					var bufferTme=new Date().getTime()-playserWaitTime;
					sendVideoTestData(1,bufferTme,"",new Date().getTime(),1);
					
				},
				 onQualityChange :function(callback){
					//console.log("onQualichange-----");
				},
		
				onError:function (callback){
					loadVideo(callback.message)
				}
            }
        })
    }



  function loadVideo(message) { 
  	var arr=[2,1,0];
	var index=arr[jwplayer().getCurrentQuality()];

	if(mediaData.mpath[index].indexOf("imooc")>-1){ //视频为imooc地址仍访问不到。
		var errorBufferTme=new Date().getTime()-playserWaitTime;
		sendVideoTestData(0,errorBufferTme,message,new Date().getTime(),1); 
		return;
	}
	mediaData.mpath[index]=mediaData.mpath[index].replace(/mukewang/,"imooc");
    jwplayer().load([{
     sources: [{
                    file: mediaData.mpath[2],
                    label: "普清",
                    "default": true
                },{
                    file: mediaData.mpath[1],
                    label: "高清"
                },{
                    file: mediaData.mpath[0],
                    label: "超清"
                }]
    }]);
    jwplayer().play();
  };
/*
  清晰度是否默认：hdDefault 1/0
  清晰度类型：hdType
  清晰度是否切换成功：hdSwitch 
  视频响应时长：bufferTime
  url测试：http://www-jiangwb.imooc.com/course/collectvideo
 
  播放器抛出错误 Error loading media: File not found  后数据监测
  videoId:   视频id
  videoFileName:  视频地址 
  
 新增－－－－－－－－－－－－－－－－
  错误内容
  erroryMsg: 

  loading类型 
  bufferType :0-->切换loading；1--->初始loading  2-->播放中loading;  
	
   来源
   source:    1-->pc;0-->wap  数据来源
   开始结束-------------
   flag:0 开始 1结束
   time: 开始和结束时间戳
   key,加在url后面
*/

var	playserWaitTime,//计时器－－－－－
	playerOldHd=0,
	playerCurrentHd=0,//0－1－2，清晰度三种类型;
	bufferType=1;  

var key;
function radKey(length){
	var word ="";
	for(var i=0;i<length;i++){
	var r = parseInt(Math.random()*75+48);
		if((r>57&&r<65) || (r>90&&r<97)){
			r-=10;
		}
	word += String.fromCharCode(r);
	}
	return word;
}
function sendVideoTestData(hdSwitch,bufferTime,msg,time,flag){
    var renderingMode=1;
	var item = jwplayer().getPlaylistItem();
	 playerCurrentHd=jwplayer().getCurrentQuality();
	var videoUrl=item.sources[playerCurrentHd].file;
    if(thePlayer.getRenderingMode()=="html5"){
        renderingMode=0;
		videoUrl=videoUrl.replace(/\.flv\s*$/,".mp4");
    }
	if(msg==""){
		msg="Error loading media: File not found";
	}
	if(playerOldHd!=playerCurrentHd){
	
		bufferType=0;
	}
    $.post("/course/collectvideo?key="+key,{
        renderingMode:renderingMode,
        oldHd:playerOldHd,
        currentHd:playerCurrentHd,
        hdSwitch:hdSwitch,
        bufferTime:bufferTime,
		videoFileName:videoUrl,
		videoId:pageInfo.mid,
		errorMsg:msg,
		source:1,
		bufferType:bufferType,
		time:time,
		flag:flag
    });
	if(flag==1){
	 playerOldHd=playerCurrentHd;
	 bufferType=2;
	
	}
}

 //视频不能播放出错后flash回调
window.videoErrorMsg=videoErrorMsg;
function videoErrorMsg(msg){
	//var bufferTme=new Date().getTime()-playserWaitTime;
	//sendVideoTestData(0,bufferTme,msg,new Date().getTime(),1);
}



//截图后flash回调
window.screenReceive=screenReceive;
function screenReceive(data){
	if(typeof data=="string"){
		data=$.parseJSON(data);
	}
	if(data.result==0){
		shot.screenShotFlashBack(data);
	}
	else{
		alert(data.msg||"错误，请稍后重试");
	}
	//console.log(url,typeof url)
}

$.each("qa,note".split(","),function(k,v){
   	commonInterface.remote[v].extendMethod("reset",function(){
   		shot.reset(".js-shot-video[data-type='"+v+"']");
   	});
});

var shot={
	screenShot:function(el){
		if(thePlayer.getState()=="IDLE"){    
			alert('请在视频播放时截图')
			return ;
		}
	    if(!thePlayer.getState()){
			alert('请在视频播放时截图')
			return ;
		}
	    if(thePlayer.getState()=="PLAYING"){
			thePlayer.pause();
		}
		try{
			thePlayer.screenShot();		
		}
		catch(e){
			alert("您当前使用的html5播放器暂不支持视频截图，请下载flash播放器");
		}						
		//$('.shot-btn').addClass('hide');
		var $el=$(el),
			time=parseInt(thePlayer.getPosition(),10);
		this.el=el;
		$el.next().find(".shot-time").text(this.formatSecond(time));
		$el.hide().next().show();
		commonInterface.remote[$el.attr("data-type")].set({picture_time:time});
	},
	formatSecond:function(sec){
        var result = _format(parseInt(sec/60))+":"+_format(sec%60);
        function _format(min){
            return min < 10 ? '0' + min: min;
        }
        return result;
    },
    reset:function(el,fromEl){
    	var $el=$(el),$next;
    	this.el=null;
    	($next=$el.show().next()).hide();
    	$next.find("img").attr("src","");
   		$next.find(".shot-time").text("");
    	fromEl&&commonInterface.remote[$el.attr("data-type")].reset();
    },
    screenShotFlashBack:function(data){
    	if(!this.el) return ;
    	$(this.el).next().find("img").attr("src",data.data.pic);
    	commonInterface.remote[$(this.el).attr("data-type")].set("picture_url",data.data.pic);
    }
}

$(".js-shot-video").click(function(){
	shot.screenShot(this);
});
$(".js-close-vshot").click(function(){
	shot.reset($(this).parent().prev(),1);
});
/*
	//截图操作对象
	var shot= (function(){
		var  screenShot=function(){
			if(thePlayer.getState()=="IDLE"){    
				alert('请在视频播放时截图')
				return ;
			}
		    if(!thePlayer.getState()){
				alert('请在视频播放时截图')
				return ;
			}
		    if(thePlayer.getState()=="PLAYING"){
				thePlayer.pause();
			}
			try{
				thePlayer.screenShot();		
			}
			catch(e){
				alert("您当前使用的html5播放器暂不支持视频截图，请下载flash播放器");
			}						
			$('.shot-btn').addClass('hide');
			$('.shot-img').removeClass('hide');
			postData.picture_time=parseInt(thePlayer.getPosition());
			require.async('util', function(util){
				$('.shot-time').text(util.formatSecond(postData.picture_time>>0));
			});
		}
		
		var reset = function(){
			$('.shot-btn').removeClass('hide');
			$('.shot-img').addClass('hide').attr('title','点击截图');
			$('.shot-time').text('');
			$('.shot-img').find('img').attr('src','');
			$('.shot-img').find('btn-close').addClass('hide');
			//$('.shot-btn').on('click', shot.screenShot);
			postData.picture_url='';
			postData.picture_time=0;
			return false
		};
		var backPlay = function(time){
			if($('#J_NextBox').is(':visible')){ 
				$('#J_NextBox').addClass('hide');
			}
			//thePlayer.play();
			//thePlayer.seek(3);
			if (thePlayer.getState() != 'PLAYING') {    
				thePlayer.play();
			}
			thePlayer.seek(time);
		};
		return {
			screenShot:screenShot,
			reset : reset,
			backPlay : backPlay
		};
	})();

	window.shot = shot;*/
});
