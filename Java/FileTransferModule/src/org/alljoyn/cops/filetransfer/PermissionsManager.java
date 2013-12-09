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
import java.util.HashMap;

import org.alljoyn.cops.filetransfer.data.FileDescriptor;

/**
 * The Permissions Manager is responsible for storing all of the files we have made
 * available to remote session peers through formal announcements or informal offers.
 * The Permissions Manager is also responsible for storing all of the files that have
 * been announced or offered to us by remote session peers. In addition to storing
 * files, the Permissions Manager is responsible for performing lookups when provided
 * a specific file ID and returning lists of the files stored in each of the hash maps.
 * This class is implemented as a singleton since only one instance of this class
 * is needed but many modules must interact with the Permissions Manager. The static
 * function getInstance() returns the single instance of the Permissions Manager when 
 * needed.
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class.
 */
public class PermissionsManager
{
	/** Member Variables **/
	private FileSystemAbstraction fsa;
	private HashMap<String, FileDescriptor> announcedLocalFilesList;
	private HashMap<String, FileDescriptor> offeredLocalFilesList;	
	private HashMap<String, FileDescriptor[]> announcedRemoteFileList;	
	private HashMap<String, ArrayList<FileDescriptor>> offeredRemoteFileList;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * PermissionsManager()
	 * constructs an instance of the Permissions Manager class. The constructor
	 * does not take any parameters and initializes member variables.
	 */
	public PermissionsManager()
	{
		fsa = FileSystemAbstraction.getInstance();
				
		announcedLocalFilesList = new HashMap<String, FileDescriptor>();
		offeredLocalFilesList = new HashMap<String, FileDescriptor>();
		announcedRemoteFileList = new HashMap<String, FileDescriptor[]>();
		offeredRemoteFileList = new HashMap<String, ArrayList<FileDescriptor>>();
	}
	
	/*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/
	
	/**
	 * addAnnouncedLocalFiles()
	 * is called by the AnnouncementManager when newly announced files need to be
	 * stored. This function stores each file descriptor in the announced files hash
	 * map where the key is the file ID of each file and the value is the file
	 * descriptor.
	 * 
	 * @param descriptors  array of announced files
	 */
	public void addAnnouncedLocalFiles(FileDescriptor[] descriptors)
	{			
		synchronized (announcedLocalFilesList)
		{
			for	(FileDescriptor descriptor : descriptors)
			{
				announcedLocalFilesList.put(Arrays.toString(descriptor.fileID), descriptor);			
			}
		}						
	}
	
	/**
	 * removeAnnouncedLocalFiles()
	 * is called when files need to be unannounced. This function takes an array of paths
	 * that specify which files need to be unannounced and searches the announced local 
	 * files list for matches. When matches are found they are removed from the announced
	 * files list. Lastly, the function returns an array of the paths that failed to unannounce
	 * successfully. 
	 * 
	 * @param paths  specifies the array of paths to be unannounced
	 * @return array of paths that failed to unannounce
	 */
	public ArrayList<String> removeAnnouncedLocalFiles(ArrayList<String> paths)
	{
		ArrayList<FileDescriptor> announcedLocalFiles = getAnnouncedLocalFiles();
		
		for (int i = 0; i < announcedLocalFiles.size(); i++)
		{
			FileDescriptor descriptor = announcedLocalFiles.get(i);
			
			String filePath = fsa.buildPathFromDescriptor(descriptor);
			
			if (paths.contains(filePath))
			{
				synchronized (announcedLocalFilesList)
				{
					announcedLocalFilesList.remove(Arrays.toString(descriptor.fileID));
				}				
				paths.remove(filePath);
			}		
		}
		
		return paths;
	}
	
	/**
	 * updateRemoteAnnouncedFiles()
	 * is called when we receive announcements from remote session peers. The array of descriptors
	 * is stored in a hash map containing all of the available remote files organized with the peer
	 * as the key.
	 * 
	 * @param descriptors  specifies an array of available remote files
	 * @param peer  specifies the peer who sent the array of files
	 */
	public void updateAnnouncedRemoteFiles(FileDescriptor[] descriptors, String peer)
	{
		synchronized(announcedRemoteFileList)
		{
			announcedRemoteFileList.put(peer, descriptors);
		}
	}
	
	/**
	 * addOfferedLocalFileDescriptor()
	 * is called when we need to add a new file descriptor to the offered local files hash
	 * map. The offered local files hash map contains records of all the files that we have 
	 * 
	 * @param descriptor  specifies the descriptor of the file to store
	 */
	public void addOfferedLocalFile(FileDescriptor descriptor)
	{		
		synchronized (offeredLocalFilesList)
		{
			offeredLocalFilesList.put(Arrays.toString(descriptor.fileID), descriptor);
		}
	}
	
