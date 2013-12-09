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

#import "FTMOfferManager.h"

/*
 * Specifies the default timeout rate which is 5 seconds.
 */
static int DEFAULT_TIMEOUT_MILLIS = 5000;

@interface FTMOfferManager()

/*
 * Stores an instance of the FTMDispatcher.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMDispatcher *dispatcher;

/*
 * Stores an instance of the FTMFileSystemAbstraction.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMFileSystemAbstraction *fsa;

/*
 * Stores an instance of the FTMPermissionManager.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMPermissionManager *pm;

/*
 * Stores the local bus ID.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSString *localBusID;

/*
 * Stores a boolean that indicates whether an offer is pending.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) bool isOfferPending;

/*
 * Stores an instance of an FTMFileDescriptor that corresponds to the offered file.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMFileDescriptor *offeredFileDescriptor;

/*
 * Specifies an NSCondition object used for thread synchronization. This object is used to
 * block the calling thread when waiting for an offer response from a remote session peer.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) NSCondition *offeredFileDescriptorLock;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the offerReceivedDelegate.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSObject *offerReceivedDelegateLock;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the sendManagerDelegate.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSObject *sendManagerDelegateLock;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the receiveManagerDelegate.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSObject *receiveManagerDelegateLock;

/*
 * Private helper function that is used to determine if the path you specified
 * matches a file that has already been announced. If so, no additional work is
 * needed and we can send the existing file descriptor in the offer. Otherwise,
 * we have to create a file descriptor for the new file.
 *
 * @param path Specifies the absolute path to file being offered.
 *
 * @return FTMFileDescriptor, nil otherwise.
 */
-(FTMFileDescriptor *)checkAnnouncedFileListForFileWithPath: (NSString *)path;

/*
 * Private helper function that is used to determine if the path you specified
 * matches a file that has already been shared. If so, no additional work is
 * needed and we can send the existing file descriptor in the offer. Otherwise,
 * we have to create a file descriptor for the new file.
 *
 * @param path Specifies the absolute path to file being offered.
 *
 * @return FTMFileDescriptor, nil otherwise.
 */
-(FTMFileDescriptor *)checkSharedFileListForFileWithPath: (NSString *)path;

/*
 * Private helper function that is used to search the specified list of FTMFileDescriptor
 * objects to see if any match the specified path.
 *
 * @param path Specifies the absolute path of the offered file.
 * @param fileList Specifies the list in which to search for the file
 *
 * @return FTMFileDescriptor if a match is found, nil otherwise.
 */
-(FTMFileDescriptor *)checkForFileWithPath: (NSString *)path inFileList: (NSArray *)fileList;

/*
 * Private helper function that is used to build the FTMOfferFileAction and send it
 * to the specified peer and wait for a response.
 *
 * @param fileDescriptor Specifies the FTMFileDescriptor for the offered file.
 * @param peer Specifies the peer to send the offer.
 */
-(FTMStatusCode)transmitOfferFileActionWithFileDescriptor: (FTMFileDescriptor *)fd toPeer: (NSString *)peer;

/*
 * Private helper function which resets member variables that assist with pending offers. This
 * function executes after every file offer sequence is complete.
 */
-(void)resetMemberVariables;

/*
 * Private helper function which sets member variables that assist with pending offers. This
 * function executes after every file offer sequence is complete.
 *
 * @param fileDescriptor Specifies the FTMFileDescriptor of the offered file.
 */
-(void)setMemberVariablesWithFileDescriptor: (FTMFileDescriptor *)fileDescriptor;

@end

@implementation FTMOfferManager

@synthesize offerReceivedDelegate = _offerReceivedDelegate;
@synthesize sendManagerDelegate = _sendManagerDelegate;
@synthesize receiveManagerDelegate = _receiveManagerDelegate;


@synthesize dispatcher = _dispatcher;
@synthesize fsa = _fsa;
@synthesize pm = _pm;
@synthesize localBusID = _localBusID;
@synthesize isOfferPending = _isOfferPending;
@synthesize offeredFileDescriptor = _offeredFileDescriptor;
@synthesize offeredFileDescriptorLock = _offeredFileDescriptorLock;
@synthesize offerReceivedDelegateLock = _offerReceivedDelegateLock;
@synthesize sendManagerDelegateLock = _sendManagerDelegateLock;
@synthesize receiveManagerDelegateLock = _receiveManagerDelegateLock;

