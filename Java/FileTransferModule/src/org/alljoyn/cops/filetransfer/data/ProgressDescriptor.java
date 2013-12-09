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
 * The Progress Descriptor is utilized when the user requests file transfer status
 * updates from either the sender or the receiver. This class will outline all of
 * the files that are being transferred by specifying how many bytes have already been
 * sent or received and give the total length of the file. This will allow the user to
 * see the current progress of each file transfer.
 * <p>
 * See {@link org.alljoyn.cops.filetransfer.FileTransferModule#getSendingProgressList}
 * and {@link org.alljoyn.cops.filetransfer.FileTransferModule#getReceiveProgressList}
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
 */
public class ProgressDescriptor 
{
	// Class Enumerator
	public enum State 
	{ 
		IN_PROGRESS, 
		PAUSED, 
		TIMED_OUT 
	}
	
	// Member Variables
	public byte[] fileID;	
	public State state;	
	public int bytesTransferred;	
	public int fileSize;
	
	/*------------------------------------------------------------------------*
     * Constructor
     *------------------------------------------------------------------------*/
	/**
	 * ProgressDescriptor()
	 * creates an instance of the ProgressDescriptor class.
	 */
	public ProgressDescriptor()
	{
		//Intentionally left blank
	}
}
