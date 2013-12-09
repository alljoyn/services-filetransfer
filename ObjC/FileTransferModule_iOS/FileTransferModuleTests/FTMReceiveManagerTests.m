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

#import "FTMReceiveManagerTests.h"
#import "FTMReceiveManager.h"
#import "FTMMockDispatcher.h"
#import "FTMMockFileSystemAbstraction.h"
#import "FTMMockPermissionsManager.h"


@interface FTMReceiveManagerTests()

@property (nonatomic, strong) FTMMockDispatcher *ftcDispatcher;
@property (nonatomic, strong) FTMMockFileSystemAbstraction *ftcFileSystemAbstraction;
@property (nonatomic, strong) FTMMockPermissionsManager *ftcPermissionManager;
@property (nonatomic, strong) FTMReceiveManager *ftcReceiveManager;
@property (nonatomic, strong) NSData *fileID;
@property (nonatomic, strong) FTMFileDescriptor *fileDescriptor;
@property (nonatomic, strong) NSString *peer;
@property (nonatomic) FTMStatusCode status;
@property (nonatomic) BOOL requestReceivedDelegateCalled;

@property (nonatomic, strong) NSString *saveFileName;
@property (nonatomic, strong) NSString *saveFileDirectory;
@property (nonatomic, strong) NSData   *chunk;

@end

@implementation FTMReceiveManagerTests


@synthesize ftcDispatcher = _ftcDispatcher;
@synthesize ftcFileSystemAbstraction = _ftcFileSystemAbstraction;
@synthesize ftcPermissionManager = _ftcPermissionManager;
@synthesize ftcReceiveManager = _ftcReceiveManager;
@synthesize fileID = _fileID;
@synthesize fileDescriptor = _fileDescriptor;
@synthesize peer = _peer;
@synthesize status = _status;
@synthesize requestReceivedDelegateCalled = _requestReceivedDelegateCalled;

@synthesize saveFileName = _saveFileName;
@synthesize saveFileDirectory = _saveFileDirectory;
@synthesize chunk = _chunk;


- (void)setUp
{
    [super setUp];
    
    // now initialize Dispatcher, FSA, etc
    self.ftcPermissionManager = [[FTMMockPermissionsManager alloc] init];
    self.ftcFileSystemAbstraction = [[FTMMockFileSystemAbstraction alloc] init];
    self.ftcDispatcher = [[FTMMockDispatcher alloc] init];
    
    self.ftcReceiveManager = [[FTMReceiveManager alloc]
                    initWithDispatcher:self.ftcDispatcher
                    fileSystemAbstraction:self.ftcFileSystemAbstraction
                    andPermissionManager:self.ftcPermissionManager];
    
    self.requestReceivedDelegateCalled = NO;
    
    self.fileDescriptor = self.ftcPermissionManager.getUnitTestDummyFileDescriptor;
    self.fileID = self.fileDescriptor.fileID;
    self.peer = self.fileDescriptor.owner;
    self.saveFileName = self.fileDescriptor.filename;
    self.saveFileDirectory = self.fileDescriptor.sharedPath;
}

- (void)tearDown
{
    // Tear-down code here.
    
    [super tearDown];
}

- (void)testRequestFileOwnedBy
{
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    self.status = [self.ftcReceiveManager requestFileOwnedBy:self.peer withFileID:self.fileID saveFileName:self.saveFileName andSaveFileDirectory:self.saveFileDirectory];
    
    STAssertTrue(self.status == FTMOK, @"requestFileOwnedBy ");
}

- (void)testGetProgressList
{
    // validate empty list
    NSArray *progressList = [self.ftcReceiveManager getProgressList];
    STAssertTrue ((progressList.count == 0), @"getProgressList");
    
    // validate non-empty list
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    self.status = [self.ftcReceiveManager requestFileOwnedBy:self.peer withFileID:self.fileID saveFileName:self.saveFileName andSaveFileDirectory:self.saveFileDirectory];
    
    STAssertTrue(self.status == FTMOK, @"requestFileOwnedBy");
    
    progressList = [self.ftcReceiveManager getProgressList];
    STAssertTrue ((progressList.count == 1), @"getProgressList");   
}

