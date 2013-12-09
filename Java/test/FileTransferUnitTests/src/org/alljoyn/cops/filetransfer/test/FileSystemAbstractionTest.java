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
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import org.alljoyn.cops.filetransfer.FileSystemAbstraction;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import android.test.AndroidTestCase;

/**
 * @author cchiou
 *
 */
public class FileSystemAbstractionTest extends AndroidTestCase
{

	private String rootPath;
	private File testDir;
	private static final String LOCALID = "ME";
	private static final String TESTPATH = "/FTC_TEST";//Environment.getExternalStorageDirectory().getAbsolutePath() + "/FTC_TEST";
	private static final String SUBPATH = "/FTC_SUB";
//	private static final String FULLTESTPATH = TESTPATH + SUBPATH;
	private static final int NUMFILES = 10;
	private static final int NUMBADPATH = 10;
	private FileSystemAbstraction fsa;
	private String localBusId;
	private Random rand;
	
	/* (non-Javadoc)
	 * @see android.test.AndroidTestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		fsa = FileSystemAbstraction.getInstance();
		localBusId = LOCALID;
		rand = new Random(System.currentTimeMillis());
		
		// create files on internal storage
		rootPath = getContext().getFilesDir().getAbsolutePath();
		
//		File testDir = getContext().getDir(rootPath + FULLTESTPATH, Context.MODE_PRIVATE);
		// create files on external storage
		///*
		testDir = new File(rootPath + TESTPATH);

	    deleteTestDirectory();

        assertFalse(testDir.exists());
        
		if (!testDir.mkdirs()) 
		{
			fail("Unable to create test directory");
		} else if (!testDir.canWrite())
		{
			fail("Unable to write to directory");
		}
		//*/
				
		assertTrue(testDir.isDirectory());
		assertTrue(testDir.list().length == 0);
		
