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

import org.alljoyn.cops.filetransfer.data.FileDescriptor;

/**
 * The OfferListener is an internal listener that is used by the File Transfer
 * Bus Object to see if there are any pending offers that are awaiting response
 * from a remote session peer. This listener is also used to respond to offers
 * received from remote session peers. 
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
 */
public interface OfferManagerListener
{
	/**
	 * handleFileRequest()
	 * is triggered by the bus object to notify the Offer Manager that a file
	 * request has been received that is in response to a pending offer.
	 * 
	 * @param fileID  file ID of the file being requested
	 * @param startByte  starting byte of the request relative to the file
	 * @param length  length of request in bytes
	 * @param peer  specifies the peer making the file request
	 * @param maxChunkLength  specifies the max chunk size
	 * @return OK or BAD_FILE_ID
	 */
	public int handleFileRequest(byte[] fileID, int startByte, int length,
			String peer, int maxChunkLength);
	
	/**
	 * handleOffer()
	 * is triggered by the bus object to notify the Offer Manager when a file offer
	 * has been received from a remote session peer.
	 * 
	 * @param file  specifies the file descriptor for the file being offered
	 * @param peer  specifies the peer offering the file
	 * @return OFFER_ACCEPTED or OFFER_REJECTED
	 */
	public int handleOffer(FileDescriptor file, String peer);
	
	/**
	 * isOfferPending()
	 * is triggered by the bus object to query the Offer Manager to see if
	 * the file ID from the most recent file request matches the file ID for
	 * a pending offer. 
	 * 
	 * @param fileID  specifies the file ID of the file being requested
	 * @return boolean
	 */
	public boolean isOfferPending(byte[] fileID);
}
