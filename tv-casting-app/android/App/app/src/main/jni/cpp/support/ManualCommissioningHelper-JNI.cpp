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
#include <CastingServer.h>
#include <TargetVideoPlayerInfo.h>

#include "../support/Converters-JNI.h"

#define JNI_METHOD(RETURN, METHOD_NAME) \
    extern "C" JNIEXPORT RETURN JNICALL Java_com_matter_casting_ManualCommissioningHelper_##METHOD_NAME

using namespace chip;

namespace {

TargetVideoPlayerInfo * gCommissionedVideoPlayer = nullptr;

// Callback when commissioning completes
void OnCommissioningComplete(CHIP_ERROR err)
{
    ChipLogProgress(AppServer, "ManualCommissioningHelper::OnCommissioningComplete called with %" CHIP_ERROR_FORMAT, err.Format());
}

// Callback when connection succeeds - this gives us the commissioned device info!
void OnConnectionSuccess(TargetVideoPlayerInfo * videoPlayer)
{
    ChipLogProgress(AppServer,
                    "ManualCommissioningHelper::OnConnectionSuccess with Video Player(nodeId: 0x" ChipLogFormatX64
                    ", fabricIndex: %d, deviceName: %s, vendorId: %d, productId: %d, deviceType: %d)",
                    ChipLogValueX64(videoPlayer->GetNodeId()), videoPlayer->GetFabricIndex(), videoPlayer->GetDeviceName(),
                    videoPlayer->GetVendorId(), videoPlayer->GetProductId(), videoPlayer->GetDeviceType());
    
    // Store the commissioned video player so we can use it to send commands
    gCommissionedVideoPlayer = videoPlayer;
    
    ChipLogProgress(AppServer, "ManualCommissioningHelper::OnConnectionSuccess - Device commissioned successfully! Can now send commands.");
}

// Callback when connection fails
void OnConnectionFailure(CHIP_ERROR err)
{
    ChipLogError(AppServer, "ManualCommissioningHelper::OnConnectionFailure error: %" CHIP_ERROR_FORMAT, err.Format());
}

// Callback when new endpoint is discovered
void OnNewOrUpdatedEndpoint(TargetEndpointInfo * endpoint)
{
    ChipLogProgress(AppServer, "ManualCommissioningHelper::OnNewOrUpdatedEndpoint called for endpoint ID: %d", endpoint->GetEndpointId());
}

} // namespace

JNI_METHOD(jobject, openBasicCommissioningWindow)(JNIEnv * env, jclass)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "ManualCommissioningHelper::openBasicCommissioningWindow() called");

    // Use CastingServer's OpenBasicCommissioningWindow with callbacks (like Linux app does)
    CastingServer::GetInstance()->Init();
    
    CommissioningCallbacks commissioningCallbacks;
    commissioningCallbacks.commissioningComplete = OnCommissioningComplete;
    
    CHIP_ERROR err = CastingServer::GetInstance()->OpenBasicCommissioningWindow(
        commissioningCallbacks, OnConnectionSuccess, OnConnectionFailure, OnNewOrUpdatedEndpoint);

    if (err == CHIP_NO_ERROR)
    {
        ChipLogProgress(AppServer, "ManualCommissioningHelper::openBasicCommissioningWindow() Successfully opened commissioning window");
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

    chip::Server::GetInstance().GetCommissioningWindowManager().CloseCommissioningWindow();
    
    ChipLogProgress(AppServer, "ManualCommissioningHelper::closeCommissioningWindow() Successfully closed commissioning window");

    return matter::casting::support::convertMatterErrorFromCppToJava(CHIP_NO_ERROR);
}

JNI_METHOD(void, logOnboardingPayload)(JNIEnv * env, jclass)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "ManualCommissioningHelper::logOnboardingPayload() called");
    
    // This will log the QR code and manual pairing code to logcat
    chip::DeviceLayer::ConfigurationMgr().LogDeviceConfig();
}

JNI_METHOD(jboolean, hasCommissionedVideoPlayer)(JNIEnv * env, jclass)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "ManualCommissioningHelper::hasCommissionedVideoPlayer() called");
    
    // Check both old and new APIs for commissioned device
    CastingServer * castingServer = CastingServer::GetInstance();
    if (castingServer != nullptr)
    {
        TargetVideoPlayerInfo * activePlayer = castingServer->GetActiveTargetVideoPlayer();
        if (activePlayer != nullptr && activePlayer->IsInitialized())
        {
            ChipLogProgress(AppServer, "ManualCommissioningHelper::hasCommissionedVideoPlayer() Found active player via CastingServer");
            return JNI_TRUE;
        }
    }
    
    // Fallback to legacy global variable
    bool hasPlayer = (gCommissionedVideoPlayer != nullptr);
    ChipLogProgress(AppServer, "ManualCommissioningHelper::hasCommissionedVideoPlayer() returns: %s", hasPlayer ? "true" : "false");
    
    return hasPlayer ? JNI_TRUE : JNI_FALSE;
}

