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
#import <MobileCoreServices/UTCoreTypes.h>
#import <UIKit/UIKit.h>
#import "AllJoynManager.h"
#import "TableViewController.h"
#import "ConnectionSateChangedDelegate.h"
#import "SelectionMadeDelegate.h"
#import "FileTransferModule/FTMFileTransferModule.h"
#import "FTMFileAnnouncementReceivedDelegate.h"
#import "FTMFileDescriptor.h"
#import "FileTransferOperationsDelegate.h"
#import "AnnouncementDelegate.h"
#import "UnannounceFileDelegate.h"
#import "RequestFileDelegate.h"
#import "OfferFileDelegate.h"
#import "RequestOfferDelegate.h"
#import "AnnounceViewController.h"

/*
 * This class specifies the public properties and functions that are associated with the sample applications 
 * main view. 
 */
@interface ViewController : UIViewController <ConnectionSateChangedDelegate, FTMFileAnnouncementReceivedDelegate, FTMFileCompletedDelegate, FTMRequestDataReceivedDelegate, FTMOfferReceivedDelegate, FTMUnannouncedFileRequestDelegate, FileTransferOperationsDelegate>

@property (weak, nonatomic) IBOutlet UIButton *hostButton;
@property (weak, nonatomic) IBOutlet UIButton *joinButton;
@property (weak, nonatomic) IBOutlet UIButton *announceButton;
@property (weak, nonatomic) IBOutlet UIButton *unannounceButton;
@property (weak, nonatomic) IBOutlet UIButton *requestButton;
@property (weak, nonatomic) IBOutlet UIButton *offerButton;
@property (weak, nonatomic) IBOutlet UIButton *requestOfferButton;
@property (weak, nonatomic) IBOutlet UITextView *textView;
@property (weak, nonatomic) IBOutlet UIProgressView *receiveProgressBar;
@property (weak, nonatomic) IBOutlet UIProgressView *sendProgressBar;
@property (weak, nonatomic) IBOutlet UIButton *pauseReceiveButton;
@property (weak, nonatomic) IBOutlet UIButton *cancelReceiveButton;
@property (weak, nonatomic) IBOutlet UIButton *cancelSendButton;

-(IBAction)hostSessionClicked: (id)sender;
-(IBAction)joinSessionClicked: (id)sender;
-(IBAction)pauseReceiveButtonClicked:(id)sender;
-(IBAction)cancelReceiveButtonClicked:(id)sender;
-(IBAction)cancelSendButtonClicked:(id)sender;

@end
