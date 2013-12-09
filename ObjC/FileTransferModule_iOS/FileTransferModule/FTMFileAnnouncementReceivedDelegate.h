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
 * The FTMFileAnnouncementReceivedDelegate can be utilized so the developer is notified
 * that an array of announced files has been received from a session peer. This delegate
 * is entirely optional but is necessary if the developer wishes to be able to send 
 * announcement requests to session peers.
 * 
 * See [FTMFileTransferModule setFileAnnouncementReceivedDelegate:].
 */
@protocol FTMFileAnnouncementReceivedDelegate <NSObject>

@required

/** @name Required Methods */

/**
 * Triggered when an announcement is received and notifies the user which files are now available for transfer.
 *
 * @param fileList Array of files available for transfer.
 * @param isFileIDResponse Indicates whether the announcement received is in response to an offer request.
 */
-(void)receivedAnnouncementForFiles: (NSArray *)fileList andIsFileIDResponse: (BOOL)isFileIDResponse;

@end
