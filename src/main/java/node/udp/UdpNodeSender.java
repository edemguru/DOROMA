package node.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import node.Node;
import node.tcp.RessourceAgreementCollector;
import cli.Command;
import cli.AdvancedShell;
import util.Config;

/**
 * Thread to send isAlive packages on the given UDP socket.
 */
public class UdpNodeSender extends Thread implements IUdpNodeSenderCli {

	private DatagramSocket socket;
	private DatagramPacket packet;
	private Node node;
	private AdvancedShell nodeShell;
	private AdvancedShell shell;
	private final String threadType;
	private boolean acceptCommands = true;
	private Config config;

	/**
	 * @param node Node object
	 * @param socket UDP sender socket
	 */
	public UdpNodeSender(Node node, DatagramSocket socket) {
		this.setNode(node);
		this.setSocket(socket);

		this.setNodeShell(node.getShell());
		this.threadType = this.getClass().getName();
		this.setName(threadType + " " + this.getName());
		this.setConfig(node.getConfig());
		
		/*
		 * create a new Shell instance, not used to process interactive commands,
		 * but to invoke commands send from client.
		 */
		setShell(new AdvancedShell(getThreadType(), null, null, null));

		// register in shared list to be accessible from outside
		this.getNode().getThreadsMap().put(this, getThreadType());
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
	 * @param socket the socket to set
	 */
	private synchronized void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}

