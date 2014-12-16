package node.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import node.Node;
import cli.AdvancedShell;

/**
 * Thread to listen for incoming TCP connections on the given socket.
 */
/**
 * @author Robert Bekker (8325143)
 *
 */
public class TcpNodeListener extends Thread {

	private ServerSocket serverSocket;
	private Node node;
	private AdvancedShell nodeShell;
	private final String threadType;
	private boolean acceptCommands = true;

	/**
	 * @param node Node object
	 * @param serverSocket TCP listener socket
	 */
	public TcpNodeListener(Node node, ServerSocket serverSocket) {
		this.setNode(node);
		this.setServerSocket(serverSocket);

		this.setNodeShell(node.getShell());
		this.threadType = this.getClass().getName();
		this.setName(getThreadType() + " " + this.getName());
		
		// register in shared list to be accessible from outside
		this.getNode().getThreadsMap().put(this, getThreadType());
	}

	/**
	 * @param serverSocket the serverSocket to set
	 */
	private synchronized void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
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
	 * @param acceptCommands the acceptCommands to set
	 */
	private synchronized void setAcceptCommands(boolean acceptCommands) {
		this.acceptCommands = acceptCommands;
	}

	/**
	 * @return the serverSocket
	 */
	private synchronized ServerSocket getServerSocket() {
		return serverSocket;
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
	 * @return the threadType
	 */
	private synchronized String getThreadType() {
		return threadType;
	}

	/**
	 * @return the acceptCommands
	 */
	private synchronized boolean isAcceptCommands() {
		return acceptCommands;
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {

		Socket socket = null;
		getNodeShell().printLine(String.format("TCP-Listener is up on port '%d'!", getServerSocket().getLocalPort()));

		while (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
			socket = new Socket();				// will be closed in the client thread

			try {
				// wait for Client to connect
				socket = getServerSocket().accept();

				getNodeShell().printLine("Accept connection from controller.");

				// fork an own thread for each connected client
				new TcpNodeController(getNode(), socket).start();
			} catch (SocketException e) {
				// ignore socket-error on close
				break;
			} catch (IOException e) {
				getNodeShell().printErrLine("Error occurred while waiting for controller: " + e.getMessage());
				break;
			}
		}
		close();
		getNodeShell().printLine("TCP Listener-Thread shut down completed.");
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
			closeTCP();
	
			// remove from list of active threads
			this.getNode().getThreadsMap().remove(this);
			
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
			getNodeShell().printLine("TCP Socket closed.");
		} else {
			getNodeShell().printErrLine("TCP Socket already closed.");
		}
		setServerSocket(null);
	}
}
