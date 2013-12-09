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

import java.io.File;
import java.util.ArrayList;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.cops.filetransfer.alljoyn.FileTransferBusObject;
import org.alljoyn.cops.filetransfer.data.*;
import org.alljoyn.cops.filetransfer.data.Action.ActionType;
import org.alljoyn.cops.filetransfer.listener.*;

/**
 * The File Transfer Module is a library that provides application developers with a
 * simple framework they can use to send and receive files with the various peers within
 * their AllJoyn session. This module is designed to be utilized with any existing
 * AllJoyn application with little, to no, modification. The framework provides many different
 * operations to the application developer that include: announce/unannounce files to session 
 * peers, requesting file announcements from other peers, request file by file ID and by absolute
 * path, cancel/pause file transfer, and offering files to a specified peer. There are also a
 * series of listeners that allow the developer to be notified at the application level when
 * various events occur; such examples include: an announcement being received by a session peer,
 * a file transfer has completed, a session peer has offered you a file, or a file request by path
 * has been received. The listeners allow the developer to respond accordingly to the various
 * events. Furthermore, the user has tremendous flexibility through the ability to change the 
 * current AllJoyn sesion associated with File Transfer. This allows users to instantiate multiple 
 * instances of the File Transfer Module and specify a different AllJoyn session for each. 
 * The user does not even have to specify an AllJoyn session for this module to work. The 
 * majority of file transfer operations can still be used but will not send any signals or perform 
 * any remote method calls until an AllJoyn session is provided. This framework is a great starting 
 * point for any AllJoyn application developers who need the ability to send/receive files.
 */
public class FileTransferModule 
{
	/** Member Variables **/
	private Dispatcher dispatcher;
	private AnnouncementManager announcementManager;
	private SendManager sendManager;
	private ReceiveManager receiveManager;
	private FileTransferBusObject busObject;
	private PermissionsManager permissionsManager;
	private DirectedAnnouncementManager directedAnnouncementManager;
	private OfferManager offerManager;
	private Receiver receiver;
	private FileSystemAbstraction fileSystemAbstraction;

	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * FileTransferModule()
	 * constructs an instance of the FileTransferModule without an AllJoyn session.
	 * This allows the user call initialize() at a later time to associate the File
	 * Transfer instance with an AllJoyn session.
	 */
	public FileTransferModule()
	{
		this(null, 0);			
	}
	
	/**
	 * FileTransferModule()
	 * constructs an instance of the FileTransferModule and uses the provided
	 * bus attachment to allow all file transfers to be sent over an existing
	 * AllJoyn session.
	 * 
	 * @param busAttachment  the AllJoyn BusAttachment to use for file transfers
	 * @param sessionID  the ID of the AllJoyn session to use for file transfers
	 */
	public FileTransferModule(BusAttachment busAttachment, int sessionID)
	{		
		//Set bus object and extract localBusID
		this.busObject = new FileTransferBusObject(busAttachment);				
		String localBusID = (busAttachment == null) ? null : busAttachment.getUniqueName();
		
		//Initialize Local Variables
		this.fileSystemAbstraction = FileSystemAbstraction.getInstance();
		this.permissionsManager = new PermissionsManager();
		
		this.dispatcher = new Dispatcher(busObject, busAttachment, sessionID);		
		this.announcementManager = new AnnouncementManager(dispatcher, permissionsManager, localBusID);
		this.offerManager = new OfferManager(dispatcher, permissionsManager, localBusID);
		this.directedAnnouncementManager = new DirectedAnnouncementManager(dispatcher, permissionsManager, localBusID);		
		this.sendManager = new SendManager(dispatcher, permissionsManager);
		this.receiveManager = new ReceiveManager(dispatcher, permissionsManager);		
		this.receiver = new Receiver(busAttachment, announcementManager, sendManager, receiveManager, 
				directedAnnouncementManager);		

		//Set listeners
		offerManager.setSendManagerListener(sendManager);
		offerManager.setReceiveManagerListener(receiveManager);		
		dispatcher.setSendManagerListener(sendManager);
		dispatcher.setDirectedAnnouncementManagerListener(directedAnnouncementManager);		
		busObject.setSendManagerListener(sendManager);
		busObject.setDirectedAnnouncementManagerListener(directedAnnouncementManager);
		busObject.setOfferManagerListener(offerManager);

		//Start dispatcher thread
		new Thread(dispatcher).start();		
	}

