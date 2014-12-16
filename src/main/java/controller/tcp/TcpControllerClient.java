package controller.tcp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import controller.CloudController;
import controller.ControllerNode;
import controller.ControllerUser;
import cli.Command;
import cli.AdvancedShell;
import util.Config;
import util.Message;

/**
 * Thread to communicate with a client on the given socket.
 */
public class TcpControllerClient extends Thread implements ITcpControllerClientCli, Runnable {

	private Socket clientSocket;
	private BufferedReader reader;
	private PrintWriter writer;
	private CloudController controller;
	private AdvancedShell controllerShell;
	private AdvancedShell shell;
	private boolean acceptCommands = true;
	private final String threadType;
	private Message message = new Message();
	private ControllerUser user = null; 

	/**
	 * @param controller Cloud controller object
	 * @param clientSocket TCP socket to client
	 */
	public TcpControllerClient(CloudController controller, Socket clientSocket) {
		this.setClientSocket(clientSocket);
		this.setController(controller);
		
		this.setControllerShell(controller.getShell());
		this.threadType = this.getClass().getName();
		this.setName(getThreadType() + " " + this.getName());
		
		/*
		 * create a new Shell instance, not used to process interactive commands,
		 * but to invoke commands send from client.
		 */
		setShell(new AdvancedShell(getThreadType(), null, null, null));

		// register in shared list to be accessible from outside
		this.getController().getThreadsMap().put(this, getThreadType());
	}

	/**
	 * @return the user
	 */
	private synchronized ControllerUser getUser() {
		return user;
	}

	/**
	 * @param user
	 */
	private synchronized void setUser(ControllerUser user) {
		this.user = user;
	}

