package node.udp;

import java.io.IOException;

public interface IUdpNodeSenderCli {

	/**
	 * Sends alive-informations.
	 *
	 * @return processing result
	 * @throws IOException
	 *			if an I/O error occurs
	 */
	String alive(int udpPort, String operations, String address, int tcpPort) throws IOException;

	/**
	 * Sends a hello message.
	 * 
	 * @param udpPort
	 * @param address
	 * @return processing result
	 * @throws IOException
	 * 
	 * 			if an I/O error occurs
	 */
	String hello(int udpPort, String address) throws IOException;
}
