<%@ page language="java" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title></title>
</head>
<body>
<jsp:include page="/template/header.jsp"></jsp:include>
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

</body>
</html>