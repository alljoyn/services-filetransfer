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
import org.alljoyn.cops.filetransfer.data.FileStatus;
import org.alljoyn.cops.filetransfer.data.ProgressDescriptor;
import org.alljoyn.cops.filetransfer.data.ProgressDescriptor.State;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.RequestDataReceivedListener;
import org.alljoyn.cops.filetransfer.listener.SendManagerListener;
import org.alljoyn.cops.filetransfer.utility.Logger;

/**
 * The SendManager (SM) is a major piece of the File Transfer Module. The SMs major
 * responsibilities include: handling file requests from other session peers, executing 
 * sender initiated cancel operations, responding to receiver initiated pause/cancel 
 * operations, and dividing larger files into smaller usable chunks. From the senders 
 * perspective, this module is the driving force behind sending files to other session 
 * peers. 
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class.
 */
public class SendManager implements SendManagerListener
{
	/** Member Variables **/
	private ArrayList<FileStatus> sendingFiles;
	private FileSystemAbstraction fsa;
	private Dispatcher dispatcher;
	private PermissionsManager permissionsManager;
	private RequestDataReceivedListener requestDataReceivedListener;	
    private final Object requestDataReceivedListenerLock;
    
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * SendManager()
	 * constructs an instance of the SendManager class and initializes all other
	 * member variables. 
	 * 
	 * @param dispatcher  instance of the Dispatcher
	 */
	public SendManager(Dispatcher dispatcher, PermissionsManager permissionsManager)
	{
		this(dispatcher, FileSystemAbstraction.getInstance(), 
				permissionsManager);
	}
	
	/**
	 * SendManager()
	 * constructs an instance of the SendManager class and uses the constructor parameters
	 * to initialize the member variables.
	 * <p>
	 * Note: this secondary constructor is used for testing purposes only when we need to specify
	 * a separate instance of the PermissionsManager and FileSystemAbstraction.
	 * 
	 * @param dispatcher  instance of dispatcher
	 * @param fsa  instance of FileSystemAbstraction module
	 * @param pm  instance of PermissionsManager module
	 */
	public SendManager(Dispatcher dispatcher, FileSystemAbstraction fsa, 
			PermissionsManager pm)
	{
		this.sendingFiles = new ArrayList<FileStatus>();
		this.fsa = fsa;
		this.dispatcher = dispatcher;

		this.requestDataReceivedListener = null;
		this.permissionsManager = pm;

        this.requestDataReceivedListenerLock = new Object();
	}   
	
	/**
	 * handleFileRequest()
	 * is called when a session peer wants to request an announced or shared file. If the specified
	 * fileID matches an announced or shared file, we queue an action in the dispatcher to send the
	 * file to the specified peer and return a status code of OK. If the fileID does not match an
	 * announced or shared file, we return a status code of BAD_FILE_ID. 
	 * 
	 * @param fileID  specifies the file ID of the file being requested
	 * @param startByte  specifies the starting position within the file (usually zero)
	 * @param length  specifies the number of bytes to be sent (usually the length of the file)
	 * @param peer  specifies the intended recipient of the file
	 * @param maxChunkLength  specifies the maximum chunk size
	 * @return OK or BAD_FILE_ID
	 */
	public int handleFileRequest(byte[] fileID, int startByte, int length, String peer, int maxChunkLength)
	{		
		return startSendingFile(fileID, startByte, length, peer, maxChunkLength);
	}

	/**
	 * startSendingFile()
	 * is a private helper function called by handleFileRequest(). For an explanation of how this method
	 * functions. See handleFileRequest().
	 * 
	 * @param fileID  specifies the filecID of the file being requested
	 * @param startByte  specifies the starting position within the file (usually zero)
	 * @param length  specifies the number of bytes to be sent (usually the length of the file)
	 * @param peer  specifies the intended recipient of the file
	 * @param maxChunkLength  specifies the maximum chunk size
	 * @return OK or BAD_FILE_ID
	 */
	private int startSendingFile(byte[] fileID, int startByte, int length,
			String peer, int maxChunkLength)
	{
		FileDescriptor fileDescriptor = permissionsManager.getLocalFileDescriptor(fileID);
		
		if(fileDescriptor != null)
		{
			String path = fsa.buildPathFromDescriptor(fileDescriptor);
			getFileChunkAndQueueDataAction(fileID, startByte, length, peer, maxChunkLength, path, fileDescriptor);
			
			synchronized(requestDataReceivedListenerLock)
			{
    			if (requestDataReceivedListener != null)
    			{
    				requestDataReceivedListener.fileRequestReceived(fileDescriptor.filename);
    			}
			}
			return StatusCode.OK;
		}
		else
		{
			return StatusCode.BAD_FILE_ID;
		}
	}
	
