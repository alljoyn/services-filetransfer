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
#import "FTMStatusCode.h"
#import "FTMReceiveManagerDelegate.h"
#import "FTMFileCompletedDelegate.h"
#import "FTMDispatcher.h"
#import "FTMFileSystemAbstraction.h"
#import "FTMPermissionManager.h"
#import "FTMFileStatus.h"
#import "FTMProgressDescriptor.h"
#import "FTMAction.h"

/**
 * The FTMReceiveManager is a major piece of the FTMFileTransferModule. The FTMReceiveManager's
 * major responsibilities include: building file requests for remote session peers, handling incoming
 * file chunks and saving them,  executing pause and cancel requests made by the file receiver,
 * and handling cancelled transfers by the remote session peer. From the file receiving perspective,
 * this component is the driving force behind receiving files from remote session peers.
 * 
 * @warning *Note:* This class is not intended to be used directly. All of the
 * supported functionality of this library is intended to be accessed through the
 * FTMFileTransferModule class.
 */
@interface FTMReceiveManager : NSObject <FTMReceiveManagerDelegate>

/** @name Class Properties */

/**
 * Sets a delegate for the implementing class of the FTMFileCompletedDelegate protocol.
 *
 * The FTMFileCompletedDelegate will notify the receiving class when a file transfer has
 * completed.
 *
 * @warning *Note:* This property is write only and does not provide a getter.
 */
@property (nonatomic, strong) id<FTMFileCompletedDelegate> fileCompletedDelegate;

/**
 * Stores the absolute path to the default save directory.
 *
 * A default location to save files is specified at startup and can be changed at any time
 * by the user.
 *
 * @return The absolute path to the default save directory.
 */
@property (nonatomic) NSString *defaultSaveDirectory;

/**
 * Stores the maximum file chunk size.
 *
 * The default file chunk size is 1024 and can be changed at any time by the user.
 *
 * @return Maximum file chunk size.
 */
@property (nonatomic) int maxChunkSize;

/** @name Creating FTMReceiveManager */

/**
 * Constructs an instance of the FTMReceiveManager.
 *
 * @param dispatcher Instance of the FTMDispatcher.
 * @param permissionManager Instance of the FTMPermissionManager.
 *
 * @return Instance of FTMReceiveManager.
 */
-(id)initWithDispatcher: (FTMDispatcher *)dispatcher andPermissionManager: (FTMPermissionManager *)permissionManager;

/**
 * Constructs an instance of the FTMReceiveManager.
 *
 * @param dispatcher Instance of the FTMDispatcher.
 * @param fsa Instance of the FTMFileSystemAbstraction.
 * @param permissionManager Instance of the FTMPermissionManager.
 *
 * @return Instance of FTMReceiveManager.
 */
-(id)initWithDispatcher: (FTMDispatcher *)dispatcher fileSystemAbstraction: (FTMFileSystemAbstraction *)fsa andPermissionManager: (FTMPermissionManager *)permissionManager;

/** @name Monitoring File Transfer Progress */

/**
 * Returns an array of FTMProgressDescriptor objects outlining the progrees of each file being received.
 *
 * The FTMProgressDescriptor objects will specify the file ID of the file, the file size (in bytes), and how
 * many bytes have already been transferred. This function allows the user to monitor the progress of files 
 * they are receiving.
 *
 * @return Array of FTMProgressDescriptor objects.
 */
-(NSArray *)getProgressList;

/** @name Request Files */

/**
 * Requests the file with the specified file ID from the provided peer.
 *
 * @param peer Specifies the owner of the file being requested.
 * @param fileID Specifies the ID of the requested file.
 * @param saveFileName Specifies the name to save the requested file.
 * @param saveFileDirectory Specifies the directory to save requested file.
 *
 * @return FTMStatusCode FTMOK, FTMBadFileID, FTMBadFilePath, or FTMFileNotBeingTransferred.
 */