-(id)initWithDispatcher: (FTMDispatcher *)dispatcher permissionManager: (FTMPermissionManager *)permissionManager andLocalBusID: (NSString *)localBusID
{
    return [self initWithDispatcher: dispatcher permissionManager: permissionManager fileSystemAbstraction: [FTMFileSystemAbstraction instance] andLocalBusID: localBusID];
}

-(id)initWithDispatcher: (FTMDispatcher *)dispatcher permissionManager: (FTMPermissionManager *)permissionManager fileSystemAbstraction: (FTMFileSystemAbstraction *)fsa andLocalBusID: (NSString *)localBusID
{
    self = [super init];
	
	if (self)
    {
        self.offerReceivedDelegate = nil;
        self.sendManagerDelegate = nil;
        self.receiveManagerDelegate = nil;
		self.dispatcher = dispatcher;
        self.pm = permissionManager;
        self.fsa = fsa;
        self.localBusID = localBusID;
        self.isOfferPending = false;
        self.offeredFileDescriptor = nil;
        self.offeredFileDescriptorLock = [[NSCondition alloc] init];
        self.offerReceivedDelegateLock = [[NSObject alloc] init];
        self.sendManagerDelegateLock = [[NSObject alloc] init];
        self.receiveManagerDelegateLock = [[NSObject alloc] init];
	}
	
	return self;
}

-(void)resetStateWithLocalBusID: (NSString *)localBusID
{
    self.localBusID = localBusID;
}

-(FTMStatusCode)offerFileToPeer: (NSString *)peer forFileWithPath: (NSString *)path andTimeout: (int)timeout
{
    FTMStatusCode offeredFileStatus = FTMBadFilePath;
    FTMFileDescriptor *fileDescriptor = [self checkAnnouncedFileListForFileWithPath:path];
    
    if (fileDescriptor == nil)
    {
        fileDescriptor = [self checkSharedFileListForFileWithPath:path];
    }
    
    if (fileDescriptor == nil)
    {                
        NSMutableArray *failedPaths = [[NSMutableArray alloc] init];
        NSArray *pathList = [[NSArray alloc] initWithObjects: path, nil];
        
        NSArray *descriptorArray = [self.fsa getFileInfo: pathList withFailedPathsArray: failedPaths andLocalBusID: self.localBusID];
        
        if ([failedPaths count] == 0)
        {
            fileDescriptor = descriptorArray[0];
            [self.pm addOfferedLocalFileDescriptor:fileDescriptor];
        }
    }
    
    if (fileDescriptor != nil)
    {
        @try
        {
            [self setMemberVariablesWithFileDescriptor:fileDescriptor];
            
            FTMStatusCode response = [self transmitOfferFileActionWithFileDescriptor:fileDescriptor toPeer:peer];
            
            if (response == FTMOfferAccepted)
            {
                if (timeout < 0)
                {
                    timeout = DEFAULT_TIMEOUT_MILLIS;
                }
                
                if ( timeout > 0)
                {
                    NSDate * startDate = [NSDate date];
                    
                    [self.offeredFileDescriptorLock lock];
                    {
                        if ((self.isOfferPending) && (timeout > 0))
                        {                            
                            [self.offeredFileDescriptorLock waitUntilDate:[NSDate dateWithTimeIntervalSinceNow:(timeout/1000)]];
                                                        
                            timeout = timeout - ([startDate timeIntervalSinceNow] * -1000.0);
                        }
                    }
                    [self.offeredFileDescriptorLock unlock];
                    
                    
                    if (timeout > 0)
                    {
                        offeredFileStatus = FTMOK;
                    }
                    else
                    {
                        offeredFileStatus = FTMOfferTimeout;
                    }
                }
                else
                {
                    offeredFileStatus = FTMOK;
                }
            }
            else
            {
                offeredFileStatus = response;
            }
        }
        @catch (NSException *e) //InterruptedException *e
        {
            offeredFileStatus = FTMOfferTimeout;
        }
        @finally
        {
            [self resetMemberVariables];
        }
    }
    
    return offeredFileStatus;
}

-(FTMFileDescriptor *)checkAnnouncedFileListForFileWithPath: (NSString *)path
{
    return [self checkForFileWithPath: path inFileList: [self.pm getAnnouncedLocalFiles]];
}

-(FTMFileDescriptor *)checkSharedFileListForFileWithPath: (NSString *)path
{
    return [self checkForFileWithPath: path inFileList: [self.pm getOfferedLocalFiles]];
}

