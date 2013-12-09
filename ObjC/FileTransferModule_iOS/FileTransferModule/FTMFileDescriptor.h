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
 * The FTMFileDescriptor class is the main object that is passed around to remote
 * session peers and provides valuable information regarding files that are
 * available for transfer. The file descriptor includes information regarding
 * who owns the file, the file name, the size of the file, the absolute path to the file (if
 * available) and the file ID. The file ID is the most important piece of data
 * because this is how most file transfers are initiated.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMFileDescriptor : NSObject

/** @name Class Properties */

/**
 * Stores the name of the file owner.
 */
@property (nonatomic, copy) NSString *owner;

/**
 * Stores the shared path of the file.
 */
@property (nonatomic, copy) NSString *sharedPath;

/**
 * Stores the relative path of the file.
 */
@property (nonatomic, copy) NSString *relativePath;

/**
 * Stores the name of the file.
 */
@property (nonatomic, copy) NSString *filename;

/**
 * Stores the ID of the file.
 */
@property (nonatomic, strong) NSData *fileID;

/**
 * Stores the size of the file (specified in bytes).
 */
@property (nonatomic) NSInteger size;

/** @name Creating FTMFileDescriptor */

/**
 * Creates an empty instance of the FTMFileDescriptor.
 *
 * @return Instance of FTMFileDescriptor
 */
- (id)init;

/**
 * Creates an instance of the FTMFileDescriptor using the provided descriptor parameter.
 *
 * This method essentially acts as a copy constructor.
 *
 * @param fileDescriptor Specifies the descriptor we are copying. 
 *
 * @return Instance of FTMFileDescriptor
 */
- (id)initWithFileDescriptor: (FTMFileDescriptor *)fileDescriptor;

/**
 * Overrides a method from NSObject, so we can define what it means for two FTMFileDescriptor objects to be equal.
 *
 * This function compares the contents of two FTMFileDescriptor objects. If the contents are identical, this function
 * will return true, otherwise this function will return false.
 *
 * @param other Specifies the descriptor we are comparing to.
 *
 * @return True if the object contents are identical, false otherwise.
 */
- (BOOL)isEqual: (id)other;

/**
 * Compares the contents of two FTMFileDescriptor objects to determine equality.
 *
 * @param that Specifies the descriptor we are comparing to.
 *
 * @return True if the object contents are identical, false otherwise.
 */
- (BOOL)isEqualToFileDescriptor: (FTMFileDescriptor *)that;

/**
 * Overrides a method from NSObject and returns the hash value of a FTMFileDescriptor object.
 *
 * @warning *Note:* Overriding this object was necessary since we overrode the isEqual: function
 * from NSObject.
 */
- (NSUInteger)hash;

@end
