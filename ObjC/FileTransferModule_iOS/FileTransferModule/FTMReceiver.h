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
#import "FTMAnnouncementManagerDelegate.h"
#import "FTMReceiveManagerDelegate.h"
#import "FTMSendManagerDelegate.h"
#import "FTMDirectedAnnouncementManagerDelegate.h"
#import "FTMFileTransferBusObject.h"
#import "FTMMessageUtility.h"

/**
 * The FTMReceiver is the main handler for AllJoyn signals. Every AllJoyn signal is handled
 * in this class and then calls functions in other classes to initiate any responses that
 * are needed. Some of the main operations include: handling announcements from other peers,
 * handling announcement requests from other peers, processing file chunks, and handling
 * sender and receiver initiated transfer cancellations. The receiver is the driving force
 * behind handling all incoming signals from AllJoyn session peers.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMReceiver : NSObject <DataTransferDelegateSignalHandler, FileDiscoveryDelegateSignalHandler>

/** @name Creating FTMReceiver */

/**
 * Constructs an instance of the FTMReceiver.
 *
 * @param busAttachment Instance of an AllJoyn bus attachment.
 * @param amDelegate Instance of FTMAnnouncementManagerDelegate.
 * @param smDelegate Instance of FTMSendManagerDelegate.
 * @param rmDelegate Instance of FTMReceiveManagerDelegate.
 * @param damDelegate Instance of FTMDirectedAnnouncementManagerDelegate.
 *
 * @return Instance of FTMReceiver.
 */
-(id)initWithBusAttachment: (AJNBusAttachment *)busAttachment announcementManagerDelegate: (id<FTMAnnouncementManagerDelegate>)amDelegate sendManagerDelegate: (id<FTMSendManagerDelegate>)smDelegate receiveManagerDelegate: (id<FTMReceiveManagerDelegate>)rmDelegate andDirectedAnnouncementManagerDelegate: (id<FTMDirectedAnnouncementManagerDelegate>)damDelegate;

/** @name Initialize FTMReceiver */

/**
 * Initializes the local variables of the FTMReceiver.
 *
 * This function is called by the init method and the resetState method.
 *
 * @param busAttachment Instance of an AllJoyn bus attachment.
 * @param amDelegate Instance of FTMAnnouncementManagerDelegate.
 * @param smDelegate Instance of FTMSendManagerDelegate.
 * @param rmDelegate Instance of FTMReceiveManagerDelegate.
 * @param damDelegate Instance of FTMDirectedAnnouncementManagerDelegate.
 */
-(void)initializeReceiverWithBusAttachment: (AJNBusAttachment *)busAttachment announcementManagerDelegate: (id<FTMAnnouncementManagerDelegate>)amDelegate sendManagerDelegate: (id<FTMSendManagerDelegate>)smDelegate receiveManagerDelegate: (id<FTMReceiveManagerDelegate>)rmDelegate andDirectedAnnouncementManagerDelegate: (id<FTMDirectedAnnouncementManagerDelegate>)damDelegate;

/** @name Reset State */

/**
 * Resets the state of the FTMReceiver to reinitialize the local variables.
 *
 * This function is called by the FTMFileTransferModule when the user specifies a new AllJoyn session
 * to be used.
 *
 * @param busAttachment Instance of an AllJoyn bus attachment.
 * @param amDelegate Instance of FTMAnnouncementManagerDelegate.
 * @param smDelegate Instance of FTMSendManagerDelegate.
 * @param rmDelegate Instance of FTMReceiveManagerDelegate.
 * @param damDelegate Instance of FTMDirectedAnnouncementManagerDelegate.
 */
-(void)resetStateWithBusAttachment: (AJNBusAttachment *)busAttachment announcementManagerDelegate: (id<FTMAnnouncementManagerDelegate>)amDelegate sendManagerDelegate: (id<FTMSendManagerDelegate>)smDelegate receiveManagerDelegate: (id<FTMReceiveManagerDelegate>)rmDelegate andDirectedAnnouncementManagerDelegate: (id<FTMDirectedAnnouncementManagerDelegate>)damDelegate;

@end
