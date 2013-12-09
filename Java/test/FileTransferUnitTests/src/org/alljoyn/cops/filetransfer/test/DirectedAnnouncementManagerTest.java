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

import java.util.ArrayList;

import org.alljoyn.cops.filetransfer.DirectedAnnouncementManager;
import org.alljoyn.cops.filetransfer.Dispatcher;
import org.alljoyn.cops.filetransfer.FileSystemAbstraction;
import org.alljoyn.cops.filetransfer.PermissionsManager;
import org.alljoyn.cops.filetransfer.Transmitter;
import org.alljoyn.cops.filetransfer.data.Action;
import org.alljoyn.cops.filetransfer.data.Action.ActionType;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.FileAnnouncementReceivedListener;
import org.alljoyn.cops.filetransfer.listener.UnannouncedFileRequestListener;

import android.test.AndroidTestCase;

public class DirectedAnnouncementManagerTest extends AndroidTestCase
{
	private DirectedAnnouncementManager directedAnnouncementManager;
	private MockDispatcher mockDispatcher;
	private MockFSA mockFSA;
	private MockTransmitter mockTransmitter;
	private MockPermissionsManager mockPM;
	private String localBusId = "me";
	
	protected void setUp() throws Exception 
	{
		mockTransmitter = new MockTransmitter();
		mockFSA = new MockFSA();
		mockPM = new MockPermissionsManager();
		mockDispatcher = new MockDispatcher(mockTransmitter);
		
		directedAnnouncementManager = new DirectedAnnouncementManager(mockDispatcher, localBusId, mockFSA, mockPM);
	}
	
	public void testRequestOffer() 
	{
		mockTransmitter.setTestListener(new TestListener()
		{
			public void sendBackAction(Action action)
			{
				assertEquals(ActionType.REQUEST_OFFER, action.actionType);
			}
		});
		
		mockTransmitter.setReturnCode(StatusCode.OK);
		int status = directedAnnouncementManager.requestOffer("Bob", "sdcard/test_path/photo.png");
		assertEquals(StatusCode.OK, status);
		
		mockTransmitter.setReturnCode(StatusCode.REQUEST_DENIED);
		status = directedAnnouncementManager.requestOffer("Bill", "sdcard/test_path/story.txt");
		assertEquals(StatusCode.REQUEST_DENIED, status);
	}
	
	public void testHandleFileIdRequest() 
	{		
		mockPM.addAnnouncedLocalFiles(generateKnownAnnouncedDummyDescriptorArray("Adam"));		
		
		mockDispatcher.setTestListener(new TestListener()
		{
			public void sendBackAction(Action action)
			{
				FileDescriptor[] files = (FileDescriptor[])action.parameters.get(0);
				boolean isFileIdResponse = (Boolean)action.parameters.get(1);
				assertEquals(1, files.length);
				assertTrue(isFileIdResponse);
				assertNotNull(action.peer);
				assertEquals(ActionType.ANNOUNCE, action.actionType);
			}
		});
		
		int statusCode = directedAnnouncementManager.handleOfferRequest("sdcard/reports/animals.txt", "Adam");
		assertEquals(StatusCode.OK, statusCode);
		
		mockPM.addOfferedLocalFile(generateSharedFileDescriptor());
		
		statusCode = directedAnnouncementManager.handleOfferRequest("sdcard/shared/meals.txt", "Adam");
		assertEquals(StatusCode.OK, statusCode);
		
		//Test where the announcers UnannouncedFileListener is uninitialized and set to null.
		statusCode = directedAnnouncementManager.handleOfferRequest("sdcard/unshared/unsharedFile.doc", "Adam");
		assertEquals(StatusCode.REQUEST_DENIED, statusCode);
		
		//Test where we initialize the UnannouncedFileListener callback and it returns false
		directedAnnouncementManager.setUnannouncedFileRequestListener(new Application());
		statusCode = directedAnnouncementManager.handleOfferRequest("sdcard/unshared/nvalid.txt", "Adam");
		assertEquals(StatusCode.REQUEST_DENIED, statusCode);
		
		mockDispatcher.setTestListener(new TestListener()
		{
			public void sendBackAction(Action action)
			{
				String path = (String)action.parameters.get(0);
				String peer = (String)action.parameters.get(1);
				assertEquals("", path);
				assertEquals("Adam", peer);
				assertEquals(ActionType.FILE_ID_RESPONSE, action.actionType);
			}
		});
		
		//Test where the UnannouncedFileListener will return true
		statusCode = directedAnnouncementManager.handleOfferRequest("", "Adam");
		assertEquals(StatusCode.OK, statusCode);
	}
	
