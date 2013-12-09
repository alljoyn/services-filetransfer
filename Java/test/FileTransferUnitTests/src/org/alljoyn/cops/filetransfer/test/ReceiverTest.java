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

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.cops.filetransfer.AnnouncementManager;
import org.alljoyn.cops.filetransfer.DirectedAnnouncementManager;
import org.alljoyn.cops.filetransfer.ReceiveManager;
import org.alljoyn.cops.filetransfer.SendManager;
import org.alljoyn.cops.filetransfer.OfferManager;
import org.alljoyn.cops.filetransfer.Receiver;
import org.alljoyn.cops.filetransfer.alljoyn.DataTransferInterface;
import org.alljoyn.cops.filetransfer.alljoyn.FileDiscoveryInterface;
import org.alljoyn.cops.filetransfer.alljoyn.FileTransferBusObject;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.utility.Logger;

import android.test.AndroidTestCase;

public class ReceiverTest extends AndroidTestCase
{
	static 
	{
	    System.loadLibrary("alljoyn_java");
	}
	
	private static final String SERVICE_NAME = "org.alljoyn.cops.filetransfer";
	private static final short CONTACT_PORT=42;
	private static final int WAIT_TIME = 100;
	
	private int sessionId;
	private BusAttachment sendingBus;
	private BusAttachment receivingBus;
	private FileTransferBusObject sendingBusObject;
	private MockAnnouncer mockAnnouncer;
	private MockTransferManager mockSendManager;
	private MockReceiveManager mockReceiveManager;
	private MockDirectedAnnouncementManager mockDirectedAnnouncementManager;
	private MockOfferManager mockOfferManager;
	private boolean messageReceived;
	
