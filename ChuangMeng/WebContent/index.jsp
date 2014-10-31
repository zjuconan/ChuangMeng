<%@ page language="java" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html>
<header>
 	<meta charset="utf-8">
    <!-- <meta http-equiv="X-UA-Compatible" content="IE=edge"> -->
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">

    <title>中国第一个在线教育平台</title>
    <!-- Bootstrap Core CSS -->
    <link href="css/bootstrap.min.css" rel="stylesheet">

    <!-- Custom CSS -->
    <link href="css/chuangmeng_homepage.css" rel="stylesheet">
	<!-- jquery slider -->
	    <!-- jQuery Version 1.11.0 -->
    <script src="js/jquery-1.11.0.js"></script>

    <!-- Bootstrap Core JavaScript -->
    <script src="js/bootstrap.min.js"></script>
	
	<!-- bxSlider Javascript file -->
	<script src="js/jquery.bxslider.min.js"></script>
	<!-- bxSlider CSS file -->
	<link href="css/jquery.bxslider.css" rel="stylesheet" />
	<!-- one page scroll -->
	<script src="js/jquery.onepage-scroll.js" type="text/javascript"> </script>
	<link href="css/onepage-scroll.css" rel="stylesheet" type="text/css">

    <!-- Fonts 
    <link href="http://fonts.googleapis.com/css?family=Open+Sans:300italic,400italic,600italic,700italic,800italic,400,300,600,700,800" rel="stylesheet" type="text/css">
    <link href="http://fonts.googleapis.com/css?family=Josefin+Slab:100,300,400,600,700,100italic,300italic,400italic,600italic,700italic" rel="stylesheet" type="text/css">
	-->
    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
        <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
        <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->

</header>

