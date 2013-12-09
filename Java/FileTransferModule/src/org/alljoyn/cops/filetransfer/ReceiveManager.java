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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import org.alljoyn.cops.filetransfer.data.Action;
import org.alljoyn.cops.filetransfer.data.Action.ActionType;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.FileStatus;
import org.alljoyn.cops.filetransfer.data.ProgressDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.FileCompletedListener;
import org.alljoyn.cops.filetransfer.listener.ReceiveManagerListener;
import org.alljoyn.cops.filetransfer.utility.Logger;

/**
 * The ReceiveManager (RM) is a major piece of the file transfer module. The RMs
 * major responsibilities include: building file requests for remote peers, handling incoming
 * file chunks and saving them,  executing pause and cancel requests made by the file receiver, 
 * and handling cancelled transfers by the remote peer. From the file receiving perspective, 
 * this module is the driving force behind receiving files from remote session peers.  
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class.
 */
public class ReceiveManager implements ReceiveManagerListener
{	
	/** Member Variables **/
	private Dispatcher dispatcher;
	private FileSystemAbstraction fsa;
	private PermissionsManager permissionsManager;
	private HashMap<String, FileStatus> fileStatuses;
	private String defaultSaveDirectory;	
	private int maxChunkSize;
	private FileCompletedListener fileCompletedListener;	
	private Object completedListenerLock;	
	private Object savePathLock;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * ReceiveManager()
	 * constructs an instance of the ReceiveManager class and initializes all other
	 * member variables.
	 * 
	 * @param dispatcher  instance of Dispatcher
	 */
	public ReceiveManager(Dispatcher dispatcher, PermissionsManager permissionsManager)
	{
		this(dispatcher, FileSystemAbstraction.getInstance(), permissionsManager);
	}
	
	/**
	 * ReceiveManager()
	 * constructs an instance of the ReceiveManager class and uses the constructor parameters
	 * to initialize the member variables.
	 * <p>
	 * Note: this secondary constructor is used for testing purposes only when we need to specify
	 * a separate instance of the PermissionsManager and FileSystemAbstraction.
	 * 
	 * @param dispatcher  instance of Dispatcher
	 * @param fsa  instance of the File System Abstraction class
	 * @param pm  instance of the Permissions Manager class
	 */
	public ReceiveManager(Dispatcher dispatcher, FileSystemAbstraction fsa, PermissionsManager pm)
	{
		this.dispatcher = dispatcher;
		this.fsa = fsa;
		this.permissionsManager = pm;
		
		fileStatuses = new HashMap<String, FileStatus>();
		maxChunkSize = 1024;
		
		completedListenerLock = new Object();
		savePathLock = new Object();
		
		setDefaultSaveDirectory("/mnt/sdcard/download");
	}
	
	/*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/
	/** 
	 * setDefaultSaveDirectory()
	 * allows the user to specify the default directory they want all file transfers to be
	 * saved. The user can change this at any time. If the change is successful, this function
	 * will return a status code of OK. If the change is not successful, this function will 
	 * return a status code of BAD_FILE_PATH.      			 
	 * 
	 * @param directory  specifies the absolute path to default save directory
	 * @return OK or BAD_FILE_PATH
	 */
	public int setDefaultSaveDirectory(String directory)
	{
		File file = new File(directory);
		
		if (!file.exists())
		{
			boolean success = file.mkdirs();
			
			if (!success)
			{
				return StatusCode.BAD_FILE_PATH;
			}
		}
		
		synchronized(savePathLock)
		{
			defaultSaveDirectory = directory;
		}			
		
		return StatusCode.OK;
	}

	/** 
	 * setFileCompletedListener()
	 * registers the {@link FileCompletedListener} listener that will be called when every file receive
	 * operation has been completed. This listener is not mandatory and does not impede normal file
	 * transfer functionality.	  			 
	 * 
	 * @param listener  instance of FileCompletedListener
	 */
	public void setFileCompletedListener(FileCompletedListener listener)
	{
		synchronized (completedListenerLock)
		{
			fileCompletedListener = listener;
		}			
	}

