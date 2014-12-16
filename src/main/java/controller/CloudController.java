package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import controller.tcp.TcpControllerClient;
import controller.tcp.TcpControllerListener;
import controller.tcp.TcpControllerNode;
import controller.udp.UdpControllerListener;
import util.Config;
import util.Message;
import cli.Command;
import cli.AdvancedShell;

/**
 * @author Robert Bekker (8325143)
 *
 */
public class CloudController implements ICloudControllerCli, Runnable {

	private String controllerName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private PrintStream userErrorStream;
	private AdvancedShell shell;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private Map<Object, String> threadsMap = new ConcurrentHashMap<>();
	private Thread shellThread;
	private boolean acceptCommands = true;
	private Message message = new Message();

	/**
	 * @param controllerName
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
	public CloudController(String controllerName, Config config,	InputStream userRequestStream, PrintStream userResponseStream, PrintStream userErrorStream) {
		this.setControllerName(controllerName);
		this.setConfig(config);
		this.setUserRequestStream(userRequestStream);
		this.setUserResponseStream(userResponseStream);
		this.setUserErrorStream(userErrorStream);
		/*
		 * create a new Shell instance and provide the name of the
		 * component, an InputStream as well as an OutputStream. If you want to
		 * test the application manually, simply use System.in and System.out.
		 */
		this.setShell(new AdvancedShell(controllerName, userRequestStream, userResponseStream, userErrorStream));
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
	 * @return the controllerName
	 */
	@SuppressWarnings("unused")
	private synchronized String getControllerName() {
		return controllerName;
	}

	/**
	 * @return the config
	 */
	public synchronized Config getConfig() {
		return config;
	}

	/**
	 * @return the userRequestStream
	 */
	@SuppressWarnings("unused")
	private synchronized InputStream getUserRequestStream() {
		return userRequestStream;
	}

	/**
	 * @return the userResponseStream
	 */
	@SuppressWarnings("unused")
	private synchronized PrintStream getUserResponseStream() {
		return userResponseStream;
	}

	/**
	 * @return the userErrorStream
	 */
	@SuppressWarnings("unused")
	private synchronized PrintStream getUserErrorStream() {
		return userErrorStream;
	}

	/**
	 * @return the shell
	 */
	public synchronized AdvancedShell getShell() {
		return shell;
	}

	/**
	 * @return the serverSocket
	 */
	private synchronized ServerSocket getServerSocket() {
		return serverSocket;
	}

	/**
	 * @return the datagramSocket
	 */
	private synchronized DatagramSocket getDatagramSocket() {
		return datagramSocket;
	}

	/**
	 * @return the threadsMap
	 */
	public synchronized Map<Object, String> getThreadsMap() {
		return threadsMap;
	}

	/**
	 * @param threadClass
	 * @return
	 */
	public ConcurrentHashMap<Object, String> getThreadsMap(Class<?> threadClass) {
		ConcurrentHashMap<Object, String> threadsMap = new ConcurrentHashMap<>();
		String type;
		String checkClass = threadClass.getName();
		for (Object thread : getThreadsMap().keySet()) {
			type = getThreadsMap().get(thread);
			if (type != null && type.equals(checkClass)) {
				threadsMap.put((Class<?>) thread, type);
			}
		}
		return threadsMap;
	}
 
	/**
	 * @return
	 */
	public ConcurrentHashMap<ControllerNode, String> getNodesMap() {
		ConcurrentHashMap<ControllerNode, String> threadsMap = new ConcurrentHashMap<>();
		String type;
		String checkClass = ControllerNode.class.getName();
		for (Object thread : getThreadsMap().keySet()) {
			type = getThreadsMap().get(thread);
			if (type != null && type.equals(checkClass)) {
				threadsMap.put((ControllerNode) thread, type);
			}
		}
		return threadsMap;
	}
 