-(FTMStatusCode)requestFileOwnedBy: (NSString *)peer withFileID: (NSData *)fileID saveFileName: (NSString *)saveFileName andSaveFileDirectory: (NSString *)saveFileDirectory;

/**
 * Builds the file request and FTMFileStatus object to monitor transfer progress.
 *
 * This function performs error checking for the provided parameters before the formal request 
 * is sent to the remote peer. After error checking is completed, the FTMRequestDataAction is 
 * built and sent to the transmitter.
 *
 * @param file Specifies the FTMFileDescriptor for the requested file.
 * @param saveFileName Specifies the name to save the requested file.
 * @param saveDirectory Specifies the directory to save the file.
 * @param useDispatcher Specifies whether or not to insert the action into the FTMDispatcher.
 *
 * @return FTMStatusCode FTMOK, FTMBadFilePath, or FTMFileNotBeingTransferred.
 */
-(int)initiateRequestForFile: (FTMFileDescriptor *)file usingSaveFileName: (NSString *)saveFileName andSaveDirectory: (NSString *)saveDirectory throughDispatcher: (BOOL)useDispatcher;

/** @name Handle File Chunks */

/**
 * Processes the received file chunk.
 *
 * This function is called when a chunk of a given file chunk is received from a remote peer. 
 * This function determines which temporary file this chunk belongs to, updates the sending
 * progress, and sends the chunk to the FTMFileSystemAbstraction to be appended to the 
 * appropriate temporary file.
 *
 * @param file Specifies the FileId of the file the chunk belongs to.
 * @param startByte Specifies the starting index of chunk relative to file.
 * @param length Specifies the length of chunk.
 * @param chunk Actual file data.
 */
-(void)handleChunkForFile: (NSData *)file withStartByte: (int)startByte andLength: (int)length andFileData: (NSData *)chunk;


/** @name Transfer Cancel Operations */

/**
 * Handles a cancelled file transfer initiated by the sender.
 *
 * This function is called when the sender cancels a file transfer and sends the DataXferCancelled
 * signal to notify the remote peer of the cancellation. This function will check to ensure that 
 * file was truly cancelled and then notify the user that the transfer has been completed.
 *
 * @warning *Note:* File transfers cancelled by the sender do not cause the temporary
 * files to be deleted. The temporary files are saved so the transfer can be resumed
 * at a later time.
 *
 * @param fileID Specifies the ID of file being cancelled.
 * @param peer Specifies peer who cancelled the transfer.
 */
-(void)handleDataXferCancelledFrom: (NSString *)peer forFileWithID: (NSData *)fileID;

/**
 * Pauses the file being received matching the provided file ID.
 *
 * This function will first check to see if the provided file ID matches a pending file transfer. 
 * If a match is found, the function will build a FTMStopXferAction to notify the file sender to
 * stop sending file chunks. The temporary file is held in memory so the transfer can be resumed
 * at a later time.
 *
 * @param fileID Specifies the ID of the file to pause.
 *
 * @return FTMStatusCode FTMOK or FTMBadFileID.
 */
-(FTMStatusCode)pauseFileWithID: (NSData *)fileID;

/**
 * Cancels the file being received matching the provided file ID.
 *
 * This function will first check to see if the provided file ID matches a pending file transfer.
 * If a match is found, the function deletes the corresponding file status object so all file chunks 
 * received after the cancel is executed are disregarded. The function will also delete all temporary 
 * files.
 *
 * @param fileID Specifies the ID of the file to pause.
 *
 * @return FTMStatusCode FTMOK or FTMBadFileID.
 */
-(FTMStatusCode)cancelFileWithID: (NSData *)fileID;

/** @name Reset State */

/**
 * Resets the state of the FTMReceiveManager.
 *
 * This function is called by the FTMFileTransferModule when the user specifies a new AllJoyn
 * session to be used. This function clears the dictionary storing the records of all current
 * file transfers.
 */
-(void)resetState;

@end