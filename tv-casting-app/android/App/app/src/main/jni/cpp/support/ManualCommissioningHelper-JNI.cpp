/*
 *   Copyright (c) 2024 Project CHIP Authors
 *   All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

#include <jni.h>
#include <lib/support/JniReferences.h>
#include <lib/support/JniTypeWrappers.h>

#include <app/server/Server.h>
#include <platform/CHIPDeviceLayer.h>

#include "../support/Converters-JNI.h"

#define JNI_METHOD(RETURN, METHOD_NAME) \
    extern "C" JNIEXPORT RETURN JNICALL Java_com_matter_casting_ManualCommissioningHelper_##METHOD_NAME

using namespace chip;

namespace {
// Default commissioning window timeout: 3 minutes (180 seconds)
constexpr uint16_t kDefaultCommissioningWindowTimeout = 3 * 60;
} // namespace

JNI_METHOD(jobject, openBasicCommissioningWindow)(JNIEnv * env, jclass)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "ManualCommissioningHelper::openBasicCommissioningWindow() called");

    CHIP_ERROR err = chip::Server::GetInstance().GetCommissioningWindowManager().OpenBasicCommissioningWindow(
        chip::System::Clock::Seconds16(kDefaultCommissioningWindowTimeout));

    if (err == CHIP_NO_ERROR)
    {
        ChipLogProgress(AppServer,
                        "ManualCommissioningHelper::openBasicCommissioningWindow() Successfully opened commissioning window for %d "
                        "seconds",
                        kDefaultCommissioningWindowTimeout);
        // Log the onboarding payload for debugging
        chip::DeviceLayer::ConfigurationMgr().LogDeviceConfig();
    }
    else
    {
        ChipLogError(AppServer, "ManualCommissioningHelper::openBasicCommissioningWindow() Failed to open commissioning window: %" CHIP_ERROR_FORMAT,
                     err.Format());
    }

    return matter::casting::support::convertMatterErrorFromCppToJava(err);
}

JNI_METHOD(jobject, openBasicCommissioningWindowWithTimeout)(JNIEnv * env, jclass, jint timeoutSeconds)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "ManualCommissioningHelper::openBasicCommissioningWindowWithTimeout() called with timeout: %d seconds",
                    timeoutSeconds);

    // Enforce minimum timeout of 3 minutes (180 seconds) as per Matter spec
    if (timeoutSeconds < 180)
    {
        ChipLogError(AppServer, "ManualCommissioningHelper::openBasicCommissioningWindowWithTimeout() Timeout must be at least 180 seconds");
        return matter::casting::support::convertMatterErrorFromCppToJava(CHIP_ERROR_INVALID_ARGUMENT);
    }

    CHIP_ERROR err = chip::Server::GetInstance().GetCommissioningWindowManager().OpenBasicCommissioningWindow(
        chip::System::Clock::Seconds16(static_cast<uint16_t>(timeoutSeconds)));

    if (err == CHIP_NO_ERROR)
    {
        ChipLogProgress(AppServer,
                        "ManualCommissioningHelper::openBasicCommissioningWindowWithTimeout() Successfully opened commissioning "
                        "window for %d seconds",
                        timeoutSeconds);
        // Log the onboarding payload for debugging
        chip::DeviceLayer::ConfigurationMgr().LogDeviceConfig();
    }
    else
    {
        ChipLogError(AppServer,
                     "ManualCommissioningHelper::openBasicCommissioningWindowWithTimeout() Failed to open commissioning window: %" CHIP_ERROR_FORMAT,
                     err.Format());
    }

    return matter::casting::support::convertMatterErrorFromCppToJava(err);
}

JNI_METHOD(jboolean, isCommissioningWindowOpen)(JNIEnv * env, jclass)
{
    chip::DeviceLayer::StackLock lock;
    
    auto & commissioningWindowManager = chip::Server::GetInstance().GetCommissioningWindowManager();
    bool isOpen = commissioningWindowManager.IsCommissioningWindowOpen();
    
    ChipLogProgress(AppServer, "ManualCommissioningHelper::isCommissioningWindowOpen() returns: %s", isOpen ? "true" : "false");
    
    return static_cast<jboolean>(isOpen);
}

JNI_METHOD(jobject, closeCommissioningWindow)(JNIEnv * env, jclass)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "ManualCommissioningHelper::closeCommissioningWindow() called");

    CHIP_ERROR err = chip::Server::GetInstance().GetCommissioningWindowManager().CloseCommissioningWindow();

    if (err == CHIP_NO_ERROR)
    {
        ChipLogProgress(AppServer, "ManualCommissioningHelper::closeCommissioningWindow() Successfully closed commissioning window");
    }
    else
    {
        ChipLogError(AppServer, "ManualCommissioningHelper::closeCommissioningWindow() Failed to close commissioning window: %" CHIP_ERROR_FORMAT,
                     err.Format());
    }

    return matter::casting::support::convertMatterErrorFromCppToJava(err);
}

JNI_METHOD(void, logOnboardingPayload)(JNIEnv * env, jclass)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "ManualCommissioningHelper::logOnboardingPayload() called");
    
    // This will log the QR code and manual pairing code to logcat
    chip::DeviceLayer::ConfigurationMgr().LogDeviceConfig();
}
