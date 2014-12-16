package controller;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import controller.CloudController;
import controller.tcp.TcpControllerClient;
import controller.tcp.TcpControllerNode;
import cli.AdvancedShell;
// import util.Message;

/**
 * Object to organize all TCP-controller-node-objects 
 * but just for one node,
 * started for each new compute-command for this node,
 * threads will communicate with the node on own sockets
 */
public class ControllerNode {

	private CloudController controller;
	private AdvancedShell controllerShell;
	private boolean acceptCommands = true;
	private final String threadType;
	// private Message message = new Message();
	private String operations;
	private String nodeAddress;
	private String nodePort;
	private boolean active = false;
	private Timer timer;
	private int usage = 0;

	/**
	 * @param controller the cloud component object
	 * @param nodeAddress IP address of the assigned node
	 * @param nodePort IP port of the assigned node
	 * @param operations currently supported operations
	 * @param timer inactivity-timeout
	 */
	public ControllerNode(CloudController controller, String nodeAddress, String nodePort, String operations, int timer) {
		this.setController(controller);
		this.setNodeAddress(nodeAddress);
		this.setNodePort(nodePort);
		this.setOperations(operations);
				
		this.setActive(true);
	    this.setControllerShell(controller.getShell());
		this.threadType = this.getClass().getName();
		// this.setName(getThreadType() + " " + this.getName());

		setTimer(timer);
		
		// register in shared list to be accessible from outside, even if this is not a really thread, just an object
		this.getController().getThreadsMap().put(this, getThreadType());
	}

	/**
	 * @return the usage
	 */
	public synchronized int getUsage() {
		return usage;
	}

	/**
	 * @param usage the usage to set
	 */
	private synchronized void setUsage(int usage) {
		this.usage = usage;
	}

	/**
	 * @param usage the usage to set
	 */
	public void increaseUsage(String value) {
		if (value.contains("-")) {
			setUsage(getUsage() + (value.length() - 1) * 50);
		} else {
			setUsage(getUsage() + value.length() * 50);
		}
	}

	/**
	 * @return the controllerShell
	 */
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
	 * @return the address
	 */
	public synchronized String getNodeAddress() {
		return nodeAddress;
	}

	/**
	 * @param address the address to set
	 */
	private synchronized void setNodeAddress(String nodeAddress) {
		this.nodeAddress = nodeAddress;
	}

	/**
	 * @return the timer
	 */
	private synchronized Timer getTimer() {
		return timer;
	}

	/**
	 * @param timer the timer to set
	 */
	private synchronized void setTimer(Timer timer) {
		this.timer = timer;
	}

	/**
	 * @param timeout
	 */
	private void setTimer(int timeout) {
		// inner TimerTask-class
		class TimeoutTask extends TimerTask {
			@Override public void run() {
				setActive(false);
				getControllerShell().printLine(String.format("Node '%s:%s' set inactive by TimeoutTask '%s'", getNodeAddress(), getNodePort(), this.toString()));
			}
		}
		
		if (getTimer() != null) {
			getTimer().cancel();
			// getControllerShell().printLine(String.format("Timer '%s' canceled by ControllerNode '%s'\n", getTimer().toString(), this.toString()));
		}
		setTimer(new Timer());
		getTimer().schedule(new TimeoutTask(), timeout);
		// getControllerShell().printLine(String.format("New Timer '%s' started with Timeout=%d by ControllerNode '%s'", getTimer().toString(), timeout, this.toString()));
	}

	/**
	 * @return the active
	 */
	public synchronized boolean isActive() {
		return active;
	}

	/**
	 * @param active the active to set
	 */
	private synchronized void setActive(boolean active) {
		this.active = active;
		if (! active && getTimer() != null) {
			getTimer().cancel();
		}
	}

	/**
	 * @param active the active to set
	 */
	public synchronized void setActive(int timeout) {
		if (! isActive()) {
			setActive(true);
			getControllerShell().printLine(String.format("Reactivate Node '%s:%s'", getNodeAddress(), getNodePort()));
		}
		setTimer(timeout);
	}

	/**
	 * @return the port
	 */
	public synchronized String getNodePort() {
		return nodePort;
	}

	/**
	 * @param port the port to set
	 */
	private synchronized void setNodePort(String nodePort) {
		this.nodePort = nodePort;
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
	public synchronized void setOperations(String operations) {
		this.operations = operations.trim();
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

	public String request(String request, TcpControllerClient tcpControllerClient) {
		String response = null;
		Socket nodeSocket = null;
		try {
		    // create new local Node-Socket
			nodeSocket = new Socket(getNodeAddress(), Integer.parseInt(getNodePort()));
			// handle outgoing connection to node in an own object
			TcpControllerNode tcpControllerNode = new TcpControllerNode(getController(), nodeSocket);
			// send request and wait for response
			response = tcpControllerNode.request(request);
			// and finally close the node-socket 
			nodeSocket.close();
			nodeSocket = null;
		} catch (NumberFormatException e) {
			throw new RuntimeException(String.format("Invalid port '%d'.", getNodePort()), e);
		} catch (IOException e) {
			if (nodeSocket != null) {
				try {
					nodeSocket.close();
				} catch (IOException e1) {
					// do nothing
				}
				nodeSocket  = null;
			}
			close();
			throw new RuntimeException("Cannot communicate with Node.", e);
		}
		return response;
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
	
			if (getTimer() != null) {
				getTimer().cancel();
			}
			
			// finally remove from list of active threads
			this.getController().getThreadsMap().remove(this);
			
			// TODO more cleanup?
			
			// Don't interrupt because it's an object, no thread
		}
	}
}