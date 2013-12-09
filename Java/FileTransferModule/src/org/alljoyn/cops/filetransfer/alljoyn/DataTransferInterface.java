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

package org.alljoyn.cops.filetransfer.alljoyn;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;

/**
 * The Data Transfer Interface specifies the AllJoyn signals and methods that are
 * associated with file transfer. The main behaviors of this interface include:
 * file requests, sending chunks of a requested file, and canceling file transfers
 * already in progress.
 * <p>
 * Note: This interface is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
*/
@BusInterface(name="org.alljoyn.Cops.DataTransfer")
public interface DataTransferInterface
{
	/**
	 * requestData()
	 * is specified as an AllJoyn method and is used to request files from a remote
	 * session peer. The method handler will delegate to the SendManager to handle
	 * the file request. 			 
	 *  
	 * @param fileID  specifies the file ID of the requested file
	 * @param startByte  specifies the starting byte of the request relative to the file
	 * @param length  specifies the length of file request in bytes
	 * @param maxChunkLength  specifies the max chunk length
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	@BusMethod
	public int requestData(byte[] fileID, int startByte, int length, int maxChunkLength) throws BusException;	
	
	/**
	 * dataChunk()
	 * is specified as an AllJoyn signal and is used to send file chunks to remote session
	 * peers. This signal is usually sent in response to a file request. The file chunk is
	 * then passed to the ReceiveManager to be merged into the temporary file stored in memory.	  			 
	 * 
	 * @param fileID  specifies the fileId of the file the data belongs to
	 * @param startByte  specifies the starting byte of the chunk relative to the file
	 * @param chunkLength  specifies the length of data chunk
	 * @param chunk  specifies the file data chunk
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	@BusSignal
	public void dataChunk(byte[] fileID, int startByte, int chunkLength, byte[] chunk) throws BusException;	

	/**
	 * stopDataXfer()
	 * is specified as an AllJoyn signal and is when the file receiver wishes to pause or cancel
	 * the current file transfer. This function immediately notifies the SendManager to 
	 * stop transfer of the file matching the fileId.  			 
	 *  
	 * @param fileID  specifies the ID of file being transferred
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	@BusSignal
	public void stopDataXfer(byte[] fileID) throws BusException;

	/**
	 * dataXferCancelled()
	 * is specified as an AllJoyn signal and is used when the sender wishes to cancel the
	 * current file transfer. This signal is sent to the receiver notifying them the current
	 * file transfer has been canceled.
	 *  
	 * @param fileID  specifies the ID of file being transferred
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	@BusSignal
	public void dataXferCancelled(byte[] fileID) throws BusException;
}