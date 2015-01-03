package node.tcp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import node.Node;

/**
 * This component is called by a node if a node wants to join.
 * It sends the "!share X" (X is the amount of resources that can be consumed by every node) command to all nodes.
 * It is also responsible for managing the results (Either "!ok" or "!nok").
 */
public class RessourceAgreementCollector {

	private Integer nodeCount;
	private AtomicInteger oksReceived = new AtomicInteger(0);
	private AtomicInteger noksReceived = new AtomicInteger(0);
	private ArrayList<String> nodeList;
	private Node node;
	private String newLineString = System.getProperty("line.separator");

	public RessourceAgreementCollector(Node node, ArrayList<String> nodeList) {
		this.node = node;
		this.nodeList = nodeList;
	}

	/**
	 * This method returns true if every node replies with "!ok", and returns false if more than one replies with "!nok".
	 * 
	 * @param ressourceLevel
	 * @return Boolean Result of the ressource agreement collection runs.
	 * @throws NumberFormatException
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public Boolean doCollection(int ressourceLevel) throws NumberFormatException, UnknownHostException, IOException {
		nodeCount = nodeList.size();
		int threadLimit = 10;
		
		// limit maximal concurrent threads if there would be more than "threadLimit" threads
		if (nodeCount <= threadLimit) {
			threadLimit = nodeCount;
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(threadLimit);

		// send "!share X" command to all nodes
		for (String node : nodeList) {
			
			String host = node.split(":")[0];
			String port = node.split(":")[1];

			Runnable runner = new RessourceAgreementCollectorRunner(host, port, ressourceLevel, oksReceived, noksReceived, this.node);
			executor.execute(runner);
			
		}

		// wait for thread pool finish
		executor.shutdown();
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				this.node.getShell().printErrLine("Error: " + e.toString());
			}
		}

		// if all nodes agreed, send a "!commit X" message to all nodes
		Boolean allNodesAgreed = ( (noksReceived.intValue() == 0) && (oksReceived.intValue() == nodeCount) );

		if (allNodesAgreed) {
			
			// all nodes agreed
			this.node.getShell().printLine(String.format("All %s nodes agreed to ressource Level %s", nodeCount, ressourceLevel));
			sendCommandToNodes("!commit" + " " + ressourceLevel, "commit");
			this.node.setRessourceLevel(ressourceLevel);
			return true;
			
		} else {
			
			// not all nodes agreed, send a "!rollback X" message to all nodes and print a error message
			String rollbackMsg = "Error: %s of %s nodes didn't agree to the ressource level %s. Need to send rollback commands.";
			this.node.getShell().printErrLine(String.format(rollbackMsg, noksReceived.intValue(), nodeCount, ressourceLevel));
			sendCommandToNodes("!rollback" + " " + ressourceLevel, "rollback");
			return false;
			
		}
	}

	/**
	 * Sends a message to all nodes currently in the node list.
	 */
	private void sendCommandToNodes(String command, String type) {
		this.node.getShell().printLine(String.format("Sending %s command to %s nodes.", type, nodeCount));

		for (String node : nodeList) {
			Socket clientSocket;

			String host = node.split(":")[0];
			String port = node.split(":")[1];

			try {
				clientSocket = new Socket(host, Integer.parseInt(port));
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				String sentence = command + newLineString;
				byte[] sentenceBytes = sentence.getBytes();
				outToServer.write(sentenceBytes);
				this.node.getShell().printLine(String.format("Sent command '%s' to node '%s'", sentence.trim(), node));
			} catch (Exception e) {
				this.node.getShell().printErrLine("Error: " + e.toString());
			}
		}		
	}
}