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

#import "FTMMessageUtility.h"
#import <alljoyn/Message.h>
#import <cstring>

//@interface FTCMessageUtility()
//+ (ajn::MsgArg *)msgArgFromFileDescriptor: (FTCFileDescriptor *)fd;
//@end

@implementation FTMMessageUtility

+ (AJNMessageArgument *)messageArgumentFromFileID: (NSData *)fileID
{
    assert([fileID length] == 20);
    
    return [FTMMessageUtility messageArgumentFromData: fileID];
}

+ (AJNMessageArgument *)messageArgumentFromData: (NSData *)data
{
    int length = [data length];
    
    const uint8_t *bytes = (const uint8_t *) data.bytes;
    ajn::MsgArg *msgArg = new ajn::MsgArg();
    
    msgArg->Set("ay", length, bytes);

    return [[AJNMessageArgument alloc] initWithHandle:msgArg shouldDeleteHandleOnDealloc:YES];
}

+ (AJNMessageArgument *)messageArgumentFromFileDescriptor: (FTMFileDescriptor *)fd
{
    ajn::MsgArg *msgArg = [FTMMessageUtility msgArgFromFileDescriptor: fd];
    
    return [[AJNMessageArgument alloc] initWithHandle:msgArg shouldDeleteHandleOnDealloc:YES];
}

+ (AJNMessageArgument *)messageArgumentFromFileList: (NSArray *)fileList
{
    int length = [fileList count];
    ajn::MsgArg *innerMsgArgs = new ajn::MsgArg[length];
    
    for (int i = 0; i < [fileList count]; i++)
    {
        FTMFileDescriptor *fd = [fileList objectAtIndex: i];
        ajn::MsgArg *msgArg = [FTMMessageUtility msgArgFromFileDescriptor:fd];
        innerMsgArgs[i] = *msgArg;
        delete msgArg;
    }
    
    ajn::MsgArg *outterMsgArg = new ajn::MsgArg();
    outterMsgArg->Set("a(ssssayi)", length, innerMsgArgs);
    
    return [[AJNMessageArgument alloc] initWithHandle:outterMsgArg shouldDeleteHandleOnDealloc:YES];
}

+ (NSData *)fileIDFromMessageArgument: (AJNMessageArgument *)message
{
    assert((((ajn::MsgArg *)message.handle)->v_scalarArray.numElements) == 20);
    
    return [FTMMessageUtility dataFromMessageArgument: message];
}

+ (NSData *)dataFromMessageArgument: (AJNMessageArgument *)message
{
    ajn::MsgArg *msgArg = (ajn::MsgArg *)message.handle;
    const uint8_t *bytes = msgArg->v_scalarArray.v_byte;
    NSData *data = [[NSData alloc] initWithBytes:bytes length:msgArg->v_scalarArray.numElements];
    
    return data;
}

+ (NSArray *)descriptorArrayFromMessageArgument: (AJNMessageArgument *) message
{
    ajn::MsgArg *outterMsgArg = (ajn::MsgArg *)message.handle;
    const ajn::MsgArg *innerMsgArgs = outterMsgArg->v_array.GetElements();
    
    NSMutableArray *fileList = [[NSMutableArray alloc] init];
    for (int i = 0; i < outterMsgArg->v_array.GetNumElements(); i++)
    {
        ajn::MsgArg msgArg = innerMsgArgs[i];
        FTMFileDescriptor *fd = [self descriptorFromMsgArg:&msgArg];
        [fileList addObject:fd];
    }    
    
    return fileList;
}

+ (FTMFileDescriptor *)descriptorFromMessageArgument: (AJNMessageArgument *)message
{
    ajn::MsgArg *msgArg = (ajn::MsgArg *)message.handle;   
    
    return [self descriptorFromMsgArg:msgArg];
}

+ (FTMFileDescriptor *) descriptorFromMsgArg: (ajn::MsgArg *) msgArg
{
    char *owner;
    char *filename;
    char *relativePath;
    char *sharedPath;
    uint8_t *fileIDBuffer;
    uint32_t size;
    size_t fileIDLength;
    msgArg->Get("(ssssayi)", &owner, &sharedPath, &relativePath, &filename, &fileIDLength, &fileIDBuffer, &size);
    
    FTMFileDescriptor *descriptor = [[FTMFileDescriptor alloc] init];
    descriptor.owner = [NSString stringWithUTF8String:owner];
    descriptor.filename = [NSString stringWithUTF8String:filename];
    descriptor.relativePath = [NSString stringWithUTF8String:relativePath];
    descriptor.sharedPath = [NSString stringWithUTF8String:sharedPath];
    descriptor.fileID = [[NSData alloc] initWithBytes:fileIDBuffer length:20];
    descriptor.size = size;
    
    return descriptor;
}

+ (ajn::MsgArg *)msgArgFromFileDescriptor: (FTMFileDescriptor *)fd
{
    const uint8_t *fileIDBuffer = (const uint8_t *)fd.fileID.bytes;    
    const char *owner = [fd.owner cStringUsingEncoding:[NSString defaultCStringEncoding]];
    const char *filename = [fd.filename cStringUsingEncoding:[NSString defaultCStringEncoding]];
    const char *relativePath = [fd.relativePath cStringUsingEncoding:[NSString defaultCStringEncoding]];
    const char *sharedPath = [fd.sharedPath cStringUsingEncoding:[NSString defaultCStringEncoding]];
    uint32_t size = fd.size;    
    
    ajn::MsgArg *msgArg = new ajn::MsgArg();
    msgArg->Set("(ssssayi)", owner, sharedPath, relativePath, filename, 20, fileIDBuffer, size);
    
    return msgArg;
}


@end
