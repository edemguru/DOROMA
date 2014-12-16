package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.ConnectException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import util.Config;
import util.Message;
import cli.Command;
import cli.AdvancedShell;

public class Client implements IClientCli, Runnable {

	private String clientName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private PrintStream userErrorStream;
	private AdvancedShell shell;
	private String loggedInUser = null;
	private Socket controllerSocket = null;
	private boolean acceptCommands = true;
	private Message message = new Message();
	private Thread shellThread;

	/**
	 * @param clientName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 * @param userErrorStream
	 *            the error output stream to write the console error output to
	 */
	public Client(String clientName, Config config, InputStream userRequestStream, PrintStream userResponseStream, PrintStream userErrorStream) {
		setClientName(clientName);
		setConfig(config);
		setUserRequestStream(userRequestStream);
		setUserResponseStream(userResponseStream);
		setUserErrorStream(userErrorStream);

		/*
		 * create a new Shell instance and provide the name of the
		 * component, an InputStream as well as an OutputStream. If you want to
		 * test the application manually, simply use System.in and System.out.
		 */
		setShell(new AdvancedShell(clientName, userRequestStream, userResponseStream, userErrorStream));
		
	}

	/**
	 * @return the clientName
	 */
	@SuppressWarnings("unused")
	private synchronized String getClientName() {
		return clientName;
	}

	/**
	 * @param clientName the clientName to set
	 */
	private synchronized void setClientName(String clientName) {
		this.clientName = clientName;
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
	 * @return the userRequestStream
	 */
	@SuppressWarnings("unused")
	private synchronized InputStream getUserRequestStream() {
		return userRequestStream;
	}

	/**
	 * @param userRequestStream the userRequestStream to set
	 */
	private synchronized void setUserRequestStream(InputStream userRequestStream) {
		this.userRequestStream = userRequestStream;
	}

	/**
	 * @return the userResponseStream
	 */
	@SuppressWarnings("unused")
	private synchronized PrintStream getUserResponseStream() {
		return userResponseStream;
	}

	/**
	 * @param userResponseStream the userResponseStream to set
	 */
	private synchronized void setUserResponseStream(PrintStream userResponseStream) {
		this.userResponseStream = userResponseStream;
	}

	/**
	 * @return the userErrorStream
	 */
	@SuppressWarnings("unused")
	private synchronized PrintStream getUserErrorStream() {
		return userErrorStream;
	}

	/**
	 * @param userErrorStream the userErrorStream to set
	 */
	private synchronized void setUserErrorStream(PrintStream userErrorStream) {
		this.userErrorStream = userErrorStream;
	}

	/**
	 * @return the shell
	 */
	private synchronized AdvancedShell getShell() {
		return shell;
	}

	/**
	 * @param shell the shell to set
	 */
	private synchronized void setShell(AdvancedShell shell) {
		this.shell = shell;
	}

	/**
	 * @return the loggedInUser
	 */
	private synchronized String getLoggedInUser() {
		return loggedInUser;
	}

	/**
	 * @param loggedInUser the loggedInUser to set
	 */
	private synchronized void setLoggedInUser(String loggedInUser) {
		this.loggedInUser = loggedInUser;
	}

	/**
	 * @return the controllerSocket
	 */
	private synchronized Socket getControllerSocket() {
		return controllerSocket;
	}

	/**
	 * @param controllerSocket the socket to set
	 */
	private synchronized void setControllerSocket(Socket controllerSocket) {
		this.controllerSocket = controllerSocket;
	}

	/**
	 * @return the acceptCommands
	 */
	private synchronized boolean isAcceptCommands() {
		return acceptCommands;
	}

	/**
	 * @param acceptCommands the acceptCommands to set
	 */
	private synchronized void setAcceptCommands(boolean acceptCommands) {
		this.acceptCommands = acceptCommands;
	}

	/**
	 * @return the message
	 */
	private synchronized Message getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	@SuppressWarnings("unused")
	private synchronized void setMessage(Message message) {
		this.message = message;
	}

	/**
	 * @return the shellThread
	 */
	private synchronized Thread getShellThread() {
		return shellThread;
	}

	/**
	 * @param shellThread the shellThread to set
	 */
	private synchronized void setShellThread(Thread shellThread) {
		this.shellThread = shellThread;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in, System.out, System.err);
		// Start the instance in a new thread
		new Thread((Runnable) client).start();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		/*
		 * register all commands the Shell should support. In this example
		 * this class implements all desired commands.
		 */
		getShell().register(this);

		/*
		 * make the Shell process the commands read from the
		 * InputStream by invoking Shell.run(). Note that Shell implements the
		 * Runnable interface. Thus, you can run the Shell asynchronously by
		 * starting a new Thread:
		 * 
		 * Thread shellThread = new Thread(shell); shellThread.start();
		 * 
		 * In that case, do not forget to terminate the Thread ordinarily.
		 * Otherwise, the program will not exit.
		 */
		setShellThread(new Thread(getShell())); 
		getShellThread().start();
		
		getShell().printLine("'" + getClass().getName() + "' is up! Enter command.");

	}
	
