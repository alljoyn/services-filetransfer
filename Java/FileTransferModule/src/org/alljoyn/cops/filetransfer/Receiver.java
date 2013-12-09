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
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.listener.AnnouncementManagerListener;
import org.alljoyn.cops.filetransfer.listener.DirectedAnnouncementManagerListener;
import org.alljoyn.cops.filetransfer.listener.ReceiveManagerListener;
import org.alljoyn.cops.filetransfer.listener.SendManagerListener;
import org.alljoyn.cops.filetransfer.utility.Logger;

/**
 * The receiver is the main handler for AllJoyn signals. Every AllJoyn signal is handled
 * in this class and then calls functions in other classes to initiate any responses that
 * are needed. Some of the main operations include: handling announcements from other peers,
 * handling announcement requests from other peers, processing file chunks, and handling
 * sender and receiver initiated transfer cancellations. The receiver is the driving force
 * behind handling all incoming signals from AllJoyn session peers.
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class.
 */
public class Receiver 
{
	//Load AllJoyn Library
	static 
	{
	    System.loadLibrary("alljoyn_java");
	}	
	
	/** Member Variables **/
	private BusAttachment bus;
	private AnnouncementManagerListener announcementManagerListener;
	private ReceiveManagerListener receiveManagerListener;
	private SendManagerListener sendManagerListener;
	private DirectedAnnouncementManagerListener directedAnnouncementManagerListener;
	private String localBusID;


	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	 /**
	  * Receiver()
	  * constructs an instance of the receiver class and uses the constructor parameters
	  * to register the receiver as the class that provides handles for AllJoyn signals.
	  * 
	  * @param bus  instance of an AllJoyn BusAttachment
	  * @param amListener  instance of AnnouncementManagerListener
	  * @param smListener  instance of SendManagerListener
	  * @param rmListener  instance of ReceiveManagerListener
	  * @param damListener  instance of DirectedAnnouncementManagerListener
	  */
	public Receiver(BusAttachment bus, AnnouncementManagerListener amListener, SendManagerListener smListener, 
			ReceiveManagerListener rmListener, DirectedAnnouncementManagerListener damListener)
	{
		initializeReceiver(bus, amListener, smListener, rmListener, damListener);
	}

	/**
	 * initializeReceiver()
	 * acts as a helper function to initialize the class variables of the Receiver. This 
	 * function is called by both the constructor and the resetState() method. 
	 * 
	 * @param bus  instance of AllJoyn Bus Attachement
	 * @param amListener  instance of AnnoucmentManagerListener
	 * @param smListener  instance of SendManagerListener
	 * @param rmListener  instance of ReceiveManagerListener
	 * @param damListener  instance of DirectedAnnouncementManagerListener
	 */
	private void initializeReceiver(BusAttachment bus,
			AnnouncementManagerListener amListener,
			SendManagerListener smListener, ReceiveManagerListener rmListener,
			DirectedAnnouncementManagerListener damListener)
	{
	    // if the bus object is getting changed, we have to remove ourselves
	    // from the previous bus to prevent signals from coming in on it.
	    if (this.bus != null)
	    {
	        this.bus.unregisterSignalHandlers(this);
	    }
	    
		this.bus = bus;
		this.announcementManagerListener = amListener;
		this.sendManagerListener = smListener;
		this.receiveManagerListener = rmListener;
		this.directedAnnouncementManagerListener = damListener;
		
		if (bus != null)
		{
			localBusID =  bus.getUniqueName();
			
			Status status = bus.registerSignalHandlers(this);
			Logger.log("registering signal handler returned: " + status.toString());
		}
	}
	
	/*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/
	/**
	 * announce()
	 * is triggered when the transmitter sends an announce signal. The fileList parameter denotes
	 * the list of files that are announced by the sender. All of those files are immediately
	 * available for transfer. This function checks to see if the isFileIDResponse parameter is set.
	 * If it is, the DirectedAnnouncementManager is called to process the offer response. Otherwise,
	 * the AnnouncementManager is called to handle the formal file announcement. All files are
	 * eventually recorded by the Permissions Manager.	  			 
	 * 
	 * @param fileList  specifies a list of files available for transfer
	 * @param isFileIDResponse  specifies if the announcement is in response to an offer request
	 */	 
	@BusSignalHandler(iface="org.alljoyn.Cops.FileDiscovery", signal="announce")
	public void announce(FileDescriptor[] fileList, boolean isFileIDResponse)
	{
		String peer = bus.getMessageContext().sender;
		
		Logger.log("received file announcement from: " + peer);
		
		if (!peer.equals(localBusID))
		{
			if (!isFileIDResponse)
			{
				announcementManagerListener.handleAnnounced(fileList, peer);				
			}
			else
			{
				directedAnnouncementManagerListener.handleOfferResponse(fileList, peer);				
			}				
		}		
	}
	
