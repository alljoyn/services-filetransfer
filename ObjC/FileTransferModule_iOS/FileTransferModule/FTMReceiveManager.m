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

#import "FTMReceiveManager.h"

@interface FTMReceiveManager()

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
 * Stores the FTMFileStatus objects to monitor the progress of all files being received. The
 * key is the string version of the file ID and the value is the corresponding FTMFileStatus
 * object.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSMutableDictionary *fileStatuses;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the fileCompletedDelegate.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSObject *fileCompletedDelegateLock;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the defaultSaveDirectory.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSObject *savePathLock;

/*
 * Private helper function that creates a FTMRequestDataAction using the provided FTMFileDescriptor
 * and FTMFileStatus objects.
 *
 * @param fd Instance of FTMFileDescriptor object.
 * @param status Instance of FTMFileStatus object.
 *
 * @return FTMRequestDataAction.
 */
-(FTMRequestDataAction *)buildDataRequestActionForFileDescriptor: (FTMFileDescriptor *)fd andFileStatus: (FTMFileStatus *)status;

/*
 * Private helper function that builds the FTMFileStatus object for the requested file. The file
 * status object provides the details to monitor the progress of the file transfer.
 *
 * @param descriptor Instance of FTMFileDescriptor for requested file.
 * @param saveFileName Specifies the name to save the file.
 * @param saveFileDirectory Specifies the location to save the file.
 *
 * @return  FTMFileStatus object.
 */
-(FTMFileStatus *)buildStatusWithDescriptor: (FTMFileDescriptor *)descriptor saveFileName: (NSString *)saveFileName andSaveFileDirectory: (NSString *)saveFileDirectory;

/*
 * Private helper function that builds a FTMStopXferAction to be sent to the file sender. 
 * This will notify the sender that the receiver wishes to pause the transfer.
 *
 * @warning *Note:* All temporary files are saved in memory so the file transfer can be resumed
 * at a later time.
 *
 * @param fileID Specifies the ID of the file the receiver wishes to pause.
 * @param status Instance of FTMFileStatus object.
 *
 * @return FTMStopXferAction.
 */
-(FTMStopXferAction *)buildStopXferActionWithFileID: (NSData *)fileID andFileStatus: (FTMFileStatus *)status;

/*
 * Retrieves the FTMFileStatus object that matches the specified file ID.
 *
 * @param fileID Specifies the file ID of the FileStatus object.
 *
 * @return  FTMFileStatus object or nil if the object does not exist.
 */
-(FTMFileStatus *)getFileStatusForFileWithID: (NSString *)fileID;

/*
 * Private helper function that safely triggers the FTMFileCompletedDelegate to notify the user that a file
 * transfer operation has been completed.
 *
 * @param filename Name of file that completed transfer.
 * @param statusCode Status of file transfer completion.
 */
-(void)fireCompletedDelegateForFileWithName: (NSString *)fileName andStatusCode: (FTMStatusCode)statusCode;

@end

@implementation FTMReceiveManager

@synthesize fileCompletedDelegate = _fileCompletedDelegate;
@synthesize defaultSaveDirectory = _defaultSaveDirectory;
@synthesize maxChunkSize = _maxChunkSize;
@synthesize dispatcher = _dispatcher;
@synthesize fsa = _fsa;
@synthesize pm = _pm;
@synthesize fileStatuses = _fileStatuses;
@synthesize fileCompletedDelegateLock = _fileCompletedDelegateLock;
@synthesize savePathLock = _savePathLock;

-(id)initWithDispatcher: (FTMDispatcher *)dispatcher andPermissionManager: (FTMPermissionManager *)permissionManager
{
    return [self initWithDispatcher: dispatcher fileSystemAbstraction: [FTMFileSystemAbstraction instance] andPermissionManager: permissionManager];
}

-(id)initWithDispatcher:(FTMDispatcher *)dispatcher fileSystemAbstraction: (FTMFileSystemAbstraction *)fsa andPermissionManager:(FTMPermissionManager *)permissionManager
{
    self = [super init];
	
	if (self)
    {
		self.dispatcher = dispatcher;
        self.pm = permissionManager;
        self.fsa = fsa;
        self.fileStatuses = [[NSMutableDictionary alloc] init];
        self.fileCompletedDelegateLock = [[NSObject alloc] init];
        self.savePathLock = [[NSObject alloc] init];
        self.maxChunkSize = 1024;
        
        NSString *documentsDirectory = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex: 0];
        
        // Ensure documents directory exists
        NSFileManager *filemgr = [[NSFileManager alloc]init];
        NSError *errorw;
        [filemgr createDirectoryAtPath:documentsDirectory withIntermediateDirectories:NO attributes:nil error:&errorw];
        self.defaultSaveDirectory = [[NSString alloc] initWithString: documentsDirectory];
	}
	
	return self;
}

