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
import org.alljoyn.cops.filetransfer.AnnouncementManager;
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
import org.alljoyn.cops.filetransfer.listener.FileAnnouncementSentListener;

import android.test.AndroidTestCase;

public class AnnouncementManagerTest extends AndroidTestCase 
{
	private AnnouncementManager announcer;
	private MockDispatcher mockDispatcher;
	private MockFSA mockFSA;
	private MockTransmitter mockTransmitter;
	private MockPermissionsManager mockPermissionsManager;
	private String localBusId = "me";
	
	protected void setUp() throws Exception 
	{
		super.setUp();
		
		this.mockTransmitter = new MockTransmitter();
		this.mockFSA = new MockFSA();
		this.mockDispatcher = new MockDispatcher(mockTransmitter);
		this.mockPermissionsManager = new MockPermissionsManager();
		this.announcer = new AnnouncementManager(mockDispatcher, localBusId, mockPermissionsManager, mockFSA);
	}

	protected void tearDown() throws Exception 
	{
		super.tearDown();
	}

	public void testAnnounce()
	{
		ArrayList<FileDescriptor> announcedFiles = mockPermissionsManager.getAnnouncedLocalFiles();
		assertEquals(0, announcedFiles.size());
		
		mockDispatcher.setTestListener(new TestListener()
		{
			public void sendBackAction(Action action)
			{
				FileDescriptor[] files = (FileDescriptor[])action.parameters.get(0);
				boolean isFileIdResponse = (Boolean)action.parameters.get(1);
				assertEquals(6, files.length);
				assertFalse(isFileIdResponse);
				assertNull(action.peer);				
				assertEquals(ActionType.ANNOUNCE, action.actionType);
			}
		});
		
		announcer.setFileAnnouncmentSentListener(new FileAnnouncementSentListener()
		{
			public void announcementSent(ArrayList<String> failedPaths)
			{
				System.out.println("AnnouncementSentListener - callback to announcementSent() executing");
				assertEquals(4, failedPaths.size());
			}
		});
		
		ArrayList<String> pathList = new ArrayList<String>();		
		announcer.announce(pathList);
		
		try
		{
			Thread.sleep(5000);
		}
		catch (Exception ex)
		{
			System.out.println("Exception caught in thread sleep try catch block");
		}
		
		announcedFiles = mockPermissionsManager.getAnnouncedLocalFiles();
		assertEquals(6, announcedFiles.size());
	}

	public void testStopAnnounce() 
	{
		testAnnounce();
		
		mockDispatcher.setTestListener(new TestListener()
		{
			public void sendBackAction(Action action)
			{
				FileDescriptor[] files = (FileDescriptor[])action.parameters.get(0);
				boolean isFileIdResponse = (Boolean)action.parameters.get(1);
				assertEquals(3, files.length);
				assertFalse(isFileIdResponse);
				assertNull(action.peer);				
				assertEquals(ActionType.ANNOUNCE, action.actionType);
			}
		});
		
		ArrayList<String> failedPaths = announcer.stopAnnounce(generatePathsToUnannounce());
		assertEquals(1, failedPaths.size());
		
		ArrayList<FileDescriptor> announcedFiles = mockPermissionsManager.getAnnouncedLocalFiles();
		assertEquals(3, announcedFiles.size());
	}

	public void testRequestFileAnnouncement() 
	{
		mockDispatcher.setTestListener(new TestListener()
		{
			public void sendBackAction(Action action)
			{
				assertEquals("Bob", action.peer);
				assertEquals(ActionType.REQUEST_ANNOUNCE, action.actionType);
			}
		});
		
		int status = announcer.requestFileAnnouncement("Bob");		
		assertEquals(StatusCode.NO_FILE_ANNOUNCEMENT_LISTENER, status);
		
		announcer.setFileAnnouncementReceivedListener(new Application());
		status = announcer.requestFileAnnouncement("Bob");		
		assertEquals(StatusCode.OK, status);
	}