- (void)testPauseFileWithID
{
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    
    self.status = [self.ftcReceiveManager requestFileOwnedBy:self.peer withFileID:self.fileID saveFileName:self.saveFileName andSaveFileDirectory:self.saveFileDirectory];
    
    STAssertTrue(self.status == FTMOK, @"requestFileOwnedBy");

    self.status = [self.ftcReceiveManager pauseFileWithID:self.fileID];
    STAssertTrue(self.status == FTMOK, @"pauseFileWithID");
    NSArray *progressList = [self.ftcReceiveManager getProgressList];
    STAssertTrue ((progressList.count == 0), @"getProgressList");
}

- (void)testCancelFileWithID
{
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    self.status = [self.ftcReceiveManager requestFileOwnedBy:self.peer withFileID:self.fileID saveFileName:self.saveFileName andSaveFileDirectory:self.saveFileDirectory];
    STAssertTrue(self.status == FTMOK, @"requestFileOwnedBy");
    
    self.status = [self.ftcReceiveManager cancelFileWithID:self.fileID];
    STAssertTrue(self.status == FTMOK, @"cancelFileWithID ");
    NSArray *progressList = [self.ftcReceiveManager getProgressList];
    STAssertTrue ((progressList.count == 0), @"getProgressList");
    
    self.status = [self.ftcReceiveManager cancelFileWithID:self.fileID];
    STAssertTrue(self.status == FTMBadFileID, @"cancelFileWithID");
}

-(void)testInitiateRequestForFile
{
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    
    self.status = [self.ftcReceiveManager initiateRequestForFile:self.fileDescriptor usingSaveFileName:self.saveFileName andSaveDirectory:self.saveFileDirectory throughDispatcher:YES];
    STAssertTrue(self.status == FTMOK, @"initiateRequestForFile");
    
    //now use a corrupt saveDirectory and verify
    [self.ftcReceiveManager resetState];
    self.status = [self.ftcReceiveManager initiateRequestForFile:self.fileDescriptor usingSaveFileName:self.saveFileName andSaveDirectory:@"09/\0r8098(*&(*&" throughDispatcher:YES];
    STAssertTrue(self.status == FTMBadFilePath, @"initiateRequestForFile");
}

