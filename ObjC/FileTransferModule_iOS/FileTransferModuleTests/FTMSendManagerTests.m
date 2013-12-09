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

#import "FTMSendManagerTests.h"
#import "FTMSendManager.h"
#import "FTMMockDispatcher.h"
#import "FTMMockFileSystemAbstraction.h"
#import "FTMMockPermissionsManager.h"

@interface FTMSendManagerTests()

@property (nonatomic, strong) FTMMockDispatcher *ftcDispatcher;
@property (nonatomic, strong) FTMMockFileSystemAbstraction *ftcFileSystemAbstraction;
@property (nonatomic, strong) FTMMockPermissionsManager *ftcPermissionManager;
@property (nonatomic, strong) FTMSendManager *ftcSendManager;
@property (nonatomic, strong) NSData *fileID;
@property (nonatomic, strong) FTMFileDescriptor *fileDescriptor;
@property (nonatomic, strong) NSString *peer;
@property (nonatomic) int startByte;
@property (nonatomic) int length;
@property (nonatomic) int maxChunkLength;
@property (nonatomic) FTMStatusCode status;
@property (nonatomic) BOOL requestReceivedDelegateCalled;

@end


@implementation FTMSendManagerTests

@synthesize ftcDispatcher = _ftcDispatcher;
@synthesize ftcFileSystemAbstraction = _ftcFileSystemAbstraction;
@synthesize ftcPermissionManager = _ftcPermissionManager;
@synthesize ftcSendManager = _ftcSendManager;
@synthesize fileID = _fileID;
@synthesize fileDescriptor = _fileDescriptor;
@synthesize startByte = _startByte;
@synthesize length = _length;
@synthesize maxChunkLength = _maxChunkLength;
@synthesize peer = _peer;
@synthesize status = _status;
@synthesize requestReceivedDelegateCalled = _requestReceivedDelegateCalled;



- (void)setUp
{
    [super setUp];
    
    // Set-up code here.
    
    // now initialize Dispatcher, FSA, etc
    self.ftcPermissionManager = [[FTMMockPermissionsManager alloc] init];
    self.ftcFileSystemAbstraction = [[FTMMockFileSystemAbstraction alloc] init];
    self.ftcDispatcher = [[FTMMockDispatcher alloc] init];
    
    self.ftcSendManager = [[FTMSendManager alloc]
                           initWithDispatcher: self.ftcDispatcher
                           fileSystemAbstraction: self.ftcFileSystemAbstraction
                           andPermnissionManager: self.ftcPermissionManager];
    
    self.fileDescriptor = self.ftcPermissionManager.getUnitTestDummyFileDescriptor;
    self.fileID = self.fileDescriptor.fileID;
    self.peer = self.fileDescriptor.owner;
    
    self.requestReceivedDelegateCalled = NO;
    
    self.startByte = 0;
    self.length    = 100;
    self.maxChunkLength = 1000;
    
    [self.ftcPermissionManager addOfferedLocalFileDescriptor:self.fileDescriptor];
}

- (void)tearDown
{
    // Tear-down code here.
    
    [super tearDown];
}

-(void)testSendFileWithID
{
    // verify can send with length <= maxLength
    self.startByte = 0;
    self.length    = 100;
    self.maxChunkLength = 1000;
    
    self.status =
    [self.ftcSendManager sendFileWithID: self.fileID withStartByte: self.startByte andLength: self.length andMaxChunkLength: self.maxChunkLength toPeer: self.peer];
    STAssertTrue(self.status == FTMOK, @"sendFileWithID returned ");

    // verify can send with length > maxLength
    self.length = self.maxChunkLength * 2;
    self.status =
    [self.ftcSendManager sendFileWithID: self.fileID withStartByte: self.startByte andLength: self.length andMaxChunkLength: self.maxChunkLength toPeer: self.peer];
    STAssertTrue(self.status == FTMOK, @"sendFileWithID returned ");
}

-(void)testCancelFileWithID
{    
    // verify can cancel a (large) file
    self.length = self.maxChunkLength * 200;
    self.status =
    [self.ftcSendManager sendFileWithID: self.fileID withStartByte: self.startByte andLength: self.length andMaxChunkLength: self.maxChunkLength toPeer: self.peer];
    STAssertTrue(self.status == FTMOK, @"sendFileWithID returned ");
    
    self.status = [self.ftcSendManager cancelFileWithID:self.fileID];
    STAssertTrue(self.status == FTMOK, @"cancelFileWithID returned ");
    
    // verify cancel called when nothing being transferred
    self.status = [self.ftcSendManager cancelFileWithID:self.fileID];
    STAssertTrue(self.status == FTMFileNotBeingTransferred, @"cancelFileWithID returned ");
}

-(void)testHandleRequestForFileWithID
{
    self.status =
    [self.ftcSendManager handleRequestForFileWithID:self.fileID withStartByte:self.startByte length:self.length fromPeer:self.peer andMaxChunkLength:self.maxChunkLength];
    
    STAssertTrue(self.status == FTMOK, @"handleRequestForFileWithID returned ");
}

