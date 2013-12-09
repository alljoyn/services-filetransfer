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
 * The FTMFileCompletedDelegate can be utilized so that the developer can be notified when
 * a file transfer has been completed. This event will be triggered by the FTMReceiveManager
 * when a file transfer has completed or been cancelled.
 * 
 * See [FTMFileTransferModule setFileCompletedDelegate:].
 */
@protocol FTMFileCompletedDelegate <NSObject>

@required

/** @name Required Methods */

/**
 * Triggered when a transfer has been completed. 
 *
 * This delegate will be triggered on cancellation as well as successful completion.
 *
 * @param fileName Specifies the name of the file that completed transmission.
 * @param statusCode FTMOK if completely transfered, FTMCancelled otherwise.
 */
-(void)fileCompletedForFile: (NSString *)fileName withStatusCode: (int)statusCode;

@end
