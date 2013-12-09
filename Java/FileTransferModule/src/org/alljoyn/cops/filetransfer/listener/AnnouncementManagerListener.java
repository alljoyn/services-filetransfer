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
 * The AnnouncementManagerListener is an internal listener that is used by the
 * Receiver to notify the AnnouncementManager that various events have occurred.
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
 */
public interface AnnouncementManagerListener 
{
	/**
	 * handleAnnounced()
	 * is triggered by the Receiver when a normal announcement is received from
	 * a remote session peer.
	 * 
	 * @param fileList  specifies the list of announced files
	 * @param peer  specifies the peer who sent the announcement
	 */
	public void handleAnnounced(FileDescriptor[] fileList, String peer);
	
	/**
	 * handleAnnouncementRequest()
	 * is triggered by the Receiver when an announcement request is received from
	 * a remote session peer.
	 * 
	 * @param peer  specifies the peer that made the announcement request
	 */
	public void handleAnnouncementRequest(String peer);
}
