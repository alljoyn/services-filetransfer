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
import java.util.ArrayList;
import org.alljoyn.cops.filetransfer.Dispatcher;
import org.alljoyn.cops.filetransfer.ReceiveManager;
import org.alljoyn.cops.filetransfer.FileSystemAbstraction;
import org.alljoyn.cops.filetransfer.PermissionsManager;
import org.alljoyn.cops.filetransfer.Transmitter;
import org.alljoyn.cops.filetransfer.data.Action;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.ProgressDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.FileCompletedListener;
import android.test.AndroidTestCase;

public class ReceiveManagerTest extends AndroidTestCase
{
	private ReceiveManager receiveManager;
	private MockDispatcher dispatcher;
	private MockTransmitter transmitter;
	private MockFsa mockFsa;
	private MockPermissionsManager mockPm;
	
	public void setUp()
	{
		transmitter = new MockTransmitter();
		dispatcher = new MockDispatcher(transmitter);	
		mockFsa = new MockFsa();
		mockPm = new MockPermissionsManager();
		
		FileDescriptor file = getDummyFileDescriptor("");	
		mockPm.updateAnnouncedRemoteFiles(new FileDescriptor[] { file }, file.owner);
		
		receiveManager = new ReceiveManager(dispatcher, mockFsa, mockPm);
	}
	
	public void testSetDefaultSaveDirectory()
	{
		File testDir = new File(getContext().getFilesDir().getAbsolutePath());		
		if (testDir.exists())
		{
			testDir.delete();
		}
		
		assertEquals(StatusCode.BAD_FILE_PATH, receiveManager.setDefaultSaveDirectory("not a vaild path"));
		
		assertEquals(StatusCode.OK, receiveManager.setDefaultSaveDirectory(testDir.getAbsolutePath()));
		
		testDir.delete();
	}
	
	public void testSettingChunkSize()
	{
		int validNum = 10000;
		int invalidNum1 = 0;
		int invalidNum2 = -435;
		
		int status = receiveManager.setMaxChunkSize(validNum);
		assertEquals(StatusCode.OK, status);
		assertEquals(validNum, receiveManager.getMaxChunkSize());
		
		status = receiveManager.setMaxChunkSize(invalidNum1);
		assertEquals(StatusCode.INVALID, status);
		assertEquals(validNum, receiveManager.getMaxChunkSize());
		
		status = receiveManager.setMaxChunkSize(invalidNum2);
		assertEquals(StatusCode.INVALID, status);
		assertEquals(validNum, receiveManager.getMaxChunkSize());
	}
	
	public void testVaildRequestFile()
	{
		FileDescriptor file = getDummyFileDescriptor("");
		
		String owner = file.owner;
		byte[] fileId = file.fileID;
		String saveFileName = "save as different file name";
		String saveFileDirectory = null;
		
		transmitter.setResponse(StatusCode.OK);
		
		int status = receiveManager.requestFile(owner, fileId, saveFileName, saveFileDirectory);
		assertEquals(StatusCode.OK, status);
	}	
	
	public void testInvaildRequestFile1()
	{
		FileDescriptor file = getDummyFileDescriptor("");
		
		String owner = file.owner;
		byte[] fileId = new byte[20]; //bad file id
		String saveFileName = file.filename;
		String saveFileDirectory = null;
		
		transmitter.setResponse(StatusCode.OK);
		
		int status = receiveManager.requestFile(owner, fileId, saveFileName, saveFileDirectory);
		assertEquals(StatusCode.BAD_FILE_ID, status);
	}
	
	public void testInvaildRequestFile2()
	{
		FileDescriptor file = getDummyFileDescriptor("");
		
		String owner = "unkown owner";
		byte[] fileId = file.fileID;
		String saveFileName = file.filename;
		String saveFileDirectory = null;
		
		transmitter.setResponse(StatusCode.OK);
		
		int status = receiveManager.requestFile(owner, fileId, saveFileName, saveFileDirectory);
		assertNotSame(StatusCode.OK, status);
	}
	
