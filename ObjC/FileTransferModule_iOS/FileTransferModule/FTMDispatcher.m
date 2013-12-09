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

#import <dispatch/dispatch.h>
#import "FTMDispatcher.h"

@interface FTMDispatcher()
{
    /*
     * Instance of dispatcher_queue_t.
     *
     * @warning *Note:* This is a private variable and is not meant to be modified directly.
     */
	dispatch_queue_t dispatcherQueue;
}

/*
 * Stores an instance of the FTMTransmitter.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) FTMTransmitter *transmitter;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the sendManagerDeleagte.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) NSObject *sendManagerDelegateLock;

/*
 * Specifies a generic object used for thread synchronization. This object is used in the
 * set method of the directedAnnouncementManagerDeleagte.
 *
 * @warning *Note:* This is a private property and is not meant to be called directly.
 */
@property (nonatomic, strong) NSObject *damDelegateLock;

/*
 * Private helper function that is used to ensure the appropriate response to a given action
 * type. Most actions are just passed to the FTMTransmitter for processing but additional action
 * must be taken when a FTMFileIDResponse or FTMDataChunk action is encountered. 
 */
-(void)processAction: (FTMAction *)action;

@end

@implementation FTMDispatcher

@synthesize transmitter = _transmitter;
@synthesize sendManagerDelegateLock = _sendManagerDelegateLock;
@synthesize damDelegateLock = _damDelegateLock;

- (id)initWithBusObject: (FTMFileTransferBusObject *)busObject busAttachment: (AJNBusAttachment *)busAttachment andSessionID: (AJNSessionId)sessionID
{
	return [self initWithTransmitter: [[FTMTransmitter alloc] initWithBusObject: busObject busAttachment: busAttachment andSessionID: sessionID]];
}

-(id)initWithTransmitter: (FTMTransmitter *)transmitter
{
    self = [super init];
	
	if (self)
    {
		dispatcherQueue = dispatch_queue_create("FTCDispatcher", NULL);
        self.transmitter = transmitter;
        self.sendManagerDelegateLock = [[NSObject alloc] init];
        self.damDelegateLock = [[NSObject alloc] init];
        self.sendManagerDelegate = nil;
        self.damDelegateLock = nil;
	}
	
	return self;
}

-(void)insertAction: (FTMAction *)action
{
    dispatch_async(dispatcherQueue, ^{
        [self processAction: action];
    });
}

-(void)processAction: (FTMAction *)action
{
    if ([action isMemberOfClass: [FTMFileIDResponseAction class]])
    {
        if (self.directedAnnouncementManagerDelegate != nil)
        {
            [self.directedAnnouncementManagerDelegate generateFileDescriptor: (FTCFileIDResponseAction *)action];
        }
        return;
    }
    
    [action transmitActionWithTransmitter: self.transmitter];
    
    if ([action isMemberOfClass: [FTMDataChunkAction class]])
    {
        if (self.sendManagerDelegate != nil)
        {
            [self.sendManagerDelegate dataSent];
        }
    }
}

-(FTMStatusCode)transmitImmediately: (FTMAction *)action
{
    @try
    {
        return [action transmitActionWithTransmitter: self.transmitter];
    }
    @catch (NSException *ex)
    {
        NSLog(@"%@", [ex reason]);
    }
}

-(void)setDirectedAnnouncementManagerDelegate: (id<FTMDirectedAnnouncementManagerDelegate>)directedAnnouncementManagerDelegate
{
    @synchronized(self.damDelegateLock)
    {
        self->_directedAnnouncementManagerDelegate = directedAnnouncementManagerDelegate;
    }
}

-(void)setSendManagerDelegate: (id<FTMSendManagerDelegate>)sendManagerDelegate
{
    @synchronized(self.sendManagerDelegateLock)
    {
        self->_sendManagerDelegate = sendManagerDelegate;
    }
}

-(void)resetStateWithBusObject: (FTMFileTransferBusObject *)busObject busAttachment: (AJNBusAttachment *)busAttachment andSessionID: (AJNSessionId)sessionID
{
    self.transmitter = [[FTMTransmitter alloc] initWithBusObject: busObject busAttachment: busAttachment andSessionID: sessionID];
}

@end