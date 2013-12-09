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

#import "FTMPermissionsManagerTests.h"

@interface FTMPermissionsManagerTests()

@property (nonatomic, strong) FTMPermissionManager *permissionManager;

-(NSArray *)generateDescriptorArrayUsingOwner: (NSString *)owner;

@end

@implementation FTMPermissionsManagerTests

@synthesize permissionManager = _permissionManager;

- (void)setUp
{
    [super setUp];
    
    self.permissionManager = [[FTMPermissionManager alloc] init];
}

- (void)tearDown
{
    self.permissionManager = nil;
    
    [super tearDown];
}

-(void)testAddAnnouncedLocalFiles
{
    NSArray *knownDescriptors = [self generateDescriptorArrayUsingOwner: @"foo"];
    
    [self.permissionManager addAnnouncedLocalFilesWithList: knownDescriptors];
    
    NSArray *files = [self.permissionManager getAnnouncedLocalFiles];
    STAssertTrue([files count] == 6, @"Number of descriptors is equal to 6.");

    for (FTMFileDescriptor *descriptor in knownDescriptors)
    {
        STAssertTrue([files containsObject: descriptor], @"Files contains the file descriptor");
    }
}

-(void)testRemoveAnnouncedLocalFiles
{
    [self testAddAnnouncedLocalFiles];
    
    NSArray *path = [[NSArray alloc] initWithObjects: @"/Documents/Photos/house.png", @"/Documents/Reports/inventors.txt", @"not here 1", @"not here 2", nil];
    
    NSArray *failedPaths = [self.permissionManager removeAnnouncedLocalFilesWithPaths: path];
    NSArray *knownDescriptors = [self generateDescriptorArrayUsingOwner: @"foo"];
    NSArray *announcedFiles = [self.permissionManager getAnnouncedLocalFiles];
    STAssertTrue([failedPaths count] == 2, @"Failed Paths array size is 2");
    STAssertTrue([announcedFiles count] == 4, @"Announced local files was reduced to a size of 4");
    STAssertFalse([announcedFiles containsObject: [knownDescriptors objectAtIndex: 0]], @"Index 0 descriptor is not announced");
    STAssertTrue([announcedFiles containsObject: [knownDescriptors objectAtIndex: 1]], @"Index 1 descriptor is announced");
    STAssertTrue([announcedFiles containsObject: [knownDescriptors objectAtIndex: 2]], @"Index 2 descriptor is announced");
    STAssertTrue([announcedFiles containsObject: [knownDescriptors objectAtIndex: 3]], @"Index 3 descriptor is announced");
    STAssertFalse([announcedFiles containsObject: [knownDescriptors objectAtIndex: 4]], @"Index 4 descriptor is not announced");
    STAssertTrue([announcedFiles containsObject: [knownDescriptors objectAtIndex: 5]], @"Index 5 descriptor is announced");
}

-(void)testUpdateAnnouncedRemoteFiles
{
    NSArray *files = [self generateDescriptorArrayUsingOwner: @"foo"];
    
    [self.permissionManager updateAnnouncedRemoteFilesWithList: files fromPeer: @"foo"];
    [self.permissionManager updateAnnouncedRemoteFilesWithList: files fromPeer: @"foo"];
    
    NSArray *remoteFiles = [self.permissionManager getAvailableRemoteFiles];
    STAssertTrue([remoteFiles count] == 6, @"Available remote file list contains 6 objects");
    
    for (FTMFileDescriptor *descriptor in files)
    {
        STAssertTrue([remoteFiles containsObject: descriptor], @"Remote files array contains the descriptor");
    }
}

