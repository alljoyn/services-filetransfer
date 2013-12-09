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

package org.alljoyn.cops.filetransfer.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import org.alljoyn.cops.filetransfer.FileTransferModule;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.ProgressDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.FileAnnouncementReceivedListener;
import org.alljoyn.cops.filetransfer.listener.OfferReceivedListener;
import org.alljoyn.cops.filetransfer.listener.UnannouncedFileRequestListener;
import org.alljoyn.cops.filetransfer.utility.Logger;

import android.test.AndroidTestCase;

public class FileTransferModuleTest extends AndroidTestCase
{
	static 
	{
	    System.loadLibrary("alljoyn_java");
	}
	
	private static final String SERVICE_NAME = "org.alljoyn.cops.filetransfer";
	private static final short CONTACT_PORT=42;
	
	private FileTransferModule sendingFtModule;
	private FileTransferModule receivingFtModule;
	private BusAttachment sendingBus;
	private BusAttachment receivingBus;
	private int sessionId;
	private File testDir;
	private File testFile;
	private boolean messageReceived = false;
	
	protected void setUp() throws Exception
	{
		sendingFtModule = new FileTransferModule();
		receivingFtModule = new FileTransferModule();
		
		testDir = new File(getContext().getFilesDir().getAbsolutePath());
		testDir.mkdir();
		
		testFile = new File(getContext().getFilesDir().getAbsolutePath(), "testFile.test");
		
		FileOutputStream out = new FileOutputStream(testFile);
		out.write("test".getBytes());
		out.close();
		
		createAjConnection();
		
		messageReceived = false;		
	}
	
	private void createAjConnection() throws Exception
	{
		sendingBus = new BusAttachment("FileTransfer", BusAttachment.RemoteMessage.Receive);		
		createSession(sendingBus);
		
		receivingBus = new BusAttachment("FileTransfer", BusAttachment.RemoteMessage.Receive);    
		joinSession(receivingBus); 
		
		//wait to join session
		Thread.sleep(1000);
		assertNotSame("invalid session id", 0, sessionId);		
	}
	
