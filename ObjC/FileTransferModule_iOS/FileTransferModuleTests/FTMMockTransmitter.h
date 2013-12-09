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

#import "FTMTransmitter.h"
#import "TestProtocol.h"

@interface FTMMockTransmitter : FTMTransmitter

@property (nonatomic, retain) id<TestProtocol> delegate;
@property (nonatomic) FTMStatusCode statusCodeToReturn;

-(FTMStatusCode)sendAnnouncementWithFileList: (NSArray *)fileList toPeer: (NSString *)peer andIsFileIDResponse: (BOOL)isFileIDResponse;
-(FTMStatusCode)sendRequestDataUsingFileID: (NSData *)fileID startByte: (int)startByte length: (int)length andMaxChunkSize: (int)maxChunkSize toPeer: (NSString *)peer;
-(FTMStatusCode)sendDataChunkUsingFileID: (NSData *)fileID startByte: (int)startByte chunkLength: (int)chunkLength andFileData: (NSData *)chunk toPeer: (NSString *)peer;
-(FTMStatusCode)sendOfferFileWithFileDescriptor: (FTMFileDescriptor *)fd toPeer: (NSString *)peer;
-(FTMStatusCode)sendAnnouncementRequestToPeer: (NSString *)peer;
-(FTMStatusCode)sendStopDataXferForFileID: (NSData *)fileID toPeer: (NSString *)peer;
-(FTMStatusCode)sendXferCancelledForFileID: (NSData *)fileID toPeer: (NSString *)peer;
-(FTMStatusCode)sendRequestOfferForFileWithPath: (NSString *)filePath toPeer: (NSString *)peer;

@end
