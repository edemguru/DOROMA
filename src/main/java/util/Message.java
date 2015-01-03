package util;

import java.util.MissingResourceException;

import util.Config;

/**
 * Object used to make common messages stored 
 * in a message-property file accessible to everyone
 */
public class Message {
	// set some constant values
	public final Config message;
	public final String controller_disconnected; 
	public final String login_success;
	public final String unknown_command;
	public final String node_disconnected; 
	public final String unsupported_operation;
	public final String zero_division;
	public final String shutdown;
	public final String illegal_arguments;
	public final String authentication_failed;
	
	/**
	 * Init constants, so no getters needed to access them later
	 */
	public Message() {
		// read shared (communication) messages
		message = new Config("message");
		
		// set some constant message-values
		try {
			controller_disconnected = message.getString("controller_disconnected");
			login_success = message.getString("login_success");
			unknown_command = message.getString("unknown_command");
			node_disconnected = message.getString("node_disconnected");
			unsupported_operation = message.getString("unsupported_operation");
			zero_division = message.getString("zero_division");
			shutdown = message.getString("shutdown");
			illegal_arguments = message.getString("illegal_arguments");
			authentication_failed = message.getString("authentication_failed");
		} catch (MissingResourceException e) {
			throw new RuntimeException("Invalid message-property!", e);
		}
	}
}
