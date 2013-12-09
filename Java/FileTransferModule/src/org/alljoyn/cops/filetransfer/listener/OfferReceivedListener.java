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
 * The OfferReceivedListener can be used by developers to determine the default
 * action taken when a file offer is received from a session peer. The default
 * behavior is to deny all file offers. The acceptOfferedFile() method should be
 * implemented to return to true if you wish to accept files offered by remote
 * session peers.
 * <p>
 * See {@link org.alljoyn.cops.filetransfer.FileTransferModule#setOfferReceivedListener}
 */
public interface OfferReceivedListener
{
	/**
	 * acceptOfferedFile()
	 * is triggered when a file offer is received. Return true if the offer
	 * is to be accepted, or false to reject it. If the listener is not
	 * implemented, the offer will be rejected by default.
	 * 
	 * @param file  the file descriptor for the file being offered
	 * @param peer  the peer offering the file
	 * @return return true to accept, false to reject
	 */
	public boolean acceptOfferedFile(FileDescriptor file, String peer);
}