	/*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/
	/**
	 * initialize()
	 * allows the user to specify a new AllJoyn session for an existing instance of the
	 * File Transfer Module (FTM) by providing a new AllJoyn Bus Attachment and session
	 * ID. This allows some tremendous flexibility by allowing the user to have more than 
	 * a single instance of the FTC and manage multiple AllJoyn sessions. This concept of
	 * dynamic sessions will also allow the user to utilize most of the core FTC operations
	 * without specifying an AllJoyn session.  
	 * <p>
	 * Note: if there are existing announced files, an announcement will be sent to all session
	 * peers when anAllJoyn session is provided in the initialize() method.
	 * 
	 * @param busAttachment  instance of the AllJoyn Bus Attachment
	 * @param sessionID  specifies the AllJoyn Session ID
	 */
	public void initialize(BusAttachment busAttachment, int sessionID)
	{
		busObject = new FileTransferBusObject(busAttachment);
		String localBusId = (busAttachment == null) ? null : busAttachment.getUniqueName();
		
		permissionsManager.resetState(localBusId);
		dispatcher.resetState(busObject, busAttachment, sessionID);
		announcementManager.resetState(localBusId);
		offerManager.resetState(localBusId);
		directedAnnouncementManager.resetState(localBusId);
		sendManager.resetState();
		receiveManager.resetState();
		receiver.resetState(busAttachment, announcementManager, sendManager, receiveManager, 
				directedAnnouncementManager);	
		
		busObject.setSendManagerListener(sendManager);
		busObject.setDirectedAnnouncementManagerListener(directedAnnouncementManager);
		busObject.setOfferManagerListener(offerManager);
		
		if (permissionsManager.getAnnouncedLocalFiles().size() > 0)
		{			
			announcementManager.handleAnnouncementRequest(null);
		}
	}
	
	/**
	 * uninitialize()
	 * allows the user to disassociate an AllJoyn session with an existing instanc of the
	 * File Transfer Module (FTM). The user will still be able to use most of the core
	 * FTC operations minus anything that must be sent over AllJoyn to session peers. For
	 * example, the user can still announce files which will be stored but the announcement
	 * will not be sent over AllJoyn because a session does not exist.
	 */
	public void uninitialize()
	{
		initialize(null, 0);
	}
	
    /**
     * destroy()
     * is called to clean up all resources used by an instance of the File Transfer Module
     * (FTM). This function will terminate the dispatcher thread and destroy all references to
     * the modules used by the FTM. Therefore, any subsequent calls to the FTM after this
     * function has been called, will result in a null pointer exception being thrown.
     */
	public void destroy()
	{
	    Action shutdownThreadAction = new Action();
	    shutdownThreadAction.actionType = ActionType.SHUTDOWN_THREAD;
	    
	    dispatcher.insertAction(shutdownThreadAction);
	    
        dispatcher = null;
        announcementManager = null;
        sendManager = null;
        receiveManager = null;
        busObject = null;
        permissionsManager = null;
        directedAnnouncementManager = null;
        offerManager = null;
        receiver = null;
        fileSystemAbstraction = null;
	}
	