	/**
	 * requestAnnouncement()
	 * is triggered when a session peer requests that you send them an announcement of all the files
	 * that you have made available. This handler calls handleAnnouncementRequest() on the Announcement
	 * ManagerListeber which will insert an announce action into the dispatcher.	  			 
	 */	
	@BusSignalHandler(iface="org.alljoyn.Cops.FileDiscovery", signal="requestAnnouncement")
	public void requestAnnouncement() 
	{
		String peer = bus.getMessageContext().sender;
		
		Logger.log("got announce request from: " + peer);
		
		if (!peer.equals(localBusID))
		{
			announcementManagerListener.handleAnnouncementRequest(peer);
		}		
	}
	
	
	/**
	 * dataChunk()
	 * is triggered when you are receive a chunk of a file from a session peer. This is usually in
	 * response to a file request you originally made. The file chunk is then passed off to the
	 * ReceiveManager to be merged into the temporary file stored in memory.	  			 
	 * 
	 * @param fileID  specifies the file ID of the file the data belongs to
	 * @param startByte  specifies the starting byte of the chunk relative to the file
	 * @param chunkLength  specifies the length of data chunk
	 * @param chunk  specifies the file data chunk
	 */
	@BusSignalHandler(iface="org.alljoyn.Cops.DataTransfer", signal="dataChunk")
	public void dataChunk(byte[] fileID, int startByte,	int chunkLength, byte[] chunk)
	{
		String peer = bus.getMessageContext().sender;
		
		if (!peer.equals(localBusID))
		{
			receiveManagerListener.handleFileChunk(fileID, startByte, chunkLength, chunk);
		}		
	}	
	
	/**
	 * stopDataXfer()
	 * is triggered when the file transfer receiver wishes to pause or cancel the current file
	 * transfer. This function immediately notifies the SendManager to stop transfer of the file
	 * matching the file ID.	  			 
	 *  
	 * @param fileID  specifies the ID of file being transferred
	 */
	@BusSignalHandler(iface="org.alljoyn.Cops.DataTransfer", signal="stopDataXfer")
	public void stopDataXfer(byte[] fileID)
	{
		String peer = bus.getMessageContext().sender;
		
		Logger.log("got stop data xfer from: " + peer);
		
		if (!peer.equals(localBusID))
		{
			sendManagerListener.handleStopDataXfer(fileID, peer);
		}		
	}
	
	/**
	 * dataXferCancelled()
	 * is triggered when the sender wishes to cancel file transfer of the file matching the fileId.
	 * This signal is sent from the sender to the receiver notifying them the current file 
	 * transfer has been cancelled.
	 *  
	 * @param fileID  specifies the ID of file being transferred
	 */
	@BusSignalHandler(iface="org.alljoyn.Cops.DataTransfer", signal="dataXferCancelled")
	public void dataXferCancelled(byte[] fileID)
	{
		String peer = bus.getMessageContext().sender;
		
		Logger.log("got data xfer cancelled from: " + peer);
		
		if (!peer.equals(localBusID))
		{
			receiveManagerListener.handleDataXferCancelled(fileID, peer);
		}		
	}

	/**
	 * resetState()
	 * is called by the File Transfer Module when specifies a new AllJoyn session to be used.
	 * This function is passed bus attachment and new instances of the callback classes that are
	 * needed and will reinitialize the necessary class variables.
	 * <p>
	 * Note: in the case where the user calls uninitialize() on the FTC, the busAttachement parameter
	 * will be null.
	 * 
	 * @param busAttachment  instance of the AllJoyn Bus Attachment, can be null.
	 * @param amListener  instance of AnnoucmentManagerListener
	 * @param smListener  instance of SendManagerListener
	 * @param rmListener  instance of ReceiveManagerListener
	 * @param damListener  instance of DirectedAnnouncementManagerListener
	 */
	public void resetState(BusAttachment busAttachment,
			AnnouncementManagerListener amListener, SendManagerListener smListener,
			ReceiveManagerListener rmListener,
			DirectedAnnouncementManagerListener damListener)
	{
		initializeReceiver(busAttachment, amListener, smListener, rmListener, damListener);			
	}
}
