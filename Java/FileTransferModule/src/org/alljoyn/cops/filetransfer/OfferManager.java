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

import java.util.ArrayList;
import java.util.Arrays;

import org.alljoyn.cops.filetransfer.data.Action;
import org.alljoyn.cops.filetransfer.data.Action.ActionType;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.OfferManagerListener;
import org.alljoyn.cops.filetransfer.listener.OfferReceivedListener;
import org.alljoyn.cops.filetransfer.listener.ReceiveManagerListener;
import org.alljoyn.cops.filetransfer.listener.SendManagerListener;

/**
 * The Offer Manager is the main driving force behind offering files to
 * and responding to offers made by remote session peers. When an offer 
 * is sent to a remote session peer the Offer Manager will wait until the 
 * offer is either accepted, rejected, or timed out. If the offer is 
 * accepted the Offer Manager will notify the Send Manager to immediately
 * begin transferring the file to the remote session peer. Conversely, if
 * an offer is received that the user wishes to accept, the Offer Manager
 * will notify the Receive Manager to immediately request the file from the
 * remote session peer. 
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class.
 */
public class OfferManager implements OfferManagerListener
{
	/** Class Constant **/
	private static final int DEFAULT_TIMEOUT_MILLIS = 5000;
	
	/** Member Variables **/
	private Dispatcher dispatcher;
	private FileSystemAbstraction fsa;	
	private PermissionsManager permissionsManager;
	private String localBusID;	
	private boolean isOfferPending;
	private FileDescriptor offeredFileDescriptor;
	private OfferReceivedListener offerReceivedListener;
	private SendManagerListener sendManagerListener;	
	private ReceiveManagerListener receiveManagerListener;
    private Object offeredFileDescriptorLock;
    private Object offerListenerLock;
	
    /*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * OfferManager()
	 * constructs an instance of the OfferManager class and uses the constructor parameters
	 * to initialize the member variables.
	 * 
	 * @param dispatcher  instance of Dispatcher 
	 * @param localBusID  specifies the bus ID for the bus attachment passed in by the user
	 */
	public OfferManager(Dispatcher dispatcher, PermissionsManager permissionsManager, String localBusID)
	{
		this(dispatcher, localBusID, permissionsManager, 
				FileSystemAbstraction.getInstance());
	}
	
	/**
	 * OfferManager()
	 * constructs an instance of the OfferManager class and uses the constructor parameters
	 * to initialize the member variables.
	 * <p>
	 * Note: this secondary constructor is used for testing purposes only when we need to specify
	 * a separate instance of the PermissionsManager and FileSystemAbstraction.
	 * 
	 * @param dispatcher  instance of Dispatcher
	 * @param localBusID  specifies the bus ID for the bus attachment passed in by the user
	 * @param pm  instance of the Permissions Manager class
	 * @param fsa  instance of the File System Abstraction class
	 */ 
	public OfferManager(Dispatcher dispatcher, String localBusID, PermissionsManager pm, 
			FileSystemAbstraction fsa)
	{
		this.dispatcher = dispatcher;
		this.localBusID = localBusID;
		this.fsa = fsa;
		this.permissionsManager = pm;
		
		this.isOfferPending = false;
		this.offeredFileDescriptor = null;
		
        this.offeredFileDescriptorLock = new Object();
		this.offerListenerLock = new Object();
	}
	
	/**
	 * resetState()
	 * is called by the File Transfer Module when specifies a new AllJoyn session to be used.
	 * This function is passed the new bus ID of the bus attachment. 
	 * <p>
	 * Note: in the case where the user calls uninitialize() on the FTC, the localBusID parameter
	 * will be null.
	 * 
	 * @param localBusID  specifies the bus ID of the bus attachment, can be null.
	 */
	public void resetState(String localBusID)
	{
		this.localBusID = localBusID;
	}
	
