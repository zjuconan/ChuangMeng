package com.cm.basic.cache.local;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.cm.logging.CmLogger;


/**
 * To use this cache service transparently, please don't call this service directly except from service layer
 * which cross cut by transaction manager
 */
public class CacheService implements ApplicationContextAware {
	
	/** cacheService config file name **/
	public static final String SERVICE_CONFIG = "cacheService.xml";
	
	/** ehcache config file name **/
	public static final String EHCACHE_CONFIG = "ehcache.xml";
	
	/**
	 * spring application context
	 */
	private ApplicationContext springApplicationCtx = null;
	
	/**
	 * ehcache cache manager
	 * in current implemenation, use singleton cache manager, 
	 * thus the ehcache config file will not be deleted while full building happened
	 */
	private CacheManager cacheManager = null;
	
	/**
	 * cache service config collection
	 */
	private Map<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<String, CacheConfig>();

	/**
	 * cache lock collection
	 */
	private Map<String, ReentrantLock> cacheLocks = new ConcurrentHashMap<String, ReentrantLock>();
	
	/**
	 * cache status collection
	 */
	private Map<String, CacheStatus> cacheStatusMap = new ConcurrentHashMap<String, CacheStatus>();
	
	
	/**
	 * toggle for asynchronous cache initialization
	 */
	private boolean asynMode = true;
	
	
	public void setAsynMode(boolean asynMode) {
		this.asynMode = asynMode;
	}

	/**
	 * load config file from classloader root path
	 * @param fileName
	 * @return
	 */
	private InputStream loadConfigFromRootPath(String fileName) {
 		//InputStream is = CacheService.class.getClassLoader().getResourceAsStream(fileName);
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		if(is ==null) is = CacheService.class.getClassLoader().getResourceAsStream(fileName);
		if(is == null) is = ClassLoader.getSystemResourceAsStream(fileName);
		java.io.File f = new java.io.File(fileName);
		System.out.println(f.getAbsolutePath());
		return is;
	}
	

//	private static CacheService instance;	
//	public static CacheService getInstance() {
//		if (instance == null) {
//			synchronized (CacheService.class) {
//				instance = new CacheService();
//			}
//		}
//		return instance;
//	}

