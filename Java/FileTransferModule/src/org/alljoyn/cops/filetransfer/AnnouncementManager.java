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
import org.alljoyn.cops.filetransfer.listener.AnnouncementManagerListener;
import org.alljoyn.cops.filetransfer.listener.FileAnnouncementReceivedListener;
import org.alljoyn.cops.filetransfer.listener.FileAnnouncementSentListener;

/**
 * The AnnouncementManager is only responsible for handling events associated
 * with announcing files. From the senders perspective, the AnnouncementManager
 * is responsible for sending announcements and announcement requests to remote
 * session peers and, if the user wishes, notify the user when an announcement
 * has finished and been sent to session peers. From the receivers perspective,
 * the AnnouncementManager is responsible for handling announced files when they
 * arrive, responding to announcement requests from session peers, and, if the user
 * wishes, notify the user when an announcement has been received from a remote 
 * session peer. The AnnouncementManager also maintains a pair of boolean settings
 * the user can set to dictate whether to show the relative/shared path of
 * announced files.  
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class.
 */
public class AnnouncementManager implements AnnouncementManagerListener
{
	/** Member Variables **/		
	private FileSystemAbstraction fsa;
	private Dispatcher dispatcher;
	private PermissionsManager permissionsManager;
	private String localBusID;
	private Boolean showRelativePath;
	private Boolean showSharedPath;
	private FileAnnouncementReceivedListener fileAnnouncementReceivedListener;
	private FileAnnouncementSentListener fileAnnouncementSentListener;
	private final Object fileAnnouncementReceivedListenerLock;
	private final Object fileAnnouncementSentListenerLock;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * AnnouncementManager()
	 * constructs an instance of the AnnouncementManager class and uses the constructor parameters
	 * to initialize the member variables.
	 * 
	 * @param dispatcher  instance of Dispatcher
	 * @param permissionsManager  instance of Permissions Manager
	 * @param localBusID  specifies the bus ID for the bus attachment passed in by the user
	 */
	public AnnouncementManager(Dispatcher dispatcher, PermissionsManager permissionsManager, String localBusID)
	{
		this(dispatcher, localBusID, permissionsManager, FileSystemAbstraction.getInstance());
	}
	
	/**
	 * AnnouncementManager()
	 * constructs an instance of the AnnouncementManager class and uses the constructor parameters
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
	public AnnouncementManager(Dispatcher dispatcher, String localBusID, PermissionsManager pm, 
			FileSystemAbstraction fsa)
	{
		this.fsa = fsa;
		this.permissionsManager = pm;
		this.dispatcher = dispatcher;
		this.localBusID = localBusID;
		this.showRelativePath = true;
		this.showSharedPath = false;
		
		this.fileAnnouncementReceivedListener = null;
		this.fileAnnouncementSentListener = null;
		this.fileAnnouncementReceivedListenerLock = new Object();		
		this.fileAnnouncementSentListenerLock = new Object();
	}
	
	/*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/
	/**
	 * announce()
	 * is called when the user wishes to announce a list of files to remote session peers. 
	 * This function only spawns a new thread and calls a private helper function that is 
	 * responsible for sending the announcement.
	 *  
	 * @param pathList  specifies a list of absolute paths to files that need to be announced
	 */
	public void announce(final ArrayList<String> pathList)
	{
		Thread announceThread = new Thread(new Runnable()
		{
			public void run()
			{
				announceFiles(pathList);
			}
		});
		
		announceThread.start();
	}
	
	/**
	 * announceFiles()
	 * is a private helper function that executes in a background thread and is responsible
	 * for delegating to the FSA to create the file descriptor for each file, invoking the
	 * PermissionsManager to store the newly announced files, and sending the announcement.
	 * If available, this function will fire the {@link FileAnnouncementSentListener} callback to notify
	 * the user that an announcement has been sent and pass back an array of paths that
	 * failed to successfully announce.
	 * 
	 * @param pathList  specifies a list of absolute paths to files that need to be announced
	 */
	private void announceFiles(ArrayList<String> pathList)
	{
		ArrayList<String> failedPaths = new ArrayList<String>();
		FileDescriptor[] files = fsa.getFileInfo(pathList, failedPaths, localBusID);
		
		permissionsManager.addAnnouncedLocalFiles(files);		
		sendAnnouncement(null, false);

		if (fileAnnouncementSentListener != null)
		{
			fileAnnouncementSentListener.announcementSent(failedPaths);
		}
	}
	