	public void testDeniedRequestFile()
	{
		FileDescriptor file = getDummyFileDescriptor("");
		
		String owner = file.owner;
		byte[] fileId = file.fileID;
		String saveFileName = file.filename;
		String saveFileDirectory = null;
		
		transmitter.setResponse(StatusCode.REQUEST_DENIED);
		
		int status = receiveManager.requestFile(owner, fileId, saveFileName, saveFileDirectory);
		assertEquals(StatusCode.REQUEST_DENIED, status);		
	}
	
	public void testRequestFilePaths()
	{
		FileDescriptor file = getDummyFileDescriptor("");
		String defaultSaveDirectory = "/mnt/sdcard/download";		
		
		//verify default save directory used
		receiveManager.setDefaultSaveDirectory(defaultSaveDirectory);
		
		int status = receiveManager.requestFile(file.owner, file.fileID, file.filename, null);
		assertEquals(StatusCode.OK, status);
		
		mockFsa.setExpectedPath(new File(defaultSaveDirectory, file.filename).getAbsolutePath());
		
		receiveManager.handleFileChunk(file.fileID, 0, 0, new byte[1]);	
		
		mockFsa.setExpectDelete(true);
		receiveManager.cancelFile(file.fileID);
		
		//verify specified save directory used, not default
		String differentSaveDirectory =  "/mnt/sdcard/download/testing";
		
		status = receiveManager.requestFile(file.owner, file.fileID, file.filename, differentSaveDirectory);
		assertEquals(StatusCode.OK, status);
		
		mockFsa.setExpectedPath(new File(differentSaveDirectory, file.filename).getAbsolutePath());
		
		receiveManager.handleFileChunk(file.fileID, 0, 0, new byte[1]);
		
		receiveManager.cancelFile(file.fileID);
		
		//verify default save directory used				
		status = receiveManager.requestFile(file.owner, file.fileID, file.filename, null);
		assertEquals(StatusCode.OK, status);
				
		mockFsa.setExpectedPath(new File(defaultSaveDirectory, file.filename).getAbsolutePath());
				
		receiveManager.handleFileChunk(file.fileID, 0, 0, new byte[1]);	
		
		receiveManager.cancelFile(file.fileID);
	}
	
	public void testGetProgressList()
	{
		FileDescriptor file = getDummyFileDescriptor("");		
		
		int status = receiveManager.requestFile(file.owner, file.fileID, file.filename, null);
		assertEquals(StatusCode.OK, status);		
		
		receiveManager.handleFileChunk(file.fileID, 0, 1, new byte[1]);
		
		ArrayList<ProgressDescriptor> descriptorList = receiveManager.getProgressList();
		assertSame(1, descriptorList.size());
		
		ProgressDescriptor descriptor = descriptorList.get(0);		
		assertSame(1, descriptor.bytesTransferred);				
		assertSame(100, descriptor.fileSize);	
	}
	
	public void testHandleFileChunk()
	{
		FileDescriptor file = getDummyFileDescriptor("");
		
		int status = receiveManager.requestFile(file.owner, file.fileID, file.filename, null);
		assertEquals(StatusCode.OK, status);
		
		receiveManager.handleFileChunk(file.fileID, 0, 1, new byte[1]);
		receiveManager.handleFileChunk(file.fileID, 1, 1, new byte[1]);
		receiveManager.handleFileChunk(file.fileID, 2, 1, new byte[1]);
		
		int bytesReceived = receiveManager.getProgressList().get(0).bytesTransferred;
		assertEquals(3, bytesReceived);
		
		//ensure out of order chunk added
		receiveManager.handleFileChunk(file.fileID, 10, 1, new byte[1]);
		
		bytesReceived = receiveManager.getProgressList().get(0).bytesTransferred;
		assertEquals(4, bytesReceived);
		
		//ensure duplicate chunk not added
		receiveManager.handleFileChunk(file.fileID, 0, 1, new byte[1]);
		
		bytesReceived = receiveManager.getProgressList().get(0).bytesTransferred;
		assertEquals(4, bytesReceived);
	}
	
