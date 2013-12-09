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

#import "FTMDispatcherTests.h"

const NSTimeInterval WAIT_INTERVAL = 3.0f;

@interface FTMDispatcherTests()

@property (nonatomic, strong) FTMDispatcher *dispatcher;
@property (nonatomic, strong) FTMMockDirectedAnnouncementManager *mockDAM;
@property (nonatomic, strong) FTMMockSendManager *mockSendManager;
@property (nonatomic, strong) FTMMockTransmitter *mockTransmitter;
@property (nonatomic, strong) NSArray *fileList;
@property (nonatomic, strong) NSString *peer;
@property (nonatomic, strong) NSData *fileID;
@property (nonatomic) BOOL isFileIDResponse;
@property (nonatomic, strong) FTMFileDescriptor *fd;
@property (nonatomic) int startByte;
@property (nonatomic) int length;
@property (nonatomic) int maxChunkSize;
@property (nonatomic, strong) NSData *chunk;
@property (nonatomic, strong) NSString *filePath;

@end

@implementation FTMDispatcherTests

@synthesize dispatcher = _dispatcher;
@synthesize mockDAM = _mockDAM;
@synthesize mockSendManager = _mockSendManager;
@synthesize mockTransmitter = _mockTransmitter;
@synthesize fileList = _fileList;
@synthesize peer = _peer;
@synthesize fileID = _fileID;
@synthesize fd = _fd;
@synthesize startByte = _startByte;
@synthesize length = _length;
@synthesize maxChunkSize = _maxChunkSize;
@synthesize chunk = _chunk;
@synthesize filePath = _filePath;

- (void)setUp
{
    [super setUp];
    
    self.mockTransmitter = [[FTMMockTransmitter alloc] initWithBusObject: nil busAttachment: nil andSessionID: 0];
    [self.mockTransmitter setDelegate: self];
    
    self.dispatcher = [[FTMDispatcher alloc] initWithTransmitter: self.mockTransmitter];
    
    self.mockDAM = [[FTMMockDirectedAnnouncementManager alloc] initWithDispatcher: self.dispatcher permissionsManager: nil fileSystemAbstraction: nil andLocalBusID: nil];
    self.mockSendManager = [[FTMMockSendManager alloc] initWithDispatcher: self.dispatcher fileSystemAbstraction: nil andPermnissionManager: nil];
    [self.mockDAM setDelegate: self];
    [self.mockSendManager setDelegate: self];
    
    [self.dispatcher setDirectedAnnouncementManagerDelegate: self.mockDAM];
    [self.dispatcher setSendManagerDelegate: self.mockSendManager];
    
    
    const unsigned char bytes[] = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
    self.fileID = [[NSData alloc] initWithBytes: bytes length: 20];
    self.fd = [[FTMFileDescriptor alloc] init];
    self.fd.owner = @"James";
    self.fd.sharedPath = @"/Documents";
    self.fd.relativePath = @"";
    self.fd.filename = @"test.txt";
    self.fd.size = 1024;
    self.fd.fileID = self.fileID;
    self.fileList = [[NSArray alloc] initWithObjects: self.fd, nil];
    self.peer = @"James";
    self.isFileIDResponse = YES;
    self.startByte = 0;
    self.length = 100000;
    self.maxChunkSize = 1024;
    
    const unsigned char chunkData[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x012, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20 };
    self.chunk = [[NSData alloc] initWithBytes: chunkData length: 20];
    self.filePath = @"/Documents/testFolder/text.txt";
}

- (void)tearDown
{
    self.dispatcher = nil;
    self.mockDAM = nil;
    self.mockSendManager = nil;
    self.mockTransmitter = nil;
    
    [super tearDown];
}

-(void)testInsertAction
{
    FTMAnnounceAction *announceAction = [[FTMAnnounceAction alloc] init];
    announceAction.peer = self.peer;
    announceAction.fileList = self.fileList;
    announceAction.isFileIDResponse = self.isFileIDResponse;
    
    [self.dispatcher insertAction: announceAction];
    
    [self waitForCompletion: WAIT_INTERVAL];
    
    FTMRequestDataAction *requestDataAction = [[FTMRequestDataAction alloc] init];
    requestDataAction.peer = self.peer;
    requestDataAction.fileID = self.fileID;
    requestDataAction.startByte = self.startByte;
    requestDataAction.length = self.length;
    requestDataAction.maxChunkSize = self.maxChunkSize;
    
    [self.dispatcher insertAction: requestDataAction];
    
    [self waitForCompletion: WAIT_INTERVAL];
    
    FTMDataChunkAction *dataChunkAction = [[FTMDataChunkAction alloc] init];
    dataChunkAction.peer = self.peer;
    dataChunkAction.fileID = self.fileID;
    dataChunkAction.startByte = self.startByte;
    dataChunkAction.chunkLength = self.maxChunkSize;
    dataChunkAction.chunk = self.chunk;
    
    [self.dispatcher insertAction: dataChunkAction];
    
    [self waitForCompletion: WAIT_INTERVAL];
    
    FTMOfferFileAction *offerFileAction = [[FTMOfferFileAction alloc] init];
    offerFileAction.peer = self.peer;
    offerFileAction.fd = self.fd;
    
    [self.dispatcher insertAction: offerFileAction];
    
    [self waitForCompletion: WAIT_INTERVAL];
    
    FTMRequestAnnouncementAction *requestAnnouncementAction = [[FTMRequestAnnouncementAction alloc] init];
    requestAnnouncementAction.peer = self.peer;
    
    [self.dispatcher insertAction: requestAnnouncementAction];
    
    [self waitForCompletion: WAIT_INTERVAL];
    
    FTMStopXferAction *stopXferAction = [[FTMStopXferAction alloc] init];
    stopXferAction.peer = self.peer;
    stopXferAction.fileID = self.fileID;
    
    [self.dispatcher insertAction: stopXferAction];
    
    [self waitForCompletion: WAIT_INTERVAL];
    
    FTMXferCancelledAction *xferCancelledAction = [[FTMXferCancelledAction alloc] init];
    xferCancelledAction.peer = self.peer;
    xferCancelledAction.fileID = self.fileID;
    
    [self.dispatcher insertAction: xferCancelledAction];
    
    [self waitForCompletion: WAIT_INTERVAL];
    
    FTMRequestOfferAction *requestOfferAction = [[FTMRequestOfferAction alloc] init];
    requestOfferAction.peer = self.peer;
    requestOfferAction.filePath = self.filePath;
    
    [self.dispatcher insertAction: requestOfferAction];
    
    [self waitForCompletion: WAIT_INTERVAL];
    
    FTMFileIDResponseAction *fileIDResponseAction = [[FTMFileIDResponseAction alloc] init];
    fileIDResponseAction.peer = self.peer;
    fileIDResponseAction.filePath = self.filePath;
    
    [self.dispatcher insertAction: fileIDResponseAction];
    
    [self waitForCompletion: WAIT_INTERVAL];
}

