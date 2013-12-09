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

#import "FTMDirectedAnnouncementManagerTests.h"
#import "FTMDirectedAnnouncementManager.h"
#import "FTMMockDispatcher.h"
#import "FTMMockFileSystemAbstraction.h"
#import "FTMMockPermissionsManager.h"
#import "FTMMockTransmitter.h"


@interface FTMDirectedAnnouncementManagerTests()

@property (nonatomic, strong) FTMMockDispatcher *ftcDispatcher;
@property (nonatomic, strong) FTMMockFileSystemAbstraction *ftcFileSystemAbstraction;
@property (nonatomic, strong) FTMMockPermissionsManager *ftcPermissionManager;
@property (nonatomic, strong) FTMMockTransmitter *ftcTransmitter;
@property (nonatomic, strong) FTMDirectedAnnouncementManager *ftcDirectedAnnouncementManager;

@property (nonatomic, strong) NSData *fileID;
@property (nonatomic, strong) FTMFileDescriptor *fileDescriptor;
@property (nonatomic, strong) NSString *localBusId;
@property (nonatomic) FTMStatusCode status;

@property (nonatomic, strong) NSString *expectedFilePath;
@property (nonatomic, strong) NSString *expectedPeer;
@property (nonatomic) BOOL callbackExecuted;
@property (nonatomic) FTMStatusCode unannouncedListenerStatusToReturn;
@end


@implementation FTMDirectedAnnouncementManagerTests

@synthesize ftcDispatcher = _ftcDispatcher;
@synthesize ftcFileSystemAbstraction = _ftcFileSystemAbstraction;
@synthesize ftcPermissionManager = _ftcPermissionManager;
@synthesize ftcTransmitter = _ftcTransmitter;
@synthesize ftcDirectedAnnouncementManager = _ftcDirectedAnnouncementManager;
@synthesize fileID = _fileID;
@synthesize fileDescriptor = _fileDescriptor;
@synthesize localBusId = _localBusId;
@synthesize status = _status;
@synthesize expectedFilePath = _expectedFilePath;
@synthesize expectedPeer = _expectedPeer;
@synthesize callbackExecuted = _callbackExecuted;
@synthesize unannouncedListenerStatusToReturn = _unannouncedListenerStatusToReturn;



- (void)setUp
{
    [super setUp];
    
    self.localBusId = @"DirectedAnnouncementManagerUnitTests";
    
    self.ftcTransmitter = [[FTMMockTransmitter alloc] initWithBusObject: nil busAttachment: nil andSessionID: 0];
    [self.ftcTransmitter setDelegate: self];
    
    self.ftcDispatcher = [[FTMMockDispatcher alloc] initWithTransmitter: self.ftcTransmitter];
    [self.ftcDispatcher setAllowDispatching: YES];
    
    
    self.ftcFileSystemAbstraction = [[FTMMockFileSystemAbstraction alloc]init];
    self.ftcPermissionManager = [[FTMMockPermissionsManager alloc]init];
    
    self.ftcDirectedAnnouncementManager = [[FTMDirectedAnnouncementManager alloc]initWithDispatcher:self.ftcDispatcher permissionsManager:self.ftcPermissionManager fileSystemAbstraction:self.ftcFileSystemAbstraction andLocalBusID:self.localBusId];
    
    self.unannouncedListenerStatusToReturn = FTMOK;
}

- (void)tearDown
{
    self.ftcDirectedAnnouncementManager = nil;
    self.ftcDispatcher = nil;
    self.ftcFileSystemAbstraction = nil;
    self.ftcPermissionManager = nil;
    self.ftcTransmitter = nil;
    
    [super tearDown];
}


- (void) testRequestOffer
{
    // verify request OK
    self.expectedFilePath = @"sdcard/test_path/photo.png";
    self.expectedPeer = @"Bob";
    
    self.callbackExecuted = NO;
    
    [self.ftcTransmitter setStatusCodeToReturn:FTMOK];
    self.status = [self.ftcDirectedAnnouncementManager requestOfferFromPeer: self.expectedPeer forFileWithPath: self.expectedFilePath];
    STAssertTrue(self.status == FTMOK, @"requestOfferFromPeer");
    
    [self waitForCompletion: 5.0f];    
    STAssertTrue(self.callbackExecuted, @"callback was executed");
    
    
    // verify request denied
    self.expectedFilePath =@"sdcard/test_path/story.txt";
    self.expectedPeer = @"Bill";
    
    self.callbackExecuted = NO;
    
    [self.ftcTransmitter setStatusCodeToReturn:FTMRequestDenied];
    self.status = [self.ftcDirectedAnnouncementManager requestOfferFromPeer: self.expectedPeer forFileWithPath: self.expectedFilePath];
    STAssertTrue(self.status == FTMRequestDenied, @"requestOfferFromPeer");
    
    [self waitForCompletion: 5.0f];    
    STAssertTrue(self.callbackExecuted, @"callback was executed");
}

