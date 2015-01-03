package controller;

import util.Config;
import controller.CloudController;
import cli.AdvancedShell;

/**
 * Object to organize all user-settings for one user,
 * started at new user-login
 */
/**
 * @author Robert Bekker (8325143)
 *
 */
/**
 * @author Robert Bekker (8325143)
 *
 */
/**
 * @author Robert Bekker (8325143)
 *
 */
public class ControllerUser {

	private CloudController controller;
	private AdvancedShell controllerShell;
	private boolean acceptCommands = true;
	private final String threadType;
	// private Message message = new Message();
	private int loggedIn = 0;
	private String userId = null;
	private String password = null;
	private byte[] challenge = null;
	private long credits = 0;
	private Config config;
	

	public ControllerUser(CloudController controller, String userId) {
		this.setController(controller);
		this.setUserId(userId);
		
		onLogin();
		setConfig(new Config("user"));
		setCredits(Integer.parseInt(getConfig().getString(userId + ".credits")));
		this.setControllerShell(controller.getShell());
		this.threadType = this.getClass().getName();
		// this.setName(getThreadType() + " " + this.getName());

		// register in shared list to be accessible from outside, even if this is not a really thread, just an object
		this.getController().getThreadsMap().put(this, getThreadType());
	}
	
	/**
	 * @param controller cloud controller object
	 * @param userId
	 * @param password
	 */
	public ControllerUser(CloudController controller, String userId, String password) {
		this(controller, userId);
		this.setPassword(password);
	}

	/**
	 * @return the config
	 */
	private synchronized Config getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	private synchronized void setConfig(Config config) {
		this.config = config;
	}

	/**
	 * @return the loggedIn
	 */
	public synchronized int getLoggedIn() {
		return loggedIn;
	}

	/**
	 * @param loggedIn the loggedIn to set
	 */
	private synchronized void setLoggedIn(int loggedIn) {
		this.loggedIn = loggedIn;
	}

	/**
	 * @return the userId
	 */
	public synchronized String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	private synchronized void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the password
	 */
	@SuppressWarnings("unused")
	private synchronized String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	private synchronized void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the credits
	 */
	public synchronized long getCredits() {
		return credits;
	}

	/**
	 * @param l the credits to set
	 */
	private synchronized void setCredits(long l) {
		if (l < 0) {
			throw new RuntimeException("!Error: Can't set credits to " + l);
		}
		this.credits = l;
	}

	/**
	 * @return the controllerShell
	 */
	@SuppressWarnings("unused")
	private synchronized AdvancedShell getControllerShell() {
		return controllerShell;
	}

	/**
	 * @param controllerShell the controllerShell to set
	 */
	private synchronized void setControllerShell(AdvancedShell controllerShell) {
		this.controllerShell = controllerShell;
	}

	/**
	 * @param controller the controller to set
	 */
	private synchronized void setController(CloudController controller) {
		this.controller = controller;
	}

	/**
	 * @param acceptCommands the acceptCommands to set
	 */
	private synchronized void setAcceptCommands(boolean acceptCommands) {
		this.acceptCommands = acceptCommands;
	}

	/**
	 * @param message the message to set
	 */
/*	@SuppressWarnings("unused")
	private synchronized void setMessage(Message message) {
		this.message = message;
	}
*/
	/**
	 * @return the controller
	 */
	private synchronized CloudController getController() {
		return controller;
	}

	/**
	 * @return the acceptCommands
	 */
	private synchronized boolean isAcceptCommands() {
		return acceptCommands;
	}

	/**
	 * @return the threadType
	 */
	private synchronized String getThreadType() {
		return threadType;
	}

	/**
	 * @param loggedIn the loggedIn to set
	 */
	public boolean isLoggedIn() {
		return (getLoggedIn() > 0);
	}

	/**
	 * @return
	 */
	public int onLogin() {
		this.setLoggedIn(getLoggedIn() + 1);
		return getLoggedIn();
	}

	/**
	 * @return
	 */
	public int onLogout() {
		this.setLoggedIn(getLoggedIn() - 1);
		return getLoggedIn();
	}
	
	/**
	 * @param operations
	 * @return
	 */
	public int calcNeededCredits(int operations) {
		return operations * 50;
	}

	/**
	 * @param operations
	 * @return
	 */
	public int availableOperations() {
		return (int) Math.floor(((double) getCredits()) / 50.0);
	}

	/**
	 * @param operations
	 * @return
	 */
	public boolean checkCredits(int operations) {
		return calcNeededCredits(operations) <= getCredits();
	}

	/**
	 * @param operations
	 */
	public void buyCredits(long credits2) {
		setCredits(getCredits() + credits2);
	}

	/**
	 * @param operations
	 * @return
	 */
	public boolean buyOperations(int operations) {
		try {
			setCredits(getCredits() - calcNeededCredits(operations));
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	/**
	 * @param operations
	 */
	public void refundOperations(int operations) {
		setCredits(getCredits() + calcNeededCredits(operations));
	}

	/**
	 * Atomic method to disable the use of commands
	 *  
	 * @return false if commands can't be disabled now because they were already disabled 
	 */
	synchronized private boolean disableCommands() {
		if (isAcceptCommands()) {
			setAcceptCommands(false);
			return true;
		};
		return false;
	}

	/**
	 * Closes all resources
	 */
	public void close() {
		// disable race conditions
		if (disableCommands()) {
			// only entered by the first close() 
	
			// finally remove from list of active threads
			this.getController().getThreadsMap().remove(this);
			
			// TODO more cleanup?
			
			// Don't interrupt because it's an object, no thread
		}
	}
}