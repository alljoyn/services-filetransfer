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

#import "FTMFileSystemAbstractionTests.h"

@interface FTMFileSystemAbstractionTests()

@property (nonatomic, strong) FTMFileSystemAbstraction *fsa;
@property (nonatomic, strong) NSString *bundleSavePath;
@property (nonatomic, strong) NSString *localBusID;

@end

@implementation FTMFileSystemAbstractionTests

@synthesize fsa = _fsa;
@synthesize bundleSavePath = _bundleSavePath;
@synthesize localBusID = _localBusID;

- (void)setUp
{
    [super setUp];
    
    self.fsa = [FTMFileSystemAbstraction instance];
    self.bundleSavePath = [[NSBundle bundleForClass: [self class]] resourcePath];
    self.localBusID = @"me";
}

- (void)tearDown
{
    self.fsa = nil;
    
    [super tearDown];
}

-(void)testSetCacheFile
{
    [self createTestImageFile];
    
    NSFileManager *fm = [NSFileManager defaultManager];
    NSString *cacheFilePathA = [self.bundleSavePath stringByAppendingPathComponent: @"cacheFileA.cache"];
    
    //Set Cache File A
    [self.fsa setCacheFileWithPath: cacheFilePathA];
    [self waitForCompletion: 5.0f];
    
    //Test to see if CacheFileA exists
    BOOL isDir;
    BOOL cacheAExists = [fm fileExistsAtPath: cacheFilePathA isDirectory: &isDir];
    STAssertTrue(cacheAExists, @"cache file A exists");
    STAssertFalse(isDir, @"cache A is not a directory");
    
    //Store last modified time for cache file A
    NSDictionary *fileAttributes = [fm attributesOfItemAtPath: cacheFilePathA error: nil];
    NSDate *lastModifiedDate = [fileAttributes objectForKey: NSFileModificationDate];
    [self waitForCompletion: 5.0f];
    
    //Call getFileInfo and pass it the testImage path and check to see if the cache file is modified
    NSString *filePath = [self.bundleSavePath stringByAppendingPathComponent: @"testImage.png"];
    NSArray *descriptorArray = [self.fsa getFileInfo: [[NSArray alloc] initWithObjects: filePath, nil] withFailedPathsArray: [[NSMutableArray alloc] init] andLocalBusID: @"me"];
    [self waitForCompletion: 5.0f];
    STAssertTrue([descriptorArray count] == 1, @"one descriptor was returned");
    
    //Test to see if the file has a different modified date
    fileAttributes = [fm attributesOfItemAtPath: cacheFilePathA error: nil];
    NSDate *currentModifiedTime = [fileAttributes objectForKey: NSFileModificationDate];
    STAssertTrue([currentModifiedTime compare: lastModifiedDate] == NSOrderedDescending, @"cache file modified");
    lastModifiedDate = currentModifiedTime;
    
    //Change to cache file B
    NSString *cacheFilePathB = [self.bundleSavePath stringByAppendingPathComponent: @"cacheFileB.cache"];
    
    //Set cache file B
    [self.fsa setCacheFileWithPath: cacheFilePathB];
    [self waitForCompletion: 5.0f];
    
    //Test to see if CacheFileA exists
    BOOL cacheBExists = [fm fileExistsAtPath: cacheFilePathB isDirectory: &isDir];
    STAssertTrue(cacheBExists, @"cache file B exists");
    STAssertFalse(isDir, @"cache B is not a directory");
    
    //Store last modified time for cache file B
    fileAttributes = [fm attributesOfItemAtPath: cacheFilePathB error: nil];
    lastModifiedDate = [fileAttributes objectForKey: NSFileModificationDate];
    [self waitForCompletion: 5.0f];
    
    //Call getFileInfo and pass it the testImage path and check to see if the cache file is modified
    descriptorArray = [self.fsa getFileInfo: [[NSArray alloc] initWithObjects: filePath, nil] withFailedPathsArray: [[NSMutableArray alloc] init] andLocalBusID: @"me"];
    [self waitForCompletion: 5.0f];
    STAssertTrue([descriptorArray count] == 1, @"one descriptor was returned");
    
    //Test to see if the file has a different modified date
    fileAttributes = [fm attributesOfItemAtPath: cacheFilePathB error: nil];
    currentModifiedTime = [fileAttributes objectForKey: NSFileModificationDate];
    STAssertTrue([currentModifiedTime compare: lastModifiedDate] == NSOrderedDescending, @"cache file modified");
    lastModifiedDate = currentModifiedTime;
    
    //Check to see if the contents of both cache files are identical
    NSFileHandle *cacheHandleA = [NSFileHandle fileHandleForReadingAtPath: cacheFilePathA];
    NSFileHandle *cacheHandleB = [NSFileHandle fileHandleForReadingAtPath: cacheFilePathB];
    NSData *fileDataCacheA = [cacheHandleA readDataToEndOfFile];
    NSData *fileDataCacheB = [cacheHandleB readDataToEndOfFile];
    STAssertTrue([fileDataCacheA isEqualToData: fileDataCacheB], @"two cache files are equal");
    
    //Switch back over to cache file A
    [self.fsa setCacheFileWithPath: cacheFilePathA];
    [self waitForCompletion: 5.0f];
    
    //Test to see if CacheFileA exists
    cacheAExists = [fm fileExistsAtPath: cacheFilePathA isDirectory: &isDir];
    STAssertTrue(cacheAExists, @"cache file A exists");
    STAssertFalse(isDir, @"cache A is not a directory");
    
    //Store last modified time for cache file A
    fileAttributes = [fm attributesOfItemAtPath: cacheFilePathA error: nil];
    lastModifiedDate = [fileAttributes objectForKey: NSFileModificationDate];
    [self waitForCompletion: 5.0f];
    
    //Call getFileInfo and pass it the testImage path and check to see if the cache file is modified
    [self.fsa getFileInfo: [[NSArray alloc] initWithObjects: filePath, nil] withFailedPathsArray: [[NSMutableArray alloc] init] andLocalBusID: @"me"];
    [self waitForCompletion: 5.0f];
    
    //Test to see if the cache file A has a different modified date. It should not have been modified because the 
    fileAttributes = [fm attributesOfItemAtPath: cacheFilePathA error: nil];
    currentModifiedTime = [fileAttributes objectForKey: NSFileModificationDate];
    STAssertTrue([currentModifiedTime compare: lastModifiedDate] == NSOrderedSame, @"cache file not modified");
    lastModifiedDate = currentModifiedTime;
    
    //Resave the testImage file
    [self createTestImageFile];
    [self waitForCompletion: 5.0f];
    
    //Call getFileInfo and pass it the testImage path and check to see if the cache file is modified
    [self.fsa getFileInfo: [[NSArray alloc] initWithObjects: filePath, nil] withFailedPathsArray: [[NSMutableArray alloc] init] andLocalBusID: @"me"];
    [self waitForCompletion: 5.0f];
    
    //Test to see if the cache file A has a different modified date. It should not have been modified because the
    fileAttributes = [fm attributesOfItemAtPath: cacheFilePathA error: nil];
    currentModifiedTime = [fileAttributes objectForKey: NSFileModificationDate];
    STAssertTrue([currentModifiedTime compare: lastModifiedDate] == NSOrderedDescending, @"cache file not modified");
    
    //Disable caching
    [self.fsa setCacheFileWithPath: nil];
    
    //delete test file and cache files
    [self deleteTestFile];
    [fm removeItemAtPath: cacheFilePathA error: nil];
    [fm removeItemAtPath: cacheFilePathB error: nil];
}