-(void)testAddOfferedLocalFileDescriptor
{
    NSArray *files = [self generateDescriptorArrayUsingOwner: @"foo"];
    
    FTMFileDescriptor *descriptor1 = [files objectAtIndex: 0];
    FTMFileDescriptor *descriptor2 = [files objectAtIndex: 5];
    
    [self.permissionManager addOfferedLocalFileDescriptor: descriptor1];
    [self.permissionManager addOfferedLocalFileDescriptor: descriptor2];
    
    NSArray *offeredFiles = [self.permissionManager getOfferedLocalFiles];
    STAssertTrue([offeredFiles count] == 2, @"2 Files contained in the local offered files list");
    
    STAssertTrue([offeredFiles containsObject: descriptor1], @"Descriptor 1 is contained in offered files");
    STAssertTrue([offeredFiles containsObject: descriptor2], @"Descriptor 2 is contained in offered files");
}
-(void)testAddOfferedRemoteFileDescriptor
{
    [self testUpdateAnnouncedRemoteFiles];
    
    FTMFileDescriptor *descriptor1 = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes10[] = { 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10 };
    NSData *fileID10 = [[NSData alloc] initWithBytes: bytes10 length: 20];
    descriptor1.fileID = fileID10;
    descriptor1.filename = @"test10.pdf";
    descriptor1.owner = @"James";
    descriptor1.relativePath = @"";
    descriptor1.sharedPath = @"/Documents";
    descriptor1.size = 100;
    
    [self.permissionManager addOfferedRemoteFileDescriptor: descriptor1 fromPeer: @"James"];
    
    FTMFileDescriptor *descriptor2 = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes11[] = { 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11 };
    NSData *fileID11 = [[NSData alloc] initWithBytes: bytes11 length: 20];
    descriptor2.fileID = fileID11;
    descriptor2.filename = @"test11.pdf";
    descriptor2.owner = @"James";
    descriptor2.relativePath = @"";
    descriptor2.sharedPath = @"/Documents";
    descriptor2.size = 100;
    
    [self.permissionManager addOfferedRemoteFileDescriptor: descriptor2 fromPeer: @"James"];
    
    FTMFileDescriptor *descriptor3 = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes12[] = { 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12 };
    NSData *fileID12 = [[NSData alloc] initWithBytes: bytes12 length: 20];
    descriptor3.fileID = fileID12;
    descriptor3.filename = @"test12.pdf";
    descriptor3.owner = @"John";
    descriptor3.relativePath = @"";
    descriptor3.sharedPath = @"/Documents";
    descriptor3.size = 100;
    
    [self.permissionManager addOfferedRemoteFileDescriptor: descriptor3 fromPeer: @"John"];
    
    NSArray *remoteFiles = [self.permissionManager getAvailableRemoteFiles];
    STAssertTrue([remoteFiles count] == 9, @"Remote files contains 9 descriptors");
    
    STAssertTrue([remoteFiles containsObject: descriptor1], @"Descriptor 1 is contained in remote files");
    STAssertTrue([remoteFiles containsObject: descriptor2], @"Descriptor 2 is contained in remote files");
    STAssertTrue([remoteFiles containsObject: descriptor3], @"Descriptor 3 is contained in remote files");
}

-(void)testGetFileID
{
    [self testUpdateAnnouncedRemoteFiles];
    
    NSData *fileID = [self.permissionManager getFileIDForFileWithPath: @"/Documents/Photos/house.png" ownedBy: @"foo"];
    STAssertNotNil(fileID, @"FileID is not nil");
    
    fileID = [self.permissionManager getFileIDForFileWithPath: @"not_here" ownedBy: @"foo"];
    STAssertNil(fileID, @"FileID is nil");
}

-(void)testGetLocalFileDescriptor1
{
    NSArray *fileDescriptors = [self generateDescriptorArrayUsingOwner: @"foo"];
    [self.permissionManager addAnnouncedLocalFilesWithList: fileDescriptors];
    
    FTMFileDescriptor *descriptor = [self.permissionManager getLocalFileDescriptorForFileID: [[fileDescriptors objectAtIndex: 3] fileID]];
    STAssertNotNil(descriptor, @"Local File descriptor found in announced file list");
    descriptor = [self.permissionManager getLocalFileDescriptorForFileID: [[NSData alloc] init]];
    STAssertNil(descriptor, @"Local file descriptor not found in announced file list");
}

-(void)testGetLocalFileDescriptor2;
{
    [self testAddOfferedLocalFileDescriptor];
    
    STAssertNotNil([self.permissionManager getLocalFileDescriptorForFileID: [[[self.permissionManager getOfferedLocalFiles] objectAtIndex: 1] fileID]], @"Local file descriptor found in offered file list");
    STAssertNil([self.permissionManager getLocalFileDescriptorForFileID: [[NSData alloc] init]], @"Local file descriptor not found in offered file list");
}

