/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cm.basic.cache.local;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.CacheManager;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.NamedThreadLocal;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.cm.logging.CmLogger;


@SuppressWarnings("serial")
class CacheServiceTransactionManager extends AbstractPlatformTransactionManager implements InitializingBean, Serializable {

	private NamedThreadLocal<TxStatus> transactions = new NamedThreadLocal<TxStatus>("cacheServiceTransactions");
	private CacheService cacheService = null;
	
	public void setCacheService(CacheService cacheService) {
		this.cacheService = cacheService;
	}

	public CacheServiceTransactionManager() {}
	protected CacheServiceTransactionManager(CacheService cacheService) {
		// setTransactionSynchronization(SYNCHRONIZATION_NEVER);
		this.cacheService = cacheService;
	}
	
	private static class TxStatus {
		TxStatus(){
			this.isNewTransaction = true;
		}
		
		private volatile boolean isNewTransaction = true;
		private ReentrantLock lock = new ReentrantLock();
		private AtomicLong count = new AtomicLong(0);
	}
	
	private CacheManager getCacheManager() {
		return this.cacheService.getCacheManager();
		
	}
	
	private void doBeginInternal() {
		CacheManager cm = getCacheManager();
		if (cm != null) {
			cm.getTransactionController().begin(10 * 60);
		}
		
	}
	private void doCommitInternal() {
		CacheManager cm = getCacheManager();
		if (cm != null) {
			cm.getTransactionController().commit(false);
		}
	}
	private void doRollbackInternal() {
		CacheManager cm = getCacheManager();
		if (cm != null) {
			cm.getTransactionController().rollback();
		}
	}

	protected Object doGetTransaction() {
		System.out.println("CacheServiceTransactionManager#doGetTransaction");
		
		TxStatus txId = transactions.get();
		if (txId == null) {
			transactions.set(new TxStatus());
			txId = transactions.get();
		} 
	
		return txId;
	}

	/**
	 * no propagation support
	 */
	protected boolean isExistingTransaction(Object transaction) {
		return false;
	}

	protected void doBegin(Object transaction, TransactionDefinition definition) {
		if (!transactions.get().equals(transaction)) {
			throw new IllegalArgumentException("Not the same transaction object");
		}
		TxStatus txStts = (TxStatus) transaction;
		txStts.count.incrementAndGet();
		if (txStts.isNewTransaction) {
			txStts.lock.lock();
			try {
				if (txStts.isNewTransaction) {
					System.out.println("tx begin...");
					txStts.isNewTransaction = false;
					doBeginInternal();
				} else {
					System.out.println("__tx begin...");
				}
			} finally {
				txStts.lock.unlock();
			}
		} else {
			System.out.println("__tx begin...");
		}
	}

	protected void doCommit(DefaultTransactionStatus status) {
		if (!transactions.get().equals(status.getTransaction())) {
			throw new IllegalArgumentException("Not the same transaction object");
		}
		TxStatus tx = transactions.get();
		
		//if back to the beginning
		if (tx.count.decrementAndGet() <= 0) {
			
			// do commit operation
			doCommitInternal();
			
			// reset tx status as null
			transactions.set(null);
			System.out.println("tx commit...");
		} else {
			System.out.println("__tx commit...");
		}
	}

	protected void doRollback(DefaultTransactionStatus status) {
		if (!transactions.get().equals(status.getTransaction())) {
			throw new IllegalArgumentException("Not the same transaction object");
		}
		TxStatus tx = transactions.get();
		if (tx.count.decrementAndGet() <= 0) {
			System.out.println("tx rollback...");
			
			try {
				// do rollback operation
				doRollbackInternal();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				CmLogger.getLogger().error(e);
			}
			
			// reset tx status as null
			transactions.set(null);
		} else {
			System.out.println("__tx rollback...");
		}
	}

	public void afterPropertiesSet() throws Exception {
		
		// make sure the cache service is available
		if (this.cacheService == null) {
			throw new IllegalStateException("No CacheService available");
		}
	}
}
