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

#import "FTMPermissionManager.h"

@interface FTMPermissionManager()

/*
 * Stores an instance of the FTMFileSystemAbstraction.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMFileSystemAbstraction *fsa;

/*
 * Stores the files that have been announced to remote session peers.The key is the string version
 * of file ID and the value is a FTMFileDescriptor.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSMutableDictionary *announcedLocalFileList;

/*
 * Stores the files that have been offered to remote session peers.The key is the string version
 * of file ID and the value is a FTMFileDescriptor.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSMutableDictionary *offeredLocalFileList;

/*
 * Stores the files that have been announced to us by remote session peers.The key is the peer name
 * and the value is the array of FTMFileDescriptor objects they have announced. 
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSMutableDictionary *announcedRemoteFileList;

/*
 * Stores the files that have been offered to us by remote session peers.The key is the peer name
 * and the value is an array of FTMFileDescriptor objects.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSMutableDictionary *offeredRemoteFileList;

/*
 * Private helper function that adds all of the file descriptors stored in announced remote 
 * files dictionary to the provided array.
 *
 * @param allKnownFiles Specifies a list to add the file descriptors.
 */
-(void)addAnnouncedRemoteFiles: (NSMutableArray *)allKnownFiles;

/*
 * Private helper function that adds all of the file descriptors stored in offered remote
 * files dictionary to the provided array.
 *
 * @param allKnownFiles Specifies a list to add the file descriptors.
 */
-(void)addOfferedRemoteFiles: (NSMutableArray *)allKnownFiles;

@end

@implementation FTMPermissionManager

@synthesize announcedLocalFileList = _announcedLocalFileList;
@synthesize offeredLocalFileList = _offeredLocalFileList;
@synthesize announcedRemoteFileList = _announcedRemoteFileList;
@synthesize offeredRemoteFileList = _offeredRemoteFileList;
@synthesize fsa = _fsa;

-(id)init
{
    self = [super init];
	
	if (self)
    {
		self.announcedLocalFileList = [[NSMutableDictionary alloc] init];
        self.offeredLocalFileList = [[NSMutableDictionary alloc] init];
        self.announcedRemoteFileList = [[NSMutableDictionary alloc] init];
        self.offeredRemoteFileList = [[NSMutableDictionary alloc] init];
        self.fsa = [FTMFileSystemAbstraction instance];
	}
	
	return self;
}

-(void)addAnnouncedLocalFilesWithList: (NSArray *)descriptors
{    
    for (FTMFileDescriptor *descriptor in descriptors)
    {
        NSString *key = [NSString stringWithFormat: @"%@", descriptor.fileID];
        [self.announcedLocalFileList setObject: descriptor forKey: key];
    }
}

-(NSArray *)removeAnnouncedLocalFilesWithPaths: (NSArray *)paths
{
    NSMutableArray *failedPaths = [[NSMutableArray alloc] initWithArray: paths];
    NSArray *announcedLocalFiles = [self getAnnouncedLocalFiles];
    
    for (int i = 0; i < [announcedLocalFiles count]; i++)
    {
        FTMFileDescriptor *descriptor = [announcedLocalFiles objectAtIndex: i];
        NSString *filePath = [self.fsa buildPathFromDescriptor: descriptor];
        
        if ([paths containsObject: filePath])
        {
            NSString *key = [NSString stringWithFormat: @"%@", descriptor.fileID];
            [self.announcedLocalFileList removeObjectForKey: key];
            [failedPaths removeObject: filePath];
        }
    }
    
    return [[NSArray alloc] initWithArray: failedPaths];
}

-(void)updateAnnouncedRemoteFilesWithList: (NSArray *)descriptors fromPeer: (NSString *)peer
{
    [self.announcedRemoteFileList setObject: descriptors forKey: peer];
}

-(void)addOfferedLocalFileDescriptor: (FTMFileDescriptor *)descriptor
{
    NSString *key = [NSString stringWithFormat: @"%@", descriptor.fileID];
    [self.offeredLocalFileList setObject: descriptor forKey: key];
}

-(void)addOfferedRemoteFileDescriptor: (FTMFileDescriptor *)descriptor fromPeer: (NSString *)peer
{
    NSMutableArray *offeredRemoteFiles = [self.offeredRemoteFileList objectForKey: peer];
    
    if (offeredRemoteFiles == nil)
    {
        offeredRemoteFiles = [[NSMutableArray alloc] init];
        [self.offeredRemoteFileList setObject: offeredRemoteFiles forKey: peer];
    }
    
    [offeredRemoteFiles addObject: descriptor];
}

