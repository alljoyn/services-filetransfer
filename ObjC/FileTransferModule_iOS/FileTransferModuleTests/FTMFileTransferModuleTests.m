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

#import "FTMFileTransferModule.h"
#import "FTMFileDescriptor.h"
#import "AJNFileTransferBusObject.h"
#import "AllJoynFramework/AJNSessionOptions.h"
#import "AllJoynFramework/AJNBus.h"

#import "FTMFileTransferModuleTests.h"

const AJNSessionPort CONTACT_PORT = 42;
const NSTimeInterval WAIT_TIME_SECS = 5.0f;
NSString * const SERVICE_NAME = @"org.alljoyn.cops.filetransfer";

NSString *testFileName = @"/testfile.test";
NSString *testFileContents = @"test contents";

@interface FTMFileTransferModuleTests()

@property (nonatomic, strong) FTMFileTransferModule *sendingFTModule;
@property (nonatomic, strong) FTMFileTransferModule *receivingFTModule;
@property (nonatomic, strong) AJNBusAttachment *sendingBus;
@property (nonatomic, strong) AJNBusAttachment *receivingBus;
@property (nonatomic) AJNSessionId sessionId;
@property (nonatomic, strong) NSFileHandle *testFile;
@property (nonatomic, strong) NSString *testFilePath;
@property (nonatomic) BOOL messageReceived;

@end

@implementation FTMFileTransferModuleTests

@synthesize sendingFTModule = _sendingFTModule;
@synthesize receivingFTModule = _receivingFTModule;
@synthesize sendingBus =_sendingBus;
@synthesize receivingBus = _receivingBus;
@synthesize sessionId = _sessionId;
@synthesize testFile = _testFile;
@synthesize testFilePath = _testFilePath;
@synthesize messageReceived = _messageReceived;


