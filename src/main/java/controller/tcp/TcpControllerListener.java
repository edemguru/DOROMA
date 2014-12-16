package controller.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import controller.CloudController;
import cli.AdvancedShell;

/**
 * Thread to listen for incoming TCP connections on the given socket.
 */
public class TcpControllerListener extends Thread {

	private CloudController controller;
	private AdvancedShell controllerShell;
	private final String threadType;
	private boolean acceptCommands = true;
	private ServerSocket serverSocket;

	/**
	 * @param controller Cloud controller object
	 * @param serverSocket TCP client listening socket 
	 */
	public TcpControllerListener(CloudController controller, ServerSocket serverSocket) {
		this.setController(controller);
		this.setServerSocket(serverSocket);

		this.setControllerShell(controller.getShell());
		this.threadType = this.getClass().getName();
		this.setName(getThreadType() + " " + this.getName());
		
		// register in shared list to be accessible from outside
		this.getController().getThreadsMap().put(this, getThreadType());
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
	 * @param acceptCommands the acceptCommands to set
	 */
	private synchronized void setAcceptCommands(boolean acceptCommands) {
		this.acceptCommands = acceptCommands;
	}

	/**
	 * @param serverSocket the serverSocket to set
	 */
	private synchronized void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	/**
	 * @return the serverSocket
	 */
	private synchronized ServerSocket getServerSocket() {
		return serverSocket;
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
		getControllerShell().printLine(String.format("TCP-Listener is up on port '%d'!", getServerSocket().getLocalPort()));

		try {
			while (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
				socket = new Socket();				// will be closed in the client thread
	
				// wait for Client to connect
				socket = getServerSocket().accept();

				getControllerShell().printLine("Accept connection from client.");

				// fork an own thread for each connected client
				new TcpControllerClient(getController(), socket).start();
			}
		} catch (SocketException e) {
			// ignore socket-error on close
		} catch (IOException e) {
			getControllerShell().printErrLine("Error occurred while waiting for client: " + e.getMessage());
		}
		close();
		getControllerShell().printLine("TCP Listener-Thread shut down completed.");
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
			this.getController().getThreadsMap().remove(this);
			
			// TODO more clean up?

			// finally set the Interrupted-Flag
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes TCP socket
	 */
	public void closeTCP() {
		if (getServerSocket() != null && ! getServerSocket().isClosed()) {
			try {
				getServerSocket().close();
			} catch (IOException e) {
				throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
			}
			getControllerShell().printLine("TCP Socket closed.");
		} else {
			getControllerShell().printErrLine("TCP Socket already closed.");
		}
		setServerSocket(null);
	}
}
