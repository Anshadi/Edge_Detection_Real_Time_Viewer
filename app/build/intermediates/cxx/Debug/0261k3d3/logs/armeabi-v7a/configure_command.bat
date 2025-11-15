@echo off
"D:\\Software And Hardware\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HD:\\Helped\\sem_6\\EdgeDetectionViewer_project_ready\\app\\src\\main\\cpp" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=armeabi-v7a" ^
  "-DCMAKE_ANDROID_ARCH_ABI=armeabi-v7a" ^
  "-DANDROID_NDK=D:\\Software And Hardware\\Android\\Sdk\\ndk\\23.1.7779620" ^
  "-DCMAKE_ANDROID_NDK=D:\\Software And Hardware\\Android\\Sdk\\ndk\\23.1.7779620" ^
  "-DCMAKE_TOOLCHAIN_FILE=D:\\Software And Hardware\\Android\\Sdk\\ndk\\23.1.7779620\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=D:\\Software And Hardware\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_CXX_FLAGS=-frtti -fexceptions" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=D:\\Helped\\sem_6\\EdgeDetectionViewer_project_ready\\app\\build\\intermediates\\cxx\\Debug\\0261k3d3\\obj\\armeabi-v7a" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=D:\\Helped\\sem_6\\EdgeDetectionViewer_project_ready\\app\\build\\intermediates\\cxx\\Debug\\0261k3d3\\obj\\armeabi-v7a" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BD:\\Helped\\sem_6\\EdgeDetectionViewer_project_ready\\app\\.cxx\\Debug\\0261k3d3\\armeabi-v7a" ^
  -GNinja