	/**
	 * getFileChunkAndQueueDataAction()
	 * is a private function called by startSendingFile(). This function takes the input parameters
	 * and gets the next available file chunk and inserts a DATA_CHUNK action into the dispatcher
	 * for processing. The file chunk is sent to the specified peer.
	 * 
	 * @param fileID  specifies the file ID of the file being requested
	 * @param startByte  specifies the starting position within the file (usually zero)
	 * @param length  specifies the number of bytes to be sent (usually the length of the file)
	 * @param peer  specifies the intended recipient of the file
	 * @param maxChunkLength  specifies the maximum chunk size
	 * @param path  specifies the path of the offered file
	 * @param fileDescriptor  specifies the file descriptor for the file being offered
	 */
	private void getFileChunkAndQueueDataAction(byte[] fileID, int startByte, int length, String peer, 
			int maxChunkLength, String path, FileDescriptor fileDescriptor)
	{
		if (length <= maxChunkLength)
		{
			byte[] chunk = getFileChunk(path, startByte, length);
			Action action = createAction(fileDescriptor, peer, startByte, length, chunk);				
			dispatcher.insertAction(action);
		}
		else
		{
			byte[] chunk = getFileChunk(path, startByte, maxChunkLength);
			Action action = createAction(fileDescriptor, peer, startByte, maxChunkLength, chunk);
			createFileStatus(fileDescriptor, startByte, length, peer, maxChunkLength);
			dispatcher.insertAction(action);				
		}
	}
	
	/**
	 * getFileChunk()
	 * is a private function that is called by getFileChunkAndQueueDataAction(). This function calls
	 * getChunk() in the FileSystemAbstraction to get the next chunk of a file and returns the file
	 * chunk as a byte array.
	 * 
	 * @param path  specifies the path to the file being transferred
	 * @param startByte  specifies the starting position for the file data
	 * @param chunkLength  specifies the number of bytes in the chunk
	 * @return file chunk
	 */
	private byte[] getFileChunk(String path, int startByte, int chunkLength)
	{
		byte[] chunk = new byte[chunkLength];
		
		try
		{
			fsa.getChunk(path, chunk, startByte, chunkLength);
		}
		catch (Exception e) 
		{  
			Logger.log(e.toString());
		}
		
		return chunk;
	}
	
	/**
	 * createAction()
	 * is a private function that is called by getFileChunkAndQueueDataAction(). This function
	 * simply creates and returns a DATA_CHUNK action to be inserted into the Dispatcher.
	 * 
	 * @param fileDescriptor  specifies the file descriptor for the file being transfered
	 * @param peer  specifies the recipient of the file
	 * @param startByte  specifies the starting position of the data 
	 * @param length  specifies the number of bytes being sent
	 * @param chunk  specifies the chunk of the file being sent
	 * @return reference to DATA_CHUNK Action
	 */
	private Action createAction(FileDescriptor fileDescriptor, String peer, int startByte, int length, 
			byte[] chunk)
	{
		Action action = new Action();
		action.actionType = ActionType.DATA_CHUNK;
		action.peer = peer;
		action.parameters.add(fileDescriptor.fileID);
		action.parameters.add(startByte);
		action.parameters.add(length);
		action.parameters.add(chunk);
		
		return action;
	}
	
