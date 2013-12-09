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

#import "AnnouncementDelegate.h"

@interface AnnouncementDelegate()

@property (nonatomic, strong) UIImage *chosenImage;

-(void)showAlertView;

@end

@implementation AnnouncementDelegate

@synthesize chosenImage = _chosenImage;
@synthesize operationsDelegate = _operationsDelegate;

- (void)announceDirectoryWithPath: (NSString*)path
{
    if (self.operationsDelegate != nil)
    {
        [self.operationsDelegate announceFileWithPath: path];
    }
}

/*
 * Callback method for the UIImagePickerController thats extracts the image the user selected from
 * the image picker. This function stores the selected image in a property and then calls a private
 * method to allow the user to enter the file name.
 */
#pragma mark UIImagePickerController delegate methods
-(void)imagePickerController: (UIImagePickerController *)picker didFinishPickingMediaWithInfo: (NSDictionary *)info
{
    NSString *lastChosenMediaType = [[NSString alloc] initWithString: [info objectForKey: UIImagePickerControllerMediaType]];
    
    if ([lastChosenMediaType isEqual: (NSString *)kUTTypeImage])
    {
        self.chosenImage = [info objectForKey: UIImagePickerControllerOriginalImage];
        [self showAlertView];
    }
    else
    {
        NSLog(@"Unsupported type selected");
    }
    
    [picker dismissViewControllerAnimated: YES completion: nil];
}

/*
 * Callback method for the UIImagePickerController that dismisses the picker when the cancel button
 * is pressed and does not take any further action.
 */
-(void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    [picker dismissViewControllerAnimated: YES completion: nil];
}

/*
 * Shows an alert view to the user and allows them to enter the name of the file they are announcing.
 */
-(void)showAlertView
{
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle: @"Stop" message: @"Please enter a name for the file:" delegate: self cancelButtonTitle: @"Continue" otherButtonTitles:
                          nil];
    alert.alertViewStyle = UIAlertViewStylePlainTextInput;
    UITextField *alertTextField = [alert textFieldAtIndex: 0];
    alertTextField.keyboardType = UIKeyboardTypeDefault;
    alertTextField.placeholder = @"Enter a file name";
    
    [alert show];
}

/*
 * Callback method from the UIAlertView that extracts the file name entered and delegates back to the
 * ViewController class so the announcement can be sent.
 */
#pragma mark UIAlertView delegate methods
-(void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    NSString *fileName = [[NSString alloc] initWithString: [alertView textFieldAtIndex: 0].text];
    fileName = ([fileName hasSuffix: @".png"]) ? fileName : [fileName stringByAppendingString: @".png"];
    
    NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex: 0];
    path = [path stringByAppendingPathComponent: @"Sending"];
    path = [path stringByAppendingPathComponent: fileName];
    
    NSData *chosenImageData = UIImagePNGRepresentation(self.chosenImage);
    [chosenImageData writeToFile: path atomically: YES];
    
    if (self.operationsDelegate != nil)
    {
        [self.operationsDelegate announceFileWithPath: path];
    }
}

@end
