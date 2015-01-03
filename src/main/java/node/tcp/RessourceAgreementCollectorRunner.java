package node.tcp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import node.Node;

public class RessourceAgreementCollectorRunner implements Runnable {

	private String host;
	private String port;
	private Integer ressourceLevel;
	private String newLineString = System.getProperty("line.separator");
	private AtomicInteger oksReceived;
	private AtomicInteger noksReceived;
	private Node node;

	public RessourceAgreementCollectorRunner(String host, String port, Integer ressourceLevel, AtomicInteger oksReceived, AtomicInteger noksReceived, Node node) {
		this.host = host;
		this.port = port;
		this.ressourceLevel = ressourceLevel;
		this.oksReceived = oksReceived;
		this.noksReceived = noksReceived;
		this.node = node;
	}

	@Override
	public void run() {
		Socket clientSocket;

		try {
			clientSocket = new Socket(host, Integer.parseInt(port));
			DataOutputStream outToServer = new DataOutputStream(
					clientSocket.getOutputStream());
			String sentence = "!share" + " " + ressourceLevel + newLineString;
			byte[] sentenceBytes = sentence.getBytes();
			outToServer.write(sentenceBytes);

			// get response (either !ok or !nok)
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String receivedSentence = inFromServer.readLine();

			// increment "ok"'s received
			if (receivedSentence.trim().equals("!ok")) {
				oksReceived.getAndIncrement();
			}

			// increment "noks" received
			if (receivedSentence.trim().equals("!nok")) {
				noksReceived.getAndIncrement();
			}

			this.node.getShell().printLine(String.format("Node %s replied to command '%s' with '%s'",
					host + ":" + port, sentence.trim(), receivedSentence.trim()));

		} catch (Exception e) {
			this.node.getShell().printErrLine("Error: " + e.toString());
		}
	}
}