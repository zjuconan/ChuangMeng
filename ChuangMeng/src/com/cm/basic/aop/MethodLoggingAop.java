package com.cm.basic.aop;

import org.aspectj.lang.ProceedingJoinPoint;

import com.cm.logging.CmLogger;

public class MethodLoggingAop {

	private static int depth = 6;
	
	public void before(){
		String className = getClassName(depth);
		String methodName = getMethodName(depth);
		CmLogger.getLogger().info(className + "#" + methodName +" starts...");		
	}
	
	public void afterReturning(){
		String className = getClassName(depth);
		String methodName = getMethodName(depth);
		CmLogger.getLogger().info(className + "#" + methodName +" returned...");		
	}
	
	public void afterThrowing(){
		String className = getClassName(depth);
		String methodName = getMethodName(depth);
		CmLogger.getLogger().info(className + "#" + methodName +" has exception occurred...");		
	}
	
	//finally
	public void after(){
		String className = getClassName(depth);
		String methodName = getMethodName(depth);
		CmLogger.getLogger().info(className + "#" + methodName +" ends...");		
	}
	
	public Object around(ProceedingJoinPoint pjp) throws Throwable {
		String className = pjp.getTarget().getClass().getName(); 
		String methodName = pjp.getSignature().getName();

		CmLogger.getLogger().info(className + "#" + methodName +" starts...");
		try {
			return pjp.proceed();
		} catch(Exception e){
			CmLogger.getLogger().warn(className + "#" + methodName +" has exception occurred...");
			throw e;
		} finally{
			CmLogger.getLogger().info(className + "#" + methodName +" ends...");
		}
	}
	
	public static String getMethodName(final int depth)
	{
	    final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
	    return ste[1+depth].getMethodName();
	}
	
	public static String getClassName(final int depth)
	{
	    final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
	    return ste[1+depth].getClassName();
	}
}
