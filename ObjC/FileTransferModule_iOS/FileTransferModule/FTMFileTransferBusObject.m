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

#import "FTMFileTransferBusObject.h"

@implementation FTMFileTransferBusObject

- (NSNumber*)requestDataWithFileID:(AJNMessageArgument*)fileID startByte:(NSNumber*)startByte length:(NSNumber*)length andMaxChunkLength:(NSNumber*)maxChunkLength fromSender:(NSString *)sender
{
    NSLog(@"received request data from: %@", sender);
    
    NSData *fileIDData = [FTMMessageUtility fileIDFromMessageArgument: fileID];
    int receivedStartByte = [startByte intValue];
    int receivedLength = [length intValue];
    int receivedMaxChunkLength = [maxChunkLength intValue];
    
    FTMStatusCode returnValue = FTMInvalid;
    
    if (self.offerManagerDelegate != nil && [self.offerManagerDelegate isOfferPendingForFileWithID:fileIDData])
    {
        returnValue = [self.offerManagerDelegate handleRequestFrom:sender forFileID:fileIDData usingStartByte: receivedStartByte withLength: receivedLength andMaxChunkLength: receivedMaxChunkLength];
    }
    else if (self.sendManagerDelegate != nil)
    {
        returnValue = [self.sendManagerDelegate sendFileWithID: fileIDData withStartByte: receivedStartByte andLength: receivedLength andMaxChunkLength: receivedMaxChunkLength toPeer: sender];
    }
    return [NSNumber numberWithInt: returnValue];
}

- (NSNumber*)requestOfferWithFilePath:(NSString*)filePath fromSender:(NSString *)sender
{
    NSLog(@"received request offer from: %@", sender);
    
    FTMStatusCode returnValue = FTMInvalid;
    
    if (self.directedAnnouncementManagerDelegate != nil)
    {
        returnValue = [self.directedAnnouncementManagerDelegate handleOfferRequestForFile:filePath fromPeer:sender];
    }
    return [NSNumber numberWithInt: returnValue];
}

- (NSNumber*)offerFileWithFileDescriptor:(AJNMessageArgument*)file fromSender:(NSString *)sender
{
    NSLog(@"received offer from: %@", sender);
    
    FTMFileDescriptor *descriptor = [FTMMessageUtility descriptorFromMessageArgument: file];
    
    FTMStatusCode returnValue = FTMInvalid;
    
    if (self.offerManagerDelegate != nil)
    {
        returnValue = [self.offerManagerDelegate handleOfferFrom:sender forFile:descriptor];
    }
    return [NSNumber numberWithInt: returnValue];
}

@end
