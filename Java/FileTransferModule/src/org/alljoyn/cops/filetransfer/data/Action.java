/******************************************************************************
 * Copyright (c) 2013, AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 ******************************************************************************/

package org.alljoyn.cops.filetransfer.data;

import java.util.ArrayList;

/**
 * The action class defines the different action types that tell the
 * Transmitter which action needs to be taken. This class defines an
 * enumerated type that defines 10 different actions. This class
 * also includes an array of objects because different actions require
 * different parameters to execute the action and the data types vary
 * dramatically. Lastly, the action object contains a variable for peer.
 * This variable tells the Transmitter who to send the signal to or
 * perform a method call on their proxy bus object.
 * <p>
 * Note: the peer will be set to null only when a global signal will
 * be sent to all session peers.
 * <p> 
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
 */
public class Action 
{
	// Class Enumerator
	public enum ActionType 
	{ 
		ANNOUNCE, 
		REQUEST_ANNOUNCE, 
		REQUEST_OFFER, 
		REQUEST_DATA,
		DATA_CHUNK,
		OFFER_FILE,
		STOP_XFER,
		XFER_CANCELLED,
		FILE_ID_RESPONSE,
		SHUTDOWN_THREAD
	}; 
	
	// Member Variables
	public ActionType actionType;
	public ArrayList<Object> parameters;
	public String peer;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * Action()
	 * creates an instance of the action class and provides initial values for
	 * all member variables.
	 */
	public Action()
	{
		this.actionType = ActionType.ANNOUNCE;
		this.parameters = new ArrayList<Object>();
		this.peer = null;
	}
}
