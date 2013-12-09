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

#import <SenTestingKit/SenTestingKit.h>
#import "AllJoynFrameWork/AJNBusAttachment.h"
#import "FTMFileTransferBusObject.h"
#import "FTMConstants.h"
#import "FTMTransmitter.h"
#import "AllJoynFrameWork/AJNSessionPortListener.h"
#import "AllJoynFrameWork/AJNInterfaceDescription.h"
#import "FTMFileDescriptor.h"

@interface FTMTransmitterTest : SenTestCase <AJNSessionPortListener, AJNSessionListener, AJNBusListener, FTMDirectedAnnouncementManagerDelegate, FTMSendManagerDelegate, FTMOfferManagerDelegate, DataTransferDelegateSignalHandler, FileDiscoveryDelegateSignalHandler>

-(void)setUp;
-(void)tearDown;
-(void)testRequestAnnouncement;
-(void)testStopDataXfer;
-(void)testDataChunk;
-(void)testDataXferCancelled;
-(void)testAnnouncement;
-(void)testRequestOffer;
-(void)testRequestData;
-(void)testOffer;

// From FileTransferBusObjectDelegateSignalHandler
-(void)didReceiveDataChunkWithFileID:(AJNMessageArgument*)fileID startByte:(NSNumber*)startByte length:(NSNumber*)chunkLength andFileChunk:(AJNMessageArgument*)chunk inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender;
-(void)didReceiveStopDataXferWithFileID:(AJNMessageArgument*)fileID inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender;
-(void)didReceiveDataXferCancelledWithFileID:(AJNMessageArgument*)fileID inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender;
-(void)didReceiveAnnounceWithFileList:(AJNMessageArgument*)fileList andIsFileIDResponse:(BOOL)isFileIDResponse inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender;
-(void)didReceiverequestAnnouncementInSession:(AJNSessionId)sessionId fromSender:(NSString *)sender;

@end