	private void createSession(BusAttachment bus)
	{
		bus.connect();
		
		Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
		
		SessionOpts sessionOpts = new SessionOpts();
		sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
		sessionOpts.isMultipoint = true;
		sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
		sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
		
		bus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() 
		{
		    @Override
		    public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts)
		    {		    	
		    	return true;
		    }
		    
		    @Override
			public void sessionJoined(short sessionPort, int id, String joiner) 
		    {
		    	sessionId = id;
		    	Logger.log("hosting sessionId = " + sessionId);
		    }
		});
		
		int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;		
		bus.requestName(SERVICE_NAME, flag);
		bus.advertiseName(SERVICE_NAME, SessionOpts.TRANSPORT_ANY);
	}
    
	private void joinSession(final BusAttachment bus)
	{
		bus.registerBusListener(new BusListener() 
        {
            @Override
            public void foundAdvertisedName(final String name, short transport, String namePrefix) 
            {
            	new Thread()
            	{
            		public void run()
            		{
            			SessionOpts sessionOpts = new SessionOpts();
            			Mutable.IntegerValue mutableSessionId = new Mutable.IntegerValue();
            			
            			Status status = bus.joinSession(name, CONTACT_PORT, mutableSessionId, sessionOpts, new SessionListener()); 
            		    Logger.log("join returned: " + status.toString());	         			           			
            		}
            	}.start();       	           	
            }
        });
        
        bus.connect();
        bus.findAdvertisedName(SERVICE_NAME);					
	}
	
	@Override
	protected void tearDown()
	{
		testFile.delete();
		testDir.delete();
		
		receivingBus.leaveSession(sessionId);
		receivingBus.disconnect();
		sendingBus.leaveSession(sessionId);
		sendingBus.disconnect();	
		sessionId = 0;
		
		receivingBus.release();
		sendingBus.release();
		
        sendingFtModule.destroy();
        receivingFtModule.destroy();
	}

	public void testAnnounce() throws Exception
	{	
		//test can add to announced list without initializing
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.announce(pathList);		
		
		Thread.sleep(100); //allow time to generate announcement
		
		assertEquals(1, sendingFtModule.getAnnouncedLocalFiles().size());	
		
		//test announcement received after initializing
		receivingFtModule.setFileAnnouncementReceivedListener(new FileAnnouncementReceivedListener()
		{			
			@Override
			public void receivedAnnouncement(FileDescriptor[] fileList,	boolean isFileIdResponse)
			{
				messageReceived = true;
				assertEquals(1, fileList.length);					
			}
		});
		
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);		
		
		Thread.sleep(500); //allow time to receive announcement
		assertTrue(messageReceived);
	}
	
	public void testInitialize() throws Exception
	{
		//test message received after first initialization
		receivingFtModule.setFileAnnouncementReceivedListener(new FileAnnouncementReceivedListener()
		{			
			@Override
			public void receivedAnnouncement(FileDescriptor[] fileList,	boolean isFileIdResponse)
			{
				messageReceived = true;				
			}
		});
		
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.announce(pathList);	
		
		Thread.sleep(500); //allow time to receive announcement
		assertTrue(messageReceived);
		
		//test message not received after deinitialization
		messageReceived = false;
		
		sendingFtModule.uninitialize();
		receivingFtModule.uninitialize();
		
		sendingFtModule.announce(pathList);
		
		Thread.sleep(500); //allow time to receive announcement
		assertFalse(messageReceived);
		
		//test message received after second initialization
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		sendingFtModule.announce(pathList);	
		
		Thread.sleep(500); //allow time to receive announcement
		assertTrue(messageReceived);
	}

	public void testStopAnnounce() throws Exception
	{
		//add an announcement, then remove it
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.announce(pathList);		
		
		Thread.sleep(100); //allow time to generate announcement		
		assertEquals(1, sendingFtModule.getAnnouncedLocalFiles().size());	
		
		sendingFtModule.stopAnnounce(pathList);
		assertEquals(0, sendingFtModule.getAnnouncedLocalFiles().size());
		
		//make sure no announcement is sent when initialized
		receivingFtModule.setFileAnnouncementReceivedListener(new FileAnnouncementReceivedListener()
		{			
			@Override
			public void receivedAnnouncement(FileDescriptor[] fileList,	boolean isFileIdResponse)
			{
				messageReceived = true;				
			}
		});
		
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		Thread.sleep(500); //allow time to receive announcement
		assertFalse(messageReceived);				
	}

	public void testRequestFileAnnouncement() throws Exception
	{
		int statusCode = sendingFtModule.requestFileAnnouncement(receivingBus.getUniqueName());
		assertEquals(StatusCode.NO_AJ_CONNECTION, statusCode);
		
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		receivingFtModule.announce(pathList);
		Thread.sleep(100); //allow time to generate announcement		
		
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		sendingFtModule.setFileAnnouncementReceivedListener(new FileAnnouncementReceivedListener()
		{			
			@Override
			public void receivedAnnouncement(FileDescriptor[] fileList,	boolean isFileIdResponse)
			{
				messageReceived = true;				
			}
		});
		
		sendingFtModule.requestFileAnnouncement(receivingBus.getUniqueName());
		
		Thread.sleep(500); //allow time to receive announcement
		
		assertTrue(messageReceived);
	}

	public void testRequestOffer()
	{
		//test error status returned when no connection
		int status = receivingFtModule.requestOffer(sendingBus.getUniqueName(), testFile.getAbsolutePath());
		assertEquals(StatusCode.NO_AJ_CONNECTION, status);
		
		//test offer correctly rejected
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		status = receivingFtModule.requestOffer(sendingBus.getUniqueName(), testFile.getAbsolutePath());
		assertEquals(StatusCode.REQUEST_DENIED, status);
		
		//test offer request correctly made		
		sendingFtModule.setUnannouncedFileRequestListener(new UnannouncedFileRequestListener()
		{			
			@Override
			public boolean allowUnannouncedFileRequests(String filePath)
			{
				return true;
			}
		});
		
		status = receivingFtModule.requestOffer(sendingBus.getUniqueName(), testFile.getAbsolutePath());
		assertEquals(StatusCode.OK, status);		
		
		//test error status returned when no connection
		receivingFtModule.uninitialize();
		sendingFtModule.uninitialize();
		
		status = receivingFtModule.requestOffer(sendingBus.getUniqueName(), testFile.getAbsolutePath());
		assertEquals(StatusCode.NO_AJ_CONNECTION, status);
		
		//test offer request correctly made	
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		status = receivingFtModule.requestOffer(sendingBus.getUniqueName(), testFile.getAbsolutePath());
		assertEquals(StatusCode.OK, status);	
	}

	public void testGetFileID() throws Exception
	{
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.announce(pathList);
		
		Thread.sleep(500); //allow time to generate announcement
		
		receivingFtModule.initialize(receivingBus, sessionId);
        sendingFtModule.initialize(sendingBus, sessionId);        
		
		Thread.sleep(500); //allow announcement to be received
		
		receivingFtModule.uninitialize();
		
		byte[] receivedFileId = receivingFtModule.getFileID(sendingBus.getUniqueName(), File.separator + testFile.getName());		
		byte[] correctFileId = sendingFtModule.getAnnouncedLocalFiles().get(0).fileID;
		
		assertTrue(Arrays.equals(receivedFileId, correctFileId));		
	}

	public void testGetAvailableRemoteFiles() throws Exception
	{        
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.announce(pathList);	
		
		Thread.sleep(500); //allow time to generate announcement
		
		receivingFtModule.initialize(receivingBus, sessionId);
        sendingFtModule.initialize(sendingBus, sessionId);
		
		Thread.sleep(500); //allow announcement to be received
		
		receivingFtModule.uninitialize();
		
		ArrayList<FileDescriptor> availableFiles = receivingFtModule.getAvailableRemoteFiles();
		assertEquals(1, availableFiles.size());		
	}

	public void testGetAnnouncedLocalFiles() throws Exception
	{
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.announce(pathList);	
		Thread.sleep(100); //allow the announcement to be generated	
		
		ArrayList<FileDescriptor> localFiles = sendingFtModule.getAnnouncedLocalFiles();
		assertEquals(1, localFiles.size());
	}

	public void testGetOfferedLocalFiles() throws Exception
	{
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.offerFileToPeer(receivingBus.getUniqueName(), testFile.getAbsolutePath(), 1000);	
		Thread.sleep(100); //allow the announcement to be generated	
		
		ArrayList<FileDescriptor> localFiles = sendingFtModule.getOfferedLocalFiles();
		assertEquals(1, localFiles.size());
	}

	public void testRequestFile() throws Exception
	{
		//test error status returned when no connection
		int status = receivingFtModule.requestOffer(sendingBus.getUniqueName(), testFile.getAbsolutePath());
		assertEquals(StatusCode.NO_AJ_CONNECTION, status);
		
		//test requesting an unannounced file
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		status = receivingFtModule.requestFile(sendingBus.getUniqueName(), new byte[20], "foo");
		assertEquals(StatusCode.BAD_FILE_ID, status);
		
		//test successful request		
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.announce(pathList);	
		
		Thread.sleep(500); //allow the announcement to be generated	
		FileDescriptor descriptor = sendingFtModule.getAnnouncedLocalFiles().get(0);
		
		status = receivingFtModule.requestFile(sendingBus.getUniqueName(), descriptor.fileID, descriptor.filename);
		assertEquals(StatusCode.OK, status);
		
		//test error status returned when no connection	
		receivingFtModule.uninitialize();
		sendingFtModule.uninitialize();
		
		status = receivingFtModule.requestOffer(sendingBus.getUniqueName(), testFile.getAbsolutePath());
		assertEquals(StatusCode.NO_AJ_CONNECTION, status);
		
		//test successful request
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		status = receivingFtModule.requestFile(sendingBus.getUniqueName(), descriptor.fileID, descriptor.filename);
		assertEquals(StatusCode.OK, status);
	}

	public void testOfferFileToPeer()
	{
		//test error status returned when no connection
		int status = sendingFtModule.offerFileToPeer(receivingBus.getUniqueName(), testFile.getAbsolutePath(), 1000);
		assertEquals(StatusCode.NO_AJ_CONNECTION, status);
		
		//test offer correctly made
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		status = sendingFtModule.offerFileToPeer(receivingBus.getUniqueName(), testFile.getAbsolutePath(), 1000);
		assertEquals(StatusCode.OFFER_REJECTED, status);
		
		receivingFtModule.setOfferReceivedListener(new OfferReceivedListener()
		{			
			@Override
			public boolean acceptOfferedFile(FileDescriptor file, String peer)
			{
				return true;
			}
		});	
		
		status = sendingFtModule.offerFileToPeer(receivingBus.getUniqueName(), testFile.getAbsolutePath(), 1000);		
		assertEquals(StatusCode.OK, status);
		
		//test error status once uninitialized
		receivingFtModule.uninitialize();
		sendingFtModule.uninitialize();
		
		status = sendingFtModule.offerFileToPeer(receivingBus.getUniqueName(), testFile.getAbsolutePath(), 1000);
		assertEquals(StatusCode.NO_AJ_CONNECTION, status);
		
		//test offer correctly made once re-initialized
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		status = sendingFtModule.offerFileToPeer(receivingBus.getUniqueName(), testFile.getAbsolutePath(), 1000);		
		assertEquals(StatusCode.OK, status);		
	}

	public void testGetSendingProgressList() throws Exception
	{
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.announce(pathList);	
		
		Thread.sleep(500); //allow the announcement to be generated	
		FileDescriptor descriptor = sendingFtModule.getAnnouncedLocalFiles().get(0);
		
		receivingFtModule.setChunkSize(1);
		int status = receivingFtModule.requestFile(sendingBus.getUniqueName(), descriptor.fileID, descriptor.filename);
		assertEquals(StatusCode.OK, status);
				
		ArrayList<ProgressDescriptor> progressList = sendingFtModule.getSendingProgressList();
		assertEquals(1, progressList.size());
		
		//test progress list is cleared when uninitialized
		receivingFtModule.uninitialize();
		sendingFtModule.uninitialize();
		
		progressList = sendingFtModule.getSendingProgressList();
		assertTrue(progressList.size() == 0);		
	}

	public void testGetReceiveProgressList() throws Exception
	{
		receivingFtModule.initialize(receivingBus, sessionId);
		sendingFtModule.initialize(sendingBus, sessionId);
		
		ArrayList<String> pathList = new ArrayList<String>();
		pathList.add(testFile.getAbsolutePath());		
		sendingFtModule.announce(pathList);	
		
		Thread.sleep(100); //allow the announcement to be generated	
		FileDescriptor descriptor = sendingFtModule.getAnnouncedLocalFiles().get(0);
		
		receivingFtModule.setChunkSize(1);
		int status = receivingFtModule.requestFile(sendingBus.getUniqueName(), descriptor.fileID, descriptor.filename);
		assertEquals(StatusCode.OK, status);
				
		ArrayList<ProgressDescriptor> progressList = receivingFtModule.getReceiveProgressList();
		assertTrue(progressList.size() == 1);
		
		//test progress list is cleared when uninitialized
		receivingFtModule.uninitialize();
		sendingFtModule.uninitialize();
		
		progressList = receivingFtModule.getReceiveProgressList();
		assertTrue(progressList.size() == 0);	
	}
}
