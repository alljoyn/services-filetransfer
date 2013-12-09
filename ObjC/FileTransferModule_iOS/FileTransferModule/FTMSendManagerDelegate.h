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

/**
 * The FTMSendManagerDelegate is an internal delegate that is used by the FTMOfferManager,
 * the FTMFileTransferBusObject, FTMDispatcher, and the FTMReceiver to notify the FTMSendManager 
 * that various events have occurred. Such events include: start sending a file, queue the next
 * file chunk, and handle a stop data transfer signal sent by the file receiver.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@protocol FTMSendManagerDelegate <NSObject>

@required

/** @name Required Methods */

/**
 * Triggered by the FTMFileTransferBusObject or the FTMOfferManager to begin sending the file matching the specified file ID.
 *
 * @param fileID Specifies the ID of the file being transferred.
 * @param startByte Specifies the starting byte of the data relative to the file.
 * @param length Specifies the length of data chunk.
 * @param peer Specifies the peer receiving the file.
 * @param maxChunkLength Specifies the maximum file chunk size.
 *
 * @return FTMStatusCode FTMOK or FTMBadFileID.
 */
-(FTMStatusCode)sendFileWithID: (NSData *)fileID withStartByte: (int)startByte andLength: (int)length andMaxChunkLength: (int)maxChunkLength toPeer: (NSString *)peer;

/**
 * Triggered by the FTMDispatcher to queue the next file chunk, if available.
 */
-(void)dataSent;

/**
 * Triggered by the FTMReceiver to tell the FTMSendManager the receiver has cancelled the file transfer.
 *
 * @param fileID Specifies the ID of the file being transferred.
 * @param peer Specifies the peer receiving the file.
 */
-(void)handleStopDataXferForFileWithID: (NSData *)fileID fromPeer: (NSString *)peer;

@end
