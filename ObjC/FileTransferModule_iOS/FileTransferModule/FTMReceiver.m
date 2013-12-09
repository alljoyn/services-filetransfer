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

#import "FTMReceiver.h"

@interface FTMReceiver()
{
    /*
     * Instance of dispatcher_queue_t.
     *
     * @warning *Note:* This is a private variable and is not meant to be modified directly.
     */
	dispatch_queue_t receiverDispatchQueue;
}

/*
 * Stores the instance of the AllJoyn bus attachment.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) AJNBusAttachment *busAttachment;

/*
 * Stores the local bus ID.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic) NSString *localBusID;

/*
 * Sets a delegate so the FTMReceiver can callback to the FTMAnnouncementManager.
 *
 * The FTMAnnouncementManagerDelegate will notify the FTMAnnouncementManager to handle the
 * announcement that was just received.
 */
@property (nonatomic, strong) id<FTMAnnouncementManagerDelegate> announcementManagerDelegate;

/*
 * Sets a delegate so the FTMReceiver can callback to the FTMSendManager.
 *
 * The FTMSendManagerDelegate will notify the FTMSendManager to immediately stop sending
 * the specified file.
 */
@property (nonatomic, strong) id<FTMSendManagerDelegate> sendManagerDelegate;

/*
 * Sets a delegate so the FTMReceiver can callback to the FTMReceiveManager.
 *
 * The FTMReceiveManagerDelegate will notify the FTMReceiveManager to handle the file chunk
 * that was just received.
 */
@property (nonatomic, strong) id<FTMReceiveManagerDelegate> receiveManagerDelegate;

/*
 * Sets a delegate so the FTMReceiver can callback to the FTMDirectedAnnouncementManager.
 *
 * The FTMDirectedAnnouncementManagerDelegate will notify the FTMDirectedAnnouncementManager to handle the
 * offer response from the remote session peer.
 */
@property (nonatomic, strong) id<FTMDirectedAnnouncementManagerDelegate> directedAnnouncementManagerDelegate;

@end

@implementation FTMReceiver

@synthesize busAttachment = _busAttachment;
@synthesize localBusID = _localBusID;
@synthesize announcementManagerDelegate = _announcementManagerDelegate;
@synthesize sendManagerDelegate = _sendManagerDelegate;
@synthesize receiveManagerDelegate = _receiveManagerDelegate;
@synthesize directedAnnouncementManagerDelegate = _directedAnnouncementManagerDelegate;
@synthesize handle = _handle;

-(id)initWithBusAttachment: (AJNBusAttachment *)busAttachment announcementManagerDelegate: (id<FTMAnnouncementManagerDelegate>)amDelegate sendManagerDelegate: (id<FTMSendManagerDelegate>)smDelegate receiveManagerDelegate: (id<FTMReceiveManagerDelegate>)rmDelegate andDirectedAnnouncementManagerDelegate: (id<FTMDirectedAnnouncementManagerDelegate>)damDelegate
{
    self = [super init];
	
	if (self)
    {
        receiverDispatchQueue = dispatch_queue_create("FTCReceiver", NULL);
		[self initializeReceiverWithBusAttachment: busAttachment announcementManagerDelegate: amDelegate sendManagerDelegate: smDelegate receiveManagerDelegate: rmDelegate andDirectedAnnouncementManagerDelegate: damDelegate];
	}
	
	return self;
}

-(void)initializeReceiverWithBusAttachment: (AJNBusAttachment *)busAttachment announcementManagerDelegate: (id<FTMAnnouncementManagerDelegate>)amDelegate sendManagerDelegate: (id<FTMSendManagerDelegate>)smDelegate receiveManagerDelegate: (id<FTMReceiveManagerDelegate>)rmDelegate andDirectedAnnouncementManagerDelegate: (id<FTMDirectedAnnouncementManagerDelegate>)damDelegate
{
    self.busAttachment = busAttachment;
    self.announcementManagerDelegate = amDelegate;
    self.sendManagerDelegate = smDelegate;
    self.receiveManagerDelegate = rmDelegate;
    self.directedAnnouncementManagerDelegate = damDelegate;
    
    if (busAttachment != nil)
    {
        //[self.busAttachment addMatchRule: @"type='signal',interface='org.alljoyn.Cops.FileDiscovery',member='announce'"];
        
        self.localBusID = [self.busAttachment uniqueName];
        
        [self.busAttachment registerDataTransferDelegateSignalHandler: self];
        [self.busAttachment registerFileDiscoveryDelegateSignalHandler: self];
    }
}

