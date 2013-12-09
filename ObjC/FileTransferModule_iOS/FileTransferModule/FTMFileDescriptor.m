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

#import "FTMFileDescriptor.h"

@implementation FTMFileDescriptor

@synthesize owner = _owner;
@synthesize sharedPath = _sharedPath;
@synthesize relativePath = _relativePath;
@synthesize filename = _filename;
@synthesize fileID = _fileID;
@synthesize size = _size;

- (id)init
{
	return [self initWithFileDescriptor: nil];	
}

- (id)initWithFileDescriptor: (FTMFileDescriptor *)fileDescriptor
{
	self = [super init];
	
	if (self && fileDescriptor) {
		self.owner = fileDescriptor.owner;
		self.sharedPath = fileDescriptor.sharedPath;
		self.relativePath = fileDescriptor.relativePath;
		self.filename = fileDescriptor.filename;
		self.fileID = fileDescriptor.fileID;
		self.size = fileDescriptor.size;
	}
	
	return self;
}

- (BOOL)isEqual: (id)other
{
	if (other == nil) {
		return NO;
	}

	if (other == self) {
		return YES;
	}
	
	if (![other isKindOfClass: [self class]]) {
		return NO;
	}
	
	return [self isEqualToFileDescriptor: other];
}

- (BOOL)isEqualToFileDescriptor: (FTMFileDescriptor *)that
{
	return
		[self.owner isEqual: that.owner] &&
		[self.sharedPath isEqual: that.sharedPath] &&
		[self.relativePath isEqual: that.relativePath] &&
		[self.fileID isEqual: that.fileID] &&
		self.size == that.size;
}

- (NSUInteger)hash
{
	const int prime = 31;
	int result = 1;
	
	result = (prime * result) + (self.owner ? [self.owner hash] : 0);
	result = (prime * result) + (self.sharedPath ? [self.sharedPath hash] : 0);
	result = (prime * result) + (self.relativePath ? [self.relativePath hash] : 0);
	result = (prime * result) + (self.filename ? [self.filename hash] : 0);
	result = (prime * result) + (self.fileID ? [self.fileID hash] : 0);
	result = (prime * result) + self.size;
	
	return result;	
}

@end