<body>

	<div class="main" style="height: 100% !important;
    	height: 100%;
    	margin: 0 auto; ">
		<!-- main - page1 -->
		<section style="overflow: hidden;">
				<div class="navbar navbar-default" id="navbar-background" role="navigation">
					<div class="container">
						<div class="row nav-row">
							<div class="col-lg-12">	
								<a href="#" class="brand-bar2-a navbar-right">
									<span>我的课程</span>
								</a>
								
							</div>
						 </div>
					</div>
					<div class="container">
						<div class="row nav-row">
							<div class="col-xs-4">
								<a href="#" title="startup.com/在线创业教育">
									<span class="glyphicon glyphicon-thumbs-up navbar-left" style="font-size:26px;font-weight: 700;color:black;padding-top:10px;">startup.com/在线创业教育</span>
								<a>
							</div>
							<div class="col-xs-8">
								<a href="#" class="navbar-right" id="brand-bar2-a-register">
									<span>注册</span>
								</a>
								<a href="#" class="navbar-right" id="brand-bar2-a-register">
									<span style="font-size:12px;" >您好</span>
									<span>登录</span>
								</a>
								<form class="navbar-form navbar-right" role="search">
								  <div class="form-group">
									<input type="text" class="form-control" style="width:200px" placeholder="搜索课程、老师...">
								  </div>
								  <button type="submit" class="btn btn-default">搜索</button>
								</form>
								<div class="dropdown navbar-right" id="query-search-button">
									<button class="btn btn-default dropdown-toggle navbar-right" type="button" id="dropdownMenu1" data-toggle="dropdown">
									<span class="glyphicon glyphicon-th-list"> 所有课程</span>
								  </button>
								  <ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1">
									<li role="presentation" class="dropdown-submenu"><a role="menuitem" tabindex="-1" href="play_list.html">创业者能力</a>
										<!--二级菜单 -->
										<ul class="dropdown-menu">
											<li class="menu-item dropdown">
											<a href="./cm_play6.html" class="dropdown-toggle">创业导论</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">浙商与企业家精神</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">互联网思维与创新</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创业社会责任</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">商业伦理</a></li>
										</ul>
									</li>
									<li role="presentation"  class="dropdown-submenu">
										<a role="menuitem" tabindex="-1" href="play_list.html">创业环境</a>
										<ul class="dropdown-menu">
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">国家文化与创业进入</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创业生态圈</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">行业与市场分析</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创业社会责任</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">初创企业法务</a></li>
										</ul>
									</li>
									<li role="presentation"  class="dropdown-submenu">
										<a role="menuitem" tabindex="-1" href="play_list.html">创业产品</a>
										<ul class="dropdown-menu">
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">商业模式设计与优化</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">商业计划书写作</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">从技术创新到商业化运营</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">产品设计与开发</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">质量管理</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">供应链管理</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">供应链管理</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创新管理</a></li>
										</ul>
									</li>
									<li role="presentation"  class="dropdown-submenu"><a role="menuitem" tabindex="-1" href="play_list.html">创业营销</a>
										<ul class="dropdown-menu">
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">销售与渠道管理</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">客户管理</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">品牌管理</a></li>
										</ul>
									</li>
									<li role="presentation"  class="dropdown-submenu"><a role="menuitem" tabindex="-1" href="play_list.html">创业人力资源</a>
										<ul class="dropdown-menu">
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创业合伙人搭建</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创业股权安排与激励</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">项目管理</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">绩效管理</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">战略人力资源管理</a></li>
										</ul>
									</li>
									<li role="presentation"  class="dropdown-submenu"><a role="menuitem" tabindex="-1" href="play_list.html">创业财务</a>
										<ul class="dropdown-menu">
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创业财务</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创业价值评估</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创业融资</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">资本市场与上市</a></li>
										</ul>
									</li>
									<li role="presentation"  class="dropdown-submenu"><a role="menuitem" tabindex="-1" href="play_list.html">创业战略</a>
										<ul class="dropdown-menu">
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">企业文化</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">危机管理</a></li>
											<li class="menu-item dropdown"><a href="./cm_play6.html" class="dropdown-toggle">创业风险管理</a></li>
										</ul></li>
								  </ul>
								  </div>
							</div>
						</div>
					</div>
				</div>
			<div class="container" id="body-container">
				<div class="row">
					<div class="homepage-main-text">
						<form action="./cm_play6.html">
						<h2>最专业的课程，最专业的老师；中国第一个在线创业教育平台<h2>
						<button type="submit" class="btn btn-default get-start-button">我要上课</button>
						</form>
					</div>
				</div>
				<div class="row">
					<div class="homepage-main-bottom container" id="main-course">
							<div class="row">
								<div class="course-class col-sm-2" id="course1">
									<a href="play_list.html">创业者能力</a>
								</div>
								
								<div class="course-class col-sm-2" id="course2">
									<a href="play_list.html">创业环境</a>
								</div>
								<div class="course-class col-sm-2" id="course3">
									<a href="play_list.html">创业产品</a>
								</div>
								<div class="course-class col-sm-2" id="course4">
									<a href="play_list.html">创业营销</a>
								</div>
								<div class="course-class col-sm-2" id="course5">
									<a href="play_list.html">创业人力资源</a>
								</div>
								<div class="course-class col-sm-2" id="course6">
									<a href="play_list.html">创业财务</a>
								</div>
								<div class="course-class col-sm-2" id="course7">
									<a href="play_list.html">创业战略</a>
								</div>
						</div>
				</div>
		</div>
			
		</section>
		
		<!-- main - page2 -->
		<section class="one-page-section2">
			<!-- Nav tabs -->
			<div class="container">
				<ul class="nav nav-pills .nav-justified" role="tablist" style="font-size:26px;font-weight:100;" id="myTabs">
				  <li class="active">
					<a href="#home" role="tab" data-toggle="tab">
					最热门
					</a>
					
				  </li>
				  <li><a href="#profile" role="tab" data-toggle="tab">最受欢迎</a></li>
				  <li><a href="#messages" role="tab" data-toggle="tab">最新</a></li>
				</ul>
					<script>
					 $(document).ready(function(){$("#myTabs li a").hover(function(){
						this.click();
					 })
					 });
					</script>
				<!-- Tab panes -->
				<div class="tab-content">
				  <div class="tab-pane active" id="home">
						<div class="container">
							<div class="row">
								<div class="col-xs-6">
									<div class="row">
										<div class="col-xs-6">
											<div class="view view-first">
													<img alt="" src="img/course4.jpg">
													<div class="mask">
														<h2>创业导论</h2>
														<p>什么是创业，为什么要创业，创业要做什么准备</p>
														<a href="./cm_play6.html" class="info">了解更多</a>
													</div>
											</div>
										</div>
										<div class="col-xs-6">
											<div class="view view-first">
													<img alt="" src="img/course2.jpg">
													<div class="mask">
														<h2>创业导论</h2>
														<p>什么是创业，为什么要创业，创业要做什么准备</p>
														<a href="./cm_play6.html" class="info">了解更多</a>
													</div>
											</div>
										</div>
									</div>
									<div class="row">
										<div class="col-xs-6">
											<div class="view view-first">
													<img alt="" src="img/course3.jpg">
													<div class="mask">
														<h2>创业导论</h2>
														<p>什么是创业，为什么要创业，创业要做什么准备</p>
														<a href="./cm_play6.html" class="info">了解更多</a>
													</div>
											</div>
										</div>
										<div class="col-md-6">
											<div class="view view-first">
													<img alt="" src="img/course1.jpg">
													<div class="mask">
														<h2>创业导论</h2>
														<p>什么是创业，为什么要创业，创业要做什么准备</p>
														<a href="./cm_play6.html" class="info">了解更多</a>
													</div>
											</div>
										</div>
									</div>
								</div>
								<div class="col-md-6">	
									<div class="row" >
										 <div class="col-md-6">
											<div class="view view-first">
													<img alt="" src="img/course10.jpg">
													<div class="mask">
														<h2>创业导论</h2>
														<p>什么是创业，为什么要创业，创业要做什么准备</p>
														<a href="./cm_play6.html" class="info">了解更多</a>
													</div>
											</div>
										</div>
									</div>
									<div class="row">
										 <div class="col-md-6">
											<div class="view view-first">
													<img alt="" src="img/course5.jpg">
													<div class="mask">
														<h2>创业导论</h2>
														<p>什么是创业，为什么要创业，创业要做什么准备</p>
														<a href="./cm_play6.html" class="info">了解更多</a>
													</div>
											</div>
										</div>
									</div>
								</div>
							</div>
							<!---- the third row of picture -->
							<div class="row">
								<div class="col-xs-6">
									<div class="row">
										<div class="col-xs-6">
											<div class="view view-first">
													<img alt="" src="img/course7.jpg">
													<div class="mask">
														<h2>创业导论</h2>
														<p>什么是创业，为什么要创业，创业要做什么准备</p>
														<a href="./cm_play6.html" class="info">了解更多</a>
													</div>
											</div>
										</div>
										<div class="col-xs-6">
											<div class="view view-first">
													<img alt="" src="img/course8.jpg">
													<div class="mask">
														<h2>创业导论</h2>
														<p>什么是创业，为什么要创业，创业要做什么准备</p>
														<a href="./cm_play6.html" class="info">了解更多</a>
													</div>
											</div>
										</div>
									</div>
									
								</div>
								<div class="col-md-6">	
									<div class="row" >
										 <div class="col-md-6">
											<div class="view view-first">
													<img alt="" src="img/course9.jpg">
													<div class="mask">
														<h2>创业导论</h2>
														<p>什么是创业，为什么要创业，创业要做什么准备</p>
														<a href="./cm_play6.html" class="info">了解更多</a>
													</div>
											</div>
										</div>
									</div>
									
								</div>
							</div>
						</div>
				  </div>
				  <div class="tab-pane " id="profile" style="min-height:680px;">
						<div class="row">
								<div class="col-md-6">
									<h1>最受欢迎课程1</h1>
								</div>
								<div class="col-md-6">	
									<h1>最受欢迎课程2</h1>
								</div>
							</div>
					</div>
				  <div class="tab-pane " id="messages" style="min-height:680px;">
					<div class="container">
							<div class="row">
								<div class="col-md-6">
									<h1>最新上线课程1</h1>
								</div>
								<div class="col-md-6">	
									<h1>最新上线课程2</h1>
								</div>
							</div>
						</div>
				  </div>
				</div>
			</div>
		</section>
		
		
		<!-- main - page3 -->
		<section class="one-page-section3">
				<div class="container">
					<div class="row" style="text-align:center;margin:20px;">
							<div class="col-md-12">
								<h1>这里拥有最强的师资力量</h1>
							</div>
					</div>
				</div>
				<div class="container">
					<div class="row">
						<div class="col-sm-5 col-sm-push-7" style="color:black;padding-top: 120px;">
							<h2>师资力量</h2>
							<h5>导师们都拥有着丰富的创业理论知识和实践。</h5>
							<form action="./cm_play6.html">
							<button type="submit" class="btn btn-default" style="background-color:#09f;color:black;font-size:30px;font-weight:300;padding:10px 30px;margin-top:45px;">了解更多</button>
							</form>
						</div>
						<div class="col-sm-6 col-sm-pull-5">
							<div class="row">
								<div class="col-xs-3" id="teacher_header_picture">
									<img alt="Adrian Farr" class="img-circle appeared" src="img/teacher/100.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/200.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/300.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/400.jpg">
								</div>
							</div>
							<div class="row">
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/500.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/600.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/700.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/800.jpg">
								</div>
							</div>
							<div class="row">
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/1000.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/900.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/3.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img    alt="Adrian Farr" class="img-circle appeared" src="img/teacher/4.jpg">
								</div>
							</div>
							<div class="row">
								<div class="col-xs-3" id="teacher_header_picture">
									<img alt="Adrian Farr" class="img-circle appeared" src="img/teacher/5.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img  alt="Adrian Farr" class="img-circle appeared" src="img/teacher/6.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img   alt="Adrian Farr" class="img-circle appeared" src="img/teacher/7.jpg">
								</div>
								<div class="col-xs-3" id="teacher_header_picture">
									<img   alt="Adrian Farr" class="img-circle appeared" src="img/teacher/2.jpg">
								</div>
							</div>
						</div>
				</div>
			</div>
			<footer style="text-align:center;">
				<div class="container">
					<div class="row">
						<div class="col-lg-12 text-center">
							<p>Copyright &copy; www.startup.com 2014</p>
						</div>
					</div>
				</div>
			</footer>
		</section>
	</div>	
	
    
	
	<script>
	  $(document).ready(function(){
      $(".main").onepage_scroll({
        sectionContainer: "section",
        responsiveFallback: 600,
        loop: true
      });
		});
		
	</script>
</body>
</html>