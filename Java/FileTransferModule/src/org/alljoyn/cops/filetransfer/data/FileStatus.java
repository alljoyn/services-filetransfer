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

package org.alljoyn.cops.filetransfer.data;

/**
 * The File Status object is used by the File Transfer Manager and the File
 * Receive Manager to monitor the progress of files that are being sent and
 * received. From the senders perspective, the file status will show the file
 * ID of the file being transferred, the length of the file, the peer to send
 * the file to, how many bytes have already been sent, and the maximum chunk
 * length specified by the receiver. From the receivers perspective, all of 
 * same data is monitored by the receiver with a few additions. The receiver
 * also keeps track of the save file name and the save file path. This is 
 * essential so the file chunks can be appended to the correct file.  
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
 */
public class FileStatus 
{
	// Member Variables
	public byte[] fileId;	
	public int startByte;	
	public int length;	
	public String peer;	
	public int numBytesSent;	
	public String saveFileName;
	public String saveFilePath;	
	public int chunkLength;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * FileStatus()
	 * creates an instance of the FileStatus class.
	 */
	public FileStatus()
	{
		//Intentionally left blank
	}
}