-(void)setDefaultSaveDirectory: (NSString *)defaultSaveDirectory
{    
    if ([self.fsa isValidPath: defaultSaveDirectory])
    {
        @synchronized(self.savePathLock)
        {
            self->_defaultSaveDirectory = [[NSString alloc] initWithString: defaultSaveDirectory];
        }
    }
}

-(void)setFileCompletedDelegate: (id<FTMFileCompletedDelegate>)fileCompletedDelegate
{
    @synchronized(self.fileCompletedDelegateLock)
    {
        self->_fileCompletedDelegate = fileCompletedDelegate;
    }
}

-(void)setMaxChunkSize: (int)chunkSize
{
    if (chunkSize > 0)
    {
        self->_maxChunkSize = chunkSize;
    }
}

-(NSArray *)getProgressList
{
    NSMutableArray *progressList = [[NSMutableArray alloc] init];
    
    @synchronized (self.fileStatuses)
    {
        for(id key in self.fileStatuses)
        {
            FTMFileStatus *fileStatus = [self.fileStatuses objectForKey:key];
            FTMProgressDescriptor *descriptor = [[FTMProgressDescriptor alloc] init];
       
            descriptor.fileID = fileStatus.fileID;
            descriptor.fileSize = fileStatus.length;
            descriptor.bytesTransferred = fileStatus.numBytesSent;
            descriptor.state = IN_PROGRESS;
            
            [progressList addObject: descriptor];
        }
    }
    
    return progressList;
}

-(FTMStatusCode)requestFileOwnedBy: (NSString *)peer withFileID: (NSData *)fileID saveFileName: (NSString *)saveFileName andSaveFileDirectory: (NSString *)saveFileDirectory
{
    
    FTMFileDescriptor *fileDescriptor = [self.pm getKnownFileDescritorForFileID:fileID ownedBy:peer];
    if (fileDescriptor == nil)
    {
        return FTMBadFileID;
    }
    
    return [self initiateRequestForFile:fileDescriptor usingSaveFileName:saveFileName andSaveDirectory:saveFileDirectory throughDispatcher:NO];
}

-(int)initiateRequestForFile: (FTMFileDescriptor *)file usingSaveFileName: (NSString *)saveFileName andSaveDirectory: (NSString *)saveDirectory throughDispatcher: (BOOL)useDispatcher
{
    //determine root save directory
    if (saveDirectory == nil)
    {
        @synchronized(self.savePathLock)
        {
            saveDirectory = self.defaultSaveDirectory;
        }
    }
    
    //check directory valid
    if (![self.fsa isValidPath: saveDirectory])
    {
        return FTMBadFilePath;
    }
    
    //get FileStatus to handle receiving requested file
    NSString *key = [NSString stringWithFormat: @"%@", file.fileID];
    FTMFileStatus *status = [self getFileStatusForFileWithID: key];
    
    if (status == nil)
    {
        status = [self buildStatusWithDescriptor:file saveFileName:saveFileName andSaveFileDirectory:saveDirectory];
        
        @synchronized(self.fileStatuses)
        {
            NSString *key = [NSString stringWithFormat: @"%@", file.fileID];
            [self.fileStatuses setObject:status forKey: key];
        }
    }
    
    //request file
    FTMAction *action = [self buildDataRequestActionForFileDescriptor:file andFileStatus:status];
    
    if (useDispatcher)
    {
        [self.dispatcher insertAction:action];
        
        return FTMOK;
    }
    else
    {				
        return [self.dispatcher transmitImmediately:action];
    }
}

-(FTMRequestDataAction *)buildDataRequestActionForFileDescriptor: (FTMFileDescriptor *)fd andFileStatus: (FTMFileStatus *)status
{
    FTMRequestDataAction *action = [[FTMRequestDataAction alloc] init];
    
    action.fileID = fd.fileID;
    action.startByte = status.numBytesSent;
    action.length = fd.size;
    action.maxChunkSize = self.maxChunkSize;
    action.peer = fd.owner;
    
    return action;
}

-(FTMFileStatus *)buildStatusWithDescriptor: (FTMFileDescriptor *)descriptor saveFileName: (NSString *)saveFileName andSaveFileDirectory: (NSString *)saveFileDirectory
{
    FTMFileStatus *status = [[FTMFileStatus alloc] init];
    
    status.fileID = descriptor.fileID;
    status.startByte = 0;
    status.length = descriptor.size;
    status.peer = descriptor.owner;
    status.numBytesSent = 0;
    status.saveFileName = saveFileName;
    
    NSString *saveFilePath = [[NSString alloc] initWithString: saveFileDirectory];
    saveFilePath = [saveFilePath stringByAppendingPathComponent: descriptor.relativePath];
    status.saveFilePath = saveFilePath;
    
    return status;
}

