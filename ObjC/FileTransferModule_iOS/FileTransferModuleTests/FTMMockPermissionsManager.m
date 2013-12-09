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

#import "FTMMockPermissionsManager.h"


@implementation FTMMockPermissionsManager

-(FTMFileDescriptor *)getUnitTestDummyFileDescriptor
{
    FTMFileDescriptor *descriptor = [[FTMFileDescriptor alloc ] init];
    
    descriptor.sharedPath = @"/sharedPath";
    descriptor.relativePath = @"/relativePath";
    descriptor.filename = @"testFile";
    descriptor.size = 1024;
    descriptor.owner = @"Gonzo";
        
    char theHash[] = "1234567890123456789";  // 19 plus null terminator
    int buffer_size = sizeof(theHash);
    descriptor.fileID = [[NSData alloc] initWithBytes: theHash length:buffer_size];
    
    return descriptor;
}

@end