	/**
	 * announce()
	 * accepts an array of strings which specify the absolute paths
	 * of the files that need to be announced to session peers. Announcements are
	 * sent to every peer that is in the AllJoyn session.
	 * <p>
	 * Note: you can specify the path to a directory which will announce every file
	 * contained in the directory. This does not mean that any new files added to
	 * the announced directory will be announced automatically. Announcing a directory
	 * takes a snapshot of the directories current files and announces them. If any
	 * new files are added, they must be explicitly announced at a later time.
	 * <p>
	 * Note: if you announce the same file from two separate locations, only the most
	 * recent file will be available for transfer.
	 * <p>
	 * Note: the announce method returns void but the user can register the 
	 * FileAnnouncementSentListener using {@link #setFileAnnouncementSentListener} so the 
	 * announce function will return an array of paths that failed to successfully 
	 * announce. This FileAnnouncementSentListener is not mandatory to announce files.
	 * 
	 * @param pathList  ArrayList of file paths to be announced 
	 */
	public void announce(ArrayList<String> pathList)
	{
		announcementManager.announce(pathList);
	}
	
	/**
	 * stopAnnounce()
	 * accepts an array of strings that specify the absolute paths
	 * of the files that need to be unannounced. After the files are unannounced,
	 * an announcement is sent to all session peers that contains the latest list 
	 * of files that are available. This function returns an array of paths that 
	 * failed to unannounce.
	 * 
	 * @param pathList  an array of paths to be unannounced 
	 * @return array of paths that failed to unannounce
	 */
	public ArrayList<String> stopAnnounce(ArrayList<String> pathList)
	{
		return announcementManager.stopAnnounce(pathList);
	}
	
	/**
	 * requestFileAnnouncement()
	 * sends an announcement request to the specified session peer. For the case
	 * that peer is null, a broadcast signal is sent to all session peers. This function
	 * will return NO_FILE_ANNOUNCEMENT_LISTENER if the user had failed to set a 
	 * FileAnnouncementListener. Otherwise, the function will return OK. 
	 * <p>
     * Note: a FileAnnouncementReceivedListener must be set using {@link #setFileAnnouncementReceivedListener} 
     * in order for you to call this method. This is mandatory because you will not know 
     * when a peer answers your announcement request if you have not registered this listener.
	 * 
	 * @param peer  requests an announcement from the specified peer 
	 * @return OK or NO_FILE_ANNOUNCEMENT_LISTENER
	 */
	public int requestFileAnnouncement(String peer)
	{
		return announcementManager.requestFileAnnouncement(peer);
	}
	
	/**
	 * requestOffer()
	 * sends a request to the specified peer for the file with the specified
	 * file path. This is the main method of how users can request files that have not been
	 * explicitly announced. In order for this to happen, the developer must implement the
	 * {@link UnannouncedFileRequestListener} interface to allow session peers to request files they
	 * have not announced or shared. The default behavior is to reject requests for files
	 * that have not been announced or shared. The function returns a status code of OK if
	 * the specified peer is willing to transfer you the unannounced file. An announcement
	 * for the requested file should arrive shortly. Otherwise, the transfer request will
	 * be denied.  
	 * <p>
	 * Note: The UnannouncedFileRequestListener can be set using {@link #setUnannouncedFileRequestListener}
	 * method and is mandatory if you wish to allow sessions peers to request files that have not
	 * been announced or shared.
	 * 
	 * @param peer  specifies the peer that will receive the request
	 * @param filePath  specifies the absolute path of the remote file
	 * @return OK or REQUEST_DENIED
	 */
	public int requestOffer(String peer, String filePath)
	{
		return directedAnnouncementManager.requestOffer(peer, filePath);
	}
	
    /**
     * getFileID()
     * searches the list of the files announced by all peers within the session
     * to see if there is a file that matches the parameters peer and filePath.
     * If so, it returns the fileId of the file. If a match is not found, this
     * function returns null.
     * 
     * @param peer  the owner of the file
     * @param filePath  the absolute path of the file
     * @return fileID; null otherwise
     */
	public byte[] getFileID(String peer, String filePath)
	{
		return permissionsManager.getFileID(peer, filePath);
	}
	
	/**
	 * getAvailableRemoteFiles()
	 * returns to the user a list of the files announced by all peers within the session. 
	 * This local list is constructed through announcements made by other peers.
	 * 
	 * @return array of currently announced files from other peers
	 */
	public ArrayList<FileDescriptor> getAvailableRemoteFiles()
	{
		return permissionsManager.getAvailableRemoteFiles();
	}
	
