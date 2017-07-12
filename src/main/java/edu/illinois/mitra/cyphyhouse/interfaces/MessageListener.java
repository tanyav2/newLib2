package edu.illinois.mitra.cyphyhouse.interfaces;

import edu.illinois.mitra.cyphyhouse.comms.RobotMessage;

/**
 * Objects implementing MessageListener may register themselves
 * with the main gvh.comms object to be notified of received messages
 * 
 * @author Adam Zimmerman
 * @version 1.0
 * @see RobotMessage
 * 
 */
public interface MessageListener {
	/**
	 * @param m The received RobotMessage
	 */
	void messageReceived(RobotMessage m);
}
