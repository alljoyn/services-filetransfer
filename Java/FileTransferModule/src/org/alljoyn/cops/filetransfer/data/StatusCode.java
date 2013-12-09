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
 * Class StatusCode houses all of the status codes that can possibly be returned to
 * the user when functions are called on the File Transfer Module. Certain
 * status codes do not apply to all functions. The status codes also attempt to
 * explain to the user why a function failed since there are multiple ways each
 * function can fail.
 * <p>
 * Note: This class is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
 */
public class StatusCode
{
	//TODO - Address unused variables
	
	// Member Variables
	public static final int OK = 0;
	public static final int BAD_FILE_ID = 1;
	public static final int REQUEST_DENIED = 2;
	public static final int BAD_PEER_NAME = 3; //Unused
	public static final int BAD_DIRECTORY_NAME = 4; //Unused
	public static final int TRANSFER_REQUEST_DENIED = 5; //Unused
	public static final int BAD_DATA_ID = 6; //Unused
	public static final int BAD_FILE_PATH = 7;
	public static final int OFFER_TIMEOUT = 8;
	public static final int OFFER_REJECTED = 9;
	public static final int OFFER_ACCEPTED = 10;
	public static final int NO_FILE_ANNOUNCEMENT_LISTENER = 11;
	public static final int CANCELLED = 12;
	public static final int FILE_NOT_BEING_TRANSFERRED = 13;
	public static final int TIMED_OUT = 14; //Unused
	public static final int INVALID = 15;
	public static final int OUTSTANDING_FILE_ID_REQUEST = 16; //Unused
	public static final int NO_AJ_CONNECTION = 17;
}
