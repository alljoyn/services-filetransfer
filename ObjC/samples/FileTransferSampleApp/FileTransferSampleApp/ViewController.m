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

#import "ViewController.h"

@interface ViewController ()

@property (nonatomic, strong) AllJoynManager *allJoynManager;
@property (nonatomic, strong) FTMFileTransferModule *ftModule;
@property (nonatomic, strong) AnnouncementDelegate *announcementController;
@property (nonatomic, strong) UnannounceFileDelegate *unannounceFileController;
@property (nonatomic, strong) RequestFileDelegate *requestFileController;
@property (nonatomic, strong) OfferFileDelegate *offerController;
@property (nonatomic, strong) RequestOfferDelegate *requestOfferContoller;

-(void)createFolderStructure;
-(void)hostSession;
-(void)joinSession;
-(void)display: (NSString *)text;
-(void)hideButton: (UIButton*)button;
-(void)showButton: (UIButton*)button;
-(void)showPicturePicker: (id<UINavigationControllerDelegate, UIImagePickerControllerDelegate>)delegate;
-(void)initializeFileTransferComponent;
-(void)monitorSendProgress;
-(void)monitorReceiveProgress;

@end

@implementation ViewController

@synthesize allJoynManager = _allJoynManager;
@synthesize textView = _textView;
@synthesize ftModule = _ftModule;
@synthesize announcementController = _announcementController;
@synthesize unannounceFileController = _unannounceFileController;
@synthesize requestFileController = _requestFileController;
@synthesize offerController = _offerController;
@synthesize requestOfferContoller = _requestOfferContoller;

/* Section: ViewDidLoad */

/*
 * Called when the view is loaded successfully and initializes class variables.
 */
- (void)viewDidLoad
{
    [super viewDidLoad];
	
    self.allJoynManager = [[AllJoynManager alloc] init];
    self.allJoynManager.connectionStateChangedDelegate = self;
    
    self.announcementController = [[AnnouncementDelegate alloc] init];
    [self.announcementController setOperationsDelegate: self];
    self.unannounceFileController = [[UnannounceFileDelegate alloc] init];
    [self.unannounceFileController setOperationsDelegate: self];
    self.requestFileController = [[RequestFileDelegate alloc] init];
    [self.requestFileController setOperationsDelegate: self];
    self.offerController = [[OfferFileDelegate alloc] init];
    [self.offerController setOperationsDelegate: self];
    self.requestOfferContoller = [[RequestOfferDelegate alloc] init];
    [self.requestOfferContoller setOperationsDelegate: self];
    
    [self createFolderStructure];
}

/* End Section: ViewDidLoad */

/* Section: Button Event Handlers */

/*
 * Event handler when the "Host" button is clicked and creates/hosts an AllJoyn session.
 */
-(IBAction)hostSessionClicked: (id)sender
{
    [self hostSession];
}

/*
 * Event handler when the "Join" button is clicked and attempts to joing an exisiting AllJoyn session.
 */
-(IBAction)joinSessionClicked: (id)sender
{    
    [self joinSession];
}

/*
 * Event handler when "Pause" button for a file being received is clicked. This handler pauses the
 * transfer of the file currently being received.
 */
- (IBAction)pauseReceiveButtonClicked:(id)sender
{
    NSArray *receiveList = [self.ftModule receiveProgressList];
    FTMProgressDescriptor *descriptor = receiveList[0];
    
    [self.ftModule pauseReceivingFileWithID: descriptor.fileID];
    
    [self hideButton: self.pauseReceiveButton];
}

/*
 * Event handler when "Cancel" button for a file being received is clicked. This handler cancels the
 * transfer of the file currently being received.
 */
- (IBAction)cancelReceiveButtonClicked:(id)sender
{
    NSArray *receiveList = [self.ftModule receiveProgressList];
    FTMProgressDescriptor *descriptor = receiveList[0];
    
    [self.ftModule cancelReceivingFileWithID: descriptor.fileID];
}

/*
 * Event handler when "Cancel" button for a file being sent is clicked. This handler cancels the
 * transfer of the file currently being sent.
 */
- (IBAction)cancelSendButtonClicked:(id)sender
{
    NSArray *sendList = [self.ftModule sendingProgressList];
    FTMProgressDescriptor *descriptor = sendList[0];
    
    [self.ftModule cancelSendingFileWithID: descriptor.fileID];
}

/* End Section: Button Event Handlers */

/* Section: Segue Functions for TableViewController */

