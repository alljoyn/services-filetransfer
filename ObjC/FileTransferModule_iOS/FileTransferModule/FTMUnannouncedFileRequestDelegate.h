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
 * The FTMUnannouncedFileRequestDelegate is required if the developer wishes
 * to allow session peers to request files that have not been explicitly
 * announced or shared. The default behavior is to deny all requests for
 * files that have not been announced or shared.
 * 
 * See [FTMFileTransferModule setUnannouncedFileRequestDelegate:].
 */
@protocol FTMUnannouncedFileRequestDelegate <NSObject>

@required

/** @name Required Methods */

/**
 * Triggered when a request is received for an unannounced file. 
 *
 * @param filePath Specifies the absolute path of file being requested.
 *
 * @return True to allow the request, false to reject the request.
 */
-(BOOL)allowUnannouncedRequestsForFileWithPath: (NSString *) filePath;

@end