-(void)testGetKnownFileDescritor
{
    [self testAddOfferedRemoteFileDescriptor];
    
    const unsigned char bytes[] = { 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11 };
    NSData *fileID = [[NSData alloc] initWithBytes: bytes length: 20];
    NSString *owner = @"James";
    
    STAssertNotNil([self.permissionManager getKnownFileDescritorForFileID: fileID ownedBy: owner], @"Remote file descriptor found");
    
    owner = @"John";
    
    STAssertNil([self.permissionManager getKnownFileDescritorForFileID: fileID ownedBy: owner], @"Remote file descriptor not found");
    
    const unsigned char bytes2[] = { 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05 };
    fileID = [[NSData alloc] initWithBytes: bytes2 length: 20];
    owner = @"foo";
    
    STAssertNotNil([self.permissionManager getKnownFileDescritorForFileID: fileID ownedBy: owner], @"Remote file descriptor found");
    
    owner = @"bar";
    
    STAssertNil([self.permissionManager getKnownFileDescritorForFileID: fileID ownedBy: owner], @"Remote file descriptor not found");

}

-(NSArray *)generateDescriptorArrayUsingOwner: (NSString *)owner
{
    NSMutableArray *fileList = [[NSMutableArray alloc] init];
    
    FTMFileDescriptor *descriptor = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes1[] = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
    NSData *fileID = [[NSData alloc] initWithBytes: bytes1 length: 20];
    descriptor.fileID = fileID;
    descriptor.filename = @"house.png";
    descriptor.owner = owner;
    descriptor.relativePath = @"";
    descriptor.sharedPath = @"/Documents/Photos";
    descriptor.size = 100;
    [fileList addObject: descriptor];
    
    descriptor = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes2[] = { 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02 };
    fileID = [[NSData alloc] initWithBytes: bytes2 length: 20];
    descriptor.fileID = fileID;
    descriptor.filename = @"backyard.png";
    descriptor.owner = owner;
    descriptor.relativePath = @"";
    descriptor.sharedPath = @"/Documents/Photos";
    descriptor.size = 100;
    [fileList addObject: descriptor];
    
    descriptor = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes3[] = { 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03 };
    fileID = [[NSData alloc] initWithBytes: bytes3 length: 20];
    descriptor.fileID = fileID;
    descriptor.filename = @"fireplace.png";
    descriptor.owner = owner;
    descriptor.relativePath = @"";
    descriptor.sharedPath = @"/Documents/Photos";
    descriptor.size = 100;
    [fileList addObject: descriptor];
    
    descriptor = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes4[] = { 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04 };
    fileID = [[NSData alloc] initWithBytes: bytes4 length: 20];
    descriptor.fileID = fileID;
    descriptor.filename = @"animals.txt";
    descriptor.owner = owner;
    descriptor.relativePath = @"";
    descriptor.sharedPath = @"/Documents/Reports";
    descriptor.size = 100;
    [fileList addObject: descriptor];
    
    descriptor = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes5[] = { 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05 };
    fileID = [[NSData alloc] initWithBytes: bytes5 length: 20];
    descriptor.fileID = fileID;
    descriptor.filename = @"inventors.txt";
    descriptor.owner = owner;
    descriptor.relativePath = @"";
    descriptor.sharedPath = @"/Documents/Reports";
    descriptor.size = 100;
    [fileList addObject: descriptor];

    
    descriptor = [[FTMFileDescriptor alloc] init];
    const unsigned char bytes6[] = { 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06 };
    fileID = [[NSData alloc] initWithBytes: bytes6 length: 20];
    descriptor.fileID = fileID;
    descriptor.filename = @"driving.txt";
    descriptor.owner = owner;
    descriptor.relativePath = @"";
    descriptor.sharedPath = @"/Documents/Reports";
    descriptor.size = 100;
    [fileList addObject: descriptor];

    return [[NSArray alloc] initWithArray: fileList];
}

@end
