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

#import "FTMDirectedAnnouncementManager.h"

@interface FTMDirectedAnnouncementManager()
{
    NSString *documentsDirectory;
}

/*
 * Stores an instance of the FTMDispatcher.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMDispatcher *dispatcher;

/*
 * Stores an instance of the FTMPermissionManager.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMPermissionManager *pm;

/*
 * Stores an instance of the FTMFileSystemAbstraction.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMFileSystemAbstraction *fsa;

/*
 * Stores the local bus ID.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSString *localBusID;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the fileAnnouncementReceivedDeleagte.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSObject *fileAnnouncementReceivedDelegateLock;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the unannouncedFileRequestDeleagte.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSObject *unannouncedFileRequestDelegateLock;

/*
 * Private helper function that searches the announced and shared file lists to find a file 
 * that matches the specified path.
 *
 * @param path Specifies the absolute path of the file.
 *
 * @return FTMFileDescriptor if the file has been previously announced or shared, nil otherwise.
 */
-(FTMFileDescriptor *)checkAnnouncedAndSharedFileListForPath: (NSString *)filePath;

/*
 * Private helper function that takes an FTMFileDescriptor parameter, stores it into an array, and 
 * sends an announce action to the specified peer.
 *
 * @param descriptor Specifies the FTMFileDescriptor of the file to be announced.
 * @param peer Specifies the peer to receive the announcement.
 */
-(void)insertSingleDescriptorAnnouncementUsingDescriptor: (FTMFileDescriptor *)descriptor andSendToPeer: (NSString *)peer;

/*
 * Private helper function that is called when the user grants the offer request of a remote peer.
 * A FTMFileIDResponse action will be inserted into the FTMDispatcher and will allow the class to 
 * call back to the FTMDirectedAnnouncementManager so a file descriptor for the filePath can be 
 * constructed and sent to specified remote peer.
 *
 * @param filePath Specifies the absolute path of the file.
 * @param peer Specifies the peer to receive the announcement.
 */
-(void)insertFileIDResponseActionForFilePath: (NSString *)filePath andPeer: (NSString *)peer;

@end

@implementation FTMDirectedAnnouncementManager

@synthesize fileAnnouncementReceivedDelegate = _fileAnnouncementReceivedDelegate;
@synthesize unannouncedFileRequestDelegate = _unannouncedFileRequestDelegate;
@synthesize showRelativePath = _showRelativePath;
@synthesize showSharedPath = _showSharedPath;
@synthesize dispatcher = _dispatcher;
@synthesize pm = _pm;
@synthesize fsa = _fsa;
@synthesize localBusID = _localBusID;
@synthesize fileAnnouncementReceivedDelegateLock = _fileAnnouncementReceivedDelegateLock;
@synthesize unannouncedFileRequestDelegateLock = _unannouncedFileRequestDelegateLock;

-(id)initWithDispatcher: (FTMDispatcher *)dispatcher permissionsManager: (FTMPermissionManager *)permissionManager andLocalBusID: (NSString *)localBusID
{
	return [self initWithDispatcher: dispatcher permissionsManager: permissionManager fileSystemAbstraction: [FTMFileSystemAbstraction instance] andLocalBusID: localBusID];
}

-(id)initWithDispatcher: (FTMDispatcher *)dispatcher permissionsManager: (FTMPermissionManager *)permissionManager fileSystemAbstraction: (FTMFileSystemAbstraction *)fsa andLocalBusID: (NSString *)localBusID
{
    self = [super init];
	
	if (self)
    {
		self.showRelativePath = YES;
		self.showSharedPath = NO;
		self.dispatcher = dispatcher;
        self.pm = permissionManager;
        self.fsa = fsa;
        self.localBusID = localBusID;
        self.fileAnnouncementReceivedDelegate = nil;
        self.unannouncedFileRequestDelegate = nil;
        self.fileAnnouncementReceivedDelegateLock = [[NSObject alloc] init];
        self.unannouncedFileRequestDelegateLock = [[NSObject alloc] init];
        documentsDirectory = NSHomeDirectory();
	}
	
	return self;
}

-(FTMStatusCode)requestOfferFromPeer: (NSString *)peer forFileWithPath: (NSString *)filePath
{
    FTMRequestOfferAction *action = [[FTMRequestOfferAction alloc] init];
    action.filePath = filePath;
    action.peer = peer;
    
    return [self.dispatcher transmitImmediately: action];
}

-(FTMStatusCode)handleOfferRequestForFile: (NSString *)filePath fromPeer: (NSString *)peer
{
    NSString *fullPath = NSHomeDirectory();
    fullPath = [fullPath stringByAppendingPathComponent: filePath];
    
    FTMFileDescriptor *descriptor = [self checkAnnouncedAndSharedFileListForPath: fullPath];
    
    if (descriptor != nil)
    {
        [self insertSingleDescriptorAnnouncementUsingDescriptor: descriptor andSendToPeer: peer];
        return FTMOK;
    }
    else
    {
        @synchronized (self.unannouncedFileRequestDelegateLock)
        {
            if (self.unannouncedFileRequestDelegate != nil)
            {
                if ([self.unannouncedFileRequestDelegate allowUnannouncedRequestsForFileWithPath: fullPath])
                {
                    [self insertFileIDResponseActionForFilePath: fullPath andPeer: peer];
                    return FTMOK;
                }
                else
                {
                    return FTMRequestDenied;
                }
            }
            else
            {
                return FTMRequestDenied;
            }
        }
    }
}