	/** 
	 * setMaxChunkSize()
	 * allows the user to specify the maximum chunk size for file data packets. The maximum
	 * chunk size must be greater than zero and does not impose a predetermined maximum. The
	 * default setting is 1024.  			 
	 * 
	 * @param length  specifies the max chunk size
	 */
	public int setMaxChunkSize(int length)
	{
		if (length > 0)
		{
			maxChunkSize = length;
			return StatusCode.OK;
		}
		return StatusCode.INVALID;		
	}

	/** 
	 * getMaxChunkSize()
	 * is called when the user wishes to see what they have set the max chunk size for file
	 * data packets.	  			 
	 * 
	 * @return  max chunk size
	 */
	public int getMaxChunkSize()
	{
		return maxChunkSize;
	}

	/** 
	 * getProgressList()
	 * will return to the user an array of progress descriptors that outline all pending
	 * file transfers that are being received from other session peers. The progress
	 * descriptor will specify the file ID of the file, the file size (in bytes), and how
	 * many bytes have already been transferred. This function allows the user to monitor
	 * the progress of files they are receiving.	  			 
	 * 
	 * @return  array of progress descriptors
	 */
	public ArrayList<ProgressDescriptor> getProgressList()
	{
		ArrayList<ProgressDescriptor> progressList = new ArrayList<ProgressDescriptor>();
		
		synchronized(fileStatuses)
		{
			for (Entry<String, FileStatus> entry : fileStatuses.entrySet())
			{
				FileStatus status = entry.getValue();
				
				ProgressDescriptor descriptor = new ProgressDescriptor();
				descriptor.fileID = status.fileId;
				descriptor.fileSize = status.length;
				descriptor.bytesTransferred = status.numBytesSent;
				
				progressList.add(descriptor);
			}
			return progressList;
		}		
	}

	/** 
	 * requestFile()
	 * is called when the user wishes to request a file specified by fileID from a remote
	 * session peer. This function will return one of the following status codes listed
	 * below. The only status code you want is OK. If you receive the OK status code that
	 * means the file is currently being transferred. If you receive a different status
	 * code, the transfer request failed.		 
	 * 
	 * @param owner  specifies the remote peer's unique bus ID
	 * @param fileID  specifies the id of the requested file
	 * @param saveFileName  specifies the name to save the requested file as
	 * @param saveFileDirectory  specifies the directory to save requested file to
	 * @return  OK, BAD_FILE_ID, BAD_FILE_PATH, or FILE_NOT_BEING_TRANSFERRED
	 */
	public int requestFile(String owner, byte[] fileID, String saveFileName,
			String saveFileDirectory)
	{
		FileDescriptor file = permissionsManager.getKnownFileDescriptor(fileID, owner);
		
		if (file == null)
		{
			return StatusCode.BAD_FILE_ID;
		}
		
		return requestFile(file, saveFileName, saveFileDirectory, false);						
	}
	
	/**
	 * requestFile()
	 * is a private helper function used by requestFile(). This function performs error checking
	 * for the provided parameters before the formal request is sent to the remote peer. After
	 * error checking is completed, the file request action is built and sent to the transmitter. 
	 * 
	 * @param file  instance of the FileDescriptor for the requested file
	 * @param saveFileName  specifies the name to save the requested file as
	 * @param saveDirectory  specifies the directory to save the file
	 * @param useDispatcher  specifies whether or not to insert the action into the Dispatcher 
	 * @return  OK, BAD_FILE_PATH, or FILE_NOT_BEING_TRANSFERRED
	 */
	@Override
	public int requestFile(FileDescriptor file, String saveFileName, String saveDirectory, 
			boolean useDispatcher)
	{		
		//determine root save directory
		if (saveDirectory == null)
		{
			synchronized(savePathLock)
			{
				saveDirectory = defaultSaveDirectory;
			}			
		}		
		
		//check directory valid
		if (!fsa.isValid(saveDirectory))
		{
			return StatusCode.BAD_FILE_PATH;
		}		
		
		//get FileStatus to handle receiving requested file
		FileStatus status = getFileStatus(file.fileID);
		
		if (status == null)
		{
			status = buildStatus(file, saveFileName, saveDirectory);
			
			synchronized(fileStatuses)
			{
				fileStatuses.put(Arrays.toString(status.fileId), status);
			}
		}		
		
		//request file 
		Action action = buildDataRequestAction(file, status);
		
		if (useDispatcher)
		{
			dispatcher.insertAction(action);
			return StatusCode.OK;	
		}
		else
		{				
			return dispatcher.transmitImmediately(action);
		}		
	}

