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
#import "FTMDispatcher.h"
#import "FTMStatusCode.h"
#import "FTMPermissionManager.h"
#import "FTMFileSystemAbstraction.h"
#import "FTMAnnouncementManagerDelegate.h"
#import "FTMFileAnnouncementReceivedDelegate.h"
#import "FTMFileAnnouncementSentDelegate.h"

/**
 * The FTMAnnouncementManager is responsible for handling events associated
 * with announcing files. From the senders perspective, the FTMAnnouncementManager
 * is responsible for sending announcements and announcement requests to remote
 * session peers and, if available, notify the user when an announcement
 * has finished and been sent to session peers. From the receivers perspective,
 * the FTMAnnouncementManager is responsible for handling announced files when they
 * arrive, responding to announcement requests from session peers, and, if available, 
 * notify the user when an announcement has been received from a remote
 * session peer. The FTMAnnouncementManager also maintains a pair of boolean settings
 * the user can set to dictate whether to show the shared/relative path of
 * announced files.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */

@interface FTMAnnouncementManager : NSObject <FTMAnnouncementManagerDelegate>

/** @name Class Properties */

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
 * Sets a delegate for the implementing class of the FTMFileAnnouncementReceivedDelegate protocol.
 *
 * The FTMFileAnnouncementReceivedDelegate will notify the receiving class when an announcment
 * has been received from a remote session peer.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMFileAnnouncementReceivedDelegate> fileAnnouncementReceivedDelegate;

/**
 * Sets a delegate for the implementing class of the FTMFileAnnouncementSentDelegate protocol.
 *
 * The FTMFileAnnouncementSentDelegate will notify the receiving class when an announcment
 * has been sent to a remote session peer.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMFileAnnouncementSentDelegate> fileAnnouncementSentDelegate;

/** @name Creating FTMAnnouncementManager */

/**
 * Constructs an instance of the FTMAnnouncementManager.
 * 
 * @param dispatcher Instance of the FTMDispatcher.
 * @param permissionManager Instance of the FTMPermissionManager.
 * @param localBusID Specifies the Bus ID of the Bus Attachment passed in by the user.
 *
 * @return Instance of FTMAnnouncementManager.
 */
-(id)initWithDispatcher: (FTMDispatcher *)dispatcher permissionsManager: (FTMPermissionManager *)permissionManager andLocalBusID: (NSString *)localBusID;

/**
 * Constructs an instance of the FTMAnnouncementManager.
 *
 * @param dispatcher Instance of the FTMDispatcher.
 * @param permissionManager Instance of the FTMPermissionManager.
 * @param fsa Instance of the FTMFileSystemAbstraction.
 * @param localBusID Specifies the Bus ID of the Bus Attachment passed in by the user.
 *
 * @return Instance of FTMAnnouncementManager.
 */
-(id)initWithDispatcher: (FTMDispatcher *)dispatcher permissionsManager: (FTMPermissionManager *)permissionManager fileSystemAbstraction: (FTMFileSystemAbstraction *)fsa andLocalBusID: (NSString *)localBusID;

/** @name Sending Announcments and Announcement Requests */

/**
 * Sends a global announcement to all session peers using the list of provided file paths.
 *
 * This function only spawns a new thread and calls a private helper function that is responsible for sending the 
 * announcement.
 *
 * @param pathList Specfies an array of absolute paths of files to be announced.
 *
 * @warning *Note:* If you need to know if the operation was successful, set the property
 *          fileAnnouncementSentDelegate and you will know the announcment has been sent and you will
 *          also receive an array of paths that failed to be announced.
 */
-(void)announceFilePaths: (NSArray *)pathList;

/**
 * Removes any files matching one of the provided paths from the announced files list.
 *
 * The user will pass in a list of absolute paths for files they wish to unannounce. A new
 * announcement is then sent to all remote session peers that only contains files that are
 * still available for transfer.
 *
 * @param pathList Specfies an array of absolute paths of files to be unannounced.
 *
 * @return Array containing the paths that failed to unannounce. 
 */
-(NSArray *)stopAnnounceFilePaths: (NSArray *)pathList;

/**
 * Sends an announcement request to the specified peer.
 *
 * This method sends a request to the specified peer for their list of announced files. For
 * this function to work correctly, the user must set the property fileAnnouncementReceivedDelegate
 * and have an active AllJoyn session. The delegate is mandatory because you will have no way of
 * knowing when the peer sends the response to your request. A valid AllJoyn session is also mandatory
 * because signals cannot be sent to remote peers when a session does not exist.
 *
 * @param peer Specifies the peer to send the announcement request.
 *
 * @return FTMStatusCode FTMOK if successful, FTMNoFileAnnouncementListener if delegate is not set, or FTMNoAjConnection if an AllJoyn session does not exist.
 */
-(FTMStatusCode)requestAnnouncementFromPeer: (NSString *)peer;

/** @name Handling Announcments and Announcement Requests */

/**
 * Handles the announced files from the specified peer.
 *
 * This function is called when an announcement is received from a remote session peer. The file list
 * is passed over to the FTMPermissionManager for storage and, and if the fileAnnouncementReceivedDelegate
 * is set, notifies the user that an announcement has been received.
 *
 * @param fileList Array of announced files received from a remote session peer.
 * @param peer Specifies the peer who sent the announcement.
 */
-(void)handleAnnouncedFiles: (NSArray *)fileList fromPeer: (NSString *)peer;

/**
 * Handles the announcement request from a remote session peer.
 *
 * This function is called when an announcement request is received from a remote session peer. This
 * method will queue up an announcement to be sent back to the specified peer.
 *
 * @param peer Specifies the peer who sent the announcement request.
 */
-(void)handleAnnouncementRequestFrom: (NSString *)peer;

/** @name Reset State */

/**
 * Resets the state of the FTMAnnouncementManager with the specified bus ID.
 *
 * This function is called by the FTMFileTransferModule when the user specifies a new AllJoyn session
 * to be used.
 *
 * @warning *Note:* For the case where the user calls [FTMFileTransferModule uninitialize], the localBusID
 * parameter will be nil.
 *
 * @param localBusID Specified the bus ID of the new AllJoyn bus attachment. This value can be nil. 
 */
-(void)resetStateWithLocalBusID: (NSString *)localBusID;

@end
