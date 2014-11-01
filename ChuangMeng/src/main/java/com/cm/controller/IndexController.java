package com.cm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(value="/index.shtml")
public class IndexController {

	@RequestMapping(params = "!action")
	public ModelAndView initPage(HttpServletRequest request){
		
		return new ModelAndView();
	}
}