	/**
	 * buildDataRequestAction()
	 * is called to build the REQUEST_DATA action. The function receives an instance of the
	 * File Descriptor and File Status objects for the requested file. This allows the function
	 * to place the necessary data inside the REQUEST_DATA action.
	 * 
	 * @param file  instance of file descriptor object
	 * @param status  instance of file status object
	 * @return  REQUEST_DATA action
	 */
	private Action buildDataRequestAction(FileDescriptor file, FileStatus status)
	{
		Action action = new Action();
		action.actionType = ActionType.REQUEST_DATA;
		action.parameters.add(file.fileID);
		action.parameters.add(status.numBytesSent);
		action.parameters.add(file.size);
		action.parameters.add(maxChunkSize);
		action.peer = file.owner;
		return action;
	}

	/**
	 * buildStatus()
	 * is a private function used to build the file status object for the requested file. The file
	 * status object provides the details to monitor the progress of the file transfer.
	 * 
	 * @param file  instance of file descriptor for requested file
	 * @param saveFileName  specifies the name to save the file as
	 * @param saveFileDirectory  specifies the location to save the file to
	 * @return  file status object
	 */
	private FileStatus buildStatus(FileDescriptor file, String saveFileName, String saveFileDirectory)
	{
		FileStatus status = new FileStatus();
		status.fileId = file.fileID;
		status.startByte = 0;
		status.length = file.size;
		status.peer = file.owner;
		status.numBytesSent = 0;
		status.saveFileName = saveFileName;		
		status.saveFilePath = new File(saveFileDirectory, file.relativePath).getAbsolutePath();		
		return status;
	}	

	/** 
	 * handleFileChunk()
	 * is called when a chunk of a given file is received from a remote peer. This function determines
	 * which temporary file this chunk belongs to, updates the sending progress, and sends the chunk
	 * to the FileSystemAbstraction to be appended to the appropriate temporary file.			 
	 * 
	 * @param fileID  specifies the ID of the file the chunk belongs to
	 * @param startByte  specifies the starting index of chunk relative to file
	 * @param chunkLength  specifies the length of chunk
	 * @param chunk  actual file data
	 */
	@Override
	public void handleFileChunk(byte[] fileID, int startByte, int chunkLength, byte[] chunk)
	{		
		FileStatus status = getFileStatus(fileID);
		
		if (startByte < status.numBytesSent)
		{
			Logger.log("out of order file chunk received");			
		}		
		else if (status != null)
		{			
			try
			{
				String path = new File(status.saveFilePath, status.saveFileName).getAbsolutePath();
				fsa.addChunk(path, chunk, startByte, chunkLength);
			} 
			catch (Exception e)
			{
				Logger.log(e.toString());
			}
			
			status.numBytesSent += chunkLength;
			
			if (status.numBytesSent >= status.length)
			{
				Logger.log("transfer completed");
				
				synchronized(fileStatuses)
				{
					fileStatuses.remove(Arrays.toString(fileID));
				}
				
				fireCompletedListener(status.saveFileName, StatusCode.OK);
			}
		}		
	}

