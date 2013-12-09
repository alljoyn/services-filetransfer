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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.utility.Logger;

/**
 * The File System Abstraction (FSA) is one of the main modules of the File Transfer Module. 
 * The FSA plays an integral role in calculating the file ID for each file that needs to be
 * announced. The file ID is a 20 byte array that is determined by the SHA-1 hash of the file
 * contents. The FSA is also responsible returning a specified file chunk to the Send
 * Manager. Additionally, when a file chunk is received, it is passed to the FSA so the file
 * can be reassembled. The main responsibility of the FSA is to hide the details of the local
 * file system. This class is implemented as a singleton since only one instance of this class
 * is needed but many modules must interact with the FSA. The static function getInstance()
 * returns the single instance of the FSA when needed.
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link FileTransferModule} class.
 */
public class FileSystemAbstraction 
{
	//Internal Static class to help store file hashes in a file
    private static class FileAttributes implements Serializable
    {
        private static final long serialVersionUID = 1L;
        
        public byte[] fileID;
        public long lastModified;
        
        public FileAttributes(byte[] fileID, long lastModified)
        {
            this.fileID = fileID;
            this.lastModified = lastModified;
        }
    };
    
		/** Member Variables **/
    private static FileSystemAbstraction instance;
	private File attributeCacheFile;
	private Map<File, FileAttributes> attributeCache;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * FileSystemAbstraction()
	 * constructs an instance of the File System Abstraction class. The constructor
	 * does not take any parameters and does initializes member variables.
	 */
	protected FileSystemAbstraction() 
	{
	    attributeCacheFile = null;
	    attributeCache = null;
	}
	
	/*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/
	/**
	 * getInstance()
	 * is a static function that returns the single instance of the File System 
	 * Abstraction. The first time this function is called the instance of the 
	 * FSA will be created.
	 * 
	 * @return instance of the File System Abstraction class
	 */
	public static FileSystemAbstraction getInstance()
	{
		if (instance == null)
		{
			instance = new FileSystemAbstraction();			
		}
		return instance;
	}
	
    /**
     * setCacheFile()
     * allows the user to specify the path for a file that will be used to store the hash
     * value of files that are made available to AllJoyn session peers. Caching is helpful 
     * to avoid recalculating the hash value of the same file multiple times, which for 
     * large files can be a time consuming operation. The user must call setCacheFile()
     * with a valid file path to enable caching. This function will call a helper function
     * called setCacheFile() that will handle the process for setting the new cache file. 
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
        setCacheFile(path != null ? new File(path) : (File)null);	        
	}
	
    /**
     * setCacheFile()
     * is a public helper function called by setCacheFile(). This function will handle the
     * process of setting the new cache file by first writing the existing table of file
     * hash values to the old cache file. This function will then read the contents (if
     * available) of the new cache file and store any valid hash values in the attribute
     * cache class variable.
     * <p>
     * Note: if the file parameter is null, caching will be disabled. The class level variables
     * attributeCache and attributeCacheFile will be set to null.
     * 
     * @param file  points to the new file used for caching, can be null to disable caching
     */
	public void setCacheFile(File file)
	{
	    if (attributeCacheFile != null)
        {
	        if(!attributeCacheFile.equals(file))
    	    {
	        	//Writes the current cache contents to the old cache file
	            writeCacheToFile(attributeCacheFile);	        
    	    }
        }
	        
        if (file != null)
        {
            if (!file.equals(attributeCacheFile))
            {
            	//Reads the contents (if any) of the new cache file
                readCacheFromFile(file);
            }
        }
        else
        {
            attributeCache = null;
        }
        
        attributeCacheFile = file;
	}
	
    /**
     * cleanCacheFile()
     * is called by the File Transfer Module when the user wishes to clean the current
     * cache file. This function will iterate over the contents of the cache file and remove
     * any hashes for files that no longer exist or have been modified since the last hash
     * operation occurred. 
     */
	public void cleanCacheFile()
	{
        if (attributeCache != null)
        {
            Iterator<Map.Entry<File, FileAttributes>> i = attributeCache.entrySet().iterator();
            
            while(i.hasNext())
            {
                Map.Entry<File, FileAttributes> entry = (Map.Entry<File, FileAttributes>)i.next();
                File file = entry.getKey();
                
                // If the file does not exist, or if it has been changed, then remove it
                // from the cache.
                if ((!file.exists()) || (file.lastModified() != entry.getValue().lastModified))
                {
                    i.remove();
                }
            }
        
            // Write the updated cache
            writeCacheToFile(attributeCacheFile);
        } 
	}
	
