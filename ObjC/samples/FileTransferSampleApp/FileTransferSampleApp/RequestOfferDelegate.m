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

#import "RequestOfferDelegate.h"

@interface RequestOfferDelegate()

@property (nonatomic, strong) NSString *peer;

-(void)showRequestOfferAlertView;

@end

@implementation RequestOfferDelegate

@synthesize operationsDelegate = _operationsDelegate;

/*
 * Called when the user selects the peer name from the TableViewController. The selected peer
 * name is saved in a property and a private function is called to show the alert view.
 */
-(void)selectionMade: (NSString*)selectedString on: (TableViewController *)tableViewController
{
    self.peer = selectedString;
    [self showRequestOfferAlertView];
}

/*
 * Shows an alert view to the user and allows them to enter the relative path to the file they
 * are requesting.
 */
-(void)showRequestOfferAlertView
{
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle: @"Request Offer" message: @"Please enter a relative path for the file:" delegate: self cancelButtonTitle: @"Continue" otherButtonTitles: nil];
    alert.alertViewStyle = UIAlertViewStylePlainTextInput;
    UITextField *alertTextField = [alert textFieldAtIndex: 0];
    alertTextField.keyboardType = UIKeyboardTypeDefault;
    alertTextField.placeholder = @"Enter a realtive path";
    
    [alert show];
}

/*
 * Callback method from the UIAlertView that extracts the file path entered and delegates back to the
 * ViewController class so the offer request can be sent.
 */
#pragma mark UIAlertView delegate methods
-(void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    NSString *filePath = [[NSString alloc] initWithString: [alertView textFieldAtIndex: 0].text];
    
    if (self.operationsDelegate != nil)
    {
        [self.operationsDelegate sendOfferRequestForFileWithPath: filePath toPeer: self.peer];
    }
}

@end