-(FTMFileDescriptor *)checkForFileWithPath: (NSString *)path inFileList: (NSArray *)fileList
{
    FTMFileDescriptor *offeredDescriptor = nil;
    
    if (fileList == nil)
    {
        return nil;
    }
    
    for (FTMFileDescriptor *fileDescriptor in fileList)
    {
        NSString *filePath = [self.fsa buildPathFromDescriptor:fileDescriptor];
        
        if ([filePath isEqualToString:path])
        {
            offeredDescriptor = fileDescriptor;
        }
    }
    
    return offeredDescriptor;
}

-(FTMStatusCode)transmitOfferFileActionWithFileDescriptor: (FTMFileDescriptor *)fd toPeer: (NSString *)peer
{
    FTMOfferFileAction *action = [[FTMOfferFileAction alloc] init];
    action.fd = fd;
    action.peer = peer;
    
    return [self.dispatcher transmitImmediately: action];
}

-(void)resetMemberVariables
{
    [self setMemberVariablesWithFileDescriptor: nil];
}

-(void)setMemberVariablesWithFileDescriptor: (FTMFileDescriptor *)fileDescriptor
{
    [self.offeredFileDescriptorLock lock];
    {
        self.isOfferPending = (fileDescriptor != nil);
        self.offeredFileDescriptor = fileDescriptor;
    }
    [self.offeredFileDescriptorLock unlock];
}

-(BOOL)isOfferPendingForFileWithID: (NSData *)fileID
{
    return (self.isOfferPending && [fileID isEqualToData:self.offeredFileDescriptor.fileID]);
}

-(FTMStatusCode)handleRequestFrom: (NSString *)peer forFileID: (NSData *)fileID usingStartByte: (int)startByte withLength: (int)length andMaxChunkLength: (int)maxChunkLength
{
    FTMStatusCode statusCode = FTMRequestDenied;
    
    [self.offeredFileDescriptorLock lock];
    {
        if (self.isOfferPending && [fileID isEqualToData: self.offeredFileDescriptor.fileID])
        {
            //[self resetMemberVariables];
            self.isOfferPending = false;
            self.offeredFileDescriptor = nil;
            
            [self.offeredFileDescriptorLock signal];
        }
    }
    [self.offeredFileDescriptorLock unlock];
    
    @synchronized (self.sendManagerDelegateLock)
    {
        if (self.sendManagerDelegate == nil)
        {
            statusCode = FTMRequestDenied;
        }
        else
        {
            statusCode = [self.sendManagerDelegate sendFileWithID:fileID withStartByte:startByte andLength:length andMaxChunkLength:maxChunkLength toPeer:peer];
        }
    }
    return statusCode;
}

-(FTMStatusCode)handleOfferFrom: (NSString *)peer forFile: (FTMFileDescriptor *)file
{
    BOOL acceptOffer;
    FTMStatusCode statusCode;
    
    @synchronized (self.offerReceivedDelegateLock)
    {
        if (self.offerReceivedDelegate == nil)
        {
            return FTMOfferRejected;
        }
        
        acceptOffer = [self.offerReceivedDelegate acceptFileOfferFrom: peer forFile: file];
    }
    
    if (acceptOffer)
    {
        @synchronized (self.receiveManagerDelegateLock)
        {
            if (self.receiveManagerDelegate != nil)
            {
                statusCode = [self.receiveManagerDelegate initiateRequestForFile:file usingSaveFileName:file.filename andSaveDirectory:nil throughDispatcher:YES];
            }
        }
        return FTMOfferAccepted;
    }
    
    return FTMOfferRejected;
}

-(void)setOfferReceivedDelegate: (id<FTMOfferReceivedDelegate>)offerReceivedDelegate
{
    @synchronized (self.offerReceivedDelegateLock)
    {
        self->_offerReceivedDelegate = offerReceivedDelegate;
    }
}

-(void)setSendManagerDelegate: (id<FTMSendManagerDelegate>)sendManagerDelegate
{
    @synchronized (self.sendManagerDelegateLock)
    {
        self->_sendManagerDelegate = sendManagerDelegate;
    }
}

-(void)setReceiveManagerDelegate: (id<FTMReceiveManagerDelegate>)receiveManagerDelegate
{
    @synchronized (self.receiveManagerDelegateLock)
    {
        self->_receiveManagerDelegate = receiveManagerDelegate;
    }
}

@end
