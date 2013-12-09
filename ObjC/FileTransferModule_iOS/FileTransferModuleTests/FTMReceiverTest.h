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
#import "AllJoynFramework/AJNBusAttachment.h"
#import "FTMReceiver.h"
#import "FileTransferBusObject.h"
#import "FTMConstants.h"
#import "FTMMessageUtility.h"

@interface FTMReceiverTest : SenTestCase <AJNSessionPortListener, AJNSessionListener, AJNBusListener, FTMAnnouncementManagerDelegate,  FTMSendManagerDelegate, FTMDirectedAnnouncementManagerDelegate, FTMReceiveManagerDelegate>

-(void)setUp;
-(void)tearDown;
-(void)testRequestAnnouncement;
-(void)testStopDataXfer;
-(void)testDataChunk;
-(void)testDataXferCancelled;
-(void)testAnnouncement;

@end