JNI_METHOD(jobject, getCommissionedVideoPlayerInfo)(JNIEnv * env, jclass)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "ManualCommissioningHelper::getCommissionedVideoPlayerInfo() called");
    
    if (gCommissionedVideoPlayer == nullptr)
    {
        ChipLogError(AppServer, "ManualCommissioningHelper::getCommissionedVideoPlayerInfo() No commissioned video player");
        return nullptr;
    }
    
    char infoStr[512];
    snprintf(infoStr, sizeof(infoStr), 
            "NodeId:0x%llx,FabricIndex:%d,DeviceName:%s,VendorId:%d,ProductId:%d,DeviceType:%d",
            static_cast<unsigned long long>(gCommissionedVideoPlayer->GetNodeId()),
            gCommissionedVideoPlayer->GetFabricIndex(),
            gCommissionedVideoPlayer->GetDeviceName(),
            gCommissionedVideoPlayer->GetVendorId(),
            gCommissionedVideoPlayer->GetProductId(),
            gCommissionedVideoPlayer->GetDeviceType());
    
    ChipLogProgress(AppServer, "ManualCommissioningHelper::getCommissionedVideoPlayerInfo() %s", infoStr);
    
    return env->NewStringUTF(infoStr);
}

JNI_METHOD(jobject, sendLaunchURLCommand)(JNIEnv * env, jclass, jstring contentUrl, jstring displayString)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "ManualCommissioningHelper::sendLaunchURLCommand() called");
    
    if (gCommissionedVideoPlayer == nullptr)
    {
        ChipLogError(AppServer, "ManualCommissioningHelper::sendLaunchURLCommand() No commissioned video player");
        return matter::casting::support::convertMatterErrorFromCppToJava(CHIP_ERROR_INCORRECT_STATE);
    }
    
    // Get the first endpoint that supports ContentLauncher
    TargetEndpointInfo * endpoints = gCommissionedVideoPlayer->GetEndpoints();
    if (endpoints == nullptr)
    {
        ChipLogError(AppServer, "ManualCommissioningHelper::sendLaunchURLCommand() No endpoints available");
        return matter::casting::support::convertMatterErrorFromCppToJava(CHIP_ERROR_NOT_FOUND);
    }
    
    TargetEndpointInfo * targetEndpoint = nullptr;
    for (size_t i = 0; i < kMaxNumberOfEndpoints && endpoints[i].IsInitialized(); i++)
    {
        if (endpoints[i].HasCluster(chip::app::Clusters::ContentLauncher::Id))
        {
            targetEndpoint = &endpoints[i];
            break;
        }
    }
    
    if (targetEndpoint == nullptr)
    {
        ChipLogError(AppServer, "ManualCommissioningHelper::sendLaunchURLCommand() No endpoint with ContentLauncher cluster found");
        return matter::casting::support::convertMatterErrorFromCppToJava(CHIP_ERROR_NOT_FOUND);
    }
    
    const char * nativeContentUrl = env->GetStringUTFChars(contentUrl, 0);
    const char * nativeDisplayString = env->GetStringUTFChars(displayString, 0);
    
    ChipLogProgress(AppServer, "ManualCommissioningHelper::sendLaunchURLCommand() Sending LaunchURL to endpoint %d: %s",
                    targetEndpoint->GetEndpointId(), nativeContentUrl);
    
    CHIP_ERROR err = CastingServer::GetInstance()->ContentLauncherLaunchURL(
        targetEndpoint, nativeContentUrl, nativeDisplayString,
        [](CHIP_ERROR err) {
            ChipLogProgress(AppServer, "ManualCommissioningHelper LaunchURL callback: %" CHIP_ERROR_FORMAT, err.Format());
        });
    
    env->ReleaseStringUTFChars(contentUrl, nativeContentUrl);
    env->ReleaseStringUTFChars(displayString, nativeDisplayString);
    
    if (err == CHIP_NO_ERROR)
    {
        ChipLogProgress(AppServer, "ManualCommissioningHelper::sendLaunchURLCommand() Command sent successfully");
    }
    else
    {
        ChipLogError(AppServer, "ManualCommissioningHelper::sendLaunchURLCommand() Failed: %" CHIP_ERROR_FORMAT, err.Format());
    }
    
    return matter::casting::support::convertMatterErrorFromCppToJava(err);
}
