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

#import "FTMFileSystemAbstraction.h"

/*
 * The FileAttributes class is a private inner class that is used to store the file ID
 * and modification date of a given file. This class implements the NSCoding protocol
 * so the class variables can be properly encoded and decoded when reading or writing
 * from the cache file.
 */
@interface FileAttributes : NSObject <NSCoding>

/*
 * Stores the file ID (SHA-1 hash of a file).
 */
@property (nonatomic, strong) NSData *fileID;

/*
 * Stores the modification date of a file.
 */
@property (nonatomic, strong) NSDate *modificationDate;

/*
 * Creates an instance of the FileAttributes class.
 */
-(id)initWithFileID: (NSData *)fileID andModificationDate: (NSDate *)modificationDate;

/*
 * Encodes the file ID and modification date objects using the provided NSCoder object.
 *
 * @param encoder Instance of NSCoder.
 */
-(void)encodeWithCoder: (NSCoder *)encoder;

/*
 * Decodes the file ID and modification date objects using the provided NSCoder object and
 * initializes a new instance of the FileAttributes class.
 */
-(id)initWithCoder: (NSCoder *)decoder;

@end

@implementation FileAttributes

@synthesize fileID = _fileID;
@synthesize modificationDate = _modificationDate;

-(id)initWithFileID:(NSData *)fileID andModificationDate:(NSDate *)modificationDate
{
    self = [super init];
    
    if (self)
    {
        self.fileID = fileID;
        self.modificationDate = modificationDate;
    }
    
    return self;
}

-(void)encodeWithCoder: (NSCoder *)encoder
{
    [encoder encodeDataObject: self.fileID];
    [encoder encodeObject: self.modificationDate forKey: @"modificationDate"];
}

-(id)initWithCoder: (NSCoder *)decoder
{
    NSData *fileID = [decoder decodeDataObject];
    NSDate *modificationDate = [decoder decodeObjectForKey: @"modificationDate"];
    
    return [self initWithFileID: fileID andModificationDate: modificationDate];
}

@end

/*
 * Stores the FTMFileSystemAbstraction instance.
 *
 * @warning *Note:* This is a static variable and is not meant to be called directly.
 */
static FTMFileSystemAbstraction *instance;

/*
 * Stores the max file size.
 *
 * For iOS, the max file size if 2 GB since we use NSInteger values for file size in the
 * FTMFileDescriptor.
 *
 * @warning *Note:* This is a private constant and is not meant to be called directly.
 */
static const unsigned long long MAX_FILE_SIZE = 2147483647;

@interface FTMFileSystemAbstraction()

/*
 * Stores the absolute path to the cache file.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) NSString *attributeCacheFilePath;

/*
 * Stores the cached file IDs in an NSMutableDictionary.
 *
 * The absolute file path serves as the dictionary key and the FileAttributes object is the value.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) NSMutableDictionary *attributeCache; //key is the file path, value is FileAttributes object

/*
 * Private helper function that is used to write the current hash data to cache
 * file.
 *
 * @param path Specifies the file path to write the current hash value data.
 */
-(void)writeCacheToFile: (NSString *)path;

/*
 * Private helper function that is used to read the stored hash data (if
 * available) and store it in the attributeCache property. This function
 * is used by setCacheFile: when the user wishes to specify a cache file.
 *
 * @param path Specifies the path of the cache file.
 */
-(void)readCacheFromFile: (NSString *)path;

/*
 * Private helper function that builds the FTMFileDescriptor using the provided
 * data. If the operation fails, this function will return nil and the file path
 * will be added to the failed paths list.
 *
 * @param sharedPath Specifies the shared path of the file.
 * @param relativePath Speies the relative path of the file.
 * @param fileName Specifies the name of the file.
 * @param localBusID Specifies the local bus ID of the user.
 *
 * @return FTMFileDescriptor or nil if operation unsuccessful.
 */