-(void)handleChunkForFile: (NSData *)file withStartByte: (int)startByte andLength: (int)length andFileData: (NSData *)chunk
{
    NSString *key = [NSString stringWithFormat: @"%@", file];
    FTMFileStatus *status = [self getFileStatusForFileWithID : key];
    
    if (status == nil)
    {
        NSLog(@"handleChunkForFile nil FTCFileStatus");
    }
    else
    {
        NSString *path = [[NSString alloc] initWithString: status.saveFilePath];
        path = [path stringByAppendingPathComponent: status.saveFileName];

        // be defensive against rogue sender
        if (([chunk length] > self.maxChunkSize) ||
            (length > self.maxChunkSize))
        {
            NSLog(@"too large file chunk received");
            [self.fsa deleteFileWithPath:path];     // toss the file
            
            @synchronized(self.fileStatuses)
            {
                [self.fileStatuses removeObjectForKey: key];
            }
            [self fireCompletedDelegateForFileWithName:status.saveFileName andStatusCode: FTMInvalid];
        }
        else
        {
            if (startByte < status.numBytesSent)
            {
                NSLog(@"out of order file chunk received");
            }
            else
            {
                //TODO  Consider this design modification...
                // Currently we do not verify the received file against what we are expecting
                // by file content, only by total file size.  To change this, we could
                // hash each chunk as it comes in, then compare the final hash of the received file
                // to the FTCFileDescriptor fileID. If they are different, we either have received
                // the wrong number of total bytes, or the wrong contents of the file. In either
                // case, we would call fsa delete file, below, instead of addchunk.
            
                if (status.numBytesSent <= status.length)
                {            
                    [self.fsa addChunkOfFileWithPath:path withData:chunk startingOffset:startByte andLength:length];
                }
                else
                {
                    // we have received more bytes than we were expecting. Toss the file.
                    [self.fsa deleteFileWithPath:path];
                    NSLog(@"received too many bytes for %@", path);
                }
                
                status.numBytesSent += length;
        
                if (status.numBytesSent >= status.length)
                {
                    @synchronized(self.fileStatuses)
                    {
                        [self.fileStatuses removeObjectForKey: key ];
                    }
                
                    if (status.numBytesSent == status.length)
                    {
                        NSLog(@"transfer completed");
           
                        [self fireCompletedDelegateForFileWithName:status.saveFileName andStatusCode:FTMOK];
                    }
                    else
                    {
                        [self fireCompletedDelegateForFileWithName:status.saveFileName andStatusCode:FTMInvalid];
                    }
                }
            }
        }
    }
}

-(void)handleDataXferCancelledFrom: (NSString *)peer forFileWithID: (NSData *)fileID
{
    NSString *key = [NSString stringWithFormat: @"%@", fileID];
    FTMFileStatus *status = [self getFileStatusForFileWithID : key];
    
    if (status != nil)
    {       
        NSString *filename = status.saveFileName;
        [self.fileStatuses removeObjectForKey: key];
        [self fireCompletedDelegateForFileWithName:filename andStatusCode:FTMCancelled];
    }
}

-(FTMStatusCode)pauseFileWithID: (NSData *)fileID
{
    NSString *key = [NSString stringWithFormat: @"%@", fileID];
    FTMFileStatus *status = [self getFileStatusForFileWithID : key];
    
    if (status == nil)
    {
        return FTMBadFileID;
    }
    
    FTMAction *action = [self buildStopXferActionWithFileID: fileID andFileStatus:status];
    
    [self.dispatcher insertAction:action];
    
    @synchronized(self.fileStatuses)
    {
        [self.fileStatuses removeObjectForKey:key];
    }
    
    return FTMOK;
}

-(FTMStopXferAction *)buildStopXferActionWithFileID: (NSData *)fileID andFileStatus: (FTMFileStatus *)status
{
    FTMStopXferAction *action = [[FTMStopXferAction alloc] init];
    
    action.fileID = fileID;
    action.peer = status.peer;
    
    return action;

}

-(FTMStatusCode)cancelFileWithID: (NSData *)fileID
{
    NSString *key = [NSString stringWithFormat: @"%@", fileID];
    FTMFileStatus *status = [self getFileStatusForFileWithID : key];
    
    int statusCode = [self pauseFileWithID:fileID];
    
    if (statusCode == FTMBadFileID)
    {
        return FTMBadFileID;
    }
    
    NSString *path = [[NSString alloc] initWithString: status.saveFilePath];
    path = [path stringByAppendingPathComponent: status.saveFileName];

    [self.fsa deleteFileWithPath:path];
    
    return FTMOK;
}

-(FTMFileStatus *)getFileStatusForFileWithID: (NSString *)fileID
{
    @synchronized(self.fileStatuses)
    {
        return [self.fileStatuses objectForKey: fileID];
    }
}

-(void)fireCompletedDelegateForFileWithName: (NSString *)fileName andStatusCode: (FTMStatusCode)statusCode
{
    @synchronized (self.fileCompletedDelegateLock)
    {
        if (self.fileCompletedDelegate != nil)
        {
            [self.fileCompletedDelegate fileCompletedForFile:fileName withStatusCode:statusCode];
        }
    }
}

-(void)resetState
{
    [self.fileStatuses removeAllObjects];
}

@end