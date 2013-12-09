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

@class FTCFileIDResponseAction;

/**
 * The FTMDirectedAnnouncementManagerDelegate is an internal delegate that is used by the
 * FTMFileTransferBusObject, FTMDispatcher, and FTMReceiver to notify the FTMDirectedAnnouncementManager
 * that various events have occurred. Such events include handling offer requests/responses and 
 * generating file descriptors when sending offer responses.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@protocol FTMDirectedAnnouncementManagerDelegate <NSObject>

@required

/** @name Required Methods */

/**
 * Triggered by the FTMFileTransferBusObject to notify the FTMDirectedAnnouncementManager when a request for an unannounced file has been received.
 *
 * @param filePath Specifies the absolute path of the file being requested.
 * @param peer Specifies the peer requesting an unannounced file.
 *
 * @return FTMStatusCode FTMOK or FTMRequestDenied.
 */
-(int)handleOfferRequestForFile: (NSString *)filePath fromPeer: (NSString *)peer;

/**
 * Triggered by the FTMReceiver when an announcement signal is received that is in response to an offer request.
 *
 * @param fileList Specifies the list of announced files.
 * @param peer Specifies the peer that sent the directed announcement.
 */
-(void)handleOfferResponseForFiles: (NSArray *)fileList fromPeer: (NSString *)peer;

/**
 * Triggered by the FTMDispatcher to begin generating the file descriptor for a requested file.
 *
 * @param action Instance of FTMFileIDResponseAction.
 */
-(void)generateFileDescriptor: (FTCFileIDResponseAction *)action;

@end
