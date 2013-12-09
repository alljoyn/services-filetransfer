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
#import "FTMFileDescriptor.h"
#import "FTMDispatcher.h"
#import "FTMFileSystemAbstraction.h"
#import "FTMPermissionManager.h"
#import "FTMOfferManagerDelegate.h"
#import "FTMOfferReceivedDelegate.h"
#import "FTMSendManagerDelegate.h"
#import "FTMReceiveManagerDelegate.h"

/**
 * The FTMOfferManager is the main driving force behind offering files to
 * and responding to offers made by remote session peers. When an offer
 * is sent to a remote session peer the FTMOfferManager will wait until the
 * offer is either accepted, rejected, or times out. If the offer is
 * accepted the FTMOfferManager will notify the FTMSendManager to immediately
 * begin transferring the file to the remote session peer. Conversely, if
 * an offer is received that the user wishes to accept, the FTMOfferManager
 * will notify the FTMReceiveManager to immediately request the file from the
 * remote session peer.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMOfferManager : NSObject <FTMOfferManagerDelegate>

/** @name Class Properties */

/**
 * Sets a delegate for the implementing class of the FTMOfferReceivedDelegate protocol.
 *
 * The FTMOfferReceivedDelegate will notify the receiving class when a file offer has
 * been received from a remote session peer.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMOfferReceivedDelegate> offerReceivedDelegate;

/**
 * Sets a delegate so the FTMOfferManager can callback to the FTMSendManager.
 *
 * The FTMSendManagerDelegate will notify the FTMSendManager to immediately begin sending
 * the offered file that was accepted by the remote session peer.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMSendManagerDelegate> sendManagerDelegate;

/**
 * Sets a delegate so the FTMOfferManager can callback to the FTMReceiveManager.
 *
 * The FTMReceiveManagerDelegate will notify the FTMReceiveManager to immediately request
 * the file that was just offered.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMReceiveManagerDelegate> receiveManagerDelegate;

/** @name Creating FTMOfferManager */

/**
 * Constructs an instance of the FTMOfferManager.
 *
 * @param dispatcher Instance of the FTMDispatcher.
 * @param permissionManager Instance of the FTMPermissionManager.
 * @param localBusID Specifies the Bus ID of the Bus Attachment passed in by the user.
 *
 * @return Instance of FTMOfferManager.
 */
-(id)initWithDispatcher: (FTMDispatcher *)dispatcher permissionManager: (FTMPermissionManager *)permissionManager andLocalBusID: (NSString *)localBusID;

/**
 * Constructs an instance of the FTMOfferManager.
 *
 * @param dispatcher Instance of the FTMDispatcher.
 * @param permissionManager Instance of the FTMPermissionManager.
 * @param fsa Instance of the FTMFileSystemAbstraction.
 * @param localBusID Specifies the Bus ID of the Bus Attachment passed in by the user.
 *
 * @return Instance of FTMOfferManager.
 */
-(id)initWithDispatcher: (FTMDispatcher *)dispatcher permissionManager: (FTMPermissionManager *)permissionManager fileSystemAbstraction: (FTMFileSystemAbstraction *)fsa andLocalBusID: (NSString *)localBusID;

/** @name Reset State */

/**
 * Resets the state of the FTMOfferManager with a new bus ID.
 *
 * This function is called by the FTMFileTransferComponent when the user specifies a new AllJoyn session
 * to be used.
 *
 * @warning *Note:* For the case where the user calls [FTMFileTransferModule uninitialize], the localBusID
 * parameter will be nil.
 *
 * @param localBusID Specifies the bus ID of the new AllJoyn bus attachment. This value can be nil.
 */
-(void)resetStateWithLocalBusID: (NSString *)localBusID;

/** @name Offering Files */

/**
 * Offers the file at the specified path to the provided peer.
 *
 * This function maps to an AllJoyn method call and will block for the specified timeout waiting for a response.
 * If the timeout parameter is zero, this function will wait a default of 5 seconds for a response. If the offer
 * is accepted by the remote peer, the file will be immediately requested.
 *
 * @param peer Specifies the peer to send the offer.
 * @param path Specifies the absolute path to the file being offered.
 * @param timeout Specifies the amount of time, in milliseconds, to wait for a response.
 *
 * @return FTMStatusCode FTMOK, FTMOfferRejected, FTMOfferTimeout, or FTMBadFilePath.
 */
-(FTMStatusCode)offerFileToPeer: (NSString *)peer forFileWithPath: (NSString *)path andTimeout: (int)timeout;

/**
 * Determines whether the provided file ID matches a pending offer.
 *
 * This function is called by the FTMFileTransferBusObject to see if the file request that was just
 * received matches a pending offer.
 *
 * @param fileID Specifies the ID of the file just requested.
 * @return True if there is an offer pending for the specified file ID, false otherwise.
 */
-(BOOL)isOfferPendingForFileWithID: (NSData *)fileID;

/**
 * Handles a file requests that match pending offers.
 *
 * This function is invoked by the FTMFileTransferBusObject when a file request is
 * received that matches a pending offer. This function will reset some internal variables
 * regarding offered files and notify the FTMSendManager to start sending the file to 
 * remote peer.
 *
 * @param fileID Specifies the file ID of the file being requested.
 * @param startByte Specifies the starting position within the file (usually zero).
 * @param length Specifies the number of bytes to be sent (usually the length of the file).
 * @param peer Specifies the intended recipient of the file.
 * @param maxChunkLength Specifies the maximum file chunk size.
 *
 * @return FTMStatusCode FTMOK, FTMRequestDenied, or FTMBadFileID.
 */
-(FTMStatusCode) handleRequestFrom: (NSString *)peer forFileID: (NSData *)fileID usingStartByte: (int)startByte withLength: (int)length andMaxChunkLength: (int)maxChunkLength;

/** @name Handling Offers */

/**
 * Handles file offers from remote session peers.
 *
 * This function first checks to see if the offerReceivedDelegate is registered. If it isn't, 
 * the file offer is immediately rejected. If the delegate is registered, the delegate is called
 * to see if the user will accept the offer. If the offer is accepted, return FTMOfferAccepted.
 * Otherwise, return FTMOfferRejected.
 *
 * @param peer Specifies the peer offering the file.
 * @param file Specifies the FTMFileDescriptor of the offered file.
 *
 * @return FTMStatusCode FTMOfferAccepted or FTMOfferRejected.
 */
-(FTMStatusCode) handleOfferFrom: (NSString *)peer forFile: (FTMFileDescriptor *)file;

@end
