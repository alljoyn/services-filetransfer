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

import java.util.ArrayList;

/**
 * The FileAnnouncementSentListener can be utilized so the developer is notified
 * when the announce function has finished and sent an announcement to session 
 * peers. Since the announce function is executed on a separate thread (to avoid
 * hanging the application thread due to time intensive hashing of the files), the
 * user can implement this listener to be notified when the announce function has
 * finished executing. This listener returns an array containing the paths
 * of the files that failed to be announced. This listener is entirely optional and
 * will not impede normal file announcement functionality.
 * <p>
 * See {@link org.alljoyn.cops.filetransfer.FileTransferModule#setFileAnnouncementSentListener}
 */
public interface FileAnnouncementSentListener 
{
	/**
	 * announcementSent()
	 * is triggered when the announce function has finished executing. Notifies the user
	 * which file paths, if any, failed to be announced.
	 * 
	 * @param failedPaths  array of file paths that failed to be announced
	 */
	public void announcementSent(ArrayList<String> failedPaths);
}