	/**
	 * getAnnouncedLocalFiles()
	 * returns to the user a full list of the files they have been announced to other peers within 
	 * the session. This list is built by announcing files to session peers.
	 * 
	 * @return array of currently announced files to other peers
	 */
	public ArrayList<FileDescriptor> getAnnouncedLocalFiles()
	{
		return permissionsManager.getAnnouncedLocalFiles();
	}
	
	/**
	 * getOfferedLocalFiles()
	 * returns to the user a full list of the files shared but not announced to other peers within
	 * the session. This list is built by offering files to peers or by allowing peers to request
	 * files by path.
	 * 
	 * @return array of currently offered files
	 */
	public ArrayList<FileDescriptor> getOfferedLocalFiles()
	{
		return permissionsManager.getOfferedLocalFiles();
	}
	
	/**
	 * setShowRelativePath()
	 * allows the user to set a boolean variable that determines whether or not session peers 
	 * can see the relative paths of the announced/shared files. This allows the user to hide the
	 * relative paths of all files they announce/share. The default value is true.
	 * 
	 * @param showRelativePath
	 */
	public void setShowRelativePath(boolean showRelativePath)
	{
		announcementManager.setShowRelativePath(showRelativePath);
		directedAnnouncementManager.setShowRelativePath(showRelativePath);
	}
	
	/**
	 * getShowRelativePath()
	 * Indicates whether or not session peers are currently able to see the relative paths of 
	 * announced/shared files.
	 * 
	 * @return boolean
	 */
	public boolean getShowRelativePath()
	{
		return announcementManager.getShowRelativePath();
	}
	
	/**
	 * setShowSharedPath()
	 * allows the user to set a boolean variable that determines whether or not session peers can 
	 * see the shared paths of the announced/shared files. This allows the user to hide the shared
	 * path of all files they announce/share. The default value is false.
	 * 
	 * @param showSharedPath
	 */
	public void setShowSharedPath(boolean showSharedPath)
	{
		announcementManager.setShowSharedPath(showSharedPath);
		directedAnnouncementManager.setShowSharedPath(showSharedPath);
	}
	
	/**
	 * getShowSharedPath()
	 * Indicates whether or not session peers are currently able to see the the shared paths of
	 * the announced/shared files.
	 * 
	 * @return boolean
	*/
	public boolean getShowSharedPath()
	{
		return announcementManager.getShowSharedPath();
	}

	/**
	 * setDefaultSaveDirectory()
	 * specifies the default save directory for incoming file transfers. This function will return 
	 * BAD_FILE_PATH if the method is unsuccessful. Otherwise, the function will return OK.
	 * 
	 * @param directory  the path to the default save directory
	 * @return OK or BAD_FILE_PATH
	 */
	public int setDefaultSaveDirectory(String directory)
	{		
		return receiveManager.setDefaultSaveDirectory(directory);		
	}

    /**
     * setCacheFile()
     * allows the user to specify the path for a file that will be used to store the hash
     * value of files that are made available to AllJoyn session peers. Caching is helpful 
     * to avoid recalculating the hash value of the same file multiple times, which for 
     * large files can be a time consuming operation. The user must call setCacheFile()
     * with a valid file path to enable caching. 
     * <p>
     * Note: caching is disabled by default. 
     * <p>
     * Note: Calling this function to change the cache file causes any existing cached data
     * to be written to the old file, and then the cache is replaced by the contents of the 
     * new file (if any are present in the new file). 
     * <p>
     * Note: passing in null disables caching.
     * 
     * @param path  specifies the path to the file used for caching
     */
    public void setCacheFile(String path)
    {
        fileSystemAbstraction.setCacheFile(path);
    }

