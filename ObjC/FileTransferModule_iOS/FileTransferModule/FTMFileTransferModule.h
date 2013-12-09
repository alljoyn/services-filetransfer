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
#import "AllJoynFramework/AJNBusAttachment.h"
#import "FTMFileTransferBusObject.h"
#import "FTMStatusCode.h"
#import "FTMAnnouncementManager.h"
#import "FTMDirectedAnnouncementManager.h"
#import "FTMDispatcher.h"
#import "FTMFileSystemAbstraction.h"
#import "FTMOfferManager.h"
#import "FTMPermissionManager.h"
#import "FTMReceiveManager.h"
#import "FTMReceiver.h"
#import "FTMSendManager.h"
#import "FTMFileAnnouncementReceivedDelegate.h"
#import "FTMFileAnnouncementSentDelegate.h"
#import "FTMFileCompletedDelegate.h"
#import "FTMOfferReceivedDelegate.h"
#import "FTMRequestDataReceivedDelegate.h"
#import "FTMUnannouncedFileRequestDelegate.h"
#import "FTMConstants.h"

/**
 * The FTMFileTransferModule is a library that provides application developers with a
 * simple framework they can use to send and receive files with the various peers within
 * their AllJoyn session. This component is designed to be utilized with any existing
 * AllJoyn application with little, to no, modification. The framework provides many different
 * operations to the application developer that include: announce/unannounce files to session
 * peers, requesting file announcements from other peers, request file by file ID and by absolute
 * path, cancel/pause file transfer, and offering files to a specified peer. There are also a
 * series of delegates that allow the developer to be notified at the application level when
 * various events occur; such examples include: an announcement being received by a session peer,
 * a file transfer has completed, a session peer has offered you a file, or a file request by path
 * has been received. The delegates allow the developer to respond accordingly to the various
 * events. Furthermore, the user has tremendous flexibility through the ability to change the
 * current AllJoyn sesion associated with the FTMFileTransferModule. This allows users to instantiate
 * multiple instances of the FTMFileTransferModule and specify a different AllJoyn session for each.
 * The user does not even have to specify an AllJoyn session for this component to work. The
 * majority of file transfer operations can still be used but will not send any signals or perform
 * any remote method calls until an AllJoyn session is provided. This framework is a great starting
 * point for any AllJoyn application developers who need the ability to send/receive files.
 *
 * @warning *Note:* For iOS, the FTMFileTransferModule requires files to be in your application
 * sandbox prior to announcing them to remote session peers.
 */
@interface FTMFileTransferModule : NSObject

/** @name Class Properties */

/**
 * Get the list of remote files that have been announced by remote session peers.
 *
 * @return Array of FTMFileDescriptors objects.
 */
@property (nonatomic, readonly, copy) NSArray *availableRemoteFiles;

/**
 * Get the list of local files that have been announced to remote session peers.
 *
 * @return Array of FTMFileDescriptors objects.
 */
@property (nonatomic, readonly, copy) NSArray *announcedLocalFiles;

/**
 * Get the list of files that have been offered to remote session peers.
 *
 * @return Array of FTMFileDescriptors objects.
 */
@property (nonatomic, readonly, copy) NSArray *offeredLocalFiles;

/**
 * Indicates whether the relative path is shown in the FTMFileDescriptor.
 *
 * By default, this property is set to true.
 *
 * @return True is the relative path is shown, false otherwise.
 */
@property (nonatomic) BOOL showRelativePath;

/**
 * Indicates whether the shared path is shown in the FTMFileDescriptor.
 *
 * By default, this property is set to false.
 *
 * @return True is the shared path is shown, false otherwise.
 */
@property (nonatomic) BOOL showSharedPath;

/**
 * Specifies the default directory to save transferred files.
 *
 * @return The absolute path to the save directory.
 */
@property (nonatomic) NSString *defaultSaveDirectory;

/**
 * Specifies the maximum file chunk size.
 *
 * By default, the default chunk size is 1024.
 *
 * @return Default chunk size.
 */
@property (nonatomic) int chunkSize;

/**
 * Get the progress list of files that are currently being transferred to remote session peers.
 *
 * @return Array of FTMFileStatus objects.
 */
@property (nonatomic, readonly, copy) NSArray *sendingProgressList;

/**
 * Get the progress list of files that are currently being received from remote session peers.
 *
 * @return Array of FTMFileStatus objects.
 */
