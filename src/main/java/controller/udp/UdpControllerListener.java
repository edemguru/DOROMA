package controller.udp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import cli.Command;
import cli.AdvancedShell;
import controller.CloudController;
import controller.ControllerNode;
import util.Message;

/**
 * Thread to listen for incoming data packets on the given socket.
 */
public class UdpControllerListener extends Thread implements IUdpControllerListenerCli, Runnable {

	private DatagramSocket datagramSocket;
	private CloudController controller;
	private AdvancedShell controllerShell;
	private AdvancedShell shell;
	private boolean acceptCommands = true;
	private final String threadType;
	private Message message = new Message();

	public UdpControllerListener(CloudController controller, DatagramSocket datagramSocket) {
		this.setController(controller);
		this.setDatagramSocket(datagramSocket);

		this.setControllerShell(controller.getShell());
		this.threadType = this.getClass().getName();
		this.setName(getThreadType() + " " + this.getName());

		/*
		 * create a new Shell instance, not used to process interactive commands,
		 * but to invoke commands send from node.
		 */
		setShell(new AdvancedShell(getThreadType(), null, null, null));

		// register in shared list to be accessible from outside
		this.getController().getThreadsMap().put(this, getThreadType());
	}

	/**
	 * @param datagramSocket the datagramSocket to set
	 */
	private synchronized void setDatagramSocket(DatagramSocket datagramSocket) {
		this.datagramSocket = datagramSocket;
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
	 * @return the datagramSocket
	 */
	private synchronized DatagramSocket getDatagramSocket() {
		return datagramSocket;
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

	public void run() {

		/*
		 * register all commands the Shell should support, 
		 * but never start this shell in an own thread
		 */
		getShell().register(this);
		
		byte[] buffer;
		DatagramPacket packet;
		getControllerShell().printLine(String.format("UDP-Listener is up on port '%d'!", getDatagramSocket().getLocalPort()));

		Object response;
		String request;
		while (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
			if (isAcceptCommands()) {
				buffer = new byte[1024];

				// create a datagram packet of specified length (buffer.length)
				/*
				 * Keep in mind that: in UDP, packet delivery is not
				 * guaranteed, and the order of the delivery/processing is not
				 * guaranteed
				 */
				packet = new DatagramPacket(buffer, buffer.length);

				try {
					// wait for incoming packets from client
					getDatagramSocket().receive(packet);
					// get the address of the sender (node) from the received packet
					InetAddress address = packet.getAddress();
					// get the port of the sender from the received packet
					// int udpPort = packet.getPort();
					// getControllerShell().printLine(String.format("Packet received from Node '%s:%d': %s", address, udpPort, new String(packet.getData())));
	
					// add IP-Address to received packet (as String)
					request = new String(packet.getData()) + " " + address.getCanonicalHostName();
					// invoke available commands
					try {
						response = getShell().invoke(request);
					} catch (IllegalArgumentException e) {
						response = getMessage().unknown_command;
						// response = null;
					} catch (Throwable throwable) {
						ByteArrayOutputStream str = new ByteArrayOutputStream(1024);
						throwable.printStackTrace(new PrintStream(str, true));
						response = str.toString();
					}
					if (response != null) {
						getControllerShell().printLine(response.toString());
					}
				} catch (SocketException e) {
					// ignore socket-error on close
					break;
				} catch (InterruptedIOException e) {
					// on InterruptedIOException in receive just continue closing thread
					break;
				} catch (ClosedByInterruptException e) {
					// on ClosedByInterruptException in receive just continue closing thread
					break;
				} catch (IOException e) {
					getControllerShell().printErrLine("Error occurred while waiting for/handling UDP packets: " + e.getMessage());
					break;
				}
			} else {
				response = getMessage().controller_disconnected; 
				getControllerShell().printErrLine(response.toString());
			}
		}
		close();
		getControllerShell().printLine("UDP Listener-Thread shut down completed.");
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
			closeUDP();
	
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

			// remove from list of active threads
			this.getController().getThreadsMap().remove(this);
			
			// TODO more clean up?

			// finally set the Interrupted-Flag
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes UDP listening socket
	 */
	public void closeUDP() {
		if (getDatagramSocket() != null && ! getDatagramSocket().isClosed()) {
			getDatagramSocket().close();
			getControllerShell().printLine("UDP Socket closed.");
		} else {
			getControllerShell().printErrLine("UDP Socket already closed.");
		}
		setDatagramSocket(null);
	}

//----------------------------------------------------- Node commands -----------------------------------------	

	@Override
	@Command
	// TODO Doku
	public String alive(String nodePort, String operations, String nodeAddress) throws IOException {

		// 1. Check for existing controller-node-thread running for this specific node
		// 1.a. start new thread if necessary and register in controller
		// 1.b. otherwise clear current timer in existing active thread
		// 1.c. or set inactive thread to active
		// 2. set new timer in thread
		// 3. update supported operations
		
		int timer = Integer.parseInt(controller.getConfig().getString("node.timeout"));
		
		ControllerNode controllerNode = controller.getNode(nodeAddress, nodePort);
		if (controllerNode != null) {
			controllerNode.setOperations(operations);
			controllerNode.setActive(timer);
			// return "Existing node " + nodeAddress + ":" + nodePort + " actualized.";
			return null;
		} else {
			new ControllerNode(getController(), nodeAddress, nodePort, operations, timer);
			return "New node " + nodeAddress + ":" + nodePort + " initialized."; 
		}
	}

}
