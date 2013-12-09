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

import java.util.concurrent.LinkedBlockingQueue;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.cops.filetransfer.alljoyn.FileTransferBusObject;
import org.alljoyn.cops.filetransfer.data.Action;
import org.alljoyn.cops.filetransfer.data.Action.ActionType;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.DirectedAnnouncementManagerListener;
import org.alljoyn.cops.filetransfer.listener.SendManagerListener;
import org.alljoyn.cops.filetransfer.utility.Logger;

/**
 * The Dispatcher is one of the main modules inside the File Transfer Module. The dispatchers main
 * job is to run in a background thread and provide a service queue to process all of the actions. Each
 * action will usually correspond to an AllJoyn signal or method call. The dispatcher ensures that all
 * actions are serviced in the order received and, since it is running in a background thread, does not
 * block or inhibit the application in any way. Additionally, the Dispatcher provides a method that
 * bypasses the Dispatcher queue so alljoyn method calls can be transmitted immediately. This is only
 * done for alljoyn method calls and not for alljoyn signals.
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class.
 */
public class Dispatcher implements Runnable
{
	/** Member Variables **/
	private LinkedBlockingQueue<Action> dispatcherQueue;
	private Transmitter transmitter;	
	private SendManagerListener sendManagerListener;
	private DirectedAnnouncementManagerListener directedAnnouncementManagerListener;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * Dispatcher()
	 * constructs an instance of the Dispatcher class and uses the provided parameters
	 * to initialize class member variables.
	 * 
	 * @param fileTransferBusObject  instance of alljoyn bus object
	 * @param busAttachment  instance of alljoyn bus attachment
	 * @param sessionID  specifies the ID of the alljoyn session
	 */
	public Dispatcher(FileTransferBusObject fileTransferBusObject, BusAttachment busAttachment, int sessionID)
	{		
		this(new Transmitter(fileTransferBusObject, busAttachment, sessionID));
	}
	
	/**
	 * Dispatcher()
	 * constructs an instance of the Dispatcher class and uses the provided Transmitter
	 * parameter to allow each action to be passed down to transmitter for servicing.
	 * <p>
	 * Note: this secondary constructor is used for testing purposes only when we need to specify
	 * a separate instance of the Transmitter.
	 * 
	 * @param transmitter  instance of the Transmitter Module
	 */
	public Dispatcher(Transmitter transmitter)
	{
		this.dispatcherQueue = new LinkedBlockingQueue<Action>();
		this.transmitter = transmitter;
	}
	
	/*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/
	/**
	 * run()
	 * is the implementation of the Runnable interface and will execute in a separate
	 * background thread to ensure that all actions are serviced outside of the application
	 * thread. This function runs in an infinite loop and waits until an action is inserted
	 * into the queue. When an action is inserted the appropriate action is taken based on
	 * the action type. Most actions are simply passed to the transmitter for servicing 
	 * except three. The FILE_ID_RESPONSE action calls back to the DirectedAnnouncement
	 * Manager so a file descriptor can be generated and sent as an announcement. And the 
	 * DATA_CHUNK action calls back to the SendManager so the next chunk of the file 
	 * can be inserted for transmission. And lastly, the SHUTDOWN_THREAD action is used
	 * only for unit testing and provides a cleanup mechanism to ensure all dispatcher
	 * threads can be terminated.
	 */
	@Override
	public void run()
	{
	    boolean isRunning = true;
	    
		while (isRunning)
		{
	        try
	        {
				Action action = dispatcherQueue.take();
				
				if (action.actionType == ActionType.FILE_ID_RESPONSE)
				{
					directedAnnouncementManagerListener.generateFileDescriptor(action);
				}
				else if (action.actionType == ActionType.SHUTDOWN_THREAD)
				{
				    isRunning = false;
				}
				else
				{
					transmitter.transmit(action);

					if (action.actionType == ActionType.DATA_CHUNK)
					{
						sendManagerListener.dataSent();
					}
				}
	        }
	        catch (Exception ex)
	        {
	            Logger.log(ex.toString());          
	        }
		}
	}
	
	/**
	 * insertAction()
	 * simply inserts the action parameter into the Dispatcher queue for processing.
	 * 
	 * @param action  specifies the action to be inserted in the queue
	 */
	public void insertAction(Action action)
	{
		dispatcherQueue.add(action);
	}
	
	/**
	 * setSendManagerListener()
	 * registers the {@link SendManagerListener} that allows the Dispatcher to call back
	 * to the Send Manager to insert the next data chunk for the file being transferred. This is
	 * done automatically in the constructor of the FileTransferModule.
	 * 
	 * @param listener  instance of DataSentListener
	 */
	public void setSendManagerListener(SendManagerListener listener)
	{
		sendManagerListener = listener;
	}
	
	/**
	 * setDirectedAnnouncementManagerListener()
	 * registers the {@link DirectedAnnouncementManagerListener} that allows the Dispatcher to call  
	 * back to the DirectedAnnouncementManager when a FILE_ID_RESPONSE action is encountered. This is
	 * done automatically in the constructor of the FileTransferModule.
	 * 
	 * @param listener  instance of GenerateIDListener
	 */
	public void setDirectedAnnouncementManagerListener(DirectedAnnouncementManagerListener listener)
	{
		directedAnnouncementManagerListener = listener;
	}
	
	
	
	/**
	 * transmitImmediately()
	 * bypasses the Dispatcher queue and sends the action over the wire immediately. Used
	 * for high priority messages (i.e. alljoyn method calls). 
	 * 
	 * @param action  the action to be transmitted
	 * @return  INVALID if transmit failed, StatusCode of completed action otherwise
	 */
	public int transmitImmediately(Action action)
	{
		try
		{
			return transmitter.transmit(action);
		} 
		catch (Exception e)
		{			
			Logger.log(e.toString());
			return StatusCode.INVALID;
		}
	}

	/**
	 * resetState()
	 * is called by the File Transfer Module when specifies a new AllJoyn session to be used.
	 * This function is passed the bus attachement, new AllJoyn bus object, and bus ID of the bus
	 * attachment. 
	 * <p>
	 * Note: in the case where the user calls uninitialize() on the FTC, the localBusID and bus
	 * attachment parameters will be null.
	 * 
	 * @param busObject  instance of File Transfer Bus Object
	 * @param busAttachment  instance of AllJoyn Bus Attachment, can be null
	 * @param sessionID  specifies the bus ID of the bus attachment, can be null
	 */
	public void resetState(FileTransferBusObject busObject,
			BusAttachment busAttachment, int sessionID)
	{
		transmitter = new Transmitter(busObject, busAttachment, sessionID);		
	}
}
