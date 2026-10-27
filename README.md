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
