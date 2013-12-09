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

#import "FTMSendManager.h"

@interface FTMSendManager()

/*
 * Stores an array of FTMFileStatus objects that monitor the sending progress of each file.
 */
@property (nonatomic, strong) NSMutableArray *sendingFiles;

/*
 * Stores an instance of the FTMFileSystemAbstraction.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMFileSystemAbstraction *fsa;

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
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the requestDataReceivedDelegate.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSObject *requestDataReceivedDelegateLock;

/*
 * Private helper function that initiates the transfer of the requested file.
 *
 * @param fileID Specifies the ID of the file being requested.
 * @param startByte Specifies the starting position within the file (usually zero).
 * @param length Specifies the number of bytes to be sent (usually the length of the file).
 * @param peer Specifies the intended recipient of the file.
 * @param maxChunkLength Specifies the maximum file chunk size.
 *
 * @return FTMStatusCode FTMOK or FTMBadFileID.
 */
-(FTMStatusCode)startSendingFileWithID: (NSData *)fileID withStartByte: (int)startByte length: (int)length toPeer: (NSString *)peer andMaxChunLength: (int)maxChunkLength;

/*
 * Private helper function that gets the next available file chunk and inserts a FTMDataChunkAction
 * into the FTMDispatcher for processing. The file chunk is sent to the specified peer.
 *
 * @param fileID Specifies the file ID of the file being requested.
 * @param startByte Specifies the starting position within the file (usually zero).
 * @param length Specifies the number of bytes to be sent (usually the length of the file).
 * @param peer Specifies the intended recipient of the file.
 * @param maxChunkLength Specifies the maximum file chunk size.
 * @param path Specifies the path of the file being transferred.
 * @param fileDescriptor Specifies the FTMFileDescriptor for the file being transferred.
 */
-(void)getFileChunkAndQueueDataActionForFileID: (NSData *)fileID withStartByte: (int)startByte length: (int)length forPeer: (NSString *)peer withMaxChunkLength: (int)maxChunkLength path: (NSString *)path andFileDescriptor: (FTMFileDescriptor *)fileDescriptor;

/*
 * Private helper function that gets the next chunk of a file and returns the file
 * chunk as a byte array.
 *
 * @param path Specifies the path to the file being transferred.
 * @param startByte Specifies the starting position for the file data.
 * @param chunkLength Specifies the length of the file chunk.
 *
 * @return File chunk.
 */
-(NSData *)getChunkForFileWithPath: (NSString *)path startByte: (int)startByte andChunkLength: (int)chunkLength;

/*
 * Private helper function that creates and returns a FTMDataChunkAction action to be inserted 
 * into the FTMDispatcher.
 *
 * @param fileDescriptor Specifies the FTMFileDescriptor for the file being transfered.
 * @param peer Specifies the recipient of the file.
 * @param startByte Specifies the starting position of the data.
 * @param length Specifies the number of bytes being sent.
 * @param chunk Specifies the chunk of the file being sent.
 *
 * @return Instance of FTMDataChunkAction.
 */
-(FTMDataChunkAction *)createActionUsingDescriptor: (FTMFileDescriptor *)fileDescriptor startByte: (int)startByte length: (int)length andFileData: (NSData *)chunk forPeer: (NSString *)peer;

/*
 * Private helper function that creates a file status object so the sending progress of the file
 * can be monitored.
 *
 * @param fileDescriptor Specifies the FTMFileDescriptor for the file being transfered.
 * @param startByte Specifies the starting position for the file data.
 * @param length Specifies the length of the file.
 * @param peer Specifies the recipient of the file.
 * @param chunkLength Specifies the length of each file chunk.
 */
-(void)createFileStatusUsingDescriptor: (FTMFileDescriptor *)descriptor startByte: (int)startByte length: (int)length peer: (NSString *)peer andChunkLength: (int)chunkLength;

/*
 * Private helper function the inserts the next file chunk into the FTMDispatcher for transmission.
 *
 * @param sendingFile Instance of FTMFileStatus object matching the file being sent.
 */