	/**
	 * addOfferedRemoteFile()
	 * is called when we need to add a file descriptor to the offered remote files hash map.
	 * The offered remote files hash map contains the records of files that we have either
	 * directly requested or been offered to us by a remote session peers. 
	 * 
	 * @param descriptor  specifies the descriptor an offered remote file
	 * @param peer  specifies the owner of the file
	 */
	public void addOfferedRemoteFile(FileDescriptor descriptor, String peer)
	{				
		synchronized (offeredRemoteFileList)
		{
			ArrayList<FileDescriptor> offeredRemoteFiles = offeredRemoteFileList.get(peer);

			if (offeredRemoteFiles == null)
			{
				offeredRemoteFiles = new ArrayList<FileDescriptor>();
				offeredRemoteFileList.put(peer, offeredRemoteFiles);
			}

			offeredRemoteFiles.add(descriptor);
		}
	}
	
	/**
	 * getFileID()
	 * searches the list of available remote files that match the provided peer and file path.
	 * If a match is found, the file ID is returned. Otherwise this function returns null.
	 * 
	 * @param peer  specifies the owner of the file
	 * @param filePath  specifies the absolute path of the file
	 * @return the file ID, or null
	 */
	public byte[] getFileID(String peer, String filePath)
	{		
		ArrayList<FileDescriptor> knownFiles = getAvailableRemoteFiles();
		
		for (FileDescriptor fileDescriptor : knownFiles)
		{
			String path = fsa.buildPathFromDescriptor(fileDescriptor);
			
			if((peer.equals(fileDescriptor.owner)) && (filePath.equals(path)))
			{
				return fileDescriptor.fileID;
			}
		}		
		return null;	
	}
	
	/**
	 * getAnnouncedLocalFiles()
	 * returns to the user an array of file descriptors that describes all of the files that
	 * have been announced to remote session peers.
	 * 
	 * @return array of files we have announced to session peers
	 */
	public ArrayList<FileDescriptor> getAnnouncedLocalFiles()
	{
		ArrayList<FileDescriptor> announcedLocalFileList;
		
		synchronized (announcedLocalFilesList)
		{
			announcedLocalFileList = new ArrayList<FileDescriptor>(announcedLocalFilesList.values());
		}
		
		return announcedLocalFileList;		
	}
	
	/**
	 * getOfferedLocalFiles()
	 * returns to the user an array of file descriptors that describes all of the files that
	 * have either been offered to or directly requested by remote session peers.
	 * 
	 * @return array of all offered files
	 */
	public ArrayList<FileDescriptor> getOfferedLocalFiles()
	{
		ArrayList<FileDescriptor> offeredLocalFiles;
		
		synchronized (offeredLocalFilesList)
		{
			offeredLocalFiles = new ArrayList<FileDescriptor>(offeredLocalFilesList.values());
		}
		
		return offeredLocalFiles;
	}
	
	/**
	 * getAvailableRemoteFiles()
	 * returns to the user an array of file descriptors that describes all of the files that
	 * have been announced to us by remote session peers.
	 * 
	 * @return array of all available remote files
	 */
	public ArrayList<FileDescriptor> getAvailableRemoteFiles()
	{
		ArrayList<FileDescriptor> allKnownFiles = new ArrayList<FileDescriptor>();
		
		addAnnouncedRemoteFiles(allKnownFiles);
		addOfferedRemoteFiles(allKnownFiles);		
		
		return allKnownFiles;
	}
	
	/**
	 * addAnnouncedRemoteFiles()
	 * is a private helper function called by getAvailableRemoteFiles(). This function
	 * adds all of the file descriptors stored in announced remote files hash map to the
	 * provided allKnownFiles array.
	 * 
	 * @param allKnownFiles  specifies a list to add the file descriptors
	 */
	private void addAnnouncedRemoteFiles(ArrayList<FileDescriptor> allKnownFiles)
	{		
		synchronized(announcedRemoteFileList)
		{		
			for (FileDescriptor[] descriptorArray : announcedRemoteFileList.values())
			{
				for (FileDescriptor fd : descriptorArray)
				{
					if (!allKnownFiles.contains(fd))
					{
						allKnownFiles.add(fd);
					}				
				}
			}
		}
	}

