# Sample of THETA Plug-in that communicates using Bluetooth SPP

より詳しい日本語の説明は[こちら](https://qiita.com/KA-2/items/b6d261d9b8bf584ebfe4)。<br>
[Click here](https://qiita.com/KA-2/items/b6d261d9b8bf584ebfe4) for a more detailed explanation in Japanese.

## Overview

Sample program that uses Bluetooth SPP and WLAN at the same time.<br>

Based on the following project file set, I added the code to output the attitude information with Bluetooth SPP.<br>
https://github.com/theta-skunkworks/theta-plugin-extendedpreview


In this sample, THETA behaves as a Host and searches for Devices that can use Bluetooth SPP.<br>
When the connection is established with the device found first and communication with SPP becomes possible, the attitude information is sent.<br>

If the Device can use Excel, you can draw graphs in real time by using an add-in called DataStreamer.<br>
Please also refer to the video below.<br>

[![](https://img.youtube.com/vi/qEebELw_9L0/0.jpg)](https://www.youtube.com/watch?v=qEebELw_9L0)

## Development Environment

### Camera
* RICOH THETA V Firmware ver.3.50.1 and above
* RICOH THETA Z1 Firmware ver.1.60.1 and above

### SDK/Library
* RICOH THETA Plug-in SDK ver.2.1.0

### Development Software
* Android Studio ver.4.1.1
* gradle ver.5.1.1


## License

```
Copyright 2018 Ricoh Company, Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contact
![Contact](img/contact.png)

