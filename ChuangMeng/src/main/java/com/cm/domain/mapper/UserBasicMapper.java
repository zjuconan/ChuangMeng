package com.cm.domain.mapper;

import com.cm.basic.cache.local.CacheLoader;
import com.cm.domain.UserBasic;

public interface UserBasicMapper extends CacheLoader<UserBasic> {
	public UserBasic getUser(int userId);
}