	/**
	 * check if user is logged in
	 * 
	 * @return true if the user is logged in
	 */
	public Boolean loggedIn() {
		return (getLoggedInUser() != null);
	}
	
	/**
	 * Wrapper for 'response(String request, boolean loggedIn)'
	 * Defaults to loggedIn = true
	 * 
	 * @param request
	 * @return response
	 */
	private String response(String request) {
		return response(request, true);
	}
	/**
	 * Sends request to controller and waits for response
	 * 
	 * @param request
	 * @param loggedIn
	 * @return response
	 */
	private String response(String request, boolean loggedIn) {
		String inString = "";

		if (isAcceptCommands()) {
			if (loggedIn && ! loggedIn()) {
				getShell().printLine("You have to login first!");
				return null;
			}
			if (! loggedIn && loggedIn()) {
				getShell().printLine("Already logged in!");
				return null;
			}

			try {
				// create a writer to send messages to the server
				PrintWriter serverWriter = new PrintWriter(getControllerSocket().getOutputStream(), true);
				if (serverWriter.checkError()) {
					getShell().printErrLine(getMessage().controller_disconnected);
					closeTCP();
					return null;
				}
				// write provided user input to the socket
				serverWriter.println(request);
				
				// create a reader to retrieve messages send by the server
				BufferedReader serverReader = new BufferedReader(new InputStreamReader(getControllerSocket().getInputStream()));
				// read server response
				// inString = serverReader.readLine();
				// not only read 1 line, read all
				inString = null;
				Character myChar = null;
				if ((myChar = (char) serverReader.read()) != -1) {
					inString = myChar.toString();
					while (serverReader.ready()) {
						if ((myChar = (char) serverReader.read()) == -1) {
							// ready (data available) but unexpected end of input found 
							getShell().printErrLine("!Error: Read-Problems!");
							closeTCP();
							try {
								return (String) getShell().invoke(request);
							} catch (Throwable e) {
								return null;
							}
						}
						inString += myChar.toString();
					}
				}
				if (inString == null && request.trim().equals("!exit")) {
					// disconnected, wie von "!exit" geplant
					inString = getMessage().controller_disconnected;
				} else if (inString == null || inString.contains(getMessage().controller_disconnected)) {
					// sonst immer unerwartet disconnected
					getShell().printErrLine(getMessage().controller_disconnected);
					closeTCP();
					try {
						return (String) getShell().invoke(request);
					} catch (Throwable e) {
						return null;
					}
				}
				// strip all trailing \n or \r
				while (inString.endsWith("\n") || inString.endsWith("\r")) {
					inString = inString.substring(0, inString.length() - 1);
				}
			} catch (SocketException e) {
				getShell().printErrLine(getMessage().controller_disconnected);
				closeTCP();
				try {
					return (String) getShell().invoke(request);
				} catch (Throwable e1) {
					return null;
				}
			} catch (IOException e) {
				getShell().printErrLine("!Error: Unknown communication problem!");
				closeTCP();
				try {
					return (String) getShell().invoke(request);
				} catch (Throwable e1) {
					return null;
				}
			}
			// return the server response which will be written on console
			return inString;
		} else {
			getShell().printErrLine(getMessage().controller_disconnected);
			// closeTCP();
			return null;
		}
	}
	
