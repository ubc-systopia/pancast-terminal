# pancast-terminal
Android application written in Kotlin to act as terminal between dongle and backend
Currently can only send a single HTTPS request to a backend server at https://<PLACEHOLDER>:8081

Setup instructions:
1. Install Android Studio.
2. Clone this repo.
3. Open it in Android Studio, and wait for the project to index and sync.
4. In the MainActivity file, change the BACKEND_ADDR to an IP address of your choosing.
(Note: choosing `localhost` or `127.0.0.1` will fail. The IP address needs to be one accessible from the internet. To find out your own IP address, open your terminal, run `ifconfig`, and look for your internet interface i.e. eth0. There should be a `inet` field that contains your IP address).
4. Click on the 'run app' button (green triangle) near the top of your screen (running in either emulator or device works).
5. Hope nothing goes wrong :)

Modifying target:
1. Go to the MainActivity file
2. Modify the BACKEND_ADDR and BACKEND_PORT fields as needed
