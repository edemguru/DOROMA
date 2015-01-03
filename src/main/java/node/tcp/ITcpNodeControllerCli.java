package node.tcp;

import java.io.IOException;

public interface ITcpNodeControllerCli {

	/**
	 * Process compute command.
	 *
	 * @return compute result
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String compute(int num1, String operation, int num2) throws IOException;

	/**
	 * Process a share command.
	 * 
	 * @param ressourceLevel
	 * @return command result
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String share(int ressourceLevel) throws IOException;

	/**
	 * Process a rollback command.
	 * 
	 * @param ressourceLevel
	 * @return command result
	 * @throws IOException
	 *			if an I/O error occurs
	 */
	String rollback(int ressourceLevel);

	/**
	 * Process a commit command.
	 * 
	 * @param ressourceLevel
	 * @return command result
	 * @throws IOException
	 *			if an I/O error occurs
	 */
	String commit(int ressourceLevel);

}
