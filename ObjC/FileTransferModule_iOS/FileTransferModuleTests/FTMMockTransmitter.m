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

#import "FTMMockTransmitter.h"

@implementation FTMMockTransmitter

@synthesize delegate = _delegate;
@synthesize statusCodeToReturn = _statusCodeToReturn;

-(FTMStatusCode)sendAnnouncementWithFileList: (NSArray *)fileList toPeer: (NSString *)peer andIsFileIDResponse: (BOOL)isFileIDResponse
{
    [self.delegate sendAnnouncementWithFileList: fileList toPeer: peer andIsFileIDResponse: isFileIDResponse];
    return FTMOK;
}

-(FTMStatusCode)sendRequestDataUsingFileID: (NSData *)fileID startByte: (int)startByte length: (int)length andMaxChunkSize: (int)maxChunkSize toPeer: (NSString *)peer
{
    [self.delegate sendRequestDataUsingFileID: fileID startByte: startByte length: length andMaxChunkSize: maxChunkSize toPeer: peer];
    return FTMOK;
}

-(FTMStatusCode)sendDataChunkUsingFileID: (NSData *)fileID startByte: (int)startByte chunkLength: (int)chunkLength andFileData: (NSData *)chunk toPeer: (NSString *)peer
{
    [self.delegate sendDataChunkUsingFileID: fileID startByte: startByte chunkLength: chunkLength andFileData: chunk toPeer: peer];
    return FTMOK;
}

-(FTMStatusCode)sendOfferFileWithFileDescriptor: (FTMFileDescriptor *)fd toPeer: (NSString *)peer
{
    [self.delegate sendOfferFileWithFileDescriptor: fd toPeer: peer];
    return FTMOK;
}

-(FTMStatusCode)sendAnnouncementRequestToPeer: (NSString *)peer
{
    [self.delegate sendAnnouncementRequestToPeer: peer];
    return FTMOK;
}

-(FTMStatusCode)sendStopDataXferForFileID: (NSData *)fileID toPeer: (NSString *)peer
{
    [self.delegate sendStopDataXferForFileID: fileID toPeer: peer];
    return FTMOK;
}

-(FTMStatusCode)sendXferCancelledForFileID: (NSData *)fileID toPeer: (NSString *)peer
{
    [self.delegate sendXferCancelledForFileID: fileID toPeer: peer];
    return FTMOK;
}

-(FTMStatusCode)sendRequestOfferForFileWithPath: (NSString *)filePath toPeer: (NSString *)peer
{
    [self.delegate sendRequestOfferForFileWithPath: filePath toPeer: peer];
    return self.statusCodeToReturn;
}

@end