/*
 * Prepares the segue to the TableViewController from the ViewController and calls the appropriate function
 * to populate the table with the data that corresponds to the current file transfer operation.
 */
- (void)prepareForSegue: (UIStoryboardSegue *)segue sender:(id)sender
{
    UIViewController *controller = (UIViewController *)segue.destinationViewController;
    
    if([segue.identifier isEqualToString: @"showRequestFileTableView"])
    {    
        [self prepareRequestFileTableView: (TableViewController *)controller];        
    }
    else if([segue.identifier isEqualToString: @"showUnannounceTableView"])
    {
        [self prepareUnannounceTableView: (TableViewController *)controller];
    }
    else if([segue.identifier isEqualToString: @"showOfferTableView"])
    {
        [self prepareOfferPeersTableView: (TableViewController *)controller];
    }
    else if ([segue.identifier isEqualToString: @"showRequestOfferTableView"])
    {
        [self prepareRequestOfferTableView: (TableViewController *)controller];
    }
    else if ([segue.identifier isEqualToString: @"showAnnounceView"])
    {
        ((AnnounceViewController *)controller).announcementController = self.announcementController;
    }
}

/*
 * Prepares the TableViewController with the names of the files that have been announced to us by remote
 * session peers and are available for transfer. This function also sets the delegate so the RequestFile
 * controller is called when the user selects file name in the table. The selected file name will be
 * requested from the file owner.
 */
- (void)prepareRequestFileTableView: (TableViewController *)controller
{
    NSArray *availableFiles = [self.ftModule availableRemoteFiles];
    
    NSMutableArray *availableFilenames = [[NSMutableArray alloc] initWithCapacity: availableFiles.count];
    for (int i = 0; i < availableFiles.count; i++)
    {
        [availableFilenames addObject: ((FTMFileDescriptor*)availableFiles[i]).filename];
    }
    
    controller.selectionMadeDelegate = self.requestFileController;
    controller.stringsToDisplay = availableFilenames;
}

/*
 * Prepares the TableViewController with the names of the files that have been announced by us to remote
 * session peers and are available for transfer. This function also sets the delegate so the UnannounceFile
 * controller is called when the user selects a file name in the table. The selected file name will be
 * removed from the announced files list.
 */
- (void)prepareUnannounceTableView: (TableViewController *)controller
{
    NSArray *announcedFiles = [self.ftModule announcedLocalFiles];
    
    NSMutableArray *announcedFilenames = [[NSMutableArray alloc] initWithCapacity: announcedFiles.count];
    for (int i = 0; i < announcedFiles.count; i++)
    {
        [announcedFilenames addObject: ((FTMFileDescriptor*)announcedFiles[i]).filename];
    }
    
    controller.selectionMadeDelegate = self.unannounceFileController;
    controller.stringsToDisplay = announcedFilenames;
}

/*
 * Prepares the TableViewController with the all peer names that are part of our AllJoyn session. This function
 * also sets the delegate so the OfferFileController is called when the user selects a peer name from the table.
 * The selected peer name will receive the file offer.
 */
- (void)prepareOfferPeersTableView: (TableViewController *)controller
{
    controller.selectionMadeDelegate = self.offerController;
    controller.stringsToDisplay = self.allJoynManager.peers;
}

/*
 * Prepares the TableViewController with the all peer names that are part of our AllJoyn session. This function
 * also sets the delegate so the RequestOfferController is called when the user selects a peer name from the table.
 * The selected peer name will receive the offer request.
 */
-(void)prepareRequestOfferTableView: (TableViewController *)controller
{
    controller.selectionMadeDelegate = self.requestOfferContoller;
    controller.stringsToDisplay = self.allJoynManager.peers;
}

/* End Section: Segue Functions for TableViewController */

/* Section: ConnectionStateChangedDelegate Implementation */

#pragma mark AllJoynManager delegate methods
/*
 * Updates the UI when a connection state changed event occurs. If the state is connected, this means we have connected
 * to an AllJoyn session and we need to hide the host/join buttons and enable all buttons that deal with file transfer
 * operations. However, if the state is disconnected, the opposite behavior takes place.
 */
