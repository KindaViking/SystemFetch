# SystemFetch

## Team
Callum
Hubert
Ciaran
Jamie


## Basis Of Project
This achieves a basic system fetch for a linux machine that works in the terminal but also on a HTML webpage

## Plan
First a clean working fetch will be developed to work solely in the terminal with the filtering  (1.0.0)
Version 2.0.0 will then include the same fetch but using SVG graphics on a HTML webpage

## How to Build and Run
Run the commands from the project root folder so `data/pci.ids` and `data/usb.ids` are found:

- **Build:**
  ```bash
  mvn compile
  ```
- **Run:**
  ```bash
  mvn exec:java
  ```

## Information Sources
- `/proc/cpuinfo`: CPU model name, physical core count, and logical processor count
- `/proc/stat`: CPU cumulative counters used to calculate total and per-core CPU load
- `/proc/meminfo`: Memory usage statistics (total, available, and used memory)
- `/sys/bus/pci/devices`: PCI device discovery, address, vendor ID, device ID, and class ID
- `/sys/bus/usb/devices`: USB device discovery, device name, vendor ID, and product ID
- `data/pci.ids`: Mapping PCI vendor and device IDs to human-readable names
- `data/usb.ids`: Mapping USB vendor and product IDs to human-readable names

## Live Web Report
The live report shows the same information in a web page, with CPU and memory
updating about twice a second and a bar chart of each CPU's load. It is served by
a small embedded HTTP server (`liveServer`) built with Java's built-in `HttpServer`.

### Running it locally
This needs a Linux machine (or WSL on Windows), since the data is read from `/proc` and `/sys`.

1. From the project root folder, build the project and start the server:
   ```bash
   mvn compile
   java -cp target/classes systemfetch.liveServer
   ```
2. Open <http://localhost:8080> in a browser.
3. Press Ctrl+C in the terminal to stop the server.

### Running it on alderaan from your own machine
The server runs on alderaan and an SSH port forward lets the browser on your own
machine open it.

1. **On your own computer**, connect to alderaan with port 8080 forwarded:
   ```bash
   ssh -L 8080:localhost:8080 your-username@alderaan
   ```
2. **In that ssh session**, build the project and start the server from the project root folder:
   ```bash
   mvn compile
   java -cp target/classes systemfetch.liveServer
   ```
3. Open <http://localhost:8080> in a browser on your own computer.
   The page now shows alderaan's CPU, memory and devices.
4. Press Ctrl+C in the ssh session to stop the server, then `exit` to close the connection.

**Using WSL on Windows:** if the ssh command is run from WSL and the browser on
Windows
