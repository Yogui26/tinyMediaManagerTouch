# Makefile for building the Cursor Flatpak

# --- Variables ---
APP_ID := org.tinymediamanager.tinyMediaManager
MANIFEST := $(APP_ID).yml
APPDATA := $(APP_ID).metainfo.xml
DESKTOP := $(APP_ID).desktop
BUILD_DIR := build-dir
ARCH ?= x86_64


.PHONY: all build install run clean uninstall

# --- Main Targets ---

all: build

# Builds the Flatpak. Depends on the AppImage being extracted.
	@echo "--> Building the Flatpak..."
	# Update the version in the appdata file before building
	flatpak-builder $(BUILD_DIR) $(MANIFEST) --force-clean
	@echo "--> Build complete."

# Installs the Flatpak for the current user.
install: build
	@echo "--> Installing the Flatpak for the current user..."
	flatpak-builder --user --install --force-clean $(BUILD_DIR) $(MANIFEST)
	@echo "--> Installation complete."

# Uninstalls the application.
uninstall:
	@echo "--> Uninstalling $(APP_ID)..."
	flatpak uninstall $(APP_ID)

# Cleans up all generated files and directories.
clean:
	@echo "--> Cleaning up..."
	rm -rf $(BUILD_DIR)
	@echo "--> Cleanup complete."