	/**
	 * @param clientSocket the socket to set
	 */
	private synchronized void setClientSocket(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	/**
	 * @param reader the reader to set
	 */
	private synchronized void setReader(BufferedReader reader) {
		this.reader = reader;
	}

	/**
	 * @param writer the writer to set
	 */
	private synchronized void setWriter(PrintWriter writer) {
		this.writer = writer;
	}

	/**
	 * @param controller the controller to set
	 */
	private synchronized void setController(CloudController controller) {
		this.controller = controller;
	}

	/**
	 * @param controllerShell the controllerShell to set
	 */
	private synchronized void setControllerShell(AdvancedShell controllerShell) {
		this.controllerShell = controllerShell;
	}

	/**
	 * @param shell the shell to set
	 */
	private synchronized void setShell(AdvancedShell shell) {
		this.shell = shell;
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
	@SuppressWarnings("unused")
	private synchronized void setMessage(Message message) {
		this.message = message;
	}

	/**
	 * @return the clientSocket
	 */
	private synchronized Socket getClientSocket() {
		return clientSocket;
	}

	/**
	 * @return the reader
	 */
	private synchronized BufferedReader getReader() {
		return reader;
	}

	/**
	 * @return the writer
	 */
	private synchronized PrintWriter getWriter() {
		return writer;
	}

	/**
	 * @return the controller
	 */
	private synchronized CloudController getController() {
		return controller;
	}

	/**
	 * @return the controllerShell
	 */
	private synchronized AdvancedShell getControllerShell() {
		return controllerShell;
	}

	/**
	 * @return the shell
	 */
	private synchronized AdvancedShell getShell() {
		return shell;
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
	 * @return the message
	 */
	private synchronized Message getMessage() {
		return message;
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {

		/*
		 * register all commands the Shell should support, 
		 * but never start this shell in an own thread
		 */
		getShell().register(this);
		
		try {
			
			// prepare the input reader for the socket
			setReader(new BufferedReader(new InputStreamReader(getClientSocket().getInputStream())));
			// prepare the writer for responding to clients requests
			setWriter(new PrintWriter(getClientSocket().getOutputStream(), true));

			String request;
			Object response;
			// read client requests
			while ((request = getReader().readLine()) != null && isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
				if (loggedIn()) {
					getControllerShell().printLine(String.format("User '%s' at '%s'sent the following request: %s", getUser().getUserId(), getClientSocket().getInetAddress(), request));
				} else {
					getControllerShell().printLine("Unknown user sent the following request: " + request);
				}
				
				if (isAcceptCommands()) {
					// invoke available commands
					try {
						response = getShell().invoke(request);
					} catch (IllegalArgumentException e) {
						response = getMessage().unknown_command;
						// if (loggedIn()) {
							try {
								response = response + "\n" + getShell().invoke("!help");
							} catch (Throwable e1) {
								// Do nothing
							}
						// }
					} catch (Throwable throwable) {
						ByteArrayOutputStream str = new ByteArrayOutputStream(1024);
						throwable.printStackTrace(new PrintStream(str, true));
						response = str.toString();
					}
				} else {
					response = getMessage().controller_disconnected; 
				}

				// return response
				if (response != null) {
					if (loggedIn()) {
						getControllerShell().printLine("Response sent to the user '" + getUser().getUserId() + "': " + response);
					} else {
						getControllerShell().printLine("Response sent to the unknown user: " + response);
					}
					getWriter().println(response);
				} else {
					if (loggedIn()) {
						getControllerShell().printLine("No response sent to the user '" + getUser().getUserId() + "'.");
					} else {
						getControllerShell().printLine("No response sent to the unknown user.");
					}
				}
			}
			// reader not available any more, close
		} catch (SocketException e) {
			// ignore socket-error on close
		} catch (InterruptedIOException e) {
			// on InterruptedIOException in readLine just continue closing thread
		} catch (ClosedByInterruptException e) {
			// on ClosedByInterruptException in readLine just continue closing thread
		} catch (IOException e) {
			getControllerShell().printErrLine("Error occurred while communicating with client: " + e.getMessage());
		} finally {
			close();
			getControllerShell().printLine("Client-Thread shut down completed.");
		}
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
	 * By default not initiated by the client
	 */
	public void close() {
		close(false);
	}
	synchronized public void close(boolean byClient) {
		// disable race conditions
		if (disableCommands()) {
			// only entered by the first close() 
			if (! byClient) {
				// Send information to client
				getWriter().println(getMessage().controller_disconnected);
				// but dont wait for any response
			}
			
			// log user out
			if (loggedIn()) {
				getUser().onLogout();
				setUser(null);
			}
	
			// close socket
			closeTCP();
	
			// Afterwards stop the Shell
			if (getShell() != null) {
				boolean wasInterrupted = Thread.currentThread().isInterrupted();
				getShell().close();
				if (wasInterrupted) {
					// set Interrupted-Flag again
					Thread.currentThread().interrupt();
				} else {
					// clear Interrupted-Flag
					Thread.interrupted();
				}
			}
			
			// and then remove from list of active threads
			this.getController().getThreadsMap().remove(this);
			
			// TODO more cleanup?

			// finally set the Interrupted-Flag
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes the TCP socket to the client
	 */
	synchronized public void closeTCP() {
		if (getClientSocket() != null) {
			try {
				getClientSocket().close();
			} catch (IOException e) {
				throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
			}
		}

	}

	/**
	 * @return true if the user is logged in
	 */
	public Boolean loggedIn() {
		return (getUser() != null);
	}

	/**
	 * Check user login credentials
	 * 
	 * @param username
	 * @param givenPassword
	 * @return true if userid and password are correct
	 */
	private boolean checkUser(String username, String givenPassword) {
		Config config = new Config("user");
		String password = null;
		try {
			password = config.getString(username + ".password");
		} catch (MissingResourceException e) {
			// probably user not found
		}
		
		
		if (password == null) {
			return false;
		}
		if (password.equals(givenPassword)) {
			return true;
		}
		return false;
	}
	
	/**
	 * @param string
	 * @return true if string could be converted to a double (after removing '+' signs)
	 */
	static private boolean isNumeric(String string) {
		try {
			Double.parseDouble(string.trim().replaceAll("\\+", "").replace(",", ".").trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * @param string
	 * @return true if string could be converted to an integer (after removing '+' signs)
	 */
	static private boolean isInteger(String string) {
		try {
			Integer.parseInt(string.trim().replaceAll("\\+", "").replace(",", ".").trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Splits a string in numeric and non-numeric parts
	 * 
	 * @param inString
	 * @return Array of alternating numeric and non-numeric strings
	 */
	static private ArrayList<String> splitOnNumber(String inString) {
		String pattern = "[-\\+]?\\d+([,\\.]\\d+)?([eE][-\\+]?\\d+)?";		// numbers without leading decimal point, no grouping separator and '.' or ',' as decimal separator
		int lastEnd = 0;
		ArrayList<String> results = new ArrayList<String>();
		for (Matcher matched = Pattern.compile(pattern).matcher(inString); matched.find(); ) {
			if (matched.start() > lastEnd) {
				results.add(inString.substring(lastEnd, matched.start()));
			}
			results.add(inString.substring(matched.start(), matched.end()));
			lastEnd = matched.end();
		}
		if (lastEnd < inString.length()) {
			results.add(inString.substring(lastEnd));
		}
		return results;
	}
	
//----------------------------------------------------- Remote available commands -----------------------------------------	

	/* (non-Javadoc)
	 * @see controller.tcp.ITcpControllerClientCli#login(java.lang.String, java.lang.String)
	 */
	@Override
	@Command
	public String login(String username, String password) throws IOException {
		if (loggedIn()) {
			return "Error: You are already logged in!";
		}
		if (checkUser(username, password)) {
			setUser(controller.getUser(username));
			if (getUser() != null) {
				getUser().onLogin();
				getControllerShell().printLine(String.format("Existing User %s is logged in %d times.", username, getUser().getLoggedIn()));
				//return null;
			} else {
				setUser(new ControllerUser(getController(), username, password));
				getControllerShell().printLine(String.format("New User %s is logged in %d time.", username, getUser().getLoggedIn()));
			}
			return getMessage().login_success;
		}
		return "Error: Wrong username/password combination!";
	}

	/* (non-Javadoc)
	 * @see controller.tcp.ITcpControllerClientCli#logout()
	 */
	@Override
	@Command
	public String logout() throws IOException {
		if (getUser().getUserId() == null) {
			return "Error: Already logged out!";
		}
		getUser().onLogout();
		setUser(null);
		return "Successfully logged out!";
	}

	/* (non-Javadoc)
	 * @see controller.tcp.ITcpControllerClientCli#exit()
	 */
	@Override
	@Command
	public String exit() throws IOException {
		if (loggedIn()) {
			logout();
		}
		close(true);
		getControllerShell().printLine("Client disconnected!");
		return null;
	}

	/* (non-Javadoc)
	 * @see controller.tcp.ITcpControllerClientCli#credits()
	 */
	@Override
	@Command
	public String credits() throws IOException {
		return String.format("You have %d credits left.", getUser().getCredits());
	}

	/* (non-Javadoc)
	 * @see controller.tcp.ITcpControllerClientCli#buy(long)
	 */
	@Override
	@Command
	public String buy(long credits) throws IOException {
		if (credits <= 0L) {
			return "!Error: Amount must be positive!";
		}
		getUser().buyCredits(credits);
		return String.format("After buying %d credits you have now %d left.", credits, getUser().getCredits());
	}

	/* (non-Javadoc)
	 * @see controller.tcp.ITcpControllerClientCli#list()
	 */
	@Override
	@Command
	public String list() throws IOException {
		return getController().listOperations();
	}

	/* (non-Javadoc)
	 * @see controller.tcp.ITcpControllerClientCli#compute(java.lang.String)
	 */
	@Override
	@Command
	public String compute(String term) throws IOException {
		
		// split command line
		// check for syntactical correctness of command line
		// check all connected nodes which will support the requested operations
		// check that all operations are supported
		// for each operation contact poorest node (open/request/close via TCP)
		// increase nodes credits
		// return result or error
		// decrease client credits if no error
		
		// split command line on numbers
		getControllerShell().printLine(term);
		ArrayList<String> parts = splitOnNumber(term.trim());

		// only single char binary inner operators are supported, like '+' or '*'
		// and signs for numbers
		// but no unary operators, like 'sin'
		// or multinary operators, like ternary operators
		// and no multichar operators, like 'mod'
		String result = null;
		int operatorCount = 0;
		try {
			// First check prerequisites
			// need odd number of parts
			if ((parts.size() % 2) == 0) {
				throw new IllegalArgumentException(String.format("Invalid number of operators/operands in '%s'!", term));
			}
			// the very first part has to be a number
			if (! isNumeric(parts.get(0))) {
				throw new IllegalArgumentException(String.format("'%s' is not a number!", parts.get(0)));
			}
			if (! isInteger(parts.get(0))) {
				throw new IllegalArgumentException(String.format("'%s' is not an integer!", parts.get(0)));
			}
			String operator;
			for (int index = 1; index < parts.size(); index++) {
				// the next part has to be an operator
				if (parts.get(index).trim().length() == 0) {
					throw new IllegalArgumentException(String.format("Missing operator!", parts.get(index)));
				}
				if (isNumeric(parts.get(index))) {
					throw new IllegalArgumentException(String.format("'%s' is not an operator!", parts.get(index)));
				}
				if (Pattern.matches(".*\\s+.*", parts.get(index).trim())) {
					throw new IllegalArgumentException(String.format("'%s' is not only one operator!", parts.get(index)));
				}
				operatorCount++;
				operator = parts.get(index++).trim();
				// the following part has to be a number again (should be OK because of modulo-test at the beginning)
				if (index >= parts.size()) {
					throw new IllegalArgumentException("Missing operand!");
				}
				if (! isNumeric(parts.get(index))) {
					throw new IllegalArgumentException(String.format("'%s' is not a number!", parts.get(index)));
				}
				if (! isInteger(parts.get(index))) {
					throw new IllegalArgumentException(String.format("'%s' is not an integer!", parts.get(index)));
				}
				
				// now find the best node
				ControllerNode foundNode = null;
				for (ControllerNode checkNode : getController().getNodesMap(true, operator.charAt(0)).keySet()) {
					if (foundNode == null) {
						foundNode = checkNode;
					} else if (foundNode.getUsage() > checkNode.getUsage()) {
						foundNode = checkNode;
					}
				}
				if (foundNode == null) {
					return String.format("!Warning: No node found for '%s' operation.", operator);
				}
			}
			// check credits
			if (! getUser().checkCredits(operatorCount)) {
				return String.format("!Warning: %d credits are not enough for %d operation%s (but for %d).", getUser().getCredits(), operatorCount, (operatorCount == 1 ? "" : "s"), getUser().availableOperations());
			}
			
			// now start the calculation
			result = parts.get(0).trim().replaceAll("\\+", "");
			String operand;
			String request;
			operatorCount = 0;
			for (int index = 1; index < parts.size(); index++) {
				operator = parts.get(index++).trim();
				operand = parts.get(index).trim().replaceAll("\\+", "");
				request = String.format("!compute %s %s %s", result, operator, operand);
				operatorCount++;
				// pay in advance
				if (! getUser().buyOperations(1)) {
					// no refund because user simultaneously sends operations
					return String.format("!Warning: %d credits are not enough for operation '%s %s %s'.", getUser().getCredits(), result, operator, operand);
				}
				
				// Test only?
				getControllerShell().printLine(request);
				
				// now find the best node again
				ControllerNode foundNode = null;
				for (ControllerNode checkNode : getController().getNodesMap(true, operator.charAt(0)).keySet()) {
					if (foundNode == null) {
						foundNode = checkNode;
					} else if (foundNode.getUsage() > checkNode.getUsage()) {
						foundNode = checkNode;
					}
				}
				if (foundNode == null) {
					// refund all operations done until now because an unavailable node is no failure of the user 
					getUser().refundOperations(operatorCount);
					return String.format("!Warning: No active node found for '%s' operation", operator);
				}
				result = foundNode.request(request, this);
				if (isInteger(result)) {
					foundNode.increaseUsage(result);
				} else {
					// check for the type of failure and decide who is responsible, the user or the node
					// TODO: maybe increase list of user-error
					if (getMessage().zero_division.equals(result)) {			
						// increase node usage 
						// because a user-Error is no failure of the node 
						// foundNode.increaseUsage(result);			// dont increase! (lt. Angabe!)
					} else {
						// by default refund all operations done until now 
						// because an unspecific node-Error is no failure of the user 
						getUser().refundOperations(operatorCount);
					}
					return result;
				}
			}
		} catch (IllegalArgumentException e) {
			// invalid argument in compute construct
			getControllerShell().printErrLine(e.getMessage());
			// throw new IllegalArgumentException(e);
			return e.getMessage();
		}
		return result;
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	/* (non-Javadoc)
	 * @see controller.tcp.ITcpControllerClientCli#authenticate(java.lang.String)
	 */
	@Override
	@Command
	public String authenticate(String username) throws IOException {
		
		getControllerShell().printLine("\n// TODO Implement code.\n\n");
		
		return "// TODO Implement code.";
	}

	// --- additional Commands not requested in assignment

	// TODO Remove additional command

	/**
	 * @return user name
	 */
	@Command
	public String whoami() {
		if (! loggedIn()) {
			return "!Error: You have to login first!";
		}
		return this.getUser().getUserId();
	}

	/* (non-Javadoc)
	 * @see controller.tcp.ITcpControllerClientCli#help()
	 */
	@Override
	@Command
	public String help() throws IOException {

		String usage = "\navailable Commands:\n";
		usage += "\t!whoami\n";			// TODO remove
		usage += "\t!logout\n";
		usage += "\t!credits\n";
		usage += "\t!buy <credits>\n";
		usage += "\t!list\n";
		usage += "\t!compute <math-term>\n";
		usage += "\t!exit\n";
		usage += "\t!help\n";

		return usage;
	}

}