-(FTMFileDescriptor *)buildDescriptorWithSharedPath: (NSString *)sharedPath relativePath: (NSString *)relativePath fileName: (NSString *)fileName andLocalBusID: (NSString *)localBusID;

/*
 * Private helper function that calculates the SHA-1 hash for the specified file.
 *
 * @param filePath Specifies the path to the file we need to hash.
 * @param fileSize Specifies the size of the file (in bytes).
 *
 * @return The SHA-1 hash of the specified file.
 */
-(NSData *)calculateFileIDUsingFilePath: (NSString *)filePath andFileSize: (NSInteger)fileSize;

@end

@implementation FTMFileSystemAbstraction

@synthesize attributeCacheFilePath = _attributeCacheFilePath;
@synthesize attributeCache = _attributeCache;

+(void)initialize
{
    static BOOL initialized = NO;
    
    if(!initialized)
    {
        initialized = YES;
        instance = [[FTMFileSystemAbstraction alloc] init];
    }
}

-(id)init
{
    self = [super init];
    
    if (self)
    {
        self.attributeCacheFilePath = nil;
        self.attributeCache = nil;
    }
    
    return self;
}

+(FTMFileSystemAbstraction *)instance
{
    return instance;
}

-(void)setCacheFileWithPath: (NSString *)path
{
    if (self.attributeCacheFilePath != nil)
    {
        if (![self.attributeCacheFilePath isEqualToString: path])
        {
            [self writeCacheToFile: self.attributeCacheFilePath];
        }
    }
    
    NSFileManager *fm = [NSFileManager defaultManager];
    
    if (![fm fileExistsAtPath: path])
    {
        [fm createFileAtPath: path contents: [[NSData alloc] init] attributes: nil];
    }
    
    BOOL isDirectory;
    if ((path != nil) && ([fm fileExistsAtPath: path isDirectory: &isDirectory]) && !isDirectory)
    {
        [self readCacheFromFile: path];
    }
    else
    {
        self.attributeCache = nil;
    }
    
    self.attributeCacheFilePath = path;
}

-(void)cleanCacheFile
{
    NSFileManager *fm = [NSFileManager defaultManager];
    
    if (self.attributeCache != nil)
    {
        NSArray *pathKeys = [self.attributeCache allKeys];
        
        for (NSString *path in pathKeys)
        {
            FileAttributes *currentValue = [self.attributeCache objectForKey: path];
            
            if (currentValue != nil)
            {
                NSDictionary *fileAttributes = [fm attributesOfItemAtPath: path error: nil];
                NSDate *fileModDate = [fileAttributes objectForKey: NSFileModificationDate];

                if (![fm fileExistsAtPath: path])
                {
                    [self.attributeCache removeObjectForKey: path];
                    continue;
                }
                else if ([fileModDate compare: currentValue.modificationDate] == NSOrderedDescending)
                {
                    [self.attributeCache removeObjectForKey: path];
                }
            }
        }
        
        [self writeCacheToFile: self.attributeCacheFilePath];
    }
}

-(void)writeCacheToFile: (NSString *)path
{
    NSMutableData *dictionaryData = [[NSMutableData alloc] init];
    NSKeyedArchiver *archiver = [[NSKeyedArchiver alloc] initForWritingWithMutableData: dictionaryData];
    [archiver encodeObject: self.attributeCache forKey: @"key"];
    [archiver finishEncoding];
    
    [dictionaryData writeToFile: path atomically:YES];
}

-(void)readCacheFromFile: (NSString *)path
{
    self.attributeCache = nil;
    
    NSFileManager *fm = [NSFileManager defaultManager];
    BOOL isDirectory;
    
    if (([fm fileExistsAtPath: path isDirectory: &isDirectory]) && !isDirectory && ([fm isReadableFileAtPath: path]))
    {
        NSData *fileData = [[NSMutableData alloc]initWithContentsOfFile: path];
        NSKeyedUnarchiver *unarchiver = [[NSKeyedUnarchiver alloc] initForReadingWithData: fileData];
        self.attributeCache = [unarchiver decodeObjectForKey: @"key"];
        [unarchiver finishDecoding];
        
        if (self.attributeCache == nil)
        {
            self.attributeCache = [[NSMutableDictionary alloc] init];
        }
    }
}