	public void testGetFileId() 
	{
		testHandleAnnounced();
		
		String filePath = "sdcard/photos/house.png";
		assertNotNull(mockPermissionsManager.getFileID("bar", filePath));
		
		filePath = "sdcard/photos/backyard.png";
		assertNotNull(mockPermissionsManager.getFileID("bar", filePath));
		
		filePath = "sdcard/photos/fireplace.png";
		assertNotNull(mockPermissionsManager.getFileID("bar", filePath));
		
		filePath = "sdcard/reports/animals.txt";
		assertNotNull(mockPermissionsManager.getFileID("bar", filePath));
		
		filePath = "sdcard/reports/inventors.txt";
		assertNotNull(mockPermissionsManager.getFileID("bar", filePath));
		
		filePath = "sdcard/reports/driving.txt";
		assertNotNull(mockPermissionsManager.getFileID("bar", filePath));
		
		filePath = "invalid_path";
		assertNull(mockPermissionsManager.getFileID("bar", filePath));
		
		assertNull(mockPermissionsManager.getFileID("bad_peer_name", filePath));
	}

	public void testGetKnownFiles() 
	{
		assertEquals(0, mockPermissionsManager.getAvailableRemoteFiles().size());		
		testHandleAnnounced();		
		assertEquals(12, mockPermissionsManager.getAvailableRemoteFiles().size());
	}

	public void testGetAnnouncedFiles() 
	{
		assertEquals(0, mockPermissionsManager.getAnnouncedLocalFiles().size());		
		testAnnounce();
		assertEquals(6, mockPermissionsManager.getAnnouncedLocalFiles().size());
	}

	public void testGetSharedFiles() 
	{
		assertEquals(0, mockPermissionsManager.getOfferedLocalFiles().size());
		testShareFile();
		assertEquals(1, mockPermissionsManager.getOfferedLocalFiles().size());
	}

	public void testHandleAnnounced()
	{
		assertEquals(0, mockPermissionsManager.getAvailableRemoteFiles().size());		
		announcer.handleAnnounced(generateKnownAnnouncedDummyDescriptorArray("bar"), "bar");		
		assertEquals(6, mockPermissionsManager.getAvailableRemoteFiles().size());
		announcer.handleAnnounced(generateKnownSharedDummyDescriptorArray("foo"), "foo");
		assertEquals(12, mockPermissionsManager.getAvailableRemoteFiles().size());
	}

	public void testHandleAnnouncementRequest() 
	{
		testAnnounce();
		
		mockDispatcher.setTestListener(new TestListener()
		{
			public void sendBackAction(Action action)
			{
				FileDescriptor[] files = (FileDescriptor[])action.parameters.get(0);
				boolean isFileIdResponse = (Boolean)action.parameters.get(1);
				assertEquals(6, files.length);
				assertFalse(isFileIdResponse);
				assertNotNull(action.peer);
				assertEquals(ActionType.ANNOUNCE, action.actionType);
			}
		});
		
		announcer.handleAnnouncementRequest("Steve");
	}



	public void testGetLocalFileDescriptor() 
	{
		testAnnounce();
		testShareFile();
		
		byte[] fileId = new byte[] { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 };
		assertNotNull(mockPermissionsManager.getLocalFileDescriptor(fileId));
		
		fileId = new byte[] { 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9 };
		assertNotNull(mockPermissionsManager.getLocalFileDescriptor(fileId));
		
		fileId = new byte[] { 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7 };
		assertNull(mockPermissionsManager.getLocalFileDescriptor(fileId));
	}

	public void testGetKnownFileDescriptor() 
	{
		byte[] fileId = new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		FileDescriptor fd;
					
		testHandleAnnounced();
		
		//Key exists, fileId is present
		fd = mockPermissionsManager.getKnownFileDescriptor(fileId, "bar");
		assertNotNull(fd);
		
		fileId = new byte[] { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };
		fd = mockPermissionsManager.getKnownFileDescriptor(fileId, "foo");
		assertNotNull(fd);
		
		//Key does not exist
		fd = mockPermissionsManager.getKnownFileDescriptor(fileId, "dave");
		assertNull(fd);
		
		//Key exists, fileId is not presents
		fileId = new byte[] { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 };
		fd = mockPermissionsManager.getKnownFileDescriptor(fileId, "bar");
		assertNull(fd);
		
		fd = mockPermissionsManager.getKnownFileDescriptor(fileId, "foo");
		assertNull(fd);
	}

