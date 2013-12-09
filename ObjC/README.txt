File Transfer Module README.txt
-------------------------------

Installation
------------

 * The File Transfer Module, including library, sample application, appledocs,
   and Unit tests, is provided as Xcode projects. For instructions on how to 
   configure your Xcode environment to build these projects, please see the
   'File Transfer Module Usage Guide for iOS' at 
   https://www.alljoyn.org/docs-and-downloads/modules


Known Issues
------------

  * To select files for sharing, the sample app allows the user to browse only the 
    picture library. This limitation is a result of iOS's restriction that an app
    can access only files within its sandbox, without specific 'file management'
    logic per media type.
