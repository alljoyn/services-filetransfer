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

/**
 * The FTMOfferManagerDelegate is an internal delegate that is used by the FTMFileTransferBusObject
 * to see if there are any pending offers that are awaiting response from a remote session peer. 
 * This delegate is also used to respond to offers received from remote session peers.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@protocol FTMOfferManagerDelegate <NSObject>

@required

/** @name Required Methods */

/**
 * Triggered by the FTMFileTransferBusObject to see if the file ID matches a pending offer.
 *
 * @param fileID Specifies the file ID of the file being requested.
 *
 * @return True if the file ID matches a pending offer, false otherwise.
 */
-(BOOL)isOfferPendingForFileWithID: (NSData *)fileID;

/**
 * Triggered by the FTMFileTransferBusObject to handle a file request that is in response to a pending offer.
 *
 * @param peer Specifies the peer making the file request.
 * @param fileID Specifies the ID of the requested file.
 * @param startByte Specifies the starting byte of the request relative to the file.
 * @param length Specifies the length of request in bytes.
 * @param maxChunkLength Specifies the max file chunk size.
 *
 * @return FTMStatusCode FTMOK or FTMBadFileID.
 */
-(FTMStatusCode)handleRequestFrom: (NSString *)peer forFileID: (NSData *)fileID usingStartByte: (int)startByte withLength: (int)length andMaxChunkLength: (int)maxChunkLength;

/**
 * Triggered by the FTMFileTransferBusObject to handle the offer received from a remote session peer.
 *
 * @param peer Specifies the peer offering the file.
 * @param file Specifies the FTMFileDescriptor for the file being offered.
 *
 * @return FTMStatusCode FTMOfferAccepted or FTMOfferRejected.
 */
-(FTMStatusCode)handleOfferFrom: (NSString *)peer forFile: (FTMFileDescriptor *)file;

@end
