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
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.cops.filetransfer.Transmitter;
import org.alljoyn.cops.filetransfer.alljoyn.DataTransferInterface;
import org.alljoyn.cops.filetransfer.alljoyn.FileDiscoveryInterface;
import org.alljoyn.cops.filetransfer.alljoyn.FileTransferBusObject;
import org.alljoyn.cops.filetransfer.data.Action;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.Action.ActionType;
import org.alljoyn.cops.filetransfer.utility.Logger;

import android.test.AndroidTestCase;

public class TransmitterTest extends AndroidTestCase
{
	static 
	{
	    System.loadLibrary("alljoyn_java");
	}	
	
	private static final String SERVICE_NAME = "org.alljoyn.cops.filetransfer";
	private static final short CONTACT_PORT=42;
	private static final int WAIT_TIME = 100;
	
	private int sessionId;
	private BusAttachment hostBus;
	private BusAttachment clientBus;
	private MockBusObject clientBusObject;
	private MockReceiver receiver;
	private Transmitter transmitter;	
	private boolean messageReceived;
	
	protected void setUp() throws Exception
	{
		hostBus = new BusAttachment("FileTransfer", BusAttachment.RemoteMessage.Receive);		
		createSession(hostBus);
		
		clientBus = new BusAttachment("FileTransfer", BusAttachment.RemoteMessage.Receive);    
		joinSession(clientBus); 
		
		//wait to join session
		Thread.sleep(1000);
		assertNotSame("invalid session id", 0, sessionId);
		
		//register bus object		
		clientBusObject = new MockBusObject();
		Status status = clientBus.registerBusObject(clientBusObject, FileTransferBusObject.OBJECT_PATH);
		assertEquals("client register bus object failed", status, Status.OK);
		
		receiver = new MockReceiver(clientBus);	
		
		MockBusObject hostMockBusObject = new MockBusObject();
		transmitter = new Transmitter(hostMockBusObject, hostBus, sessionId);		
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
		clientBus.leaveSession(sessionId);
		clientBus.disconnect();
		hostBus.leaveSession(sessionId);
		hostBus.disconnect();	
		sessionId = 0;
		
		clientBus.release();
		hostBus.release();
	}
	
	public void testAnnounce() throws Exception
	{
		messageReceived = false;
		
		receiver.setSignalReceivedListener(new MessageReceivedListener()
		{			
			public void messageReceived()
			{				
				messageReceived = true;
			}
		});
		
		Action action = new Action();
		action.actionType = ActionType.ANNOUNCE;
		action.parameters.add( new FileDescriptor[] { getDummyFileDescriptor() } );
		action.parameters.add(false);
		action.peer = null;
		
		transmitter.transmit(action);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("announce signal not received", messageReceived);
	}
	
	public void testRequestAnnouncement() throws Exception
	{
		messageReceived = false;
		
		receiver.setSignalReceivedListener(new MessageReceivedListener()
		{			
			public void messageReceived()
			{				
				messageReceived = true;
			}
		});
		
		Action action = new Action();
		action.actionType = ActionType.REQUEST_ANNOUNCE;
		action.peer = clientBus.getUniqueName();
		
		transmitter.transmit(action);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("request announce signal not received", messageReceived);
	}
	
	public void testRequestOffer() throws Exception
	{
		messageReceived = false;
		
		clientBusObject.setMethodReceivedListener(new MessageReceivedListener()
		{			
			public void messageReceived()
			{
				messageReceived = true;								
			}
		});
		
		Action action = new Action();
		action.actionType = ActionType.REQUEST_OFFER;
		action.parameters.add("foo\\path");
		action.peer = clientBus.getUniqueName();
		
		transmitter.transmit(action);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("request file id method not received", messageReceived);
	}
	
	public void testOfferFile() throws Exception
	{
		messageReceived = false;
		
		clientBusObject.setMethodReceivedListener(new MessageReceivedListener()
		{			
			public void messageReceived()
			{
				messageReceived = true;								
			}
		});
		
		Action action = new Action();
		action.actionType = ActionType.OFFER_FILE;
		action.parameters.add(getDummyFileDescriptor());
		action.peer = clientBus.getUniqueName();
		
		transmitter.transmit(action);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("offer method not received", messageReceived);
	}
	
	public void testRequestData() throws Exception
	{
		messageReceived = false;
		
		clientBusObject.setMethodReceivedListener(new MessageReceivedListener()
		{			
			public void messageReceived()
			{
				messageReceived = true;								
			}
		});
		
		Action action = new Action();
		FileDescriptor file = getDummyFileDescriptor();
		action.actionType = ActionType.REQUEST_DATA;		
		action.parameters.add(file.fileID);
		action.parameters.add(0);
		action.parameters.add(file.size);
		action.parameters.add(0);
		action.peer = clientBus.getUniqueName();
		
		transmitter.transmit(action);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("request data method not received", messageReceived);
	}
	
	public void testDataChunk() throws Exception
	{
		messageReceived = false;
		
		receiver.setSignalReceivedListener(new MessageReceivedListener()
		{			
			public void messageReceived()
			{				
				messageReceived = true;
			}
		});
		
		Action action = new Action();
		FileDescriptor file = getDummyFileDescriptor();
		action.actionType = ActionType.DATA_CHUNK;
		action.peer = clientBus.getUniqueName();
		action.parameters.add(file.fileID);
		action.parameters.add(0);
		action.parameters.add(0);
		action.parameters.add(new byte[1]);
		
		transmitter.transmit(action);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("data chunk signal not received", messageReceived);
	}	
	
