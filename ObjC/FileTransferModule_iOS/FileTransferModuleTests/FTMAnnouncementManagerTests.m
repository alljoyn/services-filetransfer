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

#import "FTMAnnouncementManagerTests.h"
#import <Foundation/Foundation.h>

const NSTimeInterval WAIT_TIME = 10.0f;

@interface FTMAnnouncementManagerTests()

@property (nonatomic, strong) FTMAnnouncementManager *announcementManager;
@property (nonatomic, strong) FTMMockDispatcher *mockDispatcher;
@property (nonatomic, strong) FTMMockFileSystemAbstraction *mockFSA;
@property (nonatomic, strong) FTMMockTransmitter *mockTransmitter;
@property (nonatomic, strong) FTMMockPermissionsManager *mockPermissionManager;
@property (nonatomic, strong) NSString *localBusID;
@property (nonatomic) int failedPathsArraySize;
@property (nonatomic) int announcedFilesArraySize;
@property (nonatomic, strong) NSString *peer;

@end

@implementation FTMAnnouncementManagerTests

@synthesize announcementManager = _announcementManager;
@synthesize mockDispatcher = _mockDispatcher;
@synthesize mockFSA = _mockFSA;
@synthesize mockTransmitter = _mockTransmitter;
@synthesize mockPermissionManager = _mockPermissionManager;
@synthesize localBusID = _localBusID;
@synthesize failedPathsArraySize = _failedPathsArraySize;
@synthesize announcedFilesArraySize = _announcedFilesArraySize;
@synthesize peer = _peer;

- (void)setUp
{
	[super setUp];
    
    self.localBusID = @"AnnouncementManagerUnitTests";
    self.peer = @"me";
    
    self.mockTransmitter = [[FTMMockTransmitter alloc] initWithBusObject: nil busAttachment: nil andSessionID: 0];
    [self.mockTransmitter setDelegate: self];
    
    self.mockDispatcher = [[FTMMockDispatcher alloc] initWithTransmitter: self.mockTransmitter];
    [self.mockDispatcher setAllowDispatching: YES];
    self.mockFSA = [[FTMMockFileSystemAbstraction alloc] init];
    self.mockPermissionManager = [[FTMMockPermissionsManager alloc] init];
    
    self.announcementManager = [[FTMAnnouncementManager alloc] initWithDispatcher: self.mockDispatcher permissionsManager: self.mockPermissionManager fileSystemAbstraction: self.mockFSA andLocalBusID: self.localBusID];
    [self.announcementManager setFileAnnouncementReceivedDelegate: self];
    [self.announcementManager setFileAnnouncementSentDelegate: self];
}

- (void)tearDown
{
    self.announcementManager = nil;
    self.mockDispatcher = nil;
    self.mockFSA = nil;
    self.mockPermissionManager = nil;
    self.mockTransmitter = nil;
    
	[super tearDown];
}

//FTCAnnouncementManager test functions
-(void)testAnnounce
{
    self.announcedFilesArraySize = 0;
    
    NSArray *announcedFiles = [self.mockPermissionManager getAnnouncedLocalFiles];
    NSLog(@"Expected size: %i", self.announcedFilesArraySize);
    NSLog(@"Actual size: %i", [announcedFiles count]);
    STAssertTrue([announcedFiles count] == self.announcedFilesArraySize, @"Announced Files is empty");
    
    self.failedPathsArraySize = 4;
    
    NSArray *pathList = [[NSArray alloc] init];
    [self.announcementManager announceFilePaths: pathList];
    
    self.announcedFilesArraySize = 6;
    
    [self waitForCompletion: WAIT_TIME];
    
    announcedFiles = [self.mockPermissionManager getAnnouncedLocalFiles];
    STAssertTrue([announcedFiles count] == self.announcedFilesArraySize, @"Announced Files contains 6 objects");
}

-(void)testStopAnnounce
{
    [self testAnnounce];
    
    self.failedPathsArraySize = 1;
    self.announcedFilesArraySize = 3;
    
    NSArray *failedPaths = [self.announcementManager stopAnnounceFilePaths: [self generatePathsToUnannounce]];
    
    [self waitForCompletion: WAIT_TIME];
    
    STAssertTrue([failedPaths count] == self.failedPathsArraySize, @"failed paths array sizes match");
    
    NSArray *announcedFiles = [self.mockPermissionManager getAnnouncedLocalFiles];
    STAssertTrue([announcedFiles count] == self.announcedFilesArraySize, @"Announced Files contains 3 objects");
}

-(void)testRequestAnnouncement
{
    //No listener set
    [self.announcementManager setFileAnnouncementReceivedDelegate: nil];
    
    FTMStatusCode status = [self.announcementManager requestAnnouncementFromPeer: self.peer];
    STAssertTrue(status == FTMNoFileAnnouncementListener, @"Listener not set");
    
    //Listener set
    [self.announcementManager setFileAnnouncementReceivedDelegate: self];
    status = [self.announcementManager requestAnnouncementFromPeer: self.peer];
    STAssertTrue(status == FTMOK, @"Listener not set");
    
    [self waitForCompletion: WAIT_TIME];
}

