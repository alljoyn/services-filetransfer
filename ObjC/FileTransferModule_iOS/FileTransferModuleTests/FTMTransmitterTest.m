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

#import "FTMTransmitterTest.h"

@interface FTMTransmitterTest()

@property (nonatomic, strong) AJNBusAttachment *transmitterBus;
@property (nonatomic, strong) AJNBusAttachment *receivingBus;
@property (nonatomic, strong) FTMFileTransferBusObject *transmitterBusObject;
@property (nonatomic, strong) FTMFileTransferBusObject *receivingBusObject;
@property (nonatomic, strong) FTMTransmitter *transmitter;
@property (nonatomic) AJNSessionId sessionID;
@property (nonatomic) BOOL messageReceived;

@end

@implementation FTMTransmitterTest

@synthesize handle = _handle;
@synthesize transmitterBus = _transmitterBus;
@synthesize receivingBus = _receivingBus;
@synthesize transmitterBusObject = _transmitterBusObject;
@synthesize receivingBusObject = _receivingBusObject;
@synthesize transmitter = _transmitter;
@synthesize sessionID = _sessionID;
@synthesize messageReceived = _messageReceived;

-(void)setUp
{
    self.messageReceived = NO;    

    [self initializeHost];
    [self initializeClient];
    
    [self waitForCompletion: kAllJoynWaitTime];
    STAssertFalse(self.sessionID == 0, @"session not established, invalid session id");

    self.receivingBusObject.directedAnnouncementManagerDelegate = self;
    self.receivingBusObject.sendManagerDelegate = self;
    self.receivingBusObject.offerManagerDelegate = self;
    
    [self.receivingBus registerBusObject: self.receivingBusObject];
    
    self.transmitterBusObject = [[FTMFileTransferBusObject alloc] initWithBusAttachment: self.transmitterBus onPath: kObjectPath];
    self.transmitter = [[FTMTransmitter alloc] initWithBusObject: self.transmitterBusObject busAttachment: self.transmitterBus andSessionID: self.sessionID];
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
    
    self.receivingBusObject = [[FTMFileTransferBusObject alloc] initWithBusAttachment: self.receivingBus onPath: kObjectPath];
    
    [self.receivingBus registerDataTransferDelegateSignalHandler: self];
    [self.receivingBus registerFileDiscoveryDelegateSignalHandler: self];
    
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
    return true;
}