    /**
     * setCacheFile()
     * allows the user to provide the file object that will be used to store the hash
     * value of files that are made available to AllJoyn session peers. Caching is helpful 
     * to avoid recalculating the hash value of the same file multiple times, which for 
     * large files can be a time consuming operation. The user must call setCacheFile()
     * with a valid file to enable caching. 
     * <p>
     * Note: caching is disabled by default. 
     * <p>
     * Note: Calling this function to change the cache file causes any existing cached data
     * to be written to the old file, and then the cache is replaced by the contents of the 
     * new file (if any are present in the new file). 
     * <p>
     * Note: passing in null disables caching.
     * 
     * @param file  instance of a File object to be used as the cache file
     */
    public void setCacheFile(File file)
    {
        fileSystemAbstraction.setCacheFile(file);
    }

    /**
     * cleanCacheFile()
     * allows the user to audit the current cache file and remove any hash values that correlate
     * to files that no longer exist. This function will also remove any hash values that point
     * to files that have been modified since the last time they were hashed.
     */
    public void cleanCacheFile()
    {
        fileSystemAbstraction.cleanCacheFile();    
    }
    
	
	/**
	 * setChunkSize()
	 * specifies the maximum chunk length for each data chunk for a file. The chunk length must be
	 * greater than zero. The default chunk length is 1024
	 * 
	 * @param length  the maximum length for a data chunk
	 */
	public void setChunkSize(int length)
	{
		receiveManager.setMaxChunkSize(length);
	}
	
	/**
	 * getChunkSize()
	 * returns to the user the current maximum chunk size for a data packet. 
	 * 
	 * @return current maximum chunk size
	 */
	public int getChunkSize()
	{		
		return receiveManager.getMaxChunkSize();
	}
	
	/**
	 * requestFile()
	 * sends a file request to the specified peer for the file matching the fileID parameter. 
	 * This is the main method that should be used when requesting files. Each session peer will 
	 * accumulate a list of files that are available from each peer through file announcements. 
	 * Each session peer can then request any file that is made available using this function. 
	 * The function will return one of the following status codes: OK, BAD_FILE_ID, BAD_FILE_PATH, 
	 * or FILE_NOT_BEING_TRANSFERRED. If you get the return code OK the file is on its way. 
	 * 
	 * @param peer  specifies the peer to send the file request 
	 * @param fileID  specifies the file ID of the file being requested
	 * @param saveFileName  specifies the name for which to save the file
	 * @return OK, BAD_FILE_ID, BAD_FILE_PATH, or FILE_NOT_BEING_TRANSFERRED
	 */
	public int requestFile(String peer, byte[] fileID, String saveFileName)
	{
		return receiveManager.requestFile(peer, fileID, saveFileName, null);		
	}
	
	/**
	 * requestFile()
	 * sends a file request to the specified peer for the file matching the fileID parameter. 
	 * This is the main method that should be used when requesting files. Each session peer will 
	 * accumulate a list of files that are available from each peer through file announcements. 
	 * Each session peer can then request any file that is made available using this function. 
	 * The function will return one of the following status codes: OK, BAD_FILE_ID, BAD_FILE_PATH,
	 * or FILE_NOT_BEING_TRANSFERRED. If you get the return code OK the file is on its way. 
	 * 
	 * @param peer  specifies the peer to send the file request  
	 * @param fileID  specifies the file ID of the file being requested
	 * @param saveFileName  specifies the name for which to save the file
	 * @param saveDirectory  specifies the directory of where to save the file
	 * @return OK, BAD_FILE_ID, BAD_FILE_PATH, or FILE_NOT_BEING_TRANSFERRED
	 */
	public int requestFile(String peer, byte[] fileID, String saveFileName, String saveDirectory)
	{	
		return receiveManager.requestFile(peer, fileID, saveFileName, saveDirectory);	
	}
	