	/**
	 * @param active
	 * @return
	 */
	public ConcurrentHashMap<ControllerNode, String> getNodesMap(boolean active) {
		ConcurrentHashMap<ControllerNode, String> threadsMap = new ConcurrentHashMap<>();
		String type;
		String checkClass = ControllerNode.class.getName();
		for (Object thread : getThreadsMap().keySet()) {
			type = getThreadsMap().get(thread);
			if (type != null && type.equals(checkClass) && ((ControllerNode) thread).isActive() == active) {
				threadsMap.put((ControllerNode) thread, type);
			}
		}
		return threadsMap;
	}
 
	/**
	 * @param active
	 * @param operations
	 * @return
	 */
	public ConcurrentHashMap<ControllerNode, String> getNodesMap(boolean active, Character operation) {
		ConcurrentHashMap<ControllerNode, String> threadsMap = new ConcurrentHashMap<>();
		String type;
		String checkClass = ControllerNode.class.getName();
		for (Object thread : getThreadsMap().keySet()) {
			type = getThreadsMap().get(thread);
			if (type != null && type.equals(checkClass) && ((ControllerNode) thread).isActive() == active && ((ControllerNode) thread).getOperations().contains(operation.toString())) {
				threadsMap.put((ControllerNode) thread, type);
			}
		}
		return threadsMap;
	}
 
	/**
	 * @param address
	 * @param port
	 * @return
	 */
	public ControllerNode getNode(String address, String port) {
		String type;
		String checkClass = ControllerNode.class.getName();
		ControllerNode foundNode = null;
		for (Object thread : getThreadsMap().keySet()) {
			type = getThreadsMap().get(thread);
			if (type != null && type.equals(checkClass)) {
				foundNode = (ControllerNode) thread;
				if (foundNode.getNodeAddress().equals(address) && foundNode.getNodePort().equals(port)) {
					return foundNode;
				}
			}
		}
		return null;
	}
	
	public String listOperations() {
		String operations = "";
		String operation = "";
		String nodeOperations = "";
		ConcurrentHashMap<ControllerNode, String> activeNodesMap = getNodesMap(true);
		for (ControllerNode node : activeNodesMap.keySet()) {
			nodeOperations = node.getOperations();
			for (int pos = 0; pos < nodeOperations.length(); pos++) {
				operation = nodeOperations.substring(pos, pos + 1);
				if (! operations.contains(operation)) {
					operations += operation;
				}
			}
		}
		return operations;
	}
 
	/**
	 * @return
	 */
	public ConcurrentHashMap<ControllerUser, String> getUsersMap() {
		ConcurrentHashMap<ControllerUser, String> threadsMap = new ConcurrentHashMap<>();
		String type;
		String checkClass = ControllerUser.class.getName();
		for (Object thread : getThreadsMap().keySet()) {
			type = getThreadsMap().get(thread);
			if (type != null && type.equals(checkClass)) {
				threadsMap.put((ControllerUser) thread, type);
			}
		}
		return threadsMap;
	}
 
	/**
	 * @param username
	 * @return
	 */
	public ControllerUser getUser(String username) {
		String type;
		String checkClass = ControllerUser.class.getName();
		ControllerUser foundUser = null;
		for (Object thread : getThreadsMap().keySet()) {
			type = getThreadsMap().get(thread);
			if (type != null && type.equals(checkClass)) {
				foundUser = (ControllerUser) thread;
				if (foundUser.getUserId().equals(username)) {
					return foundUser;
				}
			}
		}
		return null;
	}
 
	/**
	 * @return the shellThread
	 */
	private synchronized Thread getShellThread() {
		return shellThread;
	}

	/**
	 * @return the acceptCommands
	 */
	private synchronized boolean isAcceptCommands() {
		return acceptCommands;
	}

	/**
	 * @param controllerName the componentName to set
	 */
	private synchronized void setControllerName(String controllerName) {
		this.controllerName = controllerName;
	}

