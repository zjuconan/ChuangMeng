package com.cm.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.cm.domain.UserBasic;
import com.cm.domain.mapper.UserBasicMapper;
import com.cm.logging.CmLogger;

@Controller
@RequestMapping(value = "/index.shtml")
public class IndexController {
	private static final Logger logger = CmLogger.getLogger();

	@Autowired
	UserBasicMapper userBasicMapper;

	@RequestMapping(params = "!action")
	public ModelAndView initPage(HttpServletRequest request) {
		logger.debug("into getUser Controller.");
		UserBasic user = userBasicMapper.getUser(1);
		logger.debug("end of logger, get user: " + user);
		return new ModelAndView();
	}

	@RequestMapping(params = "getUser")
	public ModelAndView getUser(HttpServletRequest request) {
		logger.debug("into getUser Controller.");
		UserBasic user = userBasicMapper.getUser(1);
		logger.debug("end of logger, get user: " + user);
		return null;
	}
}