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

#import "FTMReceiverTest.h"

@interface FTMReceiverTest()

@property (nonatomic, strong) AJNBusAttachment *transmitterBus;
@property (nonatomic, strong) AJNBusAttachment *receivingBus;
@property (nonatomic, strong) FileTransferBusObject *transmitterBusObject;
@property (nonatomic, strong) FileTransferBusObject *receivingBusObject;
@property (nonatomic, strong) FTMReceiver *receiver;
@property (nonatomic) AJNSessionId sessionID;
@property (nonatomic) BOOL messageReceived;

@end

@implementation FTMReceiverTest

@synthesize transmitterBus = _transmitterBus;
@synthesize receivingBus = _receivingBus;
@synthesize transmitterBusObject = _transmitterBusObject;
@synthesize receivingBusObject = _receivingBusObject;
@synthesize receiver = _receiver;
@synthesize sessionID = _sessionID;
@synthesize messageReceived = _messageReceived;

-(void)setUp
{
    self.messageReceived = NO;
    
    [self initializeHost];
    [self initializeClient];
    
    [self waitForCompletion: kAllJoynWaitTime];
    STAssertFalse(self.sessionID == 0, @"session not established, invalid session id");    
    
    [self setUpBusObjects];
    
    self.receiver = [[FTMReceiver alloc] initWithBusAttachment:self.receivingBus announcementManagerDelegate:self sendManagerDelegate:self receiveManagerDelegate:self andDirectedAnnouncementManagerDelegate:self];
}

-(void)initializeHost
{
    self.transmitterBus = [[AJNBusAttachment alloc] initWithApplicationName:@"filetransfer" allowRemoteMessages: YES];
    
    int status = [self.transmitterBus start];
    
    status = [self.transmitterBus connectWithArguments: @"null:"];
    
    AJNSessionOptions *sessionOptions = [[AJNSessionOptions alloc] initWithTrafficType: kAJNTrafficMessages supportsMultipoint: YES proximity: kAJNProximityAny transportMask: kAJNTransportMaskAny];
    
    status = [self.transmitterBus requestWellKnownName: kServiceName withFlags: kAJNBusNameFlagReplaceExisting| kAJNBusNameFlagDoNotQueue];
    
    status = [self.transmitterBus bindSessionOnPort: kSessionPort withOptions: sessionOptions withDelegate:self];
    
    status = [self.transmitterBus advertiseName: kServiceName withTransportMask: sessionOptions.transports];
}

-(void)initializeClient
{
    self.receivingBus = [[AJNBusAttachment alloc] initWithApplicationName: @"filetransfer" allowRemoteMessages: YES];
    
    int status = [self.receivingBus start];  
    
    status = [self.receivingBus connectWithArguments: @"null:"];
    
    [self.receivingBus registerBusListener: self];
    
    [self.receivingBus findAdvertisedName: kServiceName];
}

-(void)didFindAdvertisedName: (NSString *)name withTransportMask: (AJNTransportMask) transport namePrefix:(NSString *)namePrefix
{
    AJNSessionOptions *sessionOptions = [[AJNSessionOptions alloc] initWithTrafficType: kAJNTrafficMessages supportsMultipoint: YES proximity: kAJNProximityAny transportMask: kAJNTransportMaskAny];
    
    [self.receivingBus joinSessionWithName: name onPort: kSessionPort withDelegate:self options: sessionOptions];
}

-(BOOL)shouldAcceptSessionJoinerNamed: (NSString *)joiner onSessionPort: (AJNSessionPort)sessionPort withSessionOptions: (AJNSessionOptions *)options
{
    return YES;
}

-(void)didJoin: (NSString *)joiner inSessionWithId: (AJNSessionId)sessionId onSessionPort: (AJNSessionPort)sessionPort
{
    self.sessionID = sessionId;
}

-(void)setUpBusObjects
{
    self.transmitterBusObject = [[FileTransferBusObject alloc] initWithBusAttachment: self.transmitterBus onPath: kObjectPath];
    self.receivingBusObject = [[FileTransferBusObject alloc] initWithBusAttachment: self.receivingBus onPath: kObjectPath];
    
    [self.receivingBus registerBusObject: self.receivingBusObject];
    [self.transmitterBus registerBusObject: self.transmitterBusObject];
}

- (void)tearDown
{
    [self.receivingBus leaveSession: self.sessionID];
    [self.receivingBus disconnectWithArguments: @"null:"];
    [self.transmitterBus disconnectWithArguments: @"null:"];
}