-(void)testHandleChunkForFile
{
    char theChunk[] = "now is the time for all good men to come to the aid of their country";
    NSData *chunkData = [[NSData alloc] initWithBytes:theChunk length:sizeof(theChunk)];
    
    // validate handling a nil file descriptor
    [self.ftcReceiveManager handleChunkForFile:nil withStartByte:0 andLength:sizeof(theChunk) andFileData:chunkData];
    
    
    // validate arbitrary chunk received when no request has been made
    [self.ftcReceiveManager resetState];
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    [self.ftcReceiveManager handleChunkForFile:self.fileDescriptor.fileID withStartByte:0 andLength:sizeof(theChunk) andFileData:chunkData];
    
    
    // validate handling a chunk corresponding to the request
    [self.ftcReceiveManager resetState];
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    self.status = [self.ftcReceiveManager initiateRequestForFile:self.fileDescriptor usingSaveFileName:self.saveFileName andSaveDirectory:self.saveFileDirectory throughDispatcher:YES];
    STAssertTrue(self.status == FTMOK, @"initiateRequestForFile");
    [self.ftcReceiveManager handleChunkForFile:self.fileDescriptor.fileID withStartByte:0 andLength:sizeof(theChunk) andFileData:chunkData];
    
    
    // handle a subsequent chunk
    [self.ftcReceiveManager handleChunkForFile:self.fileDescriptor.fileID withStartByte:sizeof(theChunk) andLength:sizeof(theChunk) andFileData:chunkData];
    
    
    // handle an out of order chunk
    [self.ftcReceiveManager handleChunkForFile:self.fileDescriptor.fileID withStartByte:0 andLength:sizeof(theChunk) andFileData:chunkData];

    
    // validate handling entire file
    [self.ftcReceiveManager resetState];
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    self.status = [self.ftcReceiveManager initiateRequestForFile:self.fileDescriptor usingSaveFileName:self.saveFileName andSaveDirectory:self.saveFileDirectory throughDispatcher:YES];
    STAssertTrue(self.status == FTMOK, @"initiateRequestForFile");
    int thisChunkLength = sizeof(theChunk);
    int remainder = self.fileDescriptor.size % sizeof(theChunk);
    NSArray *progressList;
    
    for (int i=0; i<self.fileDescriptor.size; i+=thisChunkLength)
    {
        // verify progress
        progressList = [self.ftcReceiveManager getProgressList];
        STAssertTrue ((progressList.count == 1), @"getProgressList");

        if ((remainder != 0) && i+sizeof(theChunk)>self.fileDescriptor.size)
        {
            thisChunkLength = remainder;
        }
        [self.ftcReceiveManager handleChunkForFile:self.fileDescriptor.fileID withStartByte:i andLength:thisChunkLength andFileData:chunkData];
    }
    progressList = [self.ftcReceiveManager getProgressList];
    STAssertTrue ((progressList.count == 0), @"getProgressList");

    
    // validate receiving too large a chunk
    [self.ftcReceiveManager resetState];
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    self.status = [self.ftcReceiveManager initiateRequestForFile:self.fileDescriptor usingSaveFileName:self.saveFileName andSaveDirectory:self.saveFileDirectory throughDispatcher:YES];
    STAssertTrue(self.status == FTMOK, @"initiateRequestForFile");
    [self.ftcReceiveManager handleChunkForFile:self.fileDescriptor.fileID withStartByte:0 andLength: [self.ftcReceiveManager maxChunkSize]+1 andFileData:chunkData];
    progressList = [self.ftcReceiveManager getProgressList];
    STAssertTrue ((progressList.count == 0), @"getProgressList");
    
    
    // validate handling more bytes than expected in file. The last chunck sent (below) will exceed the expected filesize.
    [self.ftcReceiveManager resetState];
    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    self.status = [self.ftcReceiveManager initiateRequestForFile:self.fileDescriptor usingSaveFileName:self.saveFileName andSaveDirectory:self.saveFileDirectory throughDispatcher:YES];
    STAssertTrue(self.status == FTMOK, @"initiateRequestForFile");
    thisChunkLength = sizeof(theChunk);
    for (int i=0; i<self.fileDescriptor.size; i+=thisChunkLength)
    {
        // verify progress
        progressList = [self.ftcReceiveManager getProgressList];
        STAssertTrue ((progressList.count == 1), @"getProgressList");

        [self.ftcReceiveManager handleChunkForFile:self.fileDescriptor.fileID withStartByte:i andLength:thisChunkLength andFileData:chunkData];
    }
    progressList = [self.ftcReceiveManager getProgressList];
    STAssertTrue ((progressList.count == 0), @"getProgressList");
}

-(void)testHandleDataXferCancelledFrom
{
    char theChunk[] = "now is the time for all good men to come to the aid of their country";
    NSData *chunkData = [[NSData alloc] initWithBytes:theChunk length:sizeof(theChunk)];

    [self.ftcPermissionManager addOfferedRemoteFileDescriptor:self.fileDescriptor fromPeer:self.peer];
    // Make a request
    self.status = [self.ftcReceiveManager initiateRequestForFile:self.fileDescriptor usingSaveFileName:self.saveFileName andSaveDirectory:self.saveFileDirectory throughDispatcher:YES];
    STAssertTrue(self.status == FTMOK, @"initiateRequestForFile");
    [self.ftcReceiveManager handleChunkForFile:self.fileDescriptor.fileID withStartByte:0 andLength:sizeof(theChunk) andFileData:chunkData];
    
    // now handle the cancelled message
    [self.ftcReceiveManager handleDataXferCancelledFrom:self.peer forFileWithID:self.fileID];
    NSArray *progressList = [self.ftcReceiveManager getProgressList];
    STAssertTrue ((progressList.count == 0), @"getProgressList");
}

@end
