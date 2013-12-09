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
import org.alljoyn.cops.filetransfer.PermissionsManager;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;

import android.test.AndroidTestCase;

public class PermissionsManagerTest extends AndroidTestCase
{
	private PermissionsManager permissionsManager;
	
	public void setUp()
	{
		permissionsManager = new PermissionsManager();		
	}
	
	public void testAnnounceFiles()
	{
		FileDescriptor[] knownDescriptors = generateKnownDescriptors("foo");
		
		permissionsManager.addAnnouncedLocalFiles(knownDescriptors);
		
		ArrayList<FileDescriptor> files = permissionsManager.getAnnouncedLocalFiles();	
		assertEquals(6, files.size());
		
		for (FileDescriptor fd : knownDescriptors)
		{
			assertTrue(files.contains(fd));		
		}
	}
	
	public void testRemoveAnnounceFiles()
	{
		testAnnounceFiles();
		
		ArrayList<String> paths = new ArrayList<String>();
		paths.add("sdcard/photos/house.png");
		paths.add("sdcard/reports/inventors.txt");
		paths.add("not here 1");
		paths.add("not here 2");
		
		ArrayList<String> failed = permissionsManager.removeAnnouncedLocalFiles(paths);
		assertEquals(2, failed.size());
		assertEquals(4, permissionsManager.getAnnouncedLocalFiles().size());
	}
	
	public void testAddKnownFiles()
	{
		FileDescriptor[] knownDescriptors = generateKnownDescriptors("foo");
		
		permissionsManager.updateAnnouncedRemoteFiles(knownDescriptors, "bar");
		permissionsManager.updateAnnouncedRemoteFiles(knownDescriptors, "bar");
		
		ArrayList<FileDescriptor> files = permissionsManager.getAvailableRemoteFiles();	
		assertEquals(6, files.size());
		
		for (FileDescriptor fd : knownDescriptors)
		{
			assertTrue(files.contains(fd));		
		}
	}
	
	public void testAddOfferedFiles()
	{
		FileDescriptor[] knownDescriptors = generateKnownDescriptors("foo");
		
		FileDescriptor knownDescriptor1 = knownDescriptors[0];		
		permissionsManager.addOfferedLocalFile(knownDescriptor1);
		
		FileDescriptor knownDescriptor2 = knownDescriptors[5];
		permissionsManager.addOfferedLocalFile(knownDescriptor2);
		
		ArrayList<FileDescriptor> files = permissionsManager.getOfferedLocalFiles();	
		assertEquals(2, files.size());		

		assertTrue(files.contains(knownDescriptor1));	
		assertTrue(files.contains(knownDescriptor2));
	}
	
	public void testGetFileId()
	{
		testAddKnownFiles();
		
		byte[] fileId = permissionsManager.getFileID("foo", "sdcard/photos/house.png");
		assertNotNull(fileId);
		
		fileId = permissionsManager.getFileID("foo", "not here");
		assertNull(fileId);
	}
	
	public void testGetLocalFileDescriptor1()
	{
		FileDescriptor[] knownDescriptors = generateKnownDescriptors("foo");
		
		permissionsManager.addAnnouncedLocalFiles(knownDescriptors);
		
		assertNotNull(permissionsManager.getLocalFileDescriptor(knownDescriptors[3].fileID));
		assertNull(permissionsManager.getLocalFileDescriptor(new byte[20]));		
	}
	
	public void testGetLocalFileDescriptor2()
	{
		testAddOfferedFiles();
		
		assertNotNull(permissionsManager.getLocalFileDescriptor(permissionsManager.getOfferedLocalFiles().get(1).fileID));
		assertNull(permissionsManager.getLocalFileDescriptor(new byte[20]));
	}	
	
	private FileDescriptor[] generateKnownDescriptors(String owner)
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
}