		super.setUp();
	}

	/* (non-Javadoc)
	 * @see android.test.AndroidTestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
        deleteTestDirectory();

		super.tearDown();
	}

    /**
     * Test method for {@link org.alljoyn.cops.filetransfer.FileSystemAbstraction#setCacheFile(java.io.File)}.
     */
	public void testSetCacheFile()
	{
        System.out.println("testSetCacheFile");
        
	    File cacheFile = new File(this.testDir + File.separator + "testSetCacheFile.cache");
	    assertFalse(cacheFile.exists());
        
	    fsa.setCacheFile(cacheFile);
        assertTrue(cacheFile.exists());
        
        // Create test files -- should update cache file
        ArrayList<File> validFiles1 = createHashAndCheckCacheChange(cacheFile, null, true, true);
       
        // Create more test files -- should update cache file
        ArrayList<File> validFiles2 = createHashAndCheckCacheChange(cacheFile, null, true, true);
       
        // Lookup file from first test set -- should not update cache file
        ArrayList<File> validFiles3 = createHashAndCheckCacheChange(cacheFile, validFiles1, false, false);
        assertTrue(validFiles3 == validFiles1);
       
        // Turn off caching and create more test files -- should not update previous cache file
        fsa.setCacheFile((File)null);    
        ArrayList<File> validFiles4 = createHashAndCheckCacheChange(cacheFile, null, false, false);
        
        // Switch back to previous cache file, and lookup file from second test set -- should not update cache file
        fsa.setCacheFile(cacheFile);
        ArrayList<File> validFiles5 = createHashAndCheckCacheChange(cacheFile, validFiles2, false, false);
        assertTrue(validFiles5 == validFiles2);
        
        // Modify file from fifth test set (which is the same as the second test set) -- should update cache file
        addChunkToFile(validFiles5.get(0));
        ArrayList<File> validFiles6 = createHashAndCheckCacheChange(cacheFile, validFiles5, true, false);        
	}
	
    /**
     * Test method for {@link org.alljoyn.cops.filetransfer.FileSystemAbstraction#cleanCacheFile()}.
     */
    public void testCleanCacheFile()
    {
        System.out.println("testCleanCacheFile");
        
        File cacheFile = new File(this.testDir + File.separator + "testCleanCacheFile.cache");
        assertFalse(cacheFile.exists());
        
        fsa.setCacheFile(cacheFile);
        assertTrue(cacheFile.exists());
        
        // Create test files -- should update cache file
        ArrayList<File> validFiles1 = createHashAndCheckCacheChange(cacheFile, null, true, true);
    
        // Verify a file is in the cache -- should not update cache file
        createHashAndCheckCacheChange(cacheFile, validFiles1, false, false);
    
        // Cleaning with an empty delete set shouldn't change things -- verify the cache is the same size before and after
        deleteCleanAndCheckCacheLength(cacheFile, null, false);
        
        // Delete files that are in the cache -- cleaning should then reduce the size of the cache 
        deleteCleanAndCheckCacheLength(cacheFile, validFiles1, true);
	}
	
	/**
	 * Test method for {@link org.alljoyn.cops.filetransfer.FileSystemAbstraction#getFileInfo(java.util.ArrayList, java.util.ArrayList, java.lang.String)}.
	 */
	public void testGetFileInfo()
	{
		System.out.println("testGetFileInfo");	

        // build an array of valid paths
        ArrayList<String> validPaths = fileArraytoPathArray(createValidFiles(NUMFILES));
		
		// build an array of invalid paths
		ArrayList<String> badPaths = new ArrayList<String>();
		for (int i = 0; i<NUMBADPATH; i++ )
		{
			badPaths.add(Integer.toString(rand.nextInt()) + "bad");
		}
		
		System.out.println("Bad Paths:");
		for (String path : badPaths)
		{
			System.out.println(path);
		}
				
		System.out.println("Valid Paths:");
		for (String path : validPaths)
		{
			System.out.println(path);
		}
		
		int expectedBad;
		int expectedValid;
		FileDescriptor[] fdArray;
		ArrayList<String> failedPaths = new ArrayList<String>();
		
		failedPaths.clear();
		expectedBad = badPaths.size();
		expectedValid = 0;
		fdArray = fsa.getFileInfo(badPaths, failedPaths, localBusId);
		assertEquals(expectedBad, failedPaths.size());
		assertEquals(expectedValid, fdArray.length);
		
		failedPaths.clear();
		expectedBad = 0;
		expectedValid = validPaths.size();
		fdArray = fsa.getFileInfo(validPaths, failedPaths, localBusId);
		assertEquals(expectedBad, failedPaths.size());
		assertEquals(expectedValid, fdArray.length);	
	}

	/**
	 * Test method for {@link org.alljoyn.cops.filetransfer.FileSystemAbstraction#getChunk(java.lang.String, byte[], int, int)}.
	 */
	public void testGetChunk()
	{
		int maxBytes = 5242880; // 5MB
		int fileSize = rand.nextInt(maxBytes);

		File testFile = new File(testDir, rand.nextInt() + ".test");
		RandomAccessFile raf = null;
		try
		{
			// create new file in test directory
			raf = new RandomAccessFile(testFile, "rw");
			// set size of file
			raf.setLength(fileSize);
		} catch (Exception e)
		{
			fail(e.toString());
		}
		
		byte[] chunk = null;
		int length = 0;
		int jumps = 0;
		int bytesRead = 0;
		int expectedSize = 0;
		for (int r = 0; r<50; r++)
		{
			length = rand.nextInt(524288);	// max 0.5MB
			jumps = rand.nextInt(length);
			chunk = new byte[length];
			for (int i=0; i<fileSize; i+=jumps)
			{
				if ((i+length) > fileSize)
				{
					expectedSize = fileSize-i;
				} else
				{
					expectedSize = length;
				}
				try
				{
					bytesRead = fsa.getChunk(testFile.getAbsolutePath(), chunk, i, length);
				} catch (Exception e)
				{
					fail(e.toString());
				} 
				if (bytesRead != expectedSize)
				{
					fail("number of bytes read (" 
							+ bytesRead + ") does not match expected size ("
							+ expectedSize +")");
				}
			}
		}
	}

	/**
	 * Test method for {@link org.alljoyn.cops.filetransfer.FileSystemAbstraction#addChunk(java.lang.String, byte[], int, int)}.
	 */
	public void testAddChunk()
	{
		int maxBytes = 5242880; // 5MB
//		int fileSize = rand.nextInt(maxBytes);
		int length = 0;
		int start = 0;
		int status = 0;
		int totLength = 0;
		byte[]chunk;
		
		String filePath = testDir.getAbsolutePath() +"/" + rand.nextInt() + ".test";
		
		for (int i=start; i<maxBytes; i+=length)
		{
			status = 0;
			length = rand.nextInt(2048); // max 2KB
			chunk = new byte[length];
			try
			{
				status = fsa.addChunk(filePath, chunk, start, length);
			} catch (Exception e)
			{
				fail(e.toString());
			} 
			if (status == 0)
			{
				fail("append failed");
			}
			start += length;
			totLength += length;
		}
		
		
		File testFile  = new File(filePath);
		if (totLength != testFile.length())
		{
			fail("number of bytes appended (" 
					+ totLength + ") does not match file size ("
					+ testFile.length() +")");
		}
	}

	/**
	 * Test method for {@link org.alljoyn.cops.filetransfer.FileSystemAbstraction#delete(java.lang.String)}.
	 */
	public void testDelete()
	{
        ArrayList<File> validFiles = createValidFiles(NUMFILES);
		
		int status;
		for (File testFile : validFiles)
		{
			status = fsa.delete(testFile.getAbsolutePath());
			if (status == 0)
			{
				fail("Failed to delete " + testFile.getName());
			}
		}
	}

	/**
	 * Test method for {@link org.alljoyn.cops.filetransfer.FileSystemAbstraction#buildPathFromDescriptor(org.alljoyn.cops.filetransfer.data.FileDescriptor)}.
	 */
	public void testBuildPathFromDescriptor()
	{
		FileDescriptor fd = new FileDescriptor();
		fd.filename = "foo";
		fd.sharedPath = testDir.getAbsolutePath();
		fd.relativePath = SUBPATH;
		String testPath = fsa.buildPathFromDescriptor(fd);
		String expectedPath = testDir.getAbsolutePath() + SUBPATH + "/" + fd.filename; 
		if (!testPath.equals(expectedPath))
		{
			fail("testPath: " + testPath + "expected path: " + expectedPath);
		}
	}

	/**
	 * Test method for {@link org.alljoyn.cops.filetransfer.FileSystemAbstraction#isValid(java.lang.String)}.
	 */
	public void testIsValid()
	{
        ArrayList<File> validFiles = createValidFiles(NUMFILES);
		
		boolean status;
		for (File testFile : validFiles)
		{
			status = fsa.isValid(testFile.getAbsolutePath());
			if (!status)
			{
				fail(testFile.getName() + " is not valid.");
			}
		}
	}
	
	/*
	 * Helper method for testSetCacheFile(). Modifies a test file by adding arbitrary data to it.
	 */
	private void addChunkToFile(File file)
	{
	    try
	    {
	        fsa.addChunk(file.getAbsolutePath(), new byte[] {1,2,3,4}, 0, 4);
	    }
	    catch (Exception e)
	    {
	        fail(e.getMessage());
	    }
    }
    
	
    /*
     * Helper method for testSetCacheFile(). Provokes cache activity and verifies results.
     */
	private ArrayList<File> createHashAndCheckCacheChange( File cacheFile, ArrayList<File> validFiles, boolean shouldChange, boolean shouldGrow )
	{
        long lastModifiedBefore = cacheFile.lastModified();
        long lengthBefore = cacheFile.length();
        
        if (validFiles == null)
        {
            validFiles = createValidFiles(1);
        }
        
        // Sleep to account for the timestamp granularity of some filesystems (up to 1000
        // according to http://docs.oracle.com/javase/6/docs/api/java/io/File.html#setLastModified(long) )
        // plus a slop factor.
        try { Thread.sleep(1300); } catch (Exception e) {}

        fsa.getFileInfo(fileArraytoPathArray(validFiles), new ArrayList<String>(), localBusId);
                
        long lastModifiedAfter = cacheFile.lastModified();
        long lengthAfter = cacheFile.length();
        
        if (shouldChange)
        {
            assertTrue(lastModifiedBefore < lastModifiedAfter);
            assertTrue(shouldGrow ? lengthBefore < lengthAfter : lengthBefore == lengthAfter);
        }
        else
        {
            assertTrue(lastModifiedBefore == lastModifiedAfter);
            assertTrue(lengthBefore == lengthAfter);
        }
        
        return validFiles;
	}
	
    /*
     * Helper method for testCleanCacheFile(). Cleans the cache and verifies results.
     */
    private void deleteCleanAndCheckCacheLength( File cacheFile, ArrayList<File> deleteFiles, boolean shouldShrink )
    {
        long lengthBefore = cacheFile.length();
        
        if (deleteFiles != null)
        {
            for (File file: deleteFiles)
            {
                file.delete();
            }
        }
        
        fsa.cleanCacheFile();

        long lengthAfter = cacheFile.length();
        
        if (shouldShrink)
        {
            assertTrue(lengthBefore > lengthAfter);
        }
        else
        {
            assertTrue(lengthBefore == lengthAfter);
        }
    }
    
    /*
     * Helper method for various test methods. Converts an array of File objects to an array
     * of absolute paths strings.
     */
    private ArrayList<String> fileArraytoPathArray(ArrayList<File> fileArray)
    {
      ArrayList<String> pathArray = new ArrayList<String>();
      
      for (File file : fileArray)
      {
          pathArray.add(file.getAbsolutePath());
      }
      
      return pathArray;
    }

	/*
	 * Helper method for various test methods. Creates a set of valid files for other tests to use.
	 */
	private ArrayList<File> createValidFiles(int numFiles)
	{
        ArrayList<File> validFiles = new ArrayList<File>();
        File newFile;
        FileOutputStream fos;
        for (int i=0; i<numFiles; i++)
        {
            newFile = new File(testDir, rand.nextInt() + ".test");
            try
            {
                fos = new FileOutputStream(newFile);
                fos.write(newFile.getName().getBytes());
                fos.close();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            validFiles.add(newFile);
        }
        
        return validFiles;
	}
	
    /*
     * Helper method for various test methods. Deletes all files from the test directory, and
     * then deletes the test directory.
     */
	private void deleteTestDirectory()
	{
        if (testDir.exists())
        {
            LinkedList<File> fileList = new LinkedList<File>();
            fileList.add(testDir);
            
            while (!fileList.isEmpty())
            {
                File file = fileList.removeFirst();
                if (file.isFile())
                {
                    if (!file.delete())
                    {
                        fail("Failed to delete " + file.getName());
                    }
                } else
                {
                    File[] subList = file.listFiles();
                    if (subList.length == 0)
                    {
                        if (!file.delete())
                        {
                            fail("Failed to delete " + file.getName());
                        }
                    } else
                    {
                        fileList.addAll(Arrays.asList(subList));
                        fileList.add(file);
                    }
                }
            }
        }
	}
}