	/**
	 * offerFile()
	 * takes the file at the specified path and offers it to the specified peer. Since this function
	 * maps to an AllJoyn method call we have to wait for a response. If the timeout parameter is zero,
	 * we will wait a default of 5 seconds for a response. Otherwise, we will wait the specified amount
	 * of time. This function will return OK if the peer accepts your file offer, OFFER_REJECTED if the
	 * peer rejects your offer, OFFER_TIMEOUT if the timeout interval is exceeded before response, or
	 * BAD_FILE_PATH if the specified path is invalid.
	 * 
	 * @param peer  specifies the peer you want to offer the file to
	 * @param path  specifies the absolute path to the file being offered
	 * @param timeout  specifies the amount of time, in milliseconds, to wait for a response
	 * @return OK, OFFER_REJECTED, OFFER_TIMEOUT, or BAD_FILE_PATH
	 */
	public int offerFile(String peer, String path, int timeout)
	{
	    int offeredFileStatus = StatusCode.BAD_FILE_PATH;
        FileDescriptor fileDescriptor = checkAnnouncedFileList(path);		
       
		if (fileDescriptor == null)
		{
		    fileDescriptor = checkSharedFileList(path);
		}
		
		if (fileDescriptor == null)
		{
			ArrayList<String> paths = new ArrayList<String>();
			paths.add(path);
			
			ArrayList<String> failedPaths = new ArrayList<String>();			
			
			FileDescriptor[] descriptorArray = fsa.getFileInfo(paths, failedPaths, localBusID);
			
			if (failedPaths.size() == 0)
			{
                fileDescriptor = descriptorArray[0];
                permissionsManager.addOfferedLocalFile(fileDescriptor);
			}
		}
		
		if (fileDescriptor != null)
		{
            try
            {
                setMemberVariables(fileDescriptor);
                
                int response = transmitOfferFileAction(fileDescriptor, peer);
                
    		    if (response == StatusCode.OFFER_ACCEPTED)
    		    {
                    if (timeout < 0)
                    {
                        timeout = DEFAULT_TIMEOUT_MILLIS;
                    }
                    
                    if ( timeout > 0)
                    {
                        long startTime = System.currentTimeMillis();
    
                        synchronized (offeredFileDescriptorLock)
                        {
                            while ((isOfferPending) && (timeout > 0))
                            {
                            	offeredFileDescriptorLock.wait(timeout);
                                timeout = timeout - (int)(System.currentTimeMillis() - startTime);
                            }
                        }
                       
                        if (timeout > 0)
                        {
                            offeredFileStatus = StatusCode.OK;
                        }
                        else
                        {
                            offeredFileStatus = StatusCode.OFFER_TIMEOUT;
                        }
                    }
                    else
                    {
                        offeredFileStatus = StatusCode.OK;
                    }
    		    }
    		    else
    		    {
    		        offeredFileStatus = response;
    		    }
            }
            catch (InterruptedException e)
            {
            	offeredFileStatus = StatusCode.OFFER_TIMEOUT;
                Thread.currentThread().interrupt();
            }
            finally
            {
                resetMemberVariables();
            }
		}
		
		return offeredFileStatus;
	}
	
	/**
	 * checkAnnouncedFileList()
	 * is a private function that is used by offerFile() to determine if the path you specified
	 * matches a file that has already been announced. If so, there is no additional work is 
	 * needed and we can send the existing file descriptor in the offer. Otherwise, we have to
	 * create a file descriptor for the new file.
	 * 
	 * @param path  specifies the path to file being offered
	 * @return FileDescriptor, null otherwise
	 */
    private FileDescriptor checkAnnouncedFileList(String path)
    {
        return checkFileList(path, permissionsManager.getAnnouncedLocalFiles());
    }
    
    /**
     * checkSharedFileList()
     * is a private function that is used by offerFile() to determine if the path you specified
     * matches a file that has already been shared. If so, there is no additional work is 
     * needed and we can send the existing file descriptor in the offer. Otherwise, we have to
     * create a file descriptor for the new file.
     * 
     * @param path  specifies the path to file being offered
     * @return FileDescriptor, null otherwise
     */
    private FileDescriptor checkSharedFileList(String path)
    {
        return checkFileList(path, permissionsManager.getOfferedLocalFiles());
    }
    
    /**
     * checkFileList()
     * is a private function that is used by checkAnnouncedFileList() and checkSharedFileList()
     * to search the specified list of file descriptors to see if any match the specified path.
     * If there is a match, it is returned, otherwise null is returned.
     * 
     * @param path  specifies the path to file being checked
     * @param fileList  specifies the list in which to search for the file
     * @return FileDescriptor, null otherwise
     */
    private FileDescriptor checkFileList(String path, ArrayList<FileDescriptor> fileList)
    {
		FileDescriptor offeredDescriptor = null;
		
		if (fileList == null)
		{
			return null;
		}

		for (FileDescriptor fileDescriptor : fileList)
		{
			String filePath = fsa.buildPathFromDescriptor(fileDescriptor);

			if (filePath.equals(path))
			{
				offeredDescriptor = fileDescriptor;
			}
		}
		
		return offeredDescriptor;
	}
	
	/**
	 * transmitOfferFileAction()
	 * is a private function that is used by offerFile() to build the OFFER_FILE action
	 * to the specified peer and wait for a response. If the response is OFFER_REJECTED,
	 * a member variable is set to immediately force the loop inside waitForResponse() to
	 * stop executing. 
	 * 
	 * @param fileDescriptor  specifies the file descriptor for the file offered
	 * @param peer  specifies the peer to send the offer
	 */
	private int transmitOfferFileAction(FileDescriptor fd, String peer)
	{
		Action action = new Action();
		action.actionType = ActionType.OFFER_FILE;
		action.parameters.add(fd);
		action.peer = peer;		
					
		return dispatcher.transmitImmediately(action);
	}
	
	/**
     * resetMemberVariables()
     * is a private function which resets member variables that deal with pending offers. This
     * function executes after every file offer sequence is complete.
     */ 
    private void resetMemberVariables()
    {
        setMemberVariables(null);
    }

