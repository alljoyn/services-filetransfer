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
import org.alljoyn.cops.filetransfer.data.Action;
import org.alljoyn.cops.filetransfer.data.Action.ActionType;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.DirectedAnnouncementManagerListener;
import org.alljoyn.cops.filetransfer.listener.FileAnnouncementReceivedListener;
import org.alljoyn.cops.filetransfer.listener.UnannouncedFileRequestListener;

/**
 * The DirectedAnnouncementManager is responsible for handling the events
 * associated with requesting unannounced files from remote session peers.
 * From the requesters perspective, the DirectedAnnouncementManager is
 * responsible for initiating requests and handling the responses to 
 * unannounced file requests with remote session peers. The DirectedAnnouncement
 * Manager is also responsible for responding to the requests made by remote
 * session peers for unannounced files. The default behavior is to automatically
 * deny any and all requests for unannounced files. To enable this behavior, the
 * user must register the {@link UnannouncedFileRequestListener}.
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class. 
 */
public class DirectedAnnouncementManager implements DirectedAnnouncementManagerListener
{
	/** Member Variables **/	
	private Dispatcher dispatcher;
	private PermissionsManager permissionsManager;
	private FileSystemAbstraction fsa;	
	private Boolean showRelativePath;
	private Boolean showSharedPath;
	private String localBusID;	
	private UnannouncedFileRequestListener unannouncedFileRequestListener;
	private FileAnnouncementReceivedListener fileAnnouncementReceivedListener;	
	private final Object unannouncedFileRequestListenerLock;
	private final Object fileAnnouncementReceivedListenerLock;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * DirectedAnnouncementManager()
	 * constructs an instance of the DirectedAnnouncementManager class and uses the constructor
	 * parameters to initialize the member variables.
	 * 
	 * @param dispatcher  instance of Dispatcher 
	 * @param localBusID  specifies the bus ID for the bus attachment passed in by the user
	 */
	public DirectedAnnouncementManager(Dispatcher dispatcher, PermissionsManager permissionsManager, String localBusID)
	{
		this(dispatcher, localBusID, FileSystemAbstraction.getInstance(),
				permissionsManager);
	}
	
	/**
	 * DirectedAnnouncementManager()
	 * constructs an instance of the DirectedAnnouncementManager class and uses the constructor 
	 * parameters to initialize the member variables.
	 * <p>
	 * Note: this secondary constructor is used for testing purposes only when we need to specify
	 * a separate instance of the PermissionsManager and FileSystemAbstraction.
	 * 
	 * @param dispatcher  instance of Dispatcher Module
	 * @param localBusID  specifies the bus ID for the bus attachment passed in by the user
	 * @param fsa  instance of FileSystemAbstraction Module
	 * @param pm  instance of PermissionsManager Module
	 */
	public DirectedAnnouncementManager(Dispatcher dispatcher, String localBusID, 
			FileSystemAbstraction fsa, PermissionsManager pm)
	{
		this.dispatcher = dispatcher;
		this.permissionsManager = pm;
		this.fsa = fsa;		
		this.showRelativePath = true;
		this.showSharedPath = false;
		this.localBusID = localBusID;		
		this.unannouncedFileRequestListenerLock = new Object();
		this.fileAnnouncementReceivedListenerLock = new Object();
	}
	
	/**
	 * requestOffer()
	 * is called when the user wishes to request a file from the specified peer that has
	 * not been explicitly announced. This function will create a REQUEST_OFFER action that
	 * will ask the remote peer if they will transfer the file with the specified path.
	 * If the remote peer agrees, they will return a status code of OK and the file will
	 * be transferred. If the remote peer rejects the request, they will return a status
	 * code of REQUEST_DENIED.
	 * <p>
	 * Note: the default behavior is to deny requests for files that have not been
	 * announced or shared. This behavior can only be changed by registering the
	 * {@link UnannouncedFileRequestListener}
	 * 
	 * @param peer  specifies the peer in which to send the file ID request
	 * @param filePath  specifies the absolute path to the file being requested
	 * @return OK or REQUEST_DENIED
	 */
	public int requestOffer(String peer, String filePath)
	{
		Action action = new Action();
		action.actionType = ActionType.REQUEST_OFFER;
		action.parameters.add(filePath);
		action.peer = peer;			

		return dispatcher.transmitImmediately(action);
	}
	
