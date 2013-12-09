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
#import <alljoyn/Status.h>
#import "AllJoynFramework/AJNMessageArgument.h"
#import "FTMFileDescriptor.h"

/**
 * The FTMMessageUtility class provides a set of functions that package Objective-C and
 * FTMFileTransferModule data types into an AJNMessageArgument objects. This ensures that
 * the custom objects as well as primitive Objective-C data types can be sent over AllJoyn. 
 * Conversely, this class provides a set of functions that unpackage AJNMessageArgument objects
 * into FTMFileTransferModule and Objective-C data types.
 *
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMMessageUtility : NSObject

/** @name Conversion From Objective-C Type to AllJoyn Message Argument */

/**
 * Converts an NSData file ID object into an AllJoyn message argument.
 *
 * This function handles the conversion from an objective-c type to a C++ type that can
 * be sent over AllJoyn.
 *
 * @param fileID Specifies the ID of a file.
 *
 * @return Instance of AJNMessageArgument.
 */
+ (AJNMessageArgument *)messageArgumentFromFileID: (NSData *)fileID;

/**
 * Converts an NSData object into an AllJoyn message argument.
 *
 * This function handles the conversion from an objective-c type to a C++ type that can
 * be sent over AllJoyn.
 *
 * @param data Specifies the NSData bytes to be converted.
 *
 * @return Instance of AJNMessageArgument.
 */
+ (AJNMessageArgument *)messageArgumentFromData: (NSData *)data;

/**
 * Converts an array of FTMFileDescriptor objects to an AllJoyn message argument.
 *
 * This function handles the conversion from an objective-c type to a C++ type that can
 * be sent over AllJoyn.
 *
 * @param fileList Specifies an array of FTMFileDescriptor objects.
 *
 * @return Instance of AJNMessageArgument.
 */
+ (AJNMessageArgument *)messageArgumentFromFileList: (NSArray *)fileList;

/**
 * Converts a FTMFileDescriptor object to an AllJoyn message argument.
 *
 * This function handles the conversion from an objective-c type to a C++ type that can
 * be sent over AllJoyn.
 *
 * @param fd Specifies a FTMFileDescriptor object.
 *
 * @return Instance of AJNMessageArgument.
 */
+ (AJNMessageArgument *)messageArgumentFromFileDescriptor: (FTMFileDescriptor *)fd;

/** @name Conversion From AllJoyn Message Argument to Objective-C Type */

/**
 * Converts an AllJoyn message argument to an NSData object.
 *
 * This function handles the conversion from a C++ type to an Objective-C type.
 *
 * @param message Specifies an AllJoyn message argument object.
 *
 * @return NSData object that represents the ID of a file.
 */
+ (NSData *)fileIDFromMessageArgument: (AJNMessageArgument *)message;

/**
 * Converts an AllJoyn message argument to an NSData object.
 *
 * This function handles the conversion from a C++ type to an Objective-C type.
 *
 * @param message Specifies an AllJoyn message argument object.
 *
 * @return NSData object that represents the ID of a file.
 */
+ (NSData *)dataFromMessageArgument: (AJNMessageArgument *)message;

/**
 * Converts an AllJoyn message argument to a FTMFileDescriptor object.
 *
 * This function handles the conversion from a C++ type to an Objective-C type.
 *
 * @param message Specifies an AllJoyn message argument object.
 *
 * @return Instance of a FTMFileDescriptor object.
 */
+ (FTMFileDescriptor *)descriptorFromMessageArgument: (AJNMessageArgument *)message;

/**
 * Converts an AllJoyn message argument to an array of FTMFileDescriptor objects.
 *
 * This function handles the conversion from a C++ type to an Objective-C type.
 *
 * @param message Specifies an AllJoyn message argument object.
 *
 * @return Array of FTMFileDescriptor objects.
 */
+ (NSArray *)descriptorArrayFromMessageArgument: (AJNMessageArgument *) message;

@end