@property (nonatomic, readonly, copy) NSArray *receiveProgressList;

/**
 * Sets a delegate for the implementing class of the FTMFileAnnouncementReceivedDelegate protocol.
 *
 * The FTMFileAnnouncementReceivedDelegate will notify the receiving class when an announcment
 * has been received from a remote session peer.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, weak) id<FTMFileAnnouncementReceivedDelegate> fileAnnouncementReceivedDelegate;

/**
 * Sets a delegate for the implementing of the FTMFileAnnouncementSentDelegate protocol.
 *
 * The FTMFileAnnouncementSentDelegate will notify the receiving class when an announcment
 * has been sent to a remote session peer.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, weak) id<FTMFileAnnouncementSentDelegate> fileAnnouncementSentDelegate;

/**
 * Sets a delegate for the implementing class of the FTMFileCompletedDelegate protocol.
 *
 * The FTMFileCompletedDelegate will notify the receiving class when a file transfer has
 * been completed. If set, this delegate is fired when files have finished transferring
 * completely or have been interrupted by a cancel operation.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, weak) id<FTMFileCompletedDelegate> fileCompletedDelegate;

/**
 * Sets a delegate for the implementing class of the FTMOfferReceivedDelegate protocol.
 *
 * The FTMOfferReceivedDelegate will notify the receiving class when a file offer has been
 * received from a remote session peer. The implementing class can specify the default
 * behavior for accepting or rejecting file offers.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, weak) id<FTMOfferReceivedDelegate> offerReceivedDelegate;

/**
 * Sets a delegate for the implementing class of the FTMRequestDataReceivedDelegate protocol.
 *
 * The FTMRequestDataReceivedDelegate will notify the receiving class when a file request
 * has been received from a remote session peer.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, weak) id<FTMRequestDataReceivedDelegate> requestDataReceivedDelegate;

/**
 * Sets a delegate for the implementing class of the FTMUnannouncedFileRequestDelegate protocol.
 *
 * The FTMUnannouncedFileRequestDelegate will notify the receiving class when a file request
 * has been received for a file that has not been explicitly announced. The implementing class
 * can specify the default behavior for allowing remote session peers to request files that
 * have not been announced.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, weak) id<FTMUnannouncedFileRequestDelegate> unannouncedFileRequestDelegate;

/** @name Creating FTMFileTransferModule */

/**
 * Constructs an instance of the FTMFileTransferModule without an AllJoyn session.
 *
 * Since an AllJoyn session is not specified, the user can call initializeWithBusAttachment:andSessionID 
 * at a later time to associate the FTMFileTransferModule with an AllJoyn session.
 *
 * @return Instance of FTMFileTransferModule.
 */
-(id)init;

/**
 * Constructs an instance of the FTMFileTransferModule with the provided AllJoyn session.
 *
 * Since an AllJoyn session is specified, the user can send and receive files over the existing
 * AllJoyn session.
 *
 * @param busAttachment Specifies the AllJoyn bus attachment.
 * @param sessionID Specifies the ID of the active AllJoyn session.
 *
 * @return Instance of FTMFileTransferModule.
 */
-(id)initWithBusAttachment: (AJNBusAttachment *)busAttachment andSessionID: (AJNSessionId)sessionID;

/** @name Initializing/Uninitializing FTMFileTransferModule */

/**
 * Provides a new AllJoyn session to an existing FTMFileTransferModule. 
 *
 * This allows tremendous flexibility by allowing the user to have more than
 * a single instance of the FTMFileTransferModule and manage multiple AllJoyn sessions. 
 * This concept of dynamic sessions will also all the user to utilize most of the core 
 * FTMFileTransferModule operations without specifying an AllJoyn session.
 * 
 * @warning *Note:* If files have been announced prior to an AllJoyn session being specified, an
 * announcement will be sent to all session peers.
 *
 * @param busAttachment Specifies the AllJoyn bus attachment.
 * @param sessionID Specifies the ID of the active AllJoyn session.
 */
-(void)initializeWithBusAttachment: (AJNBusAttachment *)busAttachment andSessionID: (AJNSessionId)sessionID;

/**
 * Disassociates the current AllJoyn session with the FTMFileTransferModule.
 *
 * The user will still be able to use most of the core FTMFileTransferModule operations minus 
 * anything that must be sent over AllJoyn to session peers. For example, the user can still 
 * announce files which will be stored but the announcement will not be sent over AllJoyn 
 * because a session does not exist.
 */