	/**
	 * sendAnnouncement()
	 * is a private function called by announce(). This function prepares an announcement
	 * action to be sent to specified peer.
	 * <p>
	 * Note: if the peer is null the announcement signal is sent to all session peers.
	 * Otherwise, the signal is directed to the specified peer.
	 *          
	 * @param peer  specifies the peer to send the announcement
	 * @param isFileIDResponse  specifies whether the announcement is a response to a file offer request
	 */
	private void sendAnnouncement(String peer, boolean isFileIDResponse)
	{
		ArrayList<FileDescriptor> myAnnouncedFiles = permissionsManager.getAnnouncedLocalFiles();
		FileDescriptor[] files = new FileDescriptor[myAnnouncedFiles.size()];		
		
		for (int i = 0; i < myAnnouncedFiles.size(); i++)
		{
			FileDescriptor localDescriptor = myAnnouncedFiles.get(i);
			FileDescriptor announcedDescriptor = new FileDescriptor(localDescriptor);
			
			synchronized(showRelativePath)
			{
				if (!showRelativePath)
				{
					announcedDescriptor.relativePath = "";
				}
			}
			
			synchronized(showSharedPath)
			{
				if (!showSharedPath)
				{
					announcedDescriptor.sharedPath = "";
				}
			}
			
			files[i] = announcedDescriptor;
		}
		
		Action action = new Action();
		action.actionType = ActionType.ANNOUNCE;
		action.parameters.add(files);
		action.parameters.add(isFileIDResponse);
		action.peer = peer;		
		dispatcher.insertAction(action);
	}
	
	/**
	 * stopAnnounce()
	 * is called when the user wishes to stop announcing specific file. All files that match  
	 * one of the provided paths are removed from the list of announced files and a new announcement
	 * is sent to all session peers. This function will also return an array of paths for the
	 * files that failed to unannounce.
	 * 
	 * @param pathList  specifies a list of absolute paths to files that need to be unannounced
	 * @return array of paths that failed to unannounce
	 */
	public ArrayList<String> stopAnnounce(ArrayList<String> pathList)
	{
		pathList = permissionsManager.removeAnnouncedLocalFiles(pathList);
		
		sendAnnouncement(null, false);
		
		return pathList;
	}
	
	/**
	 * requestFileAnnouncement()
	 * is called when the user wishes to request that the specified peer send their list
	 * of announced files. For this function to work correctly, the user must implement
	 * and set the {@link FileAnnouncementReceivedListener}. This listener is mandatory
	 * because you will have no way of knowing when the peer actually sends the announcement
	 * responding to your request. Therefore, this function will return 
	 * NO_FILE_ANNOUNCEMENT_LISTENER if the listener is not registered or OK to specify the
	 * announcement request has been sent successfully.
	 * 
	 * @param peer  specifies the peer we are requesting an announcement from
	 * @return OK or NO_FILE_ANNOUNCMENT_LISTENER
	 */
	public int requestFileAnnouncement(String peer)
	{
		if (localBusID == null)
		{
			return StatusCode.NO_AJ_CONNECTION;			
		}
		
		if (fileAnnouncementReceivedListener == null)
		{
			return StatusCode.NO_FILE_ANNOUNCEMENT_LISTENER;
		}
		
		Action action = new Action();
		action.actionType = ActionType.REQUEST_ANNOUNCE;
		action.peer = peer;		
		dispatcher.insertAction(action);
		
		return StatusCode.OK;
	}

	/**
	 * handleAnnounced()
	 * is called when an announcement has been received from a remote session
	 * peer. This method will pass the list of announced files over to the
	 * PermissionsManager for storage and, if available, fire the 
	 * {@link FileAnnouncementReceivedListener} to notify the user that an announcement
	 * has been received.
	 * 
	 * @param fileList  specifies the list of announced files
	 * @param peer  specifies the peer who sent the announcement
	 */
	@Override
	public void handleAnnounced(FileDescriptor[] fileList, String peer)
	{		
		permissionsManager.updateAnnouncedRemoteFiles(fileList, peer);
		
		if (fileAnnouncementReceivedListener != null)
		{
			fileAnnouncementReceivedListener.receivedAnnouncement(fileList, false);
		}
	}
	
	/**
	 * handleAnnouncementRequest()
	 * is called when an announcement request has been received from a remote
	 * session peer. This function calls sendAnnouncement() to send the
	 * announcement to the requesting peer.
	 * 
	 * @param peer  specifies the peer that made the announcement request
	 */
	@Override
	public void handleAnnouncementRequest(String peer)
	{
		sendAnnouncement(peer, false);
	}	

	/**
	 * setFileAnnouncementReceivedListener()
	 * is used to register the {@link FileAnnouncementReceivedListener}. Registering this
	 * listener allows the user to be notified when a announcement is received by a remote 
	 * session peer.
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
	 * setFileAnnouncementSentListener()
	 * is used to register the {@link FileAnnouncementSentListener}. Registering this listener
	 * allows the user to be notified when the announce function has finished executing
	 * and see if any files failed to be announced.
	 * <p>
	 * Note: if this listener is not registered announcements will still be sent but the user will
	 * have to manually check to see if any file paths failed to be announced.
	 *          
	 * @param listener  instance of FileAnnouncementSentListener
	 */
	public void setFileAnnouncmentSentListener(FileAnnouncementSentListener listener)
	{
		synchronized(fileAnnouncementSentListenerLock)
		{
			fileAnnouncementSentListener = listener;
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