-(void)setUp
{
    [super setUp];
    
    self.sendingBus = [[AJNBusAttachment alloc] initWithApplicationName:@"FileTransfer" allowRemoteMessages:YES];
    self.receivingBus = [[AJNBusAttachment alloc] initWithApplicationName:@"FileTransfer" allowRemoteMessages:YES];
    
    self.sendingFTModule = [[FTMFileTransferModule alloc] initWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    self.receivingFTModule = [[FTMFileTransferModule alloc] initWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    
    //self.sendingFTModule = [[FTMFileTransferModule alloc] init];
    //self.receivingFTModule = [[FTMFileTransferModule alloc] init];
                              
    // write our test file
    self.testFilePath = [NSHomeDirectory() stringByAppendingString: testFileName];
    NSFileManager *filemgr = [[NSFileManager alloc]init];
    [filemgr createFileAtPath:self.testFilePath contents:nil attributes: nil];
    NSString *text = [NSString stringWithFormat: @"%@", testFileContents];
    self.testFile = [NSFileHandle fileHandleForWritingAtPath:self.testFilePath];
    [self.testFile seekToFileOffset:0];
    [self.testFile writeData:[text dataUsingEncoding:NSUnicodeStringEncoding]];
    [self.testFile closeFile];
    
    [self createAjConnection];
    
    self.messageReceived = FALSE;
    
    // Allow each test case to initialize as it wants...
    [self.sendingFTModule uninitialize];
    [self.receivingFTModule uninitialize];
}

-(void)tearDown
{
    [[NSFileManager defaultManager] removeItemAtPath:self.testFilePath error:nil];
    
    [self.receivingBus leaveSession: self.sessionId];
    [self.receivingBus disconnectWithArguments:@"null:"];
    [self.sendingBus leaveSession: self.sessionId];
    [self.sendingBus disconnectWithArguments:@"null:"];
    self.sessionId = 0;
    
    [super tearDown];
}

-(void) createAjConnection
{
    //self.sendingBus = [[AJNBusAttachment alloc] initWithApplicationName:@"FileTransfer" allowRemoteMessages:YES];
    [self createSession: self.sendingBus];
    
    //self.receivingBus = [[AJNBusAttachment alloc] initWithApplicationName:@"FileTransfer" allowRemoteMessages:YES];
    [self joinSession: self.receivingBus];
    
    //wait to join session
    [self waitForCompletion:WAIT_TIME_SECS];
    STAssertFalse(0 == self.sessionId, @"invalid session id");
}

-(void) createSession: (AJNBusAttachment *) bus
{
    QStatus status;
    
    status = [bus start];
    status = [bus connectWithArguments:@"null:"];
    
    status = [bus requestWellKnownName: SERVICE_NAME withFlags:kAJNBusNameFlagReplaceExisting | kAJNBusNameFlagDoNotQueue];
    
    AJNSessionOptions *sessionOpts = [[AJNSessionOptions alloc] initWithTrafficType: kAJNTrafficMessages supportsMultipoint: YES proximity: kAJNProximityAny transportMask: kAJNTransportMaskAny];
    
    status = [bus bindSessionOnPort:CONTACT_PORT withOptions:sessionOpts withDelegate:self];

    status = [bus advertiseName: SERVICE_NAME withTransportMask:sessionOpts.transports];
}

-(void) joinSession: (AJNBusAttachment *)bus
{
    QStatus status;
    
    status = [bus start];
    status = [bus connectWithArguments: @"null:"];

    [bus registerBusListener: self];
    status = [bus findAdvertisedName: SERVICE_NAME];
}

//AllJoyn Listeners
- (BOOL)shouldAcceptSessionJoinerNamed: (NSString *)joiner onSessionPort: (AJNSessionPort)sessionPort withSessionOptions: (AJNSessionOptions *)options
{
    return YES;
}

- (void)didJoin: (NSString *)joiner inSessionWithId: (AJNSessionId)sessionId onSessionPort: (AJNSessionPort)sessionPort
{
    self.sessionId = sessionId;
    NSLog(@"hosting session %i", self.sessionId);
}

- (void)didFindAdvertisedName: (NSString *)name withTransportMask: (AJNTransportMask) transport namePrefix:(NSString *)namePrefix
{
    AJNSessionOptions *sessionOptions = [[AJNSessionOptions alloc] initWithTrafficType: kAJNTrafficMessages supportsMultipoint: YES proximity: kAJNProximityAny transportMask: kAJNTransportMaskAny];
    
    self.sessionId = [self.receivingBus joinSessionWithName: name onPort: CONTACT_PORT withDelegate:self options: sessionOptions];
    NSLog(@"joined session %i", self.sessionId);
}

-(void) receivedAnnouncementForFiles: (NSArray *) fileList andIsFileIDResponse: (BOOL) isFileIDResponse
{
    self.messageReceived = TRUE;
    
    STAssertTrue(1 == fileList.count, @"announcement received");
}

-(BOOL) allowUnannouncedRequestsForFileWithPath: (NSString *) filePath
{
    return YES;
}

-(BOOL)acceptFileOfferFrom: (NSString *)peer forFile: (FTMFileDescriptor *)file
{
    return YES;
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
    } while (FALSE == self.messageReceived);
}


-(FTMFileDescriptor *) generateDummyFileDescriptor
{
    FTMFileDescriptor *descriptor = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes[] = { 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9 };
    descriptor.fileID = [[NSData alloc] initWithBytes: bytes length: 20];
    descriptor.filename = @"meals.txt";
    descriptor.owner = @"bar";
    descriptor.relativePath = @"";
    descriptor.sharedPath = @"sdcard/shared";
    descriptor.size = 100;
    
    return descriptor;
}

// Unit Tests
-(void) testAnnounce
{
    //test can add to announced list without initializing
    
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];    
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
       
    STAssertTrue(1 == [self.sendingFTModule.announcedLocalFiles count], @"announcedLocalFiles");
    
    
    //test announcement received after initializing
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    [self.receivingFTModule setFileAnnouncementReceivedDelegate:self];
      
    [self waitForCompletion:WAIT_TIME_SECS]; //allow time to receive announcement
    STAssertTrue(self.messageReceived, @"received announcement");
}

-(void) testInitialize
{
    //test message received after first initialization
    [self.receivingFTModule setFileAnnouncementReceivedDelegate:self];
    
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
    STAssertTrue(self.messageReceived, @"received announcement");   
    
    //test message not received after deinitialization
    [self.sendingFTModule uninitialize];
    [self.receivingFTModule uninitialize];
    
    [self waitForCompletion: WAIT_TIME_SECS];
    self.messageReceived = FALSE;
 
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to receive announcement
    STAssertFalse(self.messageReceived, @"received announcement");

    //test message received after second initialization
    self.messageReceived = FALSE;
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to receive announcement
    STAssertTrue(self.messageReceived, @"received announcement");
}

-(void) testStopAnnounce
{
    //add an announcement, then remove it
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.sendingFTModule announceFilePaths:pathList];
        
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
    STAssertTrue(1==[self.sendingFTModule.announcedLocalFiles count], @"generate announcement");
        
    [self.sendingFTModule stopAnnounceFilePaths:pathList];
    STAssertTrue(0==[self.sendingFTModule.announcedLocalFiles count], @"generate announcement");
    
    //make sure no announcement is sent when initialized
    [self.receivingFTModule setFileAnnouncementReceivedDelegate:self];
    
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to receive announcement
    STAssertFalse(self.messageReceived, @"received announcement");			
}