	/**
	 * @param config the config to set
	 */
	private synchronized void setConfig(Config config) {
		this.config = config;
	}

	/**
	 * @param userRequestStream the userRequestStream to set
	 */
	private synchronized void setUserRequestStream(InputStream userRequestStream) {
		this.userRequestStream = userRequestStream;
	}

	/**
	 * @param userResponseStream the userResponseStream to set
	 */
	private synchronized void setUserResponseStream(PrintStream userResponseStream) {
		this.userResponseStream = userResponseStream;
	}

	/**
	 * @param userErrorStream the userErrorStream to set
	 */
	private synchronized void setUserErrorStream(PrintStream userErrorStream) {
		this.userErrorStream = userErrorStream;
	}

	/**
	 * @param shell the shell to set
	 */
	private synchronized void setShell(AdvancedShell shell) {
		this.shell = shell;
	}

	/**
	 * @param serverSocket the serverSocket to set
	 */
	private synchronized void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	/**
	 * @param datagramSocket the datagramSocket to set
	 */
	private synchronized void setDatagramSocket(DatagramSocket datagramSocket) {
		this.datagramSocket = datagramSocket;
	}

	/**
	 * @param threadsMap the threadsMap to set
	 */
	@SuppressWarnings("unused")
	private synchronized void setThreadsMap(Map<Object, String> threadsMap) {
		this.threadsMap = threadsMap;
	}

	/**
	 * @param shellThread the shellThread to set
	 */
	private synchronized void setShellThread(Thread shellThread) {
		this.shellThread = shellThread;
	}