-(void)testCleanCacheFile
{
    [self createTestImageFile];
    
    NSFileManager *fm = [NSFileManager defaultManager];
    NSString *cacheFilePath = [self.bundleSavePath stringByAppendingPathComponent: @"cacheFile.cache"];
    BOOL cacheAExists = [fm fileExistsAtPath: cacheFilePath];
    STAssertFalse(cacheAExists, @"cache file should not exist at start of test");
    
    //Set Cache File A
    [self.fsa setCacheFileWithPath: cacheFilePath];
    [self waitForCompletion: 5.0f];
    
    //Test to see if CacheFileA exists
    BOOL isDir;
    cacheAExists = [fm fileExistsAtPath: cacheFilePath isDirectory: &isDir];
    STAssertTrue(cacheAExists, @"cache file A exists");
    STAssertFalse(isDir, @"cache A is not a directory");
    
    //Store last modified time for cache file A
    NSDictionary *fileAttributes = [fm attributesOfItemAtPath: cacheFilePath error: nil];
    NSDate *lastModifiedDate = [fileAttributes objectForKey: NSFileModificationDate];
    NSNumber *fileSizeNumber = [fileAttributes objectForKey: NSFileSize];
    unsigned long long lastFileSize = [fileSizeNumber unsignedLongLongValue];
    [self waitForCompletion: 5.0f];
    
    //Call getFileInfo and pass it the testImage path and check to see if the cache file is modified
    NSString *filePath = [self.bundleSavePath stringByAppendingPathComponent: @"testImage.png"];
    NSArray *descriptorArray = [self.fsa getFileInfo: [[NSArray alloc] initWithObjects: filePath, nil] withFailedPathsArray: [[NSMutableArray alloc] init] andLocalBusID: @"me"];
    [self waitForCompletion: 5.0f];
    STAssertTrue([descriptorArray count] == 1, @"one descriptor was returned");
    
    //Test to see if the file has a different modified date
    fileAttributes = [fm attributesOfItemAtPath: cacheFilePath error: nil];
    NSDate *currentModifiedTime = [fileAttributes objectForKey: NSFileModificationDate];
    fileSizeNumber = [fileAttributes objectForKey: NSFileSize];
    unsigned long long currentFileSize = [fileSizeNumber unsignedLongLongValue];
    STAssertTrue([currentModifiedTime compare: lastModifiedDate] == NSOrderedDescending, @"cache file modified");
    STAssertTrue(lastFileSize < currentFileSize, @"cache file size grew");
    lastFileSize = currentFileSize;
    
    //clean the cache file and ensure that the size of the file does not change
    [self waitForCompletion: 5.0f];
    [self.fsa cleanCacheFile];
    [self waitForCompletion: 5.0f];
    fileAttributes = [fm attributesOfItemAtPath: cacheFilePath error: nil];
    fileSizeNumber = [fileAttributes objectForKey: NSFileSize];
    currentFileSize = [fileSizeNumber unsignedLongLongValue];
    STAssertTrue(lastFileSize == currentFileSize, @"file size did not change");
    lastFileSize = currentFileSize;

    //Delete the testImage and then ensure the cache file is written to
    [self deleteTestFile];
    [self waitForCompletion: 5.0f];
    [self.fsa cleanCacheFile];
    [self waitForCompletion: 5.0f];
    fileAttributes = [fm attributesOfItemAtPath: cacheFilePath error: nil];
    fileSizeNumber = [fileAttributes objectForKey: NSFileSize];
    currentFileSize = [fileSizeNumber unsignedLongLongValue];
    STAssertTrue(lastFileSize > currentFileSize, @"file size not change");
    
    //Disable caching
    [self.fsa setCacheFileWithPath: nil];
    
    [self waitForCompletion: 5.0f];
    
    //Delete cache file
    [fm removeItemAtPath: cacheFilePath error: nil];
}