-(void)testHandleAnnouncedFiles
{
    NSArray *availableRemoteFiles = [self.mockPermissionManager getAvailableRemoteFiles];
    STAssertTrue([availableRemoteFiles count] == 0, @"available remote files is empty");
    
    [self.announcementManager handleAnnouncedFiles: [self generateDummyDescriptorArrayUsingLocalBusID: @"foo"] fromPeer: @"foo"];
    
    [self waitForCompletion: WAIT_TIME];
    
    availableRemoteFiles = [self.mockPermissionManager getAvailableRemoteFiles];
    STAssertTrue([availableRemoteFiles count] == 6, @"available remote files has 6 descriptors");
    
    [self.announcementManager handleAnnouncedFiles: [self generateDummyDescriptorArrayUsingLocalBusID: @"bar"] fromPeer: @"bar"];
    
    [self waitForCompletion: WAIT_TIME];
    
    availableRemoteFiles = [self.mockPermissionManager getAvailableRemoteFiles];
    STAssertTrue([availableRemoteFiles count] == 12, @"available remote files has 12 descriptors");
}

-(void)testHandleAnnouncementRequest
{
    [self testAnnounce];
    
    [self.announcementManager handleAnnouncementRequestFrom: self.peer];
    
    [self waitForCompletion: WAIT_TIME];
}

-(void)testSetFileAnnouncementReceivedDelegate
{
    STAssertTrue(YES, @"No test needed");
}

-(void)testSetFileAnnouncementSentDelegate
{
    STAssertTrue(YES, @"No test needed");
}

-(void)testShowSharedPath
{
    BOOL sharedPathSetting = [self.announcementManager showSharedPath];
    STAssertFalse(sharedPathSetting, @"Show shared path is false");
    
    [self.announcementManager setShowSharedPath: YES];
    sharedPathSetting = [self.announcementManager showSharedPath];
    STAssertTrue(sharedPathSetting, @"Show shared path is true");
    
    [self.announcementManager setShowSharedPath: NO];
    sharedPathSetting = [self.announcementManager showSharedPath];
    STAssertFalse(sharedPathSetting, @"Show shared path is false");
}

-(void)testShowRelativePath
{
    BOOL relativePathSetting = [self.announcementManager showRelativePath];
    STAssertTrue(relativePathSetting, @"Show relative path is true");
    
    [self.announcementManager setShowRelativePath: NO];
    relativePathSetting = [self.announcementManager showRelativePath];
    STAssertFalse(relativePathSetting, @"Show relative path is false");
    
    [self.announcementManager setShowRelativePath: YES];
    relativePathSetting = [self.announcementManager showRelativePath];
    STAssertTrue(relativePathSetting, @"Show relative path is true");
}

//Test Protocol Function implementations
-(void)dataChunkSent: (FTMDataChunkAction *)dataChunkAction
{
    //Unused in this class
}

-(void)fileIDRequestSent: (FTCFileIDResponseAction *)fileIDResponseAction
{
    //Unused in this class
}

-(void)sendAnnouncementWithFileList: (NSArray *)fileList toPeer: (NSString *)peer andIsFileIDResponse: (BOOL)isFileIDResponse
{
    NSLog(@"sendAnnouncement callback");
    NSLog(@"Expected size: %i", self.announcedFilesArraySize);
    NSLog(@"Actual size: %i", [fileList count]);
    STAssertTrue([fileList count] == self.announcedFilesArraySize, @"File List sizes match");
    STAssertFalse(isFileIDResponse, @"isFileIDResponse is false");
    
    if (peer != nil)
    {
        STAssertTrue([peer isEqualToString: self.peer], @"peer is not nil and equal to self.peer");
    }
    else
    {
        STAssertNil(peer, @"peer is nil");
    }
}

-(void)sendRequestDataUsingFileID: (NSData *)fileID startByte: (int)startByte length: (int)length andMaxChunkSize: (int)maxChunkSize toPeer: (NSString *)peer
{
    //Unused in this class
}

-(void)sendDataChunkUsingFileID: (NSData *)fileID startByte: (int)startByte chunkLength: (int)chunkLength andFileData: (NSData *)chunk toPeer: (NSString *)peer
{
    //Unused in this class
}

-(void)sendOfferFileWithFileDescriptor: (FTMFileDescriptor *)fd toPeer: (NSString *)peer
{
    //Unused in this class
}

-(void)sendAnnouncementRequestToPeer: (NSString *)peer
{
    STAssertTrue([peer isEqualToString: self.peer], @"peers are equal");
}

