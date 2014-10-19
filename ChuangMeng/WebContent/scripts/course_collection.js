define(function(require, exports, module){
	require('/static/lib/layer/1.6.0/layer.min.js');
	require('/static/lib/layer/1.6.0/skin/layer.css');
	//关注课程
	var isAjax=0;
	$('.js-btn-collection').on('click',function(){
        if (!OP_CONFIG.userInfo) {
            function popLogin() {
                require.async('login_sns',
                function(login) {
                    login.init();
                });
            };
            popLogin();
            return;
        }
		if(isAjax) return;
		isAjax=1;
		var obj=$(this);
		var id=obj.attr("data-id")
		var url="/space/ajaxfollow";
		if(obj.hasClass('btn-remove-collection')){
			url="/space/ajaxfollowcancel";
		}
		$.ajax({
			type: "POST",
			url: url,
			dataType:"json",
			data: {
				course_id:id
			},
			success: function(res){
				isAjax=0;
				if(res.result==0){
					if(obj.hasClass('btn-remove-collection')){
						obj.removeClass('btn-remove-collection').html("关注此课程");
					}else{
						obj.addClass('btn-remove-collection').html("取消关注");
					}					
				}else{
					layer.msg('操作失败，请稍后再试', 1, 1);
				}
			},
			error:function(){
				layer.msg('网络错误，请稍后再试', 1, 1);
            	isAjax=0
        	}
		});
	});
})