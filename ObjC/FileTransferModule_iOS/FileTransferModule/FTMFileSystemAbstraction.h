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

#import <Foundation/Foundation.h>
#import "FTMFileDescriptor.h"
#import <CommonCrypto/CommonDigest.h>

/**
 * The FTMFileSystemAbstraction (FSA) is one of the main modules of the FTMFileTransferModule
 * and plays an integral role in calculating the file ID for each file that needs to be
 * announced. The file ID is a 20 byte array that is determined by the SHA-1 hash of the file
 * contents. The FSA is also responsible returning a specified file chunk to the FTMSendManager.
 * Additionally, when a file chunk is received, it is passed to the FSA so the file can be 
 * reassembled. The main responsibility of the FSA is to hide the details of the local
 * file system. This class is implemented as a singleton since only one instance of this class
 * is needed but many modules must interact with it. The class method instance: returns the single 
 * instance of the FSA when needed.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMFileSystemAbstraction : NSObject

/** @name Creating FTMFileSystemAbstraction */

/**
 * Creates and initializes the FTMFileSystemAbstraction.
 *
 * This method is called automatically before any instance methods are called and ensures that
 * the static instance variable is properly created and initialized.
 *
 * @warning *Note:* This method should not be called directly and is present in the public API
 * because it is necessary for the compiler to properly initialize the FTMFileSystemAbstraction.
 */
+(void)initialize;

/**
 * Returns the single instance of the FTMFileSystemAbstraction.
 *
 * @return Instance of the FTMFileSystemAbstraction.
 */
+(FTMFileSystemAbstraction *)instance;

/** @name File Caching */

/**
 * Enables file ID caching using the specified file path.
 *
 * This function allows the user to specify the path for a file that will be used to store 
 * the hash value of files that are made available to AllJoyn session peers. Caching is helpful
 * to avoid recalculating the hash value of the same file multiple times, which for large files 
 * can be a time consuming operation. The user must call this function with a valid file path 
 * to enable caching.
 * 
 * @warning *Note:* Caching is disabled by default.
 * 
 * @warning *Note:* Calling this function to change the cache file causes any existing 
 * cached data to be written to the old file, and then the cache is replaced by the 
 * contents of the new file (if any are present in the new file).
 *
 * @warning *Note:* Passing in nil disables caching.
 *
 * @param path Specifies the absolute path of the file to be used for caching.
 */
-(void)setCacheFileWithPath: (NSString *)path;

/**
 * Purges the current cache file of outdated hash values.
 *
 * This function is called by the FTMFileTransferModule when the user wishes to clean the current
 * cache file. This function will iterate over the contents of the cache file and remove any hashes 
 * for files that no longer exist or have been modified since the last hash operation occurred.
 */
-(void)cleanCacheFile;

/** @name Build FTMFileDescriptor Array */

/**
 * Builds an FTMFileDescriptor for each path stored in the pathList array.
 *
 * The function will test to make sure each file path exists and has sufficient read permissions. If it 
 * does not have sufficient permissions or exist, that path will be added to the failedPaths array. 
 * Additionally, if one of the paths specifies a directory, this function will recursively get all files 
 * and sub-folder contents of the directory and create file descriptors for each file. This function
 * will return an array of file descriptors that specifies which files can be successfully
 * announced to session peers.
 *
 * @param pathList Array of absolute paths (files or directories) to be announced.
 * @param failedPaths Empty array to store failed file paths.
 * @param localBusID Specifies the bus ID of the local user.
 * 
 * @return Array of FTMFileDescriptor objects.
 */
-(NSArray *)getFileInfo: (NSArray *)pathList withFailedPathsArray: (NSMutableArray *)failedPaths andLocalBusID: (NSString *)localBusID;

/** @name File Operations */

/**
 * Reads a chunk of data from the specified file.
 *
 * This function opens the file at the specified file path and reads the number of bytes equal to the 
 * length parameter starting from the startOffset parameter. The bytes read from the file are stored 
 * in an array and returned to the caller.
 *
 * @param path Specifies the absolute file path of the file to read.
 * @param startOffset Specifies the starting byte offset of where to read the data.
 * @param length Specifies the number of bytes to read.
 *
 * @return Chunk read from the specified file.
 */
-(NSData *)getChunkOfFileWithPath: (NSString *)path startingOffset: (NSInteger)startOffset andLength: (NSInteger)length;

/**
 * Writes the provided data to the specified file.
 *
 * This function is called when a file chunk is received during a file transfer. This 
 * function is responsible for appending the new data to the file beginning from the startOffset 
 * parameter.
 *
 * @param  path Specifies the absolute file path of the file.
 * @param  chunk Specifies the byte array containing the data to be appended.
 * @param  startOffset Specifies the starting byte offset of where the data will be appended to the file.
 * @param  length Specifies the number of bytes to append from the starting offset.
 */
-(void)addChunkOfFileWithPath: (NSString *)path withData: (NSData *)chunk startingOffset: (NSInteger)startOffset andLength: (NSInteger)length;

/**
 * Deletes the file at the specified path, if it exists.
 *
 * This function will be called when the receiver of the file transfer decides to cancel the transfer.
 * If the delete operation is successful, the function returns one. If the delete operation fails, the 
 * function returns zero.
 *
 * @param path Specifies the absolute file path of the targeted file
 *
 * @return 1 if the file is successfully deleted, 0 otherwise.
 */
-(int)deleteFileWithPath: (NSString *)path;

/**
 * Tests that the provided path exists and has sufficient read/write permissions.
 *
 * @param path Specifies the absolute of a file or directory.
 *
 * @return True if the path is valid, false otherwise.
 **/
-(BOOL)isValidPath: (NSString *)path;

/** @name Reconstruct File Path */

/**
 * Reconstructs the absolute file path using the provided FTMFileDescriptor.
 *
 * @param fd Instance of FTMFileDescriptor.
 * 
 * @return Absolute file path built using the provided file descriptor.
 **/
-(NSString *)buildPathFromDescriptor: (FTMFileDescriptor *)fd;

@end