-(void)queueNextChunkUsingFileStatus: (FTMFileStatus *)sendingFile;

/*
 * Private helper method that iterates over the sendingFiles list to find the file status object 
 * that matches the specified file ID. If a match is found, the file status object is deleted from 
 * the sendingFiles list and cancels that file transfer.
 *
 * @param fileID Specifies the ID of the file being cancelled.
 *
 * @return String Specifies the peer that is receiving the file.
 */
-(NSString *)deleteFileStatusForFileID: (NSData *)fileID;

/*
 * Private helper function that inserts a FTMXferCancelledAction into the FTMDispatcher. This 
 * will alert the receiver that the sender has cancelled the file transfer.
 *
 * @param fileID Specifies the ID of the file being cancelled.
 * @param peer Specifies the recipient of the FTMXferCancelledAction.
 */
-(void)queueCancelActionUsingFileID: (NSData *)fileID forPeer: (NSString *)peer;

@end

@implementation FTMSendManager

@synthesize sendingFiles = _sendingFiles;
@synthesize fsa = _fsa;
@synthesize dispatcher = _dispatcher;
@synthesize pm = _pm;
@synthesize requestDataReceivedDelegate = _requestDataReceivedDelegate;
@synthesize requestDataReceivedDelegateLock = _requestDataReceivedDelegateLock;

-(id)initWithDispatcher: (FTMDispatcher *)dispatcher andPermissionManager: (FTMPermissionManager *)permissionManager
{
    return [self initWithDispatcher: dispatcher fileSystemAbstraction: [FTMFileSystemAbstraction instance] andPermnissionManager: permissionManager];
}

-(id)initWithDispatcher: (FTMDispatcher *)dispatcher fileSystemAbstraction: (FTMFileSystemAbstraction *)fsa andPermnissionManager: (FTMPermissionManager *)permissionManager;
{
    self = [super init];
	
	if (self)
    {
        self.sendingFiles = [[NSMutableArray alloc] init];
		self.dispatcher = dispatcher;
        self.fsa = fsa;
        self.pm = permissionManager;
        self.requestDataReceivedDelegateLock = [[NSObject alloc] init];
        self.requestDataReceivedDelegate = nil;
	}
	
	return self;
}

-(FTMStatusCode)handleRequestForFileWithID: (NSData *)fileID withStartByte: (int)startByte length: (int)length fromPeer: (NSString *)peer andMaxChunkLength: (int)maxChunkLength
{
    return [self startSendingFileWithID: fileID withStartByte: startByte length: length toPeer: peer andMaxChunLength: maxChunkLength];
}

-(FTMStatusCode)startSendingFileWithID: (NSData *)fileID withStartByte: (int)startByte length: (int)length toPeer: (NSString *)peer andMaxChunLength: (int)maxChunkLength
{
    FTMFileDescriptor *fileDescriptor = [self.pm getLocalFileDescriptorForFileID: fileID];
    
    if (fileDescriptor != nil)
    {
        NSString *path = [self.fsa buildPathFromDescriptor: fileDescriptor];
        NSURL *pathURL = [[NSURL alloc] initWithString: path];
        
        if ([pathURL scheme] == nil)
        {
            [self getFileChunkAndQueueDataActionForFileID: fileID withStartByte: startByte length: length forPeer: peer withMaxChunkLength: maxChunkLength path: path andFileDescriptor: fileDescriptor];
            
            @synchronized (self.requestDataReceivedDelegateLock)
            {
                if (self.requestDataReceivedDelegate != nil)
                {
                    [self.requestDataReceivedDelegate fileRequestReceived: fileDescriptor.filename];
                }
            }
            return FTMOK;
        }
        else
        {
            //TODO - Add functionality to recognize different URL schemes and transfer files that are outside
            //your application sandbox. For example, if we save a URL to a photo that is stored in our photo
            //library, it will have a prefix, otherwise known as a URL scheme, of assets-library://.... If we
            //parse the scheme when someone requests the file and see that it is in our photo library we can
            //use the URL to get access to the bytes without having to transfer the file to out application
            //sandbox. This solution is extendible to other types of URLs with different schemes.
            
            return FTMInvalid; //should never get here. was put in to get rid of a compiler warning.
        }
    }
    else
    {
        return FTMBadFileID;
    }
}

