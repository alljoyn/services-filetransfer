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

package org.alljoyn.cops.filetransfer.alljoyn;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.DirectedAnnouncementManagerListener;
import org.alljoyn.cops.filetransfer.listener.OfferManagerListener;
import org.alljoyn.cops.filetransfer.listener.SendManagerListener;
import org.alljoyn.cops.filetransfer.utility.Logger;

/**
 * The File Transfer Bus Object is registered with the AllJoyn Bus Attachment and exposes
 * the Data Transfer and File Discovery interfaces to remote session peers. This object
 * listens and responds to remote method calls (not to be confused with signals) made by
 * AllJoyn session peers. Methods are used when a response is needed quickly since signals
 * are too slow. The three methods handled by the bus object are: requestData, requestOffer,
 * and offerFile.
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
 */
public class FileTransferBusObject implements DataTransferInterface, FileDiscoveryInterface, BusObject
{
	/** Object Path - used by AllJoyn to find the correct Bus Object **/
	public static final String OBJECT_PATH = "/filetransfer";
	
	/** Member Variables **/
	private BusAttachment bus;
	private SendManagerListener sendManagerListener;
	private DirectedAnnouncementManagerListener directedAnnouncementManagerListener;
	private OfferManagerListener offerManagerListener;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/	
	/**
	 * FileTransferBusObject()	  
	 * creates an instance of the FileTransferBusObject class 
	 * 	
	 * @param bus  instance of an AllJoyn BusAttachment	  
	 */
	public FileTransferBusObject(BusAttachment bus)
	{
		super();
		this.bus = bus;
	}
	
	/*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/	
	/**
	 * requestData()
	 * is triggered by AllJoyn when the requestData() method is called by the Transmitter.
	 * This function first checks with the Offer Manager via the OfferManagerListener, to see 
	 * if the file request matches any outstanding file offers. If so, the OfferManager
	 * is invoked to handle the file request. Otherwise, this function will invoke the
	 * SendManagerListener to notify the SendManager to handle the pending file request.	  			 
	 *  
	 * @param fileID  file ID of the file being requested
	 * @param startByte  starting byte of the request relative to the file
	 * @param length  length of request in bytes
	 * @param maxChunkLength  specifies the max chunk size
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	public int requestData(byte[] fileID, int startByte, int length, 
			int maxChunkLength) throws BusException
	{		
		String peer = bus.getMessageContext().sender;
		
		Logger.log("got file request from: " + peer + " for " + length + " bytes");
		
		if (offerManagerListener != null && offerManagerListener.isOfferPending(fileID))
		{
			return offerManagerListener.handleFileRequest(fileID, startByte, length, peer, maxChunkLength);
		}
		else if (sendManagerListener != null)
		{
			return sendManagerListener.sendFile(fileID, startByte, length, peer, maxChunkLength);
		}
		return StatusCode.FILE_NOT_BEING_TRANSFERRED;
	}
	
	/**
	 * requestOffer()
	 * is triggered by AllJoyn when the requestOffer() method is called by the Transmitter
	 * This function tells the DirectedAnnouncementManager to handle the offer request. 
	 * If the Directed Announcement Listener is not set, this function will automatically
	 * deny the offer request.	  			
	 *  
	 * @param filepath  absolute path of file
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	public int requestOffer(String filepath) throws BusException
	{
		String peer = bus.getMessageContext().sender;
		
		Logger.log("got file id request from " + peer);
		
		if (directedAnnouncementManagerListener != null)
		{
			return directedAnnouncementManagerListener.handleOfferRequest(filepath, peer);
		}
		return StatusCode.REQUEST_DENIED;
	}
	
	/**
	 * offerFile()
	 * is triggered by AllJoyn when the offerFile() method is called by the Transmitter	  			 
	 * This function notifies the Offer Manager via the Offer Listener to handle the
	 * file offer received from a remote peer.
	 * 
	 * @param file  file descriptor of offered file
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	public int offerFile(FileDescriptor file) throws BusException
	{
		String peer = bus.getMessageContext().sender;
		
		Logger.log("got offer from: " + peer);
		
		if (offerManagerListener != null)
		{
			return offerManagerListener.handleOffer(file, peer);					
		}
		return StatusCode.OFFER_REJECTED;
	}
	
	/**
	 * requestAnnouncement()
	 * is an AllJoyn signal. See Receiver for implementation	  			 
	 * 
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	public void requestAnnouncement() throws BusException
	{
		// intentionally left blank		
	}

	/**
	 * announce()	  
	 * is an AllJoyn signal. See Receiver for implementation	  			 
	 * 
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	public void announce(FileDescriptor[] fileList, boolean isFileIDResponse) throws BusException
	{
		// intentionally left blank		
	}
	
	/**
	 * dataChunk()
	 * is an AllJoyn signal. See Receiver for implementation
	 * 	  			 
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	public void dataChunk(byte[] fileID, int startByte,	int chunkLength,
			byte[] chunk) throws BusException
	{
		// intentionally left blank			
	}

	/**
	 * offerRejected()
	 * is an AllJoyn signal. See Receiver for implementation
	 * 	  			 
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	public void offerRejected(FileDescriptor file) throws BusException
	{
		// intentionally left blank			
	}

	/**
	 * stopDataXfer()
	 * is an AllJoyn signal. See Receiver for implementation	  			 
	 * 
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	public void stopDataXfer(byte[] fileID) throws BusException
	{
		// intentionally left blank		
	}

	/**
	 * dataXferCancelled()
	 * is an AllJoyn signal. See Receiver for implementation	  	
	 * 		 
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	public void dataXferCancelled(byte[] fileID) throws BusException
	{
		// intentionally left blank			
	}
	
	/**
	 * setSendManagerListener()
	 * registers the SendManagerListener and allows the bus object to callback
	 * to the Send Manager and initiate the file transfer in response to a
	 * file request.
	 * 
	 * @param listener  instance of the SendManagerListener
	 */
	public void setSendManagerListener(SendManagerListener listener)
	{
		this.sendManagerListener = listener;
	}
	
	/**
	 * setDirectedAnnouncementManagerListener()
	 * registers the DirectedAnnouncementManagerListener and allows the bus object
	 * to callback to the Directed Announcement Manager when a remote peer
	 * makes a request for an unannounced file.
	 * 
	 * @param listener  instance of the DirectedAnnouncementListener
	 */
	public void setDirectedAnnouncementManagerListener(DirectedAnnouncementManagerListener listener)
	{
		this.directedAnnouncementManagerListener = listener;
	}
	
	/**
	 * setOfferManagerListener()
	 * registers the OfferManagerListener and allows the bus object to callback to the
	 * OfferManager when a remote peer offers us a file.
	 * 
	 * @param listener  instance of the OfferManagerListener
	 */
	public void setOfferManagerListener(OfferManagerListener listener)
	{
		this.offerManagerListener = listener;
	}
}	
