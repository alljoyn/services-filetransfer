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
#import "FTMFileSystemAbstraction.h"

/**
 * The FTMPermissionManager is responsible for storing all of the files we have made
 * available to remote session peers through formal announcements or informal offers.
 * The FTMPermissionManager is also responsible for storing all of the files that have
 * been announced or offered to us by remote session peers. In addition to storing
 * files, this class is responsible for performing lookups when provided a specific file 
 * ID and returning lists of the files stored in each of the dictionaries.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMPermissionManager : NSObject

/** @name Creating FTMPermissionManager */

/**
 * Constructs an instance of the FTMPermissionManager.
 *
 * @return Instance of FTMPermissionManager.
 */
-(id)init;

/** @name Storing File Data */

/**
 * Add the specified descriptor array to the announced files list.
 *
 * This function stores each FTMFileDescriptor in the announced files dictionary
 * where the key is the file ID of each file and the value is the FTMFileDescriptor.
 *
 * @param descriptors Array of announced files.
 */
-(void) addAnnouncedLocalFilesWithList: (NSArray *)descriptors;

/**
 * Removes the specified files from the announced files dictionary.
 *
 * This function takes an array of paths that specify which files need to be unannounced
 * and searches the announced local files list for matches. When matches are found they are 
 * removed from the announced files list.
 *
 * @param paths  Array of paths to be unannounced.
 *
 * @return Array of paths that failed to unannounce.
 */
-(NSArray *)removeAnnouncedLocalFilesWithPaths: (NSArray *)paths;

/**
 * Adds the array of FTMFileDescriptors to the announced remote files dictionary.
 *
 * This function is called when we receive announcements from remote session peers. The 
 * array of FTMFileDescriptor objects is stored in a dictionary containing all of the available 
 * remote files organized with the peer name as the key.
 *
 * @param descriptors Array of available remote files.
 * @param peer Specifies the peer who sent the array of files.
 */
-(void)updateAnnouncedRemoteFilesWithList: (NSArray *)descriptors fromPeer: (NSString *)peer;

/**
 * Adds the specified FTMFileDescriptor to the offered local files dictionary.
 *
 * The offered local files dictionary contains records of all the files that we have offered
 * to remote session peers.
 *
 * @param descriptor Specifies the FTMFileDescriptor to be stored.
 */
-(void)addOfferedLocalFileDescriptor: (FTMFileDescriptor *)descriptor;

/**
 * Adds the specified FTMFileDescriptor to the offered remote files dictionary.
 *
 * The offered remote files dictionary contains the records of files that we have either
 * directly requested or been offered by a remote session peer.
 *
 * @param descriptor Specifies the FTMFileDescriptor of the remote file.
 * @param peer Specifies the owner of the file.
 */
-(void)addOfferedRemoteFileDescriptor: (FTMFileDescriptor *)descriptor fromPeer: (NSString *)peer;

/** @name Retrieving File Data */

/**
 * Searches the list of remote files for a file that matches the provide peer and file path.
 *
 * @param path Specifies the absolute path of the file.
 * @param peer Specifies the owner of the file.
 *
 * @return File ID if a match is found, nil otherwise.
 */
-(NSData *)getFileIDForFileWithPath: (NSString *)path ownedBy: (NSString *)peer;

/**
 * Returns an array of FTMFileDescriptor objects that describes all of the files that
 * have been announced to remote session peers.
 *
 * @return Array of FTMFileDescriptor objects.
 */
-(NSArray *)getAnnouncedLocalFiles;

/**
 * Returns an array of FTMFileDescriptor objects that describes all of the files that
 * have been offered to or directly requested by remote session peers.
 *
 * @return Array of FTMFileDescriptor objects.
 */
-(NSArray *)getOfferedLocalFiles;

/**
 * Returns an array of FTMFileDescriptor objects that describes all of the files that
 * have been announced to us by remote session peers.
 *
 * @return Array of FTMFileDescriptor objects.
 */
-(NSArray *)getAvailableRemoteFiles;

/**
 * Returns the FTMFileDescriptor that matches the specified file ID.
 *
 * @param fileID Specifies the ID of a file.
 *
 * @return FTMFileDescriptor matching the file ID, nil otherwise.
 */
-(FTMFileDescriptor *)getLocalFileDescriptorForFileID: (NSData *)fileID;

/**
 * Tests to see if the provided file ID matches a file stored in the announced local files dictionary. 
 *
 * @param fileID  specifies the ID of a file.
 *
 * @return True if the file has been announced, false otherwise.
 */
-(BOOL)isAnnounced: (NSData *)fileID;

/**
 * Tests to see if the provided file ID matches a file stored in the offered local files dictionary.
 *
 * @param fileID  specifies the ID of a file
 *
 * @return True if the file has been shared, false otherwise.
 */
-(BOOL)isShared: (NSData *)fileID;

/**
 * Returns the FTMFileDescriptor that matches the provided file ID and peer parameters.
 *
 * @param fileID Specifies the ID of the file being requested.
 * @param peer Specifies the owner of the file
 *
 * @return FTMFileDescriptor matching the file ID, nil otherwise.
 */
-(FTMFileDescriptor *)getKnownFileDescritorForFileID: (NSData *)fileID ownedBy: (NSString *)peer;

/** @name Reset State */

/**
 * Resets the state of the FTMPermissionManager with a new bus ID.
 *
 * This function is called by the FTMFileTransferModule when the user specifies a new AllJoyn session
 * to be used. This function iterates over the announced and offered local files list and replaces the
 * contents of the owner field with the new bus ID.
 *
 * @warning *Note:* For the case where the user calls [FTMFileTransferModule uninitialize], the localBusID
 * parameter will be nil.
 *
 * @param localBusID Specified the bus ID of the new AllJoyn bus attachment. This value can be nil.
 */
-(void)resetStateWithLocalBusID: (NSString *)localBusID;

@end