-(void)didJoin: (NSString *)joiner inSessionWithId: (AJNSessionId)sessionId onSessionPort: (AJNSessionPort)sessionPort
{
    self.sessionID = sessionId;
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
    [self.transmitter sendAnnouncementRequestToPeer: [self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testStopDataXfer
{
    FTMFileDescriptor *fd = [self getDummyFileDescriptor];
    
    [self.transmitter sendStopDataXferForFileID: fd.fileID toPeer: [self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testDataXferCancelled
{
     FTMFileDescriptor *fd = [self getDummyFileDescriptor];
    
    [self.transmitter sendXferCancelledForFileID: fd.fileID toPeer: [self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testDataChunk
{
    FTMFileDescriptor *fd = [self getDummyFileDescriptor];
    NSData *data = [self getDummyData];
     
    [self.transmitter sendDataChunkUsingFileID: fd.fileID startByte: 0 chunkLength: 0 andFileData: data toPeer:[self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testAnnouncement
{
    FTMFileDescriptor *descriptor = [self getDummyFileDescriptor];
    
    NSArray *fileList = [[NSArray alloc] initWithObjects: descriptor, descriptor, nil];
    
    [self.transmitter sendAnnouncementWithFileList: fileList toPeer: [self.receivingBus uniqueName] andIsFileIDResponse:false];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testRequestOffer
{
    NSString *path = [self getDummyFileDescriptor].sharedPath;
    
    [self.transmitter sendRequestOfferForFileWithPath:path toPeer:[self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testRequestData
{
    FTMFileDescriptor *fd = [self getDummyFileDescriptor];
    
    [self.transmitter sendRequestDataUsingFileID:fd.fileID startByte:0 length:0 andMaxChunkSize:0 toPeer:[self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];

    STAssertTrue(self.messageReceived, @"Message not received");
}

-(void)testOffer
{
    FTMFileDescriptor *descriptor = [self getDummyFileDescriptor];
    
    [self.transmitter sendOfferFileWithFileDescriptor:descriptor toPeer:[self.receivingBus uniqueName]];
    
    [self waitForCompletion: kAllJoynWaitTime];
    
    STAssertTrue(self.messageReceived, @"Message not received");
}

// From FileTransferBusObjectDelegateSignalHandler
-(void)didReceiveDataChunkWithFileID:(AJNMessageArgument*)fileID startByte:(NSNumber*)startByte length:(NSNumber*)chunkLength andFileChunk:(AJNMessageArgument*)chunk inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{
    NSData *receivedFileID = [FTMMessageUtility fileIDFromMessageArgument:fileID];
    NSData *correctFileID = [self getDummyFileDescriptor].fileID;
    STAssertEqualObjects(receivedFileID, correctFileID, @"received incorrect file id");
    
    NSNumber *correctValue = [[NSNumber alloc] initWithInt:0];
    STAssertEqualObjects(startByte, correctValue, @"incorrect number received");
    STAssertEqualObjects(chunkLength, correctValue, @"incorrect number received");
    
    NSData *receivedData = [FTMMessageUtility fileIDFromMessageArgument:chunk];
    NSData *correctData = [self getDummyData];
    STAssertEqualObjects(receivedData, correctData, @"received incorrect data");
    
    self.messageReceived = YES;
}

-(void)didReceiveStopDataXferWithFileID:(AJNMessageArgument*)fileID inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{
    NSData *receivedFileID = [FTMMessageUtility fileIDFromMessageArgument:fileID];
    
    NSData *correct = [self getDummyFileDescriptor].fileID;
    STAssertEqualObjects(receivedFileID, correct, @"received incorrect file id");
    
    self.messageReceived = YES;
}

-(void)didReceiveDataXferCancelledWithFileID:(AJNMessageArgument*)fileID inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{    
    NSData *receivedFileID = [FTMMessageUtility fileIDFromMessageArgument:fileID];
    
    NSData *correct = [self getDummyFileDescriptor].fileID;
    STAssertEqualObjects(receivedFileID, correct, @"received incorrect file id");
    
    self.messageReceived = YES;
}

-(void)didReceiveAnnounceWithFileList:(AJNMessageArgument*)fileList andIsFileIDResponse:(BOOL)isFileIDResponse inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{
    NSArray *files = [FTMMessageUtility descriptorArrayFromMessageArgument:fileList];
    
    FTMFileDescriptor *correct = [self getDummyFileDescriptor];
    STAssertEqualObjects(files[0], correct, @"error in received file descriptor");
    STAssertEqualObjects(files[1], correct, @"error in received file descriptor");
    
    self.messageReceived = YES;
}

-(void)didReceiverequestAnnouncementInSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{
    self.messageReceived = YES;
}

//Method Handler Delegates
-(int)handleOfferRequestForFile: (NSString *)filePath fromPeer: (NSString *)peer
{
    NSString *correctFilePath = [self getDummyFileDescriptor].sharedPath;
    STAssertEqualObjects(filePath, correctFilePath, @"received incorrect string");
    
    self.messageReceived = YES;
    
    return FTMOK;
}

-(FTMStatusCode)sendFileWithID: (NSData *)fileID withStartByte: (int)startByte andLength: (int)length andMaxChunkLength: (int)maxChunkLength toPeer: (NSString *)peer
{
    NSData *correctFileID = [self getDummyFileDescriptor].fileID;
    STAssertEqualObjects(fileID, correctFileID, @"received incorrect file id");
    
    int correctValue = 0;
    STAssertTrue(startByte == correctValue, @"incorrect number received");
    STAssertTrue(length == correctValue, @"incorrect number received");
    STAssertTrue(maxChunkLength == correctValue, @"incorrect number received");
    
    self.messageReceived = YES;
    
    return FTMOK;
}

-(FTMStatusCode)handleOfferFrom: (NSString *)peer forFile: (FTMFileDescriptor *)file
{
    STAssertEqualObjects(file, [self getDummyFileDescriptor], @"error in received file descriptor");
    self.messageReceived = YES;    
    return FTMOK;
}

//Called by the receiving bus object to determine which delegate to trigger
-(BOOL)isOfferPendingForFileWithID: (NSData *)fileID
{
    return false;
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
-(void)handleOfferResponseForFiles: (NSArray *)fileList fromPeer: (NSString *)peer
{
     @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];
}

-(void)generateFileDescriptor: (FTCFileIDResponseAction *)action
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];    
}

-(void)dataSent
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];     
}

-(void)handleStopDataXferForFileWithID: (NSData *)fileID fromPeer: (NSString *)peer
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];     
}

-(FTMStatusCode)handleRequestFrom: (NSString *)peer forFileID: (NSData *)fileID usingStartByte: (int)startByte withLength: (int)length andMaxChunkLength: (int)maxChunkLength
{
    @throw [NSException exceptionWithName:@"NotImplementedException" reason:@"" userInfo:nil];
}

@end
