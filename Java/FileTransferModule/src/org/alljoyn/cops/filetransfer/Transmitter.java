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

package org.alljoyn.cops.filetransfer;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.cops.filetransfer.alljoyn.FileDiscoveryInterface;
import org.alljoyn.cops.filetransfer.alljoyn.FileTransferBusObject;
import org.alljoyn.cops.filetransfer.alljoyn.DataTransferInterface;
import org.alljoyn.cops.filetransfer.data.Action;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.utility.Logger;

/**
 * The Transmitter class is a major piece of the File Transfer Module and is
 * responsible for direct communication with AllJoyn session peers. The transmitter
 * is responsible for sending directed and broadcast signals to the various session
 * peers as well as calling the appropriate AllJoyn methods on the proxy bus objects.
 * Furthermore, this module is the driving force behind communicating with peers
 * within your AllJoyn session.  
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class.
 */
public class Transmitter 
{
	//Load AllJoyn library
	static 
	{
	    System.loadLibrary("alljoyn_java");
	}	
	
	/** Member Variables **/
	private int sessionID;
	private BusAttachment bus;
	private FileTransferBusObject localBusObject;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * Transmitter()
	 * constructs a new instance of the Transmitter class. The constructor parameters
	 * bus and session ID are used to initialize member variables. If the busAttachment
	 * is not null, the file transfer bus object will be registered with the AllJoyn 
	 * bus attchment to enable session communication.
	 * 
	 * @param localBusObject  instance of FileTransferBusObject
	 * @param bus  instance of an AllJoyn BusAttachment
	 * @param sessionID  specifies the ID of the Session
	 */
	public Transmitter(FileTransferBusObject localBusObject, BusAttachment bus, int sessionID)
	{
		this.sessionID = sessionID;
		this.bus = bus;		
		this.localBusObject = localBusObject;
		
		if (bus != null)
		{
			Status status = bus.registerBusObject(localBusObject, FileTransferBusObject.OBJECT_PATH);		
			Logger.log("registering bus object returned: " + status.toString());
		}			
	}
	
	/*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/	
	/**
	 * transmit()
	 * is called when actions are dequeued from the dispatcher. This function
	 * will call various private helper functions based on the action type. If
	 * there is no AllJoyn session specified this function will the Status Code
	 * NO_AJ_CONNECTION.
	 * 
	 * @param action  specifies the action to be transmitted
	 * @return success or failure of transmitted action
	 * @throws Exception
	 */
	public int transmit(Action action) throws Exception
	{
		if (bus == null)
		{
			Logger.log("transmit called without valid connection");
			return StatusCode.NO_AJ_CONNECTION;
		}		
		
		switch (action.actionType)
		{
			case ANNOUNCE:
				return sendAnnounceSignal(action);			
			case REQUEST_DATA:
				return sendRequestData(action);
			case DATA_CHUNK:
				return sendDataChunk(action);
			case OFFER_FILE:
				return sendOfferFile(action);
			case REQUEST_ANNOUNCE:
				return sendAnnouncementRequest(action);
			case REQUEST_OFFER:
				return sendRequestOffer(action);
			case STOP_XFER:
				return sendStopDataXfer(action);
			case XFER_CANCELLED:
				return sendXferCancelled(action);
			default:
				throw new Exception("Cannot transmit unknown ActionType");
		}
	}
	
	/**
	 * sendAnnounceSignal()
	 * is called when the transmit() methods encounters an Announce action. This function
	 * uses the AllJoyn signal emitter to send announce signal to session peers. If the 
	 * peer is null, the announcement signal is sent to all session peers. Otherwise, the 
	 * signal is directed at the specified peer. This function will eventually get 
	 * triggered when announce() or stopAnnounce() is called on the FileTransferModule.
	 * 
	 * @param action  specifies the action
	 * @return OK
	 * @throws Exception
	 */
	private int sendAnnounceSignal(Action action) throws Exception
	{
		FileDescriptor[] files = (FileDescriptor[]) action.parameters.get(0);
		boolean isFileIdResponse = (Boolean) action.parameters.get(1);
		
		Logger.log("sending announce signal to " + action.peer);
		
		SignalEmitter emitter = getSignalEmitter(action.peer);		
		emitter.getInterface(FileDiscoveryInterface.class).announce(files, isFileIdResponse);		
		return StatusCode.OK;
	}

	/**
	 * sendRequestData()
	 * is called when the Transmit() method encounters REQUEST_DATA action. This function
	 * calls requestData() on the proxy bus object for the specified peer. This function 
	 * will eventually get triggered when requestFile() is called on the FileTransferModule.
	 * 
	 * @param action  specifies the action
	 * @return OK or BAD_FILE_ID
	 * @throws Exception
	 */
	private int sendRequestData(Action action) throws Exception
	{
		byte[] fileId = (byte[]) action.parameters.get(0);
		int startByte = (Integer) action.parameters.get(1);
		int length = (Integer) action.parameters.get(2);
		int maxChunkSize = (Integer) action.parameters.get(3);		
		
		ProxyBusObject proxy = bus.getProxyBusObject(action.peer, FileTransferBusObject.OBJECT_PATH, 
				sessionID, new Class[] { DataTransferInterface.class });
		return proxy.getInterface(DataTransferInterface.class).requestData(fileId, startByte, length, maxChunkSize);
	}

