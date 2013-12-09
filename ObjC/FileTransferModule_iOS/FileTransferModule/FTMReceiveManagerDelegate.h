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

/**
 * The FTMReceiveManagerDelegate is an internal delegate used by the FTMOfferManager and
 * FTMReceiver to notify the FTMReceiveManager that certain events have occurred. Such
 * events include initiating a file request, handling an incoming file chunk, and handling
 * a sender initiated transfer cancelled operation.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@protocol FTMReceiveManagerDelegate <NSObject>

@required

/** @name Required Methods */

/**
 * Triggered by the FTMOfferManager to initiate a file request when the user accepts a file offer from a session peer.
 *
 * @param file Specifies the FTMFileDescriptor of the file being requested.
 * @param saveFileName Specifies the name to save the file.
 * @param saveDirectory Specifies the location to save the file.
 * @param useDispatcher Specifies whether the request should use the FTMDispatcher.
 *
 * @return FTMStatusCode FTMOK or FTMBadFilePath.
 */
-(int)initiateRequestForFile: (FTMFileDescriptor *)file usingSaveFileName: (NSString *)saveFileName andSaveDirectory: (NSString *)saveDirectory throughDispatcher: (BOOL)useDispatcher;

/**
 * Triggered when the FTMReceiver receives a dataXferCancelled signal from the file sender.
 *
 * This function will notify the FTMReceiveManager to disregard any subsequent file chunks for the
 * file matching the provided file ID.
 *
 * @param fileID Specifies the ID of file being cancelled.
 * @param peer Specifies peer who cancelled the transfer.
 */
-(void)handleDataXferCancelledFrom: (NSString *)peer forFileWithID: (NSData *)fileID;

/**
 * Triggered when a chunk of a given file is received from a remote peer. 
 *
 * This function determines which temporary file this chunk belongs to, updates the sending progress, and sends 
 * the chunk to the FTMFileSystemAbstraction to be appended to the appropriate file.
 *
 * @param file Specifies the ID of the file the chunk belongs to.
 * @param startByte Specifies the starting index of chunk relative to file.
 * @param length Specifies the length of chunk.
 * @param chunk Actual file data.
 */
-(void)handleChunkForFile: (NSData *)file withStartByte: (int)startByte andLength: (int)length andFileData: (NSData *)chunk;


@end