	public void testStopDataXfer() throws Exception
	{
		messageReceived = false;
		
		receiver.setSignalReceivedListener(new MessageReceivedListener()
		{			
			public void messageReceived()
			{				
				messageReceived = true;
			}
		});
		
		Action action = new Action();
		FileDescriptor file = getDummyFileDescriptor();
		action.parameters.add(file.fileID);
		action.actionType = ActionType.STOP_XFER;
		action.peer = clientBus.getUniqueName();
		
		transmitter.transmit(action);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("stop data xfer signal not received", messageReceived);
	}
	
	public void testDataXferCancelled() throws Exception
	{
		messageReceived = false;
		
		receiver.setSignalReceivedListener(new MessageReceivedListener()
		{			
			public void messageReceived()
			{				
				messageReceived = true;
			}
		});
		
		Action action = new Action();
		FileDescriptor file = getDummyFileDescriptor();
		action.parameters.add(file.fileID);
		action.actionType = ActionType.XFER_CANCELLED;
		action.peer = clientBus.getUniqueName();
		
		transmitter.transmit(action);
		
		Thread.sleep(WAIT_TIME);
		assertTrue("stop data xfer signal not received", messageReceived);
	}

	private FileDescriptor getDummyFileDescriptor()
	{
		FileDescriptor descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
		descriptor.filename = "foo";
		descriptor.owner = hostBus.getUniqueName();
		descriptor.relativePath = "";
		descriptor.sharedPath = "";
		descriptor.size = 0;
		
		return descriptor;
	}
	
	private interface MessageReceivedListener
	{
		public void messageReceived();
	}
	
	private class MockReceiver
	{
		private MessageReceivedListener listener;
		
		public MockReceiver(BusAttachment bus)
		{
			bus.registerSignalHandlers(this);
		}
		
		public void setSignalReceivedListener(MessageReceivedListener listener)
		{
			this.listener = listener;
		}
		
		@SuppressWarnings("unused")
		@BusSignalHandler(iface="org.alljoyn.Cops.FileDiscovery", signal="announce")
		public void announce(FileDescriptor[] fileList, boolean isFileIdResponse)
		{			
			assertNotNull(fileList);
			assertNotNull(isFileIdResponse);
			onMessageReceived();			
		}
		
		@SuppressWarnings("unused")
		@BusSignalHandler(iface="org.alljoyn.Cops.FileDiscovery", signal="requestAnnouncement")
		public void requestAnnouncement() 
		{
			onMessageReceived();
		}		
		
		@SuppressWarnings("unused")
		@BusSignalHandler(iface="org.alljoyn.Cops.DataTransfer", signal="dataChunk")
		public void dataChunk(byte[] fileId, int startByte,	int chunkLength, byte[] chunk)
		{
			assertNotNull(fileId);
			assertNotNull(startByte);
			assertNotNull(chunkLength);
			assertNotNull(chunk);
			onMessageReceived();
		}
		
		@SuppressWarnings("unused")
		@BusSignalHandler(iface="org.alljoyn.Cops.DataTransfer", signal="stopDataXfer")
		public void stopDataXfer(byte[] fileId)
		{
			assertNotNull(fileId);
			onMessageReceived();
		}
		
		@SuppressWarnings("unused")
		@BusSignalHandler(iface="org.alljoyn.Cops.DataTransfer", signal="dataXferCancelled")
		public void dataXferCancelled(byte[] fileId)
		{
			assertNotNull(fileId);
			onMessageReceived();
		}

		private void onMessageReceived()
		{
			if (listener != null)
			{
				listener.messageReceived();
			}
		}
	}
	
	private class MockBusObject extends FileTransferBusObject implements FileDiscoveryInterface, DataTransferInterface, BusObject
	{
		private MessageReceivedListener listener;
		
		public MockBusObject()
		{
			super(null);
		}
		
		public void setMethodReceivedListener(MessageReceivedListener listener)
		{
			this.listener = listener;
		}
		
		private void onMessageReceived()
		{
			if (listener != null)
			{
				listener.messageReceived();
			}
		}

		@Override
		public int requestData(byte[] fileId, int startByte, int length,
				int maxChunkLength) throws BusException
		{
			assertNotNull(fileId);
			assertNotNull(startByte);
			assertNotNull(length);
			assertNotNull(maxChunkLength);
			onMessageReceived();
			return 0;
		}
		
		@Override
		public int requestOffer(String filepath) throws BusException
		{
			assertNotNull(filepath);
			onMessageReceived();
			return 0;
		}

		@Override
		public int offerFile(FileDescriptor file) throws BusException
		{			
			assertNotNull(file);
			onMessageReceived();			
			return 0;
		}

		@Override
		public void dataChunk(byte[] fileId, int startByte, int chunkLength,
				byte[] chunk) throws BusException { }

		@Override
		public void stopDataXfer(byte[] fileId) throws BusException	{ }

		@Override
		public void dataXferCancelled(byte[] fileId) throws BusException { }

		@Override
		public void announce(FileDescriptor[] fileList, boolean isFileIdResponse) throws BusException { }

		@Override
		public void requestAnnouncement() throws BusException { }		

		@Override
		public void offerRejected(FileDescriptor file) throws BusException { }		
	}
}