-(NSArray *)getFileInfo: (NSArray *)pathList withFailedPathsArray: (NSMutableArray *)failedPaths andLocalBusID: (NSString *)localBusID
{
    NSFileManager *fm = [NSFileManager defaultManager];
    NSMutableArray *fileList = [[NSMutableArray alloc] init];
    
    for (NSString *path in pathList)
    {
        if (![fm fileExistsAtPath: path])
        {
            [failedPaths addObject: path];
            continue;
        }
        
        FTMFileDescriptor *descriptor;
        BOOL isDir;
        if ([fm fileExistsAtPath: path isDirectory: &isDir] && isDir)
        {
            NSDirectoryEnumerator *directories = [fm enumeratorAtPath: path];
            
            for (NSString *pathInDirectory in directories)
            {
                if ([fm fileExistsAtPath: [path stringByAppendingPathComponent: pathInDirectory] isDirectory: &isDir] && !isDir)
                {
                    descriptor = [self buildDescriptorWithSharedPath: path relativePath: [pathInDirectory stringByDeletingLastPathComponent] fileName: [pathInDirectory lastPathComponent] andLocalBusID:localBusID];
                    
                    if (descriptor != nil)
                    {
                        [fileList addObject: [[FTMFileDescriptor alloc] initWithFileDescriptor: descriptor]];
                    }
                    else
                    {
                        [failedPaths addObject: path];
                    }
                }
            }
        }
        else
        {
            descriptor = [self buildDescriptorWithSharedPath: [path stringByDeletingLastPathComponent] relativePath: nil fileName: [path lastPathComponent] andLocalBusID:localBusID];
            
            if (descriptor != nil)
            {
                [fileList addObject: [[FTMFileDescriptor alloc] initWithFileDescriptor: descriptor]];
            }
            else
            {
                [failedPaths addObject: path];
            }
        }
    }
    
    return [fileList copy];
}

-(FTMFileDescriptor *)buildDescriptorWithSharedPath: (NSString *)sharedPath relativePath: (NSString *)relativePath fileName: (NSString *)fileName andLocalBusID: (NSString *)localBusID
{
    NSFileManager *fm = [NSFileManager defaultManager];
    NSString *fullPath = [[NSString alloc] initWithString: sharedPath];
    fullPath = [fullPath stringByAppendingPathComponent: relativePath];
    fullPath = [fullPath stringByAppendingPathComponent: fileName];
    
    FTMFileDescriptor *descriptor = nil;
    NSDictionary *fileAttributes;
    NSDate *lastModifiedDate;
    
    BOOL entryFound = NO;
    BOOL isDir;
    if ([fm fileExistsAtPath: fullPath isDirectory: &isDir] && !isDir && [fm isReadableFileAtPath: fullPath])
    {
        descriptor = [[FTMFileDescriptor alloc] init];
        descriptor.owner = localBusID;
        descriptor.sharedPath = sharedPath;
        descriptor.relativePath = (relativePath == nil) ? @"" : relativePath;
        descriptor.filename = fileName;
        
        fileAttributes = [fm attributesOfItemAtPath: fullPath error: nil];
        lastModifiedDate = [fileAttributes objectForKey: NSFileModificationDate];
        
        NSNumber *fileSizeNumber = [fileAttributes objectForKey: NSFileSize];
        unsigned long long fileSizeLongLong = [fileSizeNumber unsignedLongLongValue];
        
        if (fileSizeLongLong > MAX_FILE_SIZE)
        {
            return nil;
        }
        
        descriptor.size = [fileSizeNumber integerValue];
        
        FileAttributes *attributes = [self.attributeCache objectForKey: fullPath];
        
        if (attributes != nil && !([lastModifiedDate compare: attributes.modificationDate] == NSOrderedDescending))
        {
            descriptor.fileID = attributes.fileID;
            entryFound = YES;
        }
        else
        {
            descriptor.fileID = [self calculateFileIDUsingFilePath: fullPath andFileSize: descriptor.size];
        }
    }
    
    if ((descriptor != nil) && (self.attributeCacheFilePath != nil) && !entryFound)
    {
        FileAttributes *atts = [[FileAttributes alloc] initWithFileID: descriptor.fileID andModificationDate: lastModifiedDate];
        [self.attributeCache setObject: atts forKey: fullPath];
        [self writeCacheToFile: self.attributeCacheFilePath];
    }
    
    return descriptor;
}

