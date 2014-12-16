package controller.tcp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import controller.CloudController;
import cli.Command;
import cli.AdvancedShell;
import util.Message;

/**
 * Thread to communicate with a node 
 * for only one compute command
 * on a new node-socket, closed afterwards
 * returning result on given client socket
 */
// public class TcpControllerNode extends Thread implements ITcpControllerNodeCli, Runnable {
public class TcpControllerNode implements ITcpControllerNodeCli {

	private CloudController controller;
	private AdvancedShell controllerShell;
	private AdvancedShell shell;
	private boolean acceptCommands = true;
	private final String threadType;
	private Message message = new Message();
	private Socket nodeSocket;
	TcpControllerClient tcpControllerClient = null;

	/**
	 * @param controller Cloud controller object
	 * @param nodeSocket TCP node socket
	 */
	public TcpControllerNode(CloudController controller, Socket nodeSocket) {
		this.setNodeSocket(nodeSocket);
		this.setController(controller);
		
		this.setControllerShell(controller.getShell());
		this.threadType = this.getClass().getName();
		// this.setName(getThreadType() + " " + this.getName());
		
		/*
		 * create a new Shell instance, not used to process interactive commands,
		 * but to invoke commands send from client.
		 */
		setShell(new AdvancedShell(getThreadType(), null, null, null));

		// register in shared list to be accessible from outside
		this.getController().getThreadsMap().put(this, getThreadType());

		/*
		 * register all commands the Shell should support, 
		 * but never start this shell in an own thread
		 */
		getShell().register(this);
	}

	/**
	 * @return the tcpControllerClient
	 */
	@SuppressWarnings("unused")
	private synchronized TcpControllerClient getTcpControllerClient() {
		return tcpControllerClient;
	}

	/**
	 * @param tcpControllerClient the tcpControllerClient to set
	 */
	@SuppressWarnings("unused")
	private synchronized void setTcpControllerClient(
			TcpControllerClient tcpControllerClient) {
		this.tcpControllerClient = tcpControllerClient;
	}

	/**
	 * @param nodeSocket the socket to set
	 */
	private synchronized void setNodeSocket(Socket nodeSocket) {
		this.nodeSocket = nodeSocket;
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
	 * @return the socket
	 */
	private synchronized Socket getNodeSocket() {
		return nodeSocket;
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

	public String request(String request) {
		
		// 1. connect to node, if possible
		// 2. forward compute request
		// 3. receive compute response, or error
		// 4. disconnect from node
		
		// read client requests
		getControllerShell().printLine("Controller want's to send the following request: " + request);
		
		Object response;
		if (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
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

		// return response
		if (response != null) {
			getControllerShell().printLine("Response received from node: " + response);
			return response.toString();
		} else {
			getControllerShell().printLine("No response received from node.");
			return null;
		}
	}

	/**
	 * Send request to node and return result
	 * @param request
	 * @return result
	 */
	private String response(String request) {
		String inString = "";

		if (! nodeSocket.isClosed() && isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
			try {
				// create a writer to send messages to the node
				PrintWriter serverWriter = new PrintWriter(getNodeSocket().getOutputStream(), true);
				if (serverWriter.checkError()) {
					getControllerShell().printErrLine(getMessage().node_disconnected);
					throw new IOException(getMessage().node_disconnected);
				}
				// write provided user input to the socket
				serverWriter.println(request);
				
				// create a reader to retrieve messages send by the node
				BufferedReader serverReader = new BufferedReader(new InputStreamReader(getNodeSocket().getInputStream()));
				// read node response
				inString = null;
				Character myChar = null;
				if ((myChar = (char) serverReader.read()) != -1) {
					inString = myChar.toString();
					while (serverReader.ready()) {
						if ((myChar = (char) serverReader.read()) == -1) {
							// ready (data available) but unexpected end of input found 
							getControllerShell().printErrLine("!Error: Read-Problems!");
							throw new IOException("!Error: Read-Problems!");
						}
						inString += myChar.toString();
					}
				}
				if (inString == null || inString.contains(getMessage().node_disconnected)) {
					// getControllerShell().printErrLine(getMessage().node_disconnected);
					throw new SocketException(getMessage().node_disconnected);
				}
				// strip all trailing \n or \r
				while (inString.endsWith("\n") || inString.endsWith("\r")) {
					inString = inString.substring(0, inString.length() - 1);
				}
			} catch (SocketException e) {
				getControllerShell().printErrLine(getMessage().node_disconnected);
				close();
				return getMessage().node_disconnected;
			} catch (IOException e) {
				getControllerShell().printErrLine("!Error: Unknown communication problem!");
				close();
				throw new RuntimeException(e.getClass().getSimpleName(), e);		// TODO ?????
			}
			// return the response to the controller which will be written on console
			close();
			return inString;
		} else {
			getControllerShell().printErrLine(getMessage().node_disconnected);
			// return nothing to be written to console
			return null;
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
	 */
	synchronized public void close() {
		// disable race conditions
		if (disableCommands()) {
			// only entered by the first close() 

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
			this.getController().getThreadsMap().remove(this);
			
			// TODO more clean up?

			// DON'T set the Interrupted-Flag
			// Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes TCP socket
	 */
	synchronized public void closeTCP() {
		if (getNodeSocket() != null) {
			try {
				getNodeSocket().close();
			} catch (IOException e) {
				throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
			}
			setNodeSocket(null);
		}
	}

//----------------------------------------------------- Local available commands -----------------------------------------	

	@Override
	@Command
	public String compute(int val1, String operator, int val2) throws IOException {
		// TODO Auto-generated method stub
		return response(String.format("!compute %d %s %d", val1, operator, val2));
	}
	
}
