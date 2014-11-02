package com.cm.basic.cache.local;

import java.util.List;

public interface CacheLoader <V> {
	
	List<V> loadAll();
}