-(void) testRequestFileAnnouncement
{    
    int statusCode = [self.sendingFTModule requestFileAnnouncementFromPeer:[self.receivingBus uniqueName]];
    STAssertTrue(FTMNOAjConnection == statusCode, @"requestFileAnnouncementFromPeer");
    
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.receivingFTModule announceFilePaths:pathList];    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
        
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    [self.sendingFTModule setFileAnnouncementReceivedDelegate:self];
    self.messageReceived = FALSE;
    
    statusCode = [self.sendingFTModule requestFileAnnouncementFromPeer:[self.receivingBus uniqueName]];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to receive announcement
    STAssertTrue(self.messageReceived, @"received announcement");
}

-(void) testRequestOffer
{
    //test error status returned when no connection    
    FTMStatusCode status = [self.receivingFTModule requestOfferFromPeer:self.sendingBus.uniqueName forFilePath:self.testFilePath];
    STAssertTrue(FTMNOAjConnection == status, @"requestOfferFromPeer");
    
    //test offer correctly rejected
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    status = [self.receivingFTModule requestOfferFromPeer:self.sendingBus.uniqueName forFilePath:self.testFilePath];
    STAssertTrue(FTMRequestDenied == status, @"requestOfferFromPeer");

    //test offer request correctly made
    [self.sendingFTModule setUnannouncedFileRequestDelegate:self];
    
    status = [self.receivingFTModule requestOfferFromPeer:self.sendingBus.uniqueName forFilePath:self.testFilePath];
    STAssertTrue(FTMOK == status, @"requestOfferFromPeer");
    
    //test error status returned when no connection    
    [self.sendingFTModule uninitialize];
    [self.receivingFTModule uninitialize];
    
    status = [self.receivingFTModule requestOfferFromPeer:self.sendingBus.uniqueName forFilePath:self.testFilePath];
    STAssertTrue(FTMNOAjConnection == status, @"requestOfferFromPeer");
    
    //test offer request correctly made	
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    status = [self.receivingFTModule requestOfferFromPeer:self.sendingBus.uniqueName forFilePath:self.testFilePath];
    STAssertTrue(FTMOK == status, @"requestOfferFromPeer");
}

-(void) testGetFileID
{
    [self.sendingFTModule setShowSharedPath:YES];
    
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement

    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to receive announcement
    [self.receivingFTModule uninitialize];
    
    NSData *receivedFileId = [self.receivingFTModule getFileIdForFileWithPath:self.testFilePath ownedBy:self.sendingBus.uniqueName];
    NSData *correctFileId = [self.sendingFTModule.announcedLocalFiles[0] fileID];
    STAssertTrue([receivedFileId isEqualToData:correctFileId], @"getFileIdForFileWithPath");
}

-(void) testGetAvailableRemoteFiles
{    
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
    
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to receive announcement
    [self.receivingFTModule uninitialize];
    
    NSArray *availableFiles = [self.receivingFTModule availableRemoteFiles];
    STAssertTrue(1 == [availableFiles count], @"availableRemoteFiles");
}
-(void) testGetAnnouncedLocalFiles
{
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
    
    NSArray *localFiles = [self.sendingFTModule announcedLocalFiles];
    STAssertTrue(1 == [localFiles count], @"announcedLocalFiles");
}

-(void) testGetOfferedLocalFiles
{
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.sendingFTModule offerFileToPeer:self.receivingBus.uniqueName withFilePath:self.testFilePath andTimeoutMillis:2000];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
    
    NSArray *localFiles = [self.sendingFTModule offeredLocalFiles];
    STAssertTrue(1 == [localFiles count], @"offeredLocalFiles");
}

-(void) testRequestFile
{
    //test error status returned when no connection    
    FTMStatusCode status = [self.receivingFTModule requestOfferFromPeer:self.sendingBus.uniqueName forFilePath:self.testFilePath];
    STAssertTrue(FTMNOAjConnection == status, @"requestOfferFromPeer");
    
    //test requesting an unannounced file
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
        
    FTMFileDescriptor *fileDescriptor = [self generateDummyFileDescriptor];
    status = [self.receivingFTModule requestFileFromPeer:self.sendingBus.uniqueName withFileID:fileDescriptor.fileID andSaveName:@"foo"];
    STAssertTrue(FTMBadFileID == status, @"requestOfferFromPeer");
    
    //test successful request
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
    NSArray *localFiles = [self.sendingFTModule announcedLocalFiles];
    fileDescriptor = [localFiles objectAtIndex:0];
        
    status = [self.receivingFTModule requestFileFromPeer:self.sendingBus.uniqueName withFileID:fileDescriptor.fileID andSaveName:fileDescriptor.filename];
    STAssertTrue(FTMOK == status, @"requestOfferFromPeer");
    
    //test error status returned when no connection
    [self.receivingFTModule uninitialize];
    [self.sendingFTModule uninitialize];
        
    status = [self.receivingFTModule requestOfferFromPeer:self.sendingBus.uniqueName forFilePath:self.testFilePath];
    STAssertTrue(FTMNOAjConnection == status, @"requestOfferFromPeer");
    
    //test successful request
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    status = [self.receivingFTModule requestFileFromPeer:self.sendingBus.uniqueName withFileID:fileDescriptor.fileID andSaveName:fileDescriptor.filename];
    STAssertTrue(FTMOK == status, @"requestOfferFromPeer");
}

