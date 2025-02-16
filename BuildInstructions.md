# Surgical Tool Tracker Build & Packaging Instructions

## Step 1: Build the JAR
Run the following command to clean and package the project:

```sh
mvn clean package
```

## Step 2: Create the Application Bundle
Run the following jpackage command:
    
```sh
jpackage \
    --input target/ \
    --name "Aims" \
    --main-jar AIMs-1.0-SNAPSHOT.jar \
    --main-class io.github.tkjonesy.frontend.App \
    --icon "src/main/resources/aims_logo.icns" \
    --type app-image
```

## Step 3: Update Info.plist
Add the following lines to `Aims.app/Contents/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>This app requires camera access.</string>
```

## Step 4: Code Sign the Application
Run the following command to sign the application:

```sh
codesign --force --deep --sign - "Aims.app"
```

# Windows

## Step 1: Build the JAR
```sh
mvn clean package
```

## Step 2: Create the Application Bundle
Run the following jpackage command:

```sh
jpackage --name "SurgicalToolTracker" `
    --input target/ `
    --main-jar stt-1.0-SNAPSHOT.jar `
    --main-class io.github.tkjonesy.frontend.App `
    --type exe `
    --win-shortcut `
    --win-dir-chooser `
    --win-per-user-install `
    --win-upgrade-uuid "123e4567-e89b-12d3-a456-426614174000" 
```

Note: You must have Wix Toolset installed to create the installer. You can download it from [here](https://github.com/wixtoolset/wix3/releases).

## Step 3: Installer
An installer will appear in the root directory of the project. Run the installer to install the application.