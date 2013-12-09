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
 * The FTMAnnouncementManagerDelegate is an internal delegate that is used by the
 * FTMReceiver to notify the FTMAnnouncementManager that various events have occurred.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@protocol FTMAnnouncementManagerDelegate <NSObject>

@required

/** @name Required Methods */

/**
 * Triggered by the FTMReceiver when a normal announcement is received from a remote session peer.
 *
 * @param fileList Specifies the list of announced files.
 * @param peer Specifies the peer who sent the announcement.
 */
-(void)handleAnnouncedFiles: (NSArray *)fileList fromPeer: (NSString *)peer;

/**
 * Triggered by the FTMReceiver when an announcement request is received from a remote session peer.
 *
 * @param peer Specifies the peer that made the announcement request.
 */
-(void)handleAnnouncementRequestFrom: (NSString *)peer;

@end
