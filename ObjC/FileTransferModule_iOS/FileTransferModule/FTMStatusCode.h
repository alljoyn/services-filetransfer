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

/*
 * Defines an enumeration of status codes that explain that the operation was successful
 * or give a reason as to why the operation failed.
 */
typedef enum
{
    FTMOK,
    FTMBadFileID,
    FTMRequestDenied,
    FTMBadPeerName,
    FTMBadDirectoryName,
    FTMTransferRequestDenied,
    FTMBadDataID,
    FTMBadFilePath,
    FTMOfferTimeout,
    FTMOfferRejected,
    FTMOfferAccepted,
    FTMNoFileAnnouncementListener,
    FTMCancelled,
    FTMFileNotBeingTransferred,
    FTMTimedOut,
    FTMInvalid,
    FTMOutstandingFileIDRequest,
    FTMNOAjConnection
} FTMStatusCode;