-(void)testGetFileInfo
{
    //Create test file
    [self createTestImageFile];
    
    //Prepare Dummy Data
    NSMutableArray *failedPaths = [[NSMutableArray alloc] init];
    
    NSMutableArray *paths = [[NSMutableArray alloc] init];
    NSString *filePath = [self.bundleSavePath stringByAppendingPathComponent: @"invalid_file.pdf"];
    [paths addObject: filePath];
    filePath = [self.bundleSavePath stringByAppendingPathComponent: @"testImage.png"];
    [paths addObject: filePath];
    filePath = [self.bundleSavePath stringByAppendingPathComponent: @"invalid_file.jpg"];
    [paths addObject: filePath];
    filePath = [self.bundleSavePath stringByAppendingPathComponent: @"invalid_file.txt"];
    [paths addObject: filePath];
    
    //Make the call to getFileInfo on the FSA
    NSArray *descriptorArray = [self.fsa getFileInfo: [[NSArray alloc] initWithArray: paths] withFailedPathsArray: failedPaths andLocalBusID: self.localBusID];
    
    //Test we have 3 failed paths and 1 descriptor
    STAssertTrue([failedPaths count] == 3, @"three failed paths expected");
    STAssertTrue([descriptorArray count] == 1, @"one descriptor returned in the arrayt");
    
    //Test to see if the path is what we expected it to be
    NSString *expectedPath = [self.bundleSavePath stringByAppendingPathComponent:@"testImage.png"];
    NSString *descriptorPath = [self.fsa buildPathFromDescriptor: [descriptorArray objectAtIndex: 0]];
    STAssertTrue([expectedPath isEqualToString: descriptorPath], @"paths to file are equal");
    
    //Test to see if the file ID's match. We calculate our own file ID for the file.
    FTMFileDescriptor *descriptor = [descriptorArray objectAtIndex: 0];
    NSFileHandle *handle = [NSFileHandle fileHandleForReadingAtPath: expectedPath];
    NSData *data = [handle readDataToEndOfFile];
    NSData *expectedFileID = [self calculateFileIDUsingFileData: data];
    STAssertTrue([expectedFileID isEqualToData: descriptor.fileID], @"file ID's are equal and calculated separately");
    
    //Test to see if the file sizes match
    NSFileManager *fm = [NSFileManager defaultManager];
    NSDictionary *fileAttributes = [fm attributesOfItemAtPath: expectedPath error: nil];
    NSNumber *fileSizeNumber = [fileAttributes objectForKey: NSFileSize];
    NSInteger expectedSize = [fileSizeNumber integerValue];
    STAssertTrue(expectedSize == descriptor.size, @"file sizes are equal");
    
    //Test to see if the owner field matches the local bus ID we set
    STAssertTrue([self.localBusID isEqualToString: descriptor.owner], @"owner field is equal to local bus ID");
    
    //Delete test file
    [self deleteTestFile];
}

