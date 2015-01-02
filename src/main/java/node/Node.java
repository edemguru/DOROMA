package node;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import node.tcp.TcpNodeController;
import node.tcp.TcpNodeListener;
import node.udp.UdpNodeSender;
import util.Config;
import util.Message;
import cli.Command;
import cli.AdvancedShell;

public class Node implements INodeCli, Runnable {

	private String nodeName;
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
	private Timer udpTimer = null;
	private String operations;
	private Message message = new Message();
	private String logDir;
	private Integer ressourceLevel = new Integer(0);


	/**
	 * @param nodeName
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
	public Node(String nodeName, Config config, InputStream userRequestStream, PrintStream userResponseStream, PrintStream userErrorStream) {
		this.setNodeName(nodeName);
		this.setConfig(config);
		this.setUserRequestStream(userRequestStream);
		this.setUserResponseStream(userResponseStream);
		this.setUserErrorStream(userErrorStream);
		/*
		 * create a new Shell instance and provide the name of the
		 * component, an InputStream as well as an OutputStream. If you want to
		 * test the application manually, simply use System.in and System.out.
		 */
		this.setShell(new AdvancedShell(nodeName, userRequestStream, userResponseStream, userErrorStream));
		setUdpTimer(new Timer());
	}

	/**
	 * @return the logFile
	 */
	public synchronized String getLogDir() {
		return logDir;
	}

	/**
	 * @param logDir the logFile to set
	 */
	private synchronized void setLogDir(String logDir) {
		this.logDir = logDir;
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
	 * @return the operations
	 */
	public synchronized String getOperations() {
		return operations;
	}

	/**
	 * @param operations the operations to set
	 */
	private synchronized void setOperations(String operations) {
		this.operations = operations;
	}

	/**
	 * @param nodeName the componentName to set
	 */
	private synchronized void setNodeName(String nodeName) {
		this.nodeName = nodeName;
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
	 * @param udpTimer the udpTimer to set
	 */
	private synchronized void setUdpTimer(Timer udpTimer) {
		this.udpTimer = udpTimer;
	}
	
	/**
	 * @param ressourceLevel the current ressource level
	 */
	public synchronized void setRessourceLevel(Integer ressourceLevel) {
		this.ressourceLevel = ressourceLevel;
	}

	/**
	 * @return the nodeName
	 */
	public synchronized String getNodeName() {
		return nodeName;
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
	 * @return the udpTimer
	 */
	private synchronized Timer getUdpTimer() {
		return udpTimer;
	}
	
	/**
	 * @return the current ressource level
	 */
	public synchronized Integer getRessourceLevel() {
		return ressourceLevel;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Node}
	 *            component
	 * @throws Exception 
	 */
	public static void main(String[] args) {
		Node Node = new Node(args[0], new Config(args[0]), System.in, System.out, System.err);
		// Start the instance in a new thread
		new Thread((Runnable) Node).start();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
	    setOperations(getConfig().getString("node.operators"));
		this.setLogDir(getConfig().getString("log.dir"));

		/*
		 * start TCP Listener and UDP Sender
		 */
	    ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");
	    
	    // TCP
	    String tcpPort = getConfig().getString("tcp.port");
		int tcpPortNr = 0;
		try {
		    // create TCP Server-Socket
			tcpPortNr = ((Double) engine.eval(tcpPort)).intValue();
			setServerSocket(new ServerSocket(tcpPortNr));
			
			// handle incoming connections from Controller in a separate thread
			new TcpNodeListener(this, getServerSocket()).start();
		} catch (ScriptException e) {
			throw new RuntimeException("Invalid TCP port expression '" + tcpPort + "'.", e);
		} catch (IOException e) {
			close();
			throw new RuntimeException("Cannot send on TCP port.", e);
		}
		
		// UDP
		try {
			// constructs a datagram socket and binds it to the specified port
		    setDatagramSocket(new DatagramSocket());

		    // send UDP packets in a separate thread
			new UdpNodeSender(this, getDatagramSocket()).start();
		} catch (IOException e) {
			close();
			throw new RuntimeException("Cannot listen on UDP port.", e);
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
	 * Closes all resources
	 */
	synchronized public void close() {
		// disable further commands
		if (isAcceptCommands()) {
			setAcceptCommands(false);
			// TODO close all Sockets, Threads, Timer, ...
			
			if(getUdpTimer() != null) {
				getUdpTimer().cancel();
			}
			
			// close socket and listening thread
			String type;
			for (Object thread : getThreadsMap().keySet()) {
				type = getThreadsMap().get(thread);
				if (type == null) {
					getShell().printErrLine("Unknown NULL-type thread");
					getThreadsMap().remove(type);
				} else if (type.equals(TcpNodeListener.class.getName())) {
					((TcpNodeListener) thread).close();
					try {
						((TcpNodeListener) thread).join();
					} catch (InterruptedException e) {
						// do nothing
					}
				} else if (type.equals(UdpNodeSender.class.getName())) {
					((UdpNodeSender) thread).close();
					try {
						((UdpNodeSender) thread).join();
					} catch (InterruptedException e) {
						// do nothing
					}
				} else if (type.equals(TcpNodeController.class.getName())) {
					((TcpNodeController) thread).close();
					try {
						((TcpNodeController) thread).join();
					} catch (InterruptedException e) {
						// do nothing
					}
				} else {
					getShell().printErrLine("Unknown thread type '" + type + "'");
				}
			}
			
			closeTCP();			// Only to be save, if no TCP thread exists
			closeUDP();			// Only to be save, if no UDP thread exists

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

			// finally set the Interrupted-Flag
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes TCP listener socket
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
	 * Closes TCP sender socket
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
	 * @see node.INodeCli#exit()
	 */
	@Override
	@Command
	public String exit() throws IOException {

		close();
		
		return getMessage().shutdown;
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String history(int numberOfRequests) throws IOException {
		// TODO Implement code
		return "// TODO Implement code.";
	}

	@Override
	@Command
	public String resources() throws IOException {
		return String.valueOf(this.getRessourceLevel());
	}

	// --- additional Commands not requested in assignment

	// TODO Remove additional commands

	/* (non-Javadoc)
	 * @see node.INodeCli#help()
	 */
	@Override
	@Command
	public String help() throws IOException {

		String usage = "\navailable Commands:\n";
		usage += "\t!exit\n";
		usage += "\t!help\n";

		return usage;
	}

}
