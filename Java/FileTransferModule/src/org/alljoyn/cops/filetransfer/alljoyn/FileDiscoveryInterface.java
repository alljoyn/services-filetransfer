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
import org.alljoyn.cops.filetransfer.data.FileDescriptor;

/**
 * The File Discovery Interface specifies the AllJoyn signals and methods that are
 * associated with file discovery. The main behaviors of this interface include:
 * file announcing, requesting file announcements from session peers, and offering 
 * files to session peers.
 * <p>
 * Note: This interface is not intended to be used directly. All of the supported
 * functionality of this library is intended to be accessed through the
 * {@link org.alljoyn.cops.filetransfer.FileTransferModule} class.
 */
@BusInterface(name="org.alljoyn.Cops.FileDiscovery")
public interface FileDiscoveryInterface
{
	/**
	 * announce()
	 * is specified as an AllJoyn signal that is used to send file announcements to session
	 * peers over AllJoyn. The signal can be a directed or global signal. The fileList parameter 
	 * denotes the list of files that are announced by the sender and are available for immediate
	 * transfer. The fileList is passed over to the AnnouncementManager to be stored.	  			 
	 * 
	 * @param fileList  specifies a list of files available for transfer
	 * @param isFileIDResponse  specifies if the announcement is in response to an offer request
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	@BusSignal
	public void announce(FileDescriptor[] fileList, boolean isFileIDResponse) throws BusException;
	
	/**
	 * requestAnnouncement()
	 * is specified as an AllJoyn signal and allows the user to send an announcement request to a  
	 * session peer for all the files they have made available for transfer. This handler calls 
	 * handleAnnouncementRequest() on the AnnouncementManager which will insert an announce action 
	 * into the dispatcher.	  			 
	 * 
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */	
	@BusSignal
	public void requestAnnouncement() throws BusException;
	
	/**
	 * requestOffer()
	 * is specified as an AllJoyn method and is used to request files that have not been explicitly
	 * announced or shared. This handler tells the announcer to handle the offer request.	  			
	 *  
	 * @param filepath  absolute path of file being requested
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	@BusMethod
	public int requestOffer(String filepath) throws BusException;
	
	/**
	 * offerFile()
	 * is specified as an AllJoyn method and is used to send file offers to session peers. The method
	 * handler will alert the Receive Manager when an offer is received so they can respond accordingly.
	 * 
	 * @param file  file descriptor of offered file
	 * @throws BusException  thrown in the case of an AllJoyn error
	 */
	@BusMethod
	public int offerFile(FileDescriptor file) throws BusException;
}