-(void) testOfferFileToPeer
{
    //test error status returned when no connection
    int status = [self.sendingFTModule offerFileToPeer:self.receivingBus.uniqueName withFilePath:self.testFilePath andTimeoutMillis:1000];
    STAssertTrue(FTMNOAjConnection == status, @"requestOfferFromPeer");
    
    //test offer correctly made
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    status = [self.sendingFTModule offerFileToPeer:self.receivingBus.uniqueName withFilePath:self.testFilePath andTimeoutMillis:1000];
    STAssertTrue(FTMOfferRejected == status, @"requestOfferFromPeer");
        
    [self.receivingFTModule setOfferReceivedDelegate:self];
    
    status = [self.sendingFTModule offerFileToPeer:self.receivingBus.uniqueName withFilePath:self.testFilePath andTimeoutMillis:1000];
    STAssertTrue(FTMOK == status, @"requestOfferFromPeer");
    
    //test error status once uninitialized
    [self.receivingFTModule uninitialize];
    [self.sendingFTModule uninitialize];
    
    status = [self.sendingFTModule offerFileToPeer:self.receivingBus.uniqueName withFilePath:self.testFilePath andTimeoutMillis:1000];
    STAssertTrue(FTMNOAjConnection == status, @"requestOfferFromPeer");
    
    // The following test is commented out because it results in inconsistent behavior, sometimes hanging the test suite.
    // Although when stepped thru with the debugger, no problem is seen.  We believe the hang is an artifact of the test
    // environment.
    
    //test offer correctly made once re-initialized
    //[self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    //[self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    //status = [self.sendingFTModule offerFileToPeer:self.receivingBus.uniqueName withFilePath:self.testFilePath andTimeoutMillis:1000];
    //STAssertTrue(FTMOK == status, @"requestOfferFromPeer");
}

-(void) testGetSendingProgressList
{    
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
    NSArray *localFiles = [self.sendingFTModule announcedLocalFiles];
    FTMFileDescriptor *fileDescriptor = [localFiles objectAtIndex:0];

    [self.receivingFTModule setChunkSize:1];
    
    int status = [self.receivingFTModule requestFileFromPeer:self.sendingBus.uniqueName withFileID:fileDescriptor.fileID andSaveName:fileDescriptor.filename];
    STAssertTrue(FTMOK == status, @"requestOfferFromPeer");
    
    NSArray *progressList = [self.sendingFTModule sendingProgressList];
    STAssertTrue(1 == [progressList count], @"requestOfferFromPeer");
    
    self.messageReceived = FALSE;
    [self waitForCompletion: 5 * WAIT_TIME_SECS]; //allow time to complete sending chunks
    
    //test progress list is cleared when uninitialized
    [self.receivingFTModule uninitialize];
    [self.sendingFTModule uninitialize];
    
    progressList = [self.sendingFTModule sendingProgressList];
    STAssertTrue(0 == [progressList count], @"requestOfferFromPeer");
}

-(void) testGetReceiveProgressList
{    
    [self.receivingFTModule initializeWithBusAttachment:self.receivingBus andSessionID:self.sessionId];
    [self.sendingFTModule initializeWithBusAttachment:self.sendingBus andSessionID:self.sessionId];
    
    NSMutableArray *pathList = [[NSMutableArray alloc] init];
    [pathList addObject:self.testFilePath];
    [self.sendingFTModule announceFilePaths:pathList];
    
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to generate announcement
    NSArray *localFiles = [self.sendingFTModule announcedLocalFiles];
    FTMFileDescriptor *fileDescriptor = [localFiles objectAtIndex:0];
    
    [self.receivingFTModule setChunkSize:1];
    
    FTMStatusCode status = [self.receivingFTModule requestFileFromPeer:self.sendingBus.uniqueName withFileID:fileDescriptor.fileID andSaveName:fileDescriptor.filename];
    STAssertTrue(FTMOK == status, @"requestOfferFromPeer");
    
    NSArray *progressList = [self.receivingFTModule receiveProgressList];
    STAssertTrue(1 == [progressList count], @"requestOfferFromPeer");
    
    self.messageReceived = FALSE;
    [self waitForCompletion: WAIT_TIME_SECS]; //allow time to complete sending chunks
    
    //test progress list is cleared when uninitialized
    [self.receivingFTModule uninitialize];
    [self.sendingFTModule uninitialize];
    
    progressList = [self.receivingFTModule receiveProgressList];
    STAssertTrue(0 == [progressList count], @"requestOfferFromPeer");
}

@end
