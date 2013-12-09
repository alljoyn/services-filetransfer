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

#import "FTMAnnouncementManager.h"

@interface FTMAnnouncementManager()

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
@property (nonatomic, strong) NSString *localBusID;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the fileAnnouncementReceivedDeleagte.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) NSObject *fileAnnouncementReceivedDelegateLock;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the fileAnnouncementSentDeleagte.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) NSObject *fileAnnouncementSentDelegateLock;

/*
 * Private helper function that executes in a background thread and is responsible for delegating
 * to the FTMFileSystemAbstraction to create the FTMFileDescriptor for each file, invoking the 
 * FTMPermissionManager to store the newly announced files, and sending the announcement. If available,
 * this function will fire the fileAnnouncementSentDelegate to notify the user that an announcement
 * has been sent and pass back an array of paths that failed to be announced.
 *
 * @param pathList Specfies an array of absolute paths of files to be announced.
 *
 * @warning *Note:* This is a private function and is not meant to be called directly.
 */
-(void)announceFiles: (NSArray *)pathList;

/*
 * Private helper function that prepares an FTMAnnounceAction to be sent to the specified peer. If
 * peer is nil, the announcement is broadcast to all session peers. Otherwise, the signal is directed
 * to the specified peer.
 *
 * @param peer Specifies the peer to send the announcement. This value can be nil.
 * @param isFileIDResponse Specifies whether the announcement is a response to a file offer request.
 *
 * @warning *Note:* This is a private function and is not meant to be called directly.
 */
-(void)sendAnnouncementToPeer: (NSString *)peer isFileIDResponse: (BOOL)isFileIDResponse;

@end

@implementation FTMAnnouncementManager

@synthesize showRelativePath = _showRelativePath;
@synthesize showSharedPath = _showSharedPath;
@synthesize dispatcher = _dispatcher;
@synthesize pm = _pm;
@synthesize fsa = _fsa;
@synthesize localBusID = _localBusID;
@synthesize fileAnnouncementReceivedDelegateLock = _fileAnnouncementReceivedDelegateLock;
@synthesize fileAnnouncementSentDelegateLock = _fileAnnouncementSentDelegateLock;

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
        self.fileAnnouncementReceivedDelegateLock = [[NSObject alloc] init];
        self.fileAnnouncementSentDelegateLock = [[NSObject alloc] init];
        self.fileAnnouncementReceivedDelegate = nil;
        self.fileAnnouncementSentDelegate = nil;
	}
	
	return self;
}

-(void)announceFilePaths: (NSArray *)pathList
{
    [self performSelectorInBackground: @selector(announceFiles:) withObject: pathList];
}

-(void)announceFiles: (NSArray *)pathList
{
    NSMutableArray *failedPaths = [[NSMutableArray alloc] init];
    NSArray *files = [self.fsa getFileInfo: pathList withFailedPathsArray: failedPaths andLocalBusID: self.localBusID];
    
    [self.pm addAnnouncedLocalFilesWithList: files];
    [self sendAnnouncementToPeer: nil isFileIDResponse: NO];
    
    if (self.fileAnnouncementSentDelegate != nil)
    {
        [self.fileAnnouncementSentDelegate announcementSentWithFailedPaths: [[NSArray alloc] initWithArray: failedPaths]];
    }
}

-(void)sendAnnouncementToPeer: (NSString *)peer isFileIDResponse: (BOOL)isFileIDResponse
{
    NSArray *myAnnouncedFiles = [self.pm getAnnouncedLocalFiles];
    NSMutableArray *files = [[NSMutableArray alloc] initWithCapacity: [myAnnouncedFiles count]];
    
    for (int i = 0; i < [myAnnouncedFiles count]; i++)
    {
        FTMFileDescriptor *localDescriptor = [myAnnouncedFiles objectAtIndex: i];
        FTMFileDescriptor *announcedDescriptor = [[FTMFileDescriptor alloc] initWithFileDescriptor: localDescriptor];
        
        if (!self.showRelativePath)
        {
            announcedDescriptor.relativePath = @"";
        }
        
        if (!self.showSharedPath)
        {
            announcedDescriptor.sharedPath = @"";
        }
        
        [files addObject: announcedDescriptor];
    }
    
    FTMAnnounceAction *action = [[FTMAnnounceAction alloc] init];
    action.peer = peer;
    action.fileList = files;
    action.isFileIDResponse = isFileIDResponse;
    [self.dispatcher insertAction: action];
}

-(NSArray *)stopAnnounceFilePaths: (NSArray *)pathList
{
	pathList = [self.pm removeAnnouncedLocalFilesWithPaths: pathList];
    [self sendAnnouncementToPeer: nil isFileIDResponse: NO];
	return pathList;
}

-(FTMStatusCode)requestAnnouncementFromPeer: (NSString *)peer
{
	if (self.localBusID == nil)
    {
        return FTMNOAjConnection;
    }
    
    if (self.fileAnnouncementReceivedDelegate == nil)
    {
        return FTMNoFileAnnouncementListener;
    }
    
    FTMRequestAnnouncementAction *action = [[FTMRequestAnnouncementAction alloc] init];
    action.peer = peer;
    [self.dispatcher insertAction: action];
    
	return FTMOK;
}

-(void)handleAnnouncedFiles: (NSArray *)fileList fromPeer: (NSString *)peer
{
	[self.pm updateAnnouncedRemoteFilesWithList: fileList fromPeer: peer];
    
    if (self.fileAnnouncementReceivedDelegate != nil)
    {
        [self.fileAnnouncementReceivedDelegate receivedAnnouncementForFiles: fileList andIsFileIDResponse: NO];
    }
}

-(void)handleAnnouncementRequestFrom: (NSString *)peer
{
	[self sendAnnouncementToPeer: peer isFileIDResponse: NO];
}

-(void)setFileAnnouncementReceivedDelegate: (id<FTMFileAnnouncementReceivedDelegate>)fileAnnouncementReceivedDelegate
{
    @synchronized(self.fileAnnouncementReceivedDelegateLock)
    {
        self->_fileAnnouncementReceivedDelegate = fileAnnouncementReceivedDelegate;
    }
}

-(void)setFileAnnouncementSentDelegate: (id<FTMFileAnnouncementSentDelegate>)fileAnnouncementSentDelegate
{
    @synchronized(self.fileAnnouncementSentDelegateLock)
    {
        self->_fileAnnouncementSentDelegate = fileAnnouncementSentDelegate;
    }
}

-(void)resetStateWithLocalBusID: (NSString *)localBusID
{
    self.localBusID = localBusID;
}

@end