-(void)getFileChunkAndQueueDataActionForFileID: (NSData *)fileID withStartByte: (int)startByte length: (int)length forPeer: (NSString *)peer withMaxChunkLength: (int)maxChunkLength path:(NSString *)path andFileDescriptor: (FTMFileDescriptor *)fileDescriptor
{
    if (length <= maxChunkLength)
    {
        NSData *chunk = [self getChunkForFileWithPath: path startByte: startByte andChunkLength: length];
        FTMDataChunkAction *action = [self createActionUsingDescriptor: fileDescriptor startByte: startByte length: length andFileData: chunk forPeer: peer];
        [self.dispatcher insertAction: action];
    }
    else
    {
        NSData *chunk = [self getChunkForFileWithPath: path startByte: startByte andChunkLength: maxChunkLength];
        FTMDataChunkAction *action = [self createActionUsingDescriptor: fileDescriptor startByte: startByte length: maxChunkLength andFileData: chunk forPeer: peer];
        [self createFileStatusUsingDescriptor:fileDescriptor startByte:startByte length:length peer:peer andChunkLength:maxChunkLength];
        [self.dispatcher insertAction: action];
        
    }
}

-(NSData *)getChunkForFileWithPath: (NSString *)path startByte: (int)startByte andChunkLength: (int)chunkLength
{
    NSInteger newStartByte = startByte;
    NSInteger newChunkLength = chunkLength;
    
    return [self.fsa getChunkOfFileWithPath:path startingOffset: newStartByte andLength: newChunkLength];
}

-(FTMDataChunkAction *)createActionUsingDescriptor: (FTMFileDescriptor *)fileDescriptor startByte: (int)startByte length: (int)length andFileData: (NSData *)chunk forPeer: (NSString *)peer
{
    FTMDataChunkAction *action = [[FTMDataChunkAction alloc] init];
    action.peer = peer;
    action.fileID = fileDescriptor.fileID;
    action.startByte = startByte;
    action.chunkLength = length;
    action.chunk = chunk;
    
    return action;
}

-(void)createFileStatusUsingDescriptor: (FTMFileDescriptor *)descriptor startByte: (int)startByte length: (int)length peer: (NSString *)peer andChunkLength: (int)chunkLength
{
    FTMFileStatus *fileStatus = [[FTMFileStatus alloc] init];
    fileStatus.fileID = descriptor.fileID;
    fileStatus.startByte = startByte;
    fileStatus.length = length;
    fileStatus.peer = peer;
    fileStatus.numBytesSent = chunkLength;
    fileStatus.chunkLength = chunkLength;
    
    @synchronized (self.sendingFiles)
    {
        [self.sendingFiles addObject: fileStatus];
    }
}

-(FTMStatusCode)sendFileWithID: (NSData *)fileID withStartByte: (int)startByte andLength: (int)length andMaxChunkLength: (int)maxChunkLength toPeer: (NSString *)peer
{
    return [self startSendingFileWithID: fileID withStartByte: startByte length: length toPeer: peer andMaxChunLength: maxChunkLength];
}

-(void)dataSent
{
    FTMFileStatus *sendingFile = nil;
    
    @synchronized (self.sendingFiles)
    {
        if ([self.sendingFiles count] > 0)
        {
            sendingFile = [self.sendingFiles objectAtIndex: 0];
        }
    }
    
    if (sendingFile != nil)
    {
        [self queueNextChunkUsingFileStatus: sendingFile];
    }
}