-(void)dataChunkSent: (FTMDataChunkAction *)dataChunkAction
{
    STAssertTrue([dataChunkAction isMemberOfClass: [FTMDataChunkAction class]], @"");
}

-(void)fileIDRequestSent: (FTMFileIDResponseAction *)fileIDResponseAction
{
    STAssertTrue([fileIDResponseAction isMemberOfClass: [FTMFileIDResponseAction class]], @"");
    STAssertTrue([fileIDResponseAction.peer isEqualToString: self.peer], @"peers are equal");
    STAssertTrue([fileIDResponseAction.filePath isEqualToString: self.filePath], @"filepaths are equal");
}

-(void)sendAnnouncementWithFileList: (NSArray *)fileList toPeer: (NSString *)peer andIsFileIDResponse: (BOOL)isFileIDResponse
{
    STAssertTrue(([fileList count] == 1 && [fileList containsObject: self.fd]), @"File List size is 1 and contains FD");
    STAssertTrue([peer isEqualToString: self.peer], @"Peers are equal");
    STAssertTrue(isFileIDResponse, @"isFileIDResponse returned YES");
}
-(void)sendRequestDataUsingFileID: (NSData *)fileID startByte: (int)startByte length: (int)length andMaxChunkSize: (int)maxChunkSize toPeer: (NSString *)peer
{
    STAssertTrue([peer isEqualToString: self.peer], @"peers are equal");
    STAssertTrue([fileID isEqualToData: self.fileID], @"file ids are equal");
    STAssertTrue(startByte == self.startByte, @"startBytes are equal");
    STAssertTrue(length == self.length, @"lengths are equal");
    STAssertTrue(maxChunkSize == self.maxChunkSize, @"max chunk sizes are equal");
}

-(void)sendDataChunkUsingFileID: (NSData *)fileID startByte: (int)startByte chunkLength: (int)chunkLength andFileData: (NSData *)chunk toPeer: (NSString *)peer
{
    STAssertTrue([peer isEqualToString: self.peer], @"peers are equal");
    STAssertTrue([fileID isEqualToData: self.fileID], @"file ids are equal");
    STAssertTrue(startByte == self.startByte, @"startBytes are equal");
    STAssertTrue(chunkLength == self.maxChunkSize, @"lengths are equal");
    STAssertTrue([chunk isEqualToData: self.chunk], @"chunks  are equal");
}
-(void)sendOfferFileWithFileDescriptor: (FTMFileDescriptor *)fd toPeer: (NSString *)peer
{
    STAssertTrue([fd isEqual: self.fd], @"File descriptors are equal");
    STAssertTrue([peer isEqualToString: self.peer], @"Peers are equal");
}

-(void)sendAnnouncementRequestToPeer: (NSString *)peer
{
    STAssertTrue([peer isEqualToString: self.peer], @"Peers are equal");
}

-(void)sendStopDataXferForFileID: (NSData *)fileID toPeer: (NSString *)peer
{
    STAssertTrue([fileID isEqualToData: self.fileID], @"file IDs are equal");
    STAssertTrue([peer isEqualToString: self.peer], @"peers are equal");
}

-(void)sendXferCancelledForFileID: (NSData *)fileID toPeer: (NSString *)peer
{
    STAssertTrue([fileID isEqualToData: self.fileID], @"file IDs are equal");
    STAssertTrue([peer isEqualToString: self.peer], @"peers are equal");
}

-(void)sendRequestOfferForFileWithPath: (NSString *)filePath toPeer: (NSString *)peer
{
    STAssertTrue([peer isEqualToString: self.peer], @"peers are equal");
    STAssertTrue([filePath isEqualToString: self.filePath], @"filepaths are equal");
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

@end