-(NSData *)calculateFileIDUsingFilePath: (NSString *)filePath andFileSize: (NSInteger)fileSize
{
    NSFileHandle *handle = [NSFileHandle fileHandleForReadingAtPath: filePath];
    NSUInteger chunkSize = 100000;
    NSInteger startByte = 0;
    
    unsigned char digest[CC_SHA1_DIGEST_LENGTH];
    CC_SHA1_CTX ctx;
    CC_SHA1_Init(&ctx);
    {
        while (startByte < fileSize)
        {
            if ((startByte + chunkSize) > fileSize)
            {
                chunkSize = (fileSize - startByte);
            }
            
            [handle seekToFileOffset: startByte];
            NSData *fileData = [handle readDataOfLength: chunkSize];
            
            CC_SHA1_Update(&ctx, [fileData bytes], [fileData length]);
            
            startByte += chunkSize;
        }
    }
    
    CC_SHA1_Final(digest, &ctx);
    return [[NSData alloc] initWithBytes: digest length: 20];
}

-(NSData *)getChunkOfFileWithPath: (NSString *)path startingOffset: (NSInteger)startOffset andLength: (NSInteger)length
{
    NSFileHandle *file = [NSFileHandle fileHandleForReadingAtPath: path];
    [file seekToFileOffset: startOffset];
    return [file readDataOfLength: length];
}

-(void)addChunkOfFileWithPath: (NSString *)path withData: (NSData *)chunk startingOffset: (NSInteger)startOffset andLength: (NSInteger)length
{
    NSFileManager *fm = [NSFileManager defaultManager];
    
    if (![fm fileExistsAtPath: path])
    {
        NSString* parentDir = [path stringByDeletingLastPathComponent];
        if (![fm fileExistsAtPath: path])
        {
            [fm createDirectoryAtPath: parentDir withIntermediateDirectories:YES attributes: nil error: NULL];
        }        
        
        [fm createFileAtPath: path contents: [[NSData alloc] init] attributes: nil];
    }
    
    NSFileHandle *file = [NSFileHandle fileHandleForWritingAtPath: path];
    [file seekToFileOffset: startOffset];
    [file writeData: chunk];
}

-(int)deleteFileWithPath: (NSString *)path
{
    NSFileManager *fm = [NSFileManager defaultManager];
    
    BOOL successfulFileDelete = [fm removeItemAtPath: path error: nil];
    
    if (successfulFileDelete)
    {
        return 1;
    }
    else
    {
        return 0;
    }
}

-(NSString *)buildPathFromDescriptor: (FTMFileDescriptor *)fd
{
    NSString *path = [[NSString alloc] initWithString: fd.sharedPath];
    path = [path stringByAppendingPathComponent: fd.relativePath];
    path = [path stringByAppendingPathComponent: fd.filename];
    
    return path;
}

-(BOOL)isValidPath: (NSString *)path
{
    NSFileManager *fm = [NSFileManager defaultManager];
    BOOL isDirectory;
    
    if (([fm fileExistsAtPath: path isDirectory: &isDirectory]) &&
        ((!isDirectory && ([fm isReadableFileAtPath: path]) && ([fm isWritableFileAtPath: path]) ) ||
         isDirectory))
    {
        return YES;
    }
    else
    {
        return NO;
    }
}

@end