	/**
	 * @param acceptCommands the acceptCommands to set
	 */
	private synchronized void setAcceptCommands(boolean acceptCommands) {
		this.acceptCommands = acceptCommands;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link CloudController}
	 *            component
	 * @throws Exception 
	 */
	public static void main(String[] args) {
		CloudController cloudController = new CloudController(args[0], new Config("controller"), System.in, System.out, System.err);
		// Start the instance in a new thread
		new Thread((Runnable) cloudController).start();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		/*
		 * start TCP and UDP Listener
		 */
		String port = "";
		int portNr = 0;
	    ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");
		try {
			// constructs a UDP datagram socket and binds it to the specified port
			port = getConfig().getString("udp.port");
		    portNr = ((Double) engine.eval(port)).intValue();
		    
		    // create Datagram-Socket
			setDatagramSocket(new DatagramSocket(portNr));

			// create a new thread to listen for incoming packets
			new UdpControllerListener(this, getDatagramSocket()).start();
		} catch (ScriptException e) {
			throw new RuntimeException("Invalid UDP port expression '" + port + "'.", e);
		} catch (IOException e) {
			close();
			throw new RuntimeException("Cannot listen on UDP port.", e);
		}

		try {
			// get TCP port term and calculate expression 
			port = getConfig().getString("tcp.port");
		    portNr = ((Double) engine.eval(port)).intValue();
		    
		    // create Server-Socket
			setServerSocket(new ServerSocket(portNr));
			
			// handle incoming connections from client in a separate thread
			new TcpControllerListener(this, getServerSocket()).start();
		} catch (ScriptException e) {
			throw new RuntimeException("Invalid TCP port expression '" + port + "'.", e);
		} catch (IOException e) {
			close();
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}

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
	 * Closes all resources, like threads, TCP or UDP connections, shell, ...
	 */
	synchronized public void close() {
		// disable further commands
		if (isAcceptCommands()) {
			setAcceptCommands(false);
			// Logout all users, close all Sockets, Threads, Timer, ...
			// close sockets and listening threads
			String type;
			for (Object thread : getThreadsMap().keySet()) {
				type = getThreadsMap().get(thread);
				if (type == null) {
					getShell().printErrLine("Unknown NULL-type thread");
					getThreadsMap().remove(type);
				} else if (type.equals(TcpControllerListener.class.getName())) {
					((TcpControllerListener) thread).close();
					try {
						((TcpControllerListener) thread).join();
					} catch (InterruptedException e) {
						// do nothing
					}
				} else if (type.equals(UdpControllerListener.class.getName())) {
					((UdpControllerListener) thread).close();
					try {
						((UdpControllerListener) thread).join();
					} catch (InterruptedException e) {
						// do nothing
					}
				} else if (type.equals(TcpControllerClient.class.getName())) {
					((TcpControllerClient) thread).close();
					try {
						((TcpControllerClient) thread).join();
					} catch (InterruptedException e) {
						// do nothing
					}
				} else if (type.equals(ControllerUser.class.getName())) {
					((ControllerUser) thread).close();
					// Don't wait because it's no thread, only an object
				} else if (type.equals(TcpControllerNode.class.getName())) {
					((TcpControllerNode) thread).close();
					// Don't wait because it's no thread, only an object
				} else if (type.equals(ControllerNode.class.getName())) {
					((ControllerNode) thread).close();
					// Don't wait because it's no thread, only an object
				} else {
					getShell().printErrLine("Unknown thread type '" + type + "'");
					getThreadsMap().remove(type);
				}
			}
			
			closeTCP();			// Only to be save, if no thread exists, otherwise they will close it
			closeUDP();			// Only to be save, if no thread exists, otherwise they will close it
			
			// close shell
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
	 * Closes the TCP listening socket
	 */
	public void closeTCP() {
		if (getServerSocket() != null && ! getServerSocket().isClosed()) {
			try {
				getServerSocket().close();
			} catch (IOException e) {
				throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
			}
			getShell().printLine("TCP Socket closed.");
		}
		setServerSocket(null);
	}

	/**
	 * Closes the UDP listening socket
	 */
	public void closeUDP() {
		if (getDatagramSocket() != null && ! getDatagramSocket().isClosed()) {
			getDatagramSocket().close();
			getShell().printLine("UDP Socket closed.");
		}
		setDatagramSocket(null);
	}

// ----------------------------------------------------- Commands -----------------------------------------	
	
	/* (non-Javadoc)
	 * @see controller.ICloudControllerCli#nodes()
	 */
	@Override
	@Command
	public String nodes() throws IOException {
		
		int nodeCount = 0;
		String result = "";
		String format = "%n%d. IP: %s, Port: %s (%s), Operations: \'%s\', Usage: %d";
		for (ControllerNode node : getNodesMap().keySet()) {
			nodeCount++;
			result += String.format(format, nodeCount, node.getNodeAddress(), node.getNodePort(), (node.isActive() ? "online" : "offline"), node.getOperations(), node.getUsage());
		}
		return result;

	}

	/* (non-Javadoc)
	 * @see controller.ICloudControllerCli#users()
	 */
	@Override
	@Command
	public String users() throws IOException {
		
		int userCount = 0;
		String result = "";
		String format = "%n%d. %s (%s), Credits: %d";
		for (ControllerUser user : getUsersMap().keySet()) {
			userCount++;
			result += String.format(format, userCount, user.getUserId(), (user.isLoggedIn() ? String.format((user.getLoggedIn() == 1 ? "online %d time" : "online %d times"), user.getLoggedIn()) : "offline"), user.getCredits());
		}
		return result;

	}

	/* (non-Javadoc)
	 * @see controller.ICloudControllerCli#exit()
	 */
	@Override
	@Command
	public String exit() throws IOException {

		close();
		
		return getMessage().shutdown;

	}

	// --- additional Commands not requested in assignment

	// TODO Remove additional commands
	
	/* (non-Javadoc)
	 * @see controller.ICloudControllerCli#help()
	 */
	@Override
	@Command
	public String help() throws IOException {

		String usage = "\navailable Commands:\n";
		usage += "\t!nodes\n";
		usage += "\t!users\n";
		usage += "\t!exit\n";
		usage += "\t!help\n";

		return usage;
	}
}