-(void)resetStateWithBusAttachment: (AJNBusAttachment *)busAttachment announcementManagerDelegate: (id<FTMAnnouncementManagerDelegate>)amDelegate sendManagerDelegate: (id<FTMSendManagerDelegate>)smDelegate receiveManagerDelegate: (id<FTMReceiveManagerDelegate>)rmDelegate andDirectedAnnouncementManagerDelegate: (id<FTMDirectedAnnouncementManagerDelegate>)damDelegate
{
    [self initializeReceiverWithBusAttachment: busAttachment announcementManagerDelegate: amDelegate sendManagerDelegate: smDelegate receiveManagerDelegate: rmDelegate andDirectedAnnouncementManagerDelegate: damDelegate];
}

// From FileTransferBusObjectDelegateSignalHandler
-(void)didReceiveAnnounceWithFileList:(AJNMessageArgument*)fileList andIsFileIDResponse:(BOOL)isFileIDResponse inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{
    NSLog(@"received file announcement from: %@", sender);
    
    NSArray *announcedFiles = [FTMMessageUtility descriptorArrayFromMessageArgument:fileList];
    
    if (sender != self.localBusID)
    {
        if (!isFileIDResponse)
        {
            dispatch_async(receiverDispatchQueue, ^{
                [self.announcementManagerDelegate handleAnnouncedFiles:announcedFiles fromPeer:sender];
            });
        }
        else
        {
            dispatch_async(receiverDispatchQueue, ^{
                [self.directedAnnouncementManagerDelegate handleOfferResponseForFiles:announcedFiles fromPeer:sender];
            });
        }
    }
}

-(void)didReceiverequestAnnouncementInSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{
    NSLog(@"got announcement request from %@", sender);
    
    if (sender != self.localBusID)
    {
        dispatch_async(receiverDispatchQueue, ^{
            [self.announcementManagerDelegate handleAnnouncementRequestFrom:sender];
        });
    }
}

-(void)didReceiveDataChunkWithFileID:(AJNMessageArgument*)fileID startByte:(NSNumber*)startByte length:(NSNumber*)chunkLength andFileChunk:(AJNMessageArgument*)chunk inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{
    //NSLog(@"got chunk from %@", sender);
    
    NSData *receivedFileID = [FTMMessageUtility fileIDFromMessageArgument: fileID];
    NSData *receivedChunk = [FTMMessageUtility dataFromMessageArgument: chunk];
    int receivedStartByte = [startByte intValue];
    int receivedLength = [chunkLength intValue];
    
    if (sender != self.localBusID)
    {
        dispatch_async(receiverDispatchQueue, ^{
            [self.receiveManagerDelegate handleChunkForFile:receivedFileID withStartByte: receivedStartByte andLength:receivedLength andFileData:receivedChunk];
        });
    }
}

-(void)didReceiveDataXferCancelledWithFileID:(AJNMessageArgument*)fileID inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{
    NSLog(@"got data xfer cancelled from %@", sender);
    
    NSData *receivedFileID = [FTMMessageUtility fileIDFromMessageArgument:fileID];
    
    if (sender != self.localBusID)
    {
        dispatch_async(receiverDispatchQueue, ^{
            [self.receiveManagerDelegate handleDataXferCancelledFrom:sender forFileWithID:receivedFileID];
        });
    }
}

-(void)didReceiveStopDataXferWithFileID:(AJNMessageArgument*)fileID inSession:(AJNSessionId)sessionId fromSender:(NSString *)sender
{
    NSLog(@"got stop data xfer from %@", sender);
    
    NSData *receivedFileID = [FTMMessageUtility fileIDFromMessageArgument:fileID];
    
    if (sender != self.localBusID)
    {
        dispatch_async(receiverDispatchQueue, ^{
            [self.sendManagerDelegate handleStopDataXferForFileWithID:receivedFileID fromPeer:sender];
        });
    }
}


@end