- (void) testHandleOfferRequestForFile
{    
    [self.ftcPermissionManager addAnnouncedLocalFilesWithList:[self generateKnownAnnouncedDummyDescriptorArrayWithPeer:@"Adam"]];
    
    self.status = [self.ftcDirectedAnnouncementManager handleOfferRequestForFile:@"sdcard/reports/animals.txt" fromPeer:@"Adam"];
    STAssertTrue(self.status==FTMOK, @"");
    
    [self.ftcPermissionManager addOfferedLocalFileDescriptor:[self generateSharedFileDescriptor]];
    self.status = [self.ftcDirectedAnnouncementManager handleOfferRequestForFile:@"sdcard/shared/meals.txt" fromPeer:@"Adam"];
    STAssertTrue(self.status==FTMOK, @"");
    
    //Test where the announcers UnannouncedFileListener is uninitialized and set to null.
    [self.ftcDirectedAnnouncementManager setUnannouncedFileRequestDelegate:nil];
    self.status = [self.ftcDirectedAnnouncementManager handleOfferRequestForFile:@"sdcard/unshared/unsharedFile.doc" fromPeer:@"Adam"];
    STAssertTrue(self.status==FTMRequestDenied, @"");
    
    //Test where we initialize the UnannouncedFileListener callback and it returns false
    self.unannouncedListenerStatusToReturn = FTMRequestDenied;
    [self.ftcDirectedAnnouncementManager setUnannouncedFileRequestDelegate:self];
    self.status = [self.ftcDirectedAnnouncementManager handleOfferRequestForFile:@"sdcard/unshared/nvalid.txt" fromPeer:@"Adam"];    
    STAssertTrue(self.status==FTMRequestDenied, @"");

    //Test where the UnannouncedFileListener will return true
    self.unannouncedListenerStatusToReturn = FTMOK;
    [self.ftcDirectedAnnouncementManager setUnannouncedFileRequestDelegate:self];
    self.status = [self.ftcDirectedAnnouncementManager handleOfferRequestForFile:@"" fromPeer:@"Adam"];
    STAssertTrue(self.status==FTMOK, @"");
}
 
- (void) testHandleOfferResponseForFiles
{
    // verify with no listener registered
    [self.ftcDirectedAnnouncementManager setFileAnnouncementReceivedDelegate:nil];
    self.callbackExecuted = NO;
    NSArray *fileDescriptorList = [self generateKnownAnnouncedDummyDescriptorArrayWithPeer:@"Eve"];
    [self.ftcDirectedAnnouncementManager handleOfferResponseForFiles:fileDescriptorList fromPeer:@"Eve"];
    [self waitForCompletion: 1.0f];
    STAssertFalse(self.callbackExecuted, @"callback was executed");
    
    // verify WITH listener registered
    [self.ftcDirectedAnnouncementManager setFileAnnouncementReceivedDelegate:self];
    self.callbackExecuted = NO;
    fileDescriptorList = [self generateKnownAnnouncedDummyDescriptorArrayWithPeer:@"Eve"];
    [self.ftcDirectedAnnouncementManager handleOfferResponseForFiles:fileDescriptorList fromPeer:@"Eve"];
    [self waitForCompletion: 1.0f];
    STAssertTrue(self.callbackExecuted, @"callback was executed");

}

- (void) testGenerateFileDescriptor
{
    FTMFileDescriptor *fileDescriptor = [self.ftcPermissionManager getUnitTestDummyFileDescriptor];
    FTMFileIDResponseAction *action = [[FTMFileIDResponseAction alloc]init];
    action.filePath = fileDescriptor.filename;
    [self.ftcDirectedAnnouncementManager generateFileDescriptor:action];
}


//
//FTCUnannouncedFileRequestDelegate
//
-(BOOL) allowUnannouncedRequestsForFileWithPath: (NSString *) filePath
{
    NSLog(@"allowUnannouncedRequestsForFileWithPath called");
    return (FTMOK == self.unannouncedListenerStatusToReturn);
}

//
//FTCFileAnnouncementReceivedDelegate
//
-(void)receivedAnnouncementForFiles: (NSArray *)fileList andIsFileIDResponse: (BOOL)isFileIDResponse
{
    self.callbackExecuted = YES;
    NSLog(@"receivedAnnouncementForFiles called");
}

//
//Test Protocol Function implementations
//
-(void)dataChunkSent: (FTMDataChunkAction *)dataChunkAction
{
    //Unused in this class
}

-(void)fileIDRequestSent: (FTMFileIDResponseAction *)fileIDResponseAction
{
    //Unused in this class
}

-(void)sendAnnouncementWithFileList: (NSArray *)fileList toPeer: (NSString *)peer andIsFileIDResponse: (BOOL)isFileIDResponse
{
    //Unused in this class
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
    //Unused in this class
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
    self.callbackExecuted = YES;
    STAssertTrue([self.expectedFilePath isEqualToString: filePath], @"file paths are equal");
    STAssertTrue([self.expectedPeer isEqualToString: peer], @"peer names are equal");
}