	/**
	 * handleOfferRequest()
	 * is called when a remote peer is attempting to request a file that has not been announced
	 * or shared. This function will first check to see if the specified filePath matches a
	 * file they has already been announced or shared. If a match is found, an announcement signal
	 * containing a single file descriptor will be sent to the remote peer and the function 
	 * will return OK. If a match is not found, the function will invoke the Unannounced File
	 * Request Listener to see if the user will accept the request. If the request is granted,
	 * the function will return OK. Otherwise, the function will return REQUEST_DENIED.
	 * <p>
	 * Note: if the {@link UnannouncedFileRequestListener} is not registered all file ID requests will be
	 * denied by default. 
	 *          
	 * @param filePath  the absolute path of the file 
	 * @param peer  specifies the peer who sent the file ID request 
	 * @return OK or REQUEST_DENIED
	 */
	@Override
	public int handleOfferRequest(String filePath, String peer)
	{
		FileDescriptor descriptor = checkAnnouncedAndSharedFileList(filePath);
		
		if (descriptor != null)
		{
			insertSingleDescriptorAnnouncement(descriptor, peer);
			return StatusCode.OK;
		}
		else
		{
			synchronized(unannouncedFileRequestListenerLock)
			{
				if (unannouncedFileRequestListener != null)
				{
					if (unannouncedFileRequestListener.allowUnannouncedFileRequests(filePath))
					{
						insertFileIDResponseAction(filePath, peer);
						return StatusCode.OK;
					}
					else
					{
						return StatusCode.REQUEST_DENIED;
					}
				}
				else 
				{
					return StatusCode.REQUEST_DENIED;
				}
			}
		}
	}
	
	/**
	 * checkAnnouncedAndSharedFileList()
	 * is a private method called by handleOfferRequest() to traverse the announced and shared
	 * file lists to find a file that matches the specified path. If a match is found, the method
	 * returns the file descriptor. Otherwise, the function returns null.
	 *          
	 * @param path  specifies the absolute path of the file  
	 * @return file descriptor or null
	 */
	private FileDescriptor checkAnnouncedAndSharedFileList(String path)
	{
		ArrayList<FileDescriptor> announcedFiles = permissionsManager.getAnnouncedLocalFiles();

		for (FileDescriptor fileDescriptor : announcedFiles)
		{
			String filePath = fsa.buildPathFromDescriptor(fileDescriptor);

			if (filePath.equals(path))
			{
				return fileDescriptor;
			}
		}
		
		ArrayList<FileDescriptor> sharedFiles = permissionsManager.getOfferedLocalFiles();

		for (FileDescriptor fileDescriptor : sharedFiles)
		{
			String filePath = fsa.buildPathFromDescriptor(fileDescriptor);

			if (filePath.equals(path))
			{
				return fileDescriptor;
			}
		}
		
		return null;
	}
	
	/**
	 * insertSingleDescriptorAnnouncement()
	 * is a private method called by handleOfferRequest(). This method will take the file
	 * descriptor parameter, wrap it into an array, and send an announce action to the specified
	 * peer.
	 *          
	 * @param descriptor  file descriptor of the file to be announced 
	 * @param peer  specifies the peer to receive the announcement 
	 */
	private void insertSingleDescriptorAnnouncement(FileDescriptor descriptor, String peer)
	{
		FileDescriptor fd = new FileDescriptor(descriptor);
		FileDescriptor[] descriptorArray = { fd };
		
		synchronized(showRelativePath)
		{
			if (!showRelativePath)
			{
				descriptorArray[0].relativePath = "";
			}
		}
		
		synchronized(showSharedPath)
		{
			if (!showSharedPath)
			{
				descriptorArray[0].sharedPath = "";
			}
		}
		
		boolean isFileIdResponse = true;
		
		Action action = new Action();
		action.actionType = ActionType.ANNOUNCE;
		action.parameters.add(descriptorArray);
		action.parameters.add(isFileIdResponse);
		action.peer = peer;
		dispatcher.insertAction(action);
	}
	
	/**
	 * insertFileIDResponseAction()
	 * is a private method called by handleOfferRequest(). This method is called when the
	 * user grants the offer request of a remote peer. A FILE_ID_RESPONSE action will be
	 * inserted into the Dispatcher. This will allow the Dispatcher to call back to the
	 * DirectedAnnouncementManager so a file descriptor for the filePath can be constructed 
	 * and sent to the remote peer.
	 *          
	 * @param filePath  specifies the absolute path of the file 
	 * @param peer  specifies the peer to receive the announcement 
	 */
	private void insertFileIDResponseAction(String filePath, String peer)
	{
		Action action = new Action();
		action.actionType = ActionType.FILE_ID_RESPONSE;
		action.parameters.add(filePath);
		action.parameters.add(peer);
		dispatcher.insertAction(action);
	}
	