	/**
	 * offerFileToPeer()
	 * allows you to offer a file, that has not explicitly been announced, to the specified peer. 
	 * The user must specify the timeout interval because this function executes on the calling 
	 * thread and will block until the timeout interval is exceeded. However, even if an offer 
	 * expires from the sender's perspective, the peer that received the offer can still request 
	 * the file that you offered to them.  Offer file is the main method to use when you want to 
	 * share files with select peers and you do not want to announce them to the entire session. 
	 * This function will return OK if the peer accepts your offer. Otherwise, the offer will be 
	 * rejected (OFFER_REJECTED) or timeout (OFFER_TIMEOUT). If you happen to get the BAD_FILE_PATH 
	 * status code, it means the absolute file path you specified is invalid and the offer was not 
	 * sent to the remote peer.
	 * <p>
	 * Note: if the timeout interval is set to zero, the default timeout interval will be used 
	 * and is 5 seconds.
	 *          
	 * @param peer  specifies the peer to send the offer
	 * @param filePath  specifies the path of the local file being offered
	 * @param timeoutMSecs  specifies how long we will wait for a response
	 * @return OK, OFFER_REJECTED, BAD_FILE_PATH, or OFFER_TIMEOUT
	 */
	public int offerFileToPeer(String peer, String filePath, int timeoutMSecs)
	{
		return offerManager.offerFile(peer, filePath, timeoutMSecs);
	}
	
	/**
	 * cancelSendingFile()
	 * allows the sender to cancel a transfer for a file with the specified file ID and the 
	 * receiver will be notified that the sender cancelled the file transfer. The receiver 
	 * will keep the temporary file in memory so the transfer can be resumed at a later time 
	 * if the receiver wishes. This function will return one of two status codes: 1) OK,
	 * if the fileId is valid and the file transfer has been successfully cancelled; 
	 * 2) FILE_NOT_BEING_TRANSFERRED, if the specified fileId does not match any file currently 
	 * being transferred.
	 * 
	 * @param fileID  specifies the fileId for the file the user wishes to cancel
	 * @return OK or FILE_NOT_BEING_TRANSFERRED
	 */
	public int cancelSendingFile(byte[] fileID)
	{
		return sendManager.cancelFile(fileID);
	}
	
	/**
	 * cancelReceivingFile()
	 * allows the receiver to cancel a transfer for a file with the specified file ID. A cancel
	 * notification is sent to the sender to not send any more bytes. The receiver immediately 
	 * deletes any temporary files corresponding to the cancelled file transfer. This function 
	 * returns one of the following status codes: OK if the cancel is successful, or BAD_FILE_ID 
	 * if the specified fileId does not match any current files being received.
	 * 
	 * @param fileID  specifies the file ID for the file the user wishes to cancel
	 * @return OK or BAD_FILE_ID
	 */
	public int cancelReceivingFile(byte[] fileID)
	{
		return receiveManager.cancelFile(fileID);
	}
	
	/**
	 * pauseFile()
	 * can only be called by the receiver and temporarily suspends a file transfer. The sender 
	 * receives a notification to stop transmitting bytes to the receiver. Any temporary files 
	 * corresponding to the paused transfer are held in memory so the operation can be resumed 
	 * at a later time. This function returns one of the following status codes: OK if the pause 
	 * operation is successful, or BAD_FILE_ID if the specified fileId does not match any current 
	 * files being received.
	 * 
	 * @param fileID  specifies the file ID for the file the user wishes to pause
	 * @return OK or BAD_FILE_ID
	 */
	public int pauseFile(byte[] fileID)
	{
		return receiveManager.pauseFile(fileID);
	}
	
	/**
	 * getSendingProgressList()
	 * returns to the user an array of progress descriptors that specify all of the current and 
	 * pending file transfers being sent to other session peers. Each progress descriptor outlines 
	 * the fileId, file size, how many bytes have been sent, and the state of the transfer 
	 * (i.e. IN_PROGRESS, PAUSED, or TIMED_OUT).
	 *          
	 * @return array of progress descriptors
	 */
	public ArrayList<ProgressDescriptor> getSendingProgressList()
	{
		return sendManager.getProgressList();
	}
	
	/**
	 * getReceiveProgressList()
	 * returns to the user an array of progress descriptors that specify all of the current and pending 
	 * file transfers being received from other session peers. Each progress descriptor outlines the 
	 * file ID, file size, how many bytes have been sent, and the state of the transfer (i.e. IN_PROGRESS,
	 *  PAUSED, or TIMED_OUT).
	 *          
	 * @return array of progress descriptors
	 */
	public ArrayList<ProgressDescriptor> getReceiveProgressList()
	{
		return receiveManager.getProgressList();
	}
	