- (void)connectionStateChanged: (ConnectionState)state
{
    if (state == CONNECTED)
    {
        [self display: @"Connected!"];
        
        [self hideButton: self.hostButton];
        [self hideButton: self.joinButton];
        
        [self showButton: self.announceButton];
        [self showButton: self.unannounceButton];
        [self showButton: self.requestButton];
        [self showButton: self.offerButton];
        [self showButton: self.requestOfferButton];
        
        [self initializeFileTransferComponent];
    }
    else if (state == DISCONNECTED)
    {
        [self display: @"Disconnected!"];
        
        [self showButton: self.hostButton];
        [self showButton: self.joinButton];
        
        [self hideButton: self.announceButton];
        [self hideButton: self.unannounceButton];
        [self hideButton: self.requestButton];
        [self hideButton: self.offerButton];
        [self hideButton: self.requestOfferButton];
        
        [self.ftModule uninitialize];
    }
}

/* End Section: ConnectionStateChangedDelegate Implementation */

/* Section FileTransferModule Delegate Method Implementations */

/*
 * Implements the required function of the FTMFileAnnouncementReceivedDelegate of the FileTransferModule. This
 * function is called when we receive an announcement from a remote session peer and the UI text field is updated
 * when an announcement is received.
 */
- (void)receivedAnnouncementForFiles: (NSArray *)fileList andIsFileIDResponse: (BOOL)isFileIDResponse
{
    [self display: @"Received File Announcement!"];
}

/*
 * Implements the required function of the FTMFileCompletedDelegate of the FileTransferModule. This function is
 * called when a file transfer has been completed and updates the UI text field to notify the user that the file
 * transfer has completed. The receive progress bar is filled completely and the pause/cancel buttons are hidden.
 */
-(void)fileCompletedForFile: (NSString *)fileName withStatusCode: (int)statusCode
{
    [self display: [[NSString alloc] initWithFormat: @"completed file: %@", fileName] ];
    
    dispatch_async(dispatch_get_main_queue(),
    ^{
        self.receiveProgressBar.progress = 1.0f;
    });
    
    if (statusCode == FTMCancelled)
    {
        [self hideButton: self.pauseReceiveButton];
        [self hideButton: self.cancelReceiveButton];
    }
}

/*
 * Implements the required function of the FTMOfferReceivedDelegate of the FileTransferModule. This function is called
 * when a file offer is received from a remote session peer, updates several UI components, and spawns a new thread so
 * the progress of the file transfer can be monitored in the background.
 */
-(BOOL)acceptFileOfferFrom: (NSString *)peer forFile: (FTMFileDescriptor *)file
{
    [self display: @"received offer...accepting"];
    [self showButton: self.pauseReceiveButton];
    [self showButton: self.cancelReceiveButton];
    
    [self performSelectorInBackground: @selector(monitorReceiveProgress) withObject: nil];
    
    return true;
}

/*
 * Implements the required function of the FTMRequestDataReceivedDelegate of the FileTransferModule. This function is
 * called when a remote session peer requests a file that we have announced, updates several UI components, and spawns
 * a new thread so the progress of the file transfer can be monitored in the background.
 */
-(void) fileRequestReceived: (NSString *)fileName
{
    [self display: [[NSString alloc] initWithFormat: @"sending file: %@", fileName]];
    [self showButton: self.cancelSendButton];
    
    dispatch_async(dispatch_get_main_queue(),
    ^{
        self.sendProgressBar.progress = 0;
    });
    
    [self performSelectorInBackground: @selector(monitorSendProgress) withObject: nil];
}

/*
 * Implements the required function of the FTMUnannouncedFileRequetsDelegate of the FileTransferModule. This function is
 * called when an offer reuest is received from a remote session peer. This function only returns yes because we want to
 * demonstrate the request offer functionality of the file transfer module. Returning yes enables the request offer
 * functionality within the component. 
 */
-(BOOL)allowUnannouncedRequestsForFileWithPath: (NSString *) filePath
{
    return YES;
}

/* End Section FileTransferModule Delegate Method Implementations */

/* Section: FileTransferOperationsDelegate Implementation */

/*
 * Announces the file with the specified path using the FileTransferModule.
 */
-(void)announceFileWithPath: (NSString *)path
{
    NSArray *pathArray = [[NSArray alloc] initWithObjects:path, nil];
    [self.ftModule announceFilePaths:pathArray];
}

/*
 * Unannounces the file with the specified name using the FileTransferModule.
 */
-(void)unannounceFileWithName: (NSString *)fileName
{
    NSArray *announcedFiles = [self.ftModule announcedLocalFiles];
    
    FTMFileDescriptor *desiredFile;
    for (int i = 0; i < announcedFiles.count; i++)
    {
        if ([fileName isEqualToString: ((FTMFileDescriptor*)announcedFiles[i]).filename])
        {
            desiredFile = announcedFiles[i];
        }
    }
    
    NSString* path = desiredFile.sharedPath;
    path = [path stringByAppendingPathComponent: desiredFile.relativePath];
    path = [path stringByAppendingPathComponent: desiredFile.filename];
    
    NSArray *pathList = [[NSArray alloc] initWithObjects: path, nil];
    [self.ftModule stopAnnounceFilePaths: pathList];
}

