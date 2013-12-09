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

#import <Foundation/Foundation.h>
#import "SelectionMadeDelegate.h"
#import "FileTransferOperationsDelegate.h"
#import "TableViewController.h"

/*
 * This class is designated as the delegate when the user presses the "Unannounce" button and
 * receives a callback when the user selects the file name from the TableViewController. This 
 * class then delegates to the ViewController so the file can be removed from the announced files
 * list using the File Transfer Module.
 */
@interface UnannounceFileDelegate : NSObject <SelectionMadeDelegate>

@property (nonatomic, strong) id<FileTransferOperationsDelegate> operationsDelegate;

@end