	public void testGenerateFileDescriptor() 
	{
		Action action = new Action();
		action.parameters.add("sdcard/testFile/test.txt");
		action.parameters.add("Adam");
		
		mockDispatcher.setTestListener(new TestListener()
		{
			public void sendBackAction(Action action)
			{
				FileDescriptor[] fda = (FileDescriptor[])action.parameters.get(0);
				boolean isFileIdResponse = (Boolean)action.parameters.get(1);
				assertEquals(1, fda.length);
				assertTrue(isFileIdResponse);
				assertEquals("Adam", action.peer);
				assertEquals(ActionType.ANNOUNCE, action.actionType);
			}
		});
		
		directedAnnouncementManager.generateFileDescriptor(action);
	}
	
	private FileDescriptor generateSingleDescriptor()
	{
		FileDescriptor descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };
		descriptor.filename = "test.txt";
		descriptor.owner = "Adam";
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/testFile";
		descriptor.size = 100000;
		
		return descriptor;
	}
	
	private FileDescriptor generateSharedFileDescriptor()
	{
		FileDescriptor descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9 };
		descriptor.filename = "meals.txt";
		descriptor.owner = "bar";
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/shared";
		descriptor.size = 100;
		
		return descriptor;
	}
	
	private FileDescriptor[] generateKnownAnnouncedDummyDescriptorArray(String owner)
	{
		ArrayList<FileDescriptor> fileList = new ArrayList<FileDescriptor>();
		
		FileDescriptor descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		descriptor.filename = "house.png";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/photos";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 };
		descriptor.filename = "backyard.png";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/photos";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 };
		descriptor.filename = "fireplace.png";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/photos";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 };
		descriptor.filename = "animals.txt";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/reports";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 };
		descriptor.filename = "inventors.txt";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/reports";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6 };
		descriptor.filename = "driving.txt";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/reports";
		descriptor.size = 100;
		fileList.add(descriptor);

		return fileList.toArray(new FileDescriptor[fileList.size()]);
	}
	
	private interface TestListener
	{
		public void sendBackAction(Action action);
	}
	
	//Mock Transmitter
	private class MockTransmitter extends Transmitter
	{
		int returnStatusCode;
        TestListener listener;
		
		public MockTransmitter() 
		{
			super(null, null, 0);
		}
		
		public void setTestListener(TestListener listener)
		{
            this.listener = listener;
		}
		
		public void setReturnCode(int code)
		{
			returnStatusCode = code;
		}
		
		@Override
		public int transmit(Action action)
		{
            if (listener != null)
            {
                listener.sendBackAction(action);
            }
            
			return returnStatusCode;
		}
	}

	private class MockFSA extends FileSystemAbstraction
	{
		@Override
		public FileDescriptor[] getFileInfo(ArrayList<String> pathList, ArrayList<String> failedPaths, String localBusId)
		{	
			FileDescriptor[] fda = null;
			
			if (pathList.size() > 0)
			{
				if (pathList.get(0).equals("sdcard/testFile/test.txt"))
				{
					FileDescriptor fd = generateSingleDescriptor();
					fda = new FileDescriptor[] { fd };
					return fda;
				}
			}
			else
			{
				failedPaths.add("invalid_path");
				failedPaths.add("invalid_path");
				failedPaths.add("invalid_path");
				failedPaths.add("invalid_path");
				
				fda = generateKnownAnnouncedDummyDescriptorArray("bar");
			}
			
			return fda;
		}		
	}
	
	//MockDispatcher
	private class MockDispatcher extends Dispatcher
	{
		TestListener listener;
		
		public MockDispatcher(Transmitter transmitter) 
		{
			super(transmitter);
		}
		
		public void setTestListener(TestListener listener)
		{
			this.listener = listener;
		}
		
		@Override
		public void insertAction(Action action)
		{
			if (listener != null)
			{
				listener.sendBackAction(action);
			}
		}
	}
	
//	//Mock Application
	private class Application implements FileAnnouncementReceivedListener, UnannouncedFileRequestListener
	{

		public void receivedAnnouncement(FileDescriptor[] fileList, boolean isFileIdResponse) 
		{
			//Do nothing
		}
		
		public boolean allowUnannouncedFileRequests(String filePath) 
		{
			if (filePath.equals(""))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	
	private class MockPermissionsManager extends PermissionsManager
	{

	}
}