    /**
     * writeCacheToFile()
     * is a private helper function that is used to write the current hash data to cache
     * file. This function is used by setCacheFile() and cleanCacheFile().
     * 
     * 
     * @param file  specifies the file to write the current hash value data
     */
	private void writeCacheToFile(File file)
	{
	    if (attributeCache != null)
	    {
    	    if (file != null && file.exists() && file.canWrite())
    	    {
                ObjectOutputStream oos;
                
                try 
                {
                    oos = new ObjectOutputStream(new FileOutputStream(file));
                    oos.writeObject(attributeCache);
                    oos.flush();
                    oos.close(); 
                } 
                catch (Exception e) 
                {
                    attributeCache = null;
                    Logger.log(e.toString());
                } 
    	    }
	    }
	}
	
	/**
	 * readCacheFromFile()
	 * is a private helper function that is used to read the stored hash data (if
	 * available) and store it in the attributeCache class variable. This function
	 * is used by setCacheFile() when the user wishes to specify a new cache file.
	 * 
	 * @param file  specifies the to read cache data from
	 */
	@SuppressWarnings("unchecked")
    private void readCacheFromFile(File file)
	{
        if (file != null)
        {
            if (!file.exists())
            {
                try 
                {
                    file.createNewFile();
                } 
                catch (Exception e) 
                {
                    Logger.log(e.toString());
                }
            }
            
            if (file.exists() && file.canRead())
            {
                ObjectInputStream ois;
                
                try 
                {
                    ois = new ObjectInputStream(new FileInputStream(file));
                    attributeCache = (HashMap<File, FileAttributes>)ois.readObject(); 
                    ois.close();
                } 
                catch (Exception e)
                {
                    attributeCache = new HashMap<File, FileAttributes>();
                    Logger.log(e.toString());
                } 
            }
        }
	}
	
	/**
	 * getFileInfo()
	 * builds an array of file descriptors for paths listed in the pathList parameter. The function
	 * will test to make sure the file path exists and has sufficient read permissions. If it does not
	 * have sufficient permissions or exist, that path will be added to the failedPaths array. Additionally,
	 * if one of the paths specifies a directory, this function will recursively get all files and
	 * sub-folder contents of the directory and create file descriptors for each file. This function
	 * will return an array of file descriptors that specifies which files can be successfully 
	 * announced to session peers.
	 * 
	 * @param pathList  array of paths (files or directories) to be announced
	 * @param failedPaths  empty array for failed file paths
	 * @param localBusID  specifies bus ID of the local user
	 * @return array of FileDescriptors 
	 */
	public FileDescriptor[] getFileInfo(ArrayList<String> pathList, ArrayList<String> failedPaths, 
			String localBusID) 
	{
		ArrayList<FileDescriptor> fileList = new ArrayList<FileDescriptor>();

		for (String path : pathList)
		{
			File file = new File(path);
			
			if ((!file.exists()) || (!file.canRead()))
			{
				failedPaths.add(path);
				continue;
			} 
			
			if (file.isFile())
			{
                addFile(fileList, file, localBusID, file.getParent(), failedPaths, path);
			} 
			else
			{
				String rootSharePath = file.getAbsolutePath();
				
				LinkedList<File> iterQueue = new LinkedList<File>();
				iterQueue.add(file);
				
				do 
				{
					File poppedFile = iterQueue.removeFirst();
					
					for (File child : poppedFile.listFiles())
					{
						if (!child.canRead())
						{
							failedPaths.add(child.getAbsolutePath());
							continue;
						}
						if (child.isDirectory())
						{ 
							iterQueue.add(child);
						} 
						else 
						{
						    addFile(fileList, child, localBusID, rootSharePath, failedPaths, child.getAbsolutePath());
						}
					}
				} while (!iterQueue.isEmpty());
			}
		}
		
		FileDescriptor[] files = fileList.toArray(new FileDescriptor[fileList.size()]);
		return files;
	}
	
