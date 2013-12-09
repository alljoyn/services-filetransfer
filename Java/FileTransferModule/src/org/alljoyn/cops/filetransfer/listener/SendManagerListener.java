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

package org.alljoyn.cops.filetransfer.listener;

/**
 * The SendManager Listener is an internal listener that is used by the OfferManager,
 * the bus object, and the Receiver to notify the SendManager that various events 
 * have occurred.  
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
 */
public interface SendManagerListener 
{
	/**
	 * sendFile()
	 * is triggered by the bus object or the Offer Manager to notify the Send
	 * Manager to begin sending the file matching the specified file ID.
	 * 
	 * @param fileID  specifies the fileId of the file the data belongs to
	 * @param startByte  specifies the starting byte of the chunk relative to the file
	 * @param length  specifies the length of data chunk
	 * @param peer  specifies the peer to send the file to
	 * @param maxChunkLength  specifies the maximum chunk size
	 * @return OK or BAD_FILE_ID
	 */
	public int sendFile(byte[] fileID, int startByte, int length, String peer, int maxChunkLength);
	
	/**
	 * dataSent()
	 * is triggered by the dispatcher when a data chunk action has been 
	 * sent to the transmitter and to notify the Send Manager to queue the
	 * next data chunk if available.
	 */
	public void dataSent();
	
	/**
	 * handleStopDataXfer()
	 * is triggered by the Receiver when a stopDataXfer signal is received from the
	 * file receiver. This callback notifies the Send Manager to stop sending the file
	 * matching the specified file ID.
	 * 
	 * @param fileID  specifies the ID of the file
	 * @param peer  specifies the peer receiving the file
	 */
	public void handleStopDataXfer(byte[] fileID, String peer);
}