	/**
	 * Logs user out and closes all resources, like sockets, shell, ... 
	 */
	synchronized public void close() {
		// disable further commands
		if (isAcceptCommands()) {
			setAcceptCommands(false);
			// log out
			setLoggedInUser(null);

			closeTCP();
			
			// Afterwards stop the Shell from listening for commands
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
	
			// TODO more clean up?

			// finally set the Interrupted-Flag to stop the shell
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes TCP socket
	 */
	public void closeTCP() {
		if (getControllerSocket() != null && ! getControllerSocket().isClosed()) {
			try {
				getControllerSocket().close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
			// getShell().printLine("Socket closed.");
		} else {
			// getShell().printErrLine("Socket closed.");
		}
		setControllerSocket(null);
		setLoggedInUser(null);
	}

// ----------------------------------------------------- Local commands -----------------------------------------	
	
	/* (non-Javadoc)
	 * @see client.IClientCli#login(java.lang.String, java.lang.String)
	 */
	@Override
	@Command
	public String login(String username, String password) throws IOException {

		/*
		 * create a new tcp socket at specified host and port - make sure
		 * you specify them correctly in the client properties file(see
		 * client.properties)
		 */
		String port = "";
		int portNr = 0;
		try {
			// get port term and calculate expression 
			port = getConfig().getString("controller.tcp.port");
		    ScriptEngineManager mgr = new ScriptEngineManager();
		    ScriptEngine engine = mgr.getEngineByName("JavaScript");
		    portNr = ((Double) engine.eval(port)).intValue();

		    setControllerSocket(new Socket(getConfig().getString("controller.host"), portNr));
		} catch (ScriptException e) {
			return "!Error: Invalid port expression '" + port + "'.";
			// return "!Error: Invalid port expression '" + port + "'.\n" + e.getMessage() + "\n" +e.getStackTrace().toString();
		} catch (UnknownHostException | ConnectException e) {
			closeTCP();
			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.";
//			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.\n" + e.getMessage() + "\n" +e.getStackTrace().toString();
		} catch (SocketException e) {
			closeTCP();
			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.";
//			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.\n" + e.getMessage() + "\n" +e.getStackTrace().toString();
		} catch (IOException e) {
			close();
			throw new RuntimeException(e.getClass().getSimpleName(), e);
		}
		
		String response = response("!login " + username + " " + password, false);
		
		if (response != null && response.equals(getMessage().login_success)) {
			this.setLoggedInUser(username);
			return response;
		}
		// unsuccessful
		closeTCP();
		return response;
		
	}

	/* (non-Javadoc)
	 * @see client.IClientCli#logout()
	 */
	@Override
	@Command
	public String logout() throws IOException {

		// just inform the Controller
		String response  = response("!logout");
		closeTCP();

		// release all User specific state information
		setLoggedInUser(null);

		if (response == null && isAcceptCommands()) {
			return "Already logged out!";
		}
		return response;
	}

	/* (non-Javadoc)
	 * @see client.IClientCli#exit()
	 */
	@Override
	@Command
	public String exit() throws IOException {
		
		String response = "";
		
		// First try to logout in case a user is still logged in
		if (loggedIn()) {
			response = logout();
			if (response != null && response.trim().length() > 0 && ! response.trim().equals("\n")) {
				getShell().printLine(response);
			}
		}

		// also try to terminate Controller's ClientThread
		if (getControllerSocket() != null && ! getControllerSocket().isClosed()) {
			response = response("!exit", false);
		}

		// close all Sockets, Threads, Timer, ...
		close();

		if (! response.equals(getMessage().shutdown)) {
			return getMessage().shutdown;
		}
		return null;
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	/* (non-Javadoc)
	 * @see client.IClientCli#authenticate(java.lang.String)
	 */
	@Override
	@Command
	public String authenticate(String username) throws IOException {
		
		// TODO Implement code.
		
		return "// TODO Implement code.";
		
	}

	// --- additional Commands not requested in assignment

	// TODO Remove additional commands

	/* (non-Javadoc)
	 * @see client.IClientCli#help()
	 */
	@Override
	@Command
	public String help() throws IOException {

		// ensure the user is logged in
		if (! loggedIn()) {
			// return local commands only
			String usage = "\navailable Commands:\n";
			usage += "\t!login <userid> <password>\n";
			usage += "\t!exit\n";
			usage += "\t!help\n";

			return usage;
		}

		// get remote client help information
		return response("!help");
	}

	// Try command on remote site, not executable by the client shell 
	/* (non-Javadoc)
	 * @see client.IClientCli#remoteCommand(java.lang.String)
	 */
	@Override
	public String remoteCommand(String command) throws IOException {

		// only if logged in pass command to controller 
		if (loggedIn() && isAcceptCommands()) {
			return response(command);
		}
		if (isAcceptCommands()) {
			// Command not found
			String result = getMessage().unknown_command;
			try {
				result += "\n" + (String) getShell().invoke("!help");
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return result;
		}
		return null;

	}
}
