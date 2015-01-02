package node.tcp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import node.Node;
import cli.Command;
import cli.AdvancedShell;
import util.Message;

/**
 * Thread to communicate with a Controller on the given socket
 * to calculate only one compute-command
 */
public class TcpNodeController extends Thread implements ITcpNodeControllerCli, Runnable {

	private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
		}
	};

	private Socket controllerSocket;
	private BufferedReader reader;
	private PrintWriter writer;
	private Node node;
	private AdvancedShell nodeShell;
	private AdvancedShell shell;
	private boolean acceptCommands = true;
	private final String threadType;
	private Message message = new Message();

	/**
	 * @param node Node object
	 * @param controllerSocket TCP Socket to controller
	 */
	public TcpNodeController(Node node, Socket controllerSocket) {
		this.setControllerSocket(controllerSocket);
		this.setNode(node);
		
		this.setNodeShell(node.getShell());
		this.threadType = this.getClass().getName();
		this.setName(getThreadType() + " " + this.getName());
		
		/*
		 * create a new Shell instance, not used to process interactive commands,
		 * but to invoke commands send from Controller.
		 */
		setShell(new AdvancedShell(getThreadType(), null, null, null));

		// register in shared list to be accessible from outside
		this.getNode().getThreadsMap().put(this, getThreadType());
	}

	/**
	 * @param controllerSocket the socket to set
	 */
	private synchronized void setControllerSocket(Socket controllerSocket) {
		this.controllerSocket = controllerSocket;
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
	 * @param node the node to set
	 */
	private synchronized void setNode(Node node) {
		this.node = node;
	}

	/**
	 * @param nodeShell the nodeShell to set
	 */
	private synchronized void setNodeShell(AdvancedShell nodeShell) {
		this.nodeShell = nodeShell;
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
	 * @return the socket
	 */
	private synchronized Socket getControllerSocket() {
		return controllerSocket;
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
	 * @return the node
	 */
	private synchronized Node getNode() {
		return node;
	}

	/**
	 * @return the nodeShell
	 */
	private synchronized AdvancedShell getNodeShell() {
		return nodeShell;
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
			setReader(new BufferedReader(new InputStreamReader(getControllerSocket().getInputStream())));
			// prepare the writer for responding to Controllers requests
			setWriter(new PrintWriter(getControllerSocket().getOutputStream(), true));

			String request;
			Object response;
			// read one request from Controller 
			if ((request = getReader().readLine()) != null && isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {

				getNodeShell().printLine("Controller sent the following request: " + request);
				
				if (isAcceptCommands()) {
					// invoke available commands
					try {
						response = getShell().invoke(request);
					} catch (IllegalArgumentException e) {
						response = getMessage().unknown_command;
					} catch (Throwable throwable) {
						ByteArrayOutputStream str = new ByteArrayOutputStream(1024);
						throwable.printStackTrace(new PrintStream(str, true));
						response = str.toString();
					}
				} else {
					response = getMessage().node_disconnected; 
				}

				// return one response
				if (response != null) {
					getWriter().println(response);
					getNodeShell().printLine("Response sent to the controller: " + response);
				} else {
					getNodeShell().printLine("No response sent to the controller'.");
				}
			}
			// reader not available any more, close
		} catch (SocketException e) {
			// ignore socket-error on close
			getNodeShell().printLine("ignore socket-error on close");
		} catch (InterruptedIOException e) {
			// on InterruptedIOException in readLine just continue closing thread
			getNodeShell().printLine("on InterruptedIOException in readLine just continue closing thread");
		} catch (ClosedByInterruptException e) {
			// on ClosedByInterruptException in readLine just continue closing thread
			getNodeShell().printLine("on ClosedByInterruptException in readLine just continue closing thread");
		} catch (IOException e) {
			getNodeShell().printErrLine("Error occurred while communicating with controller: " + e.getMessage());
		} 
		close(true);
		getNodeShell().printLine("Node-Controller-Thread shut down completed.");
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
	 * By default not planned
	 */
	public void close() {				// ohne Boolean!!!
		close(false);
	}
	synchronized public void close(boolean isPlanned) {
		// disable race conditions
		if (disableCommands()) {
			// only entered by the first close() 
			if (! isPlanned) {
				// Send information to Controller
				getWriter().println(getMessage().node_disconnected);
				// but dont wait for any response
			}
			
			// close socket
			closeTCP();
	
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
			
			// finally remove from list of active threads
			this.getNode().getThreadsMap().remove(this);
			
			// TODO more clean up?

			// finally set the Interrupted-Flag
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes TCP socket to controller
	 */
	synchronized public void closeTCP() {
		if (getControllerSocket() != null) {
			try {
				getControllerSocket().close();
			} catch (IOException e) {
				throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
			}
			setControllerSocket(null);;
		}
	}

//----------------------------------------------------- Remote available commands -----------------------------------------	

	/* (non-Javadoc)
	 * @see node.tcp.ITcpNodeControllerCli#compute(int, java.lang.String, int)
	 */
	@Override
	@Command
	public String compute(int num1, String operation, int num2) throws IOException {
		String response = null;
		int result = 0;
		if (! getNode().getOperations().contains(operation)) {
			response = getMessage().unknown_command; 
		}
		switch (operation) {
			case "+" :
				result = num1 + num2;
			break;
			case "-" :
				result = num1 - num2;
			break;
			case "*" :
				result = num1 * num2;
			break;
			case "/" :
				if (num2 == 0) {
					response = getMessage().zero_division; 
				} else {
					result = (int) Math.round(((double) num1) / ((double) num2));
				}
			break;
			default :
				response = getMessage().unsupported_operation;
		}
		if (response == null) {
			response = String.format("%d", result);
		}

		String logDirName = node.getLogDir();
		File logDir = null;
		try {
			logDir = new File(logDirName).getCanonicalFile();
		} catch ( IOException e ) {
			getNodeShell().printErrLine(String.format("!Error: Could not resolve directory-path '%s'!%n%s", logDir, e.getMessage()));
		}
		if (logDir != null && (! logDir.exists() || logDir.isDirectory())) {
			if (! logDir.exists()) {
				logDir.mkdirs();
			}
			if (logDir.exists()) {
				String logFileName = String.format("%s/%s_%s.log", logDirName, DATE_FORMAT.get().format(new Date()), node.getNodeName());
				String logRequest = String.format("%s %s %s%n", num1, operation, num2);
				String logResponse = String.format("%s", response);		// last line has no trailing newline
				FileWriter logFile = null;
				try	{
					logFile = new FileWriter(logFileName);
					synchronized (logFile) {
						logFile.write(logRequest);
						logFile.append(logResponse);
					}
					getNodeShell().printLine(String.format("Log-file '%s' written.", logFileName));
				}
				catch ( IOException e ) {
					getNodeShell().printErrLine(String.format("!Error: Could not write log-file '%s'!%n%s", logFileName, e.getMessage()));
				}
				if ( logFile != null ) {
					try { 
						logFile.close(); 
					} catch ( IOException e ) {
						e.printStackTrace(); 
					}
					logFile = null;
				}
			} else {
				getNodeShell().printErrLine(String.format("!Error: Cannot make directory '%s'!", logDirName));
			}
		} else {
			getNodeShell().printErrLine(String.format("!Error: '%s' is not a directory!", logDirName));
		}
		
		getNodeShell().printLine("Response: " + response);
		return response;
	}
	
	@Override
	@Command
	public String share(int ressourceLevel) {
		String newLineString = System.getProperty("line.separator");
		String result = "";
		
		if (ressourceLevel >= this.getNode().getConfig().getInt("node.rmin")) {
			result = "!ok";
		} else {
			result = "!nok";
		}
		return result + newLineString;
	}
	
	@Override
	@Command
	public String rollback(int ressourceLevel) {
		return String.format("Rollback command received for ressource level '%s'", ressourceLevel);
	}
	
	@Override
	@Command
	public String commit(int ressourceLevel) {
		this.getNode().setRessourceLevel(ressourceLevel);
		return String.format("Commit command received for ressource level '%s'", ressourceLevel);
	}

}