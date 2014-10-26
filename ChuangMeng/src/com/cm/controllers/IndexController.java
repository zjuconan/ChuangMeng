package com.cm.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value="/index.shtml")
public class IndexController {

	@RequestMapping(params = "!action")
	public ModelAndView initPage(HttpServletRequest request){
		
		return new ModelAndView();
	}
}
