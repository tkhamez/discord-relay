# Discord Relay

## Build

- Install OpenJDK 17.
- Install Android SDK.
- Copy `local.properties.dist` to `local.properties` and adjust path.

### Desktop

- Run on each platform.  
- Change `TargetFormat.Deb` to `TargetFormat.Rpm` in `desktop/build.gradle.kts` to build an RPM package.

```shell
./gradlew package
# desktop/build/compose/binaries/main/deb,msi,dmg

./gradlew createDistributable
# desktop/build/compose/binaries/main/app/

./gradlew packageUberJarForCurrentOS
# desktop/build/compose/jars/
```

### Android

```shell
./gradlew build
# android/build/outputs/apk/
```

## Run

### macOS

If it crashes use:
```
SKIKO_RENDER_API=SOFTWARE ./jvm.app/Contents/MacOS/jvm
```