/*
 * Requests the file with the specified name from a remote session peer using the FileTransferModule.
 */
-(void)requestFileWithName: (NSString *)fileName
{
    NSArray *availableFiles = [self.ftModule availableRemoteFiles];
    
    FTMFileDescriptor *desiredFile;
    for (int i = 0; i < availableFiles.count; i++)
    {
        if ([fileName isEqualToString: ((FTMFileDescriptor*)availableFiles[i]).filename])
        {
            desiredFile = availableFiles[i];
        }
    }
    
    [self.ftModule requestFileFromPeer: desiredFile.owner withFileID: desiredFile.fileID andSaveName: desiredFile.filename];
    
    //Call monitor receive progress
    [self showButton: self.cancelReceiveButton];
    [self showButton: self.pauseReceiveButton];
    [self performSelectorInBackground: @selector(monitorReceiveProgress) withObject: nil];
}
/*
 * Displays the image picker for the user to select the file they wish to offer.
 */
-(void)prepareOfferFile
{
    [self showPicturePicker: self.offerController];
}

/*
 * Sends the file offer to the specified peer using the FileTransferModule.
 */
-(void)offerFileWithFilePath: (NSString *)filePath toPeer: (NSString *)peer
{
    [self.ftModule offerFileToPeer: peer withFilePath: filePath andTimeoutMillis: 1000];
}

/*
 * Sends the offer request to the specified peer for the specified file using the FileTransferModule.
 */
-(void)sendOfferRequestForFileWithPath: (NSString *)filePath toPeer: (NSString *)peer
{
    [self.ftModule requestOfferFromPeer: peer forFilePath: filePath];
}

/* End Section: FileTransferOperationsDelegate Implementation */

/* Section: Private Helper Functions */

/*
 * Private function that creates (if it does not already exist) a sending (for files announced/offered) and receiving (for files
 * announced to us) folders to store files.
 */
-(void)createFolderStructure
{
    NSString *documentsDirectory = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex: 0];
    NSFileManager *fm = [NSFileManager defaultManager];
    NSString *directoryPath = [documentsDirectory stringByAppendingPathComponent: @"Sending"];
    
    NSError *error;
    if (![fm fileExistsAtPath: directoryPath])
    {
        if (![fm createDirectoryAtPath: directoryPath withIntermediateDirectories: NO attributes: nil error: &error])
        {
            NSLog(@"Failed to create sending directory with error: %@", error);
        }
    }
    
    directoryPath = [documentsDirectory stringByAppendingPathComponent: @"Receiving"];
    
    if (![fm fileExistsAtPath: directoryPath])
    {
        if (![fm createDirectoryAtPath: directoryPath withIntermediateDirectories: NO attributes: nil error: &error])
        {
            NSLog(@"Failed to create sending directory with error: %@", error);
        }
    }
}

/*
 * Private function that makes necessary UI changes and creates an AllJoyn session.
 */
- (void)hostSession
{
    [self display: @"Hosting..."];
    [self hideButton: self.hostButton];
    [self showButton: self.joinButton];
    
    [self.allJoynManager createSession];
}

/*
 * Private function that makes necessary UI changes and attempts to join an existing AllJoyn session.
 */
-(void)joinSession
{
    [self display: @"Joining..."];
    [self hideButton: self.joinButton];
    [self showButton: self.hostButton];
    
    [self.allJoynManager joinSession];
}

/*
 * Private helper function that takes the provided text and displays it in the UI text view. The text view is used
 * to provide the user with state messages.
 */
-(void)display: (NSString *)text
{
    dispatch_async(dispatch_get_main_queue(),
    ^{
        NSString *appendText = [[NSString alloc] initWithFormat:@"%@%@", text, @"\n"];
        self.textView.text = [self.textView.text stringByAppendingString: appendText];
                       
        [self.textView scrollRangeToVisible:NSMakeRange([self.textView.text length], 0)];
    });
}

/*
 * Private helper function that disables the specified button.
 */
-(void)hideButton: (UIButton *)button
{
    dispatch_async(dispatch_get_main_queue(),
    ^{
        button.enabled = NO;
        button.alpha = .5;
    });
}

