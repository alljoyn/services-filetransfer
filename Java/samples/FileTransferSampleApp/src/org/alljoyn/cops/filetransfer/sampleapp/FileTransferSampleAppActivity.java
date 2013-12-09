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
package org.alljoyn.cops.filetransfer.sampleapp;

import java.io.File;
import java.util.ArrayList;
import org.alljoyn.cops.filetransfer.FileTransferModule;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.ProgressDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.FileAnnouncementReceivedListener;
import org.alljoyn.cops.filetransfer.listener.FileCompletedListener;
import org.alljoyn.cops.filetransfer.listener.OfferReceivedListener;
import org.alljoyn.cops.filetransfer.listener.RequestDataReceivedListener;
import org.alljoyn.cops.filetransfer.listener.UnannouncedFileRequestListener;
import org.alljoyn.cops.filetransfer.sampleapp.AlljoynManager.ConnectionState;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class FileTransferSampleAppActivity extends Activity implements ConnectionListener
{	
	/* UI Handler Codes */
	private static final int TOAST = 0;
	private static final int ENABLE_VIEW = 1;
	private static final int DISABLE_VIEW = 2;
	private static final int UPDATE_RECEIVE_PROGRESS = 3;
	private static final int UPDATE_SEND_PROGRESS = 4;
	
	/* File Explorer Activity Result Codes */
	private static final int SHARE_SELECTED_FILE = 5;
	private static final int OFFER_SELECTED_FILE = 6;
	
	/* UI Buttons */
	private Button hostButton;
	private Button joinButton;
	private Button shareButton;
	private Button unshareButton;
	private Button requestButton;
	private Button offerButton;
	private Button requestByPathButton;
	private Button pauseReceiveButton;
	private Button cancelReceiveButton;
	private Button pauseSendButton;
	
	/* Creates and manages AllJoyn communication  */
	private AlljoynManager ajManager;	
	/* Facilitates sharing and transferring files */
	private FileTransferModule ftModule;	
	/* Background thread used to monitor the progress of files being received */
	private Thread monitorReceiveThread;
	/* Background thread used to monitor the progress of files being sent */
	private Thread monitorSendThread;
	
	/* UI Handler. Ensures UI operations are performed on the UI thread */
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message message)
		{
			switch (message.what)
			{
				case TOAST:			
					Toast toast = Toast.makeText(getApplicationContext(), (String) message.obj, Toast.LENGTH_SHORT);
					toast.show();
					break;
				case ENABLE_VIEW:
					View viewToEnable = (View) message.obj;
					viewToEnable.setEnabled(true);
					break;
				case DISABLE_VIEW:
					View viewToDisable = (View) message.obj;
					viewToDisable.setEnabled(false);
					break;	
				case UPDATE_RECEIVE_PROGRESS:
					ProgressBar receiveProgressBar = (ProgressBar) findViewById(R.id.receiveProgressBar);	
					receiveProgressBar.setProgress( (Integer) message.obj);
					break;
				case UPDATE_SEND_PROGRESS:
					ProgressBar sendProgressBar = (ProgressBar) findViewById(R.id.sendProgressBar);	
					sendProgressBar.setProgress( (Integer) message.obj);
					break;		
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ajManager = new AlljoynManager();  
        ajManager.setConnectionListener(this);
        
        initializeGuiModules();  
    }
    
    /* Initialize and assign listeners to UI Modules */
	private void initializeGuiModules()
	{
		hostButton = (Button) findViewById(R.id.hostButton);
        hostButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onHostButtonClicked();
			}    	
        });        
        
        joinButton = (Button) findViewById(R.id.joinButton);
        joinButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onJoinButtonClicked();
			}    	
        });
        
        shareButton = (Button) findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onShareButtonClicked();
			}    	
        });
        
        unshareButton = (Button) findViewById(R.id.stopShareButton);
        unshareButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				OnUnshareButtonClicked();
			}    	
        });
        
        requestButton = (Button) findViewById(R.id.requestButton);
        requestButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onRequestButtonClicked();
			}    	
        });
        
        offerButton = (Button) findViewById(R.id.offerButton);
        offerButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onOfferButtonClicked();
			}    	
        });
        
        requestByPathButton = (Button) findViewById(R.id.requestFileIdButton);
        requestByPathButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				OnRequestByPathClicked();
			}
        });
        
        pauseReceiveButton = (Button) findViewById(R.id.pauseReceiveButton);
        pauseReceiveButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onPauseReceiveButtonClicked();
			}    	
        });
        
        cancelReceiveButton = (Button) findViewById(R.id.cancelButton);
        cancelReceiveButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onCancelReceiveButtonClicked();
			}    	
        });
        
        pauseSendButton = (Button) findViewById(R.id.cancelSendButton);
        pauseSendButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onPauseSendButtonClicked();
			}    	
        });
	}
	
	
	/*
	 * Initialize the File Transfer Module and its listeners
	 */
	private void initializeFileTransferModule()
	{
		//Construct the module using the Bus Attachment and Session Id created by the AllJoyn Manager
		ftModule = new FileTransferModule(ajManager.getBusAttachment(), ajManager.getSessionId());
		
		//Register announcement listener - create a toast when a file announcement is received
		ftModule.setFileAnnouncementReceivedListener(new FileAnnouncementReceivedListener()
		{
			public void receivedAnnouncement(FileDescriptor[] fileList, boolean isFileIdResponse)
			{		
				if (!isFileIdResponse)
				{
					handler.sendMessage(handler.obtainMessage(TOAST, "Regular File Announcement Received!"));
				}
				else
				{
					handler.sendMessage(handler.obtainMessage(TOAST, "File ID Response Announcement Received!"));
				}
			}			
		});
		
		//Register file completed listener - disable buttons if the completion was triggered by a cancel
		ftModule.setFileCompletedListener(new FileCompletedListener()
		{
			public void fileCompleted(String filename, int statusCode)
			{
				if (statusCode == StatusCode.CANCELLED)
				{
					handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, pauseReceiveButton));
					handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, cancelReceiveButton));
				}
				
				handler.sendMessage(handler.obtainMessage(TOAST, "Transfer Complete: " + filename));				
			}			
		});
		
		//Register offer file listener - always accept offers (return true), and monitor the receiving progress
		ftModule.setOfferReceivedListener(new OfferReceivedListener()
		{			
			public boolean acceptOfferedFile(FileDescriptor file, String peer)
			{			
				if (monitorReceiveThread == null)
				{
					monitorReceiveProgress();
				}				
				
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, pauseReceiveButton));
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, cancelReceiveButton));
				return true;
			}
		});
		
		//Register unannounced File Listener - always allow files to be requested by path, 
		//even if they aren't shared. Required for use of requestFileId feature
		ftModule.setUnannouncedFileRequestListener(new UnannouncedFileRequestListener()
		{			
			public boolean allowUnannouncedFileRequests(String filePath)
			{
				return true;
			}
		});
		
		//register request data listener - monitor sending progress when a file is requested
		ftModule.setRequestDataReceivedListener(new RequestDataReceivedListener()
		{
			public void fileRequestReceived(String fileName)
			{
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, pauseSendButton));
				
				if (monitorSendThread == null)
				{
					monitorSendProgress();
				}
			}			
		});
	}	
	
	/* 
	 * Activates the UI once an AllJoyn connection has been established.
	 * Triggered by the AllJoyn Manager. 
	 */
	public void ConnectionChanged(ConnectionState connectionState)
	{
		if (connectionState == ConnectionState.CONNECTED)
		{
			handler.sendMessage(handler.obtainMessage(TOAST, "Connected!"));
			
			handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, hostButton));
			handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, joinButton));
			
			handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, shareButton));
			handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, unshareButton));
			handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, requestButton));
			handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, offerButton));
			handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, requestByPathButton));

            new Thread()
            {
                public void run()
                {
                    initializeFileTransferModule();     
                }
            }.start();                      
		}
	}
    
	/*
	 * Start hosting and advertising an AllJoyn File Transfer Session
	 */
	private void onHostButtonClicked()
	{
		hostButton.setEnabled(false);
		joinButton.setEnabled(true);
		
		handler.sendMessage(handler.obtainMessage(TOAST, "Hosting session"));	
		
		ajManager.createSession();
	}
	
	/*
	 * Attempt to join an AllJoyn File Transfer Session. Another device must be hosting
	 * before a join will complete successfully
	 */
	private void onJoinButtonClicked()
	{
		joinButton.setEnabled(false);
		hostButton.setEnabled(true);
		
		handler.sendMessage(handler.obtainMessage(TOAST, "Attempting to join session..."));		
		
		ajManager.joinSession();	
	}

	/*
	 * Share a file. Launches the FileExplorer activity allowing the user to browse the file system.
	 * See the SHARE_SELECTED_FILE portion of the onActivityResult function
	 */
	private void onShareButtonClicked()
	{
		Intent selectFileIntent = new Intent(this, FileExplorer.class);	
		startActivityForResult(selectFileIntent, SHARE_SELECTED_FILE); 										
	}

	/*
	 * Stop sharing a file. 
	 */
	private void OnUnshareButtonClicked()
	{
		//get list of files currently being shared
		final ArrayList<FileDescriptor> announcedFiles = ftModule.getAnnouncedLocalFiles();
		
		//create an array of filenames to display to the user
		String[] filenames = new String[announcedFiles.size()];		
		for (int i = 0; i < announcedFiles.size(); i++)
		{
			filenames[i] = announcedFiles.get(i).filename;
		}
		
		//create the click listener - when a file is selected, share it
		DialogInterface.OnClickListener onFileClicked = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				FileDescriptor selected = announcedFiles.get(which);
				
				ArrayList<String> path = new ArrayList<String>();
				path.add(new File(selected.sharedPath, selected.filename).getAbsolutePath());				
				
				ftModule.stopAnnounce(path);
			}			
		};
		
		//show the file picker dialog
		showFilePickerDialog(filenames, onFileClicked);
	}

	/*
	 * Request a known file. Files must have been announced, or requested by file file path
	 * before they can be transfered.	 
	 */
	private void onRequestButtonClicked()
	{
		//get list of files available for transfer
		final ArrayList<FileDescriptor> availableFiles = ftModule.getAvailableRemoteFiles();
		
		//create an array of filenames to display to the user
		String[] filenames = new String[availableFiles.size()];		
		for (int i = 0; i < availableFiles.size(); i++)
		{
			filenames[i] = availableFiles.get(i).filename;
		}
		
		//create the click listener - when a file is selected, request it and monitor its
		//receiving progress
		DialogInterface.OnClickListener onFileClicked = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				FileDescriptor selected = availableFiles.get(which);
				
				ftModule.requestFile(selected.owner, selected.fileID, selected.filename);
				
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, pauseReceiveButton));
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, cancelReceiveButton));
				
				if (monitorReceiveThread == null)
				{
					monitorReceiveProgress();
				}				
			}			
		};
		
		showFilePickerDialog(filenames, onFileClicked);	
	}
	
	/*
	 * Start the thread responsible for monitoring receiving progress. The thread then signals the
	 * UI handler to update the receiving progress bar
	 */
	private void monitorReceiveProgress()
	{
		monitorReceiveThread = new Thread(new Runnable()
		{
			public void run()
			{
				//get list of files being received
				ArrayList<ProgressDescriptor> receiveList = ftModule.getReceiveProgressList();
				
				while (receiveList.size() > 0)
				{
					ProgressDescriptor descriptor = receiveList.get(receiveList.size() - 1);
					
					int progress = (int) (((float)descriptor.bytesTransferred)/descriptor.fileSize * 100);
					
					//signal the UI handler to update the progress bar
					handler.sendMessage(handler.obtainMessage(UPDATE_RECEIVE_PROGRESS, progress));
					
					//sleep before checking progress again
					try
					{
						Thread.sleep(100);
					} catch (InterruptedException e) { }
					
					receiveList = ftModule.getReceiveProgressList();					
				}				
				
				//no more files being received - update progress bar to 100%
				handler.sendMessage(handler.obtainMessage(UPDATE_RECEIVE_PROGRESS, 100));
				
				//disable pause and cancel buttons
				handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, pauseReceiveButton));
				handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, cancelReceiveButton));
				
				monitorReceiveThread = null;
			}				
		});
		monitorReceiveThread.start();
	}

	/*
	 * Start the thread responsible for monitoring sending progress. The thread then signals the
	 * UI handler to update the sending progress bar
	 */
	private void monitorSendProgress()
	{
		monitorSendThread = new Thread(new Runnable()
		{
			public void run()
			{
				//get list of files being sent
				ArrayList<ProgressDescriptor> sendList = ftModule.getSendingProgressList();
				
				while (sendList.size() > 0)
				{
					ProgressDescriptor descriptor = sendList.get(0);
					
					int progress = (int) (((float)descriptor.bytesTransferred)/descriptor.fileSize * 100);
					
					handler.sendMessage(handler.obtainMessage(UPDATE_SEND_PROGRESS, progress));
					
					try
					{
						Thread.sleep(100);
					} catch (InterruptedException e) { }
					
					sendList = ftModule.getSendingProgressList();					
				}				
				
				handler.sendMessage(handler.obtainMessage(UPDATE_SEND_PROGRESS, 100));
				handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, pauseSendButton));
				
				monitorSendThread = null;
			}				
		});
		monitorSendThread.start();
	}

	/*
	 * Pause the file currently being received	   
	 */
	private void onPauseReceiveButtonClicked()
	{
		ArrayList<ProgressDescriptor> receiveList = ftModule.getReceiveProgressList();
		
		ProgressDescriptor descriptor = receiveList.get(receiveList.size() - 1);
		
		ftModule.pauseFile(descriptor.fileID);
		
		handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, pauseReceiveButton));
	}

	/*
	 * Cancel the file currently being received	   
	 */
	private void onCancelReceiveButtonClicked()
	{
		ArrayList<ProgressDescriptor> receiveList = ftModule.getReceiveProgressList();
		
		ProgressDescriptor descriptor = receiveList.get(receiveList.size() - 1);
		
		ftModule.cancelReceivingFile(descriptor.fileID);		
	}
	
	/*
	 * Pause the file currently being sent	   
	 */
	private void onPauseSendButtonClicked()
	{
		ArrayList<ProgressDescriptor> sendList = ftModule.getSendingProgressList();
		
		ProgressDescriptor descriptor = sendList.get(sendList.size() - 1);
		
		ftModule.cancelSendingFile(descriptor.fileID);		
	}
	
	/*
	 * Offer a file. Launches the FileExplorer activity allowing the user to browse the file system.
	 * See the OFFER_SELECTED_FILE portion of the onActivityResult function
	 */
	private void onOfferButtonClicked()
	{
		Intent selectedFileIntent = new Intent(this, FileExplorer.class);
		startActivityForResult(selectedFileIntent, OFFER_SELECTED_FILE);				
	}

	/*
	 * Request a file announcement by path. Allows a transfer to be started if the absolute path is known
	 * on the remote device (and their UnannouncedFileListener returns true). 
	 */
	private void OnRequestByPathClicked()
	{
		//get list of known peers to display
		final String[] peers = ajManager.getPeers();
		
		//create the click listener - when a peer is selected, prompt for the path
		DialogInterface.OnClickListener onPeerClicked = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				String peer = peers[which];	
				promptForPath(peer);
			}
		};
		
		showPeerPickerDialog(onPeerClicked);
	}    	

	/*
	 * Build the path input dialog and execute a request for file id	 
	 */
	protected void promptForPath(final String peer)
	{
		final EditText input = new EditText(this);		
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("File Path");
		builder.setView(input);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				String path = input.getText().toString();
				
				ftModule.requestOffer(peer, path);
			}
		});
		builder.show();		
	}

	/* 
	 * Called when the FileExplorer exits. Retrieves the selected file
	 * and executes a command based on the requestCode
	 */
	@Override  
	public void onActivityResult(int requestCode, int resultCode, final Intent intent)
	{		
		//the intent is null if the FileExplorer was exited 
		//without a selection being made
		if (intent == null)
		{
			return;
		}
		
		switch (requestCode)
		{
			case SHARE_SELECTED_FILE:
			{
				//retrieve selected file from FileExplorer intent
				File selected = (File) intent.getExtras().get("file"); 
					
				ArrayList<String> filePath = new ArrayList<String>();
				filePath.add(selected.getAbsolutePath());
					
				//announce selected file
				ftModule.announce(filePath);
				break;
			}
			case OFFER_SELECTED_FILE:
			{							
				final String[] peers = ajManager.getPeers();
				
				//create the click listener - when a peer is selected, offer them the file
				DialogInterface.OnClickListener onPeerClicked = new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						String peer = peers[which];
						File file = (File) intent.getExtras().get("file"); 
						
						ftModule.offerFileToPeer(peer, file.getAbsolutePath(), 1000);						
					}
				};
				
				showPeerPickerDialog(onPeerClicked);
				break;
			}
		}
	}
	
	/*
	 * Build the file list dialog and execute the clickListener when a file is selected	 
	 */
	private void showFilePickerDialog(String[] filenames, DialogInterface.OnClickListener clickListener)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select File");
		builder.setItems(filenames, clickListener);
		builder.create().show();
	}

	/*
	 * Build the peer list dialog and execute the clickListener when a file is selected	 
	 */
	public void showPeerPickerDialog(DialogInterface.OnClickListener clickListener)
	{
		final String[] peers = ajManager.getPeers();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Peer");
		builder.setItems(peers, clickListener);
		builder.create().show();				
	}
	
	@Override
    public void onBackPressed() 
    {
	    if (ftModule != null)
	    {
	    	ftModule.destroy();
	    }
    	ajManager.disconnect();  	
    	super.onBackPressed();
    }	
}