	/**
	 * createFileStatus()
	 * is a private function that is called by getFileChunkAndQueueDataAction(). This function creates
	 * a file status object so we can monitor the sending progress of the file. 
	 *  
	 * @param fileDescriptor  specifies the file descriptor for the file being transfered
	 * @param startByte  specifies the starting position for the file data
	 * @param length  specifies the length of the file
	 * @param peer  specifies the recipient of the file
	 * @param chunkLength  specifies the length of each file chunk
	 */
	private void createFileStatus(FileDescriptor fileDescriptor, int startByte, int length, String peer, 
			int chunkLength)
	{
		FileStatus fileStatus = new FileStatus();
		fileStatus.fileId = fileDescriptor.fileID;
		fileStatus.startByte = startByte;
		fileStatus.length = length;
		fileStatus.peer = peer;
		fileStatus.numBytesSent = chunkLength;
		fileStatus.chunkLength = chunkLength;
		
		synchronized(sendingFiles)
		{
		    sendingFiles.add(fileStatus);
		}
	}
	
	/**
	 * sendFile()
	 * is a function implemented for SendManagerListener interface. Its sole responsibility
	 * is to call the startSendingFile() function that will start sending the file matching
	 * the specified file ID.
	 * 
	 * @param fileID  specifies the file ID of the requested file
	 * @param startByte  specifies the starting position for the file data
	 * @param length  specifies the length of the file
	 * @param peer  specifies the recipient of the file
	 * @param maxChunkLength  specifies the length of each file chunk
	 */
	@Override
	public int sendFile(byte[] fileID, int startByte, int length, String peer,
			int maxChunkLength)
	{
		return startSendingFile(fileID, startByte, length, peer, maxChunkLength);		
	}
	
	/**
	 * dataSent()
	 * is the function implemented for the SendManagerListener interface. Its sole responsibility
	 * is to call queueNextChunk() if there is an incomplete file transfer inside the sendingFiles
	 * list.
	 */
	@Override
	public void dataSent()
	{
	    FileStatus sendingFile = null;
	    
	    synchronized (sendingFiles)
	    {
	        if (sendingFiles.size() > 0)
	        {
	            sendingFile = sendingFiles.get(0);
	        }
        }
	    
		if (sendingFile != null)
		{
			queueNextChunk(sendingFile);
		}
	}
	
	/**
	* queueNextChunk()
	* is a private function called by dataSent() method. This function takes the first file status object
	* from the sendingFiles list and inserts the next file chunk into the Dispatcher for transmission.
	* 
	* @param sendingFile  instance of FileStatus object matching the file being sent
	**/
	private void queueNextChunk(FileStatus sendingFile)
	{
		FileDescriptor fileDescriptor = permissionsManager.getLocalFileDescriptor(sendingFile.fileId);
		
		if (fileDescriptor != null)
		{
			String path = fsa.buildPathFromDescriptor(fileDescriptor);
			String peer = sendingFile.peer;
			
			if ((sendingFile.length - sendingFile.numBytesSent) <= sendingFile.chunkLength)
			{
				int startByte = sendingFile.numBytesSent + sendingFile.startByte;
				int length = (sendingFile.length - sendingFile.numBytesSent);
				
				byte[] chunk = getFileChunk(path, startByte, length);
				Action action = createAction(fileDescriptor, peer, startByte, length, chunk);
				deleteFileStatus(sendingFile.fileId);
				dispatcher.insertAction(action);			
			}
			else
			{
				int startByte = sendingFile.numBytesSent + sendingFile.startByte;
				byte[] chunk = getFileChunk(path, startByte, sendingFile.chunkLength);
				Action action = createAction(fileDescriptor, peer, startByte, sendingFile.chunkLength, chunk);
				sendingFile.numBytesSent += sendingFile.chunkLength;
				dispatcher.insertAction(action);			
			}
		}
	}
	
	/**
	 * cancelFile()
	 * is called when the sender wishes to cancel a file transfer that matches the specified file ID.
	 * This method will iterate over the list of pending file transfers to try and match the file ID.
	 * If a match is found, a cancel action is queued into the Dispatcher to notify the receiver that
	 * the sender has cancelled the file transfer. If a match is not found, the fileId does not match
	 * any current file transfers so we return FILE_NOT_BEING_TRANSFERRED.
	 *          
	 * @param fileID  specifies the file ID of the file being cancelled
	 * @return OK or FILE_NOT_BEING_TRANSFERRED
	 */
	public int cancelFile(byte[] fileID)
	{
		String peer = deleteFileStatus(fileID);

		if (peer != null)
		{
			queueCancelAction(fileID, peer);
			return StatusCode.OK;
		}
		else
		{
			return StatusCode.FILE_NOT_BEING_TRANSFERRED;
		}
	}

