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
 * The UnannouncedFileRequestListener is required if the developer wishes
 * to allow session peers to request files that have not been explicitly
 * announced or shared. The default behavior is to deny all requests for
 * files that have not been announced or shared.
 * <p>
 * See {@link org.alljoyn.cops.filetransfer.FileTransferModule#setUnannouncedFileRequestListener}
 */
public interface UnannouncedFileRequestListener 
{
	/**
	 * allowUnannouncedFileRequests()
	 * is required for use of the requestOffer() method. When a file request is received
	 * for an unannounced file, this listener is triggered. Return true to allow the request, 
	 * false to deny the request.
	 * 
	 * @param filePath  path of file being requested
	 * @return  true to accept the request, false to reject the request
	 */
	public boolean allowUnannouncedFileRequests(String filePath);
}