    /**
     * addFile()
     * Helper method to attempt to create a file descriptor for the specified file. If
     * successful, the newly created file descriptor is added to the specified list of
     * valid file descriptors. If the file descriptor could not be created, the specified
     * failed path is added to the specified list of failed paths.
     * 
	 * @param fileList The current list of valid file descriptors
	 * @param file The file for which a file descriptor is to be created
	 * @param localBusID The bus ID to store in the file descriptor
	 * @param sharedPath The shared path to store in the file descriptor
	 * @param failedPaths The current list of failed paths
	 * @param failedPath The failed path to add to the failed list
	 */
	private void addFile(ArrayList<FileDescriptor> fileList, File file, String localBusID, String sharedPath, ArrayList<String> failedPaths, String failedPath)
	{
	    byte[] fileID = null;
	    FileDescriptor fileDescriptor = null;
	    FileAttributes fileInfo = attributeCache != null ? attributeCache.get(file) : null;
	    
	    // See if we already know the file ID for this file
	    if (fileInfo != null)
	    {
	        if (fileInfo.lastModified == file.lastModified())
	        {
	            fileID = fileInfo.fileID;
	        }
	    }

	    // Try to create the file descriptor (uses the known file ID if available)
        try
        {
            fileDescriptor = buildDescriptor(file, fileID, localBusID, sharedPath);
        } 
        catch (Exception e)
        {
            fileDescriptor = null;
            Logger.log(e.toString());
        }
        
        // Update the data structures  
        if (fileDescriptor != null)
        {
            fileList.add(fileDescriptor);
            
            if ((fileID == null) && (attributeCache != null))
            {
                attributeCache.put(file, new FileAttributes(fileDescriptor.fileID, file.lastModified()));
                writeCacheToFile(attributeCacheFile);
            }
        }
        else
        {
            failedPaths.add(failedPath);
        }
	}

	/**
	 * buildDescriptor()
	 * takes the file, localBusID, and sharedPath input parameter and builds the file descriptor
	 * for the specified file. If this function throws one of the exceptions listed below, this
	 * will cause the path for the file to be added to the failedPath list. 
	 * 
	 * @param file  instance of the file being announced
	 * @param localBusID  specifies the local bus ID of the file owner
	 * @param sharedPath  specifies the shared path of the file
	 * @return file descriptor
	 * @throws FileNotFoundException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private FileDescriptor buildDescriptor(File file, byte[] fileID, String localBusID, String sharedPath) 
			throws FileNotFoundException, NoSuchAlgorithmException, IOException
	{
		FileDescriptor fd = new FileDescriptor();
		fd.owner = localBusID;
		fd.sharedPath = sharedPath;
		fd.size = (int) file.length();
		fd.filename = file.getName();
		
		int spLength = sharedPath.length();
		String parentPath = file.getParent();
		int parentLength = parentPath.length();

		if (parentLength == spLength)
		{
			fd.relativePath = "";
		} 
		else if (parentLength > spLength)
		{
			fd.relativePath = parentPath.substring(spLength);
		} 
		else 
		{
			Logger.log("File path is smaller than shared path length!");
		}
		
		// If the file ID was already known, we use it. Calculating the
		// ID can be time consuming, so we avoid it if possible.
		fd.fileID = fileID == null ? calculateId(file) : fileID;
		
		return fd;
	}
	
	/**
	 * calculateID()
	 * will calculate the SHA-1 hash of the specified file. The SHA-1 hash is used to denote the
	 * file ID for the file in the corresponding file descriptor. If one of the exceptions is thrown
	 * the path for the file will be added to failedPaths list.
	 * 
	 * @param file  instance of the file being announced
	 * @return file ID for the specified file
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private byte[] calculateId(File file) throws FileNotFoundException, IOException, NoSuchAlgorithmException
	{
		MessageDigest sha1 = null;

		sha1 = MessageDigest.getInstance("SHA-1");
		sha1.reset();

		FileInputStream fis = new FileInputStream(file);
		byte[] fileData = new byte[1024];
		int read = 0;
		
		while((read = fis.read(fileData)) != -1)
		{
			sha1.update(fileData, 0, read);
		}
		
		fis.close();
		return sha1.digest();
	}

	/**
	 * getChunk()
	 * opens the file at the specified file path and reads the number of bytes equal to the length parameter
	 * starting from the startOffset parameter. The bytes read from the file are stored in the chunk array. 
	 * The total number of bytes successfully read from the file are returned to the function caller.  
	 * 
	 * @param path  specifies the absolute file path of the file
	 * @param chunk  specifies the byte array where the data of the file will be copied to
	 * @param startOffset  specifies the starting byte offset of where to read the data
	 * @param length  specifies the number of bytes to read from the starting offset
	 * @return number of bytes read from file path
	 */
	public int getChunk(String path, byte[] chunk, int startOffset, int length) throws 
		FileNotFoundException, IOException
	{
		RandomAccessFile file = new RandomAccessFile(path, "r");
		file.skipBytes(startOffset);
		
		int status = file.read(chunk, 0, length);
		
		file.close();
		return status;
	}
	