-(void)testGetChunk
{
    [self createTestImageFile];
    
    const unsigned char data[] = { 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12 };
    NSData *expectedChunk = [[NSData alloc] initWithBytes: data length: 8];
    
    NSString *filePath = [self.bundleSavePath stringByAppendingPathComponent: @"testImage.png"];
    NSFileHandle *fileHandle = [NSFileHandle fileHandleForReadingAtPath: filePath];
    [fileHandle seekToFileOffset: 4];
    NSData *readData = [fileHandle readDataOfLength: 8];
    
    STAssertTrue([expectedChunk isEqualToData: readData], @"file chunks are equal");
    
    [self deleteTestFile];
}

-(void)testAddChunk
{
    //Path to file
    NSString *filePath = [self.bundleSavePath stringByAppendingPathComponent: @"testImage.png"];
    
    //Set file with initial data
    const unsigned char fileData[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20 };
    NSData *chunk = [[NSData alloc] initWithBytes: fileData length: 20];
    
    //Call FSA add chunk
    [self.fsa addChunkOfFileWithPath: filePath withData: chunk startingOffset: 0 andLength: 20];
    
    const unsigned char data[] = { 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12 };
    NSData *expectedChunk = [[NSData alloc] initWithBytes: data length: 8];
    
    NSFileHandle *fileHandle = [NSFileHandle fileHandleForReadingAtPath: filePath];
    [fileHandle seekToFileOffset: 4];
    NSData *readData = [fileHandle readDataOfLength: 8];
    
    STAssertTrue([expectedChunk isEqualToData: readData], @"file chunks are equal");
    
    const unsigned char newData[] = { 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27 };
    expectedChunk = [[NSData alloc] initWithBytes: newData length: 8];
    
    //Write the chunk
    NSFileHandle *fh = [NSFileHandle fileHandleForUpdatingAtPath: filePath];
    [fh seekToFileOffset: 4];
    [fh writeData: expectedChunk];
    
    //Read same chunk back
    [fh seekToFileOffset: 4];
    readData = [fh readDataOfLength: 8];
    
    STAssertTrue([expectedChunk isEqualToData: readData], @"file chunks are equal");
    
    [self deleteTestFile];
}

