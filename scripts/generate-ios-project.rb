#!/usr/bin/env ruby

require "fileutils"
require "xcodeproj"

root = File.expand_path("..", __dir__)
ios_app_dir = File.join(root, "iosApp")
project_path = File.join(ios_app_dir, "App.xcodeproj")

FileUtils.rm_rf(project_path)

project = Xcodeproj::Project.new(project_path)
project.root_object.attributes["LastSwiftUpdateCheck"] = "2640"
project.root_object.attributes["LastUpgradeCheck"] = "2640"

target = project.new_target(:application, "App", :ios, "16.0")
target.product_name = "MelodyTrove"
target.product_reference.name = "MelodyTrove.app"
target.product_reference.path = "MelodyTrove.app"

app_group = project.main_group.new_group("App")
swift_file = app_group.new_file("AppMain.swift")
plist_file = app_group.new_file("Info.plist")
app_group.new_file("App.entitlements")
assets_file = app_group.new_file("Assets.xcassets")
target.add_file_references([swift_file])
target.resources_build_phase.add_file_reference(assets_file)

target.build_configurations.each do |configuration|
  settings = configuration.build_settings
  settings["ASSETCATALOG_COMPILER_APPICON_NAME"] = "AppIcon"
  settings["CODE_SIGN_STYLE"] = "Automatic"
  settings["CODE_SIGN_ENTITLEMENTS"] = "App.entitlements"
  settings["CURRENT_PROJECT_VERSION"] = "1"
  settings["DEVELOPMENT_TEAM"] = ""
  settings["ENABLE_USER_SCRIPT_SANDBOXING"] = "NO"
  settings["FRAMEWORK_SEARCH_PATHS"] = [
    "$(inherited)",
    "$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
  ]
  settings["GENERATE_INFOPLIST_FILE"] = "NO"
  settings["INFOPLIST_FILE"] = "$(SRCROOT)/Info.plist"
  settings["IPHONEOS_DEPLOYMENT_TARGET"] = "16.0"
  settings["MARKETING_VERSION"] = "0.3.0"
  settings["OTHER_LDFLAGS"] = [
    "$(inherited)",
    "-framework",
    "SharedKit",
    "-framework",
    "AudioToolbox",
  ]
  settings["PRODUCT_BUNDLE_IDENTIFIER"] = "io.github.julystar.musicapp"
  settings["PRODUCT_NAME"] = "MelodyTrove"
  settings["SUPPORTED_PLATFORMS"] = "iphoneos iphonesimulator"
  settings["SWIFT_VERSION"] = "6.0"
  settings["TARGETED_DEVICE_FAMILY"] = "1,2"
end

kotlin_phase = target.new_shell_script_build_phase("Compile Kotlin Framework")
kotlin_phase.shell_path = "/bin/sh"
kotlin_phase.always_out_of_date = "1"
kotlin_phase.shell_script = <<~SH
  set -e
  cd "$SRCROOT/.."
  ./gradlew :shared:embedAndSignAppleFrameworkForXcode
SH
target.build_phases.move(kotlin_phase, 0)

project.save

scheme = Xcodeproj::XCScheme.new
scheme.add_build_target(target)
scheme.set_launch_target(target)
scheme.save_as(project_path, "App", true)