	public void testIsAnnounced()
	{
		testAnnounce();
		
		byte[] fileId = new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		assertTrue(mockPermissionsManager.isAnnounced(fileId));
		
		fileId = new byte[] { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 };
		assertFalse(mockPermissionsManager.isAnnounced(fileId));
	}

	public void testIsShared() 
	{
		testShareFile();
		
		byte[] fileId = new byte[] { 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9 };
		assertTrue(mockPermissionsManager.isShared(fileId));
		
		fileId = new byte[] { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 };
		assertFalse(mockPermissionsManager.isShared(fileId));
	}

	public void testSetAnnouncementListener() 
	{
		assertTrue(true);
	}

	public void testSetUnannouncedFileListener()
	{
		assertTrue(true);
	}

	public void testShareFile() 
	{
		assertEquals(0, mockPermissionsManager.getOfferedLocalFiles().size());
		mockPermissionsManager.addOfferedLocalFile(generateSharedFileDescriptor());
		assertEquals(1, mockPermissionsManager.getOfferedLocalFiles().size());
	}

	public void testSetShowRelativePath()
	{
		boolean relativePathSetting = announcer.getShowRelativePath();
		assertEquals(true, relativePathSetting);
		
		announcer.setShowRelativePath(false);
		relativePathSetting = announcer.getShowRelativePath();
		assertEquals(false, relativePathSetting);
		
		announcer.setShowRelativePath(true);
		relativePathSetting = announcer.getShowRelativePath();
		assertEquals(true, relativePathSetting);
	}

	public void testSetShowSharedPath() 
	{
		boolean sharedPathSetting = announcer.getShowSharedPath();
		assertEquals(false, sharedPathSetting);
		
		announcer.setShowSharedPath(true);
		sharedPathSetting = announcer.getShowSharedPath();
		assertEquals(true, sharedPathSetting);
		
		announcer.setShowSharedPath(false);
		sharedPathSetting = announcer.getShowSharedPath();
		assertEquals(false, sharedPathSetting);
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
	
	private FileDescriptor[] generateKnownSharedDummyDescriptorArray(String owner)
	{
		ArrayList<FileDescriptor> fileList = new ArrayList<FileDescriptor>();
		
		FileDescriptor descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };
		descriptor.filename = "house.png";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/photos";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11 };
		descriptor.filename = "backyard.png";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/photos";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12 };
		descriptor.filename = "fireplace.png";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/photos";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13 };
		descriptor.filename = "animals.txt";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/reports";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14 };
		descriptor.filename = "inventors.txt";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/reports";
		descriptor.size = 100;
		fileList.add(descriptor);
		
		descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15 };
		descriptor.filename = "driving.txt";
		descriptor.owner = owner;
		descriptor.relativePath = "";
		descriptor.sharedPath = "sdcard/reports";
		descriptor.size = 100;
		fileList.add(descriptor);

		return fileList.toArray(new FileDescriptor[fileList.size()]);
	}
	
	private ArrayList<String> generatePathsToUnannounce()
	{
		ArrayList<String> unannouncedPaths = new ArrayList<String>();
		
		unannouncedPaths.add("sdcard/photos/house.png");
		unannouncedPaths.add("sdcard/photos/backyard.png");
		unannouncedPaths.add("sdcard/reports/inventors.txt");
		unannouncedPaths.add("sdcard/reports/invalid.txt");
		
		return unannouncedPaths;
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
	
	//Mock Transmitter
	private class MockTransmitter extends Transmitter
	{
		int returnStatusCode;
		
		public MockTransmitter() 
		{
			super(null, null, 0);
		}
		
		@Override
		public int transmit(Action action)
		{
			return returnStatusCode;
		}
	}
	
	//Mock FSA
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
		
//		@Override
//		public String buildPathFromDescriptor(FileDescriptor fd)
//		{
//			return fd.sharedPath + fd.relativePath + "\\" + fd.filename;
//		}
	}
	
	//Mock Application
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
	
	private interface TestListener
	{
		public void sendBackAction(Action action);
	}
}