	protected void setUp() throws Exception
	{
		sendingBus = new BusAttachment("FileTransfer", BusAttachment.RemoteMessage.Receive);		
		createSession(sendingBus);
		
		receivingBus = new BusAttachment("FileTransfer", BusAttachment.RemoteMessage.Receive);    
		joinSession(receivingBus); 
		
		//wait to join session
		Thread.sleep(1000);
		assertNotSame("invalid session id", 0, sessionId);
		
		//create message listener - used to verify AJ message received
		messageReceived = false;	
		MessageReceivedListener messageReceivedlistener = new MessageReceivedListener()
		{
			public void messageReceived()
			{
				messageReceived = true;								
			}			
		};
		
		//create mock components
		mockAnnouncer = new MockAnnouncer(messageReceivedlistener);
		mockSendManager = new MockTransferManager(messageReceivedlistener);
		mockReceiveManager = new MockReceiveManager(messageReceivedlistener);
		mockDirectedAnnouncementManager = new MockDirectedAnnouncementManager(messageReceivedlistener);
		mockOfferManager = new MockOfferManager(messageReceivedlistener);
		
		//register bus objects
		sendingBusObject = new FileTransferBusObject(sendingBus);
		Status status = sendingBus.registerBusObject(sendingBusObject, FileTransferBusObject.OBJECT_PATH);
		assertEquals("client register bus object failed", status, Status.OK);		
		
		FileTransferBusObject receivingBusObject = new FileTransferBusObject(receivingBus);
		receivingBusObject.setDirectedAnnouncementManagerListener(mockDirectedAnnouncementManager);
		receivingBusObject.setOfferManagerListener(mockOfferManager);
		receivingBusObject.setSendManagerListener(mockSendManager);		
		
		status = receivingBus.registerBusObject(receivingBusObject, FileTransferBusObject.OBJECT_PATH);
		assertEquals("client register bus object failed", status, Status.OK);		
		
		new Receiver(receivingBus, mockAnnouncer, mockSendManager, mockReceiveManager, mockDirectedAnnouncementManager);
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
		    	Logger.log("sessionId = " + sessionId);
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
	
	protected void tearDown()
	{		
		receivingBus.leaveSession(sessionId);
		receivingBus.disconnect();
		sendingBus.leaveSession(sessionId);
		sendingBus.disconnect();	
		sessionId = 0;
		
		receivingBus.release();
		sendingBus.release();
	}
	
	public void testAnnounce() throws Exception
	{
		SignalEmitter emitter = new SignalEmitter(sendingBusObject, receivingBus.getUniqueName(), 
				sessionId, SignalEmitter.GlobalBroadcast.On);	
		emitter.getInterface(FileDiscoveryInterface.class).announce(new FileDescriptor[] { getDummyFileDescriptor() }, false);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("announce signal not received", messageReceived);
	}
	
	public void testRequestAnnouncement() throws Exception
	{
		SignalEmitter emitter = new SignalEmitter(sendingBusObject, receivingBus.getUniqueName(), 
				sessionId, SignalEmitter.GlobalBroadcast.On);	
		emitter.getInterface(FileDiscoveryInterface.class).requestAnnouncement();
		
		Thread.sleep(WAIT_TIME);
		assertTrue("request announce signal not received", messageReceived);
	}
	
	public void testRequestOffer() throws Exception
	{
		ProxyBusObject proxy = sendingBus.getProxyBusObject(receivingBus.getUniqueName(), FileTransferBusObject.OBJECT_PATH, 
				sessionId, new Class[] { FileDiscoveryInterface.class });
		proxy.getInterface(FileDiscoveryInterface.class).requestOffer("foo");
		
		assertTrue("request file id method not received", messageReceived);
	}
	
	public void testOfferFile() throws Exception
	{
		ProxyBusObject proxy = sendingBus.getProxyBusObject(receivingBus.getUniqueName(), FileTransferBusObject.OBJECT_PATH, 
				sessionId, new Class[] { FileDiscoveryInterface.class });
		proxy.getInterface(FileDiscoveryInterface.class).offerFile(getDummyFileDescriptor());
		
		assertTrue("offer file method not received", messageReceived);
	}	
	
	public void testRequestData() throws Exception
	{
		FileDescriptor descriptor = getDummyFileDescriptor();
		
		ProxyBusObject proxy = sendingBus.getProxyBusObject(receivingBus.getUniqueName(), FileTransferBusObject.OBJECT_PATH, 
				sessionId, new Class[] { DataTransferInterface.class });
		proxy.getInterface(DataTransferInterface.class).requestData(descriptor.fileID, 0, descriptor.size, 0);
		
		assertTrue("request data method not received", messageReceived);
	}
	
	public void testDataChunk() throws Exception
	{
		FileDescriptor descriptor = getDummyFileDescriptor();
		
		SignalEmitter emitter = new SignalEmitter(sendingBusObject, receivingBus.getUniqueName(), 
				sessionId, SignalEmitter.GlobalBroadcast.On);	
		emitter.getInterface(DataTransferInterface.class).dataChunk(descriptor.fileID, 0, descriptor.size, new byte[1]);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("data chunk signal not received", messageReceived);
	}
	
	public void testStopDataXfer() throws Exception
	{
		SignalEmitter emitter = new SignalEmitter(sendingBusObject, receivingBus.getUniqueName(), 
				sessionId, SignalEmitter.GlobalBroadcast.On);	
		emitter.getInterface(DataTransferInterface.class).stopDataXfer(getDummyFileDescriptor().fileID);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("stop data xfer signal not received", messageReceived);
	}
	
	public void testDataXferCancelled() throws Exception
	{
		SignalEmitter emitter = new SignalEmitter(sendingBusObject, receivingBus.getUniqueName(), 
				sessionId, SignalEmitter.GlobalBroadcast.On);	
		emitter.getInterface(DataTransferInterface.class).dataXferCancelled(getDummyFileDescriptor().fileID);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("data xfer cancelled signal not received", messageReceived);
	}
	
	private FileDescriptor getDummyFileDescriptor()
	{
		FileDescriptor descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
		descriptor.filename = "foo";
		descriptor.owner = sendingBus.getUniqueName();
		descriptor.relativePath = "";
		descriptor.sharedPath = "";
		descriptor.size = 0;
		
		return descriptor;
	}	
	
	private interface MessageReceivedListener
	{
		public void messageReceived();
	}
	
	private class MockAnnouncer extends AnnouncementManager
	{
		private MessageReceivedListener listener;
		
		public MockAnnouncer(MessageReceivedListener listener)
		{
			super(null, null, null);
			this.listener = listener;
		}
		
		@Override
		public void handleAnnounced(FileDescriptor[] fileList, String peer)
		{
			listener.messageReceived();
		}
		
		@Override
		public void handleAnnouncementRequest(String peer)
		{
			listener.messageReceived();
		}
	}
	
	private class MockTransferManager extends SendManager
	{
		private MessageReceivedListener listener;
		
		public MockTransferManager(MessageReceivedListener listener)
		{
			super(null, null, null);
			this.listener = listener;
		}
		
		@Override
		public int sendFile(byte[] fileId, int startByte, int length, String peer, int maxChunkLength)
		{
			listener.messageReceived();
			return 0;
		}
		
		@Override
		public void handleStopDataXfer(byte[] fileId, String peer)
		{
			listener.messageReceived();
		}
	}
	
	private class MockReceiveManager extends ReceiveManager
	{
		private MessageReceivedListener listener;
		
		public MockReceiveManager(MessageReceivedListener listener)
		{
			super(null, null, null);
			this.listener = listener;
		}		
		
		@Override
		public void handleFileChunk(byte[] fileId, int startByte, int chunkLength, byte[] chunk)
		{
			listener.messageReceived();			
		}
		
		@Override
		public void handleDataXferCancelled(byte[] fileId, String peer)
		{
			listener.messageReceived();
		}
	}
	
	private class MockDirectedAnnouncementManager extends DirectedAnnouncementManager
	{
		private MessageReceivedListener listener;
		
		public MockDirectedAnnouncementManager(MessageReceivedListener messageReceivedlistener)
		{
			super(null, null, null);
			
			listener = messageReceivedlistener;
		}
		
		@Override
		public int handleOfferRequest(String filePath, String peer)
		{
			Logger.log("here");
			listener.messageReceived();
			return 0;
		}
	}
	
	private class MockOfferManager extends OfferManager
	{
		private MessageReceivedListener listener;
		
		public MockOfferManager(MessageReceivedListener messageReceivedlistener)
		{
			super(null, null, null);
			
			listener = messageReceivedlistener;
		}
		
		@Override
		public int handleOffer(FileDescriptor file, String peer)
		{
			listener.messageReceived();
			return 0;
		}
	}
}
