package com.cm.basic.cache.local;

import java.lang.reflect.Method;

public class CacheConfig {
	
	public CacheConfig() {}
	
	private String cacheName;
	
	private Class KeyClazz;
	
	private Method KeyMethod;
	
	private Class loaderClazz;
	
	private String beanName;

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getCacheName() {
		return cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public Class getKeyClazz() {
		return KeyClazz;
	}

	public void setKeyClazz(Class keyClazz) {
		KeyClazz = keyClazz;
	}

	public Method getKeyMethod() {
		return KeyMethod;
	}

	public void setKeyMethod(Method keyMethod) {
		KeyMethod = keyMethod;
	}

	public Class getLoaderClazz() {
		return loaderClazz;
	}

	public void setLoaderClazz(Class sourceClazz) {
		this.loaderClazz = sourceClazz;
	}
	
	
}
