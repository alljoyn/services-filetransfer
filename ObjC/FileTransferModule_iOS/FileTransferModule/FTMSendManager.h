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
#import "FTMStatusCode.h"
#import "FTMSendManagerDelegate.h"
#import "FTMDispatcher.h"
#import "FTMFileSystemAbstraction.h"
#import "FTMPermissionManager.h"
#import "FTMRequestDataReceivedDelegate.h"
#import "FTMFileStatus.h"
#import "FTMProgressDescriptor.h"

/**
 * The FTMSendManager is a major piece of the FTMFileTransferModule. The FTMSendManager major
 * responsibilities include: handling file requests from other session peers, executing
 * sender initiated cancel operations, responding to receiver initiated pause/cancel
 * operations, and dividing larger files into smaller usable chunks. From the senders
 * perspective, this component is the driving force behind sending files to other session
 * peers.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMSendManager : NSObject <FTMSendManagerDelegate>

/** @name Class Properties */

/**
 * Sets a delegate for the implementing class of the FTMRequestDataReceivedDelegate protocol.
 *
 * The FTMRequestDataReceivedDelegate will notify the receiving class when a file request
 * has been received from a remote session peer.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMRequestDataReceivedDelegate> requestDataReceivedDelegate;

/** @name Creating FTMSendManager */

/**
 * Constructs an instance of the FTMSendManager.
 *
 * @param dispatcher Instance of the FTMDispatcher.
 * @param permissionManager Instance of the FTMPermissionManager.
 *
 * @return Instance of FTMSendManager.
 */
-(id)initWithDispatcher: (FTMDispatcher *)dispatcher andPermissionManager: (FTMPermissionManager *)permissionManager;

/**
 * Constructs an instance of the FTMSendManager.
 *
 * @param dispatcher Instance of the FTMDispatcher.
 * @param fsa Instance of the FTMFileSystemAbstraction.
 * @param permissionManager Instance of the FTMPermissionManager.
 *
 * @return Instance of FTMSendManager.
 */
-(id)initWithDispatcher: (FTMDispatcher *)dispatcher fileSystemAbstraction: (FTMFileSystemAbstraction *)fsa andPermnissionManager: (FTMPermissionManager *)permissionManager;

/** @name Sending Files */

/**
 * Processes the file request from the remote session peer.
 *
 * If the specified file ID matches an announced or shared file, this function queues an action 
 * in the FTMDispatcher to send the file to the specified peer. 
 *
 * @param fileID Specifies the ID of the file being requested.
 * @param startByte Specifies the starting position within the file (usually zero).
 * @param length Specifies the number of bytes to be sent (usually the length of the file).
 * @param peer Specifies the intended recipient of the file.
 * @param maxChunkLength Specifies the maximum file chunk size.
 *
 * @return FTMStatusCode FTMOK or FTMBadFileID.
 */
-(FTMStatusCode)handleRequestForFileWithID: (NSData *)fileID withStartByte: (int)startByte length: (int)length fromPeer: (NSString *)peer andMaxChunkLength: (int)maxChunkLength;

/**
 * Begins sending the file that matches the specified file ID.
 *
 * @param fileID Specifies the file ID of the requested file.
 * @param startByte Specifies the starting position for the file data.
 * @param length Specifies the length of the file.
 * @param maxChunkLength Specifies the maximum length of each file chunk.
 * @param peer Specifies the recipient of the file.
 *
 * @return FTMStatusCode FTMOK or FTMBadFileID.
 */
-(FTMStatusCode)sendFileWithID: (NSData *)fileID withStartByte: (int)startByte andLength: (int)length andMaxChunkLength: (int)maxChunkLength toPeer: (NSString *)peer;

/** @name Queue File Chunk */

/**
 * Queues the next file chunk if there is a pending file transfer waiting.
 */
-(void)dataSent;

/** @name Handle Pause/Cancel Operations */

/**
 * Cancels the file transfer that matches the provided file ID.
 *
 * This function will iterate over the list of pending file transfers to try and match the file ID.
 * If a match is found, a cancel action is queued into the FTMDispatcher to notify the receiver that
 * the sender has cancelled the file transfer.
 *
 * @param fileID Specifies the ID of the file being cancelled.
 *
 * @return FTMStatusCode FTMOK or FTMFileNotBeingTransferred.
 */
-(FTMStatusCode)cancelFileWithID: (NSData *)fileID;

/**
 * Handles the receiver initiated file transfer pause or cancel that matches the provided file ID. 
 *
 * This function will look at at all current file transfers and delete the file status that matches the
 * specified file ID.
 *
 * @param fileID Specifies the ID of the file being paused or cancelled.
 * @param peer Specifies the peer receiving the file.
 */
-(void)handleStopDataXferForFileWithID: (NSData *)fileID fromPeer: (NSString *)peer;

/** @name Monitoring File Transfer Progress */

/**
 * Returns an Array of FTMProgressDescriptors that outline the sending progress of each file transfer.
 *
 * The FTMProgressDescriptor object details the ID of the file, the length of the file, the total number of 
 * bytes that have been transferred, and the state of the transfer (will always be IN_PROGRESS).
 *
 * @return Array of FTMProgressDescriptor objects.
 */
-(NSArray *)getProgressList;

/** @name Reset State */

/**
 * Resets the state of the FTMSendManager.
 *
 * This function is called by the FTMFileTransferModule when the user specifies a new AllJoyn session
 * to be used and clears dictionary that stores the file transfer records.
 */
-(void)resetState;

@end