	public void testDataXferCancelled()
	{
		final FileDescriptor file = getDummyFileDescriptor("");
		
		receiveManager.setFileCompletedListener(new FileCompletedListener()
		{
			public void fileCompleted(String filename, int statusCode)
			{
				assertEquals(file.filename, filename);
				assertEquals(StatusCode.CANCELLED, statusCode);
				file.filename = "cancelled";
			}			
		});
		
		int status = receiveManager.requestFile(file.owner, file.fileID, file.filename, null);
		assertEquals(StatusCode.OK, status);
		
		receiveManager.handleDataXferCancelled(file.fileID, file.owner);
		
		assertEquals("cancelled", file.filename);	
		
		//ensure invalid request properly handled
		receiveManager.setFileCompletedListener(new FileCompletedListener()
		{
			public void fileCompleted(String filename, int statusCode)
			{
				fail("listener triggered on invalid cancel notification");
			}			
		});
		
		receiveManager.handleDataXferCancelled(new byte[20], file.owner);		
	}
	
	public void testPauseFile()
	{
		FileDescriptor file = getDummyFileDescriptor("");
		
		int status = receiveManager.requestFile(file.owner, file.fileID, file.filename, null);
		assertEquals(StatusCode.OK, status);
		
		mockFsa.setExpectDelete(false);
		
		status = receiveManager.pauseFile(file.fileID);
		assertEquals(StatusCode.OK, status);
		
		ArrayList<ProgressDescriptor> progressList = receiveManager.getProgressList();
		assertEquals(1, progressList.size());
		
		//ensure invalid pause case handled
		status = receiveManager.pauseFile(new byte[20]);
		assertEquals(StatusCode.BAD_FILE_ID, status);
	}
	
	public void testCancelFile()
	{
		FileDescriptor file = getDummyFileDescriptor("");	
		
		int status = receiveManager.requestFile(file.owner, file.fileID, file.filename, null);
		assertEquals(StatusCode.OK, status);
		
		mockFsa.setExpectDelete(true);
		
		status = receiveManager.cancelFile(file.fileID);
		assertEquals(StatusCode.OK, status);
		
		ArrayList<ProgressDescriptor> progressList = receiveManager.getProgressList();
		assertEquals(0, progressList.size());
		
		//ensure invalid cancel case handled
		status = receiveManager.cancelFile(new byte[20]);
		assertEquals(StatusCode.BAD_FILE_ID, status);
	}
	
	private FileDescriptor getDummyFileDescriptor(String relativePath)
	{
		FileDescriptor descriptor = new FileDescriptor();
		descriptor.fileID = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
		descriptor.filename = "foo";
		descriptor.owner = "bar";
		descriptor.relativePath = relativePath;
		descriptor.sharedPath = "";
		descriptor.size = 100;
		
		return descriptor;
	}
	
	private class MockDispatcher extends Dispatcher
	{
		public MockDispatcher(Transmitter transmitter)
		{
			super(transmitter);
		}		
	}
	
	private class MockTransmitter extends Transmitter
	{
		private int nextResponse;
		
		public MockTransmitter()
		{
			super(null, null, 0);
		}		
		
		public void setResponse(int statusCode)
		{
			nextResponse = statusCode;
		}
		
		@Override
		public int transmit(Action action)
		{
			return nextResponse;
		}
	}
	
	private class MockFsa extends FileSystemAbstraction
	{
		private String expectedPath;	
		private boolean expectDelete;
		
		@Override
		public boolean isValid(String dir)
		{
			if (dir.equals("invalid"))
			{
				return false;
			}
			return true;			
		}
		
		public void setExpectedPath(String path)
		{
			expectedPath = path;
		}
		
		public void setExpectDelete(boolean bool)
		{
			expectDelete = bool;
		}
		
		@Override()
		public int addChunk(String path, byte[] chunk, int startOffset, int length) 
		{
			if (expectedPath != null)
			{
				assertEquals(expectedPath, path);
			}
			return StatusCode.OK;
		}
		
		@Override()
		public int delete(String path)
		{
			if (expectDelete == false)
			{
				fail("file deleted unexpectedly");
			}
			return StatusCode.OK;
		}
	}
	
	private class MockPermissionsManager extends PermissionsManager
	{

	}
}
