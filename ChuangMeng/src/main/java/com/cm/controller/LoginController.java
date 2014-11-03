package com.cm.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController {
	private Logger Logger=LoggerFactory.getLogger(LoginController.class);
	@RequestMapping(value="/login.shtml", method={RequestMethod.GET,RequestMethod.POST}, produces="application/json")
	public @ResponseBody  String login(@RequestParam(value="user",required=false) String user,HttpServletRequest request,HttpServletResponse response) {
		Logger.info("get json input from request body annotation");
		Logger.info("user "+user +" login");
		
		request.getSession().setAttribute("user", user); 
		Map<String, String> map = new HashMap<String, String>(); 
		map.put("sucess", "true");
		map.put("message", "login sucessful");
		return "{\"success\":\"true\",\"message\":\"sucessful\"}";
	}
}