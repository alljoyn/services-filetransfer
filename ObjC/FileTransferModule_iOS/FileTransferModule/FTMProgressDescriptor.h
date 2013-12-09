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

/**
 * The FTMProgressDescriptor is utilized when the user requests file transfer status
 * updates from either the sender or the receiver. This class will outline all of
 * the files that are being transferred by specifying how many bytes have already been
 * sent or received and give the total length of the file. This will allow the user to
 * see the current progress of each file transfer.
 * 
 * See [FTMFileTransferModule sendingProgressList and [FTMFileTransferModule receiveProgressList].
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMProgressDescriptor : NSObject

/** @name Class Properties */

/*
 * Defines an enumerated type to determine the current state of the file transfer.
 */
typedef enum
{
    IN_PROGRESS,
    PAUSED,
    TIMED_OUT
} State;

/**
 * Specifies the ID of the file being transferred.
 */
@property (nonatomic, strong) NSData *fileID;

/**
 * Specifies the current state of the file transfer.
 */
@property (nonatomic) State state;

/**
 * Specifies the number of bytes that have been transferred.
 */
@property (nonatomic) int bytesTransferred;

/**
 * Specifies the size of the file.
 */
@property (nonatomic) int fileSize;

@end