-(FTMFileDescriptor *)checkAnnouncedAndSharedFileListForPath: (NSString *)path
{
    NSArray *announcedFiles = [self.pm getAnnouncedLocalFiles];
        
    for (FTMFileDescriptor *fileDescriptor in announcedFiles)
    {
        NSString *filePath = [self.fsa buildPathFromDescriptor:fileDescriptor];
        
        if ([filePath isEqualToString:path])
        {
            return fileDescriptor;
        }
    }
    
    NSArray *sharedFiles = [self.pm getOfferedLocalFiles];
    
    for (FTMFileDescriptor *fileDescriptor in sharedFiles)
    {
        NSString *filePath = [self.fsa buildPathFromDescriptor: fileDescriptor];
        
        if ([filePath isEqualToString:path])
        {
            return fileDescriptor;
        }
    }
    
    return nil;
}

-(void)insertSingleDescriptorAnnouncementUsingDescriptor: (FTMFileDescriptor *)descriptor andSendToPeer: (NSString *)peer
{
    FTMFileDescriptor *fd = [[FTMFileDescriptor alloc] initWithFileDescriptor: descriptor];
    
    if (!self.showRelativePath)
    {
        fd.relativePath = @"";
    }
    
    if (!self.showSharedPath)
    {
        fd.sharedPath = @"";
    }
    
    NSArray *descriptorArray = [[NSArray alloc] initWithObjects: fd, nil];
    
    FTMAnnounceAction *action = [[FTMAnnounceAction alloc] init];
    action.peer = peer;
    action.fileList = descriptorArray;
    action.isFileIDResponse = YES;
    [self.dispatcher insertAction: action];
}

-(void)insertFileIDResponseActionForFilePath: (NSString *)filePath andPeer: (NSString *)peer
{
    FTMFileIDResponseAction *action = [[FTMFileIDResponseAction alloc] init];
    action.peer = peer;
    action.filePath = filePath;
    [self.dispatcher insertAction: action];
}

-(void)handleOfferResponseForFiles: (NSArray *)fileList fromPeer: (NSString *)peer
{
    FTMFileDescriptor *descriptor = [fileList objectAtIndex: 0];
    [self.pm addOfferedRemoteFileDescriptor: descriptor fromPeer: peer];
    
    @synchronized (self.fileAnnouncementReceivedDelegateLock)
    {
        if (self.fileAnnouncementReceivedDelegate != nil)
        {
            [self.fileAnnouncementReceivedDelegate receivedAnnouncementForFiles: fileList andIsFileIDResponse: YES];
        }
    }
}

-(void)generateFileDescriptor: (FTMFileIDResponseAction *)action
{
    NSString *filePath = action.filePath;
    NSString *peer = action.peer;
    
    NSMutableArray *failedPaths = [[NSMutableArray alloc] init];
    NSArray *pathList = [[NSArray alloc] initWithObjects: filePath, nil];
    
    NSArray *descriptorArray = [self.fsa getFileInfo: pathList withFailedPathsArray: failedPaths andLocalBusID: self.localBusID];
    
    if (([descriptorArray count] != 1) || ([failedPaths count] == 1))
    {
        return;
    }
    else
    {
        FTMFileDescriptor *generatedDescriptor = [[FTMFileDescriptor alloc] initWithFileDescriptor: [descriptorArray objectAtIndex: 0]];
        [self.pm addOfferedLocalFileDescriptor: generatedDescriptor];
        
        FTMAnnounceAction *action = [[FTMAnnounceAction alloc] init];
        action.fileList = descriptorArray;
        action.isFileIDResponse = YES;
        action.peer = peer;
        [self.dispatcher insertAction: action];
    }
}

-(void)setFileAnnouncementReceivedDelegate:(id<FTMFileAnnouncementReceivedDelegate>)fileAnnouncementReceivedDelegate
{
    @synchronized (self.fileAnnouncementReceivedDelegateLock)
    {
        self->_fileAnnouncementReceivedDelegate = fileAnnouncementReceivedDelegate;
    }
}

-(void)setUnannouncedFileRequestDelegate:(id<FTMUnannouncedFileRequestDelegate>)unannouncedFileRequestDelegate
{
    @synchronized (self.unannouncedFileRequestDelegateLock)
    {
        self->_unannouncedFileRequestDelegate = unannouncedFileRequestDelegate;
    }
}

-(void)resetStateWithLocalBusID: (NSString *)localBusID
{
    self.localBusID = localBusID;
}

@end