	/**
	 * handleOfferResponse()
	 * is called when a directed announcement is received in response to an offer
	 * request. This function passes the file list to the permissions manager to be
	 * stored and, if available, notifies the user that a directed announcement has
	 * been received.
	 * 
	 * @param fileList  specifies the list of announced files 
	 * @param peer  specifies the peer that sent the directed announcement
	 */
	@Override
	public void handleOfferResponse(FileDescriptor[] fileList, String peer)
	{
		FileDescriptor descriptor = fileList[0];
		
		permissionsManager.addOfferedRemoteFile(descriptor, peer);	
		
		synchronized(fileAnnouncementReceivedListenerLock)
		{
			if (fileAnnouncementReceivedListener != null)
			{
				fileAnnouncementReceivedListener.receivedAnnouncement(fileList, true);
			}
		}
	}
	
	/**
	 * generateFileDescriptor()
	 * is a callback method that is invoked when the Dispatcher encounters the FILE_ID_RESPONSE
	 * action. This function will build the file descriptor for the file with the specified path
	 * and insert an announce action, containing the newly created file descriptor, into the
	 * Dispatcher. The announce signal with be directed to the specified peer.
	 *          
	 * @param action  instance of FILE_ID_REPONSE action
	 */
	@Override
	public void generateFileDescriptor(Action action)
	{
		String filePath = (String)action.parameters.get(0);
		String peer = (String)action.parameters.get(1);
	
		ArrayList<String> failedPaths = new ArrayList<String>();
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(filePath);
		
		FileDescriptor[] descriptorArray = fsa.getFileInfo(pathList, failedPaths, localBusID);

		if (descriptorArray.length != 1 || failedPaths.size() == 1)
		{
			return;
		}
		else
		{	
			FileDescriptor generatedDescriptor = new FileDescriptor(descriptorArray[0]);
			permissionsManager.addOfferedLocalFile(generatedDescriptor);			
			
			boolean isFileIdResponse = true;
			
			Action announceAction = new Action();
			announceAction.actionType = ActionType.ANNOUNCE;
			announceAction.parameters.add(descriptorArray);
			announceAction.parameters.add(isFileIdResponse);
			announceAction.peer = peer;
			dispatcher.insertAction(announceAction);
		}
	}
	
	/**
	 * setUnannouncedFileRequestListener()
	 * is used to register the {@link UnannouncedFileRequestListener}. Registering this listener
	 * allows the user to be notified when a file request is made for unannounced/unshared
	 * files and to respond accordingly.
	 * <p>
	 * Note: if this listener is unregistered all requests for unannounced/unshared
	 * files will be denied by default.
	 *          
	 * @param listener  instance of UnnnoucnedFileRequestListener
	 */
	public void setUnannouncedFileRequestListener(UnannouncedFileRequestListener listener)
	{
		synchronized(unannouncedFileRequestListenerLock)
		{
			unannouncedFileRequestListener = listener;
		}
	}
	
	/**
	 * setFileAnnouncementReceivedListener()
	 * is used to register the {@link FileAnnouncementReceivedListener}. Registering this listener 
	 * allows the user to be notified when a announcement is received by a remote session peer.
	 * <p>
	 * Note: this listener must be registered for you to be able to send file announcement
	 * requests to remote session peers.
	 * 
	 * @param listener  instance of FileAnnoucnementReceivedListener
	 */
	public void setFileAnnouncementReceivedListener(FileAnnouncementReceivedListener listener)
	{
		synchronized(fileAnnouncementReceivedListenerLock)
		{
			fileAnnouncementReceivedListener = listener;
		}
	}
	
	/**
	 * setShowRelativePath()
	 * allows the user to specify whether they want remote peers to be able to see the relative
	 * path of each announced file. The default value is true.
	 *          
	 * @param showRelativePath  specifies whether the user wants to show relative path
	 */
	public void setShowRelativePath(boolean showRelativePath)
	{
		synchronized(this.showRelativePath)
		{
			this.showRelativePath = showRelativePath;
		}
	}
	
	/**
	 * getShowRelativePath()
	 * returns to the user the showRelativePath setting.
	 *           
	 * @return boolean
	 */
	public boolean getShowRelativePath()
	{
		return showRelativePath;			
	}
	
	/**
	 * setShowSharedPath()
	 * allows the user to specify whether they want remote peers to be able to see the shared
	 * path of each announced file. Default value is false.
	 *          
	 * @param showSharedPath  determines whether the user wants to show shared path
	 */
	public void setShowSharedPath(boolean showSharedPath)
	{
		synchronized(this.showSharedPath)
		{
			this.showSharedPath = showSharedPath;
		}
	}
	
	/**
	 * getShowSharedPath()
	 * returns to the user the showSharedPath setting.
	 *           
	 * @return boolean
	 */
	public boolean getShowSharedPath()
	{
		return showSharedPath;
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
}
