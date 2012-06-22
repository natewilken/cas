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
	
	protected final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<String>();
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
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		
		out.println("rejected execution for [" + principal + "]; active thread count: "
				+ monitoredExecutor.getActiveCount() + "/" + monitoredExecutor.getMaximumPoolSize()
				+ ", completed task count: " + monitoredExecutor.getCompletedTaskCount());
		
		e.printStackTrace(out);
		out.flush();
		
		queueMessage(writer.toString());
	}
	
	public void handleTimeout(String principal, TimeoutException e) {
		queueMessage("execution timeout for [" + principal + "]; active thread count: "
				+ monitoredExecutor.getActiveCount() + "/" + monitoredExecutor.getMaximumPoolSize()
				+ ", completed task count: " + monitoredExecutor.getCompletedTaskCount() + "\n");
	}
	
	public void handleMissingPrincipal(String principal, NoResultException e) {
		queueMessage("no account status result found for [" + principal + "]\n");
	}
	
	public void handleUnknownError(String principal, Throwable t) {
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		
		out.println("unknown error for [" + principal + "]; active thread count: "
				+ monitoredExecutor.getActiveCount() + "/" + monitoredExecutor.getMaximumPoolSize()
				+ ", completed task count: " + monitoredExecutor.getCompletedTaskCount());
		
		t.printStackTrace(out);
		out.flush();
		
		queueMessage(writer.toString());
	}
	
	protected void queueMessage(String message) {
		messageQueue.offer(new Date() + " " + message);
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
				queueMessage("thread pool high watermark! active thread count: "
					+ monitoredExecutor.getActiveCount() + "/" + monitoredExecutor.getMaximumPoolSize()
					+ ", completed task count: " + monitoredExecutor.getCompletedTaskCount());
			}
			
			if (!messageQueue.isEmpty()) {
				StringWriter writer = new StringWriter();
				PrintWriter out = new PrintWriter(writer);
				
				MessageFormat messageFormat = new MessageFormat("There {0}:");
				messageFormat.setFormatByArgumentIndex(0, new ChoiceFormat("1#is {1} alert|1.0<are {1} alerts"));
				
				Integer queueSize = new Integer(messageQueue.size());
				
				out.println(messageFormat.format(new Object[]{queueSize, queueSize}));
				out.println();
				
				String message = null;
				while ((message = messageQueue.poll()) != null) {
					out.println(message);
				}
				out.flush();
				
				dispatchMessage(writer.toString());
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
	
}
