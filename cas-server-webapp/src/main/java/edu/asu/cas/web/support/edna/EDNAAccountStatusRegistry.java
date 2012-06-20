package edu.asu.cas.web.support.edna;

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.internet.InternetAddress;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import edu.asu.cas.web.support.AccountStatus;
import edu.asu.cas.web.support.AccountStatusException;
import edu.asu.cas.web.support.AccountStatusRegistry;
import edu.asu.cas.web.support.PasswordState;
import edu.asu.cas.web.support.PasswordStateException;

public class EDNAAccountStatusRegistry implements AccountStatusRegistry, InitializingBean {
	public static int DEFAULT_WORKER_COUNT = 100;
	public static long DEFAULT_WORKER_TIMEOUT_MILLIS = 10000;
	
	private static Logger logger = LoggerFactory.getLogger(EDNAAccountStatusRegistry.class);

    @PersistenceContext(unitName="edna")
	protected EntityManager entityManager;
	
	protected ThreadPoolExecutor executor;
	protected ExecutorAlertNotifier alertNotifier;
	protected String alertSender;
	protected String alertSubject;
	protected String alertRecipients;
	protected int workerCount = DEFAULT_WORKER_COUNT;
	protected long workerTimeoutMillis = DEFAULT_WORKER_TIMEOUT_MILLIS;
	
	public void setAlertSender(String alertSender) {
		this.alertSender = alertSender;
	}
	
	public void setAlertSubject(String alertSubject) {
		this.alertSubject = alertSubject;
	}
	
	public void setAlertRecipients(String alertRecipients) {
		this.alertRecipients = alertRecipients;
	}
	
	public void setWorkerCount(int workerCount) {
		this.workerCount = workerCount;
	}
	
	public void setWorkerTimeoutMillis(long workerTimeoutMillis) {
		this.workerTimeoutMillis = workerTimeoutMillis;
	}
	
	public void afterPropertiesSet() throws Exception {
		executor = new ThreadPoolExecutor(workerCount, workerCount, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(true), new ThreadFactory() {
			private AtomicInteger threadNumber = new AtomicInteger(1);
			
			public Thread newThread(Runnable task) {
				Thread thread = new Thread(Thread.currentThread().getThreadGroup(), task,
						"AcctStatus-exec-" + threadNumber.getAndIncrement());
				thread.setDaemon(true);
				thread.setPriority(Thread.NORM_PRIORITY);
				
				return thread;
			}
		});
		
		InternetAddress senderAddress = null;
		try {
			senderAddress = new InternetAddress(alertSender, false);
		} catch (Exception e) {
			logger.error("couldn't parse notifier sender address: " + alertSender);
			throw e;
		}
		
		String subject = alertSubject;
		try {
			subject += " - " + InetAddress.getLocalHost().getCanonicalHostName();
		} catch (Exception e) {
			logger.warn("couldn't lookup local hostname");
		}
		
		InternetAddress[] recipientAddresses = null;
		try {
			recipientAddresses = InternetAddress.parse(alertRecipients, false);
		} catch (Exception e) {
			logger.error("couldn't parse notifier recipient addresses: " + alertRecipients);
			throw e;
		}
		
		alertNotifier = new ExecutorAlertNotifier(executor, senderAddress, subject, recipientAddresses);
	}
	
	public AccountStatus getAccountStatus(String principal) throws AccountStatusException {
		try {
			Future<EDNAAccountStatus> status = executor.submit(new AccountStatusWorker(entityManager, principal));
			return status.get(workerTimeoutMillis, TimeUnit.MILLISECONDS);
			
		} catch (RejectedExecutionException e) {
			alertNotifier.handleRejectedExecution(principal, e);
			throw new AccountStatusException("account status request execution was rejected for [" + principal + "]", e);
			
		} catch (TimeoutException e) {
			alertNotifier.handleTimeout(principal, e);
			throw new AccountStatusException("account status request timed out for [" + principal + "]", e);
			
		} catch (NoResultException e) {
			alertNotifier.handleMissingPrincipal(principal, e);
			throw new AccountStatusException("no principal found for [" + principal + "]", e);
			
		} catch (Throwable t) {
			alertNotifier.handleUnknownError(principal, t);
			throw new AccountStatusException("exception while executing account status request for [" + principal + "]", t);
		}
	}

	private static final class AccountStatusWorker implements Callable<EDNAAccountStatus> {
		EntityManager entityManager;
		String principal;
		
		AccountStatusWorker(final EntityManager entityManager, final String principal) {
			this.entityManager = entityManager;
			this.principal = principal;
		}

		public EDNAAccountStatus call() throws Exception {
			Query query = entityManager.createNativeQuery(EDNAAccountStatusSQL.SELECT_ACCOUNT_STATUS.toString());
			query.setParameter("principal", principal);
			
			Object[] result = (Object[])query.getSingleResult();
			int pwState = (result[0] != null) ? ((Number)result[0]).intValue() : 0;
			Date pwExpirationDate = (Date)result[1];
			Date pwLastChangeDate = (Date)result[2];
			
			if (pwState == 2) {
				return new EDNAAccountStatus(PasswordState.ADMIN_FORCED_CHANGE, pwExpirationDate, pwLastChangeDate);
				
			} else if (pwState == 1) {
				
				Date now = new Date();
				
				if (pwExpirationDate == null) {
					throw new PasswordStateException("password state == 1, but expiration date is null");
					
				} else if (now.before(pwExpirationDate)) {
					return new EDNAAccountStatus(PasswordState.WARN, pwExpirationDate, pwLastChangeDate);
					
				} else {
					return new EDNAAccountStatus(PasswordState.EXPIRED, pwExpirationDate, pwLastChangeDate);
				}
				
			} else {
				return new EDNAAccountStatus(PasswordState.OK, pwExpirationDate, pwLastChangeDate);
			}
		}
	}

}
