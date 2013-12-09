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
 * The FileCompletedListener can be utilized so that the developer can be notified when
 * a file transfer has been completed. This event will be triggered by the 
 * ReceiveManager when a file transfer has completed or been cancelled.
 * <p>
 * See {@link org.alljoyn.cops.filetransfer.FileTransferModule#setFileCompletedListener}
 */
public interface FileCompletedListener
{
	/**
	 * fileCompleted()
	 * is triggered when a transmission has been completed. Will be
	 * triggered on cancellation as well as successful completion.
	 * 
	 * @param filename  the name of the file that completed transmission
	 * @param statusCode  OK if completely transfered, CANCELLED otherwise 
	 */
	public void fileCompleted(String filename, int statusCode);	
}