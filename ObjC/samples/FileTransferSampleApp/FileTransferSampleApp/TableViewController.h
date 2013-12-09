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

#import <UIKit/UIKit.h>
#import "SelectionMadeDelegate.h"

/*
 * This class extends UITableViewController and allows us to specify an array of strings to display,
 * a delegate to call when the user selects a given cell, and table type. This allows tremendous 
 * flexibility to reuse the table regardless of whether we are displaying peer names of file names.
 * The delegate allows us to specify a controller class that will receive the callback when a user
 * selects a row.
 */
@interface TableViewController : UITableViewController

@property (nonatomic, strong) NSArray *stringsToDisplay;
@property (nonatomic, strong) id<SelectionMadeDelegate> selectionMadeDelegate;

@end