    /**
     * setMemberVariables()
     * is a private function which sets member variables that deal with pending offers. This
     * function executes after every file offer sequence is complete.
     * 
     * @param fileDescriptor  specifies the descriptor of the offered file
     */ 
    private void setMemberVariables(FileDescriptor fileDescriptor)
    {
        synchronized(offeredFileDescriptorLock)
        {
            isOfferPending = (fileDescriptor != null);
            offeredFileDescriptor = fileDescriptor;
        }
    }
    
    /**
     * isOfferPending()
     * is a callback function invoked by the File Transfer Bus Object to see if the file request
     * that was just received matches a pending offer. If the specified file ID matches a 
     * pending offer, this function will return true. Otherwise, this function will return
     * false.
     * 
     * @param fileID  specifies the ID of the file just requested
     * @return boolean
     */
    @Override
    public boolean isOfferPending(byte[] fileID)
    {
    	return isOfferPending && Arrays.equals(fileID, offeredFileDescriptor.fileID);
    }

    /**
     * handleFileRequest()
     * is a callback function invoked by the File Transfer Bus Object when a file request is
     * received that matches a pending offer. This function will reset some internal variables
     * regarding offered files. The function then notifies the Send Manager to start sending
     * the file to remote peer.
     * 
     * @param fileID  specifies the fileId of the file being requested
	 * @param startByte  specifies the starting position within the file (usually zero)
	 * @param length  specifies the number of bytes to be sent (usually the length of the file)
	 * @param peer  specifies the intended recipient of the file
	 * @param maxChunkLength  specifies the maximum chunk size
     * @return OK or BAD_FILE_ID
     */
    @Override
	public int handleFileRequest(byte[] fileID, int startByte, int length,
			String peer, int maxChunkLength)
	{
		synchronized(offeredFileDescriptorLock)
	    {
	        if (isOfferPending && Arrays.equals(fileID, offeredFileDescriptor.fileID))
			{
	            resetMemberVariables();
	            offeredFileDescriptorLock.notifyAll();
			}
	    }
	    
	    if (sendManagerListener == null)
	    {
	    	return StatusCode.REQUEST_DENIED;
	    }
		return sendManagerListener.sendFile(fileID, startByte, length, peer, maxChunkLength);
	}
	
	/** 
	 * handleOffer()
	 * is called when a file offer is received from a remote session peer. The function first
	 * checks to see if the offerReceivedListener is registered. If it isn't, the file offer is
	 * immediately rejected. If the listener is registered, the listener is called to see if
	 * the user will accept the offer. If the offer is accepted, return OFFER_ACCEPTED. 
	 * Otherwise, return OFFER_REJECTED.	  			 
	 * 
	 * @param file  specifies the descriptor of offered file
	 * @param peer  specifies the peer offering the file
	 * @return OFFER_ACCEPTED or OFFER_REJECTED
	 */
    @Override
	public int handleOffer(FileDescriptor file, String peer)
	{
		boolean acceptOffer;
		
		synchronized (offerListenerLock)
		{
			if (offerReceivedListener == null)
			{
				return StatusCode.OFFER_REJECTED;			
			}
			
			acceptOffer = offerReceivedListener.acceptOfferedFile(file, peer);
		}		
		
		if (acceptOffer)
		{
			if (receiveManagerListener != null)
			{
				receiveManagerListener.requestFile(file, file.filename, null, true);
			}			
			return StatusCode.OFFER_ACCEPTED;
		}
		return StatusCode.OFFER_REJECTED;
	}
	
	/** 
	 * setOfferReceivedListener()
	 * registers the {@link OfferReceivedListener} listener that will be called whenever a 
	 * session peer sends a file offer to you. Registering the listener allows the user to 
	 * either accept or deny any pending offers. If this listener is not registered the default
	 * behavior is to reject all file offers from session peers.			 
	 * 
	 * @param listener  instance of OfferReceivedListener
	 */
	public void setOfferReceivedListener(OfferReceivedListener listener)
	{
		synchronized (offerListenerLock)
		{
			offerReceivedListener = listener;
		}		
	}
	
	/**
	 * setSendManagerListener()
	 * registers the {@link SendManagerListener} that allows the OfferManager to callback to
	 * the SendManager to start transferring the requested file. 
	 * 
	 * @param listener  instance of SendManagerListener
	 */
	public void setSendManagerListener(SendManagerListener listener)
	{
		sendManagerListener = listener;
	}
	
	/**
	 * setReceiveManagerListener()
	 * registers the {@link ReceiveManagerListener} that allows the OfferManager to callback to
	 * the ReceiveManager to immediately request the file that has just been offered to them.
	 * 
	 * @param listener  instance of ReceiveManagerListener
	 */
	public void setReceiveManagerListener(ReceiveManagerListener listener)
	{
		receiveManagerListener = listener;
	}
}