/*
 * Private helper function that enables the specified button.
 */
-(void)showButton: (UIButton *)button
{
    dispatch_async(dispatch_get_main_queue(),
    ^{
        button.enabled = YES;
        button.alpha = 1;
    });
}

/*
 * Private helper function that displays an image picker for the user. This function is called when the user wishes
 * to announce or offer a file and the image picker allows them to select the file they want to announce or offer.
 */
-(void)showPicturePicker: (id<UINavigationControllerDelegate, UIImagePickerControllerDelegate>)delegate
{
    UIImagePickerControllerSourceType sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    NSArray *mediaTypes = [UIImagePickerController availableMediaTypesForSourceType: sourceType];
    
    if ([UIImagePickerController isSourceTypeAvailable: sourceType] && [mediaTypes count] > 0)
    {
        UIImagePickerController *picker = [[UIImagePickerController alloc] init];
        picker.mediaTypes = mediaTypes;
        picker.delegate = delegate;
        picker.sourceType = sourceType;
        
        [self presentViewController: picker animated: YES completion: nil];
    }
    else
    {
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle: @"Error accessing media" message: @"Device doesn't support that media source" delegate: nil cancelButtonTitle: @"Cancel" otherButtonTitles: nil];
        [alert show];
    }
}

/*
 * Private helper function that initializes the File Transfer Module (FTM) after a session has been created and at
 * least one peer has joined. This function creates the FTM using the existing AllJoyn session and sets the necessary
 * delegates to receive callbacks from the component when various events occur.
 */
-(void)initializeFileTransferComponent
{
    self.ftModule = [[FTMFileTransferModule alloc] initWithBusAttachment: self.allJoynManager.bus andSessionID: self.allJoynManager.sessionID];
    
    [self.ftModule setFileAnnouncementReceivedDelegate: self];
    [self.ftModule setFileCompletedDelegate: self];
    [self.ftModule setRequestDataReceivedDelegate: self];
    [self.ftModule setOfferReceivedDelegate: self];
    [self.ftModule setUnannouncedFileRequestDelegate: self];
    
    NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex: 0];
    path = [path stringByAppendingPathComponent: @"Receiving"];
    [self.ftModule setDefaultSaveDirectory: path];
    [self.ftModule setChunkSize: 10000];
}

/*
 * Private helper function that monitors the send progress of the current file transfer and
 * updates the send progress bar so the user can visually see the current progress of the
 * file they are currently sending.
 */
-(void)monitorSendProgress
{
    NSArray *sendProgressDescriptorArray = [self.ftModule sendingProgressList];
    
    while ([sendProgressDescriptorArray count] > 0)
    {
        FTMProgressDescriptor *descriptor = [sendProgressDescriptorArray objectAtIndex: 0];
        
        dispatch_async(dispatch_get_main_queue(),
        ^{
            self.sendProgressBar.progress = (float)descriptor.bytesTransferred / (float)descriptor.fileSize;
        });
        
        [NSThread sleepForTimeInterval: 0.1f];
        sendProgressDescriptorArray = [self.ftModule sendingProgressList];
    }
    
    dispatch_async(dispatch_get_main_queue(),
    ^{
        self.sendProgressBar.progress = 1.0f;
        [self hideButton: self.cancelSendButton];
    });
}

/*
 * Private helper function that monitors the receive progress of the current file transfer and
 * updates the receive progress bar so the user can visually see the current progress of the
 * file they are currently receiving.
 */
-(void)monitorReceiveProgress
{
    [NSThread sleepForTimeInterval: 0.1f];
    NSArray *receiveProgressDescriptorArray = [self.ftModule receiveProgressList];
    
    while ([receiveProgressDescriptorArray count] > 0)
    {
        FTMProgressDescriptor *descriptor = [receiveProgressDescriptorArray objectAtIndex: ([receiveProgressDescriptorArray count] - 1)];
        
        dispatch_async(dispatch_get_main_queue(),
        ^{
            self.receiveProgressBar.progress = (float)descriptor.bytesTransferred / (float)descriptor.fileSize;
        });
        
        [NSThread sleepForTimeInterval: 0.1f];
        receiveProgressDescriptorArray = [self.ftModule receiveProgressList];
    }
    
    dispatch_async(dispatch_get_main_queue(),
    ^{
        [self hideButton: self.pauseReceiveButton];
        [self hideButton: self.cancelReceiveButton];
        self.receiveProgressBar.progress = 1.0f;
    });
}

/* End Section: Private Helper Functions */

@end