-(void)sendStopDataXferForFileID: (NSData *)fileID toPeer: (NSString *)peer
{
    //Unused in this class
}

-(void)sendXferCancelledForFileID: (NSData *)fileID toPeer: (NSString *)peer
{
    //Unused in this class
}

-(void)sendRequestOfferForFileWithPath: (NSString *)filePath toPeer: (NSString *)peer
{
    //Unused in this class
}

//FTCFileAnnouncementReceivedDelegate
-(void)receivedAnnouncementForFiles: (NSArray *)fileList andIsFileIDResponse: (BOOL)isFileIDResponse
{
    STAssertFalse(isFileIDResponse, @"isFileIDResponse is false");
    STAssertTrue([fileList count] == 6, @"received 6 files in the announcement");
}

//FTCFileAnnouncementSentDelegate
-(void)announcementSentWithFailedPaths: (NSArray *)failedPaths
{
    STAssertTrue([failedPaths count] == self.failedPathsArraySize, @"Failed paths array size match");
}

//Helper Methods
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

-(NSArray *)generatePathsToUnannounce
{
    NSMutableArray *unannouncedPaths = [[NSMutableArray alloc] init];
    
    [unannouncedPaths addObject: @"/sdcard/photos/house.png"];
    [unannouncedPaths addObject: @"/sdcard/photos/backyard.png"];
    [unannouncedPaths addObject: @"/sdcard/reports/inventors.txt"];
    [unannouncedPaths addObject: @"/sdcard/reports/invalid.txt"];
    
    return [[NSArray alloc] initWithArray: unannouncedPaths];
}

-(NSArray *)generateDummyDescriptorArrayUsingLocalBusID: (NSString *)localBusID
{
    NSMutableArray * fileList = [[NSMutableArray alloc] init];
    
    const unsigned char chunkData1[] = { 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10 };
    NSData *fileID = [[NSData alloc] initWithBytes: chunkData1 length: 20];
    FTMFileDescriptor *descriptor = [[FTMFileDescriptor alloc] init];
    descriptor.owner = localBusID;
    descriptor.fileID = fileID;
    descriptor.sharedPath = @"/sdcard/pics";
    descriptor.relativePath = @"";
    descriptor.filename = @"test1.png";
    descriptor.size = 1024;
    [fileList addObject: descriptor];
    
    const unsigned char chunkData2[] = { 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11 };
    fileID = [[NSData alloc] initWithBytes: chunkData2 length: 20];
    descriptor = [[FTMFileDescriptor alloc] init];
    descriptor.owner = localBusID;
    descriptor.fileID = fileID;
    descriptor.sharedPath = @"/sdcard/pics";
    descriptor.relativePath = @"";
    descriptor.filename = @"test2.png";
    descriptor.size = 1024;
    [fileList addObject: descriptor];
    
    const unsigned char chunkData3[] = { 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12 };
    fileID = [[NSData alloc] initWithBytes: chunkData3 length: 20];
    descriptor = [[FTMFileDescriptor alloc] init];
    descriptor.owner = localBusID;
    descriptor.fileID = fileID;
    descriptor.sharedPath = @"/sdcard/pics";
    descriptor.relativePath = @"";
    descriptor.filename = @"test3.png";
    descriptor.size = 1024;
    [fileList addObject: descriptor];
    
    const unsigned char chunkData4[] = { 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13, 0x13 };
    fileID = [[NSData alloc] initWithBytes: chunkData4 length: 20];
    descriptor = [[FTMFileDescriptor alloc] init];
    descriptor.owner = localBusID;
    descriptor.fileID = fileID;
    descriptor.sharedPath = @"/sdcard/docs";
    descriptor.relativePath = @"";
    descriptor.filename = @"test4.txt";
    descriptor.size = 1024;
    [fileList addObject: descriptor];
    
    const unsigned char chunkData5[] = { 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14, 0x14 };
    fileID = [[NSData alloc] initWithBytes: chunkData5 length: 20];
    descriptor = [[FTMFileDescriptor alloc] init];
    descriptor.owner = localBusID;
    descriptor.fileID = fileID;
    descriptor.sharedPath = @"/sdcard/docs";
    descriptor.relativePath = @"";
    descriptor.filename = @"test5.txt";
    descriptor.size = 1024;
    [fileList addObject: descriptor];
    
    const unsigned char chunkData6[] = { 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15, 0x15 };
    fileID = [[NSData alloc] initWithBytes: chunkData6 length: 20];
    descriptor = [[FTMFileDescriptor alloc] init];
    descriptor.owner = localBusID;
    descriptor.fileID = fileID;
    descriptor.sharedPath = @"/sdcard/docs";
    descriptor.relativePath = @"";
    descriptor.filename = @"test6.txt";
    descriptor.size = 1024;
    [fileList addObject: descriptor];
    
    return [[NSArray alloc] initWithArray: fileList];
}

@end

