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
#import "FileTransferBusObject.h"
#import "FTMDirectedAnnouncementManagerDelegate.h"
#import "FTMSendManagerDelegate.h"
#import "FTMOfferManagerDelegate.h"
#import "FTMMessageUtility.h"

/**
 * The FTMFileTransferBusObject is registered with the AllJoyn Bus Attachment and exposes
 * the Data Transfer and File Discovery interfaces to remote session peers. This object
 * listens and responds to remote method calls (not to be confused with signals) made by
 * AllJoyn session peers. Methods are used when a response is needed quickly since signals
 * are too slow. The three methods handled by the bus object are: requestData, requestOffer,
 * and offerFile.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMFileTransferBusObject : FileTransferBusObject

/** @name Class Properties */

/**
 * Sets a delegate so the FTMFileTransferBusObject can callback to the FTMDirectedAnnouncementManager.
 *
 * The FTMDirectedAnnouncementManagerDelegate will notify the FTMDirectedAnnouncementManager that an offer
 * request was received.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMDirectedAnnouncementManagerDelegate> directedAnnouncementManagerDelegate;

/**
 * Sets a delegate so the FTMFileTransferBusObject can callback to the FTMSendManager.
 *
 * The FTMSendManagerDelegate will notify the FTMSendManager to start sending a file immediately.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMSendManagerDelegate> sendManagerDelegate;

/**
 * Sets a delegate so the FTMFileTransferBusObject can callback to the FTMOfferManager.
 *
 * The FTMOfferManagerDelegate will notify the FTMOfferManager when a file offer is received
 * from a remote session peer.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMOfferManagerDelegate> offerManagerDelegate;

@end