	/**
	 * @param packet the packet to set
	 */
	private synchronized void setPacket(DatagramPacket packet) {
		this.packet = packet;
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
	 * @param config the config to set
	 */
	private synchronized void setConfig(Config config) {
		this.config = config;
	}

	/**
	 * @return the socket
	 */
	private synchronized DatagramSocket getSocket() {
		return socket;
	}

	/**
	 * @return the packet
	 */
	private synchronized DatagramPacket getPacket() {
		return packet;
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

	/**
	 * @return the config
	 */
	private synchronized Config getConfig() {
		return config;
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
		
	    ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");

		String tcpPort = getConfig().getString("tcp.port");
		int tcpPortNr = 0;
	    String udpPort = getConfig().getString("controller.udp.port");
		int udpPortNr = 0;
	    
		int timeout = getConfig().getInt("node.alive");
		String operations = getConfig().getString("node.operators");
		getNodeShell().printLine(String.format("Node '%s' supports '%s'-operations.", node.getNodeName(), operations));

		try {
			tcpPortNr = ((Double) engine.eval(tcpPort)).intValue();
		} catch (ScriptException e) {
			throw new RuntimeException("Invalid TCP port expression '" + tcpPort + "'.", e);
			// e.printStackTrace();
		}
		try {
			udpPortNr = ((Double) engine.eval(udpPort)).intValue();
		} catch (ScriptException e) {
			throw new RuntimeException("Invalid UDP port expression '" + udpPort + "'.", e);
			// e.printStackTrace();
		}
	    getNodeShell().printLine(String.format("UDP-Sender is sending from port '%d', TCP-Listerner is on port '%d'!", udpPortNr, tcpPortNr));

		String helloCommand = "!hello " + udpPortNr + " " + getConfig().getString("controller.host");
		Object helloResponse = null;
		try {
			if (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
				helloResponse = getShell().invoke(helloCommand);
				if (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
					Thread.sleep(timeout);
				}
			}
		} catch (InterruptedException e) {
			// do nothing, just close
		} catch (Throwable e1) {
			throw new RuntimeException("Invalid command '" + helloCommand + "'.", e1);
		}
		
		// check output from "!hello" command
		if (helloResponse.equals("NOK")) {
			this.getShell().printErrLine("Ressource Level insufficient, not sending '!alive' messages to Controller !");
			close();
		} else {
			this.getShell().printLine("Ressource Level ok, sending '!alive' messages to Controller");
		}
		
		// public String alive(int udpPort, String operations, String address, int tcpPort) throws IOException {
		String alive = "!alive " + udpPortNr + " " + operations + " " + getConfig().getString("controller.host") + " " + tcpPortNr;
		@SuppressWarnings("unused")
		Object response = null;
		try {
			while (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
				response = getShell().invoke(alive);
				if (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
					Thread.sleep(timeout);
				}
			}
		} catch (InterruptedException e) {
			// do nothing, just close
		} catch (Throwable e1) {
			throw new RuntimeException("Invalid command '" + alive + "'.", e1);
		}

		close();
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
	
			// TODO close shell ???
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
			this.getNode().getThreadsMap().remove(this);
			
			// TODO more clean up?

			// finally set the Interrupted-Flag
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes UDP sender socket
	 */
	public void closeUDP() {
		if (getSocket() != null && ! getSocket().isClosed()) {
			getSocket().close();
			getNodeShell().printLine("UDP Socket closed.");
		} else {
			getNodeShell().printErrLine("UDP Socket already closed.");
		}
		setSocket(null);
	}

	// ----------------------------------------------------- Commands -----------------------------------------	

	/* (non-Javadoc)
	 * @see node.udp.IUdpNodeSenderCli#alive(int, java.lang.String, java.lang.String, int)
	 */
	@Override
	@Command
	public String alive(int udpPort, String operations, String address, int tcpPort) throws IOException {
	    InetAddress hostAddress;
		try {
			hostAddress = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			throw new RuntimeException("Unknwn host '" + address + "'.", e);
		}
		
		String alive = "!alive " + tcpPort + " " + operations;
		byte[] buffer = alive.getBytes();

	    // nodeShell.printLine(String.format("Sending 'isAlive' package '" + alive + "'."));
		try {
			setPacket(new DatagramPacket(buffer, buffer.length, hostAddress, udpPort));
			
			// send UDP-isAlive-packet to controller
			if (getSocket() == null) {
				getNodeShell().printErrLine("Error occurred while sending 'isAlive' packages.");
				close();
				return "Error occurred while sending 'isAlive' packages.";
			} else if (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
				getSocket().send(getPacket());
			} else {
				return null; 
			}
		} catch (IOException e) {
			getNodeShell().printErrLine("Error occurred while sending 'isAlive' packages: " + e.getMessage());
			close();
			return "Error occurred while sending 'isAlive' packages: " + e.getMessage();
		}
		return "'isAlive'-package '" + alive + "' sent.";
	}
	
	@Override
	@Command
	public String hello(int udpPort, String address) throws IOException {
		InetAddress hostAddress;
		try {
			hostAddress = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			throw new RuntimeException(String.format("Unknown host '%s'.", address), e);
		}
		
		String hello = "!hello" + " " + udpPort ;
		byte[] buffer = hello.getBytes();

		nodeShell.printLine(String.format("Sending 'hello' message '%s'.", hello));
		try {
			setPacket(new DatagramPacket(buffer, buffer.length, hostAddress, udpPort));
			
			// send udp hello packet to controller
			if (isAcceptCommands() && ! Thread.currentThread().isInterrupted()) {
				getSocket().send(getPacket());
			} else {
				return null; 
			}
		} catch (IOException | NullPointerException e) {
			getNodeShell().printErrLine("Error occurred while sending 'hello' message: " + e.getMessage());
			close();
			return "Error occurred while sending 'hello' message: " + e.getMessage();
		}
		
		getNodeShell().printLine(String.format("'hello'-package '%s' sent.", hello));

		// wait for result
		getNodeShell().printLine("Waiting for 'hello' package result");
		buffer = new byte[2048];

		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		this.getSocket().receive(packet);

		String msg = new String(buffer, 0, packet.getLength());

		getNodeShell().printLine(String.format("'hello' package result received: '%s'", msg));
		
		String[] parts = msg.split("\\s");
		String rmax  = null;
		
		// find rmax using regex
		Integer rmaxInt = null;
		if (parts.length == 3) {
			rmax = parts[2];
			rmaxInt = Integer.parseInt(rmax);
			getNodeShell().printLine(String.format("Controller sent that rmax is: %s", rmax));
		}

		// check if there are zero nodes
		boolean zeroNodesReturned = msg.contains("!init null ");
		
		boolean localResssourceLevelIsSufficient = false;
		int ressourceLevel = 0;
		Integer rmin = this.getConfig().getInt("node.rmin");
		ArrayList<String> nodeList = new ArrayList<String>();

		// more than zero nodes returned, get all nodes and do ressource level calulations
		if(!zeroNodesReturned) {

			String nodesRgxString = "(\\w+:\\d+)";
			Pattern nodesRgxPattern = Pattern.compile(nodesRgxString);
			Matcher nodesRgxMatcher = nodesRgxPattern.matcher(msg);
			int nodeCnt = 0;

			while (nodesRgxMatcher.find()) {
				nodeCnt++;
				String nodeMatchString = nodesRgxMatcher.group();
				nodeList.add(nodeMatchString);
			}

			// calculate ressource level for each node
			ressourceLevel = rmaxInt / (nodeCnt+1);
	
			// check if ressource level is sufficient for this node

			localResssourceLevelIsSufficient = (ressourceLevel >= rmin); 
		}	

		// in case zero nodes are active, just check if rmin <= rMax (rmax sent by the controller)
		if (zeroNodesReturned) {
			String verifyMsg = "Zero nodes connected, verifiying that rmin %s if lower than rmax %s";
			getNodeShell().printLine(String.format(verifyMsg, rmin, rmaxInt));
			localResssourceLevelIsSufficient = (rmin <= rmaxInt);
			ressourceLevel = rmin;
		}
	
		getNodeShell().printLine(String.format("Ressource Level %s is sufficient: %s", ressourceLevel, localResssourceLevelIsSufficient));

		Boolean agreementCollectorStarted = false;
		Boolean agreementCollectionSuccessfull = null;

		// start ressource agreement collector if there are nodes available
		if (!zeroNodesReturned) {
			RessourceAgreementCollector collector = new RessourceAgreementCollector(this.node, nodeList);
			agreementCollectorStarted = true;
			agreementCollectionSuccessfull = collector.doCollection(ressourceLevel);
		}
		
		// collector was not started and local ressource level is sufficient -> is OK
		if (!agreementCollectorStarted && localResssourceLevelIsSufficient) {
			return "OK";
		}
		// collector was started, returned true and local ressource level is sufficient -> is OK
		else if (agreementCollectorStarted && localResssourceLevelIsSufficient && agreementCollectionSuccessfull) {
			return "OK";
		// anything else ->  is NOT OK
		} else {
			return "NOK";
		}
	}

}