-(void)testDataSent
{
    self.startByte = 0;
    self.maxChunkLength = 100;
    self.length = self.maxChunkLength * 3;
    
    self.status =
    [self.ftcSendManager sendFileWithID: self.fileID withStartByte: self.startByte andLength: self.length andMaxChunkLength: self.maxChunkLength toPeer: self.peer];
    STAssertTrue(self.status == FTMOK, @"sendFileWithID returned ");
    
    NSArray *progressList = [self.ftcSendManager getProgressList];
    STAssertTrue ((progressList.count == 1), @"getProgressList returned ");
    
    // verify that 1st chunk has gone out
    FTMProgressDescriptor *descriptor = progressList[0];
    STAssertTrue ((descriptor != nil) &&
                  ([descriptor.fileID isEqualToData: self.fileID]) &&
                  (descriptor.fileSize == self.length) &&
                  (descriptor.bytesTransferred == self.maxChunkLength) &&
                  (descriptor.state == IN_PROGRESS), @"getProgressList returned ");
    
    // cause 2nd chunk to be sent
    [self.ftcSendManager dataSent];
    progressList = [self.ftcSendManager getProgressList];
    STAssertTrue ((progressList.count == 1), @"getProgressList returned ");
    
    // verify that 2nd chunk has gone out
    descriptor = progressList[0];
    STAssertTrue ((descriptor != nil) &&
                  ([descriptor.fileID isEqualToData: self.fileID]) &&
                  (descriptor.fileSize == self.length) &&
                  (descriptor.bytesTransferred ==  2 * self.maxChunkLength) &&
                  (descriptor.state == IN_PROGRESS), @"getProgressList returned ");
    
    // cause 3rd (and last) chunk to be sent, at which point progress list returns empty
    [self.ftcSendManager dataSent];
    progressList = [self.ftcSendManager getProgressList];
    STAssertTrue ((progressList.count == 0), @"getProgressList returned ");
}

-(void)testGetProgressList
{
    self.startByte = 0;
    self.length    = 1000;
    self.maxChunkLength = 100;
    
    self.status =
    [self.ftcSendManager sendFileWithID: self.fileID withStartByte: self.startByte andLength: self.length andMaxChunkLength: self.maxChunkLength toPeer: self.peer];
    STAssertTrue(self.status == FTMOK, @"sendFileWithID returned ");
    
    NSArray *progressList = [self.ftcSendManager getProgressList];
    STAssertTrue ((progressList.count == 1), @"getProgressList returned ");
    
    FTMProgressDescriptor *descriptor = progressList[0];
    STAssertTrue (
            (descriptor != nil) &&
            ([descriptor.fileID isEqualToData: self.fileID]) &&
            (descriptor.fileSize == self.length) &&
            (descriptor.bytesTransferred == self.maxChunkLength) &&
            (descriptor.state == IN_PROGRESS), @"getProgressList returned ");
}

-(void)testHandleStopDataXferForFileWithID
{
    self.status =
    [self.ftcSendManager sendFileWithID: self.fileID withStartByte: self.startByte andLength: self.length andMaxChunkLength: self.maxChunkLength toPeer: self.peer];
    STAssertTrue(self.status == FTMOK, @"sendFileWithID returned ");
    
    [self.ftcSendManager handleStopDataXferForFileWithID:self.fileID fromPeer:self.peer];

    NSArray *progressList = [self.ftcSendManager getProgressList];
    
    STAssertTrue(progressList.count == 0, @"handleStopDataXferForFileWithID returned ");
}
 
-(void)testResetState
{
    self.status =
    [self.ftcSendManager sendFileWithID: self.fileID withStartByte: self.startByte andLength: self.length andMaxChunkLength: self.maxChunkLength toPeer: self.peer];
    STAssertTrue(self.status == FTMOK, @"sendFileWithID returned ");
    
    [self.ftcSendManager resetState];
    
    NSArray *progressList = [self.ftcSendManager getProgressList];
    
    STAssertTrue(progressList.count == 0, @"resetState returned ");    
}

-(void)fileRequestReceived: (NSString *) filename
{
    self.requestReceivedDelegateCalled = YES;
    NSLog(@"requestDataReceivedDelegate called");
}


-(void)waitForCompletion: (NSTimeInterval)timeoutSeconds
{
    NSDate *timeoutDate = [NSDate dateWithTimeIntervalSinceNow: timeoutSeconds];
    
    do {
        [[NSRunLoop currentRunLoop] runMode: NSDefaultRunLoopMode beforeDate: timeoutDate];
        if ([timeoutDate timeIntervalSinceNow] < 0.0) {
            break;
        }
    } while (TRUE);
}

-(void)testSetRequestDataReceivedDelegate
{
    self.status =
    [self.ftcSendManager sendFileWithID: self.fileID withStartByte: self.startByte andLength: self.length andMaxChunkLength: self.maxChunkLength toPeer: self.peer];
    STAssertTrue(self.status == FTMOK, @"sendFileWithID returned ");
    
    [self waitForCompletion: 1];  //Don't know if this delay is strictly necessary...
    // first send something WITHOUT a delegate registered...
    STAssertTrue(self.requestReceivedDelegateCalled == NO, @"setRequestDataReceivedDelegate ");
    
    // now register a delegate and send something
    self.ftcSendManager.requestDataReceivedDelegate = self;
    
    self.status =
    [self.ftcSendManager sendFileWithID: self.fileID withStartByte: self.startByte andLength: self.length andMaxChunkLength: self.maxChunkLength toPeer: self.peer];
    STAssertTrue(self.status == FTMOK, @"sendFileWithID returned ");
    
    [self waitForCompletion: 1];  //Don't know if this delay is strictly necessary...
    
    STAssertTrue(self.requestReceivedDelegateCalled == YES, @"setRequestDataReceivedDelegate ");
}

@end

