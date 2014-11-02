package com.cm.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class CmLogger {
	
	/**
	 * if no logger with loggerName found, it will create default and use ROOT appender.
	 * so keep in mind: don't create too many loggers for different threads. 
	 * If there are many log in ROOT log file, then consider to configure new logger for 
	 * the function!
	 * @param loggerName log name
	 * @return  a {@code Logger} instance associated with the logger name
	 */
    public static Logger getLogger(String loggerName) {
    	//it will never to null
        return LogManager.getLogger(loggerName);
    }
    
    /**
     * it used to logging
     * @return  a {@code Logger} instance associated with the current thread name
     */
    public static Logger getLogger() {
    	String threadName = Thread.currentThread().getName();
    	Logger logger = (Logger) getLogger(threadName);
    	//if(logger == null) logger = getDefaultLogger();
    	return logger;
    }

	  public static Logger getDefaultLogger() {
		  return LogManager.getRootLogger();
	  }
	
}
