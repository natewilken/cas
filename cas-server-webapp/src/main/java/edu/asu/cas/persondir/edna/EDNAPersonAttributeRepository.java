package edu.asu.cas.persondir.edna;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import javax.naming.InitialContext;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.sql.DataSource;

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

    //@PersistenceContext(unitName="edna")
	//protected EntityManager entityManager;
	
	protected DataSource ednaDataSource;
	
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
		ednaDataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/edna-ds");
		
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
			Future<IPersonAttributes> result = executor.submit(new Worker(ednaDataSource, username));
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
		DataSource dataSource;
		String username;
		
		Worker(final DataSource dataSource, final String username) {
			this.dataSource = dataSource;
			this.username = username;
		}

		public IPersonAttributes call() throws Exception {
			Connection connection = null;
			PreparedStatement statement = null;
			ResultSet resultSet = null;
			
			Map<String,List<Object>> attributeMap = null;
			
			try {
				connection = dataSource.getConnection();
				statement = connection.prepareStatement(EDNAPersonSQL.SELECT_PERSON.toString());
				statement.setString(1, username);
				
				resultSet = statement.executeQuery();
				
				if (resultSet.next()) {
					
					attributeMap = new HashMap<String,List<Object>>();
					String emplId = resultSet.getString(1);
					Number pwState = resultSet.getInt(2); // 0 if null
					Date pwExpirationDate = resultSet.getTimestamp(3);
					Date pwLastChangeDate = resultSet.getTimestamp(4);
					Number principalType = resultSet.getInt(5); // 0 if null
					String principalTypeSDesc = resultSet.getString(6);
					String givenName = resultSet.getString(7);
					String middleName = resultSet.getString(8);
					String sn = resultSet.getString(9);
					String principal = resultSet.getString(10);
					String principalScoped = resultSet.getString(11);
					Number loginDiversionState = (resultSet.getObject(12) != null) ? resultSet.getInt(12) : null;
					
					attributeMap.put("emplId", new ArrayList<Object>(Collections.singleton(emplId)));
					attributeMap.put("passwordStateFlag", new ArrayList<Object>(Collections.singleton(pwState)));
					attributeMap.put("passwordExpirationDate", new ArrayList<Object>(Collections.singleton(pwExpirationDate)));
					attributeMap.put("passwordLastChangeDate", new ArrayList<Object>(Collections.singleton(pwLastChangeDate)));
					attributeMap.put("principalType", new ArrayList<Object>(Collections.singleton(principalType)));
					attributeMap.put("principalTypeSDesc", new ArrayList<Object>(Collections.singleton(principalTypeSDesc)));
					attributeMap.put("givenName", new ArrayList<Object>(Collections.singleton(givenName)));
					attributeMap.put("middleName", new ArrayList<Object>(Collections.singleton(middleName)));
					attributeMap.put("sn", new ArrayList<Object>(Collections.singleton(sn)));
					attributeMap.put("principal", new ArrayList<Object>(Collections.singleton(principal)));
					attributeMap.put("principalScoped", new ArrayList<Object>(Collections.singleton(principalScoped)));
					attributeMap.put("loginDiversionState", new ArrayList<Object>(Collections.singleton(loginDiversionState)));
					
				} else {
					throw new NoResultException();
				}
				
				if (resultSet.next()) {
					throw new NonUniqueResultException();
				}
				
				return new CaseInsensitiveNamedPersonImpl(username, attributeMap);
				
			} finally {
				if (resultSet != null) {
					try {
						resultSet.close();
					} catch (Throwable t) {
						// intentionally blank
					}
				}
				if (statement != null) {
					try {
						statement.close();
					} catch (Throwable t) {
						// intentionally blank
					}
				}
				if (connection != null) {
					try {
						connection.close();
					} catch (Throwable t) {
						// intentionally blank
					}
				}
			}
		}
	}

}
