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
#import "FTMTransmitter.h"
#import "FTMStatusCode.h"
#import "FTMFileDescriptor.h"

@class FTMTransmitter;

/**
 * Defines a generic protocol that is implemented by each action class so they can
 * transmit themselves using the instance of the FTMTransmitter object. 
 */
@protocol FTMTransmitActionProtocol <NSObject>

@required

/** @name Required Methods */

/**
 * Allows the implementing action class to use the FTMTransmitter object to call the appropriate transmitter function.
 *
 * @param transmitter Instance of FTMTransmitter.
 *
 * @return FTMStatusCode various values.
 */
-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter;

@end

/**
 * Defines a generic action that all subsequent action classes inherit from. This class
 * defines the peer property since it is needed by all action types.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMAction : NSObject <FTMTransmitActionProtocol>

/** @name Class Properties */

/**
 * Stores the name of a remote session peer. Can also be nil.
 *
 * @return Remote peer name.
 */
@property (nonatomic, strong) NSString *peer;

@end

/**
 * Defines the FTMAnnounceAction that is used anytime an announcement is sent to a
 * remote session peer.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMAnnounceAction : FTMAction <FTMTransmitActionProtocol>

/** @name Class Properties */

/**
 * Stores an Array of FTMFileDescriptor objects to be announced to session peers.
 */
@property (nonatomic, strong) NSArray *fileList;

/**
 * Specifies whether or not the announcement is in response to an offer request.
 */
@property (nonatomic) BOOL isFileIDResponse;

/**
 * Sends a file announcement to a remote session peer over AllJoyn.
 *
 * @param transmitter Instance of FTMTransmitter.
 *
 * @return FTMStatusCode various values.
 */
-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter;

@end

/**
 * Defines the FTMRequestDataAction that is used anytime a file request is sent to a
 * remote session peer.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMRequestDataAction : FTMAction <FTMTransmitActionProtocol>

/** @name Class Properties */

/**
 * Stores the ID of the file being requested.
 */
@property (nonatomic, strong) NSData *fileID;

/**
 * Stores the starting position within the file.
 */
@property (nonatomic) int startByte;

/**
 * Stores the number of bytes being requested.
 */
@property (nonatomic) int length;

/**
 * Stores the maximum allowed file chunk size.
 */
@property (nonatomic) int maxChunkSize;

/**
 * Sends a file request to a remote session peer over AllJoyn.
 *
 * @param transmitter Instance of FTMTransmitter.
 *
 * @return FTMStatusCode various values.
 */
-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter;

@end

/**
 * Defines the FTMDataChunkAction that is used anytime a file chunk is sent to a
 * remote session peer.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMDataChunkAction : FTMAction <FTMTransmitActionProtocol>

/** @name Class Properties */

/**
 * Stores the ID of the file being requested.
 */
@property (nonatomic, strong) NSData *fileID;

/**
 * Stores the starting position within the file.
 */
@property (nonatomic) int startByte;

/**
 * Stores the number of bytes being requested.
 */
@property (nonatomic) int chunkLength;

/**
 * Stores the file data.
 */
@property (nonatomic, strong) NSData *chunk;

/**
 * Sends the file chunk to a remote session peer over AllJoyn.
 *
 * @param transmitter Instance of FTMTransmitter.
 *
 * @return FTMStatusCode various values.
 */
-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter;

@end

/**
 * Defines the FTMOfferFileAction that is used anytime a file offer is sent to a
 * remote session peer.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMOfferFileAction : FTMAction <FTMTransmitActionProtocol>

/** @name Class Properties */

/**
 * Stores the FTMFileDescriptor for the file being offered.
 */
@property (nonatomic, strong) FTMFileDescriptor *fd;

/**
 * Sends a file offer to a remote session peer over AllJoyn.
 *
 * @param transmitter Instance of FTMTransmitter.
 *
 * @return FTMStatusCode various values.
 */
-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter;

@end

/**
 * Defines the FTMRequestAnnouncementAction that is used anytime an announcement request
 * is sent to a remote session peer.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMRequestAnnouncementAction : FTMAction <FTMTransmitActionProtocol>

/**
 * Sends an announcement request to a remote session peer over AllJoyn.
 *
 * @param transmitter Instance of FTMTransmitter.
 *
 * @return FTMStatusCode various values.
 */
-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter;

@end

/**
 * Defines the FTMStopXferAction that is used anytime the file receiver decides to pause
 * or cancel a file transfer. This action will notify the file sender to stop transfering
 * bytes for the specified file.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMStopXferAction : FTMAction <FTMTransmitActionProtocol>

/** @name Class Properties */

/**
 * Stores the ID of the file being cancelled.
 */
@property (nonatomic, strong) NSData *fileID;

/**
 * Sends the stop transfer signal to ma remote session peer over AllJoyn.
 *
 * @param transmitter Instance of FTMTransmitter.
 *
 * @return FTMStatusCode various values.
 */
-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter;

@end

/**
 * Defines the FTMXferCancelledAction that is used anytime the file sender decides to cancel
 * a file transfer. This action will notify the file receiver that the transfer has been
 * cancelled.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMXferCancelledAction : FTMAction <FTMTransmitActionProtocol>

/** @name Class Properties */

/**
 * Stores the ID of the file being cancelled.
 */
@property (nonatomic, strong) NSData *fileID;

/**
 * Sends the transfer cancelled signal to a remote session peer over AllJoyn.
 *
 * @param transmitter Instance of FTMTransmitter.
 *
 * @return FTMStatusCode various values.
 */
-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter;

@end

/**
 * Defines the FTMRequestOfferAction that is used anytime an offer request is sent to a
 * remote session peer.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMRequestOfferAction : FTMAction <FTMTransmitActionProtocol>

/** @name Class Properties */

/**
 * Stores the absolute path of the unannounced file being requested.
 */
@property (nonatomic, strong) NSString *filePath;

/**
 * Sends an offer request to a remote session peer over AllJoyn.
 *
 * @param transmitter Instance of FTMTransmitter.
 *
 * @return FTMStatusCode various values.
 */
-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter;

@end

/**
 * Defines the FTMFileIDResponseAction that is used anytime we need to generate a file descriptor
 * for a file with the specified path. This action is usually used when remote session peers are 
 * allowed to request unannounced files.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMFileIDResponseAction : FTMAction

/** @name Class Properties */

/**
 * Stores the absolute file path of the file.
 */
@property (nonatomic, strong) NSString *filePath;

@end