//Delegate methods
-(void) receivedAnnouncement: (NSArray *)fileList forResponse: (BOOL) isFileIdResponse
{
    //Do nothing
}

-(BOOL) allowUnannouncedFileRequestsForPath: (NSString *)filePath
{
    if ([filePath isEqualToString:@""])
    {
        return YES;
    }
    else
    {
        return NO;
    }
}


//Helper Methods
-(void)waitForCompletion: (NSTimeInterval)timeoutSeconds
{
    NSCondition *condition = [[NSCondition alloc] init];
    
    if (timeoutSeconds > 0)
    {
        [condition waitUntilDate:[NSDate dateWithTimeIntervalSinceNow:(timeoutSeconds)]];
    }
}


-(FTMFileDescriptor *) generateSharedFileDescriptor
{
    FTMFileDescriptor *descriptor = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes[] = { 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9 };
    descriptor.fileID = [[NSData alloc] initWithBytes: bytes length: 20];    
    descriptor.filename = @"meals.txt";
    descriptor.owner = @"bar";
    descriptor.relativePath = @"";
    
    NSString *fullPath = NSHomeDirectory();
    descriptor.sharedPath = [fullPath stringByAppendingPathComponent: @"sdcard/shared"];
    
    descriptor.size = 100;
    
    return descriptor;
}

const unsigned char bytes1[] = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
const unsigned char bytes2[] = { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 };
const unsigned char bytes3[] = { 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 };
const unsigned char bytes4[] = { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 };
const unsigned char bytes5[] = { 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 };
const unsigned char bytes6[] = { 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6 };

-(NSArray *)generateKnownAnnouncedDummyDescriptorArrayWithPeer: (NSString *) owner
{
    NSMutableArray *fileList = [[NSMutableArray alloc] init];
    NSString *fullPath = NSHomeDirectory();
    
    FTMFileDescriptor *descriptor1 = [[FTMFileDescriptor alloc] init];
    descriptor1.fileID = [[NSData alloc] initWithBytes: bytes1 length: 20];
    descriptor1.filename = @"house.png";
    descriptor1.owner = owner;
    descriptor1.relativePath = @"";
    descriptor1.sharedPath = [fullPath stringByAppendingPathComponent: @"sdcard/photos"];
    descriptor1.size = 100;
    [fileList addObject:descriptor1];
    
    FTMFileDescriptor *descriptor2 = [[FTMFileDescriptor alloc] init];
    descriptor2.fileID = [[NSData alloc] initWithBytes: bytes2 length: 20];
    descriptor2.filename = @"backyard.png";
    descriptor2.owner = owner;
    descriptor2.relativePath = @"";
    descriptor2.sharedPath = [fullPath stringByAppendingPathComponent: @"sdcard/photos"];
    descriptor2.size = 100;
    [fileList addObject:descriptor2];
    
    FTMFileDescriptor *descriptor3 = [[FTMFileDescriptor alloc] init];
    descriptor3.fileID = [[NSData alloc] initWithBytes: bytes3 length: 20];
    descriptor3.filename = @"fireplace.png";
    descriptor3.owner = owner;
    descriptor3.relativePath = @"";
    descriptor3.sharedPath = [fullPath stringByAppendingPathComponent: @"sdcard/photos"];
    descriptor3.size = 100;
    [fileList addObject:descriptor3];
    
    FTMFileDescriptor *descriptor4 = [[FTMFileDescriptor alloc] init];
    descriptor4.fileID = [[NSData alloc] initWithBytes: bytes4 length: 20];
    descriptor4.filename = @"animals.txt";
    descriptor4.owner = owner;
    descriptor4.relativePath = @"";
    descriptor4.sharedPath = [fullPath stringByAppendingPathComponent: @"sdcard/reports"];
    descriptor4.size = 100;
    [fileList addObject:descriptor4];
    
    FTMFileDescriptor *descriptor5 = [[FTMFileDescriptor alloc] init];
    descriptor5.fileID = [[NSData alloc] initWithBytes: bytes5 length: 20];
    descriptor5.filename = @"inventors.txt";
    descriptor5.owner = owner;
    descriptor5.relativePath = @"";
    descriptor5.sharedPath = [fullPath stringByAppendingPathComponent: @"sdcard/reports"];
    descriptor5.size = 100;
    [fileList addObject:descriptor5];
    
    FTMFileDescriptor *descriptor6 = [[FTMFileDescriptor alloc] init];
    descriptor6.fileID = [[NSData alloc] initWithBytes: bytes6 length: 20];
    descriptor6.filename = @"driving.txt";
    descriptor6.owner = owner;
    descriptor6.relativePath = @"";
    descriptor6.sharedPath = [fullPath stringByAppendingPathComponent: @"sdcard/reports"];
    descriptor6.size = 100;
    [fileList addObject:descriptor6];
    
    return fileList;
}



@end

