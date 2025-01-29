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
    --name "Surgical Tool Tracker" \
    --main-jar stt-1.0-SNAPSHOT.jar \
    --main-class io.github.tkjonesy.frontend.App \
    --type app-image
```

## Step 3: Update Info.plist
Add the following lines to `Surgical Tool Tracker.app/Contents/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>This app requires camera access to track surgical tools.</string>
```

## Step 4: Code Sign the Application
Run the following command to sign the application:

```sh
codesign --force --deep --sign - "Surgical Tool Tracker.app"
```