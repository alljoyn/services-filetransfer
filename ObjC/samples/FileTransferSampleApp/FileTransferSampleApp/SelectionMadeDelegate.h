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

@class TableViewController;

/*
 * This delegate is used extract the selection that the user made on the TableViewController. This
 * can either be a peer name (i.e. request offer, offer file, etc.) or a file name (i.e. request file).
 * This delegate allows us to specify a different controller class as a delegate depending on which
 * table type we are displaying. This, in turn, removes case statements and allows us to just process 
 * the data as needed without needing to determine a course of action first.
 */
@protocol SelectionMadeDelegate <NSObject>

@required
-(void)selectionMade: (NSString*)selectedString on: (TableViewController *)tableViewController;

@end