	/**
	 * addChunk()
	 * is called when a file chunk is received during a file transfer. This function is responsible
	 * for appending the new data to the file beginning from the startOffset parameter. The function
	 * will return one if the addChunk operation is successful, and zero if the operation fails. 
	 * 
	 * @param  path  specifies the absolute file path of the file
	 * @param  chunk  specifies the byte array containing the data to be appended
	 * @param  startOffset  specifies the starting byte offset of where the data will be appended to the file
	 * @param  length  specifies the number of bytes to append from the starting offset
	 * @return  1 for success, 0 for fail
	 */
	public int addChunk(String path, byte[] chunk, int startOffset, int length) throws FileNotFoundException, IOException
	{
		int status = 0;
		
		File file = new File(path);
		if (!file.exists())
		{
			File parent = file.getParentFile();
			if (!parent.exists())
			{
				parent.mkdirs();
			}			
		}
		
		RandomAccessFile raFile = new RandomAccessFile(file, "rw");
		raFile.skipBytes(startOffset);		
		try
		{
			raFile.write(chunk, 0, length);
		} 
		catch (Exception e) 
		{
			Logger.log(e.toString());
			status = 0;
		}
		
		status = 1;
		raFile.close();
		return status;
	}
	
	/**
	 * delete()
	 * is called when the file at the specified path needs to be deleted. This function
	 * will be called when the receiver of the file transfer decides to cancel the transfer.
	 * If the delete operation is successful, the function returns one. If the delete 
	 * operation fails, the function returns zero.
	 * 
	 * @param path  specifies the absolute file path of the targeted file
	 * @return 1 if file successfully deleted. 0 otherwise
	 */
	public int delete(String path)
	{
		Logger.log("removing: " + path);
		
		File f = new File(path);
		boolean success = false;
		
		if (f.canWrite())
		{
			success = f.delete();
		}
		
		if (success)
		{
			Logger.log("deleted temp file");
			return 1;
		}			
		else
		{
			Logger.log("failed to delete temp file");
			return 0;			
		}			
	}
	
	/**
	* buildPathFromDescriptor()
	* takes the file descriptor input parameter and builds the absolute path of the file using
	* the data stored in the file descriptor.
	* 
	* @param fd  instance of a file descriptor 
	* @return file path built from the file descriptor
	**/
	public String buildPathFromDescriptor(FileDescriptor fd)
	{
		return fd.sharedPath + fd.relativePath + File.separator + fd.filename;
	}
	
	/**
	* isValid()
	* tests that the provided path exists and has read/write permissions. The function returns
	* a boolean value that provides the result of the test.
	* 
	* @param path  specifies the absolute of a file or directory  
	* @return boolean
	**/
	public boolean isValid(String path)
	{
		Logger.log("path = " + path);
		File file = new File(path);
		
		if (file.exists() && file.canRead() && file.canWrite())
		{
			return true;
		} 
		else
		{
			return false;
		}
	}
}