-(void)queueNextChunkUsingFileStatus: (FTMFileStatus *)sendingFile
{
    FTMFileDescriptor *fileDescriptor = [self.pm getLocalFileDescriptorForFileID: sendingFile.fileID];
    if (fileDescriptor != nil)
    {
        NSString *path = [self.fsa buildPathFromDescriptor: fileDescriptor];
        NSString *peer = [[NSString alloc] initWithString: sendingFile.peer];
        
        if ((sendingFile.length - sendingFile.numBytesSent) <= sendingFile.chunkLength)
        {
            int startByte = sendingFile.numBytesSent + sendingFile.startByte;
            int length = (sendingFile.length - sendingFile.numBytesSent);
            
            NSData *chunk = [self getChunkForFileWithPath:path startByte:startByte andChunkLength:length];
            FTMDataChunkAction *action = [self createActionUsingDescriptor: fileDescriptor startByte: startByte length: length andFileData: chunk forPeer: peer];
            [self deleteFileStatusForFileID:sendingFile.fileID];
            [self.dispatcher insertAction: action];		
        }
        else
        {
            int startByte = sendingFile.numBytesSent + sendingFile.startByte;
            NSData *chunk = [self getChunkForFileWithPath:path startByte:startByte andChunkLength:sendingFile.chunkLength];
            FTMDataChunkAction *action = [self createActionUsingDescriptor: fileDescriptor startByte: startByte length: sendingFile.chunkLength andFileData: chunk forPeer: peer];
            sendingFile.numBytesSent += sendingFile.chunkLength;
            [self.dispatcher insertAction: action];		
        }

    }
}

-(FTMStatusCode)cancelFileWithID: (NSData *)fileID
{
    NSString *peer = [self deleteFileStatusForFileID:fileID];
    
    if (peer != nil)
    {
        [self queueCancelActionUsingFileID:fileID forPeer:peer];
        return FTMOK;
    }
    else
    {
        return FTMFileNotBeingTransferred;
    }
}

-(void)handleStopDataXferForFileWithID: (NSData *)fileID fromPeer: (NSString *)peer
{
    [self deleteFileStatusForFileID:fileID];
}

-(NSString *)deleteFileStatusForFileID: (NSData *)fileID
{
    NSString *peer = nil;
    
    @synchronized(self.sendingFiles)
    {
        for (int i = 0; (i < self.sendingFiles.count) && (peer == nil ); i++)
        {
            FTMFileStatus *fileStatus = [self.sendingFiles objectAtIndex: i];
            
            if ([fileID isEqualToData:(fileStatus.fileID)])
            {
                peer = [[NSString alloc] initWithString: fileStatus.peer];
 
                [self.sendingFiles removeObjectAtIndex:i]; 
            }
        }
    }
    
    return peer;
}

-(void)queueCancelActionUsingFileID: (NSData *)fileID forPeer: (NSString *)peer
{
    FTMXferCancelledAction *action = [[FTMXferCancelledAction alloc] init];
    action.fileID = fileID;
    action.peer = peer;
    
    [self.dispatcher insertAction: action];
}

-(NSArray *)getProgressList
{
    NSMutableArray *progressList = [[NSMutableArray alloc] init];
    FTMProgressDescriptor *descriptor = [[FTMProgressDescriptor alloc] init];
    
    @synchronized (self.sendingFiles)
    {
        for (FTMFileStatus *fileStatus in self.sendingFiles)
        {
            descriptor = [[FTMProgressDescriptor alloc] init];
            descriptor.fileID = fileStatus.fileID;
            descriptor.fileSize = fileStatus.length;
            descriptor.bytesTransferred = fileStatus.numBytesSent;
            descriptor.state = IN_PROGRESS;
            
            [progressList addObject: descriptor];
        }
    }
    
    return progressList;
}

-(void)setRequestDataReceivedDelegate: (id<FTMRequestDataReceivedDelegate>)requestDataReceivedDelegate
{
    @synchronized (self.requestDataReceivedDelegateLock)
    {
        self->_requestDataReceivedDelegate = requestDataReceivedDelegate;
    }
}

-(void)resetState
{
    [self.sendingFiles removeAllObjects];
}

@end