//Unit Tests
-(void)testRequestAnnouncement
{
    [self.transmitterBusObject sendrequestAnnouncementInSession: self.sessionID toDestination: [self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
        
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testStopDataXfer
{
    NSData *fileID = [self getDummyFileDescriptor].fileID;
    AJNMessageArgument *msgArg = [FTMMessageUtility messageArgumentFromFileID:fileID];
    
    [self.transmitterBusObject sendStopDataXferWithFileID: msgArg inSession: self.sessionID toDestination: [self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testDataXferCancelled
{
    NSData *fileID = [self getDummyFileDescriptor].fileID;
    AJNMessageArgument *msgArg = [FTMMessageUtility messageArgumentFromFileID:fileID];
    
    [self.transmitterBusObject sendDataXferCancelledWithFileID: msgArg inSession: self.sessionID toDestination: [self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testDataChunk
{
    NSData *fileID = [self getDummyFileDescriptor].fileID;
    AJNMessageArgument *fildIDMsgArg = [FTMMessageUtility messageArgumentFromFileID: fileID];
    NSData *chunk = [self getDummyData];
    AJNMessageArgument *chunkMessageArg = [FTMMessageUtility messageArgumentFromData: chunk];
    
    [self.transmitterBusObject sendDataChunkWithFileID:fildIDMsgArg startByte: 0 length: 0 andFileChunk:chunkMessageArg inSession: self.sessionID toDestination: [self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testAnnouncement
{
    FTMFileDescriptor *descriptor = [self getDummyFileDescriptor];
    NSArray *fileList = [[NSArray alloc] initWithObjects: descriptor, descriptor, nil];
    AJNMessageArgument *msgArg = [FTMMessageUtility messageArgumentFromFileList:fileList];
        
    [self.transmitterBusObject sendAnnounceWithFileList:msgArg andIsFileIDResponse:false inSession: self.sessionID toDestination: [self.receivingBus uniqueName]];
}

//Delegates called by self.Receiver
-(void)handleAnnouncementRequestFrom: (NSString *)peer
{
    self.messageReceived = YES;
}

-(void)handleStopDataXferForFileWithID: (NSData *)fileID fromPeer: (NSString *)peer
{
    NSData *correctFileID = [self getDummyFileDescriptor].fileID;
    
    STAssertEqualObjects(fileID, correctFileID, @"received incorrect file id");
    
    self.messageReceived = YES;
}

-(void)handleChunkForFile: (NSData *)file withStartByte: (int)startByte andLength: (int)length andFileData: (NSData *)chunk
{
    NSData *correctFileID = [self getDummyFileDescriptor].fileID;
    STAssertEqualObjects(file, correctFileID, @"received incorrect file id");    
    
    STAssertTrue(startByte == 0, @"incorrect number received");
    STAssertTrue(length == 0, @"incorrect number received");
    
    NSData *correctData = [self getDummyData];
    STAssertEqualObjects(chunk, correctData, @"received incorrect data");
    
    self.messageReceived = YES;
}

-(void)handleDataXferCancelledFrom: (NSString *)peer forFileWithID: (NSData *)fileID
{
    NSData *correctFileID = [self getDummyFileDescriptor].fileID;
    
    STAssertEqualObjects(fileID, correctFileID, @"received incorrect file id");
    
    self.messageReceived = YES;
}

-(void)handleAnnouncedFiles: (NSArray *)fileList fromPeer: (NSString *)peer
{    
    FTMFileDescriptor *correct = [self getDummyFileDescriptor];
    STAssertEqualObjects(fileList[0], correct, @"error in received file descriptor");
    STAssertEqualObjects(fileList[1], correct, @"error in received file descriptor");
    
    self.messageReceived = YES;
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
    } while (!self.messageReceived);
}

-(NSData*) getDummyData
{
    const unsigned char data[] = { 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
    NSData *chunk = [[NSData alloc] initWithBytes: data length: 20];
    return chunk;
}

-(FTMFileDescriptor *) getDummyFileDescriptor
{
    FTMFileDescriptor *descriptor = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
    NSData *fileId = [[NSData alloc] initWithBytes: bytes length: 20];
    descriptor.fileID = fileId;
    descriptor.filename = @"foo";
    descriptor.relativePath = @"";
    descriptor.sharedPath = @"bar";
    descriptor.owner = @"You";
    descriptor.size = 1337;
    
    return descriptor;
}

//Implemented to supress warnings. Shouldn't be executed
-(FTMStatusCode)sendFileWithID: (NSData *)fileID withStartByte: (int)startByte andLength: (int)length andMaxChunkLength: (int)maxChunkLength toPeer: (NSString *)peer
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];
}

-(void)dataSent
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];
}

-(int)handleOfferRequestForFile: (NSString *)filePath fromPeer: (NSString *)peer
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];    
}

-(void)handleOfferResponseForFiles: (NSArray *)fileList fromPeer: (NSString *)peer
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];
}

-(void)generateFileDescriptor: (FTCFileIDResponseAction *)action
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];
}

-(int)initiateRequestForFile: (FTMFileDescriptor *)file usingSaveFileName: (NSString *)saveFileName andSaveDirectory: (NSString *)saveDirectory throughDispatcher: (BOOL)useDispatcher
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];
}

@end
