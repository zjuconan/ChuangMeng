<%@ page language="java" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title></title>
</head>
<body>
	<jsp:include page="/template/header.jsp"></jsp:include>
	<jsp:include page="/template/nav.jsp"></jsp:include>
	
    <div class="container">
		<div class="row">
				<div class="box" style="height:600px;">
					<div id="playerContainer"></div>
						<p class="video-meta">
							这里是关于食品的介绍：我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧我们来一起创业吧。
						</p>
                   </div>
		</div>
	
		
		<div class="row">
			<div class="box">
				<div class="get-start-bar">
					<span class="main-info"><b>尝试一下我们免费的培训课程:</b></span>
					<span class="sub-info">每个课程都有免费的教学内容，成为会员系统的学习整个课程。</span>
					<a href="#" class="em-button" id="get-start-button">开始学习</a>
                   </div>
				</div>
		</div>
		<div class="row">
			<div class="box">
				<ul class="bxslider" style="width:100%; padding:1px;margin:0px;border:1 solid red;">
				<li><a href="#"><img class="img-thumbnail" src="img/slide-1.jpg" alt="image01" /></a></li>
				<li><a href="#"><img class="img-thumbnail" src="img/slide-2.jpg" alt="image01" /></a></li>
				<li><a href="#"><img class="img-thumbnail" src="img/slide-1.jpg" alt="image01" /></a></li>
				<li><a href="#"><img class="img-thumbnail" src="img/slide-2.jpg" alt="image01" /></a></li>
				<li><a href="#"><img class="img-thumbnail" src="img/slide-1.jpg" alt="image01" /></a></li>
				<li><a href="#"><img class="img-thumbnail" src="img/slide-2.jpg" alt="image01" /></a></li>
			</ul>
			</div>
		</div>
    </div>
	
    
	<jsp:include page="/template/footer.jsp"></jsp:include>
	<script type="text/javascript" src="ckplayer/ckplayer.js" charset="utf-8"></script>
	<!-- Script to Activate the Carousel -->
    <script>
    $(document).ready(function(){
	  $('.bxslider').bxSlider({
			minSlides: 2,
		  maxSlides: 4,
		  slideWidth: 275,
		  slideMargin: 20,
		 /* ticker: true speed: 10000,*/
		  auto: true,
			autoControls: true
	  });
	});
    var flashvars = {
			f : 'resources/infoq_1.flv',
			c : 0,
			p : 2,
			o : 138,
			t : '10|10',
			v : 30,
			i : 'img/slide-1.jpg',
			loaded : 'loadedHandler'
		};
		var params = {
			bgcolor : '#FFF',
			allowFullScreen : true,
			allowScriptAccess : 'always',
			wmode : "transparent"
		};
		var video = [ 'resources/infoq_1.mp4->video/mp4' ];
		CKobject.embed('ckplayer/ckplayer.swf', 'playerContainer',
				'ckplayer_a1', '1170', '560', false, flashvars, video, params);
 
		function loadedHandler() {
			if (CKobject.getObjectById('ckplayer_a1').getType()) {//说明使用html5播放器
				CKobject.getObjectById('ckplayer_a1').addListener('play',
						playHandler);
			} else {
				CKobject.getObjectById('ckplayer_a1').addListener('play',
						'playHandler');
			} 
		}
		function removeEvent() {//删除监听事件
			if (CKobject.getObjectById('ckplayer_a1').getType()) {//说明使用html5播放器
				CKobject.getObjectById('ckplayer_a1').removeListener('time',
						playHandler);
			} else {
				CKobject.getObjectById('ckplayer_a1').removeListener('time',
						'playHandler');
			}
		}
		function playHandler() {
			//alert("start to play");
		}
    </script>
    
</body>
</html>