-(void)uninitialize;

/** @name File Publishing and Discovery API */

/**
 * Sends an announcement for the specified files to all session peers.
 *
 * This function accepts an array of strings which specify the absolute paths
 * of the files that need to be announced to session peers. This operation is
 * performed in a background thread so the application thread is not blocked.
 * 
 * @warning *Note:* You can specify the path to a directory which will announce every file
 * contained in the directory. This does not mean that any new files added to
 * the announced directory will be announced automatically. Announcing a directory
 * takes a snapshot of the directories current files and announces them. If any
 * new files are added, they must be explicitly announced at a later time.
 * 
 * @warning *Note:* If you announce the same file from two separate locations, only the most
 * recent file will be available for transfer.
 *
 * @warning *Note:* The announce method returns void but the user can register the
 * FTMFileAnnouncementSentDelegate using setFileAnnouncementSentListener: so the
 * announce function will return an array of paths that failed to successfully
 * announce. This FTMFileAnnouncementSentDelegate is not mandatory to announce files.
 *
 * @param paths Array of absolute file paths to be announced.
 */
-(void)announceFilePaths: (NSArray *)paths;

/**
 * Removes the specified files from the announced files list. 
 *
 * After the files are removed from the announced files list, an announcement is sent 
 * to all session peers that contains the latest list of files that are available. 
 *
 * @param paths Array of absolute paths to be unannounced.
 *
 * @return Array of paths that failed to unannounce.
 */
-(NSArray *)stopAnnounceFilePaths: (NSArray *)paths;

/**
 * Sends a request to the specified session peer for their announced files. 
 *
 * The peer parameter can be nil and will send a global signal to all remote session
 * peers requesting their announced files.
 *
 * @warning *Note:* The FTMFileAnnouncementReceivedDelegate must be set using 
 * setFileAnnouncementReceivedDelegate: in order for you to call this method. This 
 * is mandatory because you will not know when a peer answers your announcement 
 * request if you have not registered this delegate.
 *
 * @param peer Specifies the peer to send the announcement request.
 *
 * @return FTMStatusCode FTMOK or FTMNoFileAnnouncementListener.
 */
-(FTMStatusCode)requestFileAnnouncementFromPeer: (NSString *)peer;

/**
 * Sends a request to specified peer for the file with the specified path.
 *
 * This is the main mechanism that users will use to request files that have not been
 * explicitly announced. In order for this to happen, the FTMUnannouncedFileRequestDelegate
 * must be registered to allow session peers to request files that have not announced or 
 * shared. The default behavior is to reject requests for files that have not been announced 
 * or shared. An announcement for the requested file should arrive shortly if the request has
 * been granted.
 * 
 * @warning *Note:* The FTMUnannouncedFileRequestDelegate can be set using the
 * setUnannouncedFileRequestDelegate: method and is mandatory if you wish to allow sessions 
 * peers to request files that have not been announced or shared.
 *
 * @param peer Specifies the peer that will receive the request
 * @param path Specifies the absolute path of the remote file
 *
 * @return FTMStatusCode FTMOK or FTMRequestDenied.
 */
-(FTMStatusCode)requestOfferFromPeer: (NSString *)peer forFilePath: (NSString *)path;

/**
 * Searches the list of available files for a file matching the specified file path and owner parameters.
 *
 * @param path Specifies the absolute path of the file.
 * @param peer Specifies the owner of the file.
 *
 * @return The file ID if a match is found, nil otherwise.
 */
-(NSData *)getFileIdForFileWithPath: (NSString *)path ownedBy: (NSString *)peer;

/** @name File Caching */

/**
 * Enables caching file hash values to the specified file. 
 *
 * When caching is enabled, all file hash values are stored in the cache file. This operation
 * is helpful to avoid recalculating the hash values of the same file mutliple times, which for
 * large files can be a time consuming operation. 
 * 
 * @warning *Note:* Caching is disabled by default.
 * 
 * @warning *Note:* Calling this function to change the cache file causes any existing 
 * cached data to be written to the old file, and then the cache is replaced by the 
 * contents of the new file (if any are present in the new file).
 *
 * @warning *Note:* Specifying nil for the path parameter, disables caching.
 *
 * @param path Specifies the path to the file used for caching
 */
-(void)setCacheFileWithPath: (NSString *)path;

/**
 * Purges the cache file of hash values that are out-dated.
 *
 * All hash values that correspond to files that have been deleted or been modified since the
 * last time they were calculated will be removed from the cache file.
 */