	/**
	 * addAnnouncedRemoteFiles()
	 * is a private helper function called by getAvailableRemoteFiles(). This function
	 * adds all of the file descriptors stored in offered remote files hash map to the
	 * provided allKnownFiles array.
	 * 
	 * @param allKnownFiles  specifies a list to add the file descriptors
	 */
	private void addOfferedRemoteFiles(ArrayList<FileDescriptor> allKnownFiles)
	{		
		synchronized(offeredRemoteFileList)
		{
			for (ArrayList<FileDescriptor> descriptorArrayList : offeredRemoteFileList.values())
			{
				for (FileDescriptor fd : descriptorArrayList)
				{
					if (!allKnownFiles.contains(fd))
					{
						allKnownFiles.add(fd);
					}
				}
			}
		}
	}
	
	/**
	 * getLocalFileDescriptor()
	 * is called by the SendManager and returns the file descriptor that matches the specified
	 * file ID.
	 * 
	 * @param fileID  specifies the file ID of a file
	 * @return file descriptor matching the file ID, null otherwise
	 */
	public FileDescriptor getLocalFileDescriptor(byte[] fileID)
	{
		FileDescriptor descriptor;
		
		synchronized (announcedLocalFilesList)
		{
			descriptor = announcedLocalFilesList.get(Arrays.toString(fileID));
		}
		
		if (descriptor == null)
		{
			synchronized(offeredLocalFilesList)
			{
				descriptor = offeredLocalFilesList.get(Arrays.toString(fileID));
			}
		}
		
		return descriptor;
	}
	
	/**
	 * isAnnounced()
	 * tests to see if the provided file ID matches a file stored in the announced local
	 * files hash map. If a match is found the function returns true. Otherwise the function
	 * will return false.
	 * 
	 * @param fileID  specifies the file ID of a file
	 * @return boolean
	 */
	public boolean isAnnounced(byte[] fileID)
	{
		synchronized(announcedLocalFilesList)
		{
			return announcedLocalFilesList.containsKey(Arrays.toString(fileID));
		}
	}
	
	/**
	 * isShared()
	 * tests to see if the provided file ID matches a file stored in the offered local
	 * files hash map. If a match is found the function returns true. Otherwise the function
	 * will return false.
	 * 
	 * @param fileID  specifies the file ID of a file
	 * @return boolean
	 */
	public boolean isShared(byte[] fileID)
	{
		synchronized(offeredLocalFilesList)
		{
			return offeredLocalFilesList.containsKey(Arrays.toString(fileID));
		}
	}
	
	/**
	 * getKnownFileDescriptor()
	 * is called by the ReceiveManager and returns the file descriptor that matches the
	 * provided file ID and peer parameters. If a match is not found, this function will
	 * return null. 
	 * 
	 * @param fileID  specifies the ID of the file being requested
	 * @param peer  specifies the owner of the file
	 * @return file descriptor matching the file ID, null otherwise
	 */
	public FileDescriptor getKnownFileDescriptor(byte[] fileID, String peer)
    {
		String myFileId = Arrays.toString(fileID);

		synchronized(announcedRemoteFileList)
		{
			FileDescriptor[] files = announcedRemoteFileList.get(peer);

			if (files != null)
			{
				for (int i = 0; i < files.length; i++)
				{
					if (myFileId.equals(Arrays.toString(files[i].fileID)))
					{
						return files[i];
					}
				}                          
			}
		}

		synchronized(offeredRemoteFileList)
		{
			ArrayList<FileDescriptor> files = offeredRemoteFileList.get(peer);

			if (files != null)
			{
				for (FileDescriptor fd : files)
				{
					if (myFileId.equals(Arrays.toString(fd.fileID)))
					{
						return fd;
					}
				}
			}
		}

		return null;
    }
	
	/**
	 * resetState()
	 * is called by the File Transfer Module when specifies a new AllJoyn session to be used.
	 * This function is passed the new bus ID of the bus attachment an must iterate
	 * over the files stored in the announced and offered file lists and overwrite the owener
	 * field in each file descriptor to the new bus ID.
	 * <p>
	 * Note: in the case where the user calls uninitialize() on the FTC, the localBusID parameter
	 * will be null.
	 * 
	 * @param localBusID  specifies the bus ID of the bus attachment, can be null.
	 */
	public void resetState(String localBusID)
	{
		synchronized(announcedLocalFilesList)
		{
			for (FileDescriptor descriptor : announcedLocalFilesList.values())
			{
				descriptor.owner = localBusID;
			}
		}
		
		synchronized(offeredLocalFilesList)
		{
			for (FileDescriptor descriptor : offeredLocalFilesList.values())
			{
				descriptor.owner = localBusID;
			}
		}
	}
}
