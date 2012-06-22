package edu.asu.cas.persondir.edna;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import org.jasig.services.persondir.IPersonAttributes;
import org.jasig.services.persondir.support.AbstractDefaultAttributePersonAttributeDao;
import org.jasig.services.persondir.support.CaseInsensitiveNamedPersonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class EDNAPersonAttributeRepository extends AbstractDefaultAttributePersonAttributeDao implements InitializingBean {

	public static int DEFAULT_WORKER_COUNT = 100;
	public static long DEFAULT_WORKER_TIMEOUT_MILLIS = 10000;
	
	private static Logger logger = LoggerFactory.getLogger(EDNAPersonAttributeRepository.class);

    @PersistenceContext(unitName="edna")
	protected EntityManager entityManager;
	
    protected Set<String> userAttributeNames = null;
	protected String alertSender;
	protected String alertSubject;
	protected String alertRecipients;
	protected int workerCount = DEFAULT_WORKER_COUNT;
	protected long workerTimeoutMillis = DEFAULT_WORKER_TIMEOUT_MILLIS;
	
    protected ThreadPoolExecutor executor;
	protected ExecutorAlertNotifier alertNotifier;
	
	public void setUserAttributeNames(Set<String> userAttributeNames) {
		this.userAttributeNames = Collections.unmodifiableSet(userAttributeNames);
	}
	
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
						"EDNAPersonAttributeRepository-worker-" + threadNumber.getAndIncrement());
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
	
	public Set<String> getAvailableQueryAttributes() {
		return null;
	}

	@Override
	public IPersonAttributes getPerson(String username) {
		try {
			Future<IPersonAttributes> result = executor.submit(new Worker(entityManager, username));
			return result.get(workerTimeoutMillis, TimeUnit.MILLISECONDS);
			
		} catch (RejectedExecutionException e) {
			alertNotifier.handleRejectedExecution(username, e);
			logger.error("account status request execution was rejected for [" + username + "]", e);
			
		} catch (TimeoutException e) {
			alertNotifier.handleTimeout(username, e);
			logger.error("account status request timed out for [" + username + "]", e);
			
		} catch (NoResultException e) {
			alertNotifier.handleMissingPrincipal(username, e);
			logger.error("no principal found for [" + username + "]", e);
			
		} catch (Throwable t) {
			alertNotifier.handleUnknownError(username, t);
			logger.error("exception while executing account status request for [" + username + "]", t);
		}
		
		return null;
	}

	public Set<IPersonAttributes> getPeopleWithMultivaluedAttributes(Map<String,List<Object>> attributes) {
		throw new UnsupportedOperationException();
	}

	public Set<String> getPossibleUserAttributeNames() {
		return userAttributeNames;
	}

	private static final class Worker implements Callable<IPersonAttributes> {
		EntityManager entityManager;
		String username;
		
		Worker(final EntityManager entityManager, final String username) {
			this.entityManager = entityManager;
			this.username = username;
		}

		public IPersonAttributes call() throws Exception {
			Query query = entityManager.createNativeQuery(EDNAPersonSQL.SELECT_PERSON.toString());
			query.setParameter("username", username);
			
			Object[] result = (Object[])query.getSingleResult();
			String emplId = (String)result[0];
			Number pwState = (result[1] != null) ? (Number)result[1] : 0;
			Date pwExpirationDate = (Date)result[2];
			Date pwLastChangeDate = (Date)result[3];
			Number principalType = (result[4] != null) ? (Number)result[4] : 0;
			String principalTypeSDesc = (String)result[5];
			String firstName = (String)result[6];
			String middleName = (String)result[7];
			String lastName = (String)result[8];

			Map<String,List<Object>> mapOfLists = new HashMap<String,List<Object>>();
			mapOfLists.put("emplId", new ArrayList<Object>(Collections.singleton(emplId)));
			mapOfLists.put("pwState", new ArrayList<Object>(Collections.singleton(pwState)));
			mapOfLists.put("pwExpirationDate", new ArrayList<Object>(Collections.singleton(pwExpirationDate)));
			mapOfLists.put("pwLastChangeDate", new ArrayList<Object>(Collections.singleton(pwLastChangeDate)));
			mapOfLists.put("principalType", new ArrayList<Object>(Collections.singleton(principalType)));
			mapOfLists.put("principalTypeSDesc", new ArrayList<Object>(Collections.singleton(principalTypeSDesc)));
			mapOfLists.put("firstName", new ArrayList<Object>(Collections.singleton(firstName)));
			mapOfLists.put("middleName", new ArrayList<Object>(Collections.singleton(middleName)));
			mapOfLists.put("lastName", new ArrayList<Object>(Collections.singleton(lastName)));

			return new CaseInsensitiveNamedPersonImpl(username, mapOfLists);
		}
	}

}