	/**
	 * handleStopDataXfer()
	 * is called when the receiver wishes to pause or cancel a file transfer. This function will
	 * look at at all current file transfers and delete the file status that matches the specified
	 * file ID. 
	 *          
	 * @param fileID  specifies the file ID of the file being requested
	 * @param peer  specifies the peer receiving the file
	 */ 
	public void handleStopDataXfer(byte[] fileID, String peer)
	{
		deleteFileStatus(fileID);
	}
	
	/**
	 * deleteFileStatus()
	 * is a private method called by handleStopDataXfer() and cancelFile(). Its main function is
	 * to iterate over the sendingFiles list to find the file status object that matches the
	 * specified file ID. If a match is found, the file status object is deleted from the sendingFiles
	 * list. This effectively cancels that file transfer.
	 * 
	 * @param fileID  specifies the file ID for the file being cancelled
	 * @return String  specifies the peer we are transferring the file to
	 */
	private String deleteFileStatus(byte[] fileID)
	{
		String peer = null;
		
		synchronized(sendingFiles)
		{
    		for (int i = 0; (i < sendingFiles.size()) && (peer == null ); i++)
    		{
    			FileStatus fileStatus = sendingFiles.get(i);
    			
    			if (Arrays.equals(fileID, fileStatus.fileId))
    			{
    				peer = fileStatus.peer;
    				sendingFiles.remove(i);
    			}
    		}
		}
		
		return peer;
	}
	
	/**
	 * queueCancelAction()
	 * is a private function called by cancelFile() and will insert a XFER_CANCELLED
	 * action into the Dispatcher. This will alert the receiver that the sender has 
	 * cancelled the file transfer.
	 * 
	 * @param fileID  specifies the file ID for the file being cancelled
	 * @param peer  specifies the recipient of the XFER_CANCELLED action
	 */
	private void queueCancelAction(byte[] fileID, String peer)
	{
		Action action = new Action();
		action.actionType = ActionType.XFER_CANCELLED;
		action.parameters.add(fileID);
		action.peer = peer;
		
		dispatcher.insertAction(action);
	}
	
	/** 
	 * getProgressList()
	 * compiles a list of all the current file transfers. The progress descriptor object details
	 * the fileId of the file, the length of the file, the total number of bytes that have been
	 * transferred, and the state of the transfer (will always be IN_PROGRESS).	 
	 *           			 
	 * @return array of progress descriptors
	 */
	public ArrayList<ProgressDescriptor> getProgressList()
	{
		ArrayList<ProgressDescriptor> progressList = new ArrayList<ProgressDescriptor>();
		ProgressDescriptor descriptor;
		
		synchronized(sendingFiles)
		{
    		for (FileStatus fileStatus : sendingFiles)
    		{
    			descriptor = new ProgressDescriptor();
    			descriptor.fileID = fileStatus.fileId;
    			descriptor.fileSize = fileStatus.length;
    			descriptor.bytesTransferred = fileStatus.numBytesSent;
    			descriptor.state = State.IN_PROGRESS;
    			
    			progressList.add(descriptor);
    		}
		}
		
		return progressList;
	}
	
	/** 
	 * setRequestDataListener()
	 * registers the listener that will notify the user when a file request has been received. 	 
	 *           			 
	 * @param listener  instance of RequestDataReceivedListener
	 */
	public void setRequestDataReceivedListener(RequestDataReceivedListener listener)
	{
        synchronized(requestDataReceivedListenerLock)
        {
            requestDataReceivedListener = listener;
        }
	}

	/**
	 * resetState()
	 * is called by the File Transfer Module when the user specifies a new AllJoyn
	 * session to be used. This function clears the hash map storing the file transfer 
	 * records. 
	 */
	public void resetState()
	{
		sendingFiles.clear();		
	}
}
