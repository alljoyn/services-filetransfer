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

#import "FTMAction.h"

@implementation FTMAction

@synthesize peer = _peer;

-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter
{
    return FTMOK;
}

@end

@implementation FTMAnnounceAction

@synthesize fileList = _fileList;
@synthesize isFileIDResponse = _isFileIDResponse;

-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter
{
    return [transmitter sendAnnouncementWithFileList: self.fileList toPeer: self.peer andIsFileIDResponse: self.isFileIDResponse];
}

@end

@implementation FTMRequestDataAction

@synthesize fileID = _fileID;
@synthesize startByte = _startByte;
@synthesize length = _length;
@synthesize maxChunkSize = _maxChunkSize;

-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter
{
    return [transmitter sendRequestDataUsingFileID: self.fileID startByte: self.startByte length: self.length andMaxChunkSize: self.maxChunkSize toPeer: self.peer];
}

@end

@implementation FTMDataChunkAction

@synthesize fileID = _fileID;
@synthesize startByte = _startByte;
@synthesize chunkLength = _chunkLength;
@synthesize chunk = _chunk;

-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter
{
    return [transmitter sendDataChunkUsingFileID: self.fileID startByte: self.startByte chunkLength: self.chunkLength andFileData: self.chunk toPeer: self.peer];
}

@end

@implementation FTMOfferFileAction

@synthesize fd = _fd;

-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter
{
    return [transmitter sendOfferFileWithFileDescriptor: self.fd toPeer: self.peer];
}

@end

@implementation FTMRequestAnnouncementAction

-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter
{
    return [transmitter sendAnnouncementRequestToPeer: self.peer];
}

@end

@implementation FTMStopXferAction

@synthesize fileID = _fileID;

-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter
{
    return [transmitter sendStopDataXferForFileID: self.fileID toPeer: self.peer];
}

@end

@implementation FTMXferCancelledAction

@synthesize fileID = _fileID;

-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter
{
    return [transmitter sendXferCancelledForFileID: self.fileID toPeer: self.peer];
}

@end

@implementation FTMRequestOfferAction

@synthesize filePath = _filePath;

-(FTMStatusCode)transmitActionWithTransmitter: (FTMTransmitter *)transmitter
{
    return [transmitter sendRequestOfferForFileWithPath: self.filePath toPeer: self.peer];
}

@end

@implementation FTMFileIDResponseAction

@synthesize filePath = _filePath;

@end