	/**
	 * setFileAnnouncementReceivedListener()
	 * allows the user to register a listener that will be called every time a file announcement is 
	 * received by a session peer. The intent is to allow the user to continuously update its local 
	 * list of available files so they can request files as needed.
	 * <p>
	 * Note: this listener must be registered in order for you to send file announcement requests to 
	 * other session peers using {@link #requestFileAnnouncement}. This is mandatory because you will 
	 * not know when a peer answers your announcement request if you have not registered this listener.
	 * 
	 * @param listener  instance of FileAnnouncementReceivedListener
	 */
	public void setFileAnnouncementReceivedListener(FileAnnouncementReceivedListener listener)
	{
		announcementManager.setFileAnnouncementReceivedListener(listener);
		directedAnnouncementManager.setFileAnnouncementReceivedListener(listener);
	}
	
	/**
	 * setFileAnnouncementSentListener()
	 * allows the user to register a listener that will be called when the announce function has finished 
	 * executing and the announcement has been sent. The user will also be provided with an array that 
	 * contains all of the file paths that failed to be successfully announced.
	 * <p>
	 * Note: this listener is entirely optional but very handy so the user can verify that all of the 
	 * provided files were successfully announced.
	 *          
	 * @param listener  instance of FileAnnouncementSentListener
	 */
	public void setFileAnnouncementSentListener(FileAnnouncementSentListener listener)
	{
		announcementManager.setFileAnnouncmentSentListener(listener);
	}
	
	/**
	 * setFileCompletedListener()
	 * allows the user to register a listener that will be called every time a file transfer has been completed. 
	 * This listener is only called once the file has been completely received (or cancelled), so to report 
	 * progress you must use the {@link #getReceiveProgressList} method.
	 * <p>
	 * Note: this listener is not mandatory to ensure proper execution of other API functions.
	 * 
	 * @param listener  instance of FileCompletedListener
	 */
	public void setFileCompletedListener(FileCompletedListener listener)
	{
		receiveManager.setFileCompletedListener(listener);
	}
	
	/**
	 * setOfferReceivedListener()
	 * allows the user to register a listener that will be called every time an file offer is received from a 
	 * session peer. This will allow the user to spontaneously decide whether or not they are going to accept
	 * or reject the file offer they have just received.
	 * <p>
	 * Note: if you do not register this listener any file offers you receive will go unnoticed and timeout.
	 * 
	 * @param listener  instance of OfferReceivedListener
	 */
	public void setOfferReceivedListener(OfferReceivedListener listener)
	{
		offerManager.setOfferReceivedListener(listener);
	}
	
	/**
	 * setRequestDataReceivedListener()
	 * allows the user to register a listener that will be called every time a file request is received from a 
	 * session peer. This listener will only tell you the name of the file being requested but not the peer who
	 * sent the request.
	 * <p>
	 * Note: this listener is not mandatory to ensure proper execution of other API functions.
	 * 
	 * @param listener  instance of RequestDataReceivedListener
	 */
	public void setRequestDataReceivedListener(RequestDataReceivedListener listener)
	{
		sendManager.setRequestDataReceivedListener(listener);
	}
	
	/**
	 * setUnannouncedFileRequestListener()
	 * allows the user to register a listener that will be called when file requests are received for files that 
	 * have not been explicitly announced or shared. The default behavior is to reject all requests for files 
	 * that have not been announced or shared.
	 * <p>
	 * Note: if you wish to allow requests for files that have not been announced or shared you need to register
	 * this listener and have the function return true.
	 *          
	 * @param listener  instance of UnannouncedFileRequestListener
	 */
	public void setUnannouncedFileRequestListener(UnannouncedFileRequestListener listener)
	{
		directedAnnouncementManager.setUnannouncedFileRequestListener(listener);
	}
}