	/**
	 * mark private to make sure no new instance created in programmer way
	 */
	private CacheService () {
		System.out.println("############################CacheService initialization begin..............");
		try {
			InputStream is = loadConfigFromRootPath(EHCACHE_CONFIG);
			if (is == null) {
				CmLogger.getLogger().debug("ehcache config " + EHCACHE_CONFIG + " not found");
				return;
			}
			cacheManager = CacheManager.create(is);
			
		} catch (Exception e) {
			System.out.println("CacheService initialization failed.");
			CmLogger.getLogger().error(e);
		}
	}
	
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		springApplicationCtx = ctx;
		try {
			loadConfig();
		} catch (Exception e) {
			CmLogger.getLogger().error(e);
		}
	}
	
	/**
	 * load config from file and then fill cache base the config
	 * @throws Exception
	 */
	private void loadConfig() throws Exception {
		InputStream is = loadConfigFromRootPath(SERVICE_CONFIG);
		if (is == null){
			CmLogger.getLogger().debug("cache service config " + SERVICE_CONFIG + " not found.");
			return;
		}
		
		// 1. load xml & then parse
		List<CacheConfig> configs = parseConfig(is);
		
		// 2. refresh cache based on config
		for (CacheConfig cfg : configs) {
			Cache cache = cacheManager.getCache(cfg.getCacheName());
			if (cache != null) {
				// store config
				cacheConfigs.put(cfg.getCacheName(), cfg);
				
				// store status for each cache
				CacheStatus status = new CacheStatus();
				cacheStatusMap.put(cfg.getCacheName(), status);
				
				// store lock for each cache
				ReentrantLock lock = new ReentrantLock();
				cacheLocks.put(cfg.getCacheName(), lock);
				
				// enrich cache from db
				fillCache(cache, cfg, status, lock);
			} else {
				CmLogger.getLogger().debug("no cache [ " + cfg.getCacheName() + "] found.");
			}
		}
	}
	
	
	/**
	 * get loader method then will be invoked by reflection
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	private static Method getLoadMethod () throws SecurityException, NoSuchMethodException {
		return CacheLoader.class.getMethod("loadAll", new Class[]{});
	}
	
	/**
	 * parse cache service config
	 * @param configFile
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<CacheConfig> parseConfig(InputStream configFile) throws Exception {
		Document doc = getDocument(configFile);
		Element root = doc.getRootElement();
		List<Node> caches = root.selectNodes("cache");
		List<CacheConfig> configs = new ArrayList<CacheConfig>();
		for (Node ele : caches) {
			CacheConfig config = new CacheConfig();
			config.setCacheName(ele.valueOf("@name"));
			
			Node keyEle = ele.selectSingleNode("key");
			
			Class keyClz = Class.forName(keyEle.valueOf("@class"));
			
			config.setKeyClazz(keyClz);
			config.setKeyMethod(keyClz.getMethod(keyEle.valueOf("@method"), new Class[]{}));
			
			Node srcEle = ele.selectSingleNode("loader");
			Class srcClz = Class.forName(srcEle.valueOf("@class"));
			config.setLoaderClazz(srcClz);
			
			Node beanEle = ele.selectSingleNode("bean");
			if (beanEle != null)
				config.setBeanName(beanEle.valueOf("@id"));
			
			configs.add(config);
		}
		return configs;
	}
	
	/**
	 * get dom4j document from inputstream
	 * @param file
	 * @return
	 */
	private static Document getDocument(final InputStream file) {
		Document document = null;
		SAXReader reader = new SAXReader();
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * fill cache with the records from db based on config
	 * @param cache
	 * @param cfg
	 * @param status
	 * @param lock
	 * @return
	 */
	private boolean fillCache(Cache cache, CacheConfig cfg, CacheStatus status, ReentrantLock lock) {
		System.out.println("filling cache [" + cache.getName() + "]......");
		boolean result = false;
		try {
			//refreshCacheInternal(cache, cfg);
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					cache,
					cfg,
					status,
					lock,
					CacheRefreshingTask.ACTION_INIT
			);
			System.out.println("filling cache [" + cache.getName() + "]......");
			CmLogger.getLogger().debug("filling cache [" + cache.getName() + "]......");
			Future<Boolean> future = executorService.submit(task);
			if (!asynMode) {
				result = future.get();
			} else {
				result = true;
			}
		} catch (Exception e) {
			result = false;
			CmLogger.getLogger().debug("refresh cache [" + cache.getName() + "] failed.");
			CmLogger.getLogger().error(e);
			System.out.println("refresh cache [" + cache.getName() + "]......");
			e.printStackTrace();
		}
		return result;
		
	}
	
	/**
	 * get cache from cache manager by given name
	 * @param cacheName
	 * @return
	 */
	public Cache getCache(String cacheName) {
		
		CacheStatus status = cacheStatusMap.get(cacheName);
		if (status == null) {
			// no cache configed
			return null;
		}
		CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
		
		// TODO if cache have not started, raise error or wait?
		if (!status.available()) {
			while (true) {
				CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
				if (status.available()) {
					break;
				} else if (status.getStatus() == CacheStatus.STARTING) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] is starting...");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						CmLogger.getLogger().error(e);
					}
				} else if (status.getStatus() == CacheStatus.FAILED) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					throw new IllegalStateException("cache [ " + cacheName + " ] initialization failed");
				} else if (status.getStatus() == CacheStatus.STOPPED) {
					// never happen
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization not started");
					boolean success = this.refreshCache(cacheName, true);
					if (!success) {
						CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					}
					break;
				}
			}
		}
		Cache cache = cacheManager.getCache(cacheName);
		return cache;
	}
	
	/**
	 * add or replace the cache item into cache identified by the given name
	 * @param cacheName
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean addOrUpdateObject(String cacheName, Object key, Object value) {
		if (cacheName == null || key == null || value == null) {
			throw new IllegalArgumentException("arguments could not be null: cacheName"+cacheName+" ,key"+key+" ,value"+value);
		}
		Cache legacy = getCache(cacheName);
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		if (legacy.get(key) != null) {
			legacy.replace(new net.sf.ehcache.Element(key, value));
		} else {
			legacy.put(new net.sf.ehcache.Element(key, value));
		}
		return true;
	}
	
	/**
	 * remove the cached item from cache
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public boolean removeObject(String cacheName, Object key) {
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		return legacy.remove(key);
	}
	
	/**
	 * load the cached item by the key/id
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public Object getObjectById(String cacheName, Object key) {
		
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return null;
		}
		
		net.sf.ehcache.Element ele = legacy.get(key);
		if (ele != null) {
			return ele.getObjectValue();
		} else {
			CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
		}
		
		// no value object found
		return null;
	}
	
	/**
	 * full-refresh the cache from db
	 * <br/>
	 * <font color="red">
	 * NOTE:<br/>
	 * consider the method below firstly before using this method:
	 * </font><br/>
	 * <ol>
	 * <li> {@code CacheService#addOrUpdateObject(String, Object, Object)}({@link #addOrUpdateObject(String, Object, Object)})</li>
	 * <li> {@code CacheService#removeObject(String, Object)}({@link #removeObject(String, Object)})</li>
	 * </ol>
	 * @param cacheName
	 * @return
	 */
	public boolean refreshCache(String cacheName) {
		return refreshCache(cacheName, !asynMode);
	}
	
	public void refreshCacheAll() {
		String[] cachesNames = this.getCacheManager().getCacheNames();
		for (String cacheName : cachesNames) {
			refreshCache(cacheName, !asynMode);
		}
	}
	
	/**
	 * full-refresh the cache from DB in a synchronized/non-synchronized mode
	 * @param cacheName
	 * @param synMode
	 * @return
	 */
	private boolean refreshCache(String cacheName, boolean synMode) {
		if (cacheName == null) {
			throw new IllegalArgumentException("cacheName could not be null");
		}
		Cache legacy = cacheManager.getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		boolean result = false;
		try {
			
			//refreshCacheInternal(legacy, CacheConfigs.get(cacheName));
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					legacy,
					cacheConfigs.get(cacheName),
					cacheStatusMap.get(cacheName),
					cacheLocks.get(cacheName),
					CacheRefreshingTask.ACTION_REFRESH
			);
			Future<Boolean> future = executorService.submit( task );
			if (synMode) {
				result = future.get();
			} else {
				result = true;
			}
			
		} catch (Exception exp) {
			CmLogger.getLogger().debug("refresh Cache " + cacheName + "failed.");
			CmLogger.getLogger().error(exp);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated
	public void refreshCacheInternal(Cache cache, CacheConfig cfg) throws Exception {
		if (cache == null || cfg == null) {
			throw new IllegalArgumentException("cache or cacheConfig could not be null.");
		}
		
		Object obj = null;
		if (springApplicationCtx != null) {
			obj = getLoadMethod().invoke(springApplicationCtx.getBean(cfg.getBeanName()), new Object[]{});
		} else {
			obj = getLoadMethod().invoke(cfg.getLoaderClazz().newInstance(), new Object[]{});
		}
		if (obj instanceof List) {
			List keys = cache.getKeys();
			Set toBeRemoved = new HashSet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				toBeRemoved.add(it.next());
			}
			List list = (List) obj;
			List newKeys = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cfg.getKeyMethod().invoke(v, new Object[]{});
				newKeys.add(k);
				cache.put(new net.sf.ehcache.Element(k, v));
			}
			toBeRemoved.removeAll(newKeys);
			if (toBeRemoved != null && toBeRemoved.size() > 0) {
				for (Iterator it = toBeRemoved.iterator(); it.hasNext();) {
					Object k = it.next();
					cache.remove(k);
				}
			}
		}
	}
	
	/**
	 * load all cached item from the given cache
	 * @param <T>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		List<T> list = new ArrayList<T>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			list.add((T)cache.get(it.next()).getObjectValue());
		}
		
		return list;
	}
	
	/**
	 * load multiple elements by the given keys
	 * @param <T>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <T> List<T> getListByIds(String cacheName, Object ... keys) {

		List<T> list = new ArrayList<T>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return list;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				list.add((T)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return list;
	}
	/**
	 * load multiple elements stored in map by the given keys
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <K, V> Map<K, V>  getMapByIds(String cacheName, Object ... keys) {
		
		Map<K, V> map = new HashMap<K, V>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return map;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				map.put((K)key, (V)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return map;
	}
	
	/**
	 * load all cached items from the given cache
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		Map<K, V> map = new HashMap<K, V>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			K key = (K)it.next();
			map.put(key, (V)cache.get(key).getObjectValue());
		}
		
		return map;
	}
	
	
	/**
	 * thread pool for refreshing cache
	 */
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	/**
	 * class for storing cache status
	 */
	private static class CacheStatus {
		
		public static final int STOPPED = 1;
		public static final int STARTING = 1<<1;
		public static final int FAILED = 1<<2;
		
		public static final int RUNNING = 1<<4;
		public static final int REFRESHING = 1<<5;
		
		private volatile int status = STOPPED;
		
		public void setStatus(int stts) {
			this.status = stts;
		}
		public int getStatus() {
			return this.status;
		}
		public String getStatusDesc() {
			switch(this.status) {
			case STOPPED : return "Stopped";
			case STARTING: return "Starting";
			case FAILED  : return "Failed";
			case RUNNING : return "Running";
			case REFRESHING: return "Refreshing";
			default: return "Unknown";
			}
		}
		
		public boolean available() {
			return this.status >= RUNNING;
		}
		
	}
	
	/**
	 * refresh cache task
	 */
	private static class CacheRefreshingTask implements Callable<Boolean> {
		
		private static final int ACTION_INIT = 1<<1;
		private static final int ACTION_REFRESH = 1<<2;
		

		private ApplicationContext appCtx;
		private CacheConfig cacheConfig;
		private Cache cache;
		private CacheStatus status;
		private ReentrantLock lock;
		private int action;
		
		public CacheRefreshingTask(
				ApplicationContext appCtx, 
				Cache cache,
				CacheConfig cacheConfig,
				CacheStatus status,
				ReentrantLock lock,
				int action
		) {
			this.appCtx = appCtx;
			this.cache = cache;
			this.cacheConfig = cacheConfig;
			this.status = status;
			this.lock = lock;
			this.action = action;
		}

		public Boolean call() {
			
			if (cache == null || cacheConfig == null || lock == null) {
				return false;
			}
			
			Thread current = Thread.currentThread();
			String legacyThreadName = current.getName();
			current.setName(cacheConfig.getCacheName());
			int firstStatus = status.getStatus();
			
			// tryLock is incorrect, use lock instead
			// this.lock.tryLock();
			boolean result = false;
			this.lock.lock();
			try {
				if (action == ACTION_INIT) {
					status.setStatus(CacheStatus.STARTING);
				} else if (action == ACTION_REFRESH){
					if (firstStatus < CacheStatus.RUNNING) {
						status.setStatus(CacheStatus.STARTING);
					} else {
						status.setStatus(CacheStatus.REFRESHING);
					}
				}
				try {
					long start = System.currentTimeMillis();
					doRefresh();
					long stop = System.currentTimeMillis();
					CmLogger.getLogger().debug("fill Cache [" + cache.getName() + "] cost time :" + (stop - start) / 1.0e3 + " sec");
					status.setStatus(CacheStatus.RUNNING);
					result = true;
					
				} catch(Exception exp) {
					CmLogger.getLogger().error(exp);
					status.setStatus(CacheStatus.FAILED);
					// ignore exception
					result = false;
				}
			} finally {
				this.lock.unlock();
			}
			current.setName(legacyThreadName);
			return result;
		}
		
		/**
		 * refresh the cache from db
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		private void doRefresh() throws Exception {
			Object obj = null;
			if (appCtx != null) {
				CmLogger.getLogger().debug("doRefresh:use spring.");
				obj = getLoadMethod().invoke(appCtx.getBean(cacheConfig.getBeanName()), new Object[]{});
			} else {
				CmLogger.getLogger().debug("doRefresh:use loader.");
				obj = getLoadMethod().invoke(cacheConfig.getLoaderClazz().newInstance(), new Object[]{});
			}
			if (obj instanceof List) {
				this.doRefreshInternal((List) obj);
			} else {
				throw new IllegalStateException("the returned type not supported");
			}
		}
		@SuppressWarnings("unchecked")
		private void doRefreshInternal(List list) throws Exception {

			
			cache.removeAll();
			List<net.sf.ehcache.Element> eles = new ArrayList<net.sf.ehcache.Element>();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cacheConfig.getKeyMethod().invoke(v, new Object[]{});
				eles.add(new net.sf.ehcache.Element(k, v));
			}
			cache.putAll(eles);
		
		}
		
	}
	
	
}
package com.cm.basic.cache.local;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.cm.logging.CmLogger;


/**
 * To use this cache service transparently, please don't call this service directly except from service layer
 * which cross cut by transaction manager
 */
public class CacheService implements ApplicationContextAware, DisposableBean {

    public void destroy() {
        this.cacheConfigs.clear();
        this.cacheLocks.clear();
        this.cacheStatusMap.clear();
        this.cacheManager.shutdown();
        this.executorService.shutdownNow();
    }

	/** cacheService config file name **/
	public static final String SERVICE_CONFIG = "cacheService.xml";
	
	/** ehcache config file name **/
	public static final String EHCACHE_CONFIG = "ehcache.xml";
	
	/**
	 * spring application context
	 */
	private ApplicationContext springApplicationCtx = null;
	
	/**
	 * ehcache cache manager
	 * in current implemenation, use singleton cache manager, 
	 * thus the ehcache config file will not be deleted while full building happened
	 */
	private CacheManager cacheManager = null;
	
	/**
	 * cache service config collection
	 */
	private Map<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<String, CacheConfig>();

	/**
	 * cache lock collection
	 */
	private Map<String, ReentrantLock> cacheLocks = new ConcurrentHashMap<String, ReentrantLock>();
	
	/**
	 * cache status collection
	 */
	private Map<String, CacheStatus> cacheStatusMap = new ConcurrentHashMap<String, CacheStatus>();
	
	
	/**
	 * toggle for asynchronous cache initialization
	 */
	private boolean asynMode = true;
	
	
	public void setAsynMode(boolean asynMode) {
		this.asynMode = asynMode;
	}

	/**
	 * load config file from classloader root path
	 * @param fileName
	 * @return
	 */
	private InputStream loadConfigFromRootPath(String fileName) {
		InputStream is = CacheService.class.getClassLoader().getResourceAsStream(fileName);
		return is;
	}

//	private static CacheService instance;	
//	public static CacheService getInstance() {
//		if (instance == null) {
//			synchronized (CacheService.class) {
//				instance = new CacheService();
//			}
//		}
//		return instance;
//	}

	/**
	 * mark private to make sure no new instance created in programmer way
	 */
	private CacheService () {
		
		try {
			InputStream is = loadConfigFromRootPath(EHCACHE_CONFIG);
			if (is == null) {
				CmLogger.getLogger().debug("ehcache config " + EHCACHE_CONFIG + " not found");
				return;
			}
			cacheManager = CacheManager.create(is);
			
		} catch (Exception e) {
			System.out.println("CacheService initialization failed.");
			CmLogger.getLogger().error(e);
		}
	}
	
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		springApplicationCtx = ctx;
		try {
			loadConfig();
		} catch (Exception e) {
			CmLogger.getLogger().error(e);
		}
	}
	
	/**
	 * load config from file and then fill cache base the config
	 * @throws Exception
	 */
	private void loadConfig() throws Exception {
		InputStream is = loadConfigFromRootPath(SERVICE_CONFIG);
		if (is == null){
			CmLogger.getLogger().debug("cache service config " + SERVICE_CONFIG + " found.");
			return;
		}
		
		// 1. load xml & then parse
		List<CacheConfig> configs = parseConfig(is);
		
		// 2. refresh cache based on config
		for (CacheConfig cfg : configs) {
			Cache cache = cacheManager.getCache(cfg.getCacheName());
			if (cache != null) {
				// store config
				cacheConfigs.put(cfg.getCacheName(), cfg);
				
				// store status for each cache
				CacheStatus status = new CacheStatus();
				cacheStatusMap.put(cfg.getCacheName(), status);
				
				// store lock for each cache
				ReentrantLock lock = new ReentrantLock();
				cacheLocks.put(cfg.getCacheName(), lock);
				
				// enrich cache from db
				fillCache(cache, cfg, status, lock);
			} else {
				CmLogger.getLogger().debug("no cache [ " + cfg.getCacheName() + "] found.");
			}
		}
	}
	
	
	/**
	 * get loader method then will be invoked by reflection
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	private static Method getLoadMethod () throws SecurityException, NoSuchMethodException {
		return CacheLoader.class.getMethod("loadAll", new Class[]{});
	}
	
	/**
	 * parse cache service config
	 * @param configFile
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<CacheConfig> parseConfig(InputStream configFile) throws Exception {
		Document doc = getDocument(configFile);
		Element root = doc.getRootElement();
		List<Node> caches = root.selectNodes("cache");
		List<CacheConfig> configs = new ArrayList<CacheConfig>();
		for (Node ele : caches) {
			CacheConfig config = new CacheConfig();
			config.setCacheName(ele.valueOf("@name"));
			
			Node keyEle = ele.selectSingleNode("key");
			
			Class keyClz = Class.forName(keyEle.valueOf("@class"));
			
			config.setKeyClazz(keyClz);
			config.setKeyMethod(keyClz.getMethod(keyEle.valueOf("@method"), new Class[]{}));
			
			Node srcEle = ele.selectSingleNode("loader");
			Class srcClz = Class.forName(srcEle.valueOf("@class"));
			config.setLoaderClazz(srcClz);
			
			Node beanEle = ele.selectSingleNode("bean");
			if (beanEle != null)
				config.setBeanName(beanEle.valueOf("@id"));
			
			configs.add(config);
		}
		return configs;
	}
	
	/**
	 * get dom4j document from inputstream
	 * @param file
	 * @return
	 */
	private static Document getDocument(final InputStream file) {
		Document document = null;
		SAXReader reader = new SAXReader();
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * fill cache with the records from db based on config
	 * @param cache
	 * @param cfg
	 * @param status
	 * @param lock
	 * @return
	 */
	private boolean fillCache(Cache cache, CacheConfig cfg, CacheStatus status, ReentrantLock lock) {
		boolean result = false;
		try {
			//refreshCacheInternal(cache, cfg);
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					cache,
					cfg,
					status,
					lock,
					CacheRefreshingTask.ACTION_INIT
			);
			
			CmLogger.getLogger().debug("filling cache [" + cache.getName() + "]......");
			Future<Boolean> future = executorService.submit(task);
			if (!asynMode) {
				result = future.get();
			} else {
				result = true;
			}
		} catch (Exception e) {
			result = false;
			CmLogger.getLogger().debug("refresh cache [" + cache.getName() + "] failed.");
			CmLogger.getLogger().error(e);
		}
		return result;
		
	}
	
	/**
	 * get cache from cache manager by given name
	 * @param cacheName
	 * @return
	 */
	public Cache getCache(String cacheName) {
		
		CacheStatus status = cacheStatusMap.get(cacheName);
		if (status == null) {
			// no cache configed
			return null;
		}
		CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
		
		// TODO if cache have not started, raise error or wait?
		if (!status.available()) {
			while (true) {
				CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
				if (status.available()) {
					break;
				} else if (status.getStatus() == CacheStatus.STARTING) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] is starting...");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						CmLogger.getLogger().error(e);
					}
				} else if (status.getStatus() == CacheStatus.FAILED) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					throw new IllegalStateException("cache [ " + cacheName + " ] initialization failed");
				} else if (status.getStatus() == CacheStatus.STOPPED) {
					// never happen
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization not started");
					boolean success = this.refreshCache(cacheName, true);
					if (!success) {
						CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					}
					break;
				}
			}
		}
		Cache cache = cacheManager.getCache(cacheName);
		return cache;
	}
	
	/**
	 * add or replace the cache item into cache identified by the given name
	 * @param cacheName
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean addOrUpdateObject(String cacheName, Object key, Object value) {
		if (cacheName == null || key == null || value == null) {
			throw new IllegalArgumentException("arguments could not be null: cacheName"+cacheName+" ,key"+key+" ,value"+value);
		}
		Cache legacy = getCache(cacheName);
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		if (legacy.get(key) != null) {
			legacy.replace(new net.sf.ehcache.Element(key, value));
		} else {
			legacy.put(new net.sf.ehcache.Element(key, value));
		}
		return true;
	}
	
	/**
	 * remove the cached item from cache
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public boolean removeObject(String cacheName, Object key) {
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		return legacy.remove(key);
	}
	
	/**
	 * load the cached item by the key/id
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public Object getObjectById(String cacheName, Object key) {
		
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return null;
		}
		
		net.sf.ehcache.Element ele = legacy.get(key);
		if (ele != null) {
			return ele.getObjectValue();
		} else {
			CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
		}
		
		// no value object found
		return null;
	}
	
	/**
	 * full-refresh the cache from db
	 * <br/>
	 * <font color="red">
	 * NOTE:<br/>
	 * consider the method below firstly before using this method:
	 * </font><br/>
	 * <ol>
	 * <li> {@code CacheService#addOrUpdateObject(String, Object, Object)}({@link #addOrUpdateObject(String, Object, Object)})</li>
	 * <li> {@code CacheService#removeObject(String, Object)}({@link #removeObject(String, Object)})</li>
	 * </ol>
	 * @param cacheName
	 * @return
	 */
	public boolean refreshCache(String cacheName) {
		return refreshCache(cacheName, !asynMode);
	}
	
	public void refreshCacheAll() {
		String[] cachesNames = this.getCacheManager().getCacheNames();
		for (String cacheName : cachesNames) {
			refreshCache(cacheName, !asynMode);
		}
	}
	
	/**
	 * full-refresh the cache from DB in a synchronized/non-synchronized mode
	 * @param cacheName
	 * @param synMode
	 * @return
	 */
	private boolean refreshCache(String cacheName, boolean synMode) {
		if (cacheName == null) {
			throw new IllegalArgumentException("cacheName could not be null");
		}
		Cache legacy = cacheManager.getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		boolean result = false;
		try {
			
			//refreshCacheInternal(legacy, CacheConfigs.get(cacheName));
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					legacy,
					cacheConfigs.get(cacheName),
					cacheStatusMap.get(cacheName),
					cacheLocks.get(cacheName),
					CacheRefreshingTask.ACTION_REFRESH
			);
			Future<Boolean> future = executorService.submit( task );
			if (synMode) {
				result = future.get();
			} else {
				result = true;
			}
			
		} catch (Exception exp) {
			CmLogger.getLogger().debug("refresh Cache " + cacheName + "failed.");
			CmLogger.getLogger().error(exp);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated
	public void refreshCacheInternal(Cache cache, CacheConfig cfg) throws Exception {
		if (cache == null || cfg == null) {
			throw new IllegalArgumentException("cache or cacheConfig could not be null.");
		}
		
		Object obj = null;
		if (springApplicationCtx != null) {
			obj = getLoadMethod().invoke(springApplicationCtx.getBean(cfg.getBeanName()), new Object[]{});
		} else {
			obj = getLoadMethod().invoke(cfg.getLoaderClazz().newInstance(), new Object[]{});
		}
		if (obj instanceof List) {
			List keys = cache.getKeys();
			Set toBeRemoved = new HashSet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				toBeRemoved.add(it.next());
			}
			List list = (List) obj;
			List newKeys = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cfg.getKeyMethod().invoke(v, new Object[]{});
				newKeys.add(k);
				cache.put(new net.sf.ehcache.Element(k, v));
			}
			toBeRemoved.removeAll(newKeys);
			if (toBeRemoved != null && toBeRemoved.size() > 0) {
				for (Iterator it = toBeRemoved.iterator(); it.hasNext();) {
					Object k = it.next();
					cache.remove(k);
				}
			}
		}
	}
	
	/**
	 * load all cached item from the given cache
	 * @param <T>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		List<T> list = new ArrayList<T>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			list.add((T)cache.get(it.next()).getObjectValue());
		}
		
		return list;
	}
	
	/**
	 * load multiple elements by the given keys
	 * @param <T>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <T> List<T> getListByIds(String cacheName, Object ... keys) {

		List<T> list = new ArrayList<T>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return list;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				list.add((T)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return list;
	}
	/**
	 * load multiple elements stored in map by the given keys
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <K, V> Map<K, V>  getMapByIds(String cacheName, Object ... keys) {
		
		Map<K, V> map = new HashMap<K, V>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return map;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				map.put((K)key, (V)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return map;
	}
	
	/**
	 * load all cached items from the given cache
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		Map<K, V> map = new HashMap<K, V>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			K key = (K)it.next();
			map.put(key, (V)cache.get(key).getObjectValue());
		}
		
		return map;
	}
	
	
	/**
	 * thread pool for refreshing cache
	 */
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	/**
	 * class for storing cache status
	 */
	private static class CacheStatus {
		
		public static final int STOPPED = 1;
		public static final int STARTING = 1<<1;
		public static final int FAILED = 1<<2;
		
		public static final int RUNNING = 1<<4;
		public static final int REFRESHING = 1<<5;
		
		private volatile int status = STOPPED;
		
		public void setStatus(int stts) {
			this.status = stts;
		}
		public int getStatus() {
			return this.status;
		}
		public String getStatusDesc() {
			switch(this.status) {
			case STOPPED : return "Stopped";
			case STARTING: return "Starting";
			case FAILED  : return "Failed";
			case RUNNING : return "Running";
			case REFRESHING: return "Refreshing";
			default: return "Unknown";
			}
		}
		
		public boolean available() {
			return this.status >= RUNNING;
		}
		
	}
	
	/**
	 * refresh cache task
	 */
	private static class CacheRefreshingTask implements Callable<Boolean> {
		
		private static final int ACTION_INIT = 1<<1;
		private static final int ACTION_REFRESH = 1<<2;
		

		private ApplicationContext appCtx;
		private CacheConfig cacheConfig;
		private Cache cache;
		private CacheStatus status;
		private ReentrantLock lock;
		private int action;
		
		public CacheRefreshingTask(
				ApplicationContext appCtx, 
				Cache cache,
				CacheConfig cacheConfig,
				CacheStatus status,
				ReentrantLock lock,
				int action
		) {
			this.appCtx = appCtx;
			this.cache = cache;
			this.cacheConfig = cacheConfig;
			this.status = status;
			this.lock = lock;
			this.action = action;
		}

		public Boolean call() {
			
			if (cache == null || cacheConfig == null || lock == null) {
				return false;
			}
			
			Thread current = Thread.currentThread();
			String legacyThreadName = current.getName();
			current.setName(cacheConfig.getCacheName());
			int firstStatus = status.getStatus();
			
			// tryLock is incorrect, use lock instead
			// this.lock.tryLock();
			boolean result = false;
			this.lock.lock();
			try {
				if (action == ACTION_INIT) {
					status.setStatus(CacheStatus.STARTING);
				} else if (action == ACTION_REFRESH){
					if (firstStatus < CacheStatus.RUNNING) {
						status.setStatus(CacheStatus.STARTING);
					} else {
						status.setStatus(CacheStatus.REFRESHING);
					}
				}
				try {
					long start = System.currentTimeMillis();
					doRefresh();
					long stop = System.currentTimeMillis();
					CmLogger.getLogger().debug("fill Cache [" + cache.getName() + "] cost time :" + (stop - start) / 1.0e3 + " sec");
					status.setStatus(CacheStatus.RUNNING);
					result = true;
					
				} catch(Exception exp) {
					CmLogger.getLogger().error(exp);
					status.setStatus(CacheStatus.FAILED);
					// ignore exception
					result = false;
				}
			} finally {
				this.lock.unlock();
			}
			current.setName(legacyThreadName);
			return result;
		}
		
		/**
		 * refresh the cache from db
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		private void doRefresh() throws Exception {
			Object obj = null;
			if (appCtx != null) {
				CmLogger.getLogger().debug("doRefresh:use spring.");
				obj = getLoadMethod().invoke(appCtx.getBean(cacheConfig.getBeanName()), new Object[]{});
			} else {
				CmLogger.getLogger().debug("doRefresh:use loader.");
				obj = getLoadMethod().invoke(cacheConfig.getLoaderClazz().newInstance(), new Object[]{});
			}
			if (obj instanceof List) {
				this.doRefreshInternal((List) obj);
			} else {
				throw new IllegalStateException("the returned type not supported");
			}
		}
		@SuppressWarnings("unchecked")
		private void doRefreshInternal(List list) throws Exception {

			
			cache.removeAll();
			List<net.sf.ehcache.Element> eles = new ArrayList<net.sf.ehcache.Element>();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cacheConfig.getKeyMethod().invoke(v, new Object[]{});
				eles.add(new net.sf.ehcache.Element(k, v));
			}
			cache.putAll(eles);
		
		}
		
	}
	
	
}
package com.cm.basic.cache.local;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.cm.logging.CmLogger;


/**
 * To use this cache service transparently, please don't call this service directly except from service layer
 * which cross cut by transaction manager
 */
public class CacheService implements ApplicationContextAware, DisposableBean {

    public void destroy() {
        this.cacheConfigs.clear();
        this.cacheLocks.clear();
        this.cacheStatusMap.clear();
        this.cacheManager.shutdown();
        this.executorService.shutdownNow();
    }

	/** cacheService config file name **/
	public static final String SERVICE_CONFIG = "cacheService.xml";
	
	/** ehcache config file name **/
	public static final String EHCACHE_CONFIG = "ehcache.xml";
	
	/**
	 * spring application context
	 */
	private ApplicationContext springApplicationCtx = null;
	
	/**
	 * ehcache cache manager
	 * in current implemenation, use singleton cache manager, 
	 * thus the ehcache config file will not be deleted while full building happened
	 */
	private CacheManager cacheManager = null;
	
	/**
	 * cache service config collection
	 */
	private Map<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<String, CacheConfig>();

	/**
	 * cache lock collection
	 */
	private Map<String, ReentrantLock> cacheLocks = new ConcurrentHashMap<String, ReentrantLock>();
	
	/**
	 * cache status collection
	 */
	private Map<String, CacheStatus> cacheStatusMap = new ConcurrentHashMap<String, CacheStatus>();
	
	
	/**
	 * toggle for asynchronous cache initialization
	 */
	private boolean asynMode = true;
	
	
	public void setAsynMode(boolean asynMode) {
		this.asynMode = asynMode;
	}

	/**
	 * load config file from classloader root path
	 * @param fileName
	 * @return
	 */
	private InputStream loadConfigFromRootPath(String fileName) {
		InputStream is = CacheService.class.getClassLoader().getResourceAsStream(fileName);
		return is;
	}

//	private static CacheService instance;	
//	public static CacheService getInstance() {
//		if (instance == null) {
//			synchronized (CacheService.class) {
//				instance = new CacheService();
//			}
//		}
//		return instance;
//	}

	/**
	 * mark private to make sure no new instance created in programmer way
	 */
	private CacheService () {
		
		try {
			InputStream is = loadConfigFromRootPath(EHCACHE_CONFIG);
			if (is == null) {
				CmLogger.getLogger().debug("ehcache config " + EHCACHE_CONFIG + " not found");
				return;
			}
			cacheManager = CacheManager.create(is);
			
		} catch (Exception e) {
			System.out.println("CacheService initialization failed.");
			CmLogger.getLogger().error(e);
		}
	}
	
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		springApplicationCtx = ctx;
		try {
			loadConfig();
		} catch (Exception e) {
			CmLogger.getLogger().error(e);
		}
	}
	
	/**
	 * load config from file and then fill cache base the config
	 * @throws Exception
	 */
	private void loadConfig() throws Exception {
		InputStream is = loadConfigFromRootPath(SERVICE_CONFIG);
		if (is == null){
			CmLogger.getLogger().debug("cache service config " + SERVICE_CONFIG + " found.");
			return;
		}
		
		// 1. load xml & then parse
		List<CacheConfig> configs = parseConfig(is);
		
		// 2. refresh cache based on config
		for (CacheConfig cfg : configs) {
			Cache cache = cacheManager.getCache(cfg.getCacheName());
			if (cache != null) {
				// store config
				cacheConfigs.put(cfg.getCacheName(), cfg);
				
				// store status for each cache
				CacheStatus status = new CacheStatus();
				cacheStatusMap.put(cfg.getCacheName(), status);
				
				// store lock for each cache
				ReentrantLock lock = new ReentrantLock();
				cacheLocks.put(cfg.getCacheName(), lock);
				
				// enrich cache from db
				fillCache(cache, cfg, status, lock);
			} else {
				CmLogger.getLogger().debug("no cache [ " + cfg.getCacheName() + "] found.");
			}
		}
	}
	
	
	/**
	 * get loader method then will be invoked by reflection
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	private static Method getLoadMethod () throws SecurityException, NoSuchMethodException {
		return CacheLoader.class.getMethod("loadAll", new Class[]{});
	}
	
	/**
	 * parse cache service config
	 * @param configFile
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<CacheConfig> parseConfig(InputStream configFile) throws Exception {
		Document doc = getDocument(configFile);
		Element root = doc.getRootElement();
		List<Node> caches = root.selectNodes("cache");
		List<CacheConfig> configs = new ArrayList<CacheConfig>();
		for (Node ele : caches) {
			CacheConfig config = new CacheConfig();
			config.setCacheName(ele.valueOf("@name"));
			
			Node keyEle = ele.selectSingleNode("key");
			
			Class keyClz = Class.forName(keyEle.valueOf("@class"));
			
			config.setKeyClazz(keyClz);
			config.setKeyMethod(keyClz.getMethod(keyEle.valueOf("@method"), new Class[]{}));
			
			Node srcEle = ele.selectSingleNode("loader");
			Class srcClz = Class.forName(srcEle.valueOf("@class"));
			config.setLoaderClazz(srcClz);
			
			Node beanEle = ele.selectSingleNode("bean");
			if (beanEle != null)
				config.setBeanName(beanEle.valueOf("@id"));
			
			configs.add(config);
		}
		return configs;
	}
	
	/**
	 * get dom4j document from inputstream
	 * @param file
	 * @return
	 */
	private static Document getDocument(final InputStream file) {
		Document document = null;
		SAXReader reader = new SAXReader();
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * fill cache with the records from db based on config
	 * @param cache
	 * @param cfg
	 * @param status
	 * @param lock
	 * @return
	 */
	private boolean fillCache(Cache cache, CacheConfig cfg, CacheStatus status, ReentrantLock lock) {
		boolean result = false;
		try {
			//refreshCacheInternal(cache, cfg);
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					cache,
					cfg,
					status,
					lock,
					CacheRefreshingTask.ACTION_INIT
			);
			
			CmLogger.getLogger().debug("filling cache [" + cache.getName() + "]......");
			Future<Boolean> future = executorService.submit(task);
			if (!asynMode) {
				result = future.get();
			} else {
				result = true;
			}
		} catch (Exception e) {
			result = false;
			CmLogger.getLogger().debug("refresh cache [" + cache.getName() + "] failed.");
			CmLogger.getLogger().error(e);
		}
		return result;
		
	}
	
	/**
	 * get cache from cache manager by given name
	 * @param cacheName
	 * @return
	 */
	public Cache getCache(String cacheName) {
		
		CacheStatus status = cacheStatusMap.get(cacheName);
		if (status == null) {
			// no cache configed
			return null;
		}
		CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
		
		// TODO if cache have not started, raise error or wait?
		if (!status.available()) {
			while (true) {
				CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
				if (status.available()) {
					break;
				} else if (status.getStatus() == CacheStatus.STARTING) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] is starting...");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						CmLogger.getLogger().error(e);
					}
				} else if (status.getStatus() == CacheStatus.FAILED) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					throw new IllegalStateException("cache [ " + cacheName + " ] initialization failed");
				} else if (status.getStatus() == CacheStatus.STOPPED) {
					// never happen
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization not started");
					boolean success = this.refreshCache(cacheName, true);
					if (!success) {
						CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					}
					break;
				}
			}
		}
		Cache cache = cacheManager.getCache(cacheName);
		return cache;
	}
	
	/**
	 * add or replace the cache item into cache identified by the given name
	 * @param cacheName
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean addOrUpdateObject(String cacheName, Object key, Object value) {
		if (cacheName == null || key == null || value == null) {
			throw new IllegalArgumentException("arguments could not be null: cacheName"+cacheName+" ,key"+key+" ,value"+value);
		}
		Cache legacy = getCache(cacheName);
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		if (legacy.get(key) != null) {
			legacy.replace(new net.sf.ehcache.Element(key, value));
		} else {
			legacy.put(new net.sf.ehcache.Element(key, value));
		}
		return true;
	}
	
	/**
	 * remove the cached item from cache
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public boolean removeObject(String cacheName, Object key) {
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		return legacy.remove(key);
	}
	
	/**
	 * load the cached item by the key/id
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public Object getObjectById(String cacheName, Object key) {
		
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return null;
		}
		
		net.sf.ehcache.Element ele = legacy.get(key);
		if (ele != null) {
			return ele.getObjectValue();
		} else {
			CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
		}
		
		// no value object found
		return null;
	}
	
	/**
	 * full-refresh the cache from db
	 * <br/>
	 * <font color="red">
	 * NOTE:<br/>
	 * consider the method below firstly before using this method:
	 * </font><br/>
	 * <ol>
	 * <li> {@code CacheService#addOrUpdateObject(String, Object, Object)}({@link #addOrUpdateObject(String, Object, Object)})</li>
	 * <li> {@code CacheService#removeObject(String, Object)}({@link #removeObject(String, Object)})</li>
	 * </ol>
	 * @param cacheName
	 * @return
	 */
	public boolean refreshCache(String cacheName) {
		return refreshCache(cacheName, !asynMode);
	}
	
	public void refreshCacheAll() {
		String[] cachesNames = this.getCacheManager().getCacheNames();
		for (String cacheName : cachesNames) {
			refreshCache(cacheName, !asynMode);
		}
	}
	
	/**
	 * full-refresh the cache from DB in a synchronized/non-synchronized mode
	 * @param cacheName
	 * @param synMode
	 * @return
	 */
	private boolean refreshCache(String cacheName, boolean synMode) {
		if (cacheName == null) {
			throw new IllegalArgumentException("cacheName could not be null");
		}
		Cache legacy = cacheManager.getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		boolean result = false;
		try {
			
			//refreshCacheInternal(legacy, CacheConfigs.get(cacheName));
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					legacy,
					cacheConfigs.get(cacheName),
					cacheStatusMap.get(cacheName),
					cacheLocks.get(cacheName),
					CacheRefreshingTask.ACTION_REFRESH
			);
			Future<Boolean> future = executorService.submit( task );
			if (synMode) {
				result = future.get();
			} else {
				result = true;
			}
			
		} catch (Exception exp) {
			CmLogger.getLogger().debug("refresh Cache " + cacheName + "failed.");
			CmLogger.getLogger().error(exp);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated
	public void refreshCacheInternal(Cache cache, CacheConfig cfg) throws Exception {
		if (cache == null || cfg == null) {
			throw new IllegalArgumentException("cache or cacheConfig could not be null.");
		}
		
		Object obj = null;
		if (springApplicationCtx != null) {
			obj = getLoadMethod().invoke(springApplicationCtx.getBean(cfg.getBeanName()), new Object[]{});
		} else {
			obj = getLoadMethod().invoke(cfg.getLoaderClazz().newInstance(), new Object[]{});
		}
		if (obj instanceof List) {
			List keys = cache.getKeys();
			Set toBeRemoved = new HashSet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				toBeRemoved.add(it.next());
			}
			List list = (List) obj;
			List newKeys = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cfg.getKeyMethod().invoke(v, new Object[]{});
				newKeys.add(k);
				cache.put(new net.sf.ehcache.Element(k, v));
			}
			toBeRemoved.removeAll(newKeys);
			if (toBeRemoved != null && toBeRemoved.size() > 0) {
				for (Iterator it = toBeRemoved.iterator(); it.hasNext();) {
					Object k = it.next();
					cache.remove(k);
				}
			}
		}
	}
	
	/**
	 * load all cached item from the given cache
	 * @param <T>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		List<T> list = new ArrayList<T>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			list.add((T)cache.get(it.next()).getObjectValue());
		}
		
		return list;
	}
	
	/**
	 * load multiple elements by the given keys
	 * @param <T>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <T> List<T> getListByIds(String cacheName, Object ... keys) {

		List<T> list = new ArrayList<T>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return list;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				list.add((T)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return list;
	}
	/**
	 * load multiple elements stored in map by the given keys
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <K, V> Map<K, V>  getMapByIds(String cacheName, Object ... keys) {
		
		Map<K, V> map = new HashMap<K, V>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return map;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				map.put((K)key, (V)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return map;
	}
	
	/**
	 * load all cached items from the given cache
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		Map<K, V> map = new HashMap<K, V>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			K key = (K)it.next();
			map.put(key, (V)cache.get(key).getObjectValue());
		}
		
		return map;
	}
	
	
	/**
	 * thread pool for refreshing cache
	 */
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	/**
	 * class for storing cache status
	 */
	private static class CacheStatus {
		
		public static final int STOPPED = 1;
		public static final int STARTING = 1<<1;
		public static final int FAILED = 1<<2;
		
		public static final int RUNNING = 1<<4;
		public static final int REFRESHING = 1<<5;
		
		private volatile int status = STOPPED;
		
		public void setStatus(int stts) {
			this.status = stts;
		}
		public int getStatus() {
			return this.status;
		}
		public String getStatusDesc() {
			switch(this.status) {
			case STOPPED : return "Stopped";
			case STARTING: return "Starting";
			case FAILED  : return "Failed";
			case RUNNING : return "Running";
			case REFRESHING: return "Refreshing";
			default: return "Unknown";
			}
		}
		
		public boolean available() {
			return this.status >= RUNNING;
		}
		
	}
	
	/**
	 * refresh cache task
	 */
	private static class CacheRefreshingTask implements Callable<Boolean> {
		
		private static final int ACTION_INIT = 1<<1;
		private static final int ACTION_REFRESH = 1<<2;
		

		private ApplicationContext appCtx;
		private CacheConfig cacheConfig;
		private Cache cache;
		private CacheStatus status;
		private ReentrantLock lock;
		private int action;
		
		public CacheRefreshingTask(
				ApplicationContext appCtx, 
				Cache cache,
				CacheConfig cacheConfig,
				CacheStatus status,
				ReentrantLock lock,
				int action
		) {
			this.appCtx = appCtx;
			this.cache = cache;
			this.cacheConfig = cacheConfig;
			this.status = status;
			this.lock = lock;
			this.action = action;
		}

		public Boolean call() {
			
			if (cache == null || cacheConfig == null || lock == null) {
				return false;
			}
			
			Thread current = Thread.currentThread();
			String legacyThreadName = current.getName();
			current.setName(cacheConfig.getCacheName());
			int firstStatus = status.getStatus();
			
			// tryLock is incorrect, use lock instead
			// this.lock.tryLock();
			boolean result = false;
			this.lock.lock();
			try {
				if (action == ACTION_INIT) {
					status.setStatus(CacheStatus.STARTING);
				} else if (action == ACTION_REFRESH){
					if (firstStatus < CacheStatus.RUNNING) {
						status.setStatus(CacheStatus.STARTING);
					} else {
						status.setStatus(CacheStatus.REFRESHING);
					}
				}
				try {
					long start = System.currentTimeMillis();
					doRefresh();
					long stop = System.currentTimeMillis();
					CmLogger.getLogger().debug("fill Cache [" + cache.getName() + "] cost time :" + (stop - start) / 1.0e3 + " sec");
					status.setStatus(CacheStatus.RUNNING);
					result = true;
					
				} catch(Exception exp) {
					CmLogger.getLogger().error(exp);
					status.setStatus(CacheStatus.FAILED);
					// ignore exception
					result = false;
				}
			} finally {
				this.lock.unlock();
			}
			current.setName(legacyThreadName);
			return result;
		}
		
		/**
		 * refresh the cache from db
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		private void doRefresh() throws Exception {
			Object obj = null;
			if (appCtx != null) {
				CmLogger.getLogger().debug("doRefresh:use spring.");
				obj = getLoadMethod().invoke(appCtx.getBean(cacheConfig.getBeanName()), new Object[]{});
			} else {
				CmLogger.getLogger().debug("doRefresh:use loader.");
				obj = getLoadMethod().invoke(cacheConfig.getLoaderClazz().newInstance(), new Object[]{});
			}
			if (obj instanceof List) {
				this.doRefreshInternal((List) obj);
			} else {
				throw new IllegalStateException("the returned type not supported");
			}
		}
		@SuppressWarnings("unchecked")
		private void doRefreshInternal(List list) throws Exception {

			
			cache.removeAll();
			List<net.sf.ehcache.Element> eles = new ArrayList<net.sf.ehcache.Element>();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cacheConfig.getKeyMethod().invoke(v, new Object[]{});
				eles.add(new net.sf.ehcache.Element(k, v));
			}
			cache.putAll(eles);
		
		}
		
	}
	
	
}
package com.cm.basic.cache.local;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.cm.logging.CmLogger;


/**
 * To use this cache service transparently, please don't call this service directly except from service layer
 * which cross cut by transaction manager
 */
public class CacheService implements ApplicationContextAware, DisposableBean {

    public void destroy() {
        this.cacheConfigs.clear();
        this.cacheLocks.clear();
        this.cacheStatusMap.clear();
        this.cacheManager.shutdown();
        this.executorService.shutdownNow();
    }

	/** cacheService config file name **/
	public static final String SERVICE_CONFIG = "cacheService.xml";
	
	/** ehcache config file name **/
	public static final String EHCACHE_CONFIG = "ehcache.xml";
	
	/**
	 * spring application context
	 */
	private ApplicationContext springApplicationCtx = null;
	
	/**
	 * ehcache cache manager
	 * in current implemenation, use singleton cache manager, 
	 * thus the ehcache config file will not be deleted while full building happened
	 */
	private CacheManager cacheManager = null;
	
	/**
	 * cache service config collection
	 */
	private Map<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<String, CacheConfig>();

	/**
	 * cache lock collection
	 */
	private Map<String, ReentrantLock> cacheLocks = new ConcurrentHashMap<String, ReentrantLock>();
	
	/**
	 * cache status collection
	 */
	private Map<String, CacheStatus> cacheStatusMap = new ConcurrentHashMap<String, CacheStatus>();
	
	
	/**
	 * toggle for asynchronous cache initialization
	 */
	private boolean asynMode = true;
	
	
	public void setAsynMode(boolean asynMode) {
		this.asynMode = asynMode;
	}

	/**
	 * load config file from classloader root path
	 * @param fileName
	 * @return
	 */
	private InputStream loadConfigFromRootPath(String fileName) {
		InputStream is = CacheService.class.getClassLoader().getResourceAsStream(fileName);
		return is;
	}

//	private static CacheService instance;	
//	public static CacheService getInstance() {
//		if (instance == null) {
//			synchronized (CacheService.class) {
//				instance = new CacheService();
//			}
//		}
//		return instance;
//	}

	/**
	 * mark private to make sure no new instance created in programmer way
	 */
	private CacheService () {
		
		try {
			InputStream is = loadConfigFromRootPath(EHCACHE_CONFIG);
			if (is == null) {
				CmLogger.getLogger().debug("ehcache config " + EHCACHE_CONFIG + " not found");
				return;
			}
			cacheManager = CacheManager.create(is);
			
		} catch (Exception e) {
			System.out.println("CacheService initialization failed.");
			CmLogger.getLogger().error(e);
		}
	}
	
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		springApplicationCtx = ctx;
		try {
			loadConfig();
		} catch (Exception e) {
			CmLogger.getLogger().error(e);
		}
	}
	
	/**
	 * load config from file and then fill cache base the config
	 * @throws Exception
	 */
	private void loadConfig() throws Exception {
		InputStream is = loadConfigFromRootPath(SERVICE_CONFIG);
		if (is == null){
			CmLogger.getLogger().debug("cache service config " + SERVICE_CONFIG + " found.");
			return;
		}
		
		// 1. load xml & then parse
		List<CacheConfig> configs = parseConfig(is);
		
		// 2. refresh cache based on config
		for (CacheConfig cfg : configs) {
			Cache cache = cacheManager.getCache(cfg.getCacheName());
			if (cache != null) {
				// store config
				cacheConfigs.put(cfg.getCacheName(), cfg);
				
				// store status for each cache
				CacheStatus status = new CacheStatus();
				cacheStatusMap.put(cfg.getCacheName(), status);
				
				// store lock for each cache
				ReentrantLock lock = new ReentrantLock();
				cacheLocks.put(cfg.getCacheName(), lock);
				
				// enrich cache from db
				fillCache(cache, cfg, status, lock);
			} else {
				CmLogger.getLogger().debug("no cache [ " + cfg.getCacheName() + "] found.");
			}
		}
	}
	
	
	/**
	 * get loader method then will be invoked by reflection
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	private static Method getLoadMethod () throws SecurityException, NoSuchMethodException {
		return CacheLoader.class.getMethod("loadAll", new Class[]{});
	}
	
	/**
	 * parse cache service config
	 * @param configFile
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<CacheConfig> parseConfig(InputStream configFile) throws Exception {
		Document doc = getDocument(configFile);
		Element root = doc.getRootElement();
		List<Node> caches = root.selectNodes("cache");
		List<CacheConfig> configs = new ArrayList<CacheConfig>();
		for (Node ele : caches) {
			CacheConfig config = new CacheConfig();
			config.setCacheName(ele.valueOf("@name"));
			
			Node keyEle = ele.selectSingleNode("key");
			
			Class keyClz = Class.forName(keyEle.valueOf("@class"));
			
			config.setKeyClazz(keyClz);
			config.setKeyMethod(keyClz.getMethod(keyEle.valueOf("@method"), new Class[]{}));
			
			Node srcEle = ele.selectSingleNode("loader");
			Class srcClz = Class.forName(srcEle.valueOf("@class"));
			config.setLoaderClazz(srcClz);
			
			Node beanEle = ele.selectSingleNode("bean");
			if (beanEle != null)
				config.setBeanName(beanEle.valueOf("@id"));
			
			configs.add(config);
		}
		return configs;
	}
	
	/**
	 * get dom4j document from inputstream
	 * @param file
	 * @return
	 */
	private static Document getDocument(final InputStream file) {
		Document document = null;
		SAXReader reader = new SAXReader();
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * fill cache with the records from db based on config
	 * @param cache
	 * @param cfg
	 * @param status
	 * @param lock
	 * @return
	 */
	private boolean fillCache(Cache cache, CacheConfig cfg, CacheStatus status, ReentrantLock lock) {
		boolean result = false;
		try {
			//refreshCacheInternal(cache, cfg);
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					cache,
					cfg,
					status,
					lock,
					CacheRefreshingTask.ACTION_INIT
			);
			
			CmLogger.getLogger().debug("filling cache [" + cache.getName() + "]......");
			Future<Boolean> future = executorService.submit(task);
			if (!asynMode) {
				result = future.get();
			} else {
				result = true;
			}
		} catch (Exception e) {
			result = false;
			CmLogger.getLogger().debug("refresh cache [" + cache.getName() + "] failed.");
			CmLogger.getLogger().error(e);
		}
		return result;
		
	}
	
	/**
	 * get cache from cache manager by given name
	 * @param cacheName
	 * @return
	 */
	public Cache getCache(String cacheName) {
		
		CacheStatus status = cacheStatusMap.get(cacheName);
		if (status == null) {
			// no cache configed
			return null;
		}
		CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
		
		// TODO if cache have not started, raise error or wait?
		if (!status.available()) {
			while (true) {
				CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
				if (status.available()) {
					break;
				} else if (status.getStatus() == CacheStatus.STARTING) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] is starting...");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						CmLogger.getLogger().error(e);
					}
				} else if (status.getStatus() == CacheStatus.FAILED) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					throw new IllegalStateException("cache [ " + cacheName + " ] initialization failed");
				} else if (status.getStatus() == CacheStatus.STOPPED) {
					// never happen
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization not started");
					boolean success = this.refreshCache(cacheName, true);
					if (!success) {
						CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					}
					break;
				}
			}
		}
		Cache cache = cacheManager.getCache(cacheName);
		return cache;
	}
	
	/**
	 * add or replace the cache item into cache identified by the given name
	 * @param cacheName
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean addOrUpdateObject(String cacheName, Object key, Object value) {
		if (cacheName == null || key == null || value == null) {
			throw new IllegalArgumentException("arguments could not be null: cacheName"+cacheName+" ,key"+key+" ,value"+value);
		}
		Cache legacy = getCache(cacheName);
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		if (legacy.get(key) != null) {
			legacy.replace(new net.sf.ehcache.Element(key, value));
		} else {
			legacy.put(new net.sf.ehcache.Element(key, value));
		}
		return true;
	}
	
	/**
	 * remove the cached item from cache
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public boolean removeObject(String cacheName, Object key) {
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		return legacy.remove(key);
	}
	
	/**
	 * load the cached item by the key/id
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public Object getObjectById(String cacheName, Object key) {
		
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return null;
		}
		
		net.sf.ehcache.Element ele = legacy.get(key);
		if (ele != null) {
			return ele.getObjectValue();
		} else {
			CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
		}
		
		// no value object found
		return null;
	}
	
	/**
	 * full-refresh the cache from db
	 * <br/>
	 * <font color="red">
	 * NOTE:<br/>
	 * consider the method below firstly before using this method:
	 * </font><br/>
	 * <ol>
	 * <li> {@code CacheService#addOrUpdateObject(String, Object, Object)}({@link #addOrUpdateObject(String, Object, Object)})</li>
	 * <li> {@code CacheService#removeObject(String, Object)}({@link #removeObject(String, Object)})</li>
	 * </ol>
	 * @param cacheName
	 * @return
	 */
	public boolean refreshCache(String cacheName) {
		return refreshCache(cacheName, !asynMode);
	}
	
	public void refreshCacheAll() {
		String[] cachesNames = this.getCacheManager().getCacheNames();
		for (String cacheName : cachesNames) {
			refreshCache(cacheName, !asynMode);
		}
	}
	
	/**
	 * full-refresh the cache from DB in a synchronized/non-synchronized mode
	 * @param cacheName
	 * @param synMode
	 * @return
	 */
	private boolean refreshCache(String cacheName, boolean synMode) {
		if (cacheName == null) {
			throw new IllegalArgumentException("cacheName could not be null");
		}
		Cache legacy = cacheManager.getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		boolean result = false;
		try {
			
			//refreshCacheInternal(legacy, CacheConfigs.get(cacheName));
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					legacy,
					cacheConfigs.get(cacheName),
					cacheStatusMap.get(cacheName),
					cacheLocks.get(cacheName),
					CacheRefreshingTask.ACTION_REFRESH
			);
			Future<Boolean> future = executorService.submit( task );
			if (synMode) {
				result = future.get();
			} else {
				result = true;
			}
			
		} catch (Exception exp) {
			CmLogger.getLogger().debug("refresh Cache " + cacheName + "failed.");
			CmLogger.getLogger().error(exp);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated
	public void refreshCacheInternal(Cache cache, CacheConfig cfg) throws Exception {
		if (cache == null || cfg == null) {
			throw new IllegalArgumentException("cache or cacheConfig could not be null.");
		}
		
		Object obj = null;
		if (springApplicationCtx != null) {
			obj = getLoadMethod().invoke(springApplicationCtx.getBean(cfg.getBeanName()), new Object[]{});
		} else {
			obj = getLoadMethod().invoke(cfg.getLoaderClazz().newInstance(), new Object[]{});
		}
		if (obj instanceof List) {
			List keys = cache.getKeys();
			Set toBeRemoved = new HashSet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				toBeRemoved.add(it.next());
			}
			List list = (List) obj;
			List newKeys = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cfg.getKeyMethod().invoke(v, new Object[]{});
				newKeys.add(k);
				cache.put(new net.sf.ehcache.Element(k, v));
			}
			toBeRemoved.removeAll(newKeys);
			if (toBeRemoved != null && toBeRemoved.size() > 0) {
				for (Iterator it = toBeRemoved.iterator(); it.hasNext();) {
					Object k = it.next();
					cache.remove(k);
				}
			}
		}
	}
	
	/**
	 * load all cached item from the given cache
	 * @param <T>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		List<T> list = new ArrayList<T>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			list.add((T)cache.get(it.next()).getObjectValue());
		}
		
		return list;
	}
	
	/**
	 * load multiple elements by the given keys
	 * @param <T>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <T> List<T> getListByIds(String cacheName, Object ... keys) {

		List<T> list = new ArrayList<T>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return list;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				list.add((T)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return list;
	}
	/**
	 * load multiple elements stored in map by the given keys
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <K, V> Map<K, V>  getMapByIds(String cacheName, Object ... keys) {
		
		Map<K, V> map = new HashMap<K, V>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return map;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				map.put((K)key, (V)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return map;
	}
	
	/**
	 * load all cached items from the given cache
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		Map<K, V> map = new HashMap<K, V>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			K key = (K)it.next();
			map.put(key, (V)cache.get(key).getObjectValue());
		}
		
		return map;
	}
	
	
	/**
	 * thread pool for refreshing cache
	 */
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	/**
	 * class for storing cache status
	 */
	private static class CacheStatus {
		
		public static final int STOPPED = 1;
		public static final int STARTING = 1<<1;
		public static final int FAILED = 1<<2;
		
		public static final int RUNNING = 1<<4;
		public static final int REFRESHING = 1<<5;
		
		private volatile int status = STOPPED;
		
		public void setStatus(int stts) {
			this.status = stts;
		}
		public int getStatus() {
			return this.status;
		}
		public String getStatusDesc() {
			switch(this.status) {
			case STOPPED : return "Stopped";
			case STARTING: return "Starting";
			case FAILED  : return "Failed";
			case RUNNING : return "Running";
			case REFRESHING: return "Refreshing";
			default: return "Unknown";
			}
		}
		
		public boolean available() {
			return this.status >= RUNNING;
		}
		
	}
	
	/**
	 * refresh cache task
	 */
	private static class CacheRefreshingTask implements Callable<Boolean> {
		
		private static final int ACTION_INIT = 1<<1;
		private static final int ACTION_REFRESH = 1<<2;
		

		private ApplicationContext appCtx;
		private CacheConfig cacheConfig;
		private Cache cache;
		private CacheStatus status;
		private ReentrantLock lock;
		private int action;
		
		public CacheRefreshingTask(
				ApplicationContext appCtx, 
				Cache cache,
				CacheConfig cacheConfig,
				CacheStatus status,
				ReentrantLock lock,
				int action
		) {
			this.appCtx = appCtx;
			this.cache = cache;
			this.cacheConfig = cacheConfig;
			this.status = status;
			this.lock = lock;
			this.action = action;
		}

		public Boolean call() {
			
			if (cache == null || cacheConfig == null || lock == null) {
				return false;
			}
			
			Thread current = Thread.currentThread();
			String legacyThreadName = current.getName();
			current.setName(cacheConfig.getCacheName());
			int firstStatus = status.getStatus();
			
			// tryLock is incorrect, use lock instead
			// this.lock.tryLock();
			boolean result = false;
			this.lock.lock();
			try {
				if (action == ACTION_INIT) {
					status.setStatus(CacheStatus.STARTING);
				} else if (action == ACTION_REFRESH){
					if (firstStatus < CacheStatus.RUNNING) {
						status.setStatus(CacheStatus.STARTING);
					} else {
						status.setStatus(CacheStatus.REFRESHING);
					}
				}
				try {
					long start = System.currentTimeMillis();
					doRefresh();
					long stop = System.currentTimeMillis();
					CmLogger.getLogger().debug("fill Cache [" + cache.getName() + "] cost time :" + (stop - start) / 1.0e3 + " sec");
					status.setStatus(CacheStatus.RUNNING);
					result = true;
					
				} catch(Exception exp) {
					CmLogger.getLogger().error(exp);
					status.setStatus(CacheStatus.FAILED);
					// ignore exception
					result = false;
				}
			} finally {
				this.lock.unlock();
			}
			current.setName(legacyThreadName);
			return result;
		}
		
		/**
		 * refresh the cache from db
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		private void doRefresh() throws Exception {
			Object obj = null;
			if (appCtx != null) {
				CmLogger.getLogger().debug("doRefresh:use spring.");
				obj = getLoadMethod().invoke(appCtx.getBean(cacheConfig.getBeanName()), new Object[]{});
			} else {
				CmLogger.getLogger().debug("doRefresh:use loader.");
				obj = getLoadMethod().invoke(cacheConfig.getLoaderClazz().newInstance(), new Object[]{});
			}
			if (obj instanceof List) {
				this.doRefreshInternal((List) obj);
			} else {
				throw new IllegalStateException("the returned type not supported");
			}
		}
		@SuppressWarnings("unchecked")
		private void doRefreshInternal(List list) throws Exception {

			
			cache.removeAll();
			List<net.sf.ehcache.Element> eles = new ArrayList<net.sf.ehcache.Element>();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cacheConfig.getKeyMethod().invoke(v, new Object[]{});
				eles.add(new net.sf.ehcache.Element(k, v));
			}
			cache.putAll(eles);
		
		}
		
	}
	
	
}
package com.cm.basic.cache.local;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.cm.logging.CmLogger;


/**
 * To use this cache service transparently, please don't call this service directly except from service layer
 * which cross cut by transaction manager
 */
public class CacheService implements ApplicationContextAware, DisposableBean {

    public void destroy() {
        this.cacheConfigs.clear();
        this.cacheLocks.clear();
        this.cacheStatusMap.clear();
        this.cacheManager.shutdown();
        this.executorService.shutdownNow();
    }

	/** cacheService config file name **/
	public static final String SERVICE_CONFIG = "cacheService.xml";
	
	/** ehcache config file name **/
	public static final String EHCACHE_CONFIG = "ehcache.xml";
	
	/**
	 * spring application context
	 */
	private ApplicationContext springApplicationCtx = null;
	
	/**
	 * ehcache cache manager
	 * in current implemenation, use singleton cache manager, 
	 * thus the ehcache config file will not be deleted while full building happened
	 */
	private CacheManager cacheManager = null;
	
	/**
	 * cache service config collection
	 */
	private Map<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<String, CacheConfig>();

	/**
	 * cache lock collection
	 */
	private Map<String, ReentrantLock> cacheLocks = new ConcurrentHashMap<String, ReentrantLock>();
	
	/**
	 * cache status collection
	 */
	private Map<String, CacheStatus> cacheStatusMap = new ConcurrentHashMap<String, CacheStatus>();
	
	
	/**
	 * toggle for asynchronous cache initialization
	 */
	private boolean asynMode = true;
	
	
	public void setAsynMode(boolean asynMode) {
		this.asynMode = asynMode;
	}

	/**
	 * load config file from classloader root path
	 * @param fileName
	 * @return
	 */
	private InputStream loadConfigFromRootPath(String fileName) {
		InputStream is = CacheService.class.getClassLoader().getResourceAsStream(fileName);
		return is;
	}

//	private static CacheService instance;	
//	public static CacheService getInstance() {
//		if (instance == null) {
//			synchronized (CacheService.class) {
//				instance = new CacheService();
//			}
//		}
//		return instance;
//	}

	/**
	 * mark private to make sure no new instance created in programmer way
	 */
	private CacheService () {
		
		try {
			InputStream is = loadConfigFromRootPath(EHCACHE_CONFIG);
			if (is == null) {
				CmLogger.getLogger().debug("ehcache config " + EHCACHE_CONFIG + " not found");
				return;
			}
			cacheManager = CacheManager.create(is);
			
		} catch (Exception e) {
			System.out.println("CacheService initialization failed.");
			CmLogger.getLogger().error(e);
		}
	}
	
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		springApplicationCtx = ctx;
		try {
			loadConfig();
		} catch (Exception e) {
			CmLogger.getLogger().error(e);
		}
	}
	
	/**
	 * load config from file and then fill cache base the config
	 * @throws Exception
	 */
	private void loadConfig() throws Exception {
		InputStream is = loadConfigFromRootPath(SERVICE_CONFIG);
		if (is == null){
			CmLogger.getLogger().debug("cache service config " + SERVICE_CONFIG + " found.");
			return;
		}
		
		// 1. load xml & then parse
		List<CacheConfig> configs = parseConfig(is);
		
		// 2. refresh cache based on config
		for (CacheConfig cfg : configs) {
			Cache cache = cacheManager.getCache(cfg.getCacheName());
			if (cache != null) {
				// store config
				cacheConfigs.put(cfg.getCacheName(), cfg);
				
				// store status for each cache
				CacheStatus status = new CacheStatus();
				cacheStatusMap.put(cfg.getCacheName(), status);
				
				// store lock for each cache
				ReentrantLock lock = new ReentrantLock();
				cacheLocks.put(cfg.getCacheName(), lock);
				
				// enrich cache from db
				fillCache(cache, cfg, status, lock);
			} else {
				CmLogger.getLogger().debug("no cache [ " + cfg.getCacheName() + "] found.");
			}
		}
	}
	
	
	/**
	 * get loader method then will be invoked by reflection
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	private static Method getLoadMethod () throws SecurityException, NoSuchMethodException {
		return CacheLoader.class.getMethod("loadAll", new Class[]{});
	}
	
	/**
	 * parse cache service config
	 * @param configFile
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<CacheConfig> parseConfig(InputStream configFile) throws Exception {
		Document doc = getDocument(configFile);
		Element root = doc.getRootElement();
		List<Node> caches = root.selectNodes("cache");
		List<CacheConfig> configs = new ArrayList<CacheConfig>();
		for (Node ele : caches) {
			CacheConfig config = new CacheConfig();
			config.setCacheName(ele.valueOf("@name"));
			
			Node keyEle = ele.selectSingleNode("key");
			
			Class keyClz = Class.forName(keyEle.valueOf("@class"));
			
			config.setKeyClazz(keyClz);
			config.setKeyMethod(keyClz.getMethod(keyEle.valueOf("@method"), new Class[]{}));
			
			Node srcEle = ele.selectSingleNode("loader");
			Class srcClz = Class.forName(srcEle.valueOf("@class"));
			config.setLoaderClazz(srcClz);
			
			Node beanEle = ele.selectSingleNode("bean");
			if (beanEle != null)
				config.setBeanName(beanEle.valueOf("@id"));
			
			configs.add(config);
		}
		return configs;
	}
	
	/**
	 * get dom4j document from inputstream
	 * @param file
	 * @return
	 */
	private static Document getDocument(final InputStream file) {
		Document document = null;
		SAXReader reader = new SAXReader();
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * fill cache with the records from db based on config
	 * @param cache
	 * @param cfg
	 * @param status
	 * @param lock
	 * @return
	 */
	private boolean fillCache(Cache cache, CacheConfig cfg, CacheStatus status, ReentrantLock lock) {
		boolean result = false;
		try {
			//refreshCacheInternal(cache, cfg);
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					cache,
					cfg,
					status,
					lock,
					CacheRefreshingTask.ACTION_INIT
			);
			
			CmLogger.getLogger().debug("filling cache [" + cache.getName() + "]......");
			Future<Boolean> future = executorService.submit(task);
			if (!asynMode) {
				result = future.get();
			} else {
				result = true;
			}
		} catch (Exception e) {
			result = false;
			CmLogger.getLogger().debug("refresh cache [" + cache.getName() + "] failed.");
			CmLogger.getLogger().error(e);
		}
		return result;
		
	}
	
	/**
	 * get cache from cache manager by given name
	 * @param cacheName
	 * @return
	 */
	public Cache getCache(String cacheName) {
		
		CacheStatus status = cacheStatusMap.get(cacheName);
		if (status == null) {
			// no cache configed
			return null;
		}
		CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
		
		// TODO if cache have not started, raise error or wait?
		if (!status.available()) {
			while (true) {
				CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
				if (status.available()) {
					break;
				} else if (status.getStatus() == CacheStatus.STARTING) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] is starting...");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						CmLogger.getLogger().error(e);
					}
				} else if (status.getStatus() == CacheStatus.FAILED) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					throw new IllegalStateException("cache [ " + cacheName + " ] initialization failed");
				} else if (status.getStatus() == CacheStatus.STOPPED) {
					// never happen
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization not started");
					boolean success = this.refreshCache(cacheName, true);
					if (!success) {
						CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					}
					break;
				}
			}
		}
		Cache cache = cacheManager.getCache(cacheName);
		return cache;
	}
	
	/**
	 * add or replace the cache item into cache identified by the given name
	 * @param cacheName
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean addOrUpdateObject(String cacheName, Object key, Object value) {
		if (cacheName == null || key == null || value == null) {
			throw new IllegalArgumentException("arguments could not be null: cacheName"+cacheName+" ,key"+key+" ,value"+value);
		}
		Cache legacy = getCache(cacheName);
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		if (legacy.get(key) != null) {
			legacy.replace(new net.sf.ehcache.Element(key, value));
		} else {
			legacy.put(new net.sf.ehcache.Element(key, value));
		}
		return true;
	}
	
	/**
	 * remove the cached item from cache
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public boolean removeObject(String cacheName, Object key) {
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		return legacy.remove(key);
	}
	
	/**
	 * load the cached item by the key/id
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public Object getObjectById(String cacheName, Object key) {
		
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return null;
		}
		
		net.sf.ehcache.Element ele = legacy.get(key);
		if (ele != null) {
			return ele.getObjectValue();
		} else {
			CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
		}
		
		// no value object found
		return null;
	}
	
	/**
	 * full-refresh the cache from db
	 * <br/>
	 * <font color="red">
	 * NOTE:<br/>
	 * consider the method below firstly before using this method:
	 * </font><br/>
	 * <ol>
	 * <li> {@code CacheService#addOrUpdateObject(String, Object, Object)}({@link #addOrUpdateObject(String, Object, Object)})</li>
	 * <li> {@code CacheService#removeObject(String, Object)}({@link #removeObject(String, Object)})</li>
	 * </ol>
	 * @param cacheName
	 * @return
	 */
	public boolean refreshCache(String cacheName) {
		return refreshCache(cacheName, !asynMode);
	}
	
	public void refreshCacheAll() {
		String[] cachesNames = this.getCacheManager().getCacheNames();
		for (String cacheName : cachesNames) {
			refreshCache(cacheName, !asynMode);
		}
	}
	
	/**
	 * full-refresh the cache from DB in a synchronized/non-synchronized mode
	 * @param cacheName
	 * @param synMode
	 * @return
	 */
	private boolean refreshCache(String cacheName, boolean synMode) {
		if (cacheName == null) {
			throw new IllegalArgumentException("cacheName could not be null");
		}
		Cache legacy = cacheManager.getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		boolean result = false;
		try {
			
			//refreshCacheInternal(legacy, CacheConfigs.get(cacheName));
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					legacy,
					cacheConfigs.get(cacheName),
					cacheStatusMap.get(cacheName),
					cacheLocks.get(cacheName),
					CacheRefreshingTask.ACTION_REFRESH
			);
			Future<Boolean> future = executorService.submit( task );
			if (synMode) {
				result = future.get();
			} else {
				result = true;
			}
			
		} catch (Exception exp) {
			CmLogger.getLogger().debug("refresh Cache " + cacheName + "failed.");
			CmLogger.getLogger().error(exp);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated
	public void refreshCacheInternal(Cache cache, CacheConfig cfg) throws Exception {
		if (cache == null || cfg == null) {
			throw new IllegalArgumentException("cache or cacheConfig could not be null.");
		}
		
		Object obj = null;
		if (springApplicationCtx != null) {
			obj = getLoadMethod().invoke(springApplicationCtx.getBean(cfg.getBeanName()), new Object[]{});
		} else {
			obj = getLoadMethod().invoke(cfg.getLoaderClazz().newInstance(), new Object[]{});
		}
		if (obj instanceof List) {
			List keys = cache.getKeys();
			Set toBeRemoved = new HashSet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				toBeRemoved.add(it.next());
			}
			List list = (List) obj;
			List newKeys = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cfg.getKeyMethod().invoke(v, new Object[]{});
				newKeys.add(k);
				cache.put(new net.sf.ehcache.Element(k, v));
			}
			toBeRemoved.removeAll(newKeys);
			if (toBeRemoved != null && toBeRemoved.size() > 0) {
				for (Iterator it = toBeRemoved.iterator(); it.hasNext();) {
					Object k = it.next();
					cache.remove(k);
				}
			}
		}
	}
	
	/**
	 * load all cached item from the given cache
	 * @param <T>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		List<T> list = new ArrayList<T>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			list.add((T)cache.get(it.next()).getObjectValue());
		}
		
		return list;
	}
	
	/**
	 * load multiple elements by the given keys
	 * @param <T>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <T> List<T> getListByIds(String cacheName, Object ... keys) {

		List<T> list = new ArrayList<T>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return list;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				list.add((T)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return list;
	}
	/**
	 * load multiple elements stored in map by the given keys
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <K, V> Map<K, V>  getMapByIds(String cacheName, Object ... keys) {
		
		Map<K, V> map = new HashMap<K, V>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return map;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				map.put((K)key, (V)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return map;
	}
	
	/**
	 * load all cached items from the given cache
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		Map<K, V> map = new HashMap<K, V>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			K key = (K)it.next();
			map.put(key, (V)cache.get(key).getObjectValue());
		}
		
		return map;
	}
	
	
	/**
	 * thread pool for refreshing cache
	 */
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	/**
	 * class for storing cache status
	 */
	private static class CacheStatus {
		
		public static final int STOPPED = 1;
		public static final int STARTING = 1<<1;
		public static final int FAILED = 1<<2;
		
		public static final int RUNNING = 1<<4;
		public static final int REFRESHING = 1<<5;
		
		private volatile int status = STOPPED;
		
		public void setStatus(int stts) {
			this.status = stts;
		}
		public int getStatus() {
			return this.status;
		}
		public String getStatusDesc() {
			switch(this.status) {
			case STOPPED : return "Stopped";
			case STARTING: return "Starting";
			case FAILED  : return "Failed";
			case RUNNING : return "Running";
			case REFRESHING: return "Refreshing";
			default: return "Unknown";
			}
		}
		
		public boolean available() {
			return this.status >= RUNNING;
		}
		
	}
	
	/**
	 * refresh cache task
	 */
	private static class CacheRefreshingTask implements Callable<Boolean> {
		
		private static final int ACTION_INIT = 1<<1;
		private static final int ACTION_REFRESH = 1<<2;
		

		private ApplicationContext appCtx;
		private CacheConfig cacheConfig;
		private Cache cache;
		private CacheStatus status;
		private ReentrantLock lock;
		private int action;
		
		public CacheRefreshingTask(
				ApplicationContext appCtx, 
				Cache cache,
				CacheConfig cacheConfig,
				CacheStatus status,
				ReentrantLock lock,
				int action
		) {
			this.appCtx = appCtx;
			this.cache = cache;
			this.cacheConfig = cacheConfig;
			this.status = status;
			this.lock = lock;
			this.action = action;
		}

		public Boolean call() {
			
			if (cache == null || cacheConfig == null || lock == null) {
				return false;
			}
			
			Thread current = Thread.currentThread();
			String legacyThreadName = current.getName();
			current.setName(cacheConfig.getCacheName());
			int firstStatus = status.getStatus();
			
			// tryLock is incorrect, use lock instead
			// this.lock.tryLock();
			boolean result = false;
			this.lock.lock();
			try {
				if (action == ACTION_INIT) {
					status.setStatus(CacheStatus.STARTING);
				} else if (action == ACTION_REFRESH){
					if (firstStatus < CacheStatus.RUNNING) {
						status.setStatus(CacheStatus.STARTING);
					} else {
						status.setStatus(CacheStatus.REFRESHING);
					}
				}
				try {
					long start = System.currentTimeMillis();
					doRefresh();
					long stop = System.currentTimeMillis();
					CmLogger.getLogger().debug("fill Cache [" + cache.getName() + "] cost time :" + (stop - start) / 1.0e3 + " sec");
					status.setStatus(CacheStatus.RUNNING);
					result = true;
					
				} catch(Exception exp) {
					CmLogger.getLogger().error(exp);
					status.setStatus(CacheStatus.FAILED);
					// ignore exception
					result = false;
				}
			} finally {
				this.lock.unlock();
			}
			current.setName(legacyThreadName);
			return result;
		}
		
		/**
		 * refresh the cache from db
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		private void doRefresh() throws Exception {
			Object obj = null;
			if (appCtx != null) {
				CmLogger.getLogger().debug("doRefresh:use spring.");
				obj = getLoadMethod().invoke(appCtx.getBean(cacheConfig.getBeanName()), new Object[]{});
			} else {
				CmLogger.getLogger().debug("doRefresh:use loader.");
				obj = getLoadMethod().invoke(cacheConfig.getLoaderClazz().newInstance(), new Object[]{});
			}
			if (obj instanceof List) {
				this.doRefreshInternal((List) obj);
			} else {
				throw new IllegalStateException("the returned type not supported");
			}
		}
		@SuppressWarnings("unchecked")
		private void doRefreshInternal(List list) throws Exception {

			
			cache.removeAll();
			List<net.sf.ehcache.Element> eles = new ArrayList<net.sf.ehcache.Element>();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cacheConfig.getKeyMethod().invoke(v, new Object[]{});
				eles.add(new net.sf.ehcache.Element(k, v));
			}
			cache.putAll(eles);
		
		}
		
	}
	
	
}
package com.cm.basic.cache.local;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.cm.logging.CmLogger;


/**
 * To use this cache service transparently, please don't call this service directly except from service layer
 * which cross cut by transaction manager
 */
public class CacheService implements ApplicationContextAware, DisposableBean {

    public void destroy() {
        this.cacheConfigs.clear();
        this.cacheLocks.clear();
        this.cacheStatusMap.clear();
        this.cacheManager.shutdown();
        this.executorService.shutdownNow();
    }

	/** cacheService config file name **/
	public static final String SERVICE_CONFIG = "cacheService.xml";
	
	/** ehcache config file name **/
	public static final String EHCACHE_CONFIG = "ehcache.xml";
	
	/**
	 * spring application context
	 */
	private ApplicationContext springApplicationCtx = null;
	
	/**
	 * ehcache cache manager
	 * in current implemenation, use singleton cache manager, 
	 * thus the ehcache config file will not be deleted while full building happened
	 */
	private CacheManager cacheManager = null;
	
	/**
	 * cache service config collection
	 */
	private Map<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<String, CacheConfig>();

	/**
	 * cache lock collection
	 */
	private Map<String, ReentrantLock> cacheLocks = new ConcurrentHashMap<String, ReentrantLock>();
	
	/**
	 * cache status collection
	 */
	private Map<String, CacheStatus> cacheStatusMap = new ConcurrentHashMap<String, CacheStatus>();
	
	
	/**
	 * toggle for asynchronous cache initialization
	 */
	private boolean asynMode = true;
	
	
	public void setAsynMode(boolean asynMode) {
		this.asynMode = asynMode;
	}

	/**
	 * load config file from classloader root path
	 * @param fileName
	 * @return
	 */
	private InputStream loadConfigFromRootPath(String fileName) {
		InputStream is = CacheService.class.getClassLoader().getResourceAsStream(fileName);
		return is;
	}

//	private static CacheService instance;	
//	public static CacheService getInstance() {
//		if (instance == null) {
//			synchronized (CacheService.class) {
//				instance = new CacheService();
//			}
//		}
//		return instance;
//	}

	/**
	 * mark private to make sure no new instance created in programmer way
	 */
	private CacheService () {
		
		try {
			InputStream is = loadConfigFromRootPath(EHCACHE_CONFIG);
			if (is == null) {
				CmLogger.getLogger().debug("ehcache config " + EHCACHE_CONFIG + " not found");
				return;
			}
			cacheManager = CacheManager.create(is);
			
		} catch (Exception e) {
			System.out.println("CacheService initialization failed.");
			CmLogger.getLogger().error(e);
		}
	}
	
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		springApplicationCtx = ctx;
		try {
			loadConfig();
		} catch (Exception e) {
			CmLogger.getLogger().error(e);
		}
	}
	
	/**
	 * load config from file and then fill cache base the config
	 * @throws Exception
	 */
	private void loadConfig() throws Exception {
		InputStream is = loadConfigFromRootPath(SERVICE_CONFIG);
		if (is == null){
			CmLogger.getLogger().debug("cache service config " + SERVICE_CONFIG + " found.");
			return;
		}
		
		// 1. load xml & then parse
		List<CacheConfig> configs = parseConfig(is);
		
		// 2. refresh cache based on config
		for (CacheConfig cfg : configs) {
			Cache cache = cacheManager.getCache(cfg.getCacheName());
			if (cache != null) {
				// store config
				cacheConfigs.put(cfg.getCacheName(), cfg);
				
				// store status for each cache
				CacheStatus status = new CacheStatus();
				cacheStatusMap.put(cfg.getCacheName(), status);
				
				// store lock for each cache
				ReentrantLock lock = new ReentrantLock();
				cacheLocks.put(cfg.getCacheName(), lock);
				
				// enrich cache from db
				fillCache(cache, cfg, status, lock);
			} else {
				CmLogger.getLogger().debug("no cache [ " + cfg.getCacheName() + "] found.");
			}
		}
	}
	
	
	/**
	 * get loader method then will be invoked by reflection
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	private static Method getLoadMethod () throws SecurityException, NoSuchMethodException {
		return CacheLoader.class.getMethod("loadAll", new Class[]{});
	}
	
	/**
	 * parse cache service config
	 * @param configFile
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<CacheConfig> parseConfig(InputStream configFile) throws Exception {
		Document doc = getDocument(configFile);
		Element root = doc.getRootElement();
		List<Node> caches = root.selectNodes("cache");
		List<CacheConfig> configs = new ArrayList<CacheConfig>();
		for (Node ele : caches) {
			CacheConfig config = new CacheConfig();
			config.setCacheName(ele.valueOf("@name"));
			
			Node keyEle = ele.selectSingleNode("key");
			
			Class keyClz = Class.forName(keyEle.valueOf("@class"));
			
			config.setKeyClazz(keyClz);
			config.setKeyMethod(keyClz.getMethod(keyEle.valueOf("@method"), new Class[]{}));
			
			Node srcEle = ele.selectSingleNode("loader");
			Class srcClz = Class.forName(srcEle.valueOf("@class"));
			config.setLoaderClazz(srcClz);
			
			Node beanEle = ele.selectSingleNode("bean");
			if (beanEle != null)
				config.setBeanName(beanEle.valueOf("@id"));
			
			configs.add(config);
		}
		return configs;
	}
	
	/**
	 * get dom4j document from inputstream
	 * @param file
	 * @return
	 */
	private static Document getDocument(final InputStream file) {
		Document document = null;
		SAXReader reader = new SAXReader();
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * fill cache with the records from db based on config
	 * @param cache
	 * @param cfg
	 * @param status
	 * @param lock
	 * @return
	 */
	private boolean fillCache(Cache cache, CacheConfig cfg, CacheStatus status, ReentrantLock lock) {
		boolean result = false;
		try {
			//refreshCacheInternal(cache, cfg);
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					cache,
					cfg,
					status,
					lock,
					CacheRefreshingTask.ACTION_INIT
			);
			
			CmLogger.getLogger().debug("filling cache [" + cache.getName() + "]......");
			Future<Boolean> future = executorService.submit(task);
			if (!asynMode) {
				result = future.get();
			} else {
				result = true;
			}
		} catch (Exception e) {
			result = false;
			CmLogger.getLogger().debug("refresh cache [" + cache.getName() + "] failed.");
			CmLogger.getLogger().error(e);
		}
		return result;
		
	}
	
	/**
	 * get cache from cache manager by given name
	 * @param cacheName
	 * @return
	 */
	public Cache getCache(String cacheName) {
		
		CacheStatus status = cacheStatusMap.get(cacheName);
		if (status == null) {
			// no cache configed
			return null;
		}
		CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
		
		// TODO if cache have not started, raise error or wait?
		if (!status.available()) {
			while (true) {
				CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
				if (status.available()) {
					break;
				} else if (status.getStatus() == CacheStatus.STARTING) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] is starting...");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						CmLogger.getLogger().error(e);
					}
				} else if (status.getStatus() == CacheStatus.FAILED) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					throw new IllegalStateException("cache [ " + cacheName + " ] initialization failed");
				} else if (status.getStatus() == CacheStatus.STOPPED) {
					// never happen
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization not started");
					boolean success = this.refreshCache(cacheName, true);
					if (!success) {
						CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					}
					break;
				}
			}
		}
		Cache cache = cacheManager.getCache(cacheName);
		return cache;
	}
	
	/**
	 * add or replace the cache item into cache identified by the given name
	 * @param cacheName
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean addOrUpdateObject(String cacheName, Object key, Object value) {
		if (cacheName == null || key == null || value == null) {
			throw new IllegalArgumentException("arguments could not be null: cacheName"+cacheName+" ,key"+key+" ,value"+value);
		}
		Cache legacy = getCache(cacheName);
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		if (legacy.get(key) != null) {
			legacy.replace(new net.sf.ehcache.Element(key, value));
		} else {
			legacy.put(new net.sf.ehcache.Element(key, value));
		}
		return true;
	}
	
	/**
	 * remove the cached item from cache
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public boolean removeObject(String cacheName, Object key) {
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		return legacy.remove(key);
	}
	
	/**
	 * load the cached item by the key/id
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public Object getObjectById(String cacheName, Object key) {
		
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return null;
		}
		
		net.sf.ehcache.Element ele = legacy.get(key);
		if (ele != null) {
			return ele.getObjectValue();
		} else {
			CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
		}
		
		// no value object found
		return null;
	}
	
	/**
	 * full-refresh the cache from db
	 * <br/>
	 * <font color="red">
	 * NOTE:<br/>
	 * consider the method below firstly before using this method:
	 * </font><br/>
	 * <ol>
	 * <li> {@code CacheService#addOrUpdateObject(String, Object, Object)}({@link #addOrUpdateObject(String, Object, Object)})</li>
	 * <li> {@code CacheService#removeObject(String, Object)}({@link #removeObject(String, Object)})</li>
	 * </ol>
	 * @param cacheName
	 * @return
	 */
	public boolean refreshCache(String cacheName) {
		return refreshCache(cacheName, !asynMode);
	}
	
	public void refreshCacheAll() {
		String[] cachesNames = this.getCacheManager().getCacheNames();
		for (String cacheName : cachesNames) {
			refreshCache(cacheName, !asynMode);
		}
	}
	
	/**
	 * full-refresh the cache from DB in a synchronized/non-synchronized mode
	 * @param cacheName
	 * @param synMode
	 * @return
	 */
	private boolean refreshCache(String cacheName, boolean synMode) {
		if (cacheName == null) {
			throw new IllegalArgumentException("cacheName could not be null");
		}
		Cache legacy = cacheManager.getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		boolean result = false;
		try {
			
			//refreshCacheInternal(legacy, CacheConfigs.get(cacheName));
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					legacy,
					cacheConfigs.get(cacheName),
					cacheStatusMap.get(cacheName),
					cacheLocks.get(cacheName),
					CacheRefreshingTask.ACTION_REFRESH
			);
			Future<Boolean> future = executorService.submit( task );
			if (synMode) {
				result = future.get();
			} else {
				result = true;
			}
			
		} catch (Exception exp) {
			CmLogger.getLogger().debug("refresh Cache " + cacheName + "failed.");
			CmLogger.getLogger().error(exp);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated
	public void refreshCacheInternal(Cache cache, CacheConfig cfg) throws Exception {
		if (cache == null || cfg == null) {
			throw new IllegalArgumentException("cache or cacheConfig could not be null.");
		}
		
		Object obj = null;
		if (springApplicationCtx != null) {
			obj = getLoadMethod().invoke(springApplicationCtx.getBean(cfg.getBeanName()), new Object[]{});
		} else {
			obj = getLoadMethod().invoke(cfg.getLoaderClazz().newInstance(), new Object[]{});
		}
		if (obj instanceof List) {
			List keys = cache.getKeys();
			Set toBeRemoved = new HashSet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				toBeRemoved.add(it.next());
			}
			List list = (List) obj;
			List newKeys = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cfg.getKeyMethod().invoke(v, new Object[]{});
				newKeys.add(k);
				cache.put(new net.sf.ehcache.Element(k, v));
			}
			toBeRemoved.removeAll(newKeys);
			if (toBeRemoved != null && toBeRemoved.size() > 0) {
				for (Iterator it = toBeRemoved.iterator(); it.hasNext();) {
					Object k = it.next();
					cache.remove(k);
				}
			}
		}
	}
	
	/**
	 * load all cached item from the given cache
	 * @param <T>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		List<T> list = new ArrayList<T>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			list.add((T)cache.get(it.next()).getObjectValue());
		}
		
		return list;
	}
	
	/**
	 * load multiple elements by the given keys
	 * @param <T>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <T> List<T> getListByIds(String cacheName, Object ... keys) {

		List<T> list = new ArrayList<T>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return list;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				list.add((T)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return list;
	}
	/**
	 * load multiple elements stored in map by the given keys
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <K, V> Map<K, V>  getMapByIds(String cacheName, Object ... keys) {
		
		Map<K, V> map = new HashMap<K, V>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return map;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				map.put((K)key, (V)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return map;
	}
	
	/**
	 * load all cached items from the given cache
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		Map<K, V> map = new HashMap<K, V>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			K key = (K)it.next();
			map.put(key, (V)cache.get(key).getObjectValue());
		}
		
		return map;
	}
	
	
	/**
	 * thread pool for refreshing cache
	 */
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	/**
	 * class for storing cache status
	 */
	private static class CacheStatus {
		
		public static final int STOPPED = 1;
		public static final int STARTING = 1<<1;
		public static final int FAILED = 1<<2;
		
		public static final int RUNNING = 1<<4;
		public static final int REFRESHING = 1<<5;
		
		private volatile int status = STOPPED;
		
		public void setStatus(int stts) {
			this.status = stts;
		}
		public int getStatus() {
			return this.status;
		}
		public String getStatusDesc() {
			switch(this.status) {
			case STOPPED : return "Stopped";
			case STARTING: return "Starting";
			case FAILED  : return "Failed";
			case RUNNING : return "Running";
			case REFRESHING: return "Refreshing";
			default: return "Unknown";
			}
		}
		
		public boolean available() {
			return this.status >= RUNNING;
		}
		
	}
	
	/**
	 * refresh cache task
	 */
	private static class CacheRefreshingTask implements Callable<Boolean> {
		
		private static final int ACTION_INIT = 1<<1;
		private static final int ACTION_REFRESH = 1<<2;
		

		private ApplicationContext appCtx;
		private CacheConfig cacheConfig;
		private Cache cache;
		private CacheStatus status;
		private ReentrantLock lock;
		private int action;
		
		public CacheRefreshingTask(
				ApplicationContext appCtx, 
				Cache cache,
				CacheConfig cacheConfig,
				CacheStatus status,
				ReentrantLock lock,
				int action
		) {
			this.appCtx = appCtx;
			this.cache = cache;
			this.cacheConfig = cacheConfig;
			this.status = status;
			this.lock = lock;
			this.action = action;
		}

		public Boolean call() {
			
			if (cache == null || cacheConfig == null || lock == null) {
				return false;
			}
			
			Thread current = Thread.currentThread();
			String legacyThreadName = current.getName();
			current.setName(cacheConfig.getCacheName());
			int firstStatus = status.getStatus();
			
			// tryLock is incorrect, use lock instead
			// this.lock.tryLock();
			boolean result = false;
			this.lock.lock();
			try {
				if (action == ACTION_INIT) {
					status.setStatus(CacheStatus.STARTING);
				} else if (action == ACTION_REFRESH){
					if (firstStatus < CacheStatus.RUNNING) {
						status.setStatus(CacheStatus.STARTING);
					} else {
						status.setStatus(CacheStatus.REFRESHING);
					}
				}
				try {
					long start = System.currentTimeMillis();
					doRefresh();
					long stop = System.currentTimeMillis();
					CmLogger.getLogger().debug("fill Cache [" + cache.getName() + "] cost time :" + (stop - start) / 1.0e3 + " sec");
					status.setStatus(CacheStatus.RUNNING);
					result = true;
					
				} catch(Exception exp) {
					CmLogger.getLogger().error(exp);
					status.setStatus(CacheStatus.FAILED);
					// ignore exception
					result = false;
				}
			} finally {
				this.lock.unlock();
			}
			current.setName(legacyThreadName);
			return result;
		}
		
		/**
		 * refresh the cache from db
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		private void doRefresh() throws Exception {
			Object obj = null;
			if (appCtx != null) {
				CmLogger.getLogger().debug("doRefresh:use spring.");
				obj = getLoadMethod().invoke(appCtx.getBean(cacheConfig.getBeanName()), new Object[]{});
			} else {
				CmLogger.getLogger().debug("doRefresh:use loader.");
				obj = getLoadMethod().invoke(cacheConfig.getLoaderClazz().newInstance(), new Object[]{});
			}
			if (obj instanceof List) {
				this.doRefreshInternal((List) obj);
			} else {
				throw new IllegalStateException("the returned type not supported");
			}
		}
		@SuppressWarnings("unchecked")
		private void doRefreshInternal(List list) throws Exception {

			
			cache.removeAll();
			List<net.sf.ehcache.Element> eles = new ArrayList<net.sf.ehcache.Element>();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cacheConfig.getKeyMethod().invoke(v, new Object[]{});
				eles.add(new net.sf.ehcache.Element(k, v));
			}
			cache.putAll(eles);
		
		}
		
	}
	
	
}
package com.cm.basic.cache.local;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.cm.logging.CmLogger;


/**
 * To use this cache service transparently, please don't call this service directly except from service layer
 * which cross cut by transaction manager
 */
public class CacheService implements ApplicationContextAware, DisposableBean {

    public void destroy() {
        this.cacheConfigs.clear();
        this.cacheLocks.clear();
        this.cacheStatusMap.clear();
        this.cacheManager.shutdown();
        this.executorService.shutdownNow();
    }

	/** cacheService config file name **/
	public static final String SERVICE_CONFIG = "cacheService.xml";
	
	/** ehcache config file name **/
	public static final String EHCACHE_CONFIG = "ehcache.xml";
	
	/**
	 * spring application context
	 */
	private ApplicationContext springApplicationCtx = null;
	
	/**
	 * ehcache cache manager
	 * in current implemenation, use singleton cache manager, 
	 * thus the ehcache config file will not be deleted while full building happened
	 */
	private CacheManager cacheManager = null;
	
	/**
	 * cache service config collection
	 */
	private Map<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<String, CacheConfig>();

	/**
	 * cache lock collection
	 */
	private Map<String, ReentrantLock> cacheLocks = new ConcurrentHashMap<String, ReentrantLock>();
	
	/**
	 * cache status collection
	 */
	private Map<String, CacheStatus> cacheStatusMap = new ConcurrentHashMap<String, CacheStatus>();
	
	
	/**
	 * toggle for asynchronous cache initialization
	 */
	private boolean asynMode = true;
	
	
	public void setAsynMode(boolean asynMode) {
		this.asynMode = asynMode;
	}

	/**
	 * load config file from classloader root path
	 * @param fileName
	 * @return
	 */
	private InputStream loadConfigFromRootPath(String fileName) {
		InputStream is = CacheService.class.getClassLoader().getResourceAsStream(fileName);
		return is;
	}

//	private static CacheService instance;	
//	public static CacheService getInstance() {
//		if (instance == null) {
//			synchronized (CacheService.class) {
//				instance = new CacheService();
//			}
//		}
//		return instance;
//	}

	/**
	 * mark private to make sure no new instance created in programmer way
	 */
	private CacheService () {
		
		try {
			InputStream is = loadConfigFromRootPath(EHCACHE_CONFIG);
			if (is == null) {
				CmLogger.getLogger().debug("ehcache config " + EHCACHE_CONFIG + " not found");
				return;
			}
			cacheManager = CacheManager.create(is);
			
		} catch (Exception e) {
			System.out.println("CacheService initialization failed.");
			CmLogger.getLogger().error(e);
		}
	}
	
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		springApplicationCtx = ctx;
		try {
			loadConfig();
		} catch (Exception e) {
			CmLogger.getLogger().error(e);
		}
	}
	
	/**
	 * load config from file and then fill cache base the config
	 * @throws Exception
	 */
	private void loadConfig() throws Exception {
		InputStream is = loadConfigFromRootPath(SERVICE_CONFIG);
		if (is == null){
			CmLogger.getLogger().debug("cache service config " + SERVICE_CONFIG + " found.");
			return;
		}
		
		// 1. load xml & then parse
		List<CacheConfig> configs = parseConfig(is);
		
		// 2. refresh cache based on config
		for (CacheConfig cfg : configs) {
			Cache cache = cacheManager.getCache(cfg.getCacheName());
			if (cache != null) {
				// store config
				cacheConfigs.put(cfg.getCacheName(), cfg);
				
				// store status for each cache
				CacheStatus status = new CacheStatus();
				cacheStatusMap.put(cfg.getCacheName(), status);
				
				// store lock for each cache
				ReentrantLock lock = new ReentrantLock();
				cacheLocks.put(cfg.getCacheName(), lock);
				
				// enrich cache from db
				fillCache(cache, cfg, status, lock);
			} else {
				CmLogger.getLogger().debug("no cache [ " + cfg.getCacheName() + "] found.");
			}
		}
	}
	
	
	/**
	 * get loader method then will be invoked by reflection
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	private static Method getLoadMethod () throws SecurityException, NoSuchMethodException {
		return CacheLoader.class.getMethod("loadAll", new Class[]{});
	}
	
	/**
	 * parse cache service config
	 * @param configFile
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<CacheConfig> parseConfig(InputStream configFile) throws Exception {
		Document doc = getDocument(configFile);
		Element root = doc.getRootElement();
		List<Node> caches = root.selectNodes("cache");
		List<CacheConfig> configs = new ArrayList<CacheConfig>();
		for (Node ele : caches) {
			CacheConfig config = new CacheConfig();
			config.setCacheName(ele.valueOf("@name"));
			
			Node keyEle = ele.selectSingleNode("key");
			
			Class keyClz = Class.forName(keyEle.valueOf("@class"));
			
			config.setKeyClazz(keyClz);
			config.setKeyMethod(keyClz.getMethod(keyEle.valueOf("@method"), new Class[]{}));
			
			Node srcEle = ele.selectSingleNode("loader");
			Class srcClz = Class.forName(srcEle.valueOf("@class"));
			config.setLoaderClazz(srcClz);
			
			Node beanEle = ele.selectSingleNode("bean");
			if (beanEle != null)
				config.setBeanName(beanEle.valueOf("@id"));
			
			configs.add(config);
		}
		return configs;
	}
	
	/**
	 * get dom4j document from inputstream
	 * @param file
	 * @return
	 */
	private static Document getDocument(final InputStream file) {
		Document document = null;
		SAXReader reader = new SAXReader();
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * fill cache with the records from db based on config
	 * @param cache
	 * @param cfg
	 * @param status
	 * @param lock
	 * @return
	 */
	private boolean fillCache(Cache cache, CacheConfig cfg, CacheStatus status, ReentrantLock lock) {
		boolean result = false;
		try {
			//refreshCacheInternal(cache, cfg);
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					cache,
					cfg,
					status,
					lock,
					CacheRefreshingTask.ACTION_INIT
			);
			
			CmLogger.getLogger().debug("filling cache [" + cache.getName() + "]......");
			Future<Boolean> future = executorService.submit(task);
			if (!asynMode) {
				result = future.get();
			} else {
				result = true;
			}
		} catch (Exception e) {
			result = false;
			CmLogger.getLogger().debug("refresh cache [" + cache.getName() + "] failed.");
			CmLogger.getLogger().error(e);
		}
		return result;
		
	}
	
	/**
	 * get cache from cache manager by given name
	 * @param cacheName
	 * @return
	 */
	public Cache getCache(String cacheName) {
		
		CacheStatus status = cacheStatusMap.get(cacheName);
		if (status == null) {
			// no cache configed
			return null;
		}
		CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
		
		// TODO if cache have not started, raise error or wait?
		if (!status.available()) {
			while (true) {
				CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
				if (status.available()) {
					break;
				} else if (status.getStatus() == CacheStatus.STARTING) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] is starting...");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						CmLogger.getLogger().error(e);
					}
				} else if (status.getStatus() == CacheStatus.FAILED) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					throw new IllegalStateException("cache [ " + cacheName + " ] initialization failed");
				} else if (status.getStatus() == CacheStatus.STOPPED) {
					// never happen
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization not started");
					boolean success = this.refreshCache(cacheName, true);
					if (!success) {
						CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					}
					break;
				}
			}
		}
		Cache cache = cacheManager.getCache(cacheName);
		return cache;
	}
	
	/**
	 * add or replace the cache item into cache identified by the given name
	 * @param cacheName
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean addOrUpdateObject(String cacheName, Object key, Object value) {
		if (cacheName == null || key == null || value == null) {
			throw new IllegalArgumentException("arguments could not be null: cacheName"+cacheName+" ,key"+key+" ,value"+value);
		}
		Cache legacy = getCache(cacheName);
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		if (legacy.get(key) != null) {
			legacy.replace(new net.sf.ehcache.Element(key, value));
		} else {
			legacy.put(new net.sf.ehcache.Element(key, value));
		}
		return true;
	}
	
	/**
	 * remove the cached item from cache
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public boolean removeObject(String cacheName, Object key) {
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		return legacy.remove(key);
	}
	
	/**
	 * load the cached item by the key/id
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public Object getObjectById(String cacheName, Object key) {
		
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return null;
		}
		
		net.sf.ehcache.Element ele = legacy.get(key);
		if (ele != null) {
			return ele.getObjectValue();
		} else {
			CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
		}
		
		// no value object found
		return null;
	}
	
	/**
	 * full-refresh the cache from db
	 * <br/>
	 * <font color="red">
	 * NOTE:<br/>
	 * consider the method below firstly before using this method:
	 * </font><br/>
	 * <ol>
	 * <li> {@code CacheService#addOrUpdateObject(String, Object, Object)}({@link #addOrUpdateObject(String, Object, Object)})</li>
	 * <li> {@code CacheService#removeObject(String, Object)}({@link #removeObject(String, Object)})</li>
	 * </ol>
	 * @param cacheName
	 * @return
	 */
	public boolean refreshCache(String cacheName) {
		return refreshCache(cacheName, !asynMode);
	}
	
	public void refreshCacheAll() {
		String[] cachesNames = this.getCacheManager().getCacheNames();
		for (String cacheName : cachesNames) {
			refreshCache(cacheName, !asynMode);
		}
	}
	
	/**
	 * full-refresh the cache from DB in a synchronized/non-synchronized mode
	 * @param cacheName
	 * @param synMode
	 * @return
	 */
	private boolean refreshCache(String cacheName, boolean synMode) {
		if (cacheName == null) {
			throw new IllegalArgumentException("cacheName could not be null");
		}
		Cache legacy = cacheManager.getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		boolean result = false;
		try {
			
			//refreshCacheInternal(legacy, CacheConfigs.get(cacheName));
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					legacy,
					cacheConfigs.get(cacheName),
					cacheStatusMap.get(cacheName),
					cacheLocks.get(cacheName),
					CacheRefreshingTask.ACTION_REFRESH
			);
			Future<Boolean> future = executorService.submit( task );
			if (synMode) {
				result = future.get();
			} else {
				result = true;
			}
			
		} catch (Exception exp) {
			CmLogger.getLogger().debug("refresh Cache " + cacheName + "failed.");
			CmLogger.getLogger().error(exp);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated
	public void refreshCacheInternal(Cache cache, CacheConfig cfg) throws Exception {
		if (cache == null || cfg == null) {
			throw new IllegalArgumentException("cache or cacheConfig could not be null.");
		}
		
		Object obj = null;
		if (springApplicationCtx != null) {
			obj = getLoadMethod().invoke(springApplicationCtx.getBean(cfg.getBeanName()), new Object[]{});
		} else {
			obj = getLoadMethod().invoke(cfg.getLoaderClazz().newInstance(), new Object[]{});
		}
		if (obj instanceof List) {
			List keys = cache.getKeys();
			Set toBeRemoved = new HashSet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				toBeRemoved.add(it.next());
			}
			List list = (List) obj;
			List newKeys = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cfg.getKeyMethod().invoke(v, new Object[]{});
				newKeys.add(k);
				cache.put(new net.sf.ehcache.Element(k, v));
			}
			toBeRemoved.removeAll(newKeys);
			if (toBeRemoved != null && toBeRemoved.size() > 0) {
				for (Iterator it = toBeRemoved.iterator(); it.hasNext();) {
					Object k = it.next();
					cache.remove(k);
				}
			}
		}
	}
	
	/**
	 * load all cached item from the given cache
	 * @param <T>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		List<T> list = new ArrayList<T>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			list.add((T)cache.get(it.next()).getObjectValue());
		}
		
		return list;
	}
	
	/**
	 * load multiple elements by the given keys
	 * @param <T>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <T> List<T> getListByIds(String cacheName, Object ... keys) {

		List<T> list = new ArrayList<T>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return list;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				list.add((T)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return list;
	}
	/**
	 * load multiple elements stored in map by the given keys
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <K, V> Map<K, V>  getMapByIds(String cacheName, Object ... keys) {
		
		Map<K, V> map = new HashMap<K, V>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return map;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				map.put((K)key, (V)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return map;
	}
	
	/**
	 * load all cached items from the given cache
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		Map<K, V> map = new HashMap<K, V>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			K key = (K)it.next();
			map.put(key, (V)cache.get(key).getObjectValue());
		}
		
		return map;
	}
	
	
	/**
	 * thread pool for refreshing cache
	 */
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	/**
	 * class for storing cache status
	 */
	private static class CacheStatus {
		
		public static final int STOPPED = 1;
		public static final int STARTING = 1<<1;
		public static final int FAILED = 1<<2;
		
		public static final int RUNNING = 1<<4;
		public static final int REFRESHING = 1<<5;
		
		private volatile int status = STOPPED;
		
		public void setStatus(int stts) {
			this.status = stts;
		}
		public int getStatus() {
			return this.status;
		}
		public String getStatusDesc() {
			switch(this.status) {
			case STOPPED : return "Stopped";
			case STARTING: return "Starting";
			case FAILED  : return "Failed";
			case RUNNING : return "Running";
			case REFRESHING: return "Refreshing";
			default: return "Unknown";
			}
		}
		
		public boolean available() {
			return this.status >= RUNNING;
		}
		
	}
	
	/**
	 * refresh cache task
	 */
	private static class CacheRefreshingTask implements Callable<Boolean> {
		
		private static final int ACTION_INIT = 1<<1;
		private static final int ACTION_REFRESH = 1<<2;
		

		private ApplicationContext appCtx;
		private CacheConfig cacheConfig;
		private Cache cache;
		private CacheStatus status;
		private ReentrantLock lock;
		private int action;
		
		public CacheRefreshingTask(
				ApplicationContext appCtx, 
				Cache cache,
				CacheConfig cacheConfig,
				CacheStatus status,
				ReentrantLock lock,
				int action
		) {
			this.appCtx = appCtx;
			this.cache = cache;
			this.cacheConfig = cacheConfig;
			this.status = status;
			this.lock = lock;
			this.action = action;
		}

		public Boolean call() {
			
			if (cache == null || cacheConfig == null || lock == null) {
				return false;
			}
			
			Thread current = Thread.currentThread();
			String legacyThreadName = current.getName();
			current.setName(cacheConfig.getCacheName());
			int firstStatus = status.getStatus();
			
			// tryLock is incorrect, use lock instead
			// this.lock.tryLock();
			boolean result = false;
			this.lock.lock();
			try {
				if (action == ACTION_INIT) {
					status.setStatus(CacheStatus.STARTING);
				} else if (action == ACTION_REFRESH){
					if (firstStatus < CacheStatus.RUNNING) {
						status.setStatus(CacheStatus.STARTING);
					} else {
						status.setStatus(CacheStatus.REFRESHING);
					}
				}
				try {
					long start = System.currentTimeMillis();
					doRefresh();
					long stop = System.currentTimeMillis();
					CmLogger.getLogger().debug("fill Cache [" + cache.getName() + "] cost time :" + (stop - start) / 1.0e3 + " sec");
					status.setStatus(CacheStatus.RUNNING);
					result = true;
					
				} catch(Exception exp) {
					CmLogger.getLogger().error(exp);
					status.setStatus(CacheStatus.FAILED);
					// ignore exception
					result = false;
				}
			} finally {
				this.lock.unlock();
			}
			current.setName(legacyThreadName);
			return result;
		}
		
		/**
		 * refresh the cache from db
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		private void doRefresh() throws Exception {
			Object obj = null;
			if (appCtx != null) {
				CmLogger.getLogger().debug("doRefresh:use spring.");
				obj = getLoadMethod().invoke(appCtx.getBean(cacheConfig.getBeanName()), new Object[]{});
			} else {
				CmLogger.getLogger().debug("doRefresh:use loader.");
				obj = getLoadMethod().invoke(cacheConfig.getLoaderClazz().newInstance(), new Object[]{});
			}
			if (obj instanceof List) {
				this.doRefreshInternal((List) obj);
			} else {
				throw new IllegalStateException("the returned type not supported");
			}
		}
		@SuppressWarnings("unchecked")
		private void doRefreshInternal(List list) throws Exception {

			
			cache.removeAll();
			List<net.sf.ehcache.Element> eles = new ArrayList<net.sf.ehcache.Element>();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cacheConfig.getKeyMethod().invoke(v, new Object[]{});
				eles.add(new net.sf.ehcache.Element(k, v));
			}
			cache.putAll(eles);
		
		}
		
	}
	
	
}
package com.cm.basic.cache.local;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.cm.logging.CmLogger;


/**
 * To use this cache service transparently, please don't call this service directly except from service layer
 * which cross cut by transaction manager
 */
public class CacheService implements ApplicationContextAware, DisposableBean {

    public void destroy() {
        this.cacheConfigs.clear();
        this.cacheLocks.clear();
        this.cacheStatusMap.clear();
        this.cacheManager.shutdown();
        this.executorService.shutdownNow();
    }

	/** cacheService config file name **/
	public static final String SERVICE_CONFIG = "cacheService.xml";
	
	/** ehcache config file name **/
	public static final String EHCACHE_CONFIG = "ehcache.xml";
	
	/**
	 * spring application context
	 */
	private ApplicationContext springApplicationCtx = null;
	
	/**
	 * ehcache cache manager
	 * in current implemenation, use singleton cache manager, 
	 * thus the ehcache config file will not be deleted while full building happened
	 */
	private CacheManager cacheManager = null;
	
	/**
	 * cache service config collection
	 */
	private Map<String, CacheConfig> cacheConfigs = new ConcurrentHashMap<String, CacheConfig>();

	/**
	 * cache lock collection
	 */
	private Map<String, ReentrantLock> cacheLocks = new ConcurrentHashMap<String, ReentrantLock>();
	
	/**
	 * cache status collection
	 */
	private Map<String, CacheStatus> cacheStatusMap = new ConcurrentHashMap<String, CacheStatus>();
	
	
	/**
	 * toggle for asynchronous cache initialization
	 */
	private boolean asynMode = true;
	
	
	public void setAsynMode(boolean asynMode) {
		this.asynMode = asynMode;
	}

	/**
	 * load config file from classloader root path
	 * @param fileName
	 * @return
	 */
	private InputStream loadConfigFromRootPath(String fileName) {
		InputStream is = CacheService.class.getClassLoader().getResourceAsStream(fileName);
		return is;
	}

//	private static CacheService instance;	
//	public static CacheService getInstance() {
//		if (instance == null) {
//			synchronized (CacheService.class) {
//				instance = new CacheService();
//			}
//		}
//		return instance;
//	}

	/**
	 * mark private to make sure no new instance created in programmer way
	 */
	private CacheService () {
		
		try {
			InputStream is = loadConfigFromRootPath(EHCACHE_CONFIG);
			if (is == null) {
				CmLogger.getLogger().debug("ehcache config " + EHCACHE_CONFIG + " not found");
				return;
			}
			cacheManager = CacheManager.create(is);
			
		} catch (Exception e) {
			System.out.println("CacheService initialization failed.");
			CmLogger.getLogger().error(e);
		}
	}
	
	public CacheManager getCacheManager() {
		return this.cacheManager;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		springApplicationCtx = ctx;
		try {
			loadConfig();
		} catch (Exception e) {
			CmLogger.getLogger().error(e);
		}
	}
	
	/**
	 * load config from file and then fill cache base the config
	 * @throws Exception
	 */
	private void loadConfig() throws Exception {
		InputStream is = loadConfigFromRootPath(SERVICE_CONFIG);
		if (is == null){
			CmLogger.getLogger().debug("cache service config " + SERVICE_CONFIG + " found.");
			return;
		}
		
		// 1. load xml & then parse
		List<CacheConfig> configs = parseConfig(is);
		
		// 2. refresh cache based on config
		for (CacheConfig cfg : configs) {
			Cache cache = cacheManager.getCache(cfg.getCacheName());
			if (cache != null) {
				// store config
				cacheConfigs.put(cfg.getCacheName(), cfg);
				
				// store status for each cache
				CacheStatus status = new CacheStatus();
				cacheStatusMap.put(cfg.getCacheName(), status);
				
				// store lock for each cache
				ReentrantLock lock = new ReentrantLock();
				cacheLocks.put(cfg.getCacheName(), lock);
				
				// enrich cache from db
				fillCache(cache, cfg, status, lock);
			} else {
				CmLogger.getLogger().debug("no cache [ " + cfg.getCacheName() + "] found.");
			}
		}
	}
	
	
	/**
	 * get loader method then will be invoked by reflection
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	private static Method getLoadMethod () throws SecurityException, NoSuchMethodException {
		return CacheLoader.class.getMethod("loadAll", new Class[]{});
	}
	
	/**
	 * parse cache service config
	 * @param configFile
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private List<CacheConfig> parseConfig(InputStream configFile) throws Exception {
		Document doc = getDocument(configFile);
		Element root = doc.getRootElement();
		List<Node> caches = root.selectNodes("cache");
		List<CacheConfig> configs = new ArrayList<CacheConfig>();
		for (Node ele : caches) {
			CacheConfig config = new CacheConfig();
			config.setCacheName(ele.valueOf("@name"));
			
			Node keyEle = ele.selectSingleNode("key");
			
			Class keyClz = Class.forName(keyEle.valueOf("@class"));
			
			config.setKeyClazz(keyClz);
			config.setKeyMethod(keyClz.getMethod(keyEle.valueOf("@method"), new Class[]{}));
			
			Node srcEle = ele.selectSingleNode("loader");
			Class srcClz = Class.forName(srcEle.valueOf("@class"));
			config.setLoaderClazz(srcClz);
			
			Node beanEle = ele.selectSingleNode("bean");
			if (beanEle != null)
				config.setBeanName(beanEle.valueOf("@id"));
			
			configs.add(config);
		}
		return configs;
	}
	
	/**
	 * get dom4j document from inputstream
	 * @param file
	 * @return
	 */
	private static Document getDocument(final InputStream file) {
		Document document = null;
		SAXReader reader = new SAXReader();
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * fill cache with the records from db based on config
	 * @param cache
	 * @param cfg
	 * @param status
	 * @param lock
	 * @return
	 */
	private boolean fillCache(Cache cache, CacheConfig cfg, CacheStatus status, ReentrantLock lock) {
		boolean result = false;
		try {
			//refreshCacheInternal(cache, cfg);
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					cache,
					cfg,
					status,
					lock,
					CacheRefreshingTask.ACTION_INIT
			);
			
			CmLogger.getLogger().debug("filling cache [" + cache.getName() + "]......");
			Future<Boolean> future = executorService.submit(task);
			if (!asynMode) {
				result = future.get();
			} else {
				result = true;
			}
		} catch (Exception e) {
			result = false;
			CmLogger.getLogger().debug("refresh cache [" + cache.getName() + "] failed.");
			CmLogger.getLogger().error(e);
		}
		return result;
		
	}
	
	/**
	 * get cache from cache manager by given name
	 * @param cacheName
	 * @return
	 */
	public Cache getCache(String cacheName) {
		
		CacheStatus status = cacheStatusMap.get(cacheName);
		if (status == null) {
			// no cache configed
			return null;
		}
		CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
		
		// TODO if cache have not started, raise error or wait?
		if (!status.available()) {
			while (true) {
				CmLogger.getLogger().debug("cache [" + cacheName + "] status: " + status.getStatusDesc());
				if (status.available()) {
					break;
				} else if (status.getStatus() == CacheStatus.STARTING) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] is starting...");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						CmLogger.getLogger().error(e);
					}
				} else if (status.getStatus() == CacheStatus.FAILED) {
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					throw new IllegalStateException("cache [ " + cacheName + " ] initialization failed");
				} else if (status.getStatus() == CacheStatus.STOPPED) {
					// never happen
					CmLogger.getLogger().debug("cache [" + cacheName + "] initialization not started");
					boolean success = this.refreshCache(cacheName, true);
					if (!success) {
						CmLogger.getLogger().debug("cache [" + cacheName + "] initialization failed");
					}
					break;
				}
			}
		}
		Cache cache = cacheManager.getCache(cacheName);
		return cache;
	}
	
	/**
	 * add or replace the cache item into cache identified by the given name
	 * @param cacheName
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean addOrUpdateObject(String cacheName, Object key, Object value) {
		if (cacheName == null || key == null || value == null) {
			throw new IllegalArgumentException("arguments could not be null: cacheName"+cacheName+" ,key"+key+" ,value"+value);
		}
		Cache legacy = getCache(cacheName);
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		if (legacy.get(key) != null) {
			legacy.replace(new net.sf.ehcache.Element(key, value));
		} else {
			legacy.put(new net.sf.ehcache.Element(key, value));
		}
		return true;
	}
	
	/**
	 * remove the cached item from cache
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public boolean removeObject(String cacheName, Object key) {
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		return legacy.remove(key);
	}
	
	/**
	 * load the cached item by the key/id
	 * @param cacheName
	 * @param key
	 * @return
	 */
	public Object getObjectById(String cacheName, Object key) {
		
		if (cacheName == null || key == null) {
			throw new IllegalArgumentException("arguments could not be null");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return null;
		}
		
		net.sf.ehcache.Element ele = legacy.get(key);
		if (ele != null) {
			return ele.getObjectValue();
		} else {
			CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
		}
		
		// no value object found
		return null;
	}
	
	/**
	 * full-refresh the cache from db
	 * <br/>
	 * <font color="red">
	 * NOTE:<br/>
	 * consider the method below firstly before using this method:
	 * </font><br/>
	 * <ol>
	 * <li> {@code CacheService#addOrUpdateObject(String, Object, Object)}({@link #addOrUpdateObject(String, Object, Object)})</li>
	 * <li> {@code CacheService#removeObject(String, Object)}({@link #removeObject(String, Object)})</li>
	 * </ol>
	 * @param cacheName
	 * @return
	 */
	public boolean refreshCache(String cacheName) {
		return refreshCache(cacheName, !asynMode);
	}
	
	public void refreshCacheAll() {
		String[] cachesNames = this.getCacheManager().getCacheNames();
		for (String cacheName : cachesNames) {
			refreshCache(cacheName, !asynMode);
		}
	}
	
	/**
	 * full-refresh the cache from DB in a synchronized/non-synchronized mode
	 * @param cacheName
	 * @param synMode
	 * @return
	 */
	private boolean refreshCache(String cacheName, boolean synMode) {
		if (cacheName == null) {
			throw new IllegalArgumentException("cacheName could not be null");
		}
		Cache legacy = cacheManager.getCache(cacheName);
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return false;
		}
		
		boolean result = false;
		try {
			
			//refreshCacheInternal(legacy, CacheConfigs.get(cacheName));
			Callable<Boolean> task = new CacheRefreshingTask(
					springApplicationCtx,
					legacy,
					cacheConfigs.get(cacheName),
					cacheStatusMap.get(cacheName),
					cacheLocks.get(cacheName),
					CacheRefreshingTask.ACTION_REFRESH
			);
			Future<Boolean> future = executorService.submit( task );
			if (synMode) {
				result = future.get();
			} else {
				result = true;
			}
			
		} catch (Exception exp) {
			CmLogger.getLogger().debug("refresh Cache " + cacheName + "failed.");
			CmLogger.getLogger().error(exp);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated
	public void refreshCacheInternal(Cache cache, CacheConfig cfg) throws Exception {
		if (cache == null || cfg == null) {
			throw new IllegalArgumentException("cache or cacheConfig could not be null.");
		}
		
		Object obj = null;
		if (springApplicationCtx != null) {
			obj = getLoadMethod().invoke(springApplicationCtx.getBean(cfg.getBeanName()), new Object[]{});
		} else {
			obj = getLoadMethod().invoke(cfg.getLoaderClazz().newInstance(), new Object[]{});
		}
		if (obj instanceof List) {
			List keys = cache.getKeys();
			Set toBeRemoved = new HashSet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				toBeRemoved.add(it.next());
			}
			List list = (List) obj;
			List newKeys = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cfg.getKeyMethod().invoke(v, new Object[]{});
				newKeys.add(k);
				cache.put(new net.sf.ehcache.Element(k, v));
			}
			toBeRemoved.removeAll(newKeys);
			if (toBeRemoved != null && toBeRemoved.size() > 0) {
				for (Iterator it = toBeRemoved.iterator(); it.hasNext();) {
					Object k = it.next();
					cache.remove(k);
				}
			}
		}
	}
	
	/**
	 * load all cached item from the given cache
	 * @param <T>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		List<T> list = new ArrayList<T>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			list.add((T)cache.get(it.next()).getObjectValue());
		}
		
		return list;
	}
	
	/**
	 * load multiple elements by the given keys
	 * @param <T>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <T> List<T> getListByIds(String cacheName, Object ... keys) {

		List<T> list = new ArrayList<T>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return list;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				list.add((T)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return list;
	}
	/**
	 * load multiple elements stored in map by the given keys
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @param keys
	 * @return
	 */
	public <K, V> Map<K, V>  getMapByIds(String cacheName, Object ... keys) {
		
		Map<K, V> map = new HashMap<K, V>();
		if (cacheName == null || keys.length == 0) {
			throw new IllegalArgumentException("arguments could not be null/empty");
		}
		Cache legacy = getCache(cacheName);
		
		// no cache found
		if (legacy == null) {
			CmLogger.getLogger().debug("cache [" + cacheName + "] not found");
			return map;
		}
		
		for (Object key : keys) {
			net.sf.ehcache.Element ele = legacy.get(key);
			if (ele != null) {
				map.put((K)key, (V)ele.getObjectValue());
			} else {
				CmLogger.getLogger().debug("no item found in cache[" + cacheName + "] by key: " + key);
			}
		}
		
		return map;
	}
	
	/**
	 * load all cached items from the given cache
	 * @param <K>
	 * @param <V>
	 * @param cacheName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String cacheName) {
		Cache cache = getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("no cache found for " + cacheName);
		}
		Map<K, V> map = new HashMap<K, V>();
		for (Iterator it = cache.getKeys().iterator(); it.hasNext();) {
			K key = (K)it.next();
			map.put(key, (V)cache.get(key).getObjectValue());
		}
		
		return map;
	}
	
	
	/**
	 * thread pool for refreshing cache
	 */
	private ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	/**
	 * class for storing cache status
	 */
	private static class CacheStatus {
		
		public static final int STOPPED = 1;
		public static final int STARTING = 1<<1;
		public static final int FAILED = 1<<2;
		
		public static final int RUNNING = 1<<4;
		public static final int REFRESHING = 1<<5;
		
		private volatile int status = STOPPED;
		
		public void setStatus(int stts) {
			this.status = stts;
		}
		public int getStatus() {
			return this.status;
		}
		public String getStatusDesc() {
			switch(this.status) {
			case STOPPED : return "Stopped";
			case STARTING: return "Starting";
			case FAILED  : return "Failed";
			case RUNNING : return "Running";
			case REFRESHING: return "Refreshing";
			default: return "Unknown";
			}
		}
		
		public boolean available() {
			return this.status >= RUNNING;
		}
		
	}
	
	/**
	 * refresh cache task
	 */
	private static class CacheRefreshingTask implements Callable<Boolean> {
		
		private static final int ACTION_INIT = 1<<1;
		private static final int ACTION_REFRESH = 1<<2;
		

		private ApplicationContext appCtx;
		private CacheConfig cacheConfig;
		private Cache cache;
		private CacheStatus status;
		private ReentrantLock lock;
		private int action;
		
		public CacheRefreshingTask(
				ApplicationContext appCtx, 
				Cache cache,
				CacheConfig cacheConfig,
				CacheStatus status,
				ReentrantLock lock,
				int action
		) {
			this.appCtx = appCtx;
			this.cache = cache;
			this.cacheConfig = cacheConfig;
			this.status = status;
			this.lock = lock;
			this.action = action;
		}

		public Boolean call() {
			
			if (cache == null || cacheConfig == null || lock == null) {
				return false;
			}
			
			Thread current = Thread.currentThread();
			String legacyThreadName = current.getName();
			current.setName(cacheConfig.getCacheName());
			int firstStatus = status.getStatus();
			
			// tryLock is incorrect, use lock instead
			// this.lock.tryLock();
			boolean result = false;
			this.lock.lock();
			try {
				if (action == ACTION_INIT) {
					status.setStatus(CacheStatus.STARTING);
				} else if (action == ACTION_REFRESH){
					if (firstStatus < CacheStatus.RUNNING) {
						status.setStatus(CacheStatus.STARTING);
					} else {
						status.setStatus(CacheStatus.REFRESHING);
					}
				}
				try {
					long start = System.currentTimeMillis();
					doRefresh();
					long stop = System.currentTimeMillis();
					CmLogger.getLogger().debug("fill Cache [" + cache.getName() + "] cost time :" + (stop - start) / 1.0e3 + " sec");
					status.setStatus(CacheStatus.RUNNING);
					result = true;
					
				} catch(Exception exp) {
					CmLogger.getLogger().error(exp);
					status.setStatus(CacheStatus.FAILED);
					// ignore exception
					result = false;
				}
			} finally {
				this.lock.unlock();
			}
			current.setName(legacyThreadName);
			return result;
		}
		
		/**
		 * refresh the cache from db
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		private void doRefresh() throws Exception {
			Object obj = null;
			if (appCtx != null) {
				CmLogger.getLogger().debug("doRefresh:use spring.");
				obj = getLoadMethod().invoke(appCtx.getBean(cacheConfig.getBeanName()), new Object[]{});
			} else {
				CmLogger.getLogger().debug("doRefresh:use loader.");
				obj = getLoadMethod().invoke(cacheConfig.getLoaderClazz().newInstance(), new Object[]{});
			}
			if (obj instanceof List) {
				this.doRefreshInternal((List) obj);
			} else {
				throw new IllegalStateException("the returned type not supported");
			}
		}
		@SuppressWarnings("unchecked")
		private void doRefreshInternal(List list) throws Exception {

			
			cache.removeAll();
			List<net.sf.ehcache.Element> eles = new ArrayList<net.sf.ehcache.Element>();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Object v = it.next();
				Object k = cacheConfig.getKeyMethod().invoke(v, new Object[]{});
				eles.add(new net.sf.ehcache.Element(k, v));
			}
			cache.putAll(eles);
		
		}
		
	}
	
	
}