-(void)cleanCacheFile;

/** @name File Tranfer API */

/**
 * Sends a file request to the specified peer for the file matching the file ID parameter.
 *
 * This is the main method that should be used when requesting files. Each session peer will
 * accumulate a list of files that are available from each peer through file announcements.
 * Each session peer can then request any file that is made available using this function.
 *
 * @param peer Specifies the peer to send the file request.
 * @param fileID Specifies the file ID of the file being requested.
 * @param fileName Specifies the name for which to save the file.
 *
 * @return FTMStatusCode FTMOK, FTMBadFileID, FTMBadFilePath, or FTMFileNotBeingTransferred.
 */
- (FTMStatusCode)requestFileFromPeer: (NSString *)peer withFileID: (NSData *)fileID andSaveName: (NSString *)fileName;

/**
 * Sends a file request to the specified peer for the file matching the fileID parameter.
 *
 * This is the main method that should be used when requesting files. Each session peer will
 * accumulate a list of files that are available from each peer through file announcements.
 * Each session peer can then request any file that is made available using this function.
 *
 * @param peer Specifies the peer to send the file request.
 * @param fileID Specifies the file ID of the file being requested.
 * @param fileName Specifies the name for which to save the file.
 * @param directory Specifies the directory to save the file.
 *
 * @return FTMStatusCode FTMOK, FTMBadFileID, FTMBadFilePath, or FTMFileNotBeingTransferred.
 */
- (FTMStatusCode)requestFileFromPeer: (NSString *)peer withFileID: (NSData *)fileID andSaveName: (NSString *)fileName andSaveDirectory: (NSString *)directory;

/**
 * Sends a file offer to the specified peer.
 *
 * Allows the user to offer a file, that has not explicitly been announced to the specified peer.
 * The user must specify the timeout interval because this function executes on the calling
 * thread and will block until the timeout interval is exceeded. However, even if an offer
 * expires from the sender's perspective, the peer that received the offer can still request
 * the file that you offered to them.  Offering files is the main method to use when you want to
 * share files with select peers and you do not want to announce them to the entire session.
 *
 * @warning *Note:* if the timeout interval is set to zero, the default timeout interval will
 * be used and is 5 seconds.
 *
 * @param peer Specifies the peer to send the offer.
 * @param path Specifies the path of the local file being offered.
 * @param timeout Specifies how long we will wait for a response.
 *
 * @return FTMStatusCode FTMOK, FTMOfferRejected, FTMBadFilePath, or FTMOfferTimeout.
 */
- (FTMStatusCode)offerFileToPeer: (NSString *)peer withFilePath: (NSString *)path andTimeoutMillis: (int)timeout;

/**
 * Cancels the file being sent that matches the specified file ID.
 *
 * Allows the sender to cancel a transfer for a file with the specified file ID and the
 * receiver will be notified that the sender cancelled the file transfer. The receiver
 * will keep the temporary file in memory so the transfer can be resumed at a later time
 * if the receiver wishes.
 *
 * @param fileID Specifies the file ID for the file the user wishes to cancel.
 *
 * @return FTMStatusCode FTMOK or FTMFileNotBeingTransferred.
 */
- (FTMStatusCode)cancelSendingFileWithID: (NSData *)fileID;

/**
 * Cancels the file being received that matches the specified file ID.
 *
 * Allows the receiver to cancel a transfer for a file with the specified file ID. A cancel
 * notification is sent to the sender to not send any more bytes. The receiver immediately
 * deletes any temporary files corresponding to the cancelled file transfer.
 *
 * @param fileID Specifies the file ID for the file the user wishes to cancel
 *
 * @return FTMStatusCode FTMOK or FTMBadFileID.
 */
- (FTMStatusCode)cancelReceivingFileWithID: (NSData *)fileID;

/**
 * Pauses the file being received matching the specified file ID.
 *
 * This method can only be called by the receiver and temporarily suspends a file transfer. 
 * The sender receives a notification to stop transmitting bytes to the receiver. Any temporary
 * files corresponding to the paused transfer are held in memory so the operation can be resumed
 * at a later time.
 *
 * @param fileID Specifies the file ID for the file the user wishes to pause.
 *
 * @return FTMStatusCode FTMOK or FTMBadFileID.
 */
- (FTMStatusCode)pauseReceivingFileWithID: (NSData *)fileID;

@end

