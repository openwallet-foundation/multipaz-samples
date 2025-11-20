Pod::Spec.new do |spec|
    spec.name                     = 'composeApp'
    spec.version                  = '1.0'
    spec.homepage                 = 'https://multipaz.org'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Compose Multiplatform Getting Started App'
    spec.vendored_frameworks      = 'build/cocoapods/framework/ComposeApp.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '15.5'
    spec.dependency 'GoogleMLKit/BarcodeScanning'
    spec.dependency 'GoogleMLKit/FaceDetection'
    spec.dependency 'GoogleMLKit/Vision'
    spec.dependency 'TensorFlowLiteSwift'
                
    if !Dir.exist?('build/cocoapods/framework/ComposeApp.framework') || Dir.empty?('build/cocoapods/framework/ComposeApp.framework')
        raise "

        Kotlin framework 'ComposeApp' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :composeApp:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':composeApp',
        'PRODUCT_MODULE_NAME' => 'ComposeApp',
    }
                
    spec.script_phases = [
        {
            :name => 'Build composeApp',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:linkDebugFrameworkIosArm64
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:prepareComposeResourcesTaskForCommonMain
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:iosArm64ResolveResourcesFromDependencies
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:iosSimulatorArm64ResolveResourcesFromDependencies

                PREPARED_RESOURCES_DIR="$REPO_ROOT/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"
                DEPENDENCY_RESOURCES_ROOT="$REPO_ROOT/build/kotlin-multiplatform-resources/resources-from-dependencies"
                TARGET_RESOURCES_ROOT="$REPO_ROOT/build/compose/cocoapods/compose-resources/composeResources"

                rm -rf "$TARGET_RESOURCES_ROOT"
                mkdir -p "$TARGET_RESOURCES_ROOT"

                # Determine app's package name for app resources
                RES_CLASS_FILE=$(find "$REPO_ROOT/build/generated/compose/resourceGenerator/kotlin/commonResClass" -name Res.kt -print -quit)
                if [ -n "$RES_CLASS_FILE" ]; then
                    RES_CLASS_DIR=$(dirname "$RES_CLASS_FILE")
                    PACKAGE_PATH=$(echo "$RES_CLASS_DIR" | sed "s#^$REPO_ROOT/build/generated/compose/resourceGenerator/kotlin/commonResClass/##")
                    PACKAGE_NAME=$(echo "$PACKAGE_PATH" | tr '/' '.')
                else
                    echo "Warning: unable to determine compose resource package name; using fallback name"
                    PACKAGE_NAME="multipazgettingstartedsample.composeapp.generated.resources"
                fi
                APP_RES_DEST_DIR="$TARGET_RESOURCES_ROOT/$PACKAGE_NAME"

                if [ -d "$PREPARED_RESOURCES_DIR" ]; then
                    mkdir -p "$APP_RES_DEST_DIR"
                    cp -R "$PREPARED_RESOURCES_DIR/." "$APP_RES_DEST_DIR/" 2>/dev/null || true
                    echo "Copied app compose resources into $APP_RES_DEST_DIR"
                else
                    echo "Warning: prepared app resources directory not found at $PREPARED_RESOURCES_DIR"
                fi

                # Ensure PEM files from source are present in TARGET resources before packaging/signing
                SRC_PEMS_DIR="$REPO_ROOT/src/commonMain/composeResources/files"
                if [ -d "$SRC_PEMS_DIR" ]; then
                    mkdir -p "$APP_RES_DEST_DIR/files"
                    find "$SRC_PEMS_DIR" -name "*.pem" -maxdepth 1 -type f -print -exec cp {} "$APP_RES_DEST_DIR/files/" \\;
                    echo "Ensured PEM files present in $APP_RES_DEST_DIR/files"
                fi

                if [ -d "$DEPENDENCY_RESOURCES_ROOT" ]; then
                    find "$DEPENDENCY_RESOURCES_ROOT" -mindepth 1 -maxdepth 1 -type d | while IFS= read -r arch_dir; do
                        ARCH_NAME=$(basename "$arch_dir")
                        DEP_ARCH_SRC="$arch_dir/composeResources"
                        if [ -d "$DEP_ARCH_SRC" ]; then
                            for dep_pkg in "$DEP_ARCH_SRC"/*; do
                                if [ -d "$dep_pkg" ]; then
                                    dep_name=$(basename "$dep_pkg")
                                    mkdir -p "$TARGET_RESOURCES_ROOT/$dep_name"
                                    cp -R "$dep_pkg/." "$TARGET_RESOURCES_ROOT/$dep_name/" 2>/dev/null || true
                                    echo "  Copied dependency package: $dep_name (from $ARCH_NAME)"
                                fi
                            done
                        fi
                    done
                else
                    echo "Warning: dependency compose resources not found at $DEPENDENCY_RESOURCES_ROOT"
                fi
            SCRIPT
        },
        {
            :name => 'Copy Compose Resources',
            :execution_position => :after_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                APP_BUNDLE_PATH="$BUILT_PRODUCTS_DIR/$PRODUCT_NAME.app"
                if [ -d "$REPO_ROOT/build/compose/cocoapods/compose-resources" ]; then
                    echo "Copying compose resources to app bundle..."
                    cp -r "$REPO_ROOT/build/compose/cocoapods/compose-resources" "$APP_BUNDLE_PATH/"
                    echo "Compose resources copied successfully"
                else
                    echo "Warning: compose-resources directory not found at $REPO_ROOT/build/compose/cocoapods/compose-resources"
                fi

                # Ensure PEM files from source are present in the bundle even if generation skipped them
                # Derive package name again (same logic as in the build phase)
                RES_CLASS_FILE=$(find "$REPO_ROOT/build/generated/compose/resourceGenerator/kotlin/commonResClass" -name Res.kt -print -quit)
                if [ -n "$RES_CLASS_FILE" ]; then
                    RES_CLASS_DIR=$(dirname "$RES_CLASS_FILE")
                    PACKAGE_PATH=$(echo "$RES_CLASS_DIR" | sed "s#^$REPO_ROOT/build/generated/compose/resourceGenerator/kotlin/commonResClass/##")
                    PACKAGE_NAME=$(echo "$PACKAGE_PATH" | tr '/' '.')
                else
                    PACKAGE_NAME="multipazgettingstartedsample.composeapp.generated.resources"
                fi
                DEST_DIR="$APP_BUNDLE_PATH/compose-resources/composeResources/$PACKAGE_NAME/files"
                mkdir -p "$DEST_DIR"
                SRC_PEMS_DIR="$REPO_ROOT/src/commonMain/composeResources/files"
                if [ -d "$SRC_PEMS_DIR" ]; then
                    echo "Copying PEM files from $SRC_PEMS_DIR to $DEST_DIR"
                    find "$SRC_PEMS_DIR" -name "*.pem" -maxdepth 1 -type f -print -exec cp {} "$DEST_DIR/" \\;
                fi
            SCRIPT
        }
    ]
    spec.resources = ['build/compose/cocoapods/compose-resources']
end