	/**
	 * sendDataChunk()
	 * is called when the Transmit() method encounters DATA_CHUNK action. This function
	 * sends a directed signal with the file chunk to the specified peer. This function 
	 * is triggered when you grant a file request from a peer. All file chunks are sent 
	 * via this signal to session peers.
	 * 
	 * @param action  specifies the action
	 * @return OK
	 * @throws Exception
	 */
	private int sendDataChunk(Action action) throws Exception
	{
		byte[] fileId = (byte[]) action.parameters.get(0);		
		int startByte = (Integer) action.parameters.get(1);
		int chunkLength = (Integer) action.parameters.get(2);
		byte[] chunk = (byte[]) action.parameters.get(3);
		
		SignalEmitter emitter = getSignalEmitter(action.peer);		
		emitter.getInterface(DataTransferInterface.class).dataChunk(fileId, startByte, chunkLength, chunk);		
		return StatusCode.OK;
	}
	
	/**
	 * sendOfferFile()
	 * is called when the Transmit() function encounters an OFFER_FILE action. This function
	 * calls offerFile() on the specified peers proxy bus object to formally send the file
	 * offer. 
	 * 
	 * @param action  specifies the action
	 * @return OK, BAD_FILE_ID, OFFER_REJECTED, or OFFER_TIMEOUT
	 * @throws Exception
	 */
	private int sendOfferFile(Action action) throws Exception
	{
		FileDescriptor file = (FileDescriptor) action.parameters.get(0);
		
		Logger.log("sending offer method to " + action.peer);		
		
		ProxyBusObject proxy = bus.getProxyBusObject(action.peer, FileTransferBusObject.OBJECT_PATH, 
				sessionID, new Class[] { FileDiscoveryInterface.class });
		return proxy.getInterface(FileDiscoveryInterface.class).offerFile(file);
	}	

	/**
	 * sendAnnouncementRequest()
	 * is called when the Transmit() function encounters a REQUEST_ANNOUNCEMENT action. This
	 * function sends an announcement request signal to the specified peer signifying they
	 * want that peer to send them their announced files.
	 * 
	 * @param action  specifies the action
	 * @return OK
	 * @throws Exception
	 */
	private int sendAnnouncementRequest(Action action) throws Exception
	{
		Logger.log("sending announcement request signal");
		
		SignalEmitter emitter = getSignalEmitter(action.peer);
		emitter.getInterface(FileDiscoveryInterface.class).requestAnnouncement();
		return StatusCode.OK;
	}

	/**
	 * sendStopDataXfer()
	 * is called when the file receiver wants to stop a current file transfer. This 
	 * function sends the signal to the specified peer stating that they wish to stop the
	 * current transfer for the corresponding file ID.
	 * 
	 * @param action  specifies the action
	 * @return OK
	 * @throws Exception
	 */
	private int sendStopDataXfer(Action action) throws Exception
	{
		byte[] fileId = (byte[]) action.parameters.get(0);
		
		Logger.log("sending stop data xfer signal");
		
		SignalEmitter emitter = getSignalEmitter(action.peer);
		emitter.getInterface(DataTransferInterface.class).stopDataXfer(fileId);
		return StatusCode.OK;
	}

	/**
	 * sendXferCancelled()
	 * is called when the sender wishes to cancel the file transfer corresponding to the
	 * specified file ID. A signal is sent to the receiver stating that the file transfer
	 * has been cancelled and they should not expect to receive any more data packets.
	 * 
	 * @param action  specifies the action
	 * @return OK
	 * @throws Exception
	 */
	private int sendXferCancelled(Action action) throws Exception
	{
		byte[] fileId = (byte[]) action.parameters.get(0);
		
		Logger.log("sending xfer cancelled signal");
		
		SignalEmitter emitter = getSignalEmitter(action.peer);
		emitter.getInterface(DataTransferInterface.class).dataXferCancelled(fileId);
		return StatusCode.OK;
	}

	/**
	 * sendRequestOffer()
	 * is called when the user wishes to request a file from a specified peer that has not
	 * been explicitly announced or shared. This function will call requestOffer() on the
	 * peers proxy bus object and wait for a response.
	 * 
	 * @param action  specifies the action
	 * @return OK or REQUEST_DENIED
	 * @throws Exception
	 */
	private int sendRequestOffer(Action action) throws Exception
	{
		String filepath = (String) action.parameters.get(0);
		
		Logger.log("sending file id request signal");		
		
		ProxyBusObject proxy = bus.getProxyBusObject(action.peer, FileTransferBusObject.OBJECT_PATH, 
				sessionID, new Class[] { FileDiscoveryInterface.class });
		return proxy.getInterface(FileDiscoveryInterface.class).requestOffer(filepath);
	}
	
	/**
	 * getSignalEmitter()
	 * is called when an AllJoyn signal needs to be constructed and sent. This function will
	 * return a new instance of SignalEmitter. If the the destinationBusId parameter is not
	 * null, the function will return a directed signal emitter so the AllJoyn signal can be
	 * directed at a specific session peer. Otherwise, the function will return a broadcast
	 * signal emitter that will be received by every session peer.
	 * 
	 * @param destinationBusId  specifies the bus id for a given session peer
	 * @return SignalEmitter
	 */
	private SignalEmitter getSignalEmitter(String destinationBusId)
	{
		if (destinationBusId == null)
		{
			//broadcast signal emitter
			return new SignalEmitter(localBusObject, sessionID, SignalEmitter.GlobalBroadcast.On);
		}		
		//directed signal emitter
		return new SignalEmitter(localBusObject, destinationBusId, sessionID, SignalEmitter.GlobalBroadcast.On);
	}
}