-(void)testDeleteFile
{
    [self createTestImageFile];
    
    NSFileManager *fm = [NSFileManager defaultManager];
    
    NSString *filePath = [self.bundleSavePath stringByAppendingPathComponent: @"testImage.png"];
    BOOL deleteSuccess = [fm removeItemAtPath: filePath error: nil];
    STAssertTrue(deleteSuccess, @"file deleted successfully");
    
    filePath = [self.bundleSavePath stringByAppendingPathComponent: @"invalid_file.txt"];
    deleteSuccess = [fm removeItemAtPath: filePath error: nil];
    STAssertFalse(deleteSuccess, @"file not deleted");
}

-(void)testBuildPathFromDescriptor
{
    NSString *expectedPath = self.bundleSavePath;
    expectedPath = [expectedPath stringByAppendingPathComponent: @"/testDirectory"];
    expectedPath = [expectedPath stringByAppendingPathComponent: @"testDoc.png"];
    
    FTMFileDescriptor *descriptor = [[FTMFileDescriptor alloc] init];
    descriptor.sharedPath = self.bundleSavePath;
    descriptor.relativePath = @"testDirectory";
    descriptor.filename = @"testDoc.png";
    
    NSString *builtPath = [self.fsa buildPathFromDescriptor: descriptor];
    STAssertTrue([expectedPath isEqualToString: builtPath], @"paths are equal");
}

-(void)testIsValidPath
{
    [self createTestImageFile];
    
    NSString *filePath = [self.bundleSavePath stringByAppendingPathComponent: @"testImage.png"];
    STAssertTrue([self.fsa isValidPath: filePath], @"file path is valid");
    
    filePath = [self.bundleSavePath stringByAppendingPathComponent: @"invalid_path.txt"];
    STAssertFalse([self.fsa isValidPath: filePath], @"file path is not valid");
    
    [self deleteTestFile];
}

//Helper Functions
-(void)createTestImageFile
{
    NSString *filePath = [self.bundleSavePath stringByAppendingPathComponent: @"testImage.png"];
    
    const unsigned char fileData[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20 };
    NSData *data = [[NSData alloc] initWithBytes: fileData length: 20];
    
    NSFileManager *fm = [NSFileManager defaultManager];
    [fm createFileAtPath: filePath contents: data attributes: nil];
}

-(void)deleteTestFile
{
    NSString *filePath = [self.bundleSavePath stringByAppendingPathComponent: @"testImage.png"];
    NSFileManager *fm = [NSFileManager defaultManager];
    [fm removeItemAtPath: filePath error: nil];
}

-(NSData *)calculateFileIDUsingFileData: (NSData *)fileData
{
    unsigned char digest[CC_SHA1_DIGEST_LENGTH];
    
    if (CC_SHA1([fileData bytes], [fileData length], digest))
    {
        return [[NSData alloc] initWithBytes: digest length: 20];
    }
    else
    {
        return nil;
    }
}

-(void)waitForCompletion: (NSTimeInterval)timeoutSeconds
{
    NSDate *timeoutDate = [NSDate dateWithTimeIntervalSinceNow: timeoutSeconds];
    
    do
    {
        [[NSRunLoop currentRunLoop] runMode: NSDefaultRunLoopMode beforeDate: timeoutDate];
        if ([timeoutDate timeIntervalSinceNow] < 0.0)
        {
            break;
        }
    } while (YES);
}

@end