	/** 
	 * handleDataXferCancelled()
	 * is called when the sender cancels a file transfer and sends the DataXferCancelled
	 * signal to notify the remote peer of the cancellation. This function will check to
	 * ensure that file was truly cancelled and then notify the user that the transfer has
	 * been completed.
	 * <p>
	 * Note: transfers cancelled by the sender do not cause the temporary
	 * files to be deleted. The temporary files are saved so the transfer can be resumed
	 * at a later time. 	  			 
	 * 
	 * @param fileID  specifies the ID of file being cancelled
	 * @param peer  specifies peer who cancelled the transfer
	 */
	@Override
	public void handleDataXferCancelled(byte[] fileID, String peer)
	{		
		FileStatus status = getFileStatus(fileID);		

		if (status != null)
		{
			String filename = status.saveFileName;
				
			fireCompletedListener(filename, StatusCode.CANCELLED);
		}		
	}

	/** 
	 * pauseFile()
	 * is called when the receiver wishes to pause a file transfer. The function will first
	 * check to see if the provided file ID matches a pending file transfer. If it does not,
	 * the function will return BAD_FILE_ID. Otherwise, the function will build a STOP_XFER
	 * action to be sent to the file sender and return the status code OK. 	  			 
	 * 
	 * @param fileID  specifies the fileId of the file to pause
	 * @return OK or BAD_FILE_ID
	 */
	public int pauseFile(byte[] fileID)
	{		
		FileStatus status = getFileStatus(fileID);
		
		if (status == null)
		{
			return StatusCode.BAD_FILE_ID;
		}
		
		Action action = buildStopXferAction(fileID, status);
		dispatcher.insertAction(action);
		
		return StatusCode.OK;
	}

	/**
	 * buildStopXferAction()
	 * is a private function called by pauseFile(). This function will build a STOP_XFER
	 * action to be sent to the file sender. This will notify the sender that the receiver
	 * wishes to pause the transfer.
	 * <p>
	 * Note: all temporary files are saved in memory so the file transfer can be resumed
	 * at a later time.
	 * 
	 * @param fileID  specifies the fileId for the file the receiver wishes to pause
	 * @param status  instance of file status object
	 * @return STOP_XFER action
	 */
	private Action buildStopXferAction(byte[] fileID, FileStatus status)
	{
		Action action = new Action();
		action.actionType = ActionType.STOP_XFER;
		action.parameters.add(fileID);
		action.peer = status.peer;
		return action;
	}

	/** 
	 * cancelFile()
	 * is called when the receiver wishes to cancel a file transfer. The function first checks
	 * to see if the file ID matches a current file transfer. If not, the function returns
	 * BAD_FILE_ID. If so, the function deletes the corresponding file status object so all
	 * file chunks received after the cancel is executed are disregarded. The function will
	 * also delete all temporary files. 	  			 
	 * 
	 * @param fileID  file to cancel receiving
	 * @return OK or BAD_FILE_ID
	 */
	public int cancelFile(byte[] fileID)
	{
		FileStatus status = getFileStatus(fileID);
		
		int statusCode = pauseFile(fileID);
		
		if (statusCode == StatusCode.BAD_FILE_ID)
		{
			return StatusCode.BAD_FILE_ID;
		}	
		
		synchronized(fileStatuses)
		{
			fileStatuses.remove(Arrays.toString(fileID));
		}		
		
		String path = new File(status.saveFilePath, status.saveFileName).getAbsolutePath();
		fsa.delete(path);
		
		return StatusCode.OK;
	}
	
	
	/**
	 * getFileStatus()
	 * used to safely get a FileStatus object from the fileStatuses HashMap
	 * 
	 * @param fileID  the file ID of the FileStatus object
	 * @return  the FileStatus object or null if not present
	 */
	private FileStatus getFileStatus(byte[] fileID)
	{				
		synchronized(fileStatuses)
		{
			return fileStatuses.get(Arrays.toString(fileID));				
		}			
	}
	
	/**
	 * fireCompletedListener()
	 * Safely triggers the registered file completed listener to notify the user
	 * that a file transfer operation has been completed.
	 * 
	 * @param filename  name of file completed
	 * @param statusCode  status of completion
	 */
	private void fireCompletedListener(String filename, int statusCode)
	{
		synchronized (completedListenerLock)
		{
			if (fileCompletedListener != null)
			{
				fileCompletedListener.fileCompleted(filename, statusCode);
			}
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
		fileStatuses.clear();		
	}
}
