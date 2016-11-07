package edu.asu.cas.persondir.edna;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.NoResultException;

import org.apache.log4j.Logger;

public class ExecutorAlertNotifier extends TimerTask {
	
	protected static final Logger logger = Logger.getLogger(ExecutorAlertNotifier.class);
	
	protected final BlockingQueue<AlertMessage> messageQueue = new LinkedBlockingQueue<AlertMessage>();
	protected final ThreadPoolExecutor monitoredExecutor;
	protected final Properties mailProps;
	protected final InternetAddress alertSender;
	protected final String alertSubject;
	protected final InternetAddress[] alertRecipients;
	
	public ExecutorAlertNotifier(ThreadPoolExecutor executor, InternetAddress alertSender, String alertSubject, InternetAddress[] alertRecipients) {
		this.monitoredExecutor = executor;
		
		mailProps = new Properties();
		mailProps.put("mail.smtp.host", "smtp.asu.edu");
		
		this.alertSender = alertSender;
		this.alertSubject = alertSubject;
		this.alertRecipients = alertRecipients;
		
		Timer timer = new Timer("ExecutorAlertNotifier-timer", true);
		timer.scheduleAtFixedRate(this, 0, TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));
	}
	
	public void handleRejectedExecution(String principal, RejectedExecutionException e) {
		queueMessage("rejected execution for [" + principal + "]", e);
	}
	
	public void handleTimeout(String principal, TimeoutException e) {
		queueMessage("execution timeout for [" + principal + "]", null);
	}
	
	public void handleMissingPrincipal(String principal, NoResultException e) {
		queueMessage("no account status result found for [" + principal + "]", null);
	}
	
	public void handleUnknownError(String principal, Throwable t) {
		queueMessage("unknown error for [" + principal + "]", t);
	}
	
	protected void queueMessage(String alert, Throwable error) {
		String message = "[active/max/completed: " + monitoredExecutor.getActiveCount() + "/" + monitoredExecutor.getMaximumPoolSize()
				+ "/" + monitoredExecutor.getCompletedTaskCount() + "] " + alert;
		
		messageQueue.offer(new AlertMessage(new Date(), message, error));
	}
	
	protected void dispatchMessage(String content) throws UnsupportedEncodingException, MessagingException {
		Session session = Session.getDefaultInstance(mailProps, null);
		
		MimeMessage message = new MimeMessage(session);
		
		message.setFrom(alertSender);
		message.setSubject(alertSubject);
		message.addRecipients(Message.RecipientType.TO, alertRecipients);
		
		message.setContent(content, "text/plain");
		message.saveChanges();
		
		Transport.send(message);
	}
	
	public void run() {
		try {
			if ((monitoredExecutor.getActiveCount() / monitoredExecutor.getMaximumPoolSize()) > 0.95) {
				queueMessage("thread pool high watermark!", null);
			}
			
			if (!messageQueue.isEmpty()) {
				StringWriter summary = new StringWriter();
				PrintWriter summaryWriter = new PrintWriter(summary);
				
				StringWriter body = new StringWriter();
				PrintWriter bodyWriter = new PrintWriter(body);
				
				MessageFormat messageFormat = new MessageFormat("There {0}.");
				messageFormat.setFormatByArgumentIndex(0, new ChoiceFormat("1#is {1} alert|1.0<are {1} alerts"));
				
				Integer queueSize = new Integer(messageQueue.size());
				
				summaryWriter.println(messageFormat.format(new Object[]{queueSize, queueSize}));
				summaryWriter.println();
				summaryWriter.println();
				
				summaryWriter.println("Alert Summary");
				summaryWriter.println("________________________________________");
				summaryWriter.println();
				
				bodyWriter.println("Stack Traces");
				bodyWriter.println("________________________________________");
				bodyWriter.println();
				
				int stackTraceCount = 0;
				
				AlertMessage alert = null;
				while ((alert = messageQueue.poll()) != null) {
					summaryWriter.print(alert.getDate() + " " + alert.getMessage());
					if (alert.getError() != null) summaryWriter.print("; " + alert.getError().toString());
					summaryWriter.println();
					
					if (alert.getError() != null) {
						stackTraceCount++;
						bodyWriter.println(alert.getDate() + " " + alert.getMessage());
						alert.getError().printStackTrace(bodyWriter);
						bodyWriter.println();
					}
				}
				
				summaryWriter.println();
				summaryWriter.println();
				summaryWriter.flush();
				
				bodyWriter.flush();
				
				dispatchMessage(summary.toString() + (stackTraceCount > 0 ? body.toString() : ""));
			}
			
		} catch (Throwable t) {
			logger.error("error dispatching alert message", t);
		}
	}
	
	public static void main(String[] args) throws Exception {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(true));
		executor.submit(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES));
					} catch (InterruptedException e) {}
				}
			}
		});
		
		ExecutorAlertNotifier notifier = new ExecutorAlertNotifier(executor,
				new InternetAddress("wilken@asu.edu", "CAS EDNA Alert Notifier"),
				"CAS EDNA DB Lookup Alert " + InetAddress.getLocalHost().getHostName(),
				new InternetAddress[]{new InternetAddress("wilken@asu.edu")});
		
		while (true) {
			double random = Math.random();
			if (random < 0.25) {
				notifier.handleUnknownError("wilken", new RuntimeException());
			} else if (random < 0.5) {
				notifier.handleMissingPrincipal("wilken", null);
			} else if (random < 0.75) {
				notifier.handleRejectedExecution("wilken", new RejectedExecutionException());
			} else {
				notifier.handleTimeout("wilken", new TimeoutException());
			}
			
			Thread.sleep(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS));
		}
	}
	
	private static final class AlertMessage {
		private Date date;
		private String message;
		private Throwable error;
		
		public AlertMessage(final Date date, final String message, final Throwable error) {
			this.date = date;
			this.message = message;
			this.error = error;
		}
		
		public Date getDate() {
			return date;
		}
		
		public String getMessage() {
			return message;
		}
		
		public Throwable getError() {
			return error;
		}
	}
	
	/*
	private static class HTMLDataSource implements DataSource {
		private final String htmlString;
		
		public HTMLDataSource(String htmlString) {
			this.htmlString = htmlString;
		}
		
		@Override
		public String getContentType() {
			return "text/html";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(htmlString.getBytes());
		}

		@Override
		public String getName() {
			return "text/html DataSource for sending email";
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			throw new IOException("cannot write html");
		}
	}
	*/
}