-(NSData *)getFileIDForFileWithPath: (NSString *)path ownedBy: (NSString *)peer
{
    NSArray *knownFiles = [self getAvailableRemoteFiles];
    
    for (FTMFileDescriptor *descriptor in knownFiles)
    {
        NSString *filePath = [self.fsa buildPathFromDescriptor: descriptor];
        
        if ([peer isEqualToString: descriptor.owner] && [filePath isEqualToString: path])
        {
            return descriptor.fileID;
        }
    }
    
    return nil;
}

-(NSArray *)getAnnouncedLocalFiles
{
    return [[NSArray alloc] initWithArray: [self.announcedLocalFileList allValues]];
}

-(NSArray *)getOfferedLocalFiles
{
    return [[NSArray alloc] initWithArray: [self.offeredLocalFileList allValues]];;
}

-(NSArray *)getAvailableRemoteFiles
{
    NSMutableArray *allknownFiles = [[NSMutableArray alloc] init];
    
    [self addAnnouncedRemoteFiles: allknownFiles];
    [self addOfferedRemoteFiles: allknownFiles];
    
    return [[NSArray alloc] initWithArray: allknownFiles];
}

-(void)addAnnouncedRemoteFiles: (NSMutableArray *)allKnownFiles
{
    for (NSArray *descriptorArray in [self.announcedRemoteFileList allValues])
    {
        for (FTMFileDescriptor *descriptor in descriptorArray)
        {
            if (![allKnownFiles containsObject: descriptor])
            {
                [allKnownFiles addObject: descriptor];
            }
        }
    }
}

-(void)addOfferedRemoteFiles: (NSMutableArray *)allKnownFiles
{
    for (NSMutableArray *descriptorArray in [self.offeredRemoteFileList allValues])
    {
        for (FTMFileDescriptor *descriptor in descriptorArray)
        {
            if (![allKnownFiles containsObject: descriptor])
            {
                [allKnownFiles addObject: descriptor];
            }
        }
    }
}

-(FTMFileDescriptor *)getLocalFileDescriptorForFileID: (NSData *)fileID
{
    NSString *key = [NSString stringWithFormat: @"%@", fileID];
    
    FTMFileDescriptor *descriptor;
    
    descriptor = [self.announcedLocalFileList objectForKey: key];
    
    if (descriptor == nil)
    {
        descriptor = [self.offeredLocalFileList objectForKey: key];
    }
    
    return descriptor;
}

-(BOOL)isAnnounced: (NSData *)fileID
{
    NSString *key = [NSString stringWithFormat: @"%@", fileID];
    
    if ([self.announcedLocalFileList objectForKey: key] != nil)
    {
        return YES;
    }
    else
    {
        return NO;
    }
}

-(BOOL)isShared: (NSData *)fileID
{
    NSString *key = [NSString stringWithFormat: @"%@", fileID];
    
    if ([self.offeredLocalFileList objectForKey: key] != nil)
    {
        return YES;
    }
    else
    {
        return NO;
    }
}

-(FTMFileDescriptor *)getKnownFileDescritorForFileID: (NSData *)fileID ownedBy: (NSString *)peer
{
    NSString *key = [NSString stringWithFormat: @"%@", fileID];
    
    NSArray *announcedRemoteFiles = [self.announcedRemoteFileList objectForKey: peer];
    
    if (announcedRemoteFiles != nil)
    {
        for (int i = 0; i < [announcedRemoteFiles count]; i++)
        {
            FTMFileDescriptor *descriptor1 = [announcedRemoteFiles objectAtIndex: i];
            NSString *key2 = [NSString stringWithFormat: @"%@", descriptor1.fileID];
            
            if ([key isEqualToString: key2])
            {
                return descriptor1;
            }
        }
    }
    
    NSArray *offeredRemoteFiles = [self.offeredRemoteFileList objectForKey: peer];
    
    if (offeredRemoteFiles != nil)
    {
        for (int i = 0; i < [offeredRemoteFiles count]; i++)
        {
            FTMFileDescriptor *descriptor1 = [offeredRemoteFiles objectAtIndex: i];
            NSString *key2 = [NSString stringWithFormat: @"%@", descriptor1.fileID];

            
            if ([key isEqualToString: key2])
            {
                return descriptor1;
            }
        }
    }
    
    return nil;
}

-(void)resetStateWithLocalBusID: (NSString *)localBusID
{
    for (FTMFileDescriptor *descriptor in [self.announcedLocalFileList allValues])
    {
        descriptor.owner = localBusID;
    }
    
    for (FTMFileDescriptor *descriptor in [self.offeredLocalFileList allValues])
    {
        descriptor.owner = localBusID;
    }
}

@end
