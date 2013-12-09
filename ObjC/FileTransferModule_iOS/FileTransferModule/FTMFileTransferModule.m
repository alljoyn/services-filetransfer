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

@interface FTMFileTransferModule()

/*
 * Stores an instance of the FTMAnnouncementManager.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMAnnouncementManager *announcementManager;

/*
 * Stores an instance of the FTMDispatcher.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMDispatcher *dispatcher;

/*
 * Stores an instance of the FTMSendManager.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMSendManager *sendManager;

/*
 * Stores an instance of the FTMReceiveManager.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMReceiveManager *receiveManager;

/*
 * Stores an instance of the FTMFileTransferBusObject.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMFileTransferBusObject *busObject;

/*
 * Stores an instance of the FTMPermissionManager.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMPermissionManager *pm;

/*
 * Stores an instance of the FTMDirectedAnnouncementManager.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMDirectedAnnouncementManager *directedAnnouncementManager;

/*
 * Stores an instance of the FTMOfferManager.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMOfferManager *offerManager;

/*
 * Stores an instance of the FTMReceiver.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMReceiver *receiver;

/*
 * Stores an instance of the FTMFileSystemAbstraction.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMFileSystemAbstraction *fsa;

@end

@implementation FTMFileTransferModule

@synthesize announcementManager = _announcementManager;
@synthesize dispatcher = _dispatcher;
@synthesize sendManager = _sendManager;
@synthesize receiveManager = _receiveManager;
@synthesize busObject = _busObject;
@synthesize pm = _pm;
@synthesize directedAnnouncementManager = _directedAnnouncementManager;
@synthesize offerManager = _offerManager;
@synthesize receiver = _receiver;
@synthesize fsa = _fsa;

- (id)init
{
    return [self initWithBusAttachment: nil andSessionID: 0];
}

-(id)initWithBusAttachment: (AJNBusAttachment *)busAttachment andSessionID: (AJNSessionId)sessionID
{
	self = [super init];
	
	if (self)
    {
        //Set BusObject and extract local bus ID
		self.busObject = [[FTMFileTransferBusObject alloc] initWithBusAttachment: busAttachment onPath: kObjectPath];
        NSString *localBusID = (busAttachment == nil) ? nil : busAttachment.uniqueName;
        
        //Initialize local variables
        self.fsa = [FTMFileSystemAbstraction instance];
        self.pm = [[FTMPermissionManager alloc] init];
        
        self.dispatcher = [[FTMDispatcher alloc] initWithBusObject: self.busObject busAttachment: busAttachment andSessionID: sessionID];
        self.announcementManager = [[FTMAnnouncementManager alloc] initWithDispatcher: self.dispatcher permissionsManager: self.pm andLocalBusID: localBusID];
        self.offerManager = [[FTMOfferManager alloc] initWithDispatcher: self.dispatcher permissionManager: self.pm andLocalBusID: localBusID];
        self.directedAnnouncementManager = [[FTMDirectedAnnouncementManager alloc] initWithDispatcher: self.dispatcher permissionsManager: self.pm andLocalBusID: localBusID];
        self.sendManager = [[FTMSendManager alloc] initWithDispatcher: self.dispatcher andPermissionManager: self.pm];
        self.receiveManager = [[FTMReceiveManager alloc] initWithDispatcher: self.dispatcher andPermissionManager:self.pm];
        self.receiver = [[FTMReceiver alloc] initWithBusAttachment: busAttachment announcementManagerDelegate: self.announcementManager sendManagerDelegate: self.sendManager receiveManagerDelegate: self.receiveManager andDirectedAnnouncementManagerDelegate: self.directedAnnouncementManager];
        
        //Set Delegates
        [self.offerManager setSendManagerDelegate: self.sendManager];
        [self.offerManager setReceiveManagerDelegate: self.receiveManager];
        [self.dispatcher setSendManagerDelegate: self.sendManager];
        [self.dispatcher setDirectedAnnouncementManagerDelegate: self.directedAnnouncementManager];
        [self.busObject setSendManagerDelegate: self.sendManager];
        [self.busObject setOfferManagerDelegate: self.offerManager];
        [self.busObject setDirectedAnnouncementManagerDelegate: self.directedAnnouncementManager];
	}
	
	return self;
}

-(void)initializeWithBusAttachment: (AJNBusAttachment *)busAttachment andSessionID: (AJNSessionId)sessionID
{
    self.busObject = (busAttachment == nil) ? nil :[[FTMFileTransferBusObject alloc] initWithBusAttachment: busAttachment onPath: kObjectPath];
    NSString *localBusID = (busAttachment == nil) ? nil : busAttachment.uniqueName;
    
    [self.pm resetStateWithLocalBusID: localBusID];
    [self.dispatcher resetStateWithBusObject: self.busObject busAttachment: busAttachment andSessionID: sessionID];
    [self.announcementManager resetStateWithLocalBusID: localBusID];
    [self.offerManager resetStateWithLocalBusID: localBusID];
    [self.directedAnnouncementManager resetStateWithLocalBusID: localBusID];
    [self.sendManager resetState];
    [self.receiveManager resetState];
    [self.receiver resetStateWithBusAttachment: busAttachment announcementManagerDelegate: self.announcementManager sendManagerDelegate: self.sendManager receiveManagerDelegate: self.receiveManager andDirectedAnnouncementManagerDelegate: self.directedAnnouncementManager];
    
    [self.busObject setSendManagerDelegate: self.sendManager];
    [self.busObject setOfferManagerDelegate: self.offerManager];
    [self.busObject setDirectedAnnouncementManagerDelegate: self.directedAnnouncementManager];
    
    if ([[self.pm getAnnouncedLocalFiles] count] > 0)
    {
        [self.announcementManager handleAnnouncementRequestFrom: nil];
    }
}

-(void)uninitialize
{
    [self initializeWithBusAttachment: nil andSessionID: 0];
}

-(void)announceFilePaths: (NSArray *)paths
{
    [self.announcementManager announceFilePaths: paths];
}

-(NSArray *)stopAnnounceFilePaths: (NSArray *)paths
{
    return [self.announcementManager stopAnnounceFilePaths: paths];
}

-(FTMStatusCode)requestFileAnnouncementFromPeer: (NSString *)peer
{
    return [self.announcementManager requestAnnouncementFromPeer: peer];
}

-(FTMStatusCode)requestOfferFromPeer: (NSString *)peer forFilePath: (NSString *)path
{
    return [self.directedAnnouncementManager requestOfferFromPeer: peer forFileWithPath: path];
}

-(NSData *)getFileIdForFileWithPath: (NSString *)path ownedBy: (NSString *)peer
{
    return [self.pm getFileIDForFileWithPath: path ownedBy: peer];
}

-(NSArray *)availableRemoteFiles
{
    return [self.pm getAvailableRemoteFiles];
}

-(NSArray *)announcedLocalFiles
{
    return [self.pm getAnnouncedLocalFiles];
}

-(NSArray *)offeredLocalFiles
{
    return [self.pm getOfferedLocalFiles];
}

-(void)setShowRelativePath: (BOOL)showRelativePath
{
    [self.announcementManager setShowRelativePath: showRelativePath];
    [self.directedAnnouncementManager setShowRelativePath: showRelativePath];
}

-(BOOL)showRelativePath
{
    return [self.announcementManager showRelativePath];
}

-(void)setShowSharedPath: (BOOL)showSharedPath
{
    [self.announcementManager setShowSharedPath: showSharedPath];
    [self.directedAnnouncementManager setShowSharedPath: showSharedPath];
}

-(BOOL)showSharedPath
{
    return [self.announcementManager showSharedPath];
}

-(void)setDefaultSaveDirectory: (NSString *)defaultSaveDirectory
{
    [self.receiveManager setDefaultSaveDirectory: defaultSaveDirectory];
}

-(void)setCacheFileWithPath:(NSString *)path
{
    [self.fsa setCacheFileWithPath: path];
}

-(void)cleanCacheFile
{
    [self.fsa cleanCacheFile];
}

-(void)setChunkSize: (int)chunkSize
{
    [self.receiveManager setMaxChunkSize: chunkSize];
}

-(int)chunkSize
{
    return [self.receiveManager maxChunkSize];
}

-(FTMStatusCode)requestFileFromPeer: (NSString *)peer withFileID: (NSData *)fileId andSaveName: (NSString *)fileName
{
    return [self.receiveManager requestFileOwnedBy: peer withFileID: fileId saveFileName: fileName andSaveFileDirectory: nil];
}

-(FTMStatusCode)requestFileFromPeer: (NSString *)peer withFileID: (NSData *)fileId andSaveName: (NSString *)fileName andSaveDirectory:(NSString *)directory
{
	return [self.receiveManager requestFileOwnedBy: peer withFileID: fileId saveFileName: fileName andSaveFileDirectory: directory];
}

- (FTMStatusCode)offerFileToPeer: (NSString *)peer withFilePath: (NSString *)path andTimeoutMillis: (int)timeout
{
	return [self.offerManager offerFileToPeer: peer forFileWithPath: path andTimeout: timeout];
}

- (FTMStatusCode)cancelSendingFileWithID: (NSData *)fileID
{
	return [self.sendManager cancelFileWithID: fileID];
}

- (FTMStatusCode)cancelReceivingFileWithID: (NSData *)fileID
{
	return [self.receiveManager cancelFileWithID: fileID];
}

- (FTMStatusCode)pauseReceivingFileWithID: (NSData *)fileID
{
	return [self.receiveManager pauseFileWithID: fileID];
}

- (NSArray *)sendingProgressList
{
    return [self.sendManager getProgressList];
}

- (NSArray *)receiveProgressList
{
    return [self.receiveManager getProgressList];
}

-(void)setFileAnnouncementReceivedDelegate: (id<FTMFileAnnouncementReceivedDelegate>)fileAnnouncementReceivedDelegate
{
    [self.announcementManager setFileAnnouncementReceivedDelegate: fileAnnouncementReceivedDelegate];
    [self.directedAnnouncementManager setFileAnnouncementReceivedDelegate: fileAnnouncementReceivedDelegate];
}

-(void)setFileAnnouncementSentDelegate: (id<FTMFileAnnouncementSentDelegate>)fileAnnouncementSentDelegate
{
    [self.announcementManager setFileAnnouncementSentDelegate: fileAnnouncementSentDelegate];
}

-(void)setFileCompletedDelegate: (id<FTMFileCompletedDelegate>)fileCompletedDelegate
{
    [self.receiveManager setFileCompletedDelegate: fileCompletedDelegate];
}

-(void)setOfferReceivedDelegate: (id<FTMOfferReceivedDelegate>)offerReceivedDelegate
{
    [self.offerManager setOfferReceivedDelegate: offerReceivedDelegate];
}

-(void)setRequestDataReceivedDelegate: (id<FTMRequestDataReceivedDelegate>)requestDataReceivedDelegate
{
    [self.sendManager setRequestDataReceivedDelegate: requestDataReceivedDelegate];
}

-(void)setUnannouncedFileRequestDelegate: (id<FTMUnannouncedFileRequestDelegate>)unannouncedFileRequestDelegate
{
    [self.directedAnnouncementManager setUnannouncedFileRequestDelegate: unannouncedFileRequestDelegate];
}

@end



